package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory_items")
data class InventoryItemEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val itemName: String,
    val unit: String = "kg", // kg, litre, piece, bag, etc.
    val currentStock: Double = 0.0,
    val openingStock: Double = 0.0, // Strictly 0.0 by default for new accounts
    val isOpeningVerified: Boolean = false, // Distinguishes "0 verified" from "not entered"
    val costPerUnit: Double = 0.0, // Purchase cost / average rate
    val dailyUsage: Double? = null, // Null or 0 means "Daily use: Not set"
    val alertDaysThreshold: Int = 15,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdBy: String = "system",
    val updatedBy: String = "system",
    val version: Long = 1L,
    val deletedAt: Long? = null,
    val syncStatus: String = "PENDING"
) {
    val estimatedDaysRemaining: Double?
        get() = if (dailyUsage != null && dailyUsage > 0.0) {
            currentStock / dailyUsage
        } else {
            null
        }

    val stockAlertStatus: String
        get() = when {
            currentStock <= 0.0 -> "OUT_OF_STOCK"
            estimatedDaysRemaining != null && estimatedDaysRemaining!! <= 15.0 -> "LOW_STOCK"
            estimatedDaysRemaining == null && currentStock <= alertDaysThreshold.toDouble() -> "LOW_STOCK"
            else -> "NORMAL"
        }
}
