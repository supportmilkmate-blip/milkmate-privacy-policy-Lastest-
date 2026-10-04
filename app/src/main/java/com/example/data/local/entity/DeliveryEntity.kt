package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "deliveries")
data class DeliveryEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val customerId: String,
    val customerName: String,
    val customerType: String = "INDIVIDUAL", // INDIVIDUAL, BULK_BUYER, SUPPLIER
    val deliveryDate: Long, // Epoch day (normalized midnight millis)
    val shift: String = "MORNING", // MORNING, EVENING
    val milkType: String = "COW", // COW, BUFFALO
    val quantityLiters: Double,
    val fat: Double = 0.0,
    val snf: Double = 0.0,
    val clr: Double = 0.0,
    val ratePerLiter: Double,
    val totalAmount: Double,
    val isDelivered: Boolean = true,
    val notes: String = "",
    val deliveredByStaffId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdBy: String = "system",
    val updatedBy: String = "system",
    val version: Long = 1L,
    val deletedAt: Long? = null,
    val syncStatus: String = "PENDING"
)
