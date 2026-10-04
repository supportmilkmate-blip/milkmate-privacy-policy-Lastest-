package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "staff_attendance")
data class StaffAttendanceEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val staffId: String,
    val staffName: String,
    val date: Long, // Midnight timestamp
    val status: String, // "PRESENT", "HALF_DAY", "ABSENT", "LEAVE"
    val shift: String = "ALL_DAY",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
