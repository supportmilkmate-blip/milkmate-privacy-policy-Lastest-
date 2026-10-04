package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory_transactions")
data class InventoryTransactionEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val itemId: String,
    val itemName: String,
    val transactionType: String, // OPENING_STOCK, PURCHASE, CONSUMPTION, ADJUSTMENT, PHYSICAL_CLOSING
    val quantity: Double,
    val unitPrice: Double = 0.0,
    val totalAmount: Double = 0.0,
    val date: Long,
    val supplier: String = "",
    val paymentStatus: String = "PAID", // PAID, PENDING, CREDIT
    val paymentMethod: String = "CASH", // CASH, UPI, BANK, CREDIT
    val reason: String = "", // Feed, Spoilage, Measurement difference, etc.
    val notes: String = "",
    val yearMonth: String = "", // "YYYY-MM"
    val linkedExpenseId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdBy: String = "system",
    val updatedBy: String = "system",
    val version: Long = 1L,
    val deletedAt: Long? = null,
    val syncStatus: String = "PENDING"
)
