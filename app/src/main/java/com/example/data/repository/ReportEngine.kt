package com.example.data.repository

import com.example.data.local.dao.*
import com.example.data.local.entity.DeliveryEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.PaymentEntity
import java.util.Calendar

data class ShiftSummary(
    val morningLiters: Double = 0.0,
    val morningAmount: Double = 0.0,
    val eveningLiters: Double = 0.0,
    val eveningAmount: Double = 0.0,
    val totalLiters: Double = 0.0,
    val totalAmount: Double = 0.0
)

data class MilkBalanceSummary(
    val milkInCollectedLiters: Double = 0.0,
    val milkInCost: Double = 0.0,
    val milkOutDispatchedLiters: Double = 0.0,
    val milkOutRevenue: Double = 0.0,
    val netLitersBalance: Double = 0.0 // In - Out
)

data class ProfitReport(
    val startDate: Long,
    val endDate: Long,
    val milkSalesRevenue: Double,
    val farmMilkProductionValue: Double = 0.0,
    val otherSalesRevenue: Double = 0.0,
    val totalRevenue: Double,
    val milkPurchaseCost: Double, // Milk IN collected from suppliers / farmers
    val inventoryPurchaseCost: Double = 0.0, // Feed & item purchases
    val actualFeedConsumptionCost: Double = 0.0, // Value of feed actually consumed by herd in this period
    val milkWastageCost: Double = 0.0, // Value of lost / spoiled milk
    val milkWastageLiters: Double = 0.0, // Volume of lost / spoiled milk
    val feedCost: Double = 0.0, // Specific cattle feed & fodder expenses
    val veterinaryCost: Double = 0.0, // Medical, deworming, vaccination, doctor
    val breedingCost: Double = 0.0, // A.I., semen straw charges
    val laborCost: Double = 0.0, // Farm labor / milker wages
    val totalPurchaseCost: Double = milkPurchaseCost + inventoryPurchaseCost,
    val operatingExpenses: Double, // General farm operational expenses
    val personalExpenses: Double = 0.0, // Personal drawings kept separate
    val totalCost: Double,
    val base1Profit: Double = totalRevenue - (milkPurchaseCost + inventoryPurchaseCost), // Sales - Purchases
    val base2Profit: Double = totalRevenue - (milkPurchaseCost + inventoryPurchaseCost) - operatingExpenses, // Sales - Purchases - Business Expenses
    val base3Profit: Double = 0.0, // Collections - Expenses
    val actualNetOperatingProfit: Double = totalRevenue - milkPurchaseCost - actualFeedConsumptionCost - operatingExpenses - milkWastageCost, // Actual Sales - Actual Milk Intake - Actual Feed Consumption - Operating Costs - Wastage Loss
    val incomeOverFeedCost: Double = totalRevenue - feedCost, // Revenue minus Feed Cost (IOFC)
    val totalMilkLiters: Double = 0.0,
    val costPerLiter: Double = 0.0,
    val revenuePerLiter: Double = 0.0,
    val profitPerLiter: Double = 0.0,
    val feedCostPerLiter: Double = 0.0,
    val netProfit: Double,
    val profitMarginPercent: Double,
    val expenseBreakdown: Map<String, Double>
)

data class CustomerStatement(
    val customerId: String,
    val customerName: String,
    val totalDeliveredLiters: Double,
    val totalBilledAmount: Double,
    val totalPaidAmount: Double,
    val currentBalance: Double,
    val deliveries: List<DeliveryEntity>,
    val payments: List<PaymentEntity>
)

