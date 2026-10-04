package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.ProfitReport
import com.example.ui.MilkMateViewModel
import com.example.ui.theme.*
import com.example.ui.util.BusinessModeFeatures
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitScreen(
    viewModel: MilkMateViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentRange by viewModel.selectedReportRange.collectAsStateWithLifecycle()
    val business by viewModel.currentBusiness.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()

    val supportedMilkTypes = business?.supportedMilkTypes ?: "BOTH"
    val isCowEnabled = BusinessModeFeatures.isCowEnabled(supportedMilkTypes)
    val isBuffaloEnabled = BusinessModeFeatures.isBuffaloEnabled(supportedMilkTypes)
    val isCowOnly = isCowEnabled && !isBuffaloEnabled
    val isBuffaloOnly = isBuffaloEnabled && !isCowEnabled

    var calcBasis by remember { mutableStateOf("BASE_2") } // BASE_1: Gross, BASE_2: Operating EBITDA, BASE_3: Cashflow
    var profitReport by remember { mutableStateOf<ProfitReport?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(currentRange, business) {
        isLoading = true
        profitReport = viewModel.getProfitReport()
        isLoading = false
    }

    val report = profitReport ?: ProfitReport(
        startDate = System.currentTimeMillis(),
        endDate = System.currentTimeMillis(),
        milkSalesRevenue = 0.0,
        farmMilkProductionValue = 0.0,
        otherSalesRevenue = 0.0,
        totalRevenue = 0.0,
        milkPurchaseCost = 0.0,
        inventoryPurchaseCost = 0.0,
        feedCost = 0.0,
        veterinaryCost = 0.0,
        breedingCost = 0.0,
        laborCost = 0.0,
        totalPurchaseCost = 0.0,
        operatingExpenses = 0.0,
        personalExpenses = 0.0,
        totalCost = 0.0,
        base1Profit = 0.0,
        base2Profit = 0.0,
        base3Profit = 0.0,
        incomeOverFeedCost = 0.0,
        totalMilkLiters = 0.0,
        costPerLiter = 0.0,
        revenuePerLiter = 0.0,
        profitPerLiter = 0.0,
        feedCostPerLiter = 0.0,
        netProfit = 0.0,
        profitMarginPercent = 0.0,
        expenseBreakdown = emptyMap()
    )

    val displayedProfit = when (calcBasis) {
        "BASE_1" -> report.base1Profit
        "BASE_3" -> report.base3Profit
        else -> report.base2Profit
    }

    val isProfitable = displayedProfit >= 0
    val heroColor = if (isProfitable) ForestGreenAccent else DangerRed

    val animalLabel = when {
        isCowOnly -> "Cow Milk Dairy"
        isBuffaloOnly -> "Buffalo Milk Dairy"
        else -> "Dairy Farm"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Top Bar ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "$animalLabel • Financial Performance".uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = RoyalBluePrimary,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "Farm Profit & Loss",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isProfitable) DairyGreenLight else Color(0xFFFFEBEE)
            ) {
                Text(
                    text = if (isProfitable) "🟢 Profitable" else "🔴 Deficit",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isProfitable) DairyGreen else DangerRed,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // --- Date Range Filter Selector ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Accounting Period", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                    }
                    val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
                    Text(
                        text = "${sdf.format(Date(report.startDate))} – ${sdf.format(Date(report.endDate))}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "THIS_MONTH" to "This Month",
                        "LAST_MONTH" to "Last Month",
                        "THIS_FY" to "This FY"
                    ).forEach { (key, label) ->
                        val isSel = currentRange == key
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) RoyalBluePrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setReportRange(key) }
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) Color.White else TextPrimary,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // --- Profit Hero Card ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isProfitable) Color(0xFF1B5E20) else Color(0xFFB71C1C)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = when (calcBasis) {
                                "BASE_1" -> "GROSS MARGIN (SALES − PURCHASES)"
                                "BASE_3" -> "CASH FLOW PROFIT (COLLECTIONS − OUTFLOW)"
                                else -> "NET FARM OPERATING PROFIT (EBITDA)"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White.copy(alpha = 0.8f),
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = String.format(Locale.US, "₹%,.0f", displayedProfit),
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (isProfitable) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Profit Margin", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                        Text(
                            text = "${report.profitMarginPercent}%",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Revenue per L", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                        Text(
                            text = "₹${String.format(Locale.US, "%.1f", report.revenuePerLiter)}/L",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Net Profit per L", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                        Text(
                            text = "₹${String.format(Locale.US, "%.1f", report.profitPerLiter)}/L",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // --- Golden Dairy Metric: Mode-Specific Prime Metric ---
        val mode = business?.businessMode ?: "FARMER"
        val showFeed = BusinessModeFeatures.showCattleFeed(mode)

        Card(
            modifier = Modifier.fillMaxWidth().testTag("profit_key_metric_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, WarmHoney.copy(alpha = 0.35f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = if (showFeed) FreshGoldLight else RoyalBlueLight, modifier = Modifier.size(28.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (showFeed) Icons.Default.Grass else Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = if (showFeed) WarmHoney else RoyalBluePrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            val metricTitle = when (mode) {
                                "TRADER" -> "Gross Trading Margin (Spread)"
                                "COLLECTION_CENTER" -> "Center Chilling Spread"
                                "PROCESSING_UNIT" -> "Byproduct Value-Add Margin"
                                "RETAIL_PARLOUR" -> "Counter Retail Gross Margin"
                                else -> "Income Over Feed Cost (IOFC)"
                            }
                            val metricSubtitle = when (mode) {
                                "TRADER" -> "Milk Sales Revenue − Farmer Procurement Cost"
                                "COLLECTION_CENTER" -> "Bulk Tanker Invoicing − Farmer Payouts"
                                "PROCESSING_UNIT" -> "Finished Product Sales − Raw Milk Intake"
                                "RETAIL_PARLOUR" -> "Counter Cash/UPI Sales − Stock Purchases"
                                else -> "Primary Dairy Producer Profit Indicator"
                            }
                            Text(metricTitle, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                            Text(metricSubtitle, fontSize = 10.sp, color = TextSecondary)
                        }
                    }

                    val mainMetricVal = if (showFeed) report.incomeOverFeedCost else (report.totalRevenue - report.milkPurchaseCost)
                    Text(
                        text = String.format(Locale.US, "₹%,.0f", mainMetricVal),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (mainMetricVal >= 0) DairyGreen else DangerRed
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                if (showFeed) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        val feedRatio = if (report.totalRevenue > 0) ((report.feedCost / report.totalRevenue) * 100).toInt() else 0
                        Column {
                            Text("Feed Cost / Liter", fontSize = 10.sp, color = TextSecondary)
                            Text("₹${String.format(Locale.US, "%.1f", report.feedCostPerLiter)} / L", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Feed % of Milk Sales", fontSize = 10.sp, color = TextSecondary)
                            Text("$feedRatio%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (feedRatio <= 55) DairyGreen else DangerRed)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Benchmark Target", fontSize = 10.sp, color = TextSecondary)
                            Text("< 50% ideal", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                        }
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        val procureCost = report.milkPurchaseCost
                        val spreadPerLiter = if (report.totalMilkLiters > 0) (report.totalRevenue - procureCost) / report.totalMilkLiters else 0.0
                        val costRatio = if (report.totalRevenue > 0) ((procureCost / report.totalRevenue) * 100).toInt() else 0
                        Column {
                            Text("Spread / Liter", fontSize = 10.sp, color = TextSecondary)
                            Text("₹${String.format(Locale.US, "%.1f", spreadPerLiter)} / L", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (spreadPerLiter >= 0) DairyGreen else DangerRed)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Sourcing Cost Ratio", fontSize = 10.sp, color = TextSecondary)
                            Text("$costRatio%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Status", fontSize = 10.sp, color = TextSecondary)
                            Text(if (spreadPerLiter > 0) "Profitable Spread" else "Break-even", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (spreadPerLiter > 0) DairyGreen else TextSecondary)
                        }
                    }
                }
            }
        }

        // --- Calculation Basis Switcher ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Calculation Accounting Model:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "BASE_2" to "Net Operating (All Exp)",
                        "BASE_1" to if (showFeed) "Gross (Sales − Feed)" else "Gross (Sales − Stock)",
                        "BASE_3" to "Cash Flow (In − Out)"
                    ).forEach { (code, label) ->
                        val isSel = calcBasis == code
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) RoyalBlueLight else Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) RoyalBluePrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { calcBasis = code }
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) RoyalBluePrimary else TextSecondary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }

        // --- Revenue & Cost Structure Breakdown ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Comprehensive Revenue & Cost Sheet", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)

                // 1. Incomes
                ProfitBreakdownLine(
                    icon = Icons.Default.WaterDrop,
                    title = "Milk Sales & Dispatches (${String.format(Locale.US, "%.1f", report.totalMilkLiters)}L)",
                    value = String.format(Locale.US, "+₹%,.0f", report.milkSalesRevenue),
                    valueColor = DairyGreen,
                    isPositive = true
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                if (BusinessModeFeatures.showHerdBreeding(mode)) {
                    // Cattle Farm Producer Breakdown
                    // 2. Feed Cost (Actual Consumption)
                    ProfitBreakdownLine(
                        icon = Icons.Default.Grass,
                        title = "Cattle Feed & Consumables (Actual Consumption)",
                        value = String.format(Locale.US, "-₹%,.0f", report.actualFeedConsumptionCost),
                        valueColor = if (report.actualFeedConsumptionCost > 0) DangerRed else TextSecondary,
                        isPositive = false
                    )

                    // 3. Veterinary & Meds
                    ProfitBreakdownLine(
                        icon = Icons.Default.MedicalServices,
                        title = "Veterinary, Meds & Deworming",
                        value = String.format(Locale.US, "-₹%,.0f", report.veterinaryCost),
                        valueColor = if (report.veterinaryCost > 0) DangerRed else TextSecondary,
                        isPositive = false
                    )

                    // 4. Breeding & A.I.
                    ProfitBreakdownLine(
                        icon = Icons.Default.Pets,
                        title = "Breeding & A.I. Semen Straws",
                        value = String.format(Locale.US, "-₹%,.0f", report.breedingCost),
                        valueColor = if (report.breedingCost > 0) DangerRed else TextSecondary,
                        isPositive = false
                    )

                    // 5. Labor & Milker
                    ProfitBreakdownLine(
                        icon = Icons.Default.People,
                        title = "Farm Labor & Milker Wages",
                        value = String.format(Locale.US, "-₹%,.0f", report.laborCost),
                        valueColor = if (report.laborCost > 0) DangerRed else TextSecondary,
                        isPositive = false
                    )

                    // 6. Milk Wastage / Spoilage Loss
                    if (report.milkWastageCost > 0) {
                        ProfitBreakdownLine(
                            icon = Icons.Default.DeleteSweep,
                            title = "Milk Wastage & Spoilage Loss (${String.format(Locale.US, "%.1f", report.milkWastageLiters)}L)",
                            value = String.format(Locale.US, "-₹%,.0f", report.milkWastageCost),
                            valueColor = DangerRed,
                            isPositive = false
                        )
                    }

                    // 7. Other Farm Overheads
                    val otherExp = (report.operatingExpenses - report.veterinaryCost - report.breedingCost - report.laborCost).coerceAtLeast(0.0)
                    ProfitBreakdownLine(
                        icon = Icons.Default.ElectricBolt,
                        title = "Electricity, Diesel & Shed Maintenance",
                        value = String.format(Locale.US, "-₹%,.0f", otherExp),
                        valueColor = if (otherExp > 0) DangerRed else TextSecondary,
                        isPositive = false
                    )
                } else {
                    // Trader / Aggregator / Processing / Retail Breakdown
                    // 2. Farmer Milk Procurement Outlay
                    ProfitBreakdownLine(
                        icon = Icons.Default.AddShoppingCart,
                        title = if (mode == "RETAIL_PARLOUR" || mode == "PROCESSING_UNIT") "Stock & Raw Milk Purchase" else "Farmer Milk Procurement Outlay",
                        value = String.format(Locale.US, "-₹%,.0f", report.milkPurchaseCost),
                        valueColor = if (report.milkPurchaseCost > 0) DangerRed else TextSecondary,
                        isPositive = false
                    )

                    // 3. Milk Wastage / Spoilage Loss
                    if (report.milkWastageCost > 0) {
                        ProfitBreakdownLine(
                            icon = Icons.Default.DeleteSweep,
                            title = "Milk Spoilage / Wastage Loss (${String.format(Locale.US, "%.1f", report.milkWastageLiters)}L)",
                            value = String.format(Locale.US, "-₹%,.0f", report.milkWastageCost),
                            valueColor = DangerRed,
                            isPositive = false
                        )
                    }

                    // 4. Logistics & Fuel
                    ProfitBreakdownLine(
                        icon = Icons.Default.LocalShipping,
                        title = "Transport, Route Fuel & Delivery Freight",
                        value = String.format(Locale.US, "-₹%,.0f", report.operatingExpenses * 0.4),
                        valueColor = if (report.operatingExpenses > 0) DangerRed else TextSecondary,
                        isPositive = false
                    )

                    // 5. Staff & Helper Salaries
                    ProfitBreakdownLine(
                        icon = Icons.Default.People,
                        title = "Staff, Drivers & Center Helpers",
                        value = String.format(Locale.US, "-₹%,.0f", report.laborCost),
                        valueColor = if (report.laborCost > 0) DangerRed else TextSecondary,
                        isPositive = false
                    )

                    // 6. Rent & Equipment Maintenance
                    val tradeOverheads = (report.operatingExpenses * 0.6 - report.laborCost).coerceAtLeast(0.0)
                    ProfitBreakdownLine(
                        icon = Icons.Default.Build,
                        title = "Can Washing, Rent & Maintenance",
                        value = String.format(Locale.US, "-₹%,.0f", tradeOverheads),
                        valueColor = if (tradeOverheads > 0) DangerRed else TextSecondary,
                        isPositive = false
                    )
                }

                HorizontalDivider(thickness = 1.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                // Net Result
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (BusinessModeFeatures.showHerdBreeding(mode)) "NET DAIRY FARM PROFIT" else "NET BUSINESS PROFIT",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = String.format(Locale.US, "₹%,.0f", displayedProfit),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = heroColor
                    )
                }
            }
        }

        // --- Export & WhatsApp Share Actions ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val shareSummary = buildString {
                        val isFarm = BusinessModeFeatures.showHerdBreeding(mode)
                        append("📊 *${if (isFarm) "MILKMATE DAIRY FARM P&L" else "MILKMATE DAIRY BUSINESS P&L"}*\n")
                        append("🏢 Business: ${business?.businessName ?: "My Dairy"}\n")
                        append("📅 Period: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(report.startDate))} to ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(report.endDate))}\n\n")
                        append("🥛 Total Volume: ${String.format(Locale.US, "%.1f", report.totalMilkLiters)} Liters\n")
                        append("💵 Total Revenue: ₹${String.format(Locale.US, "%,.0f", report.totalRevenue)}\n")
                        if (isFarm) {
                            append("🌾 Total Feed Cost: ₹${String.format(Locale.US, "%,.0f", report.feedCost)}\n")
                            append("💉 Medical/Vet: ₹${String.format(Locale.US, "%,.0f", report.veterinaryCost)}\n")
                            append("👨‍🌾 Farm Labor: ₹${String.format(Locale.US, "%,.0f", report.laborCost)}\n")
                        } else {
                            append("🥛 Stock / Milk Purchases: ₹${String.format(Locale.US, "%,.0f", report.milkPurchaseCost)}\n")
                            append("💼 Operating Costs: ₹${String.format(Locale.US, "%,.0f", report.operatingExpenses)}\n")
                        }
                        append("⚡ Total Operating Cost: ₹${String.format(Locale.US, "%,.0f", report.totalCost)}\n")
                        append("----------------------------\n")
                        append("💰 *NET PROFIT: ₹${String.format(Locale.US, "%,.0f", displayedProfit)}* (${report.profitMarginPercent}% Margin)\n")
                        if (isFarm) {
                            append("📈 Income Over Feed Cost (IOFC): ₹${String.format(Locale.US, "%,.0f", report.incomeOverFeedCost)}\n")
                        }
                        append("📊 Cost per Liter: ₹${String.format(Locale.US, "%.1f", report.costPerLiter)}/L\n")
                    }
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareSummary)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share Profit Statement"))
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share P&L", fontSize = 12.sp)
            }

            Button(
                onClick = {
                    viewModel.exportProfitPdf { file ->
                        val intent = viewModel.repository.exportManager.shareFileIntent(file, "application/pdf")
                        context.startActivity(Intent.createChooser(intent, "Download PDF P&L"))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export PDF", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ProfitBreakdownLine(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    valueColor: Color,
    isPositive: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = if (isPositive) DairyGreen else TextSecondary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, fontSize = 12.sp, color = TextPrimary)
        }
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}
