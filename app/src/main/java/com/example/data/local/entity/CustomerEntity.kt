package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val name: String,
    val mobile: String,
    val address: String = "",
    val type: String = "INDIVIDUAL", // INDIVIDUAL, BULK_BUYER, SUPPLIER
    val status: String = "ACTIVE", // ACTIVE, INACTIVE
    val rate: Double = 0.0, // 0.0 = use business default rate
    val milkType: String = "COW", // COW, BUFFALO, BOTH
    val defaultQuantity: Double = 1.0,
    val defaultShift: String = "MORNING", // MORNING, EVENING, BOTH
    val cowQuantity: Double = 1.0,
    val cowRate: Double = 55.0,
    val buffaloQuantity: Double = 1.0,
    val buffaloRate: Double = 70.0,
    val quickPresets: String = "0.5,1.0,1.5,2.0",
    val route: String = "",
    val assignedStaffId: String = "",
    val assignedStaffName: String = "",
    val notes: String = "",
    val outstandingBalance: Double = 0.0,
    val rateMethod: String = "FLAT", // FLAT, FAT_SNF, FAT_ONLY, PANEER_YIELD, KHOA_YIELD, DEFAULT
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdBy: String = "system",
    val updatedBy: String = "system",
    val version: Long = 1L,
    val deletedAt: Long? = null,
    val syncStatus: String = "PENDING"
)

fun CustomerEntity.getQuickPresetsList(): List<Double> {
    if (quickPresets.isBlank()) {
        return listOf(0.5, 1.0, 1.5, 2.0)
    }
    return try {
        val list = quickPresets.split(",")
            .mapNotNull { it.trim().toDoubleOrNull() }
            .filter { it > 0.0 }
            .distinct()
            .sorted()
        if (list.isEmpty()) listOf(0.5, 1.0, 1.5, 2.0) else list
    } catch (e: Exception) {
        listOf(0.5, 1.0, 1.5, 2.0)
    }
}

