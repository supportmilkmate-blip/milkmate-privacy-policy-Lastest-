package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "farmers")
data class FarmerEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val farmerCode: String, // e.g. "F-01", "101"
    val name: String,
    val mobile: String = "",
    val village: String = "",
    val milkType: String = "COW", // COW, BUFFALO, BOTH
    val defaultShift: String = "BOTH", // MORNING, EVENING, BOTH
    val defaultQuantityEstimate: Double = 5.0,
    val paymentMode: String = "CASH", // CASH, UPI, BANK_TRANSFER
    val bankAccountNo: String = "",
    val ifscCode: String = "",
    val upiId: String = "",
    val rateAdjustmentPerLiter: Double = 0.0, // +/- per liter adjustment
    val balancePayable: Double = 0.0, // Amount owed to farmer for procured milk
    val status: String = "ACTIVE", // ACTIVE, INACTIVE
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: String = "PENDING"
)
