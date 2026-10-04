package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for tracking milk wastage, curdling, spillage, quality rejection,
 * and daily volume reconciliation losses.
 */
@Entity(tableName = "milk_wastage")
data class MilkWastageEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val date: Long, // timestamp of the incident
    val shift: String = "MORNING", // MORNING, EVENING, FULL_DAY
    val milkType: String = "COW", // COW, BUFFALO, MIXED
    val quantityLiters: Double, // volume wasted in Liters
    val reason: String = "CURDLING_SPOILAGE", // CURDLING_SPOILAGE, TRANSIT_SPILLAGE, UNSOLD_EXPIRED, QUALITY_REJECTION, CHILLING_FAILURE, CALF_FEEDING_EXCESS, EQUIPMENT_LEAK, OTHER
    val reasonDetails: String = "",
    val costPerLiter: Double = 50.0,
    val totalLossAmount: Double = quantityLiters * costPerLiter, // Financial loss in ₹
    val batchSource: String = "Farm Milking", // Farm Milking, Route Extra, Chilling Vat, Farmer Batch
    val preventativeAction: String = "", // e.g. "Clean chilling vat", "Adjust route buffer"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdBy: String = "owner",
    val syncStatus: String = "PENDING"
)
