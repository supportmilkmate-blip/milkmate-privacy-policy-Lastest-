package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val customerId: String,
    val customerName: String,
    val paymentType: String = "CUSTOMER_PAYMENT", // CUSTOMER_PAYMENT, SUPPLIER_PAYMENT
    val date: Long,
    val amount: Double,
    val method: String = "CASH", // CASH, UPI, BANK_TRANSFER, CHEQUE
    val referenceNo: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdBy: String = "system",
    val updatedBy: String = "system",
    val version: Long = 1L,
    val deletedAt: Long? = null,
    val syncStatus: String = "PENDING"
)
