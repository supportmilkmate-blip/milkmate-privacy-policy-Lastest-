package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "staff_payments")
data class StaffPaymentEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val staffId: String,
    val staffName: String,
    val type: String, // "SALARY_PAYOUT", "ADVANCE", "BONUS", "DEDUCTION"
    val amount: Double,
    val paymentMode: String = "CASH", // "CASH", "UPI", "BANK"
    val date: Long = System.currentTimeMillis(),
    val monthYear: String = "", // e.g. "2026-09"
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
