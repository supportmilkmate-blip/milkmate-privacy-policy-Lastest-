package com.example.data.repository

import com.example.data.local.entity.BusinessEntity
import com.example.data.local.entity.CustomerEntity

object PricingEngine {

    enum class PricingMode {
        DIRECT,
        FAT_SNF,
        FAT_ONLY,
        SNF_ONLY,
        PANEER_YIELD,
        KHOA_YIELD,
        GHEE_YIELD,
        CLR_TS,
        CUSTOM
    }

    /**
     * Compute expected Paneer yield in Grams per Liter from Fat & SNF.
     * Standard Indian Dairy Science Empirical Equation:
     * Yield (g/L) = (FAT% * 21.5) + (SNF% * 9.2)
     * e.g. 6.5% Fat + 9.0% SNF = 139.75 + 82.8 = 222.55 g/L
     */
    fun calculatePaneerYieldGrams(fat: Double, snf: Double = 8.5): Double {
        val effectiveFat = if (fat > 0.0) fat else 4.5
        val effectiveSnf = if (snf > 0.0) snf else 8.5
        val grams = (effectiveFat * 21.5) + (effectiveSnf * 9.2)
        return Math.round(grams * 10.0) / 10.0
    }

    /**
     * Compute expected Khoya / Mawa yield in Grams per Liter from Fat & SNF.
     * Total Solids concentration yield:
     * Yield (g/L) = (FAT% * 24.0) + (SNF% * 10.5)
     * e.g. 7.0% Fat Buffalo Milk + 9.0% SNF = 168.0 + 94.5 = 262.5 g/L
     */
    fun calculateKhoaYieldGrams(fat: Double, snf: Double = 8.5): Double {
        val effectiveFat = if (fat > 0.0) fat else 6.5
        val effectiveSnf = if (snf > 0.0) snf else 9.0
        val grams = (effectiveFat * 24.0) + (effectiveSnf * 10.5)
        return Math.round(grams * 10.0) / 10.0
    }

    /**
     * Compute expected Pure Desi Ghee yield in Grams per Liter from Fat.
     * Fat Recovery % = ~88% conversion of fat to clarified butterfat.
     * Yield (g/L) = FAT% * 10 * 0.88
     * e.g. 7.0% Fat = 7.0 * 10 * 0.88 = 61.6 g/L
     */
    fun calculateGheeYieldGrams(fat: Double): Double {
        val effectiveFat = if (fat > 0.0) fat else 6.5
        val grams = effectiveFat * 10.0 * 0.88
        return Math.round(grams * 10.0) / 10.0
    }

    /**
     * Calculate SNF from CLR (Corrected Lactometer Reading) and FAT %.
     * Standard Richmond Dairy Formula: SNF = (CLR / 4) + (0.25 * FAT) + 0.36
     */
    fun calculateSnfFromClr(clr: Double, fat: Double): Double {
        if (clr <= 0.0 || fat <= 0.0) return 8.5
        val snf = (clr / 4.0) + (0.25 * fat) + 0.36
        return Math.round(snf * 100.0) / 100.0
    }

