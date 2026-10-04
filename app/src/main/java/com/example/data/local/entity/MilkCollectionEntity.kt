package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "milk_collections")
data class MilkCollectionEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val farmerId: String,
    val farmerCode: String,
    val farmerName: String,
    val dateEpochMidnight: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val shift: String = "MORNING", // MORNING, EVENING
    val milkType: String = "COW", // COW, BUFFALO
    val quantityLiters: Double,
    val fat: Double = 4.0,
    val snf: Double = 8.5,
    val clr: Double = 28.0,
    val ratePerLiter: Double,
    val totalAmount: Double,
    val paymentStatus: String = "PENDING", // PENDING, PAID, PARTIAL
    val paymentReference: String = "",
    val sampleNo: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: String = "PENDING"
)
