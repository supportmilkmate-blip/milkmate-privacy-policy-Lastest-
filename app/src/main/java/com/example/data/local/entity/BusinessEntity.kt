package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "businesses")
data class BusinessEntity(
    @PrimaryKey val id: String,
    val ownerUid: String,
    val businessName: String,
    val ownerName: String,
    val phone: String,
    val accountStartDate: Long,
    val currencySymbol: String = "₹",
    val defaultMilkType: String = "COW",
    val supportedMilkTypes: String = "BOTH", // COW_ONLY, BUFFALO_ONLY, BOTH, MIXED
    val businessMode: String = "FARMER", // FARMER, FARMER_WHOLESALE, COLLECTION_CENTER, TRADER, INTEGRATED, PROCESSING_UNIT, RETAIL_PARLOUR, DELIVERY_AGENT, GAUSHALA

    // 🌟 Source Capabilities (Where milk comes from)
    val sourceOwnCattle: Boolean = true,
    val sourceVillageFarmers: Boolean = false,

    // 🌟 Destination Capabilities (Where milk is supplied)
    val destHouseholds: Boolean = true,
    val destCollectionCenter: Boolean = false,
    val destBulkCommercial: Boolean = false,
    val destFactoryTankers: Boolean = false,

    // 🌟 Value-Added Product Offerings
    val hasPaneer: Boolean = true,
    val hasCurdChaas: Boolean = true,
    val hasGheeButter: Boolean = true,
    val hasKhoyaSweets: Boolean = false,
    val hasCattleFeed: Boolean = false,

    val defaultRatePerLiter: Double = 50.0,
    val cowMilkRate: Double = 50.0,
    val buffaloMilkRate: Double = 65.0,
    val pricingMode: String = "DIRECT", // DIRECT, FAT_SNF, FAT_ONLY, SNF_ONLY, PANEER_YIELD, KHOA_YIELD, CUSTOM
    val fatBaseRate: Double = 6.5,
    val snfBaseRate: Double = 4.0,
    val fatDiffRate: Double = 0.5,
    val snfDiffRate: Double = 0.4,
    val paneerRatePerKg: Double = 350.0,
    val khoaRatePerKg: Double = 320.0,
    val khoaBaseYieldGrams: Double = 160.0,
    val khoaBaseRatePerLiter: Double = 45.0,
    val khoaDiffRatePer10g: Double = 2.8,
    val paneerBaseYieldGrams: Double = 140.0,
    val paneerBaseRatePerLiter: Double = 44.0,
    val paneerDiffRatePer10g: Double = 3.0,
    val gheeBaseYieldGrams: Double = 60.0,
    val gheeBaseRatePerLiter: Double = 42.0,
    val gheeDiffRatePer10g: Double = 7.0,
    val customFormulaName: String = "Cooperative Standard",
    val customFormulaBase: Double = 28.0,
    val customFormulaFatMultiplier: Double = 6.2,
    val customFormulaSnfMultiplier: Double = 4.1,
    val customFormulaTsMultiplier: Double = 0.0,
    val customFormulaClrMultiplier: Double = 0.0,
    val customFormulaQualityBonus: Double = 1.0,
    val customFormulaChillingDeduction: Double = 0.0,
    val customFormulaMinFloorRate: Double = 10.0,
    val customFormulaYieldType: String = "NONE", // NONE, PANEER, KHOA, GHEE
    val customFormulaYieldMultiplier: Double = 0.0,
    val customFormulaBaseYieldGrams: Double = 160.0,
    val customFormulaDiffRatePer10g: Double = 2.8,
    val customFormulaDifferentialMode: Boolean = false,
    val subscriptionPlan: String = "FREE", // FREE, PRO
    val subscriptionStatus: String = "ACTIVE", // ACTIVE, EXPIRED, CANCELLED, PENDING
    val billingPeriod: String = "MONTHLY", // MONTHLY, YEARLY
    val subscriptionStartedAt: Long = System.currentTimeMillis(),
    val subscriptionExpiresAt: Long = 0L, // 0 = permanent/perpetual for free, or timestamp for pro
    val provider: String = "PLAY_STORE",
    val providerSubscriptionId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdBy: String = "owner",
    val updatedBy: String = "owner",
    val version: Long = 1L,
    val deletedAt: Long? = null,
    val syncStatus: String = "PENDING" // PENDING, SYNCING, SYNCED, FAILED, CONFLICT
)
