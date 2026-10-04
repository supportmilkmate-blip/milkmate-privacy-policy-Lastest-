package com.example.data.repository

import com.example.data.local.MilkMateDatabase
import com.example.data.local.entity.*
import com.example.data.remote.FirestoreSyncManager
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class MilkMateRepository(
    private val db: MilkMateDatabase,
    val sessionManager: SessionManager,
    val billingManager: BillingManager,
    val reportEngine: ReportEngine,
    val consumptionForecastEngine: ConsumptionForecastEngine,
    val exportManager: ExportManager,
    val backupManager: BackupManager,
    val firestoreSyncManager: FirestoreSyncManager
) {
    val pendingSyncCount: Flow<Int> = db.syncQueueDao().getPendingCountFlow()

    // --- Business ---
    fun getBusinessFlow(businessId: String): Flow<BusinessEntity?> =
        db.businessDao().getBusinessFlow(businessId)

    suspend fun getBusiness(businessId: String): BusinessEntity? =
        db.businessDao().getBusiness(businessId)

    suspend fun getBusinessByPhone(phone: String): BusinessEntity? =
        db.businessDao().getBusinessByPhone(phone.trim())

    suspend fun saveBusiness(business: BusinessEntity) {
        db.businessDao().insertBusiness(business)
        enqueueSync(
            businessId = business.id,
            collection = "business_info",
            docId = business.id,
            operation = "UPSERT",
            payload = businessToJson(business)
        )
    }

    // --- Customers ---
    fun getCustomersFlow(businessId: String): Flow<List<CustomerEntity>> =
        db.customerDao().getAllCustomersFlow(businessId)

    fun getCustomersByTypeFlow(businessId: String, type: String): Flow<List<CustomerEntity>> =
        db.customerDao().getCustomersByTypeFlow(businessId, type)

    suspend fun saveCustomer(customer: CustomerEntity): CanCreateResult {
        val isNew = db.customerDao().getCustomerById(customer.businessId, customer.id) == null
        if (isNew) {
            val business = db.businessDao().getBusiness(customer.businessId)
            val canCreate = billingManager.canCreateCustomer(business, customer.type)
            if (canCreate is CanCreateResult.Blocked) {
                return canCreate
            }
        }

        db.customerDao().insertCustomer(customer)
        enqueueSync(
            businessId = customer.businessId,
            collection = "customers",
            docId = customer.id,
            operation = "UPSERT",
            payload = customerToJson(customer)
        )
        return CanCreateResult.Allowed
    }

    suspend fun deleteCustomer(businessId: String, customerId: String) {
        db.customerDao().softDeleteCustomer(businessId, customerId)
        enqueueSync(
            businessId = businessId,
            collection = "customers",
            docId = customerId,
            operation = "DELETE",
            payload = "{}"
        )
    }

    fun getDeletedCustomersFlow(businessId: String): Flow<List<CustomerEntity>> =
        db.customerDao().getDeletedCustomersFlow(businessId)

    suspend fun restoreCustomer(businessId: String, customerId: String) {
        db.customerDao().restoreCustomer(businessId, customerId)
        val restored = db.customerDao().getCustomerById(businessId, customerId)
        if (restored != null) {
            enqueueSync(businessId, "customers", customerId, "UPSERT", customerToJson(restored))
        }
    }

    suspend fun updateCustomerQuickPresets(businessId: String, customerId: String, presets: String) {
        db.customerDao().updateQuickPresets(businessId, customerId, presets)
        val customer = db.customerDao().getCustomerById(businessId, customerId)
        if (customer != null) {
            enqueueSync(businessId, "customers", customerId, "UPSERT", customerToJson(customer))
        }
    }

    // --- Deliveries ---
    fun getAllDeliveriesFlow(businessId: String): Flow<List<DeliveryEntity>> =
        db.deliveryDao().getDeliveriesBetweenDatesFlow(businessId, 0L, Long.MAX_VALUE)

    fun getDeliveriesByDateAndShiftFlow(businessId: String, date: Long, shift: String): Flow<List<DeliveryEntity>> =
        db.deliveryDao().getDeliveriesByDateAndShiftFlow(businessId, date, shift)

    fun getDeliveriesByDateFlow(businessId: String, date: Long): Flow<List<DeliveryEntity>> =
        db.deliveryDao().getDeliveriesByDateFlow(businessId, date)

    fun getDeliveriesByCustomerFlow(businessId: String, customerId: String): Flow<List<DeliveryEntity>> =
        db.deliveryDao().getDeliveriesByCustomerFlow(businessId, customerId)

    suspend fun saveDelivery(delivery: DeliveryEntity) {
        db.deliveryDao().insertDelivery(delivery)

        // Adjust customer balance: delivery increases balance customer owes us (for INDIVIDUAL / BULK_BUYER)
        // or decreases balance for SUPPLIER milk collection
        val delta = if (delivery.customerType == "SUPPLIER") -delivery.totalAmount else delivery.totalAmount
        if (delivery.isDelivered) {
            db.customerDao().adjustOutstandingBalance(delivery.businessId, delivery.customerId, delta)
        }

        enqueueSync(
            businessId = delivery.businessId,
            collection = "deliveries",
            docId = delivery.id,
            operation = "UPSERT",
            payload = deliveryToJson(delivery)
        )
    }

    suspend fun deleteDelivery(businessId: String, deliveryId: String) {
        val existing = db.deliveryDao().getDeliveryById(businessId, deliveryId)
        if (existing != null && existing.isDelivered) {
            val reverseDelta = if (existing.customerType == "SUPPLIER") existing.totalAmount else -existing.totalAmount
            db.customerDao().adjustOutstandingBalance(businessId, existing.customerId, reverseDelta)
        }
        db.deliveryDao().softDeleteDelivery(businessId, deliveryId)
        enqueueSync(
            businessId = businessId,
            collection = "deliveries",
            docId = deliveryId,
            operation = "DELETE",
            payload = "{}"
        )
    }

    /**
     * Copy deliveries from previous day to today.
     * Prevents duplicate entries.
     */
    suspend fun copyPreviousDayDeliveries(businessId: String, fromDate: Long, toDate: Long, shift: String): Int {
        val previousDeliveries = db.deliveryDao().getDeliveriesByDateAndShift(businessId, fromDate, shift)
        val todayDeliveries = db.deliveryDao().getDeliveriesByDateAndShift(businessId, toDate, shift)
        val existingCustomerIds = todayDeliveries.map { it.customerId }.toSet()

        var copied = 0
        for (prev in previousDeliveries) {
            if (!existingCustomerIds.contains(prev.customerId)) {
                val newDelivery = prev.copy(
                    id = UUID.randomUUID().toString(),
                    deliveryDate = toDate,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    syncStatus = "PENDING"
                )
                saveDelivery(newDelivery)
                copied++
            }
        }
        return copied
    }

    // --- Payments ---
    fun getPaymentsFlow(businessId: String): Flow<List<PaymentEntity>> =
        db.paymentDao().getAllPaymentsFlow(businessId)

    fun getPaymentsByCustomerFlow(businessId: String, customerId: String): Flow<List<PaymentEntity>> =
        db.paymentDao().getPaymentsByCustomerFlow(businessId, customerId)

    suspend fun savePayment(payment: PaymentEntity) {
        db.paymentDao().insertPayment(payment)
        // Payment reduces customer balance owed
        val delta = if (payment.paymentType == "SUPPLIER_PAYMENT") payment.amount else -payment.amount
        db.customerDao().adjustOutstandingBalance(payment.businessId, payment.customerId, delta)

        enqueueSync(
            businessId = payment.businessId,
            collection = "payments",
            docId = payment.id,
            operation = "UPSERT",
            payload = paymentToJson(payment)
        )
    }

    suspend fun deletePayment(businessId: String, paymentId: String) {
        val existing = db.paymentDao().getPaymentById(businessId, paymentId)
        if (existing != null) {
            val reverseDelta = if (existing.paymentType == "SUPPLIER_PAYMENT") -existing.amount else existing.amount
            db.customerDao().adjustOutstandingBalance(businessId, existing.customerId, reverseDelta)
        }
        db.paymentDao().softDeletePayment(businessId, paymentId)
        enqueueSync(
            businessId = businessId,
            collection = "payments",
            docId = paymentId,
            operation = "DELETE",
            payload = "{}"
        )
    }

    // --- Expenses ---
    fun getExpensesFlow(businessId: String, accountStartDate: Long = 0L): Flow<List<ExpenseEntity>> {
        return if (accountStartDate > 0L) {
            db.expenseDao().getExpensesFromDateFlow(businessId, accountStartDate)
        } else {
            db.expenseDao().getAllExpensesFlow(businessId)
        }
    }

    suspend fun getExpense(businessId: String, expenseId: String): ExpenseEntity? =
        db.expenseDao().getExpenseById(businessId, expenseId)

    suspend fun saveExpense(expense: ExpenseEntity) {
        db.expenseDao().insertExpense(expense)
        enqueueSync(
            businessId = expense.businessId,
            collection = "expenses",
            docId = expense.id,
            operation = "UPSERT",
            payload = expenseToJson(expense)
        )
    }

    suspend fun deleteExpense(businessId: String, expenseId: String, permanent: Boolean = true): Result<Unit> {
        val existing = db.expenseDao().getExpenseById(businessId, expenseId)
        if (existing != null && (existing.isInventoryPurchase || !existing.inventoryItemId.isNullOrBlank())) {
            // Find linked inventory purchase transaction
            val linkedTx = db.inventoryDao().getTransactionByLinkedExpenseId(expenseId)
                ?: (if (!existing.inventoryItemId.isNullOrBlank()) {
                    db.inventoryDao().getAllTransactions(businessId).firstOrNull {
                        it.itemId == existing.inventoryItemId && it.transactionType == "PURCHASE" && Math.abs(it.totalAmount - existing.amount) < 0.01 && it.date == existing.date
                    }
                } else null)

            if (linkedTx != null) {
                val deleteResult = deleteStockPurchase(linkedTx.id)
                if (deleteResult.isFailure) {
                    return deleteResult
                }
            }
        }

        if (permanent) {
            db.expenseDao().deleteExpensePermanently(businessId, expenseId)
        } else {
            db.expenseDao().softDeleteExpense(businessId, expenseId)
        }
        enqueueSync(
            businessId = businessId,
            collection = "expenses",
            docId = expenseId,
            operation = "DELETE",
            payload = "{}"
        )
        return Result.success(Unit)
    }

    fun getDeletedExpensesFlow(businessId: String): Flow<List<ExpenseEntity>> =
        db.expenseDao().getDeletedExpensesFlow(businessId)

    suspend fun restoreExpense(businessId: String, expenseId: String) {
        db.expenseDao().restoreExpense(businessId, expenseId)
        val restored = db.expenseDao().getExpenseById(businessId, expenseId)
        if (restored != null) {
            enqueueSync(businessId, "expenses", expenseId, "UPSERT", expenseToJson(restored))
        }
    }

    // --- Inventory ---
    fun getInventoryItemsFlow(businessId: String): Flow<List<InventoryItemEntity>> =
        db.inventoryDao().getAllInventoryItemsFlow(businessId)

    suspend fun getInventoryItem(businessId: String, itemId: String): InventoryItemEntity? =
        db.inventoryDao().getInventoryItemById(businessId, itemId)

    suspend fun initializeDefaultInventoryItems(businessId: String, businessMode: String? = null) {
        val biz = db.businessDao().getBusinessById(businessId)
        val mode = businessMode ?: biz?.businessMode
        val showFeed = com.example.ui.util.BusinessModeFeatures.showCattleFeed(mode)
        val existing = db.inventoryDao().getAllInventoryItems(businessId)

        // 1. If this business mode DOES NOT use cattle feed, purge all feed items
        if (!showFeed) {
            val feedItems = existing.filter { com.example.ui.util.BusinessModeFeatures.isFeedItem(it.itemName) }
            val idsToDelete = feedItems.map { it.id }
            if (idsToDelete.isNotEmpty()) {
                db.inventoryDao().deleteItemsPermanently(businessId, idsToDelete)
            }
        }

        val updatedExisting = db.inventoryDao().getAllInventoryItems(businessId)
        val showProduct = com.example.ui.util.BusinessModeFeatures.showProductInventory(mode)

        val needsItems = if (updatedExisting.isEmpty()) {
            true
        } else if (!showFeed && showProduct) {
            // If in non-feed product mode (Retail Parlour / Processing Unit), check if any dairy products exist
            updatedExisting.none { com.example.ui.util.BusinessModeFeatures.isDairyProductItem(it.itemName) }
        } else if (showFeed && !showProduct) {
            updatedExisting.none { com.example.ui.util.BusinessModeFeatures.isFeedItem(it.itemName) }
        } else {
            false
        }

        if (needsItems) {
            val defaultItems = com.example.ui.util.BusinessModeFeatures.getDefaultInventoryItemsForMode(mode)
            val now = System.currentTimeMillis()
            for ((name, unit, rate) in defaultItems) {
                // Avoid duplicating if an item with the same name already exists
                if (updatedExisting.any { it.itemName.equals(name, ignoreCase = true) }) continue

                val item = InventoryItemEntity(
                    id = "item_${name.lowercase().replace(" ", "_").replace("/", "_")}_${UUID.randomUUID().toString().take(6)}",
                    businessId = businessId,
                    itemName = name,
                    unit = unit,
                    currentStock = 0.0,
                    openingStock = 0.0,
                    isOpeningVerified = false,
                    costPerUnit = rate,
                    dailyUsage = null,
                    alertDaysThreshold = 10,
                    createdAt = now,
                    updatedAt = now
                )
                db.inventoryDao().insertItem(item)
            }
        }
    }

    suspend fun saveInventoryItem(item: InventoryItemEntity) {
        db.inventoryDao().insertItem(item)
        enqueueSync(
            businessId = item.businessId,
            collection = "inventory_items",
            docId = item.id,
            operation = "UPSERT",
            payload = inventoryItemToJson(item)
        )
    }

    suspend fun updateDailyUsage(businessId: String, itemId: String, dailyUsage: Double?) {
        db.inventoryDao().updateDailyUsage(businessId, itemId, dailyUsage)
    }

    suspend fun verifyOpeningStock(
        businessId: String,
        itemId: String,
        openingQuantity: Double,
        yearMonth: String
    ): Result<Unit> {
        val item = db.inventoryDao().getInventoryItemById(businessId, itemId)
            ?: return Result.failure(Exception("Item not found"))

        val oldOpening = if (item.isOpeningVerified) item.openingStock else 0.0
        val delta = openingQuantity - oldOpening

        val updatedItem = item.copy(
            openingStock = openingQuantity,
            isOpeningVerified = true,
            currentStock = (item.currentStock + delta).coerceAtLeast(0.0),
            updatedAt = System.currentTimeMillis()
        )
        db.inventoryDao().updateItem(updatedItem)

        // Record or update month closing for start month
        val closingId = "${businessId}_${itemId}_${yearMonth}"
        val existingClosing = db.inventoryDao().getMonthClosing(businessId, itemId, yearMonth)
        val closing = existingClosing?.copy(
            openingStock = openingQuantity,
            isOpeningVerified = true,
            updatedAt = System.currentTimeMillis()
        ) ?: InventoryMonthClosingEntity(
            id = closingId,
            businessId = businessId,
            itemId = itemId,
            itemName = item.itemName,
            yearMonth = yearMonth,
            openingStock = openingQuantity,
            isOpeningVerified = true
        )
        db.inventoryDao().insertMonthClosing(closing)

        // Log OPENING_STOCK transaction
        val tx = InventoryTransactionEntity(
            id = UUID.randomUUID().toString(),
            businessId = businessId,
            itemId = itemId,
            itemName = item.itemName,
            transactionType = "OPENING_STOCK",
            quantity = openingQuantity,
            unitPrice = item.costPerUnit,
            totalAmount = openingQuantity * item.costPerUnit,
            date = System.currentTimeMillis(),
            notes = "Opening stock verified: $openingQuantity ${item.unit}",
            yearMonth = yearMonth
        )
        db.inventoryDao().insertTransaction(tx)
        return Result.success(Unit)
    }

    suspend fun recordStockPurchase(
        businessId: String,
        itemId: String,
        quantity: Double,
        rate: Double,
        date: Long,
        supplier: String,
        paymentStatus: String,
        paymentMethod: String,
        notes: String,
        accountStartDate: Long
    ): Result<InventoryTransactionEntity> {
        if (date < accountStartDate) {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            return Result.failure(Exception("Purchase date cannot be before Account Start Date (${sdf.format(Date(accountStartDate))})"))
        }
        val item = db.inventoryDao().getInventoryItemById(businessId, itemId)
            ?: return Result.failure(Exception("Item not found"))

        val totalAmount = Math.round(quantity * rate * 100.0) / 100.0
        val yearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(date))

        // Create single financial outflow expense
        val expenseId = "EXP-PURCHASE-${UUID.randomUUID().toString().take(8).uppercase()}"
        val expense = ExpenseEntity(
            id = expenseId,
            businessId = businessId,
            date = date,
            amount = totalAmount,
            category = "INVENTORY_PURCHASE",
            subcategory = item.itemName,
            notes = "Stock purchase: $quantity ${item.unit} @ ₹$rate/unit from ${supplier.ifBlank { "Supplier" }}. $notes",
            paymentMethod = paymentMethod,
            inventoryItemId = itemId,
            inventoryItemName = item.itemName,
            expenseType = "BUSINESS",
            isInventoryPurchase = true
        )
        saveExpense(expense)

        val tx = InventoryTransactionEntity(
            id = UUID.randomUUID().toString(),
            businessId = businessId,
            itemId = itemId,
            itemName = item.itemName,
            transactionType = "PURCHASE",
            quantity = quantity,
            unitPrice = rate,
            totalAmount = totalAmount,
            date = date,
            supplier = supplier.trim(),
            paymentStatus = paymentStatus,
            paymentMethod = paymentMethod,
            notes = notes.trim(),
            yearMonth = yearMonth,
            linkedExpenseId = expenseId
        )
        db.inventoryDao().insertTransaction(tx)
        db.inventoryDao().adjustCurrentStock(businessId, itemId, quantity)

        // Update item average cost / latest rate
        val updatedItem = item.copy(
            costPerUnit = rate,
            updatedAt = System.currentTimeMillis()
        )
        db.inventoryDao().updateItem(updatedItem)

        return Result.success(tx)
    }

    suspend fun editStockPurchase(
        transactionId: String,
        newQuantity: Double,
        newRate: Double,
        newDate: Long,
        newSupplier: String,
        newPaymentStatus: String,
        newPaymentMethod: String,
        newNotes: String,
        accountStartDate: Long
    ): Result<Unit> {
        if (newDate < accountStartDate) {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            return Result.failure(Exception("Purchase date cannot be before Account Start Date (${sdf.format(Date(accountStartDate))})"))
        }

        val tx = db.inventoryDao().getTransactionById(transactionId)
            ?: return Result.failure(Exception("Purchase record not found"))

        val item = db.inventoryDao().getInventoryItemById(tx.businessId, tx.itemId)
            ?: return Result.failure(Exception("Item not found"))

        val deltaStock = newQuantity - tx.quantity
        if (item.currentStock + deltaStock < 0) {
            return Result.failure(
                Exception(
                    "Cannot reduce purchase to $newQuantity ${item.unit}. You have only ${item.currentStock} ${item.unit} in current physical stock because part of this purchase was already consumed."
                )
            )
        }

        val newTotalAmount = Math.round(newQuantity * newRate * 100.0) / 100.0
        val newYearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(newDate))

        val updatedTx = tx.copy(
            quantity = newQuantity,
            unitPrice = newRate,
            totalAmount = newTotalAmount,
            date = newDate,
            supplier = newSupplier.trim(),
            paymentStatus = newPaymentStatus,
            paymentMethod = newPaymentMethod,
            notes = newNotes.trim(),
            yearMonth = newYearMonth,
            updatedAt = System.currentTimeMillis()
        )
        db.inventoryDao().updateTransaction(updatedTx)

        // Adjust stock and rate
        db.inventoryDao().adjustCurrentStock(tx.businessId, tx.itemId, deltaStock)
        db.inventoryDao().updateItem(item.copy(costPerUnit = newRate, updatedAt = System.currentTimeMillis()))

        // Update linked expense if present
        if (!tx.linkedExpenseId.isNullOrBlank()) {
            val existingExp = db.expenseDao().getExpenseById(tx.businessId, tx.linkedExpenseId)
            if (existingExp != null) {
                val updatedExp = existingExp.copy(
                    amount = newTotalAmount,
                    date = newDate,
                    notes = "Stock purchase: $newQuantity ${item.unit} @ ₹$newRate/unit from ${newSupplier.ifBlank { "Supplier" }}. $newNotes",
                    paymentMethod = newPaymentMethod,
                    updatedAt = System.currentTimeMillis()
                )
                db.expenseDao().updateExpense(updatedExp)
            }
        }

        return Result.success(Unit)
    }

    suspend fun deleteStockPurchase(transactionId: String): Result<Unit> {
        val tx = db.inventoryDao().getTransactionById(transactionId)
            ?: return Result.failure(Exception("Purchase record not found"))

        val item = db.inventoryDao().getInventoryItemById(tx.businessId, tx.itemId)
            ?: return Result.failure(Exception("Item not found"))

        // Check safety: if deleting this purchase would make current stock negative, warn and prevent!
        if (item.currentStock - tx.quantity < -0.001) {
            return Result.failure(
                Exception(
                    "Cannot delete this purchase of ${tx.quantity} ${item.unit}. Only ${item.currentStock} ${item.unit} currently remains in stock because this purchase has already been consumed! Please adjust consumption records first."
                )
            )
        }

        // Reverse stock
        db.inventoryDao().adjustCurrentStock(tx.businessId, tx.itemId, -tx.quantity)
        db.inventoryDao().softDeleteTransaction(transactionId)

        // Soft delete linked expense so financial records reflect deletion
        if (!tx.linkedExpenseId.isNullOrBlank()) {
            db.expenseDao().softDeleteExpense(tx.businessId, tx.linkedExpenseId)
        }

        return Result.success(Unit)
    }

    suspend fun recordStockConsumption(
        businessId: String,
        itemId: String,
        quantity: Double,
        date: Long,
        reason: String,
        notes: String,
        accountStartDate: Long
    ): Result<Unit> {
        if (date < accountStartDate) {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            return Result.failure(Exception("Consumption date cannot be before Account Start Date (${sdf.format(Date(accountStartDate))})"))
        }

        val item = db.inventoryDao().getInventoryItemById(businessId, itemId)
            ?: return Result.failure(Exception("Item not found"))

        if (quantity > item.currentStock + 0.001) {
            return Result.failure(
                Exception("Cannot consume $quantity ${item.unit}. Current available stock is only ${item.currentStock} ${item.unit}.")
            )
        }

        val yearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(date))

        // CRITICAL SPECIFICATION: Consumption does NOT create an expense in Profit!
        // Purely a stock movement transaction.
        val tx = InventoryTransactionEntity(
            id = UUID.randomUUID().toString(),
            businessId = businessId,
            itemId = itemId,
            itemName = item.itemName,
            transactionType = "CONSUMPTION",
            quantity = quantity,
            unitPrice = item.costPerUnit,
            totalAmount = 0.0,
            date = date,
            reason = reason.trim(),
            notes = notes.trim(),
            yearMonth = yearMonth
        )
        db.inventoryDao().insertTransaction(tx)
        db.inventoryDao().adjustCurrentStock(businessId, itemId, -quantity)

        return Result.success(Unit)
    }

    suspend fun deleteStockConsumption(transactionId: String): Result<Unit> {
        val tx = db.inventoryDao().getTransactionById(transactionId)
            ?: return Result.failure(Exception("Consumption record not found"))

        // Reverse stock by adding consumed quantity back
        db.inventoryDao().adjustCurrentStock(tx.businessId, tx.itemId, tx.quantity)
        db.inventoryDao().softDeleteTransaction(transactionId)
        return Result.success(Unit)
    }

    suspend fun verifyPhysicalClosingStock(
        businessId: String,
        itemId: String,
        yearMonth: String,
        physicalStock: Double,
        notes: String,
        verificationDate: Long = System.currentTimeMillis()
    ): Result<Double> {
        val item = db.inventoryDao().getInventoryItemById(businessId, itemId)
            ?: return Result.failure(Exception("Item not found"))

        // Difference between physical measured stock and current book stock
        val difference = physicalStock - item.currentStock

        // If difference, record ADJUSTMENT
        if (Math.abs(difference) > 0.001) {
            val tx = InventoryTransactionEntity(
                id = UUID.randomUUID().toString(),
                businessId = businessId,
                itemId = itemId,
                itemName = item.itemName,
                transactionType = "ADJUSTMENT",
                quantity = difference,
                unitPrice = item.costPerUnit,
                totalAmount = 0.0,
                date = verificationDate,
                reason = "Physical Stock Verification",
                notes = "Stock verified on ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(verificationDate))}. Count: $physicalStock ${item.unit} (Adjustment: ${String.format(Locale.US, "%+.2f", difference)} ${item.unit}). $notes",
                yearMonth = yearMonth
            )
            db.inventoryDao().insertTransaction(tx)
            db.inventoryDao().setCurrentStock(businessId, itemId, physicalStock)
        }

        // Save month closing record
        val closingId = "${businessId}_${itemId}_${yearMonth}"
        val existingClosing = db.inventoryDao().getMonthClosing(businessId, itemId, yearMonth)
        val closing = existingClosing?.copy(
            physicalClosingStock = physicalStock,
            isClosingVerified = true,
            verifiedAt = verificationDate,
            notes = notes.trim(),
            updatedAt = System.currentTimeMillis()
        ) ?: InventoryMonthClosingEntity(
            id = closingId,
            businessId = businessId,
            itemId = itemId,
            itemName = item.itemName,
            yearMonth = yearMonth,
            openingStock = item.openingStock,
            isOpeningVerified = item.isOpeningVerified,
            bookClosingStock = item.currentStock,
            physicalClosingStock = physicalStock,
            isClosingVerified = true,
            verifiedAt = verificationDate,
            notes = notes.trim()
        )
        db.inventoryDao().insertMonthClosing(closing)

        // Automatically set NEXT MONTH's opening stock to this verified physical closing!
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val cal = Calendar.getInstance().apply {
            time = sdf.parse(yearMonth) ?: Date()
            add(Calendar.MONTH, 1)
        }
        val nextYearMonth = sdf.format(cal.time)
        val nextClosingId = "${businessId}_${itemId}_${nextYearMonth}"
        val existingNext = db.inventoryDao().getMonthClosing(businessId, itemId, nextYearMonth)
        val nextClosing = existingNext?.copy(
            openingStock = physicalStock,
            isOpeningVerified = true,
            updatedAt = System.currentTimeMillis()
        ) ?: InventoryMonthClosingEntity(
            id = nextClosingId,
            businessId = businessId,
            itemId = itemId,
            itemName = item.itemName,
            yearMonth = nextYearMonth,
            openingStock = physicalStock,
            isOpeningVerified = true
        )
        db.inventoryDao().insertMonthClosing(nextClosing)

        return Result.success(difference)
    }

    suspend fun setInitialOpeningStock(
        businessId: String,
        itemId: String,
        openingStock: Double,
        costPerUnit: Double,
        asOfDate: Long
    ): Result<Unit> {
        val item = db.inventoryDao().getInventoryItemById(businessId, itemId)
            ?: return Result.failure(Exception("Item not found"))

        val yearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(asOfDate))

        // Calculate adjustment difference if current stock was based on previous opening stock
        val oldOpening = item.openingStock
        val openingDifference = openingStock - oldOpening
        val newCurrentStock = (item.currentStock + openingDifference).coerceAtLeast(0.0)

        // Update item entity
        val updatedItem = item.copy(
            openingStock = openingStock,
            currentStock = if (!item.isOpeningVerified && item.currentStock == 0.0) openingStock else newCurrentStock,
            isOpeningVerified = true,
            costPerUnit = if (costPerUnit > 0) costPerUnit else item.costPerUnit,
            updatedAt = System.currentTimeMillis()
        )
        db.inventoryDao().updateItem(updatedItem)

        // Record or update initial stock transaction
        val existingTx = db.inventoryDao().getAllTransactions(businessId)
            .find { it.itemId == itemId && it.transactionType == "OPENING_STOCK" }

        if (existingTx != null) {
            val updatedTx = existingTx.copy(
                quantity = openingStock,
                unitPrice = if (costPerUnit > 0) costPerUnit else item.costPerUnit,
                totalAmount = openingStock * (if (costPerUnit > 0) costPerUnit else item.costPerUnit),
                date = asOfDate,
                notes = "Initial Opening Stock on Hand before account creation / as-of ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(asOfDate))}"
            )
            db.inventoryDao().updateTransaction(updatedTx)
        } else {
            val tx = InventoryTransactionEntity(
                id = UUID.randomUUID().toString(),
                businessId = businessId,
                itemId = itemId,
                itemName = item.itemName,
                transactionType = "OPENING_STOCK",
                quantity = openingStock,
                unitPrice = if (costPerUnit > 0) costPerUnit else item.costPerUnit,
                totalAmount = openingStock * (if (costPerUnit > 0) costPerUnit else item.costPerUnit),
                date = asOfDate,
                reason = "Initial Stock on Hand",
                notes = "Pre-existing stock before app registration entered as of ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(asOfDate))}",
                yearMonth = yearMonth
            )
            db.inventoryDao().insertTransaction(tx)
        }

        // Save into month closing for that yearMonth
        val closingId = "${businessId}_${itemId}_${yearMonth}"
        val existingClosing = db.inventoryDao().getMonthClosing(businessId, itemId, yearMonth)
        val closing = existingClosing?.copy(
            openingStock = openingStock,
            isOpeningVerified = true,
            updatedAt = System.currentTimeMillis()
        ) ?: InventoryMonthClosingEntity(
            id = closingId,
            businessId = businessId,
            itemId = itemId,
            itemName = item.itemName,
            yearMonth = yearMonth,
            openingStock = openingStock,
            isOpeningVerified = true,
            bookClosingStock = newCurrentStock
        )
        db.inventoryDao().insertMonthClosing(closing)

        return Result.success(Unit)
    }

    fun getAllInventoryTransactionsFlow(businessId: String): Flow<List<InventoryTransactionEntity>> =
        db.inventoryDao().getAllTransactionsFlow(businessId)

    fun getInventoryTransactionsBetweenDatesFlow(businessId: String, startDate: Long, endDate: Long): Flow<List<InventoryTransactionEntity>> =
        db.inventoryDao().getTransactionsBetweenDatesFlow(businessId, startDate, endDate)

    fun getInventoryLedgerFlow(businessId: String, itemId: String): Flow<List<InventoryTransactionEntity>> =
        db.inventoryDao().getTransactionsByItemFlow(businessId, itemId)

    fun getMonthClosingsFlow(businessId: String, yearMonth: String): Flow<List<InventoryMonthClosingEntity>> =
        db.inventoryDao().getMonthClosingsFlow(businessId, yearMonth)

    suspend fun getMonthClosings(businessId: String, yearMonth: String): List<InventoryMonthClosingEntity> =
        db.inventoryDao().getMonthClosings(businessId, yearMonth)

    // --- Consumption Forecasting ---
    suspend fun generateConsumptionForecast(
        businessId: String,
        planningHorizonDays: Int = 30,
        safetyBufferDays: Int = 7,
        businessMode: String? = null
    ): ConsumptionForecastSummary {
        val biz = if (businessMode == null) db.businessDao().getBusinessById(businessId) else null
        val effectiveMode = businessMode ?: biz?.businessMode
        return consumptionForecastEngine.generateForecast(
            businessId = businessId,
            planningHorizonDays = planningHorizonDays,
            safetyBufferDays = safetyBufferDays,
            businessMode = effectiveMode
        )
    }

    // --- Orders ---
    fun getOrdersFlow(businessId: String): Flow<List<OrderEntity>> =
        db.orderDao().getAllOrdersFlow(businessId)

    suspend fun saveOrder(order: OrderEntity) {
        db.orderDao().insertOrder(order)
        enqueueSync(
            businessId = order.businessId,
            collection = "orders",
            docId = order.id,
            operation = "UPSERT",
            payload = orderToJson(order)
        )
    }

    suspend fun updateOrderStatus(businessId: String, orderId: String, status: String) {
        db.orderDao().updateOrderStatus(businessId, orderId, status)
        enqueueSync(
            businessId = businessId,
            collection = "orders",
            docId = orderId,
            operation = "UPSERT",
            payload = "{\"id\":\"$orderId\",\"status\":\"$status\"}"
        )
    }

    suspend fun deleteOrder(businessId: String, orderId: String) {
        db.orderDao().softDeleteOrder(businessId, orderId)
        enqueueSync(
            businessId = businessId,
            collection = "orders",
            docId = orderId,
            operation = "DELETE",
            payload = "{}"
        )
    }

    // --- Staff ---
    fun getStaffFlow(businessId: String): Flow<List<StaffEntity>> =
        db.staffDao().getAllStaffFlow(businessId)

    suspend fun saveStaff(staff: StaffEntity) {
        db.staffDao().insertStaff(staff)
        enqueueSync(
            businessId = staff.businessId,
            collection = "staff",
            docId = staff.id,
            operation = "UPSERT",
            payload = staffToJson(staff)
        )
    }

    suspend fun deleteStaff(businessId: String, staffId: String) {
        db.staffDao().softDeleteStaff(businessId, staffId)
        enqueueSync(
            businessId = businessId,
            collection = "staff",
            docId = staffId,
            operation = "DELETE",
            payload = "{}"
        )
    }

    suspend fun authenticateStaff(businessId: String, identifier: String, pin: String): StaffEntity? {
        return db.staffDao().authenticateStaffInBusiness(businessId, identifier, pin)
    }

    // --- Staff Attendance & Payments ---
    fun getAttendanceForDateFlow(businessId: String, date: Long): Flow<List<StaffAttendanceEntity>> =
        db.staffDao().getAttendanceForDateFlow(businessId, date)

    fun getAllAttendanceFlow(businessId: String): Flow<List<StaffAttendanceEntity>> =
        db.staffDao().getAllAttendanceFlow(businessId)

    suspend fun saveAttendance(attendance: StaffAttendanceEntity) {
        db.staffDao().insertAttendance(attendance)
    }

    suspend fun deleteAttendance(id: String) {
        db.staffDao().deleteAttendance(id)
    }

    fun getAllStaffPaymentsFlow(businessId: String): Flow<List<StaffPaymentEntity>> =
        db.staffDao().getAllStaffPaymentsFlow(businessId)

    fun getStaffPaymentsForMonthFlow(businessId: String, monthYear: String): Flow<List<StaffPaymentEntity>> =
        db.staffDao().getStaffPaymentsForMonthFlow(businessId, monthYear)

    suspend fun saveStaffPayment(payment: StaffPaymentEntity) {
        db.staffDao().insertStaffPayment(payment)
    }

    suspend fun deleteStaffPayment(id: String) {
        db.staffDao().deleteStaffPayment(id)
    }

    suspend fun getAllBusinesses(): List<BusinessEntity> =
        db.businessDao().getAllBusinesses()

    suspend fun allotRouteAndStaff(
        businessId: String,
        customerIds: List<String>,
        route: String,
        staffId: String,
        staffName: String
    ) {
        db.customerDao().bulkAllotRouteAndStaff(businessId, customerIds, route, staffId, staffName)
    }

    // --- Sync Queue Helper ---
    private suspend fun enqueueSync(
        businessId: String,
        collection: String,
        docId: String,
        operation: String,
        payload: String
    ) {
        val item = SyncQueueEntity(
            id = UUID.randomUUID().toString(),
            businessId = businessId,
            collectionName = collection,
            documentId = docId,
            operation = operation,
            payloadJson = payload,
            timestamp = System.currentTimeMillis()
        )
        db.syncQueueDao().enqueue(item)
    }

    suspend fun triggerSync(): Int {
        return firestoreSyncManager.syncPendingItems()
    }

    // --- JSON Serializers ---
    private fun businessToJson(b: BusinessEntity): String = JSONObject().apply {
        put("id", b.id)
        put("businessName", b.businessName)
        put("ownerName", b.ownerName)
        put("phone", b.phone)
        put("accountStartDate", b.accountStartDate)
        put("defaultMilkType", b.defaultMilkType)
        put("cowMilkRate", b.cowMilkRate)
        put("buffaloMilkRate", b.buffaloMilkRate)
        put("pricingMode", b.pricingMode)
        put("subscriptionPlan", b.subscriptionPlan)
        put("subscriptionStatus", b.subscriptionStatus)
        put("updatedAt", b.updatedAt)
    }.toString()

    private fun customerToJson(c: CustomerEntity): String = JSONObject().apply {
        put("id", c.id)
        put("businessId", c.businessId)
        put("name", c.name)
        put("mobile", c.mobile)
        put("address", c.address)
        put("type", c.type)
        put("rate", c.rate)
        put("milkType", c.milkType)
        put("defaultQuantity", c.defaultQuantity)
        put("defaultShift", c.defaultShift)
        put("quickPresets", c.quickPresets)
        put("route", c.route)
        put("notes", c.notes)
        put("outstandingBalance", c.outstandingBalance)
        put("updatedAt", c.updatedAt)
    }.toString()

    private fun deliveryToJson(d: DeliveryEntity): String = JSONObject().apply {
        put("id", d.id)
        put("businessId", d.businessId)
        put("customerId", d.customerId)
        put("customerName", d.customerName)
        put("customerType", d.customerType)
        put("deliveryDate", d.deliveryDate)
        put("shift", d.shift)
        put("milkType", d.milkType)
        put("quantityLiters", d.quantityLiters)
        put("ratePerLiter", d.ratePerLiter)
        put("totalAmount", d.totalAmount)
        put("fat", d.fat)
        put("snf", d.snf)
        put("isDelivered", d.isDelivered)
        put("updatedAt", d.updatedAt)
    }.toString()

    private fun paymentToJson(p: PaymentEntity): String = JSONObject().apply {
        put("id", p.id)
        put("businessId", p.businessId)
        put("customerId", p.customerId)
        put("customerName", p.customerName)
        put("amount", p.amount)
        put("date", p.date)
        put("method", p.method)
        put("paymentType", p.paymentType)
        put("referenceNo", p.referenceNo)
    }.toString()

    private fun expenseToJson(e: ExpenseEntity): String = JSONObject().apply {
        put("id", e.id)
        put("businessId", e.businessId)
        put("date", e.date)
        put("amount", e.amount)
        put("category", e.category)
        put("subcategory", e.subcategory)
        put("notes", e.notes)
        put("paymentMethod", e.paymentMethod)
        put("inventoryItemId", e.inventoryItemId ?: "")
        put("inventoryItemName", e.safeInventoryName())
    }.toString()

    private fun inventoryItemToJson(i: InventoryItemEntity): String = JSONObject().apply {
        put("id", i.id)
        put("businessId", i.businessId)
        put("itemName", i.itemName)
        put("unit", i.unit)
        put("currentStock", i.currentStock)
        put("openingStock", i.openingStock)
        put("costPerUnit", i.costPerUnit)
    }.toString()

    private fun orderToJson(o: OrderEntity): String = JSONObject().apply {
        put("id", o.id)
        put("businessId", o.businessId)
        put("customerId", o.customerId)
        put("customerName", o.customerName)
        put("productName", o.productName)
        put("quantity", o.quantity)
        put("unit", o.unit)
        put("rate", o.rate)
        put("totalAmount", o.totalAmount)
        put("status", o.status)
        put("deliveryDate", o.deliveryDate)
        put("reminderDate", o.reminderDate)
        put("reminderTime", o.reminderTime)
    }.toString()

    private fun staffToJson(s: StaffEntity): String = JSONObject().apply {
        put("id", s.id)
        put("businessId", s.businessId)
        put("name", s.name)
        put("mobile", s.mobile)
        put("pin", s.pin)
        put("role", s.role)
        put("permissions", s.permissions)
        put("assignedRoute", s.assignedRoute)
        put("isActive", s.isActive)
    }.toString()

    suspend fun populateDemoData(businessId: String) {
        val now = System.currentTimeMillis()
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val todayMidnight = cal.timeInMillis

        // Demo Customers
        val demoCustomers = listOf(
            CustomerEntity(
                id = "cust_demo_1",
                businessId = businessId,
                name = "Ramesh Sharma",
                mobile = "+91 98765 43210",
                address = "Flat 204, Green Park Heights",
                route = "Green Park",
                type = "INDIVIDUAL",
                defaultShift = "MORNING",
                defaultQuantity = 2.5,
                milkType = "BOTH",
                cowQuantity = 1.0,
                cowRate = 60.0,
                buffaloQuantity = 1.5,
                buffaloRate = 72.0,
                rate = 67.2,
                outstandingBalance = 450.0,
                createdAt = now
            ),
            CustomerEntity(
                id = "cust_demo_2",
                businessId = businessId,
                name = "Anita Verma",
                mobile = "+91 98111 22334",
                address = "House 14, Civil Lines",
                route = "Civil Lines",
                type = "INDIVIDUAL",
                defaultShift = "MORNING",
                defaultQuantity = 2.0,
                milkType = "BUFFALO",
                cowQuantity = 1.0,
                cowRate = 55.0,
                buffaloQuantity = 2.0,
                buffaloRate = 68.0,
                rate = 68.0,
                outstandingBalance = 0.0,
                createdAt = now
            ),
            CustomerEntity(
                id = "cust_demo_3",
                businessId = businessId,
                name = "Suresh Patel",
                mobile = "+91 98222 33445",
                address = "Villa 8, Green Park",
                route = "Green Park",
                type = "INDIVIDUAL",
                defaultShift = "EVENING",
                defaultQuantity = 1.0,
                milkType = "COW",
                cowQuantity = 1.0,
                cowRate = 60.0,
                buffaloQuantity = 1.0,
                buffaloRate = 70.0,
                rate = 60.0,
                outstandingBalance = 180.0,
                createdAt = now
            ),
            CustomerEntity(
                id = "cust_demo_4",
                businessId = businessId,
                name = "Bansal Dairy Sweets",
                mobile = "+91 98333 44556",
                address = "Shop 12, Main Market Road",
                route = "Commercial Market",
                type = "BULK_BUYER",
                defaultShift = "BOTH",
                defaultQuantity = 25.0,
                milkType = "BUFFALO",
                rate = 58.0,
                outstandingBalance = 2400.0,
                createdAt = now
            ),
            CustomerEntity(
                id = "cust_demo_5",
                businessId = businessId,
                name = "Hotel Krishna Veg",
                mobile = "+91 98444 55667",
                address = "Opposite Railway Station",
                route = "Station Road",
                type = "BULK_BUYER",
                defaultShift = "MORNING",
                defaultQuantity = 35.0,
                milkType = "COW",
                rate = 54.0,
                outstandingBalance = 1620.0,
                createdAt = now
            ),
            CustomerEntity(
                id = "cust_demo_6",
                businessId = businessId,
                name = "Kishan Yadav (Farmer)",
                mobile = "+91 98555 66778",
                address = "Rampur Dairy Cluster, Gate 2",
                route = "Dairy Cluster",
                type = "SUPPLIER",
                defaultShift = "BOTH",
                defaultQuantity = 40.0,
                milkType = "COW",
                rate = 42.0,
                outstandingBalance = -3360.0, // We owe supplier
                createdAt = now
            ),
            CustomerEntity(
                id = "cust_demo_7",
                businessId = businessId,
                name = "Dharampal Dairy Farm",
                mobile = "+91 98666 77889",
                address = "Village Road, Dairy Shed 4",
                route = "Dairy Cluster",
                type = "SUPPLIER",
                defaultShift = "BOTH",
                defaultQuantity = 50.0,
                milkType = "BUFFALO",
                rate = 48.0,
                outstandingBalance = -4800.0,
                createdAt = now
            )
        )

        for (c in demoCustomers) {
            db.customerDao().insertCustomer(c)
        }

        // Demo Deliveries for Today
        val demoDeliveries = listOf(
            DeliveryEntity(
                id = "del_demo_1",
                businessId = businessId,
                customerId = "cust_demo_1",
                customerName = "Ramesh Sharma",
                customerType = "INDIVIDUAL",
                deliveryDate = todayMidnight,
                shift = "MORNING",
                milkType = "COW",
                quantityLiters = 1.5,
                ratePerLiter = 60.0,
                totalAmount = 90.0,
                isDelivered = true,
                notes = "Paid Cash",
                createdAt = now
            ),
            DeliveryEntity(
                id = "del_demo_2",
                businessId = businessId,
                customerId = "cust_demo_2",
                customerName = "Anita Verma",
                customerType = "INDIVIDUAL",
                deliveryDate = todayMidnight,
                shift = "MORNING",
                milkType = "BUFFALO",
                quantityLiters = 2.0,
                ratePerLiter = 68.0,
                totalAmount = 136.0,
                isDelivered = true,
                notes = "Pending",
                createdAt = now
            ),
            DeliveryEntity(
                id = "del_demo_3",
                businessId = businessId,
                customerId = "cust_demo_5",
                customerName = "Hotel Krishna Veg",
                customerType = "BULK_BUYER",
                deliveryDate = todayMidnight,
                shift = "MORNING",
                milkType = "COW",
                quantityLiters = 35.0,
                ratePerLiter = 54.0,
                totalAmount = 1890.0,
                isDelivered = true,
                notes = "Paid Online UPI",
                createdAt = now
            ),
            DeliveryEntity(
                id = "del_demo_4",
                businessId = businessId,
                customerId = "cust_demo_4",
                customerName = "Bansal Dairy Sweets",
                customerType = "BULK_BUYER",
                deliveryDate = todayMidnight,
                shift = "MORNING",
                milkType = "BUFFALO",
                quantityLiters = 15.0,
                ratePerLiter = 58.0,
                totalAmount = 870.0,
                isDelivered = false,
                notes = "Pending Delivery",
                createdAt = now
            ),
            DeliveryEntity(
                id = "del_demo_5",
                businessId = businessId,
                customerId = "cust_demo_6",
                customerName = "Kishan Yadav (Farmer)",
                customerType = "SUPPLIER",
                deliveryDate = todayMidnight,
                shift = "MORNING",
                milkType = "COW",
                quantityLiters = 40.0,
                ratePerLiter = 42.0,
                totalAmount = 1680.0,
                fat = 4.2,
                snf = 8.5,
                clr = 28.0,
                isDelivered = true,
                notes = "Collected Morning Shift",
                createdAt = now
            )
        )

        for (d in demoDeliveries) {
            db.deliveryDao().insertDelivery(d)
        }

        // Standard Inventory Items with real 0 stock as per specification
        val biz = db.businessDao().getBusinessById(businessId)
        initializeDefaultInventoryItems(businessId, biz?.businessMode)

        // Demo Expenses: None inserted. Per specification, no demo/fake expenses are allowed.

        // Demo Orders (Extra milk & paneer)
        val demoOrders = listOf(
            OrderEntity(
                id = "ord_demo_1",
                businessId = businessId,
                customerId = "cust_demo_1",
                customerName = "Ramesh Sharma",
                productName = "Fresh Paneer (Cottage Cheese)",
                quantity = 1.0,
                unit = "kg",
                rate = 360.0,
                totalAmount = 360.0,
                status = "PENDING",
                deliveryDate = todayMidnight,
                createdAt = now
            ),
            OrderEntity(
                id = "ord_demo_2",
                businessId = businessId,
                customerId = "cust_demo_2",
                customerName = "Anita Verma",
                productName = "Extra Buffalo Milk",
                quantity = 1.0,
                unit = "Liter",
                rate = 68.0,
                totalAmount = 68.0,
                status = "CONFIRMED",
                deliveryDate = todayMidnight,
                createdAt = now
            )
        )

        for (o in demoOrders) {
            db.orderDao().insertOrder(o)
        }

        // Demo Staff
        val demoStaff = StaffEntity(
            id = "STF-101",
            businessId = businessId,
            name = "Mukesh Kumar",
            mobile = "+91 98700 11223",
            pin = "1234",
            role = "STAFF",
            permissions = "canDeliver,canCollectPayment",
            assignedRoute = "Green Park",
            isActive = true,
            createdAt = now
        )
        db.staffDao().insertStaff(demoStaff)

        // Seed initial Farmers for Collection Center & Trader modes
        seedInitialFarmers(businessId)
    }

    // --- Farmers Management ---
    fun getFarmersFlow(businessId: String): Flow<List<FarmerEntity>> =
        db.farmerDao().getFarmersFlow(businessId)

    suspend fun getFarmers(businessId: String): List<FarmerEntity> =
        db.farmerDao().getFarmers(businessId)

    suspend fun getFarmerById(farmerId: String): FarmerEntity? =
        db.farmerDao().getFarmerById(farmerId)

    suspend fun saveFarmer(farmer: FarmerEntity) {
        db.farmerDao().insertFarmer(farmer)
        enqueueSync(
            businessId = farmer.businessId,
            collection = "farmers",
            docId = farmer.id,
            operation = "UPSERT",
            payload = farmerToJson(farmer)
        )
    }

    suspend fun deleteFarmer(businessId: String, farmerId: String) {
        db.farmerDao().softDeleteFarmer(farmerId)
        enqueueSync(
            businessId = businessId,
            collection = "farmers",
            docId = farmerId,
            operation = "DELETE",
            payload = "{}"
        )
    }

    suspend fun recordFarmerPayment(businessId: String, farmerId: String, amount: Double, reference: String) {
        // Reduce payable balance
        db.farmerDao().updateFarmerBalance(farmerId, -amount)
        db.milkCollectionDao().markAllFarmerCollectionsPaid(farmerId, reference)
    }

    // --- Milk Collection Desk (Farmer Procurement) ---
    fun getAllMilkCollectionsFlow(businessId: String): Flow<List<MilkCollectionEntity>> =
        db.milkCollectionDao().getAllMilkCollectionsFlow(businessId)

    fun getMilkCollectionsByDateFlow(businessId: String, dateEpoch: Long): Flow<List<MilkCollectionEntity>> =
        db.milkCollectionDao().getMilkCollectionsByDateFlow(businessId, dateEpoch)

    fun getMilkCollectionsByDateAndShiftFlow(businessId: String, dateEpoch: Long, shift: String): Flow<List<MilkCollectionEntity>> =
        db.milkCollectionDao().getMilkCollectionsByDateAndShiftFlow(businessId, dateEpoch, shift)

    fun getMilkCollectionsByFarmerFlow(farmerId: String): Flow<List<MilkCollectionEntity>> =
        db.milkCollectionDao().getMilkCollectionsByFarmerFlow(farmerId)

    suspend fun saveMilkCollection(collection: MilkCollectionEntity) {
        db.milkCollectionDao().insertMilkCollection(collection)
        // Add to farmer balance payable if pending
        if (collection.paymentStatus == "PENDING") {
            db.farmerDao().updateFarmerBalance(collection.farmerId, collection.totalAmount)
        }
        enqueueSync(
            businessId = collection.businessId,
            collection = "milk_collections",
            docId = collection.id,
            operation = "UPSERT",
            payload = collectionToJson(collection)
        )
    }

    suspend fun deleteMilkCollection(businessId: String, collectionId: String) {
        db.milkCollectionDao().softDeleteMilkCollection(collectionId)
        enqueueSync(
            businessId = businessId,
            collection = "milk_collections",
            docId = collectionId,
            operation = "DELETE",
            payload = "{}"
        )
    }

    // --- Bulk Dispatches (Wholesale & Tanker Supply) ---
    fun getAllBulkDispatchesFlow(businessId: String): Flow<List<BulkDispatchEntity>> =
        db.bulkDispatchDao().getAllDispatchesFlow(businessId)

    fun getBulkDispatchesByDateFlow(businessId: String, dateEpoch: Long): Flow<List<BulkDispatchEntity>> =
        db.bulkDispatchDao().getDispatchesByDateFlow(businessId, dateEpoch)

    suspend fun saveBulkDispatch(dispatch: BulkDispatchEntity) {
        db.bulkDispatchDao().insertDispatch(dispatch)
        enqueueSync(
            businessId = dispatch.businessId,
            collection = "bulk_dispatches",
            docId = dispatch.id,
            operation = "UPSERT",
            payload = dispatchToJson(dispatch)
        )
    }

    suspend fun deleteBulkDispatch(businessId: String, dispatchId: String) {
        db.bulkDispatchDao().softDeleteDispatch(dispatchId)
        enqueueSync(
            businessId = businessId,
            collection = "bulk_dispatches",
            docId = dispatchId,
            operation = "DELETE",
            payload = "{}"
        )
    }

    private suspend fun seedInitialFarmers(businessId: String) {
        val existing = db.farmerDao().getFarmers(businessId)
        if (existing.isEmpty()) {
            val now = System.currentTimeMillis()
            val sampleFarmers = listOf(
                FarmerEntity(
                    id = "FARM_101",
                    businessId = businessId,
                    farmerCode = "101",
                    name = "Suresh Patel (Kisan)",
                    mobile = "+91 98200 44101",
                    village = "Kalyanpur",
                    milkType = "COW",
                    defaultShift = "BOTH",
                    defaultQuantityEstimate = 8.5,
                    paymentMode = "UPI",
                    upiId = "sureshpatel@okhdfc",
                    balancePayable = 2150.0,
                    createdAt = now
                ),
                FarmerEntity(
                    id = "FARM_102",
                    businessId = businessId,
                    farmerCode = "102",
                    name = "Harish Choudhary",
                    mobile = "+91 98200 44102",
                    village = "Shivnagar",
                    milkType = "BUFFALO",
                    defaultShift = "BOTH",
                    defaultQuantityEstimate = 12.0,
                    paymentMode = "BANK_TRANSFER",
                    bankAccountNo = "918237461928",
                    ifscCode = "SBIN0001234",
                    balancePayable = 3840.0,
                    createdAt = now
                ),
                FarmerEntity(
                    id = "FARM_103",
                    businessId = businessId,
                    farmerCode = "103",
                    name = "Manoj Yadav",
                    mobile = "+91 98200 44103",
                    village = "Rampur",
                    milkType = "BOTH",
                    defaultShift = "MORNING",
                    defaultQuantityEstimate = 15.0,
                    paymentMode = "CASH",
                    balancePayable = 1420.0,
                    createdAt = now
                )
            )
            db.farmerDao().insertFarmers(sampleFarmers)

            // Also seed today's morning collections for initial experience
            val todayMidnight = getTodayMidnightEpoch()
            val sampleCollections = listOf(
                MilkCollectionEntity(
                    id = "COL_101_M",
                    businessId = businessId,
                    farmerId = "FARM_101",
                    farmerCode = "101",
                    farmerName = "Suresh Patel (Kisan)",
                    dateEpochMidnight = todayMidnight,
                    shift = "MORNING",
                    milkType = "COW",
                    quantityLiters = 9.0,
                    fat = 4.2,
                    snf = 8.6,
                    clr = 28.5,
                    ratePerLiter = 42.5,
                    totalAmount = 382.5,
                    paymentStatus = "PENDING"
                ),
                MilkCollectionEntity(
                    id = "COL_102_M",
                    businessId = businessId,
                    farmerId = "FARM_102",
                    farmerCode = "102",
                    farmerName = "Harish Choudhary",
                    dateEpochMidnight = todayMidnight,
                    shift = "MORNING",
                    milkType = "BUFFALO",
                    quantityLiters = 14.0,
                    fat = 6.8,
                    snf = 9.1,
                    clr = 30.0,
                    ratePerLiter = 58.0,
                    totalAmount = 812.0,
                    paymentStatus = "PENDING"
                )
            )
            db.milkCollectionDao().insertMilkCollections(sampleCollections)
        }
    }

    private fun getTodayMidnightEpoch(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    // --- Cattle & Breeding Management ---
    fun getCattleFlow(businessId: String): Flow<List<CattleEntity>> =
        db.cattleDao().getCattleFlow(businessId)

    suspend fun getCattleList(businessId: String): List<CattleEntity> =
        db.cattleDao().getCattleList(businessId)

    suspend fun saveCattle(cattle: CattleEntity) {
        db.cattleDao().insertCattle(cattle)
        enqueueSync(
            businessId = cattle.businessId,
            collection = "cattle_records",
            docId = cattle.id,
            operation = "UPSERT",
            payload = cattleToJson(cattle)
        )
    }

    suspend fun deleteCattle(businessId: String, cattleId: String) {
        db.cattleDao().softDeleteCattle(cattleId)
        enqueueSync(
            businessId = businessId,
            collection = "cattle_records",
            docId = cattleId,
            operation = "DELETE",
            payload = "{}"
        )
    }

    // --- Cattle Weight Records ---
    fun getAllWeightsFlow(businessId: String): Flow<List<CattleWeightEntity>> =
        db.cattleDao().getAllWeightsFlow(businessId)

    fun getWeightsForCattleFlow(cattleId: String): Flow<List<CattleWeightEntity>> =
        db.cattleDao().getWeightsForCattleFlow(cattleId)

    suspend fun saveWeight(weight: CattleWeightEntity) {
        db.cattleDao().insertWeight(weight)
        // Also update cattle current weight
        val cattle = db.cattleDao().getCattleById(weight.cattleId)
        if (cattle != null) {
            db.cattleDao().updateCattle(cattle.copy(currentWeightKg = weight.weightKg, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun deleteWeight(id: String) {
        db.cattleDao().deleteWeight(id)
    }

    // --- California Mastitis Test (CMT) Records ---
    fun getAllCmtFlow(businessId: String): Flow<List<CattleCmtEntity>> =
        db.cattleDao().getAllCmtFlow(businessId)

    fun getCmtForCattleFlow(cattleId: String): Flow<List<CattleCmtEntity>> =
        db.cattleDao().getCmtForCattleFlow(cattleId)

    suspend fun saveCmt(cmt: CattleCmtEntity) {
        db.cattleDao().insertCmt(cmt)
    }

    suspend fun deleteCmt(id: String) {
        db.cattleDao().deleteCmt(id)
    }

    // --- Body Condition Scoring (BCS) Records ---
    fun getAllBcsFlow(businessId: String): Flow<List<CattleBcsEntity>> =
        db.cattleDao().getAllBcsFlow(businessId)

    fun getBcsForCattleFlow(cattleId: String): Flow<List<CattleBcsEntity>> =
        db.cattleDao().getBcsForCattleFlow(cattleId)

    suspend fun saveBcs(bcs: CattleBcsEntity) {
        db.cattleDao().insertBcs(bcs)
        // Update cattle latest BCS
        val cattle = db.cattleDao().getCattleById(bcs.cattleId)
        if (cattle != null) {
            db.cattleDao().updateCattle(cattle.copy(latestBcs = bcs.score, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun deleteBcs(id: String) {
        db.cattleDao().deleteBcs(id)
    }

    // --- Breeding & Reproduction Records ---
    fun getAllBreedingFlow(businessId: String): Flow<List<BreedingRecordEntity>> =
        db.cattleDao().getAllBreedingFlow(businessId)

    fun getBreedingForCattleFlow(cattleId: String): Flow<List<BreedingRecordEntity>> =
        db.cattleDao().getBreedingForCattleFlow(cattleId)

    suspend fun saveBreeding(record: BreedingRecordEntity) {
        db.cattleDao().insertBreeding(record)
        // Dynamically update cattle's breeding status & expected calving date
        val cattle = db.cattleDao().getCattleById(record.cattleId)
        if (cattle != null) {
            val updated = when (record.eventType) {
                "INSEMINATION" -> cattle.copy(
                    breedingStatus = "INSEMINATED",
                    lastInseminationDate = record.date,
                    inseminationType = record.inseminationType,
                    bullIdOrSemenBrand = record.bullIdOrName,
                    expectedCalvingDate = if (record.expectedCalvingDate > 0) record.expectedCalvingDate else (record.date + (if (cattle.type == "BUFFALO") 310L else 283L) * 86400000L),
                    updatedAt = System.currentTimeMillis()
                )
                "PREGNANCY_DIAGNOSIS" -> {
                    if (record.pdStatus == "POSITIVE_PREGNANT") {
                        cattle.copy(
                            breedingStatus = "CONFIRMED_PREGNANT",
                            expectedCalvingDate = if (record.expectedCalvingDate > 0) record.expectedCalvingDate else (cattle.lastInseminationDate + (if (cattle.type == "BUFFALO") 310L else 283L) * 86400000L),
                            updatedAt = System.currentTimeMillis()
                        )
                    } else if (record.pdStatus == "NEGATIVE") {
                        cattle.copy(
                            breedingStatus = "OPEN",
                            expectedCalvingDate = 0L,
                            updatedAt = System.currentTimeMillis()
                        )
                    } else cattle
                }
                "CALVING" -> cattle.copy(
                    breedingStatus = "OPEN",
                    lactationStage = "LACTATING",
                    lactationCount = cattle.lactationCount + 1,
                    expectedCalvingDate = 0L,
                    updatedAt = System.currentTimeMillis()
                )
                "DRY_OFF" -> cattle.copy(
                    lactationStage = if (cattle.breedingStatus == "CONFIRMED_PREGNANT") "PREGNANT_DRY" else "DRY",
                    dailyYieldLiters = 0.0,
                    updatedAt = System.currentTimeMillis()
                )
                else -> cattle
            }
            db.cattleDao().updateCattle(updated)
        }
    }

    suspend fun deleteBreeding(id: String) {
        db.cattleDao().deleteBreeding(id)
    }

    // --- Deworming Records ---
    fun getAllDewormingFlow(businessId: String): Flow<List<DewormingRecordEntity>> =
        db.cattleDao().getAllDewormingFlow(businessId)

    fun getDewormingForCattleFlow(cattleId: String): Flow<List<DewormingRecordEntity>> =
        db.cattleDao().getDewormingForCattleFlow(cattleId)

    suspend fun saveDeworming(record: DewormingRecordEntity) {
        db.cattleDao().insertDeworming(record)
    }

    suspend fun deleteDeworming(id: String) {
        db.cattleDao().deleteDeworming(id)
    }

    // --- Vaccination Records ---
    fun getAllVaccinationFlow(businessId: String): Flow<List<VaccinationRecordEntity>> =
        db.cattleDao().getAllVaccinationFlow(businessId)

    fun getVaccinationForCattleFlow(cattleId: String): Flow<List<VaccinationRecordEntity>> =
        db.cattleDao().getVaccinationForCattleFlow(cattleId)

    suspend fun saveVaccination(record: VaccinationRecordEntity) {
        db.cattleDao().insertVaccination(record)
    }

    suspend fun deleteVaccination(id: String) {
        db.cattleDao().deleteVaccination(id)
    }

    // --- Treatment Records ---
    fun getAllTreatmentFlow(businessId: String): Flow<List<TreatmentRecordEntity>> =
        db.cattleDao().getAllTreatmentFlow(businessId)

    fun getTreatmentForCattleFlow(cattleId: String): Flow<List<TreatmentRecordEntity>> =
        db.cattleDao().getTreatmentForCattleFlow(cattleId)

    suspend fun saveTreatment(record: TreatmentRecordEntity) {
        db.cattleDao().insertTreatment(record)
    }

    suspend fun deleteTreatment(id: String) {
        db.cattleDao().deleteTreatment(id)
    }

    // --- Farm Observation Records ---
    fun getAllObservationsFlow(businessId: String): Flow<List<FarmObservationEntity>> =
        db.cattleDao().getAllObservationsFlow(businessId)

    suspend fun saveObservation(record: FarmObservationEntity) {
        db.cattleDao().insertObservation(record)
    }

    suspend fun deleteObservation(id: String) {
        db.cattleDao().deleteObservation(id)
    }

    // --- Milking Records ---
    fun getAllMilkingFlow(businessId: String): Flow<List<CattleMilkingRecordEntity>> =
        db.cattleDao().getAllMilkingFlow(businessId)

    fun getMilkingForCattleFlow(cattleId: String): Flow<List<CattleMilkingRecordEntity>> =
        db.cattleDao().getMilkingForCattleFlow(cattleId)

    suspend fun getMilkingForShift(businessId: String, dateEpochMidnight: Long, shift: String): List<CattleMilkingRecordEntity> =
        db.cattleDao().getMilkingForShift(businessId, dateEpochMidnight, shift)

    suspend fun saveMilking(record: CattleMilkingRecordEntity) {
        db.cattleDao().insertMilking(record)
        // Optionally update cattle's estimated daily yield
        val cattle = db.cattleDao().getCattleById(record.cattleId)
        if (cattle != null && record.quantityLiters > 0.0) {
            val updatedYield = if (record.shift == "MORNING") {
                record.quantityLiters * 2.1 // Morning usually ~50-55% of daily yield
            } else {
                record.quantityLiters * 2.2
            }
            db.cattleDao().updateCattle(cattle.copy(dailyYieldLiters = Math.round(updatedYield * 10.0) / 10.0, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun deleteMilking(id: String) {
        db.cattleDao().deleteMilking(id)
    }

    suspend fun seedInitialCattle(businessId: String, supportedMilkTypes: String = "BOTH") {
        val existing = db.cattleDao().getCattleList(businessId)
        if (existing.isEmpty()) {
            val now = System.currentTimeMillis()
            val dayMillis = 86400000L
            val allSample = mutableListOf<CattleEntity>()

            if (supportedMilkTypes == "COW_ONLY" || supportedMilkTypes == "BOTH") {
                allSample.add(
                    CattleEntity(
                        id = "CAT_101",
                        businessId = businessId,
                        tagNumber = "COW-101",
                        name = "Ganga (HF Cross)",
                        type = "COW",
                        breed = "Holstein Friesian",
                        lactationStage = "LACTATING",
                        dailyYieldLiters = 16.5,
                        breedingStatus = "CONFIRMED_PREGNANT",
                        lastInseminationDate = now - (150 * dayMillis),
                        inseminationType = "ARTIFICIAL_INSEMINATION",
                        bullIdOrSemenBrand = "ABS Sexed Semen Bull #402",
                        expectedCalvingDate = now + (130 * dayMillis),
                        lactationCount = 2,
                        birthDate = now - (1200 * dayMillis),
                        currentWeightKg = 485.0,
                        latestBcs = 3.25,
                        damTagOrName = "DAM-08 Ganga-M1",
                        sireTagOrName = "ABS-Titan-901",
                        healthNotes = "High yield, vaccinated, due in 4 months"
                    )
                )
                allSample.add(
                    CattleEntity(
                        id = "CAT_103",
                        businessId = businessId,
                        tagNumber = "COW-102",
                        name = "Nandi (Gir Indigenous)",
                        type = "COW",
                        breed = "Gir Desi",
                        lactationStage = "DRY",
                        dailyYieldLiters = 0.0,
                        breedingStatus = "CONFIRMED_PREGNANT",
                        lastInseminationDate = now - (250 * dayMillis),
                        inseminationType = "ARTIFICIAL_INSEMINATION",
                        bullIdOrSemenBrand = "Desi Gir Bull Royal",
                        expectedCalvingDate = now + (30 * dayMillis),
                        lactationCount = 4,
                        birthDate = now - (1800 * dayMillis),
                        currentWeightKg = 420.0,
                        latestBcs = 3.5,
                        damTagOrName = "Gir-Queen-12",
                        sireTagOrName = "GIR-MAHARAJA-01",
                        healthNotes = "Dry period, advanced pregnancy, calving due in 30 days"
                    )
                )
            }

            if (supportedMilkTypes == "BUFFALO_ONLY" || supportedMilkTypes == "BOTH") {
                allSample.add(
                    CattleEntity(
                        id = "CAT_102",
                        businessId = businessId,
                        tagNumber = "BUF-201",
                        name = "Kaveri (Murrah)",
                        type = "BUFFALO",
                        breed = "Murrah Buffalo",
                        lactationStage = "LACTATING",
                        dailyYieldLiters = 13.0,
                        breedingStatus = "INSEMINATED",
                        lastInseminationDate = now - (45 * dayMillis),
                        inseminationType = "ARTIFICIAL_INSEMINATION",
                        bullIdOrSemenBrand = "NDDB Murrah Bull #99",
                        expectedCalvingDate = now + (265 * dayMillis),
                        lactationCount = 3,
                        birthDate = now - (1500 * dayMillis),
                        currentWeightKg = 560.0,
                        latestBcs = 3.0,
                        damTagOrName = "BUF-D-45",
                        sireTagOrName = "NDDB-PRIDE-77",
                        healthNotes = "Pregnancy test checkup scheduled next week"
                    )
                )
            }

            db.cattleDao().insertCattleList(allSample)

            // Seed initial weights
            val sampleWeights = listOf(
                CattleWeightEntity(
                    id = "W_101_1",
                    businessId = businessId,
                    cattleId = "CAT_101",
                    tagNumber = "COW-101",
                    date = now - (30 * dayMillis),
                    stage = "ADULT",
                    weightKg = 472.0,
                    method = "GIRTH_CALCULATION",
                    heartGirthCm = 188.0,
                    bodyLengthCm = 148.0,
                    dailyGainKg = 0.45,
                    notes = "Monthly weigh-in using chest girth tape"
                ),
                CattleWeightEntity(
                    id = "W_101_2",
                    businessId = businessId,
                    cattleId = "CAT_101",
                    tagNumber = "COW-101",
                    date = now - (2 * dayMillis),
                    stage = "ADULT",
                    weightKg = 485.0,
                    method = "MANUAL",
                    dailyGainKg = 0.46,
                    notes = "Platform scale weigh-in before milking"
                ),
                CattleWeightEntity(
                    id = "W_102_1",
                    businessId = businessId,
                    cattleId = "CAT_102",
                    tagNumber = "BUF-201",
                    date = now - (15 * dayMillis),
                    stage = "ADULT",
                    weightKg = 560.0,
                    method = "GIRTH_CALCULATION",
                    heartGirthCm = 202.0,
                    bodyLengthCm = 155.0,
                    dailyGainKg = 0.38,
                    notes = "Calculated by heart girth & body length"
                )
            )
            db.cattleDao().insertWeightList(sampleWeights)

            // Seed initial CMT tests
            val sampleCmt = listOf(
                CattleCmtEntity(
                    id = "CMT_101_1",
                    businessId = businessId,
                    cattleId = "CAT_101",
                    tagNumber = "COW-101",
                    date = now - (5 * dayMillis),
                    testerName = "Ramesh Kumar (Farm Mgr)",
                    quarterLf = "N",
                    quarterRf = "N",
                    quarterLh = "T",
                    quarterRh = "N",
                    overallDiagnosis = "SUBCLINICAL_MASTITIS",
                    treatmentRecommendation = "Trace in Left Hind (LH). Apply herbal udder spray & iodine teat dip. Re-test in 3 days.",
                    milkWithheld = false,
                    notes = "Morning milking paddle test"
                ),
                CattleCmtEntity(
                    id = "CMT_102_1",
                    businessId = businessId,
                    cattleId = "CAT_102",
                    tagNumber = "BUF-201",
                    date = now - (1 * dayMillis),
                    testerName = "Dr. Suresh (Vet)",
                    quarterLf = "N",
                    quarterRf = "N",
                    quarterLh = "N",
                    quarterRh = "N",
                    overallDiagnosis = "NORMAL",
                    treatmentRecommendation = "All 4 quarters clear. Good udder hygiene.",
                    milkWithheld = false,
                    notes = "Routine weekly screening"
                )
            )
            db.cattleDao().insertCmtList(sampleCmt)

            // Seed initial BCS
            val sampleBcs = listOf(
                CattleBcsEntity(
                    id = "BCS_101_1",
                    businessId = businessId,
                    cattleId = "CAT_101",
                    tagNumber = "COW-101",
                    date = now - (7 * dayMillis),
                    score = 3.25,
                    category = "IDEAL",
                    spineAssessment = "Rounded spine, vertebrae felt with mild pressure",
                    ribsAssessment = "Short ribs well covered, gentle wave feel",
                    hooksAndPins = "Shallow V to U shape with adequate fat layer",
                    nutritionAdvice = "Excellent condition for mid lactation. Maintain 4kg concentrate + 25kg green maize.",
                    notes = "Optimal body condition"
                ),
                CattleBcsEntity(
                    id = "BCS_102_1",
                    businessId = businessId,
                    cattleId = "CAT_102",
                    tagNumber = "BUF-201",
                    date = now - (10 * dayMillis),
                    score = 3.0,
                    category = "IDEAL",
                    spineAssessment = "Flat backline with good muscle tone",
                    ribsAssessment = "Moderate fat cover over last 3 ribs",
                    hooksAndPins = "Smooth contours around hip and pin bones",
                    nutritionAdvice = "Sustain mineral mixture 50g/day and cottonseed cake.",
                    notes = "Checked after A.I. insemination"
                )
            )
            db.cattleDao().insertBcsList(sampleBcs)

            // Seed initial Breeding Records
            val sampleBreeding = listOf(
                BreedingRecordEntity(
                    id = "BR_101_AI",
                    businessId = businessId,
                    cattleId = "CAT_101",
                    cattleTag = "COW-101",
                    eventType = "INSEMINATION",
                    date = now - (150 * dayMillis),
                    inseminationType = "ARTIFICIAL",
                    bullIdOrName = "ABS Sexed Semen Bull #402 (HF 90% Female)",
                    semenStrawsUsed = 1,
                    doneBy = "Dr. R. K. Sharma (AI Tech)",
                    pdStatus = "POSITIVE_PREGNANT",
                    expectedCalvingDate = now + (133 * dayMillis),
                    notes = "Clear standing heat observed at 06:00 AM, inseminated at 04:30 PM (AM-PM rule)."
                ),
                BreedingRecordEntity(
                    id = "BR_101_PD",
                    businessId = businessId,
                    cattleId = "CAT_101",
                    cattleTag = "COW-101",
                    eventType = "PREGNANCY_DIAGNOSIS",
                    date = now - (60 * dayMillis),
                    doneBy = "Dr. Suresh Verma (Govt Vet Hospital)",
                    pdStatus = "POSITIVE_PREGNANT",
                    expectedCalvingDate = now + (133 * dayMillis),
                    notes = "Rectal palpation confirmed 90-day gravid right uterine horn. Fetal membrane slip felt."
                ),
                BreedingRecordEntity(
                    id = "BR_201_AI",
                    businessId = businessId,
                    cattleId = "CAT_102",
                    cattleTag = "BUF-201",
                    eventType = "INSEMINATION",
                    date = now - (45 * dayMillis),
                    inseminationType = "ARTIFICIAL",
                    bullIdOrName = "Murrah Elite Bull 'Sultan-Line #18'",
                    semenStrawsUsed = 1,
                    doneBy = "Dr. Suresh Verma",
                    pdStatus = "PENDING",
                    expectedCalvingDate = now + (265 * dayMillis),
                    notes = "Silent heat detected via bellowing & clear cervical discharge. PD due in 45 days."
                )
            )
            db.cattleDao().insertBreedingList(sampleBreeding)

            // Seed initial Deworming Records
            val sampleDeworming = listOf(
                DewormingRecordEntity(
                    id = "DEW_101_1",
                    businessId = businessId,
                    cattleId = "CAT_101",
                    cattleTag = "COW-101",
                    dewormerSalt = "Albendazole Oral Suspension",
                    dose = "100 ml (3000 mg)",
                    date = now - (35 * dayMillis),
                    repeatAfterDays = 90,
                    nextDueDate = now + (55 * dayMillis),
                    administeredBy = "Self / Farm Manager",
                    cost = 140.0,
                    remarks = "Monsoon deworming course before peak rainy season."
                ),
                DewormingRecordEntity(
                    id = "DEW_201_1",
                    businessId = businessId,
                    cattleId = "CAT_102",
                    cattleTag = "BUF-201",
                    dewormerSalt = "Ivermectin 1% Injection + Oxyclozanide",
                    dose = "10 ml S/C + 90 ml drench",
                    date = now - (40 * dayMillis),
                    repeatAfterDays = 90,
                    nextDueDate = now + (50 * dayMillis),
                    administeredBy = "Dr. R. K. Sharma",
                    cost = 220.0,
                    remarks = "Liver fluke & ectoparasite prevention."
                )
            )
            db.cattleDao().insertDewormingList(sampleDeworming)

            // Seed initial Vaccination Records
            val sampleVaccinations = listOf(
                VaccinationRecordEntity(
                    id = "VAC_101_FMD",
                    businessId = businessId,
                    cattleId = "CAT_101",
                    cattleTag = "COW-101",
                    vaccineName = "FMD (Foot & Mouth Disease) Raksha-Ovac",
                    manufacturer = "Indian Immunologicals Ltd",
                    batchNo = "RO-8842",
                    date = now - (45 * dayMillis),
                    nextDueDate = now + (135 * dayMillis),
                    cost = 0.0,
                    vaccinatedBy = "National Animal Disease Control Programme (NADCP)",
                    proofAttachment = "Ear Tag Verified"
                ),
                VaccinationRecordEntity(
                    id = "VAC_201_HSBQ",
                    businessId = businessId,
                    cattleId = "CAT_102",
                    cattleTag = "BUF-201",
                    vaccineName = "HS + BQ Combined (Raksha-Biovac)",
                    manufacturer = "Indian Immunologicals Ltd",
                    batchNo = "HB-7320",
                    date = now - (60 * dayMillis),
                    nextDueDate = now + (305 * dayMillis),
                    cost = 25.0,
                    vaccinatedBy = "Veterinary Assistant / Doctor",
                    proofAttachment = "Govt Vaccination Drive"
                )
            )
            db.cattleDao().insertVaccinationList(sampleVaccinations)

            // Seed initial Treatment Records
            val sampleTreatments = listOf(
                TreatmentRecordEntity(
                    id = "TR_101_1",
                    businessId = businessId,
                    cattleId = "CAT_101",
                    cattleTag = "COW-101",
                    diseaseName = "Subclinical Mastitis (Right Hind)",
                    symptoms = "Mild flakes in first strip cup, CMT score +1, slight teat swelling",
                    medicationName = "Intramammary Infusion (Cefquinome) + Meloxicam 15ml",
                    dosage = "1 syringe per day for 3 days",
                    timing = "After Evening Milking",
                    durationDays = 3,
                    checkupDate = now - (15 * dayMillis),
                    treatmentCost = 480.0,
                    performedBy = "Dr. Suresh Verma",
                    followUpDate = now - (10 * dayMillis),
                    milkWithdrawalDays = 4,
                    notes = "CMT retested negative on Day 5. Milk discarded during withdrawal period."
                )
            )
            db.cattleDao().insertTreatmentList(sampleTreatments)

            // Seed initial Farm Observations
            val sampleObservations = listOf(
                FarmObservationEntity(
                    id = "OBS_101_1",
                    businessId = businessId,
                    cattleId = "CAT_101",
                    cattleTag = "COW-101",
                    activityType = "RUMINATION",
                    date = now - (1 * dayMillis),
                    description = "Excellent cud chewing (>55 chews/bolus). Fresh silage intake normal.",
                    hasAlert = false,
                    loggedBy = "Farm Manager"
                ),
                FarmObservationEntity(
                    id = "OBS_201_1",
                    businessId = businessId,
                    cattleId = "CAT_102",
                    cattleTag = "BUF-201",
                    activityType = "HEAT_SIGNS",
                    date = now - (46 * dayMillis),
                    description = "Clear mucosal string discharge observed during morning wash. Tail raising & mounting behavior.",
                    hasAlert = true,
                    loggedBy = "Morning Shift Helper"
                )
            )
            db.cattleDao().insertObservationList(sampleObservations)

            // Seed initial Cattle Milking Records
            val sampleMilking = listOf(
                CattleMilkingRecordEntity(
                    id = "MILK_101_M",
                    businessId = businessId,
                    cattleId = "CAT_101",
                    cattleTag = "COW-101",
                    dateEpochMidnight = now - (now % dayMillis),
                    shift = "MORNING",
                    quantityLiters = 9.2,
                    fat = 4.3,
                    snf = 8.6,
                    recordedBy = "Farm Staff"
                ),
                CattleMilkingRecordEntity(
                    id = "MILK_101_E",
                    businessId = businessId,
                    cattleId = "CAT_101",
                    cattleTag = "COW-101",
                    dateEpochMidnight = now - (now % dayMillis),
                    shift = "EVENING",
                    quantityLiters = 7.3,
                    fat = 4.4,
                    snf = 8.6,
                    recordedBy = "Farm Staff"
                ),
                CattleMilkingRecordEntity(
                    id = "MILK_201_M",
                    businessId = businessId,
                    cattleId = "CAT_102",
                    cattleTag = "BUF-201",
                    dateEpochMidnight = now - (now % dayMillis),
                    shift = "MORNING",
                    quantityLiters = 6.8,
                    fat = 7.2,
                    snf = 9.2,
                    recordedBy = "Farm Staff"
                ),
                CattleMilkingRecordEntity(
                    id = "MILK_201_E",
                    businessId = businessId,
                    cattleId = "CAT_102",
                    cattleTag = "BUF-201",
                    dateEpochMidnight = now - (now % dayMillis),
                    shift = "EVENING",
                    quantityLiters = 5.2,
                    fat = 7.4,
                    snf = 9.1,
                    recordedBy = "Farm Staff"
                )
            )
            db.cattleDao().insertMilkingList(sampleMilking)
        }
    }

    private fun cattleToJson(c: CattleEntity): String = JSONObject().apply {
        put("id", c.id)
        put("businessId", c.businessId)
        put("tagNumber", c.tagNumber)
        put("name", c.name)
        put("type", c.type)
        put("breed", c.breed)
        put("lactationStage", c.lactationStage)
        put("dailyYieldLiters", c.dailyYieldLiters)
        put("breedingStatus", c.breedingStatus)
        put("expectedCalvingDate", c.expectedCalvingDate)
    }.toString()

    private fun farmerToJson(f: FarmerEntity): String = JSONObject().apply {
        put("id", f.id)
        put("businessId", f.businessId)
        put("farmerCode", f.farmerCode)
        put("name", f.name)
        put("mobile", f.mobile)
        put("village", f.village)
        put("milkType", f.milkType)
        put("defaultShift", f.defaultShift)
        put("balancePayable", f.balancePayable)
        put("status", f.status)
    }.toString()

    private fun collectionToJson(c: MilkCollectionEntity): String = JSONObject().apply {
        put("id", c.id)
        put("businessId", c.businessId)
        put("farmerId", c.farmerId)
        put("farmerCode", c.farmerCode)
        put("farmerName", c.farmerName)
        put("dateEpochMidnight", c.dateEpochMidnight)
        put("shift", c.shift)
        put("milkType", c.milkType)
        put("quantityLiters", c.quantityLiters)
        put("fat", c.fat)
        put("snf", c.snf)
        put("ratePerLiter", c.ratePerLiter)
        put("totalAmount", c.totalAmount)
        put("paymentStatus", c.paymentStatus)
    }.toString()

    private fun dispatchToJson(d: BulkDispatchEntity): String = JSONObject().apply {
        put("id", d.id)
        put("businessId", d.businessId)
        put("buyerOrPlantName", d.buyerOrPlantName)
        put("dateEpochMidnight", d.dateEpochMidnight)
        put("shift", d.shift)
        put("totalLiters", d.totalLiters)
        put("ratePerLiter", d.ratePerLiter)
        put("totalAmount", d.totalAmount)
        put("paymentStatus", d.paymentStatus)
    }.toString()

    // ================= MILK WASTAGE & DAILY RECONCILIATION =================
    fun getMilkWastageFlow(businessId: String): Flow<List<MilkWastageEntity>> =
        db.milkWastageDao().getAllWastage(businessId)

    suspend fun getMilkWastageBetweenDates(businessId: String, startDate: Long, endDate: Long): List<MilkWastageEntity> =
        db.milkWastageDao().getWastageBetweenDates(businessId, startDate, endDate)

    suspend fun saveMilkWastage(wastage: MilkWastageEntity) {
        db.milkWastageDao().insertWastage(wastage)
    }

    suspend fun deleteMilkWastage(businessId: String, id: String) {
        db.milkWastageDao().deleteById(businessId, id)
    }

    suspend fun getDailyMilkReconciliation(businessId: String, targetDate: Long): MilkReconciliationSummary {
        val cal = Calendar.getInstance().apply {
            timeInMillis = targetDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis

        val endCal = Calendar.getInstance().apply {
            timeInMillis = targetDate
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val endOfDay = endCal.timeInMillis

        // 1. Farm Milking Production Inflow
        val farmMilkingList = db.cattleDao().getMilkingBetweenDates(businessId, startOfDay, endOfDay)
        val farmMilkingLiters = Math.round(farmMilkingList.sumOf { it.quantityLiters } * 100.0) / 100.0

        // 2. Village Farmer Milk Collection Inflow
        val farmerCollections = db.milkCollectionDao().getMilkCollectionsBetweenDates(businessId, startOfDay, endOfDay)
        val farmerCollectionLiters = Math.round(farmerCollections.sumOf { it.quantityLiters } * 100.0) / 100.0

        // 3. Deliveries Outflow (Customer doorstep routes / wholesale supply)
        val deliveries = db.deliveryDao().getDeliveriesBetweenDates(businessId, startOfDay, endOfDay)
        val customerDeliveredLiters = Math.round(deliveries.filter { it.isDelivered && it.customerType != "SUPPLIER" }.sumOf { it.quantityLiters } * 100.0) / 100.0
        val bulkInwardLiters = Math.round(deliveries.filter { it.isDelivered && it.customerType == "SUPPLIER" }.sumOf { it.quantityLiters } * 100.0) / 100.0

        // 4. Bulk Tanker Dispatches Outflow
        val dispatches = db.bulkDispatchDao().getDispatchesBetweenDates(businessId, startOfDay, endOfDay)
        val bulkDispatchedLiters = Math.round(dispatches.sumOf { it.totalLiters } * 100.0) / 100.0

        // 5. Retail Counter Orders Outflow
        val orders = db.orderDao().getOrdersBetweenDates(businessId, startOfDay, endOfDay)
        val retailOrderLiters = Math.round(orders.filter { it.status != "CANCELLED" && (it.productName.contains("Milk", ignoreCase = true) || it.productName.contains("दूध") || it.unit == "L") }.sumOf { it.quantity } * 100.0) / 100.0

        // 6. Logged Wastage & Spoilage Incidents
        val wastageList = db.milkWastageDao().getWastageBetweenDates(businessId, startOfDay, endOfDay)
        val loggedWastageLiters = Math.round(wastageList.sumOf { it.quantityLiters } * 100.0) / 100.0
        val loggedWastageLossAmount = Math.round(wastageList.sumOf { it.totalLossAmount } * 100.0) / 100.0

        val totalInflow = Math.round((farmMilkingLiters + farmerCollectionLiters + bulkInwardLiters) * 100.0) / 100.0
        val totalOutflow = Math.round((customerDeliveredLiters + bulkDispatchedLiters + retailOrderLiters) * 100.0) / 100.0
        val variance = Math.round((totalInflow - (totalOutflow + loggedWastageLiters)) * 100.0) / 100.0
        val wastagePercent = if (totalInflow > 0.0) Math.round((loggedWastageLiters / totalInflow * 100.0) * 10.0) / 10.0 else 0.0

        return MilkReconciliationSummary(
            date = startOfDay,
            farmMilkingLiters = farmMilkingLiters,
            farmerCollectionLiters = farmerCollectionLiters,
            bulkInwardLiters = bulkInwardLiters,
            totalInflowLiters = totalInflow,
            customerDeliveredLiters = customerDeliveredLiters,
            bulkDispatchedLiters = bulkDispatchedLiters,
            retailOrderLiters = retailOrderLiters,
            totalOutflowLiters = totalOutflow,
            loggedWastageLiters = loggedWastageLiters,
            loggedWastageLossAmount = loggedWastageLossAmount,
            varianceLiters = variance,
            wastagePercentage = wastagePercent,
            wastageList = wastageList
        )
    }
}

data class MilkReconciliationSummary(
    val date: Long,
    val farmMilkingLiters: Double = 0.0,
    val farmerCollectionLiters: Double = 0.0,
    val bulkInwardLiters: Double = 0.0,
    val totalInflowLiters: Double = farmMilkingLiters + farmerCollectionLiters + bulkInwardLiters,
    val customerDeliveredLiters: Double = 0.0,
    val bulkDispatchedLiters: Double = 0.0,
    val retailOrderLiters: Double = 0.0,
    val totalOutflowLiters: Double = customerDeliveredLiters + bulkDispatchedLiters + retailOrderLiters,
    val loggedWastageLiters: Double = 0.0,
    val loggedWastageLossAmount: Double = 0.0,
    val varianceLiters: Double = totalInflowLiters - (totalOutflowLiters + loggedWastageLiters),
    val wastagePercentage: Double = if (totalInflowLiters > 0.0) Math.round((loggedWastageLiters / totalInflowLiters * 100.0) * 10.0) / 10.0 else 0.0,
    val wastageList: List<MilkWastageEntity> = emptyList()
)

