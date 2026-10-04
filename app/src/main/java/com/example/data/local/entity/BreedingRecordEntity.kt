package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cattle_breeding_records")
data class BreedingRecordEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val cattleId: String,
    val cattleTag: String,
    val eventType: String, // HEAT, INSEMINATION, PREGNANCY_DIAGNOSIS, CALVING, DRY_OFF, ABORTION
    val date: Long = System.currentTimeMillis(),
    val inseminationType: String = "ARTIFICIAL", // ARTIFICIAL, NATURAL
    val bullIdOrName: String = "",
    val semenStrawsUsed: Int = 1,
    val doneBy: String = "Veterinary Doctor",
    val pdStatus: String = "PENDING", // PENDING, POSITIVE_PREGNANT, NEGATIVE
    val expectedCalvingDate: Long = 0L,
    val actualCalvingDate: Long = 0L,
    val calfGender: String = "NONE", // MALE, FEMALE, TWIN, NONE
    val calfTag: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
