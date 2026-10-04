package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cattle_bcs_records")
data class CattleBcsEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val cattleId: String,
    val tagNumber: String,
    val date: Long = System.currentTimeMillis(),
    val score: Double = 3.0, // 1.0 - 5.0
    val category: String = "IDEAL", // VERY_THIN, THIN, IDEAL, OVERWEIGHT, OBESE
    val spineAssessment: String = "Slightly rounded, vertebrae visible with pressure",
    val ribsAssessment: String = "Smooth feel with slight fat cushion",
    val hooksAndPins: String = "Rounded U-shape profile",
    val nutritionAdvice: String = "Maintain current balanced ration (Green fodder + 3kg concentrate)",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
