package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cattle_records")
data class CattleEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val tagNumber: String, // e.g. "C-101", "BUF-04"
    val name: String = "",
    val type: String = "COW", // COW, BUFFALO
    val breed: String = "Holstein Friesian", // HF, Jersey, Gir, Sahiwal, Murrah, Jaffarabadi
    val lactationStage: String = "LACTATING", // LACTATING, DRY, HEIFER, PREGNANT_DRY
    val dailyYieldLiters: Double = 12.0,
    val breedingStatus: String = "INSEMINATED", // OPEN, INSEMINATED, CONFIRMED_PREGNANT, CALVED
    val lastInseminationDate: Long = 0L, // Epoch millis
    val inseminationType: String = "ARTIFICIAL_INSEMINATION", // ARTIFICIAL_INSEMINATION, NATURAL_MATING
    val bullIdOrSemenBrand: String = "ABS Prime Bull #92",
    val expectedCalvingDate: Long = 0L, // ~280 days for Cow, ~310 days for Buffalo
    val lactationCount: Int = 2,
    val birthDate: Long = 0L,
    val currentWeightKg: Double = 0.0,
    val latestBcs: Double = 3.0,
    val damTagOrName: String = "",
    val sireTagOrName: String = "",
    val healthNotes: String = "Vaccinated, healthy",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: String = "PENDING"
)