class ReportEngine(
    private val deliveryDao: DeliveryDao,
    private val paymentDao: PaymentDao,
    private val expenseDao: ExpenseDao,
    private val customerDao: CustomerDao,
    private val inventoryDao: InventoryDao,
    private val milkWastageDao: MilkWastageDao? = null
) {

    suspend fun calculateShiftSummary(businessId: String, date: Long): ShiftSummary {
        val morningList = deliveryDao.getDeliveriesByDateAndShift(businessId, date, "MORNING")
        val eveningList = deliveryDao.getDeliveriesByDateAndShift(businessId, date, "EVENING")

        val morningLiters = morningList.filter { it.isDelivered && it.customerType != "SUPPLIER" }.sumOf { it.quantityLiters }
        val morningAmount = morningList.filter { it.isDelivered && it.customerType != "SUPPLIER" }.sumOf { it.totalAmount }

        val eveningLiters = eveningList.filter { it.isDelivered && it.customerType != "SUPPLIER" }.sumOf { it.quantityLiters }
        val eveningAmount = eveningList.filter { it.isDelivered && it.customerType != "SUPPLIER" }.sumOf { it.totalAmount }

        return ShiftSummary(
            morningLiters = Math.round(morningLiters * 100.0) / 100.0,
            morningAmount = Math.round(morningAmount * 100.0) / 100.0,
            eveningLiters = Math.round(eveningLiters * 100.0) / 100.0,
            eveningAmount = Math.round(eveningAmount * 100.0) / 100.0,
            totalLiters = Math.round((morningLiters + eveningLiters) * 100.0) / 100.0,
            totalAmount = Math.round((morningAmount + eveningAmount) * 100.0) / 100.0
        )
    }

    suspend fun calculateProfitReport(businessId: String, startDate: Long, endDate: Long, accountStartDate: Long): ProfitReport {
        // Enforce account start date boundary
        val effectiveStart = maxOf(startDate, accountStartDate)

        val deliveries = deliveryDao.getDeliveriesBetweenDates(businessId, effectiveStart, endDate)
        val allExpenses = expenseDao.getExpensesBetweenDates(businessId, effectiveStart, endDate)
        val payments = paymentDao.getPaymentsBetweenDates(businessId, effectiveStart, endDate)
        val inventoryTx = inventoryDao.getTransactionsBetweenDates(businessId, effectiveStart, endDate)
        val wastageList = milkWastageDao?.getWastageBetweenDates(businessId, effectiveStart, endDate) ?: emptyList()

        // Milk OUT (Actual Sales to Individual, Restaurant & Bulk Buyers)
        val salesDeliveries = deliveries.filter { it.isDelivered && it.customerType != "SUPPLIER" }
        val milkSalesRevenue = Math.round(salesDeliveries.sumOf { it.totalAmount } * 100.0) / 100.0

        // Milk IN (Purchases from Suppliers / Farmers)
        val supplierDeliveries = deliveries.filter { it.isDelivered && it.customerType == "SUPPLIER" }
        val milkPurchaseCost = Math.round(supplierDeliveries.sumOf { it.totalAmount } * 100.0) / 100.0

        // Inventory Purchases (Cash Outflow for stocking feed & supplies)
        val inventoryPurchases = allExpenses.filter { it.isInventoryPurchase || it.category == "INVENTORY_PURCHASE" }
        val inventoryPurchaseCost = Math.round(inventoryPurchases.sumOf { it.amount } * 100.0) / 100.0
        val totalPurchaseCost = Math.round((milkPurchaseCost + inventoryPurchaseCost) * 100.0) / 100.0

        // Category-specific cost aggregations for Dairy Farming
        val feedExpenseCost = Math.round(allExpenses.filter { 
            it.isBusiness() && (
                valCat(it.category).contains("FEED") || valCat(it.category).contains("FODDER") || 
                valCat(it.category).contains("BHUSA") || valCat(it.category).contains("SILAGE") ||
                valCat(it.category).contains("KHAL") || valCat(it.category).contains("CHURI") || 
                valCat(it.category).contains("MINERAL") || valCat(it.category).contains("CHOKAR") ||
                valCat(it.category).contains("CHOKKAR") || (it.isInventoryPurchase && com.example.ui.util.BusinessModeFeatures.isFeedItem(it.notes.ifBlank { it.category }))
            )
        }.sumOf { it.amount } * 100.0) / 100.0

        // Actual Feed & Consumables Consumption (from logged daily herd feeding transactions)
        val feedConsumptionTx = inventoryTx.filter { 
            it.transactionType == "CONSUMPTION" && com.example.ui.util.BusinessModeFeatures.isFeedItem(it.itemName) 
        }
        val actualFeedConsumptionCost = if (feedConsumptionTx.isNotEmpty()) {
            Math.round(feedConsumptionTx.sumOf { it.totalAmount } * 100.0) / 100.0
        } else {
            feedExpenseCost
        }

        // Milk Wastage Financial Loss & Volume
        val milkWastageCost = Math.round(wastageList.sumOf { it.totalLossAmount } * 100.0) / 100.0
        val milkWastageLiters = Math.round(wastageList.sumOf { it.quantityLiters } * 100.0) / 100.0

        // Ordinary Business Expenses (EXCLUDES inventory purchases to prevent double-counting, and EXCLUDES personal expenses)
        val businessExpenses = allExpenses.filter { it.isBusiness() && !it.isInventoryPurchase && it.category != "INVENTORY_PURCHASE" }
        val expenseMap = mutableMapOf<String, Double>()
        for (expense in businessExpenses) {
            val cat = expense.category.ifBlank { "OTHER" }
            expenseMap[cat] = (expenseMap[cat] ?: 0.0) + expense.amount
        }
        val totalOperatingExpenses = Math.round(businessExpenses.sumOf { it.amount } * 100.0) / 100.0

        // Personal Expenses (Separated)
        val personalExpenses = allExpenses.filter { it.isPersonal() }
        val totalPersonalExpenses = Math.round(personalExpenses.sumOf { it.amount } * 100.0) / 100.0

        // Collections (Cash/UPI collected from customers)
        val totalCollections = Math.round(payments.filter { it.paymentType == "CUSTOMER_PAYMENT" }.sumOf { it.amount } * 100.0) / 100.0
        val totalExpensesCashOutflow = Math.round((totalPurchaseCost + totalOperatingExpenses + totalPersonalExpenses) * 100.0) / 100.0

        val totalRevenue = milkSalesRevenue
        val totalCost = Math.round((totalPurchaseCost + totalOperatingExpenses) * 100.0) / 100.0

        // 3 Intended Profit Bases per specification:
        // Base 1: Sales - Purchases
        val base1 = Math.round((totalRevenue - totalPurchaseCost) * 100.0) / 100.0
        // Base 2: Sales - Purchases - Business Expenses
        val base2 = Math.round((totalRevenue - totalPurchaseCost - totalOperatingExpenses) * 100.0) / 100.0
        // Base 3: Collections - Expenses
        val base3 = Math.round((totalCollections - totalExpensesCashOutflow) * 100.0) / 100.0

        // True Net Operating Profit = Actual Sales Revenue - Actual Milk Procurement - Actual Feed Consumed - Operating Expenses - Milk Wastage Loss
        val actualNetOperatingProfit = Math.round((totalRevenue - milkPurchaseCost - actualFeedConsumptionCost - totalOperatingExpenses - milkWastageCost) * 100.0) / 100.0

        val netProfit = actualNetOperatingProfit
        val margin = if (totalRevenue > 0) Math.round((netProfit / totalRevenue * 100.0) * 10.0) / 10.0 else 0.0

        val veterinaryCost = Math.round(businessExpenses.filter { 
            val cat = it.category.uppercase()
            cat.contains("VET") || cat.contains("MEDICINE") || cat.contains("DOCTOR") || cat.contains("TREATMENT") || cat.contains("VACCIN") || cat.contains("DEWORM")
        }.sumOf { it.amount } * 100.0) / 100.0

        val breedingCost = Math.round(businessExpenses.filter { 
            val cat = it.category.uppercase()
            cat.contains("BREED") || cat.contains("INSEMIN") || cat.contains("AI_") || cat.contains("SEMEN") || cat.contains("STRAW")
        }.sumOf { it.amount } * 100.0) / 100.0

        val laborCost = Math.round(businessExpenses.filter { 
            val cat = it.category.uppercase()
            cat.contains("LABOR") || cat.contains("LABOUR") || cat.contains("SALARY") || cat.contains("WAGE") || cat.contains("MILKER")
        }.sumOf { it.amount } * 100.0) / 100.0

        val totalMilkLiters = Math.round(salesDeliveries.sumOf { it.quantityLiters } * 100.0) / 100.0
        val costPerLiter = if (totalMilkLiters > 0) Math.round((totalCost / totalMilkLiters) * 100.0) / 100.0 else 0.0
        val revenuePerLiter = if (totalMilkLiters > 0) Math.round((totalRevenue / totalMilkLiters) * 100.0) / 100.0 else 0.0
        val profitPerLiter = if (totalMilkLiters > 0) Math.round((netProfit / totalMilkLiters) * 100.0) / 100.0 else 0.0
        val feedCostPerLiter = if (totalMilkLiters > 0) Math.round((actualFeedConsumptionCost / totalMilkLiters) * 100.0) / 100.0 else 0.0
        val incomeOverFeed = Math.round((totalRevenue - actualFeedConsumptionCost) * 100.0) / 100.0

        return ProfitReport(
            startDate = effectiveStart,
            endDate = endDate,
            milkSalesRevenue = milkSalesRevenue,
            farmMilkProductionValue = milkSalesRevenue,
            otherSalesRevenue = 0.0,
            totalRevenue = totalRevenue,
            milkPurchaseCost = milkPurchaseCost,
            inventoryPurchaseCost = inventoryPurchaseCost,
            actualFeedConsumptionCost = actualFeedConsumptionCost,
            milkWastageCost = milkWastageCost,
            milkWastageLiters = milkWastageLiters,
            feedCost = actualFeedConsumptionCost,
            veterinaryCost = veterinaryCost,
            breedingCost = breedingCost,
            laborCost = laborCost,
            totalPurchaseCost = totalPurchaseCost,
            operatingExpenses = totalOperatingExpenses,
            personalExpenses = totalPersonalExpenses,
            totalCost = totalCost,
            base1Profit = base1,
            base2Profit = base2,
            base3Profit = base3,
            actualNetOperatingProfit = actualNetOperatingProfit,
            incomeOverFeedCost = incomeOverFeed,
            totalMilkLiters = totalMilkLiters,
            costPerLiter = costPerLiter,
            revenuePerLiter = revenuePerLiter,
            profitPerLiter = profitPerLiter,
            feedCostPerLiter = feedCostPerLiter,
            netProfit = netProfit,
            profitMarginPercent = margin,
            expenseBreakdown = expenseMap
        )
    }

    private fun valCat(cat: String): String = cat.uppercase()

    suspend fun getCustomerStatement(businessId: String, customerId: String): CustomerStatement? {
        val customer = customerDao.getCustomerById(businessId, customerId) ?: return null
        val deliveries = deliveryDao.getDeliveriesBetweenDates(businessId, 0L, Long.MAX_VALUE)
            .filter { it.customerId == customerId && it.isDelivered }
        val payments = paymentDao.getPaymentsBetweenDates(businessId, 0L, Long.MAX_VALUE)
            .filter { it.customerId == customerId }

        val totalLiters = deliveries.sumOf { it.quantityLiters }
        val totalBilled = deliveries.sumOf { it.totalAmount }
        val totalPaid = payments.sumOf { it.amount }

        return CustomerStatement(
            customerId = customerId,
            customerName = customer.name,
            totalDeliveredLiters = Math.round(totalLiters * 100.0) / 100.0,
            totalBilledAmount = Math.round(totalBilled * 100.0) / 100.0,
            totalPaidAmount = Math.round(totalPaid * 100.0) / 100.0,
            currentBalance = customer.outstandingBalance,
            deliveries = deliveries.sortedByDescending { it.deliveryDate },
            payments = payments.sortedByDescending { it.date }
        )
    }

    companion object {
        fun getThisMonthRange(accountStartDate: Long): Pair<Long, Long> {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = maxOf(cal.timeInMillis, accountStartDate)

            val endCal = Calendar.getInstance()
            endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH))
            endCal.set(Calendar.HOUR_OF_DAY, 23)
            endCal.set(Calendar.MINUTE, 59)
            endCal.set(Calendar.SECOND, 59)
            endCal.set(Calendar.MILLISECOND, 999)

            return Pair(start, endCal.timeInMillis)
        }

        fun getLastMonthRange(accountStartDate: Long): Pair<Long, Long> {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -1)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = maxOf(cal.timeInMillis, accountStartDate)

            val endCal = Calendar.getInstance()
            endCal.add(Calendar.MONTH, -1)
            endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH))
            endCal.set(Calendar.HOUR_OF_DAY, 23)
            endCal.set(Calendar.MINUTE, 59)
            endCal.set(Calendar.SECOND, 59)
            endCal.set(Calendar.MILLISECOND, 999)

            return Pair(start, endCal.timeInMillis)
        }
    }
}
