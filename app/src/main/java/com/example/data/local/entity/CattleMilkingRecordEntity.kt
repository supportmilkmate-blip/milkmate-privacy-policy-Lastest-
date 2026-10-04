package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cattle_milking_records")
data class CattleMilkingRecordEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val cattleId: String,
    val cattleTag: String,
    val dateEpochMidnight: Long = System.currentTimeMillis(),
    val shift: String = "MORNING", // MORNING, EVENING
    val quantityLiters: Double = 6.0,
    val fat: Double = 4.2,
    val snf: Double = 8.5,
    val recordedBy: String = "Myself",
    val createdAt: Long = System.currentTimeMillis()
)
