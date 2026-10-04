package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.DeliveryEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.InventoryItemEntity
import com.example.data.local.entity.PaymentEntity
import com.example.ui.MilkMateViewModel
import com.example.ui.theme.*
import com.example.ui.util.BusinessModeFeatures
import com.example.ui.util.loc
import com.example.ui.util.appStr
import java.text.SimpleDateFormat
import java.util.*

enum class ChartTimeRange(val label: String, val days: Int) {
    TODAY("Today (Live)", 1),
    WEEK_7D("7-Day Trend", 7),
    MONTH_30D("30-Day Output", 30)
}

enum class ProductionChartType(val label: String, val icon: ImageVector) {
    DUAL_BAR("Cow vs Buffalo", Icons.Default.BarChart),
    STACKED("Stacked Total", Icons.Default.StackedBarChart),
    RATIO_DONUT("Ratio Split", Icons.Default.PieChart)
}

data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)

data class DashboardSlideItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val themeColor: Color
)

data class DailyProductionPoint(
    val dayLabel: String,
    val fullDate: String,
    val cowLiters: Double,
    val buffLiters: Double,
    val totalLiters: Double,
    val cowRevenue: Double,
    val buffRevenue: Double,
    val totalRevenue: Double,
    val isToday: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MilkMateViewModel,
    onNavigateToTab: (Int) -> Unit,
    onOpenProfit: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenManage: () -> Unit,
    onOpenExpenses: () -> Unit = {},
    onOpenPayments: () -> Unit = {},
    onOpenOrders: () -> Unit = {},
    onOpenCollectionDesk: () -> Unit = {},
    onOpenCattleBreeding: () -> Unit = {},
    onOpenBusinessModeSelector: () -> Unit = {},
    onOpenAIAssistant: ((String?) -> Unit)? = null,
    onOpenSettings: () -> Unit = {},
    onOpenDrawer: () -> Unit = {}
) {
    val business by viewModel.currentBusiness.collectAsStateWithLifecycle()
    val currentMode = business?.businessMode ?: "FARMER"
    val todayDeliveries by viewModel.todayDeliveries.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val inventoryItems by viewModel.inventoryItems.collectAsStateWithLifecycle()
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val selectedShift by viewModel.selectedShift.collectAsStateWithLifecycle()
    
    // Farmer Collections & Dispatches flows
    val farmers by viewModel.farmers.collectAsStateWithLifecycle()
    val todayCollections by viewModel.todayMilkCollections.collectAsStateWithLifecycle()
    val allCollections by viewModel.allMilkCollections.collectAsStateWithLifecycle()
    val bulkDispatches by viewModel.bulkDispatches.collectAsStateWithLifecycle()
    val cattleList by viewModel.cattleList.collectAsStateWithLifecycle()
    val allMilkingRecords by viewModel.allMilkingRecords.collectAsStateWithLifecycle()
    val allDeliveries by viewModel.allDeliveries.collectAsStateWithLifecycle()

    val supportedTypes = business?.supportedMilkTypes ?: "BOTH"
    val isCowEnabled = BusinessModeFeatures.isCowEnabled(supportedTypes)
    val isBuffaloEnabled = BusinessModeFeatures.isBuffaloEnabled(supportedTypes)

    var selectedDayIndex by remember { mutableIntStateOf(3) }
    var showCustomFormulaStudio by remember { mutableStateOf(false) }

    val todayMidnight = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val todayEnd = remember(todayMidnight) { todayMidnight + 86400000L - 1L }

    // Milk Volumes (Live Today)
    val milkSoldDeliveries = todayDeliveries.filter { it.customerType != "SUPPLIER" && it.isDelivered }
    val cowSoldLiters = if (isCowEnabled) milkSoldDeliveries.filter { it.milkType == "COW" }.sumOf { it.quantityLiters } else 0.0
    val buffSoldLiters = if (isBuffaloEnabled) milkSoldDeliveries.filter { it.milkType == "BUFFALO" }.sumOf { it.quantityLiters } else 0.0
    val milkSoldLiters = cowSoldLiters + buffSoldLiters

    val milkProcuredDeliveries = todayDeliveries.filter { it.customerType == "SUPPLIER" && it.isDelivered }
    val milkPurchasedLiters = milkProcuredDeliveries.sumOf { it.quantityLiters }

    // Financial Metrics
    val totalSales = milkSoldDeliveries.sumOf { it.totalAmount }
    val supplierMilkPurchases = milkProcuredDeliveries.sumOf { it.totalAmount }
    val todayInventoryPurchases = expenses.filter {
        it.date in todayMidnight..todayEnd && (it.isInventoryPurchase || it.category == "INVENTORY_PURCHASE")
    }.sumOf { it.amount }
    val totalPurchases = supplierMilkPurchases + todayInventoryPurchases
    val totalTodayExpenses = expenses.filter {
        it.date in todayMidnight..todayEnd && it.isBusiness()
    }.sumOf { it.amount }
    val todayPersonalExpenses = expenses.filter {
        it.date in todayMidnight..todayEnd && it.isPersonal()
    }.sumOf { it.amount }

    val netCashMovement = totalSales - totalPurchases - totalTodayExpenses - todayPersonalExpenses

    // Outstanding Dues & Balances
    val totalReceivable = customers.filter { it.outstandingBalance > 0 }.sumOf { it.outstandingBalance }
    val totalPayable = customers.filter { it.outstandingBalance < 0 }.sumOf { -it.outstandingBalance }
    val todayCollectedPayments = payments.filter { it.date in todayMidnight..todayEnd }.sumOf { it.amount }

    // Delivery progress
    val totalCustomerTarget = customers.count { it.type != "SUPPLIER" }
    val completedDeliveries = milkSoldDeliveries.size

    // Inventory & Orders Status
    val lowStockItems = inventoryItems.filter { it.stockAlertStatus != "NORMAL" }
    val lowStockCount = lowStockItems.size

    // Shift Collections
    val shiftCollections = todayCollections.filter { it.shift == selectedShift }
    val totalCollectedLiters = shiftCollections.sumOf { it.quantityLiters }
    val avgProcureFat = if (shiftCollections.isNotEmpty()) shiftCollections.map { it.fat }.average() else 0.0
    val totalDispatchedLiters = bulkDispatches.filter { it.dateEpochMidnight == todayMidnight }.sumOf { it.totalLiters }

    // Cattle summary
    val scopedDashboardCattle = remember(cattleList, supportedTypes) {
        when (supportedTypes) {
            "COW_ONLY" -> cattleList.filter { it.type.equals("COW", ignoreCase = true) }
            "BUFFALO_ONLY" -> cattleList.filter { it.type.equals("BUFFALO", ignoreCase = true) }
            else -> cattleList
        }
    }
    val lactatingCattle = scopedDashboardCattle.count { it.lactationStage == "LACTATING" }
    val totalCattle = scopedDashboardCattle.size

    // 7-day trend data
    val productionDataPoints = remember(allMilkingRecords, cattleList, allDeliveries, business, supportedTypes) {
        val cowRate = business?.cowMilkRate ?: 55.0
        val buffRate = business?.buffaloMilkRate ?: 75.0
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("EEE", Locale.getDefault())
        val fullSdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val buffaloIdSet = cattleList.filter { it.type.equals("BUFFALO", ignoreCase = true) }.map { it.id }.toSet()

        (6 downTo 0).map { daysAgo ->
            val dayCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -daysAgo)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startMs = dayCal.timeInMillis
            val endMs = startMs + 86400000L - 1L

            val dayMilking = allMilkingRecords.filter { it.dateEpochMidnight in startMs..endMs }
            val dayDeliveries = allDeliveries.filter { it.deliveryDate in startMs..endMs && it.isDelivered }

            val cowL = if (isCowEnabled) {
                val farmY = dayMilking.filter { it.cattleId !in buffaloIdSet }.sumOf { it.quantityLiters }
                if (farmY > 0.0) farmY else dayDeliveries.filter { it.milkType == "COW" }.sumOf { it.quantityLiters }
            } else 0.0

            val buffL = if (isBuffaloEnabled) {
                val farmY = dayMilking.filter { it.cattleId in buffaloIdSet || it.cattleTag.contains("BUF", true) }.sumOf { it.quantityLiters }
                if (farmY > 0.0) farmY else dayDeliveries.filter { it.milkType == "BUFFALO" }.sumOf { it.quantityLiters }
            } else 0.0

            val tot = cowL + buffL
            DailyProductionPoint(
                dayLabel = sdf.format(Date(startMs)),
                fullDate = fullSdf.format(Date(startMs)),
                cowLiters = cowL,
                buffLiters = buffL,
                totalLiters = tot,
                cowRevenue = cowL * cowRate,
                buffRevenue = buffL * buffRate,
                totalRevenue = (cowL * cowRate) + (buffL * buffRate),
                isToday = daysAgo == 0
            )
        }
    }

    val todayFormatted = remember {
        SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date())
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBg)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ================= 1. CLEAN TOP HEADER WITH LIVE SHIFT SWITCH =================
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = (business?.businessName ?: "MilkMate Dairy").uppercase(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = RoyalBluePrimary,
                        letterSpacing = 0.5.sp,
                        maxLines = 1
                    )
                    Text(
                        text = todayFormatted,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Tactile Big Shift Toggle Button
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (selectedShift == "MORNING") FreshGoldLight else RoyalBlueLight,
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (selectedShift == "MORNING") WarmHoney else RoyalBluePrimary
                        ),
                        modifier = Modifier.clickable {
                            val next = if (selectedShift == "MORNING") "EVENING" else "MORNING"
                            viewModel.setShift(next)
                            viewModel.showMessage("Switched to $next Shift")
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (selectedShift == "MORNING") "🌅 MORNING" else "🌆 EVENING",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (selectedShift == "MORNING") WarmHoney else RoyalBluePrimary
                            )
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Switch Shift",
                                tint = if (selectedShift == "MORNING") WarmHoney else RoyalBluePrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    // Reload/Demo Data Button
                    IconButton(
                        onClick = { viewModel.populateDemoData() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Reload Data",
                            tint = RoyalBluePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // ================= 2. HERO TODAY SNAPSHOT (3 BIG PILLARS) =================
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(listOf(OceanMidnight, RoyalBluePrimary)),
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = FreshGold, modifier = Modifier.size(18.dp))
                            Text("TODAY'S DAIRY SUMMARY", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.clickable { onOpenProfit() }
                        ) {
                            Text("Full P&L →", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }

                    // 3-Column Pillar Stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 1. Milk Volume
                        Column {
                            val volumeLabel = if (currentMode == "COLLECTION_CENTER" || currentMode == "TRADER") "Sourced / Intake" else "Milk Output / Sold"
                            val volumeVal = if (currentMode == "COLLECTION_CENTER") totalCollectedLiters else if (milkSoldLiters > 0) milkSoldLiters else totalCollectedLiters
                            Text(volumeLabel, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                            Text(
                                text = "${String.format(Locale.US, "%.1f", volumeVal)} L",
                                color = FreshGold,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = if (selectedShift == "MORNING") "🌅 Morning Shift" else "🌆 Evening Shift",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 9.5.sp
                            )
                        }

                        // 2. Today's Revenue / Cash In
                        Column {
                            Text("Sales / Cash In", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                            Text(
                                text = "₹${totalSales.toInt()}",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "₹${todayCollectedPayments.toInt()} Collected",
                                color = GrassGreen,
                                fontSize = 9.5.sp
                            )
                        }

                        // 3. Pending Khata Dues
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Pending Khata Dues", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                            Text(
                                text = "₹${totalReceivable.toInt()}",
                                color = if (totalReceivable > 0) Color(0xFFFF8A80) else Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "${customers.count { it.outstandingBalance > 0 }} Parties Due",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 9.5.sp
                            )
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

                    // Quick Net Margin Strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = if (netCashMovement >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = if (netCashMovement >= 0) Color(0xFF4ADE80) else Color(0xFFFF8A80),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Net Operating Margin: ₹${netCashMovement.toInt()}",
                                color = if (netCashMovement >= 0) Color(0xFF4ADE80) else Color(0xFFFF8A80),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = if (totalTodayExpenses > 0) "Expenses: ₹${totalTodayExpenses.toInt()}" else "No Expenses Logged",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 10.5.sp
                        )
                    }
                }
            }
        }

        // ================= 3. ACTIVE RATE ENGINE & FORMULA STRIP =================
        item {
            val biz = business
            val formulaLabel = when (biz?.pricingMode?.uppercase()) {
                "PANEER_YIELD" -> "🧀 Paneer Yield Pricing"
                "KHOA_YIELD", "KHOYA_YIELD" -> "🍬 Khoya Yield Pricing"
                "GHEE_YIELD" -> "🏺 Ghee Fat Recovery Pricing"
                "FAT_SNF" -> "🧪 Cooperative FAT + SNF Matrix"
                "FAT_ONLY" -> "🧈 Pure Fat Multiplier"
                "CUSTOM" -> "⚙️ Custom Formula: ${biz.customFormulaName}"
                else -> "💵 Flat Base Rate (Cow ₹${biz?.cowMilkRate ?: 50.0} / Buffalo ₹${biz?.buffaloMilkRate ?: 65.0})"
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                shadowElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCustomFormulaStudio = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = CircleShape,
                            color = RoyalBlueLight,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Calculate, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(16.dp))
                            }
                        }
                        Column {
                            Text("Active Milk Rate Engine", fontSize = 10.sp, color = TextSecondary)
                            Text(formulaLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1)
                        }
                    }
                    Text("Change ⚙️", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                }
            }
        }

        // ================= 4. BIG 4 PRIMARY ACTIONS (2x2 LARGE TILES) =================
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Quick Actions",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Action 1: New Milk Entry
                    val entryTitle = if (currentMode == "RETAIL_PARLOUR") "🛒 Counter POS" else "🥛 New Milk Entry"
                    val entrySub = if (currentMode == "RETAIL_PARLOUR") "Fast Cash & UPI Sale" else "Record shift collection"
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = DairyGreenLight,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DairyGreen.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(95.dp)
                            .clickable {
                                if (currentMode == "RETAIL_PARLOUR") onNavigateToTab(1) else onOpenCollectionDesk()
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(shape = CircleShape, color = DairyGreen, modifier = Modifier.size(32.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            if (currentMode == "RETAIL_PARLOUR") Icons.Default.PointOfSale else Icons.Default.LocalDrink,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(16.dp))
                            }
                            Column {
                                Text(entryTitle, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = TextPrimary)
                                Text(entrySub, fontSize = 10.sp, color = TextSecondary, maxLines = 1)
                            }
                        }
                    }

                    // Action 2: Delivery & Route
                    val deliveryTitle = if (currentMode == "COLLECTION_CENTER") "🚚 Tanker Dispatch" else "🚚 Deliveries"
                    val deliverySub = if (currentMode == "COLLECTION_CENTER") "${bulkDispatches.size} Dispatched" else "$completedDeliveries/$totalCustomerTarget Completed"
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = RoyalBlueLight,
                        border = androidx.compose.foundation.BorderStroke(1.dp, RoyalBluePrimary.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(95.dp)
                            .clickable { onNavigateToTab(1) }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(shape = CircleShape, color = RoyalBluePrimary, modifier = Modifier.size(32.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.LocalShipping, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(16.dp))
                            }
                            Column {
                                Text(deliveryTitle, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = TextPrimary)
                                Text(deliverySub, fontSize = 10.sp, color = TextSecondary, maxLines = 1)
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Action 3: Record Payment
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = FreshGoldLight,
                        border = androidx.compose.foundation.BorderStroke(1.dp, WarmHoney.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(95.dp)
                            .clickable { onOpenPayments() }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(shape = CircleShape, color = WarmHoney, modifier = Modifier.size(32.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Payment, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = WarmHoney, modifier = Modifier.size(16.dp))
                            }
                            Column {
                                Text("💵 Record Payment", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = TextPrimary)
                                Text("Cash, UPI & Bank", fontSize = 10.sp, color = TextSecondary, maxLines = 1)
                            }
                        }
                    }

                    // Action 4: Khata / Parties
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFEDE7F6),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF673AB7).copy(alpha = 0.3f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(95.dp)
                            .clickable { onNavigateToTab(2) }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(shape = CircleShape, color = Color(0xFF673AB7), modifier = Modifier.size(32.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.People, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color(0xFF673AB7), modifier = Modifier.size(16.dp))
                            }
                            Column {
                                Text("👥 Parties & Khata", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = TextPrimary)
                                Text("${customers.size} Accounts", fontSize = 10.sp, color = TextSecondary, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        // ================= 5. UNIFIED MODULES & OPERATIONS HUB (4 LOGICAL GROUPS) =================
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header with Drawer Quick Launch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Dairy Operations Hub",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Direct access to all 4 business modules",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = RoyalBlueLight,
                            modifier = Modifier.clickable { onOpenDrawer() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Menu, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(16.dp))
                                Text("Drawer ☰", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                            }
                        }
                    }

                    // GROUP 1: MANAGE SALES
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = DairyGreenLight.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DairyGreen.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(16.dp))
                                Text("MANAGE SALES & SOURCING", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, color = DairyGreen)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    modifier = Modifier.weight(1f).clickable { onOpenCollectionDesk() }
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("🥛 Intake Desk", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text("Record Milk", fontSize = 9.sp, color = TextSecondary)
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    modifier = Modifier.weight(1f).clickable { onNavigateToTab(1) }
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("🚚 Deliveries", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text("Route Drops", fontSize = 9.sp, color = TextSecondary)
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    modifier = Modifier.weight(1f).clickable { onNavigateToTab(2) }
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("👥 Khata Book", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text("${customers.size} Parties", fontSize = 9.sp, color = TextSecondary)
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    modifier = Modifier.weight(1f).clickable { onOpenOrders() }
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("📋 Orders", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text("Daily Slips", fontSize = 9.sp, color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }

                    // GROUP 2: INVENTORY CONTROL
                    val showCattle = BusinessModeFeatures.showHerdBreeding(currentMode)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = FreshGoldLight.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WarmHoney.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Inventory2, contentDescription = null, tint = WarmHoney, modifier = Modifier.size(16.dp))
                                Text("INVENTORY CONTROL", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, color = WarmHoney)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    modifier = Modifier.weight(1f).clickable { onOpenInventory() }
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("🌾 Feed & Stock", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text("${inventoryItems.size} Items Tracked", fontSize = 9.sp, color = TextSecondary)
                                    }
                                }
                                if (showCattle) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.White,
                                        modifier = Modifier.weight(1f).clickable { onOpenCattleBreeding() }
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("🐄 Cattle Herd", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Text("$totalCattle Animals", fontSize = 9.sp, color = TextSecondary)
                                        }
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    modifier = Modifier.weight(1f).clickable { onOpenManage() }
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("🛠️ Operations Hub", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text("Shifts & Routes", fontSize = 9.sp, color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }

                    // GROUP 3: FINANCIAL REPORTS
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = RoyalBlueLight.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RoyalBluePrimary.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(16.dp))
                                Text("FINANCIAL REPORTS", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, color = RoyalBluePrimary)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    modifier = Modifier.weight(1f).clickable { onOpenPayments() }
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("💳 Payments", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text("Cash & UPI", fontSize = 9.sp, color = TextSecondary)
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    modifier = Modifier.weight(1f).clickable { onOpenExpenses() }
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("🧾 Expenses", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text("₹${totalTodayExpenses.toInt()}", fontSize = 9.sp, color = TextSecondary)
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    modifier = Modifier.weight(1f).clickable { onOpenProfit() }
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("📈 P&L Spread", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text("Net Margins", fontSize = 9.sp, color = TextSecondary)
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    modifier = Modifier.weight(1f).clickable { onOpenReports() }
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("📑 Analytics", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text("PDF Exports", fontSize = 9.sp, color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }

                    // GROUP 4: SETTINGS & TOOLS
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF3E5F5),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF9C27B0).copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF9C27B0), modifier = Modifier.size(16.dp))
                                Text("SETTINGS & TOOLS", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, color = Color(0xFF9C27B0))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    modifier = Modifier.weight(1f).clickable { showCustomFormulaStudio = true }
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("🧪 Milk Rates", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text("FAT/SNF Rules", fontSize = 9.sp, color = TextSecondary)
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    modifier = Modifier.weight(1f).clickable {
                                        onOpenAIAssistant?.invoke("How can I optimize milk yield, reduce feed cost and maximize profit?")
                                    }
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("🤖 AI Advisor", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text("Smart Tips", fontSize = 9.sp, color = TextSecondary)
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    modifier = Modifier.weight(1f).clickable { onOpenSettings() }
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("⚙️ Preferences", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text("Profile & Staff", fontSize = 9.sp, color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ================= 6. 7-DAY VOLUME & REVENUE TREND =================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("📊 7-Day Production & Sales Trend", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                            Text("Tap any bar to view day details", fontSize = 10.sp, color = TextSecondary)
                        }
                        TextButton(onClick = onOpenReports) {
                            Text("Reports →", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                        }
                    }

                    InteractiveDualBarChart(
                        dataPoints = productionDataPoints,
                        selectedIndex = selectedDayIndex,
                        onSelect = { selectedDayIndex = it },
                        supportedMilkTypes = supportedTypes
                    )
                }
            }
        }

        // ================= 7. TODAY'S RECENT ENTRIES & ACTIVITY =================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⏱️ Today's Recent Transactions", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                        TextButton(onClick = onOpenPayments) {
                            Text("View All →", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                        }
                    }

                    val recentActivities = remember(todayCollections, milkSoldDeliveries, payments) {
                        val list = mutableListOf<String>()
                        todayCollections.take(3).forEach { c ->
                            list.add("🥛 Procured ${String.format(Locale.US, "%.1f", c.quantityLiters)}L from Farmer #${c.farmerId} • ₹${c.totalAmount.toInt()}")
                        }
                        milkSoldDeliveries.take(3).forEach { d ->
                            list.add("🚚 Delivered ${String.format(Locale.US, "%.1f", d.quantityLiters)}L to Customer #${d.customerId} • ₹${d.totalAmount.toInt()}")
                        }
                        payments.filter { it.date in todayMidnight..todayEnd }.take(2).forEach { p ->
                            list.add("💵 Payment received ₹${p.amount.toInt()} via ${p.method}")
                        }
                        list.take(4)
                    }

                    if (recentActivities.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("No entries recorded yet today", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                                FilledTonalButton(
                                    onClick = { viewModel.populateDemoData() },
                                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = DairyGreenLight),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Load Sample Data to Test", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DairyGreen)
                                }
                            }
                        }
                    } else {
                        recentActivities.forEach { act ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SurfaceBg,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = act,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }





    if (showCustomFormulaStudio) {
        CustomFormulaStudioDialog(
            business = business,
            viewModel = viewModel,
            onDismiss = { showCustomFormulaStudio = false }
        )
    }
}

