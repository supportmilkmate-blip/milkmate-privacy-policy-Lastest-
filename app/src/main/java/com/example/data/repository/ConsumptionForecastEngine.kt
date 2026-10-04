package com.example.data.repository

import com.example.data.local.dao.DeliveryDao
import com.example.data.local.dao.InventoryDao
import com.example.data.local.entity.DeliveryEntity
import com.example.data.local.entity.InventoryItemEntity
import com.example.data.local.entity.InventoryTransactionEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.round

enum class ForecastUrgency {
    CRITICAL,    // <= 5 days or out of stock -> Red Alert
    WARNING,     // 6 - 15 days -> Amber Warning
    ADEQUATE,    // 16 - 45 days -> Green Healthy
    WELL_STOCKED // > 45 days -> Blue / Grey
}

data class MonthlyDeliveryStats(
    val yearMonth: String,
    val monthName: String,
    val totalLiters: Double,
    val totalDeliveries: Int,
    val dailyAverageLiters: Double
)

data class MonthlyItemUsageStats(
    val yearMonth: String,
    val monthName: String,
    val usageQty: Double,
    val dailyAverageQty: Double
)

data class ItemForecastResult(
    val item: InventoryItemEntity,
    val category: String, // CATTLE_FEED, DAIRY_PRODUCT, GENERAL
    val currentStock: Double,
    val unit: String,
    val costPerUnit: Double,
    val m1Usage: Double, // 60-90 days ago
    val m2Usage: Double, // 30-60 days ago
    val m3Usage: Double, // Last 30 days
    val total90DayUsage: Double,
    val historicalDailyUsage: Double,
    val effectiveProjectedDailyUsage: Double,
    val usageTrendPercent: Double,
    val daysRemaining: Double,
    val runOutDateMillis: Long?,
    val urgencyStatus: ForecastUrgency,
    val suggestedPurchaseQty: Double,
    val suggestedPackageUnits: String,
    val estimatedPurchaseCost: Double,
    val lastSupplier: String,
    val lastPurchasePrice: Double,
    val feedToMilkRatio: Double?,
    val demandInsight: String
)

data class ConsumptionForecastSummary(
    val businessId: String,
    val generatedAt: Long = System.currentTimeMillis(),
    val planningHorizonDays: Int = 30,
    val safetyBufferDays: Int = 7,
    val totalItemsAnalyzed: Int = 0,
    val criticalItemsCount: Int = 0,
    val warningItemsCount: Int = 0,
    val adequateItemsCount: Int = 0,
    val totalEstimatedReorderCost: Double = 0.0,
    val total90DayMilkDelivered: Double = 0.0,
    val avgDailyMilkDelivered: Double = 0.0,
    val milkTrendPercent: Double = 0.0,
    val monthlyDeliveryStats: List<MonthlyDeliveryStats> = emptyList(),
    val itemForecasts: List<ItemForecastResult> = emptyList()
)

