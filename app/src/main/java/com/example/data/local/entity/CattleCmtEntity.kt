package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cattle_cmt_records")
data class CattleCmtEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val cattleId: String,
    val tagNumber: String,
    val date: Long = System.currentTimeMillis(),
    val testerName: String = "",
    val quarterLf: String = "N", // N (Negative), T (Trace), 1, 2, 3
    val quarterRf: String = "N",
    val quarterLh: String = "N",
    val quarterRh: String = "N",
    val overallDiagnosis: String = "NORMAL", // NORMAL, SUBCLINICAL_MASTITIS, CLINICAL_MASTITIS
    val treatmentRecommendation: String = "Teat dip after milking. Routine monitoring.",
    val milkWithheld: Boolean = false,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
