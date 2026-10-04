package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory_month_closings")
data class InventoryMonthClosingEntity(
    @PrimaryKey val id: String, // "${businessId}_${itemId}_${yearMonth}" e.g. "BIZ1_makka_2026-09"
    val businessId: String,
    val itemId: String,
    val itemName: String,
    val yearMonth: String, // "YYYY-MM" e.g. "2026-09"
    val openingStock: Double = 0.0,
    val isOpeningVerified: Boolean = false,
    val totalPurchased: Double = 0.0,
    val totalConsumed: Double = 0.0,
    val totalAdjusted: Double = 0.0,
    val bookClosingStock: Double = 0.0,
    val physicalClosingStock: Double? = null,
    val isClosingVerified: Boolean = false,
    val verifiedAt: Long? = null,
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