class ConsumptionForecastEngine(
    private val deliveryDao: DeliveryDao,
    private val inventoryDao: InventoryDao
) {
    private val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
    private val yearMonthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())

    suspend fun generateForecast(
        businessId: String,
        planningHorizonDays: Int = 30,
        safetyBufferDays: Int = 7,
        businessMode: String? = null
    ): ConsumptionForecastSummary {
        val now = System.currentTimeMillis()

        // Establish the 3-Month Windows (M1: -90 to -60d, M2: -60 to -30d, M3: -30d to now)
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val endM3 = cal.timeInMillis

        // Start of M3 (30 days ago)
        cal.add(Calendar.DAY_OF_YEAR, -30)
        setMidnight(cal)
        val startM3 = cal.timeInMillis

        // Start of M2 (60 days ago)
        cal.add(Calendar.DAY_OF_YEAR, -30)
        setMidnight(cal)
        val startM2 = cal.timeInMillis

        // Start of M1 (90 days ago)
        cal.add(Calendar.DAY_OF_YEAR, -30)
        setMidnight(cal)
        val startM1 = cal.timeInMillis

        // 1. Fetch Deliveries across the 90-day period
        val deliveries = deliveryDao.getDeliveriesBetweenDates(businessId, startM1, endM3)
            .filter { it.isDelivered && it.customerType != "SUPPLIER" }

        val m1Deliveries = deliveries.filter { it.deliveryDate in startM1 until startM2 }
        val m2Deliveries = deliveries.filter { it.deliveryDate in startM2 until startM3 }
        val m3Deliveries = deliveries.filter { it.deliveryDate >= startM3 }

        val m1Liters = m1Deliveries.sumOf { it.quantityLiters }
        val m2Liters = m2Deliveries.sumOf { it.quantityLiters }
        val m3Liters = m3Deliveries.sumOf { it.quantityLiters }
        val total90DayMilk = m1Liters + m2Liters + m3Liters
        val avgDailyMilk90 = if (total90DayMilk > 0) total90DayMilk / 90.0 else 0.0
        val recentDailyMilk30 = if (m3Liters > 0) m3Liters / 30.0 else avgDailyMilk90

        // Calculate Milk Trend (+/- percentage change)
        val milkTrendPercent = when {
            m1Liters > 0 -> ((m3Liters - m1Liters) / m1Liters) * 100.0
            m2Liters > 0 -> ((m3Liters - m2Liters) / m2Liters) * 100.0
            else -> 0.0
        }

        // Milk growth elasticity factor for Cattle Feed demand (e.g. 1.0 = baseline, 1.15 = +15% more feed needed)
        val milkDemandScalingFactor = if (avgDailyMilk90 > 0 && recentDailyMilk30 > 0) {
            (recentDailyMilk30 / avgDailyMilk90).coerceIn(0.75, 1.45)
        } else {
            1.0
        }

        val monthlyDeliveryStats = listOf(
            MonthlyDeliveryStats(
                yearMonth = yearMonthFormat.format(Date(startM1)),
                monthName = monthFormat.format(Date(startM1)),
                totalLiters = round2(m1Liters),
                totalDeliveries = m1Deliveries.size,
                dailyAverageLiters = round2(m1Liters / 30.0)
            ),
            MonthlyDeliveryStats(
                yearMonth = yearMonthFormat.format(Date(startM2)),
                monthName = monthFormat.format(Date(startM2)),
                totalLiters = round2(m2Liters),
                totalDeliveries = m2Deliveries.size,
                dailyAverageLiters = round2(m2Liters / 30.0)
            ),
            MonthlyDeliveryStats(
                yearMonth = yearMonthFormat.format(Date(startM3)),
                monthName = monthFormat.format(Date(startM3)),
                totalLiters = round2(m3Liters),
                totalDeliveries = m3Deliveries.size,
                dailyAverageLiters = round2(m3Liters / 30.0)
            )
        )

        // 2. Fetch Items & Transactions
        val rawItems = inventoryDao.getAllInventoryItems(businessId)
        val items = com.example.ui.util.BusinessModeFeatures.filterInventoryForMode(rawItems, businessMode)
        val allTransactions = inventoryDao.getTransactionsBetweenDates(businessId, startM1, endM3)
        val allLifetimeTransactions = inventoryDao.getAllTransactions(businessId)

        val itemForecasts = mutableListOf<ItemForecastResult>()

        for (item in items) {
            val itemTxs = allTransactions.filter { it.itemId == item.id }

            // Extract Consumption transactions in 3 periods
            val consumptionTxs = itemTxs.filter {
                it.transactionType.equals("CONSUMPTION", ignoreCase = true) ||
                it.transactionType.equals("FEEDING", ignoreCase = true) ||
                (it.transactionType.equals("ADJUSTMENT", ignoreCase = true) && it.quantity < 0)
            }

            val m1Usage = consumptionTxs.filter { it.date in startM1 until startM2 }.sumOf { Math.abs(it.quantity) }
            val m2Usage = consumptionTxs.filter { it.date in startM2 until startM3 }.sumOf { Math.abs(it.quantity) }
            val m3Usage = consumptionTxs.filter { it.date >= startM3 }.sumOf { Math.abs(it.quantity) }
            val total90DayUsage = m1Usage + m2Usage + m3Usage

            // Calculate Multi-Period Weighted Historical Daily Usage (M3 weight: 50%, M2: 30%, M1: 20%)
            val weightedHistoricalDaily = if (total90DayUsage > 0.0) {
                val dailyM3 = m3Usage / 30.0
                val dailyM2 = m2Usage / 30.0
                val dailyM1 = m1Usage / 30.0
                (dailyM3 * 0.50) + (dailyM2 * 0.30) + (dailyM1 * 0.20)
            } else {
                item.dailyUsage ?: 0.0
            }

            // Usage Trend %
            val usageTrendPercent = when {
                m1Usage > 0 -> ((m3Usage - m1Usage) / m1Usage) * 100.0
                m2Usage > 0 -> ((m3Usage - m2Usage) / m2Usage) * 100.0
                else -> 0.0
            }

            val category = categorizeItem(item.itemName, businessMode)

            // Dynamic Demand Correlation: Adjust projected daily usage based on Milk Delivery demand & category
            val effectiveProjectedDailyUsage = when (category) {
                "CATTLE_FEED" -> {
                    // Feed requirement strongly correlates with herd milk output
                    if (weightedHistoricalDaily > 0) {
                        val correlatedFactor = 1.0 + (milkDemandScalingFactor - 1.0) * 0.70
                        max(0.01, weightedHistoricalDaily * correlatedFactor)
                    } else {
                        item.dailyUsage ?: 0.0
                    }
                }
                "DAIRY_PRODUCT" -> {
                    // Dairy conversion / packaging scales directly with production
                    if (weightedHistoricalDaily > 0) {
                        max(0.01, weightedHistoricalDaily * milkDemandScalingFactor)
                    } else {
                        item.dailyUsage ?: 0.0
                    }
                }
                else -> {
                    if (weightedHistoricalDaily > 0) weightedHistoricalDaily else (item.dailyUsage ?: 0.0)
                }
            }

            // Days remaining & Run-Out Date
            val daysRemaining = if (effectiveProjectedDailyUsage > 0.001) {
                max(0.0, item.currentStock / effectiveProjectedDailyUsage)
            } else if (item.currentStock <= 0.0) {
                0.0
            } else {
                999.0
            }

            val runOutDateMillis = if (daysRemaining in 0.0..365.0) {
                now + (daysRemaining * 86_400_000L).toLong()
            } else {
                null
            }

            // Determine Urgency
            val urgencyStatus = when {
                daysRemaining <= 5.0 || item.currentStock <= 0.0 -> ForecastUrgency.CRITICAL
                daysRemaining <= 15.0 -> ForecastUrgency.WARNING
                daysRemaining <= 45.0 -> ForecastUrgency.ADEQUATE
                else -> ForecastUrgency.WELL_STOCKED
            }

            // Target Coverage & Purchase Recommendation
            val targetCoverageDays = planningHorizonDays + safetyBufferDays
            val grossRequired = targetCoverageDays * effectiveProjectedDailyUsage
            val netRequired = max(0.0, grossRequired - item.currentStock)

            // Smart Bag / Unit Rounding for Agricultural Procurement
            val (suggestedPurchaseQty, packageUnitDesc) = calculateSmartProcurementQuantity(netRequired, item.unit)

            // Supplier intelligence & pricing
            val recentPurchases = allLifetimeTransactions
                .filter { it.itemId == item.id && it.transactionType.equals("PURCHASE", ignoreCase = true) }
                .sortedByDescending { it.date }

            val lastPurchase = recentPurchases.firstOrNull()
            val lastSupplier = lastPurchase?.supplier?.ifBlank { "Local Feed Dealer" } ?: "Preferred Supplier"
            val lastPurchasePrice = when {
                lastPurchase != null && lastPurchase.unitPrice > 0 -> lastPurchase.unitPrice
                item.costPerUnit > 0 -> item.costPerUnit
                else -> 0.0
            }

            val effectiveUnitPrice = if (lastPurchasePrice > 0) lastPurchasePrice else item.costPerUnit
            val estimatedPurchaseCost = round2(suggestedPurchaseQty * effectiveUnitPrice)

            // Feed to Milk Ratio (kg feed / Litre milk)
            val feedToMilkRatio = if (category == "CATTLE_FEED" && total90DayMilk > 0 && total90DayUsage > 0) {
                round2(total90DayUsage / total90DayMilk)
            } else {
                null
            }

            // Insight Narrative
            val demandInsight = buildDemandInsight(
                item = item,
                daysRemaining = daysRemaining,
                usageTrendPercent = usageTrendPercent,
                milkTrendPercent = milkTrendPercent,
                suggestedPurchaseQty = suggestedPurchaseQty,
                unit = item.unit,
                category = category
            )

            itemForecasts.add(
                ItemForecastResult(
                    item = item,
                    category = category,
                    currentStock = round2(item.currentStock),
                    unit = item.unit,
                    costPerUnit = round2(effectiveUnitPrice),
                    m1Usage = round2(m1Usage),
                    m2Usage = round2(m2Usage),
                    m3Usage = round2(m3Usage),
                    total90DayUsage = round2(total90DayUsage),
                    historicalDailyUsage = round2(weightedHistoricalDaily),
                    effectiveProjectedDailyUsage = round2(effectiveProjectedDailyUsage),
                    usageTrendPercent = round2(usageTrendPercent),
                    daysRemaining = round1(daysRemaining),
                    runOutDateMillis = runOutDateMillis,
                    urgencyStatus = urgencyStatus,
                    suggestedPurchaseQty = round2(suggestedPurchaseQty),
                    suggestedPackageUnits = packageUnitDesc,
                    estimatedPurchaseCost = estimatedPurchaseCost,
                    lastSupplier = lastSupplier,
                    lastPurchasePrice = round2(lastPurchasePrice),
                    feedToMilkRatio = feedToMilkRatio,
                    demandInsight = demandInsight
                )
            )
        }

        // Sort items: CRITICAL first, then WARNING, then by daysRemaining ascending
        val sortedForecasts = itemForecasts.sortedWith(
            compareBy<ItemForecastResult> {
                when (it.urgencyStatus) {
                    ForecastUrgency.CRITICAL -> 0
                    ForecastUrgency.WARNING -> 1
                    ForecastUrgency.ADEQUATE -> 2
                    ForecastUrgency.WELL_STOCKED -> 3
                }
            }.thenBy { it.daysRemaining }
        )

        val criticalCount = sortedForecasts.count { it.urgencyStatus == ForecastUrgency.CRITICAL }
        val warningCount = sortedForecasts.count { it.urgencyStatus == ForecastUrgency.WARNING }
        val adequateCount = sortedForecasts.count { it.urgencyStatus == ForecastUrgency.ADEQUATE }
        val totalCost = sortedForecasts.sumOf { it.estimatedPurchaseCost }

        return ConsumptionForecastSummary(
            businessId = businessId,
            generatedAt = now,
            planningHorizonDays = planningHorizonDays,
            safetyBufferDays = safetyBufferDays,
            totalItemsAnalyzed = sortedForecasts.size,
            criticalItemsCount = criticalCount,
            warningItemsCount = warningCount,
            adequateItemsCount = adequateCount,
            totalEstimatedReorderCost = round2(totalCost),
            total90DayMilkDelivered = round2(total90DayMilk),
            avgDailyMilkDelivered = round2(avgDailyMilk90),
            milkTrendPercent = round2(milkTrendPercent),
            monthlyDeliveryStats = monthlyDeliveryStats,
            itemForecasts = sortedForecasts
        )
    }

    private fun categorizeItem(itemName: String, businessMode: String?): String {
        val showFeed = com.example.ui.util.BusinessModeFeatures.showCattleFeed(businessMode)
        if (!showFeed) {
            return "DAIRY_PRODUCT"
        }
        return if (com.example.ui.util.BusinessModeFeatures.isFeedItem(itemName)) {
            "CATTLE_FEED"
        } else {
            "DAIRY_PRODUCT"
        }
    }

    private fun calculateSmartProcurementQuantity(netRequired: Double, unit: String): Pair<Double, String> {
        if (netRequired <= 0.0) return Pair(0.0, "0 $unit (Stock Sufficient)")

        val unitLower = unit.lowercase(Locale.getDefault())
        if (unitLower == "kg") {
            // For large quantities (feed/grains), round to standard 50kg or 25kg bags
            if (netRequired >= 100.0) {
                val bags50 = ceil(netRequired / 50.0).toInt()
                val roundedQty = bags50 * 50.0
                return Pair(roundedQty, "$bags50 Bags (50kg each)")
            } else if (netRequired >= 25.0) {
                val bags25 = ceil(netRequired / 25.0).toInt()
                val roundedQty = bags25 * 25.0
                return Pair(roundedQty, "$bags25 Bags (25kg each)")
            } else {
                val roundedQty = ceil(netRequired)
                return Pair(roundedQty, "${roundedQty.toInt()} kg")
            }
        } else if (unitLower in listOf("bag", "bags", "packet", "packets", "piece", "pieces", "can", "cans", "bottle", "bottles", "litre", "liter", "l")) {
            val roundedQty = ceil(netRequired)
            return Pair(roundedQty, "${roundedQty.toInt()} $unit")
        } else {
            val roundedQty = ceil(netRequired * 10.0) / 10.0
            return Pair(roundedQty, "$roundedQty $unit")
        }
    }

    private fun buildDemandInsight(
        item: InventoryItemEntity,
        daysRemaining: Double,
        usageTrendPercent: Double,
        milkTrendPercent: Double,
        suggestedPurchaseQty: Double,
        unit: String,
        category: String
    ): String {
        return when {
            daysRemaining <= 0.0 -> {
                "🚨 OUT OF STOCK: Order ${suggestedPurchaseQty.toInt()} $unit immediately to resume cattle feeding & operations."
            }
            daysRemaining <= 5.0 -> {
                val milkFactorStr = if (milkTrendPercent > 5.0) " Herd delivery is up +${milkTrendPercent.toInt()}%, accelerating feed burn." else ""
                "⚠️ Stock depleting rapidly: Runs out in ${String.format(Locale.getDefault(), "%.1f", daysRemaining)} days.$milkFactorStr Reorder suggested before stock-out."
            }
            daysRemaining <= 15.0 -> {
                val trendStr = if (usageTrendPercent > 10.0) " Usage increased by +${usageTrendPercent.toInt()}%." else ""
                "📅 Safe for ${daysRemaining.toInt()} days.$trendStr Plan purchase of ${suggestedPurchaseQty.toInt()} $unit for uninterrupted supply."
            }
            daysRemaining > 60.0 -> {
                "✅ Well-stocked for ${daysRemaining.toInt()} days. No immediate replenishment required."
            }
            else -> {
                "✅ Healthy stock level (${daysRemaining.toInt()} days remaining). On track with seasonal usage."
            }
        }
    }

    private fun setMidnight(cal: Calendar) {
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
    }

    private fun round2(value: Double): Double = round(value * 100.0) / 100.0
    private fun round1(value: Double): Double = round(value * 10.0) / 10.0
}
