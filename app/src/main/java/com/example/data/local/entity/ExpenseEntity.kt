package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val date: Long,
    val amount: Double,
    val category: String, // FEED, SUPPLEMENT, CONVEYANCE, MEDICINE, DOCTOR_CHARGE, INVENTORY_PURCHASE, OTHER
    val subcategory: String = "",
    val notes: String = "",
    val paymentMethod: String = "CASH", // CASH, UPI, BANK_TRANSFER
    val inventoryItemId: String? = null,
    val inventoryItemName: String? = null,
    val expenseType: String = "BUSINESS", // "BUSINESS" or "PERSONAL"
    val isInventoryPurchase: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdBy: String = "system",
    val updatedBy: String = "system",
    val version: Long = 1L,
    val deletedAt: Long? = null,
    val syncStatus: String = "PENDING"
) {
    fun safeInventoryName(): String {
        return inventoryItemName?.takeIf { it.isNotBlank() && it != "undefined" } ?: "Direct Purchase"
    }

    fun isPersonal(): Boolean {
        return expenseType.equals("PERSONAL", ignoreCase = true) ||
                subcategory.contains("[Personal]", ignoreCase = true)
    }

    fun isBusiness(): Boolean {
        return !isPersonal() && !isInventoryPurchase
    }
}
