package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "farm_observation_records")
data class FarmObservationEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val cattleId: String = "", // empty for whole farm
    val cattleTag: String = "Whole Farm",
    val activityType: String = "GENERAL_NOTE", // GENERAL_NOTE, FEEDING, RUMINATION, DUNG_CONSISTENCY, HEAT_SIGNS, LAMENESS, TEMPERATURE, VET_REMARK
    val date: Long = System.currentTimeMillis(),
    val description: String = "",
    val hasAlert: Boolean = false,
    val loggedBy: String = "Farm Helper / Manager",
    val createdAt: Long = System.currentTimeMillis()
)
