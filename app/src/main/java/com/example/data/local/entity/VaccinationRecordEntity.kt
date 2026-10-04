package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cattle_vaccination_records")
data class VaccinationRecordEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val cattleId: String,
    val cattleTag: String,
    val vaccineName: String, // FMD, HS, BQ, FMD+HS+BQ, Brucellosis, Theileriosis, Rabies, Anthrax, Lumpy Skin, PPR
    val manufacturer: String = "Indian Immunologicals Ltd",
    val batchNo: String = "",
    val date: Long = System.currentTimeMillis(),
    val nextDueDate: Long = System.currentTimeMillis() + (180L * 86400000L),
    val cost: Double = 0.0,
    val vaccinatedBy: String = "Veterinary Assistant / Doctor",
    val proofAttachment: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
