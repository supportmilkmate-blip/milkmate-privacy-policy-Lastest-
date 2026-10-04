package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cattle_deworming_records")
data class DewormingRecordEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val cattleId: String, // Individual cattle ID or comma-separated/ALL
    val cattleTag: String,
    val dewormerSalt: String, // Albendazole, Fenbendazole, Ivermectin, Oxyclozanide, Piperazine, Levamisole, etc.
    val dose: String = "100 ml",
    val date: Long = System.currentTimeMillis(),
    val repeatAfterDays: Int = 90,
    val nextDueDate: Long = System.currentTimeMillis() + (90L * 86400000L),
    val administeredBy: String = "Self / Farm Manager",
    val cost: Double = 0.0,
    val remarks: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
