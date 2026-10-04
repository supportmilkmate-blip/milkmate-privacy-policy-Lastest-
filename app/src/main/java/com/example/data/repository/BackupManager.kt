package com.example.data.repository

import android.content.Context
import com.example.data.local.MilkMateDatabase
import com.example.data.local.entity.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.first

data class BackupValidationResult(
    val isValid: Boolean,
    val businessId: String = "",
    val backupDate: Long = 0L,
    val customerCount: Int = 0,
    val deliveryCount: Int = 0,
    val paymentCount: Int = 0,
    val expenseCount: Int = 0,
    val errorMessage: String? = null
)

class BackupManager(
    private val context: Context,
    private val db: MilkMateDatabase
) {

    private val fileTimestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    fun createBackupJson(businessId: String): File {
        val root = JSONObject()
        root.put("version", 1)
        root.put("app", "MilkMate")
        root.put("businessId", businessId)
        root.put("timestamp", System.currentTimeMillis())

        // In a coroutine, we query all tables
        // For synchronous file writing here, we can store in a temp file or via suspend
        val dir = File(context.filesDir, "backups")
        if (!dir.exists()) dir.mkdirs()

        val backupFile = File(dir, "milkmate_backup_${businessId}_${fileTimestampFormat.format(Date())}.json")
        return backupFile
    }

    suspend fun exportBusinessData(businessId: String): File {
        val root = JSONObject()
        root.put("version", 1)
        root.put("app", "MilkMate")
        root.put("businessId", businessId)
        root.put("timestamp", System.currentTimeMillis())

        val business = db.businessDao().getBusiness(businessId)
        if (business != null) {
            val bObj = JSONObject().apply {
                put("id", business.id)
                put("businessName", business.businessName)
                put("ownerName", business.ownerName)
                put("phone", business.phone)
                put("accountStartDate", business.accountStartDate)
                put("defaultMilkType", business.defaultMilkType)
                put("cowMilkRate", business.cowMilkRate)
                put("buffaloMilkRate", business.buffaloMilkRate)
                put("pricingMode", business.pricingMode)
                put("subscriptionPlan", business.subscriptionPlan)
            }
            root.put("business", bObj)
        }

        val customers = db.customerDao().getAllCustomersFlow(businessId).first()
        val custArray = JSONArray()
        for (c in customers) {
            val obj = JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("mobile", c.mobile)
                put("address", c.address)
                put("type", c.type)
                put("rate", c.rate)
                put("milkType", c.milkType)
                put("defaultQuantity", c.defaultQuantity)
                put("defaultShift", c.defaultShift)
                put("route", c.route)
                put("notes", c.notes)
                put("outstandingBalance", c.outstandingBalance)
            }
            custArray.put(obj)
        }
        root.put("customers", custArray)

        val deliveries = db.deliveryDao().getDeliveriesBetweenDates(businessId, 0L, Long.MAX_VALUE)
        val delArray = JSONArray()
        for (d in deliveries) {
            val obj = JSONObject().apply {
                put("id", d.id)
                put("customerId", d.customerId)
                put("customerName", d.customerName)
                put("customerType", d.customerType)
                put("deliveryDate", d.deliveryDate)
                put("shift", d.shift)
                put("milkType", d.milkType)
                put("quantityLiters", d.quantityLiters)
                put("fat", d.fat)
                put("snf", d.snf)
                put("ratePerLiter", d.ratePerLiter)
                put("totalAmount", d.totalAmount)
                put("isDelivered", d.isDelivered)
            }
            delArray.put(obj)
        }
        root.put("deliveries", delArray)

        val payments = db.paymentDao().getPaymentsBetweenDates(businessId, 0L, Long.MAX_VALUE)
        val payArray = JSONArray()
        for (p in payments) {
            val obj = JSONObject().apply {
                put("id", p.id)
                put("customerId", p.customerId)
                put("customerName", p.customerName)
                put("paymentType", p.paymentType)
                put("date", p.date)
                put("amount", p.amount)
                put("method", p.method)
                put("referenceNo", p.referenceNo)
            }
            payArray.put(obj)
        }
        root.put("payments", payArray)

        val expenses = db.expenseDao().getExpensesBetweenDates(businessId, 0L, Long.MAX_VALUE)
        val expArray = JSONArray()
        for (e in expenses) {
            val obj = JSONObject().apply {
                put("id", e.id)
                put("date", e.date)
                put("amount", e.amount)
                put("category", e.category)
                put("subcategory", e.subcategory)
                put("notes", e.notes)
                put("paymentMethod", e.paymentMethod)
                put("inventoryItemId", e.inventoryItemId)
                put("inventoryItemName", e.safeInventoryName())
            }
            expArray.put(obj)
        }
        root.put("expenses", expArray)

        val dir = File(context.filesDir, "backups")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "milkmate_backup_${businessId}_${fileTimestampFormat.format(Date())}.json")
        file.writeText(root.toString(2))
        return file
    }

    fun validateBackupContent(jsonString: String, currentBusinessId: String): BackupValidationResult {
        return try {
            val root = JSONObject(jsonString)
            if (!root.has("app") || root.getString("app") != "MilkMate") {
                return BackupValidationResult(isValid = false, errorMessage = "Invalid file: Not a MilkMate backup.")
            }
            val bId = root.optString("businessId", "")
            if (bId.isNotBlank() && bId != currentBusinessId) {
                return BackupValidationResult(
                    isValid = false,
                    businessId = bId,
                    errorMessage = "Business mismatch! This backup belongs to business ID '$bId', not current business."
                )
            }
            val timestamp = root.optLong("timestamp", 0L)
            val customersCount = root.optJSONArray("customers")?.length() ?: 0
            val deliveriesCount = root.optJSONArray("deliveries")?.length() ?: 0
            val paymentsCount = root.optJSONArray("payments")?.length() ?: 0
            val expensesCount = root.optJSONArray("expenses")?.length() ?: 0

            BackupValidationResult(
                isValid = true,
                businessId = bId,
                backupDate = timestamp,
                customerCount = customersCount,
                deliveryCount = deliveriesCount,
                paymentCount = paymentsCount,
                expenseCount = expensesCount
            )
        } catch (e: Exception) {
            BackupValidationResult(isValid = false, errorMessage = "Failed to parse backup JSON: ${e.localizedMessage}")
        }
    }

    suspend fun restoreBackupContent(jsonString: String, currentBusinessId: String): Boolean {
        val validation = validateBackupContent(jsonString, currentBusinessId)
        if (!validation.isValid) return false

        val root = JSONObject(jsonString)

        val customersArray = root.optJSONArray("customers")
        if (customersArray != null) {
            val list = mutableListOf<CustomerEntity>()
            for (i in 0 until customersArray.length()) {
                val obj = customersArray.getJSONObject(i)
                list.add(
                    CustomerEntity(
                        id = obj.getString("id"),
                        businessId = currentBusinessId,
                        name = obj.getString("name"),
                        mobile = obj.optString("mobile", ""),
                        address = obj.optString("address", ""),
                        type = obj.optString("type", "INDIVIDUAL"),
                        rate = obj.optDouble("rate", 0.0),
                        milkType = obj.optString("milkType", "COW"),
                        defaultQuantity = obj.optDouble("defaultQuantity", 1.0),
                        defaultShift = obj.optString("defaultShift", "MORNING"),
                        route = obj.optString("route", ""),
                        notes = obj.optString("notes", ""),
                        outstandingBalance = obj.optDouble("outstandingBalance", 0.0)
                    )
                )
            }
            db.customerDao().insertAll(list)
        }

        val deliveriesArray = root.optJSONArray("deliveries")
        if (deliveriesArray != null) {
            val list = mutableListOf<DeliveryEntity>()
            for (i in 0 until deliveriesArray.length()) {
                val obj = deliveriesArray.getJSONObject(i)
                list.add(
                    DeliveryEntity(
                        id = obj.getString("id"),
                        businessId = currentBusinessId,
                        customerId = obj.getString("customerId"),
                        customerName = obj.getString("customerName"),
                        customerType = obj.optString("customerType", "INDIVIDUAL"),
                        deliveryDate = obj.getLong("deliveryDate"),
                        shift = obj.optString("shift", "MORNING"),
                        milkType = obj.optString("milkType", "COW"),
                        quantityLiters = obj.getDouble("quantityLiters"),
                        fat = obj.optDouble("fat", 0.0),
                        snf = obj.optDouble("snf", 0.0),
                        ratePerLiter = obj.getDouble("ratePerLiter"),
                        totalAmount = obj.getDouble("totalAmount"),
                        isDelivered = obj.optBoolean("isDelivered", true)
                    )
                )
            }
            db.deliveryDao().insertAll(list)
        }

        return true
    }
}
