package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val customerId: String,
    val customerName: String,
    val productName: String, // Cow Milk, Buffalo Milk, Paneer, Ghee, Curd, Khoa, Butter
    val quantity: Double,
    val unit: String = "L", // L, Kg, Unit
    val rate: Double,
    val totalAmount: Double,
    val status: String = "PENDING", // PENDING, CONFIRMED, DELIVERED, CANCELLED
    val orderDate: Long = System.currentTimeMillis(),
    val deliveryDate: Long,
    val reminderDate: Long = deliveryDate,
    val reminderTime: String = "7:00 pm",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdBy: String = "system",
    val updatedBy: String = "system",
    val version: Long = 1L,
    val deletedAt: Long? = null,
    val syncStatus: String = "PENDING"
)
