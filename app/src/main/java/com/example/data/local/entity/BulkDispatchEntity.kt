package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bulk_dispatches")
data class BulkDispatchEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val buyerOrPlantName: String, // e.g. "Heritage Dairy Chilling Plant", "Amul Bulk Tanker"
    val dateEpochMidnight: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val shift: String = "MORNING", // MORNING, EVENING
    val milkType: String = "MIXED", // COW, BUFFALO, MIXED
    val vehicleNo: String = "",
    val driverName: String = "",
    val driverPhone: String = "",
    val totalLiters: Double,
    val avgFat: Double = 4.2,
    val avgSnf: Double = 8.6,
    val ratePerLiter: Double,
    val totalAmount: Double,
    val amountReceived: Double = 0.0,
    val paymentStatus: String = "PENDING", // PENDING, PARTIAL, PAID
    val challanNo: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: String = "PENDING"
)
