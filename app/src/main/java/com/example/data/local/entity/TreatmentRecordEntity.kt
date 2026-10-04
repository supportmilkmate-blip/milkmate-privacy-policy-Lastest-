package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cattle_treatment_records")
data class TreatmentRecordEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val cattleId: String,
    val cattleTag: String,
    val diseaseName: String, // Mastitis, Milk Fever, Ketosis, Foot Rot, Bloat, Theileriosis, Fever, Indigestion, Wound, Prolapse
    val symptoms: String = "",
    val medicationName: String = "",
    val dosage: String = "",
    val timing: String = "Morning & Evening", // Morning, Afternoon, Night, Morning & Evening
    val durationDays: Int = 3,
    val checkupDate: Long = System.currentTimeMillis(),
    val treatmentCost: Double = 0.0,
    val performedBy: String = "Veterinary Doctor",
    val followUpDate: Long = 0L,
    val milkWithdrawalDays: Int = 0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
