package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cattle_weight_records")
data class CattleWeightEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val cattleId: String,
    val tagNumber: String,
    val date: Long = System.currentTimeMillis(),
    val stage: String = "ADULT", // CALF, HEIFER, ADULT
    val weightKg: Double,
    val method: String = "MANUAL", // MANUAL, GIRTH_CALCULATION, PHOTO
    val heartGirthCm: Double = 0.0,
    val bodyLengthCm: Double = 0.0,
    val dailyGainKg: Double = 0.0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