    /**
     * Calculates the rate per liter for a delivery or collection entry.
     * Respects customer-specific rate override (if customer.rate > 0.0).
     * Otherwise, calculates dynamically from the selected pricing model.
     */
    fun calculateRatePerLiter(
        business: BusinessEntity?,
        customer: CustomerEntity?,
        milkType: String, // "COW" or "BUFFALO"
        fat: Double = 0.0,
        snf: Double = 0.0,
        clr: Double = 0.0
    ): Double {
        val effectiveMethod = if (customer != null && customer.rateMethod.isNotBlank() && customer.rateMethod != "DEFAULT") {
            customer.rateMethod.uppercase()
        } else {
            business?.pricingMode?.uppercase() ?: "DIRECT"
        }

        // 1. Direct Fixed / Flat Rate mode
        if (effectiveMethod == "FLAT" || effectiveMethod == "DIRECT" || effectiveMethod == "FIXED") {
            if (customer != null) {
                if (milkType.equals("COW", ignoreCase = true) && customer.cowRate > 0.0) {
                    return customer.cowRate
                }
                if (milkType.equals("BUFFALO", ignoreCase = true) && customer.buffaloRate > 0.0) {
                    return customer.buffaloRate
                }
                if (customer.rate > 0.0) {
                    return customer.rate
                }
            }
            return if (milkType.equals("BUFFALO", ignoreCase = true)) (business?.buffaloMilkRate ?: 65.0) else (business?.cowMilkRate ?: 50.0)
        }

        if (business == null) {
            return if (milkType.equals("BUFFALO", ignoreCase = true)) 65.0 else 50.0
        }

        // Calculate SNF from CLR if SNF is not supplied but CLR is provided:
        val effectiveSnf = if (snf > 0.0) {
            snf
        } else if (clr > 0.0 && fat > 0.0) {
            calculateSnfFromClr(clr, fat)
        } else {
            if (milkType.equals("BUFFALO", ignoreCase = true)) 9.0 else 8.5
        }

        val baseMilkRate = if (milkType.equals("BUFFALO", ignoreCase = true)) {
            if (customer != null && customer.buffaloRate > 0.0) customer.buffaloRate else business.buffaloMilkRate
        } else {
            if (customer != null && customer.cowRate > 0.0) customer.cowRate else business.cowMilkRate
        }

        return when (effectiveMethod) {
            "DIRECT", "FIXED", "FLAT" -> baseMilkRate

            "FAT_SNF" -> {
                // Standard Two-axis formula:
                // Base Rate + ((FAT - BaseFat) * FatDiff) + ((SNF - BaseSnf) * SnfDiff)
                if (fat > 0.0) {
                    val fatDiff = (fat - business.fatBaseRate) * business.fatDiffRate
                    val snfDiff = (effectiveSnf - business.snfBaseRate) * business.snfDiffRate
                    val calculated = baseMilkRate + fatDiff + snfDiff
                    maxOf(10.0, Math.round(calculated * 100.0) / 100.0)
                } else {
                    baseMilkRate
                }
            }

            "FAT_ONLY" -> {
                // Rate proportional to Fat percentage: e.g. Fat * (BaseRate / StandardFat)
                if (fat > 0.0) {
                    val ratePerFatKg = baseMilkRate / (if (business.fatBaseRate > 0) business.fatBaseRate else 6.5)
                    val calculated = fat * ratePerFatKg
                    maxOf(10.0, Math.round(calculated * 100.0) / 100.0)
                } else {
                    baseMilkRate
                }
            }

            "SNF_ONLY" -> {
                if (effectiveSnf > 0.0) {
                    val ratePerSnfUnit = baseMilkRate / (if (business.snfBaseRate > 0) business.snfBaseRate else 8.5)
                    val calculated = effectiveSnf * ratePerSnfUnit
                    maxOf(10.0, Math.round(calculated * 100.0) / 100.0)
                } else {
                    baseMilkRate
                }
            }

            "PANEER_YIELD" -> {
                // Indian Standard Paneer Yield Benchmark:
                // Base: 140g Paneer per Liter = ₹44.0/L Base Rate
                // For every +10g Paneer yield: +₹3.0/L (or configurable paneerDiffRatePer10g)
                val yieldGrams = calculatePaneerYieldGrams(fat, effectiveSnf)
                val baseYield = if (business.paneerBaseYieldGrams > 0) business.paneerBaseYieldGrams else 140.0
                val baseRate = if (business.paneerBaseRatePerLiter > 0) business.paneerBaseRatePerLiter else 44.0
                val diffRatePer10g = if (business.paneerDiffRatePer10g > 0) business.paneerDiffRatePer10g else 3.0
                val yieldDiff = yieldGrams - baseYield
                val calculatedRate = baseRate + (yieldDiff * (diffRatePer10g / 10.0))
                maxOf(15.0, Math.round(calculatedRate * 100.0) / 100.0)
            }

            "KHOA_YIELD", "KHOYA_YIELD" -> {
                // Indian Standard Khoa / Mawa Yield Benchmark:
                // Base: 160g Khoa per Liter = ₹45.0/L Base Rate
                // For every +10g Khoa yield: +₹2.8/L (or configurable khoaDiffRatePer10g)
                val yieldGrams = calculateKhoaYieldGrams(fat, effectiveSnf)
                val baseYield = if (business.khoaBaseYieldGrams > 0) business.khoaBaseYieldGrams else 160.0
                val baseRate = if (business.khoaBaseRatePerLiter > 0) business.khoaBaseRatePerLiter else 45.0
                val diffRatePer10g = if (business.khoaDiffRatePer10g > 0) business.khoaDiffRatePer10g else 2.8
                val yieldDiff = yieldGrams - baseYield
                val calculatedRate = baseRate + (yieldDiff * (diffRatePer10g / 10.0))
                maxOf(15.0, Math.round(calculatedRate * 100.0) / 100.0)
            }

            "GHEE_YIELD" -> {
                // Indian Standard Desi Ghee Recovery Benchmark:
                // Base: 60g Ghee per Liter = ₹42.0/L Base Rate
                // For every +10g Ghee: +₹7.0/L
                val yieldGrams = calculateGheeYieldGrams(fat)
                val baseYield = if (business.gheeBaseYieldGrams > 0) business.gheeBaseYieldGrams else 60.0
                val baseRate = if (business.gheeBaseRatePerLiter > 0) business.gheeBaseRatePerLiter else 42.0
                val diffRatePer10g = if (business.gheeDiffRatePer10g > 0) business.gheeDiffRatePer10g else 7.0
                val yieldDiff = yieldGrams - baseYield
                val calculatedRate = baseRate + (yieldDiff * (diffRatePer10g / 10.0))
                maxOf(15.0, Math.round(calculatedRate * 100.0) / 100.0)
            }

            "CLR_TS" -> {
                // CLR density formula: SNF = (CLR/4) + (0.25*FAT) + 0.36
                val calculatedSnf = calculateSnfFromClr(clr, fat)
                val calculated = 20.0 + (fat * 4.8) + (calculatedSnf * 2.5)
                maxOf(10.0, Math.round(calculated * 100.0) / 100.0)
            }

            "CUSTOM" -> {
                val base = business.customFormulaBase
                val fatComponent = if (business.customFormulaDifferentialMode) {
                    (fat - business.fatBaseRate) * business.customFormulaFatMultiplier
                } else {
                    fat * business.customFormulaFatMultiplier
                }
                val snfComponent = if (business.customFormulaDifferentialMode) {
                    (effectiveSnf - business.snfBaseRate) * business.customFormulaSnfMultiplier
                } else {
                    effectiveSnf * business.customFormulaSnfMultiplier
                }
                val ts = fat + effectiveSnf
                val tsComponent = ts * business.customFormulaTsMultiplier
                val clrComponent = clr * business.customFormulaClrMultiplier
                val yieldComponent = when (business.customFormulaYieldType.uppercase()) {
                    "PANEER" -> {
                        val yieldGrams = calculatePaneerYieldGrams(fat, effectiveSnf)
                        val baseGrams = if (business.customFormulaBaseYieldGrams > 0) business.customFormulaBaseYieldGrams else 140.0
                        val stepRate = if (business.customFormulaDiffRatePer10g > 0) business.customFormulaDiffRatePer10g else 3.0
                        (yieldGrams - baseGrams) * (stepRate / 10.0)
                    }
                    "KHOA", "KHOYA" -> {
                        val yieldGrams = calculateKhoaYieldGrams(fat, effectiveSnf)
                        val baseGrams = if (business.customFormulaBaseYieldGrams > 0) business.customFormulaBaseYieldGrams else 160.0
                        val stepRate = if (business.customFormulaDiffRatePer10g > 0) business.customFormulaDiffRatePer10g else 2.8
                        (yieldGrams - baseGrams) * (stepRate / 10.0)
                    }
                    "GHEE" -> {
                        val yieldGrams = calculateGheeYieldGrams(fat)
                        val baseGrams = if (business.customFormulaBaseYieldGrams > 0) business.customFormulaBaseYieldGrams else 60.0
                        val stepRate = if (business.customFormulaDiffRatePer10g > 0) business.customFormulaDiffRatePer10g else 7.0
                        (yieldGrams - baseGrams) * (stepRate / 10.0)
                    }
                    else -> {
                        if (business.customFormulaYieldMultiplier > 0) {
                            (calculatePaneerYieldGrams(fat, effectiveSnf) / 1000.0) * business.customFormulaYieldMultiplier
                        } else 0.0
                    }
                }
                val rawTotal = base + fatComponent + snfComponent + tsComponent + clrComponent + yieldComponent + business.customFormulaQualityBonus - business.customFormulaChillingDeduction
                val floor = if (business.customFormulaMinFloorRate > 0) business.customFormulaMinFloorRate else 10.0
                maxOf(floor, Math.round(rawTotal * 100.0) / 100.0)
            }

            else -> baseMilkRate
        }
    }

    /**
     * Compute total delivery or collection amount: quantity * rate.
     */
    fun calculateTotalAmount(quantityLiters: Double, ratePerLiter: Double): Double {
        return Math.round(quantityLiters * ratePerLiter * 100.0) / 100.0
    }
}