// -------------------------------------------------------------
// NATIVE COMPOSE CHART RENDERERS (Cow vs Buffalo Interactive Analytics)
// -------------------------------------------------------------

@Composable
fun InteractiveDualBarChart(
    dataPoints: List<DailyProductionPoint>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    supportedMilkTypes: String = "BOTH"
) {
    val maxSingle = remember(dataPoints) {
        val maxC = dataPoints.maxOfOrNull { it.cowLiters } ?: 50.0
        val maxB = dataPoints.maxOfOrNull { it.buffLiters } ?: 50.0
        maxOf(maxC, maxB).coerceAtLeast(10.0)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            dataPoints.forEachIndexed { index, point ->
                val isSelected = index == selectedIndex
                val cowFraction = (point.cowLiters / maxSingle).toFloat().coerceIn(0.08f, 1.0f)
                val buffFraction = (point.buffLiters / maxSingle).toFloat().coerceIn(0.08f, 1.0f)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(index) }
                        .padding(horizontal = 2.dp)
                ) {
                    // Volume tooltip tag on selected
                    if (isSelected) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = RoyalBluePrimary
                        ) {
                            Text(
                                text = "${point.totalLiters.toInt()}L",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                    } else {
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Dual Bars Container
                    Row(
                        modifier = Modifier.height(84.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val barWidth = if (supportedMilkTypes == "BOTH") 10.dp else 16.dp
                        // Cow Bar
                        if (supportedMilkTypes == "COW_ONLY" || supportedMilkTypes == "BOTH") {
                            Box(
                                modifier = Modifier
                                    .width(barWidth)
                                    .fillMaxHeight(cowFraction)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        if (isSelected) RoyalBlueSecondary else RoyalBlueSecondary.copy(alpha = 0.45f)
                                    )
                            )
                        }
                        // Buffalo Bar
                        if (supportedMilkTypes == "BUFFALO_ONLY" || supportedMilkTypes == "BOTH") {
                            Box(
                                modifier = Modifier
                                    .width(barWidth)
                                    .fillMaxHeight(buffFraction)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        if (isSelected) GoldenOrange else GoldenOrange.copy(alpha = 0.45f)
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = point.dayLabel,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) RoyalBluePrimary else TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun InteractiveStackedBarChart(
    dataPoints: List<DailyProductionPoint>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    supportedMilkTypes: String = "BOTH"
) {
    val maxTotal = remember(dataPoints) {
        (dataPoints.maxOfOrNull { it.totalLiters } ?: 100.0).coerceAtLeast(10.0)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            dataPoints.forEachIndexed { index, point ->
                val isSelected = index == selectedIndex
                val totalFraction = (point.totalLiters / maxTotal).toFloat().coerceIn(0.1f, 1.0f)
                val cowShare = if (point.totalLiters > 0) (point.cowLiters / point.totalLiters).toFloat() else 0.5f
                val buffShare = 1f - cowShare

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(index) }
                        .padding(horizontal = 2.dp)
                ) {
                    if (isSelected) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = RoyalBluePrimary
                        ) {
                            Text(
                                text = "${point.totalLiters.toInt()}L",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                    } else {
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Stacked Bar
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height(84.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Column(
                            modifier = Modifier
                                .width(18.dp)
                                .fillMaxHeight(totalFraction)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        ) {
                            if (supportedMilkTypes == "BUFFALO_ONLY") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight()
                                        .background(if (isSelected) GoldenOrange else GoldenOrange.copy(alpha = 0.5f))
                                )
                            } else if (supportedMilkTypes == "COW_ONLY") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight()
                                        .background(if (isSelected) RoyalBlueSecondary else RoyalBlueSecondary.copy(alpha = 0.5f))
                                )
                            } else {
                                // Top part = Buffalo
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(buffShare.coerceAtLeast(0.01f))
                                        .background(if (isSelected) GoldenOrange else GoldenOrange.copy(alpha = 0.5f))
                                )
                                // Bottom part = Cow
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(cowShare.coerceAtLeast(0.01f))
                                        .background(if (isSelected) RoyalBlueSecondary else RoyalBlueSecondary.copy(alpha = 0.5f))
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = point.dayLabel,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) RoyalBluePrimary else TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun InteractiveProductionDonut(
    cowLiters: Double,
    buffLiters: Double,
    morningLiters: Double = 0.0,
    eveningLiters: Double = 0.0,
    cowRate: Double,
    buffRate: Double,
    supportedMilkTypes: String = "BOTH"
) {
    val isCowOnly = supportedMilkTypes == "COW_ONLY"
    val isBuffOnly = supportedMilkTypes == "BUFFALO_ONLY"

    val (slice1Percent, slice2Percent, slice1Color, slice2Color, slice1Label, slice2Label, centerRatioText, centerSubText) = when {
        isCowOnly -> {
            val total = (morningLiters + eveningLiters).coerceAtLeast(1.0)
            val mFrac = (morningLiters / total).toFloat()
            val eFrac = (eveningLiters / total).toFloat()
            Tuple8(
                mFrac, eFrac,
                FreshGold, RoyalBlueSecondary,
                "🌅 Morning Cow: ${String.format(Locale.US, "%.1f L", morningLiters)} (${(mFrac * 100).toInt()}%)",
                "🌆 Evening Cow: ${String.format(Locale.US, "%.1f L", eveningLiters)} (${(eFrac * 100).toInt()}%)",
                "${(mFrac * 100).toInt()}% / ${(eFrac * 100).toInt()}%",
                "Morn / Eve"
            )
        }
        isBuffOnly -> {
            val total = (morningLiters + eveningLiters).coerceAtLeast(1.0)
            val mFrac = (morningLiters / total).toFloat()
            val eFrac = (eveningLiters / total).toFloat()
            Tuple8(
                mFrac, eFrac,
                FreshGold, GoldenOrange,
                "🌅 Morning Buff: ${String.format(Locale.US, "%.1f L", morningLiters)} (${(mFrac * 100).toInt()}%)",
                "🌆 Evening Buff: ${String.format(Locale.US, "%.1f L", eveningLiters)} (${(eFrac * 100).toInt()}%)",
                "${(mFrac * 100).toInt()}% / ${(eFrac * 100).toInt()}%",
                "Morn / Eve"
            )
        }
        else -> {
            val total = (cowLiters + buffLiters).coerceAtLeast(1.0)
            val cFrac = (cowLiters / total).toFloat()
            val bFrac = (buffLiters / total).toFloat()
            Tuple8(
                cFrac, bFrac,
                RoyalBlueSecondary, GoldenOrange,
                "🐄 Cow Milk: ${String.format(Locale.US, "%.1f L", cowLiters)} (${(cFrac * 100).toInt()}%)",
                "🐃 Buffalo Milk: ${String.format(Locale.US, "%.1f L", buffLiters)} (${(bFrac * 100).toInt()}%)",
                "${(cFrac * 100).toInt()}% / ${(bFrac * 100).toInt()}%",
                "Cow / Buff"
            )
        }
    }

    val sweep1 = slice1Percent * 360f
    val sweep2 = slice2Percent * 360f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Circular Donut
        Box(
            modifier = Modifier.size(110.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(100.dp)) {
                drawArc(
                    color = slice1Color,
                    startAngle = -90f,
                    sweepAngle = sweep1,
                    useCenter = false,
                    style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Butt)
                )
                drawArc(
                    color = slice2Color,
                    startAngle = -90f + sweep1,
                    sweepAngle = sweep2,
                    useCenter = false,
                    style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Butt)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = centerRatioText,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = TextPrimary
                )
                Text(
                    text = centerSubText,
                    fontSize = 9.sp,
                    color = TextSecondary
                )
            }
        }

        // Side Breakdown Table
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(12.dp).background(slice1Color, CircleShape))
                Text(
                    text = slice1Label,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = TextPrimary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(12.dp).background(slice2Color, CircleShape))
                Text(
                    text = slice2Label,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = TextPrimary
                )
            }
        }
    }
}

data class Tuple8<A, B, C, D, E, F, G, H>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
    val sixth: F,
    val seventh: G,
    val eighth: H
)

// -------------------------------------------------------------
// DRILL-DOWN TILES & MINI COMPONENTS
// -------------------------------------------------------------

@Composable
fun DrillDownStatPill(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = accentColor.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = title.loc(), fontSize = 10.sp, color = TextSecondary, maxLines = 1)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = accentColor)
            Spacer(modifier = Modifier.height(1.dp))
            Text(text = subtitle.loc(), fontSize = 9.sp, color = TextSecondary, maxLines = 1)
        }
    }
}

@Composable
fun ExpenseMiniCategory(
    label: String,
    amount: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label.loc(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = amount, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WarmHoney)
        }
    }
}

@Composable
fun SummaryMiniPill(
    label: String,
    value: String,
    badgeColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = 0.15f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).background(badgeColor, shape = CircleShape))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = label.loc(), color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
fun QuickActionRound(
    title: String,
    icon: ImageVector,
    badgeText: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SurfaceCard,
                shadowElevation = 1.dp,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = title.loc(), tint = RoyalBluePrimary, modifier = Modifier.size(22.dp))
                }
            }
            if (badgeText != null) {
                Surface(
                    shape = CircleShape,
                    color = DairyGreen,
                    modifier = Modifier.offset(x = 4.dp, y = (-4).dp)
                ) {
                    Text(
                        text = badgeText,
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = title.loc(), fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1, color = TextPrimary)
    }
}
