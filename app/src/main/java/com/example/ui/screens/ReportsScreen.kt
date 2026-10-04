package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.*
import com.example.ui.MilkMateViewModel
import com.example.ui.theme.*
import com.example.ui.util.BusinessModeFeatures
import com.example.ui.util.ReportExportHelper
import java.text.SimpleDateFormat
import java.util.*

data class DairyReportDef(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String, // "MILK", "HERD", "FINANCE", "CUSTOMER", "HEALTH"
    val iconName: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: MilkMateViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val business by viewModel.currentBusiness.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val inventoryItems by viewModel.inventoryItems.collectAsStateWithLifecycle()
    val cattleList by viewModel.cattleList.collectAsStateWithLifecycle()
    val allMilkingRecords by viewModel.allMilkingRecords.collectAsStateWithLifecycle()
    val allDeliveries by viewModel.allDeliveries.collectAsStateWithLifecycle()
    val allMilkCollections by viewModel.allMilkCollections.collectAsStateWithLifecycle()
    val allBreedingRecords by viewModel.allBreedingRecords.collectAsStateWithLifecycle()
    val allDewormingRecords by viewModel.allDewormingRecords.collectAsStateWithLifecycle()
    val allVaccinationRecords by viewModel.allVaccinationRecords.collectAsStateWithLifecycle()
    val allTreatmentRecords by viewModel.allTreatmentRecords.collectAsStateWithLifecycle()

    var selectedPeriodFilter by remember { mutableStateOf("THIS_MONTH") } // TODAY, YESTERDAY, THIS_WEEK, THIS_MONTH, ALL_TIME
    var selectedMonthOffset by remember { mutableIntStateOf(0) }
    var activeReportModal by remember { mutableStateOf<DairyReportDef?>(null) }
    var reportSearchQuery by remember { mutableStateOf("") }

    val calendar = remember(selectedMonthOffset) {
        Calendar.getInstance().apply {
            add(Calendar.MONTH, selectedMonthOffset)
        }
    }

    val monthDisplayName = remember(calendar) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
    }

    val accountStartDate = business?.accountStartDate ?: 0L
    val startYearMonth = remember(accountStartDate) {
        if (accountStartDate > 0L) SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(accountStartDate)) else ""
    }
    val currentYearMonth = remember {
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    }
    val viewingYearMonth = remember(calendar) {
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)
    }
    val canGoPrevMonth = startYearMonth.isBlank() || viewingYearMonth > startYearMonth
    val canGoNextMonth = viewingYearMonth < currentYearMonth

    val supportedMilkTypes = business?.supportedMilkTypes ?: "BOTH"
    val isCowEnabled = BusinessModeFeatures.isCowEnabled(supportedMilkTypes)
    val isBuffaloEnabled = BusinessModeFeatures.isBuffaloEnabled(supportedMilkTypes)
    val isCowOnly = isCowEnabled && !isBuffaloEnabled
    val isBuffaloOnly = isBuffaloEnabled && !isCowEnabled

    val todayMidnight = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val todayEnd = remember(todayMidnight) { todayMidnight + 86400000L - 1L }

    // Dynamic Period Timestamp Range
    val (periodStart, periodEnd) = remember(selectedPeriodFilter, selectedMonthOffset, todayMidnight, todayEnd, accountStartDate) {
        when (selectedPeriodFilter) {
            "TODAY" -> Pair(todayMidnight, todayEnd)
            "YESTERDAY" -> Pair(todayMidnight - 86400000L, todayMidnight - 1L)
            "THIS_WEEK" -> Pair(todayMidnight - (6 * 86400000L), todayEnd)
            "ALL_TIME" -> Pair(maxOf(0L, accountStartDate), Long.MAX_VALUE)
            else -> { // THIS_MONTH (with offset)
                val cal = Calendar.getInstance().apply {
                    add(Calendar.MONTH, selectedMonthOffset)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val start = maxOf(cal.timeInMillis, accountStartDate)
                val endCal = Calendar.getInstance().apply {
                    add(Calendar.MONTH, selectedMonthOffset)
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                Pair(start, endCal.timeInMillis)
            }
        }
    }

    // Dynamic Filtered Data Collections
    val filteredDeliveries = remember(allDeliveries, periodStart, periodEnd) {
        allDeliveries.filter { it.deliveryDate in periodStart..periodEnd && it.isDelivered }
    }
    val filteredMilking = remember(allMilkingRecords, periodStart, periodEnd) {
        allMilkingRecords.filter { it.dateEpochMidnight in periodStart..periodEnd }
    }
    val filteredExpenses = remember(expenses, periodStart, periodEnd) {
        expenses.filter { it.date in periodStart..periodEnd && it.isBusiness() }
    }
    val filteredPayments = remember(payments, periodStart, periodEnd) {
        payments.filter { it.date in periodStart..periodEnd }
    }

    // Animals Scoped
    val buffaloIdSet = remember(cattleList) {
        cattleList.filter { it.type.equals("BUFFALO", ignoreCase = true) }.map { it.id }.toSet()
    }
    val scopedCattle = remember(cattleList, isCowOnly, isBuffaloOnly) {
        when {
            isCowOnly -> cattleList.filter { it.type.equals("COW", ignoreCase = true) }
            isBuffaloOnly -> cattleList.filter { it.type.equals("BUFFALO", ignoreCase = true) }
            else -> cattleList
        }
    }

    // Dynamic Calculations
    val validSalesDeliveries = filteredDeliveries.filter { it.customerType != "SUPPLIER" }
    val totalLitersSold = validSalesDeliveries.sumOf { it.quantityLiters }
    val cowLitersSold = if (isCowEnabled) validSalesDeliveries.filter { it.milkType == "COW" }.sumOf { it.quantityLiters } else 0.0
    val buffaloLitersSold = if (isBuffaloEnabled) validSalesDeliveries.filter { it.milkType == "BUFFALO" }.sumOf { it.quantityLiters } else 0.0

    // Real Milking Production Liters
    val totalMilkingLiters = filteredMilking.sumOf { it.quantityLiters }
    val cowMilkingLiters = if (isCowEnabled) filteredMilking.filter { it.cattleId !in buffaloIdSet }.sumOf { it.quantityLiters } else 0.0
    val buffMilkingLiters = if (isBuffaloEnabled) filteredMilking.filter { it.cattleId in buffaloIdSet || it.cattleTag.contains("BUF", true) }.sumOf { it.quantityLiters } else 0.0
    val morningMilkingLiters = filteredMilking.filter { it.shift == "MORNING" }.sumOf { it.quantityLiters }
    val eveningMilkingLiters = filteredMilking.filter { it.shift == "EVENING" }.sumOf { it.quantityLiters }

    // Display Liters (Milking Logs if recorded, else Sales Deliveries)
    val displayTotalLiters = if (totalMilkingLiters > 0.0) totalMilkingLiters else totalLitersSold
    val displayCowLiters = if (cowMilkingLiters > 0.0) cowMilkingLiters else cowLitersSold
    val displayBuffLiters = if (buffMilkingLiters > 0.0) buffMilkingLiters else buffaloLitersSold
    val displayMorningLiters = if (morningMilkingLiters > 0.0) morningMilkingLiters else validSalesDeliveries.filter { it.shift == "MORNING" }.sumOf { it.quantityLiters }
    val displayEveningLiters = if (eveningMilkingLiters > 0.0) eveningMilkingLiters else validSalesDeliveries.filter { it.shift == "EVENING" }.sumOf { it.quantityLiters }

    val totalRevenue = validSalesDeliveries.sumOf { it.totalAmount }
    val totalCollected = filteredPayments.filter { it.paymentType == "CUSTOMER_PAYMENT" }.sumOf { it.amount }
    val totalOutstanding = customers.filter { it.type != "SUPPLIER" }.sumOf { it.outstandingBalance }
    val totalExpenses = filteredExpenses.sumOf { it.amount }
    val currentMode = business?.businessMode ?: "FARMER"
    val showFeed = BusinessModeFeatures.showCattleFeed(currentMode)
    val feedCost = if (!showFeed) 0.0 else filteredExpenses.filter { 
        val cat = it.category.uppercase()
        cat.contains("FEED") || cat.contains("FODDER") || cat.contains("BHUSA") || cat.contains("SILAGE") ||
        (it.isInventoryPurchase && BusinessModeFeatures.isFeedItem(it.notes.ifBlank { it.category }))
    }.sumOf { it.amount }
    val stockPurchasesCost = filteredExpenses.filter { it.isInventoryPurchase || it.category == "INVENTORY_PURCHASE" }.sumOf { it.amount }
    val netProfit = maxOf(0.0, totalRevenue - totalExpenses)
    val iofcSurplus = totalRevenue - feedCost

    val reportDefinitions = remember(supportedMilkTypes, isCowOnly, isBuffaloOnly, currentMode) {
        val list = mutableListOf<DairyReportDef>()
        when (currentMode) {
            "TRADER" -> {
                list.add(
                    DairyReportDef(
                        id = "FARMER_PROCUREMENT",
                        title = "Farmer Milk Procurement & FAT/SNF",
                        subtitle = "Daily procurement slips from farmers, FAT, SNF & rate ledger",
                        category = "MILK",
                        iconName = "water_drop"
                    )
                )
                list.add(
                    DairyReportDef(
                        id = "DELIVERY_DISPATCH",
                        title = "Customer Route Deliveries & Sales",
                        subtitle = "Household routes, tea stalls & commercial bulk buyer orders",
                        category = "DELIVERY",
                        iconName = "local_shipping"
                    )
                )
                list.add(
                    DairyReportDef(
                        id = "FIN_PROFIT_LOSS",
                        title = "Trading Margin & Profit Spread",
                        subtitle = "Total sales revenue minus farmer procurement cost & expenses",
                        category = "FINANCE",
                        iconName = "trending_up"
                    )
                )
                list.add(
                    DairyReportDef(
                        id = "CUST_OUTSTANDING",
                        title = "Customer Receivables & Khata",
                        subtitle = "Customer dues, pending balances with 1-tap WhatsApp reminders",
                        category = "CUSTOMER",
                        iconName = "account_balance_wallet"
                    )
                )
            }
            "COLLECTION_CENTER" -> {
                list.add(
                    DairyReportDef(
                        id = "FARMER_PROCUREMENT",
                        title = "Farmer Milk Collection Daily Sheet",
                        subtitle = "Shift-wise member farmer milk intake, FAT, SNF and totals",
                        category = "MILK",
                        iconName = "water_drop"
                    )
                )
                list.add(
                    DairyReportDef(
                        id = "DELIVERY_DISPATCH",
                        title = "Bulk Tanker Dispatch Challans",
                        subtitle = "Chilled bulk milk shipments sent to dairy processing plants",
                        category = "DELIVERY",
                        iconName = "local_shipping"
                    )
                )
                list.add(
                    DairyReportDef(
                        id = "FIN_PROFIT_LOSS",
                        title = "Collection Center Spread Statement",
                        subtitle = "Factory revenue minus farmer disbursements & testing costs",
                        category = "FINANCE",
                        iconName = "trending_up"
                    )
                )
            }
            "PROCESSING_UNIT" -> {
                list.add(
                    DairyReportDef(
                        id = "DELIVERY_DISPATCH",
                        title = "Raw Milk Intake & Byproduct Dispatches",
                        subtitle = "Raw milk batch receipts and finished byproduct shipments",
                        category = "DELIVERY",
                        iconName = "local_shipping"
                    )
                )
                list.add(
                    DairyReportDef(
                        id = "FIN_PROFIT_LOSS",
                        title = "Processing Unit P&L Statement",
                        subtitle = "Value-added byproduct sales minus raw milk & packaging costs",
                        category = "FINANCE",
                        iconName = "trending_up"
                    )
                )
                list.add(
                    DairyReportDef(
                        id = "CUST_OUTSTANDING",
                        title = "Merchant & Distributor Accounts",
                        subtitle = "Wholesale customer khatas and pending invoice payments",
                        category = "CUSTOMER",
                        iconName = "account_balance_wallet"
                    )
                )
            }
            "RETAIL_PARLOUR" -> {
                list.add(
                    DairyReportDef(
                        id = "DELIVERY_DISPATCH",
                        title = "Counter POS Daily Sales Register",
                        subtitle = "Daily walk-in customer sales, pouch milk & byproduct volume",
                        category = "DELIVERY",
                        iconName = "local_shipping"
                    )
                )
                list.add(
                    DairyReportDef(
                        id = "FIN_PROFIT_LOSS",
                        title = "Retail Daily Profit & Margin",
                        subtitle = "Counter cash/UPI revenue minus stock procurement cost",
                        category = "FINANCE",
                        iconName = "trending_up"
                    )
                )
                list.add(
                    DairyReportDef(
                        id = "CUST_OUTSTANDING",
                        title = "Customer Credit Khata (Udhar)",
                        subtitle = "Local customer udhar balances and payment tracking",
                        category = "CUSTOMER",
                        iconName = "account_balance_wallet"
                    )
                )
            }
            else -> {
                list.add(
                    DairyReportDef(
                        id = "MILK_PRODUCTION",
                        title = if (isCowOnly) "Cow Milk Production & Yields" else "Milk Production & Shift Yields",
                        subtitle = if (isCowOnly) "Shift-wise morning & evening Cow Milk records & FAT %" else "Morning vs Evening, Cow vs Buffalo yield ledger",
                        category = "MILK",
                        iconName = "water_drop"
                    )
                )
                if (BusinessModeFeatures.showHerdBreeding(currentMode)) {
                    list.add(
                        DairyReportDef(
                            id = "HERD_PERFORMANCE",
                            title = if (isCowOnly) "Cow Milking Performance" else "Cattle Herd Milking Performance",
                            subtitle = "Individual cow/buffalo yield, peak production & lactation stage",
                            category = "HERD",
                            iconName = "pets"
                        )
                    )
                }
                if (BusinessModeFeatures.showCattleFeed(currentMode)) {
                    list.add(
                        DairyReportDef(
                            id = "FEED_ECONOMICS",
                            title = "Feed Consumption & IOFC Report",
                            subtitle = "Income Over Feed Cost, feed cost per liter & bag inventory",
                            category = "FINANCE",
                            iconName = "grass"
                        )
                    )
                }
                if (BusinessModeFeatures.showHerdBreeding(currentMode)) {
                    list.add(
                        DairyReportDef(
                            id = "HEALTH_VACCINATION",
                            title = "Cattle Health, Deworming & Vaccines",
                            subtitle = "Dewormer salt history, booster schedules & treatment costs",
                            category = "HEALTH",
                            iconName = "medical_services"
                        )
                    )
                    list.add(
                        DairyReportDef(
                            id = "BREEDING_CALVING",
                            title = "Reproduction & Calving Schedule",
                            subtitle = "A.I. semen straw logs, pregnancy diagnosis & 60-day calving alerts",
                            category = "HERD",
                            iconName = "event"
                        )
                    )
                }
                list.add(
                    DairyReportDef(
                        id = "FIN_PROFIT_LOSS",
                        title = "Profit & Loss Statement",
                        subtitle = if (showFeed) "Milk revenue minus feed, medicines, labor and utilities" else "Sales revenue minus stock purchase, rent & operating expenses",
                        category = "FINANCE",
                        iconName = "trending_up"
                    )
                )
                if (BusinessModeFeatures.showBilling(currentMode)) {
                    list.add(
                        DairyReportDef(
                            id = "CUST_OUTSTANDING",
                            title = "Customer Outstanding & Khata",
                            subtitle = "Pending receivables with 1-tap WhatsApp reminder",
                            category = "CUSTOMER",
                            iconName = "account_balance_wallet"
                        )
                    )
                }
                list.add(
                    DairyReportDef(
                        id = "DELIVERY_DISPATCH",
                        title = "Dispatches & Supply Slips",
                        subtitle = "Household routes, Center slips & bulk dispatches",
                        category = "DELIVERY",
                        iconName = "local_shipping"
                    )
                )
            }
        }
        list
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = if (isCowOnly) "Cow Dairy Business Intelligence" else "Dairy Business Intelligence",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Dynamic farm analytics & verified audit sheets",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                // PDF Export of Complete Dairy Statement
                IconButton(onClick = {
                    val headers = listOf("Metric / Category", "Volume / Details", "Value (₹)")
                    val rows = mutableListOf(
                        listOf("Total Milk Produced / Sold", String.format(Locale.US, "%.1f Liters", displayTotalLiters), "₹${totalRevenue.toInt()}")
                    )
                    if (!isBuffaloOnly) {
                        rows.add(listOf("• Cow Milk", String.format(Locale.US, "%.1f Liters", displayCowLiters), "₹${(displayCowLiters * (business?.cowMilkRate ?: 50.0)).toInt()}"))
                    }
                    if (!isCowOnly) {
                        rows.add(listOf("• Buffalo Milk", String.format(Locale.US, "%.1f Liters", displayBuffLiters), "₹${(displayBuffLiters * (business?.buffaloMilkRate ?: 65.0)).toInt()}"))
                    }
                    rows.addAll(listOf(
                        listOf("Morning Shift Yield", String.format(Locale.US, "%.1f Liters", displayMorningLiters), "—"),
                        listOf("Evening Shift Yield", String.format(Locale.US, "%.1f Liters", displayEveningLiters), "—"),
                        listOf("Customer Collections", "${filteredPayments.filter { it.paymentType == "CUSTOMER_PAYMENT" }.size} receipts", "₹${totalCollected.toInt()}"),
                        listOf("Outstanding Dues", "${customers.count { it.outstandingBalance > 0 }} parties", "₹${totalOutstanding.toInt()}")
                    ))
                    if (showFeed) {
                        rows.add(listOf("Feed & Fodder Cost", "Cattle nutrition", "₹${feedCost.toInt()}"))
                        rows.add(listOf("Income Over Feed Cost (IOFC)", "Revenue − Feed", "₹${iofcSurplus.toInt()}"))
                    } else {
                        rows.add(listOf("Stock Purchases", "Product Inventory", "₹${stockPurchasesCost.toInt()}"))
                    }
                    rows.addAll(listOf(
                        listOf("Total Expenses", "${filteredExpenses.size} entries", "₹${totalExpenses.toInt()}"),
                        listOf("Net Operating Profit", "Revenue − Costs", "₹${netProfit.toInt()}")
                    ))
                    ReportExportHelper.generateAndSharePdf(
                        context = context,
                        title = "${business?.businessName ?: "MilkMate Dairy"} - Performance Report",
                        period = if (selectedPeriodFilter == "THIS_MONTH") monthDisplayName else selectedPeriodFilter,
                        headers = headers,
                        rows = rows,
                        summaryMetrics = listOf(
                            "Total Revenue" to "₹${totalRevenue.toInt()}",
                            (if (showFeed) "Feed Cost" else "Stock Spend") to "₹${(if (showFeed) feedCost else stockPurchasesCost).toInt()}",
                            "Total Expenses" to "₹${totalExpenses.toInt()}",
                            "Net Profit" to "₹${netProfit.toInt()}"
                        )
                    )
                }) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF Statement", tint = DangerRed)
                }

                // WhatsApp Summary Share
                IconButton(onClick = {
                    val shareText = buildString {
                        append("🥛 *${business?.businessName ?: "MilkMate Dairy"} - Performance Report*\n")
                        append("📅 Period: ${if (selectedPeriodFilter == "THIS_MONTH") monthDisplayName else selectedPeriodFilter}\n\n")
                        append("📊 *Performance:*\n")
                        append("• Milk Handled / Sold: ${String.format(Locale.US, "%.1f", displayTotalLiters)} Liters\n")
                        if (!isBuffaloOnly) {
                            append("  - Cow Milk: ${String.format(Locale.US, "%.1f", displayCowLiters)} L\n")
                        }
                        if (!isCowOnly) {
                            append("  - Buffalo Milk: ${String.format(Locale.US, "%.1f", displayBuffLiters)} L\n")
                        }
                        append("• Morning Shift: ${String.format(Locale.US, "%.1f", displayMorningLiters)} L\n")
                        append("• Evening Shift: ${String.format(Locale.US, "%.1f", displayEveningLiters)} L\n\n")
                        append("💰 *Financial Summary:*\n")
                        append("• Total Revenue: ₹${totalRevenue.toInt()}\n")
                        if (showFeed) {
                            append("• Feed Cost: ₹${feedCost.toInt()}\n")
                            append("• Income Over Feed Cost (IOFC): ₹${iofcSurplus.toInt()}\n")
                        } else {
                            append("• Product Stock Purchases: ₹${stockPurchasesCost.toInt()}\n")
                        }
                        append("• Net Profit: ₹${netProfit.toInt()}\n")
                        append("• Outstanding Dues: ₹${totalOutstanding.toInt()}\n\n")
                        append("Generated via MilkMate OS ✓")
                    }
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Farm Report"))
                }) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = RoyalBluePrimary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Period Filter & Month Selector
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (selectedPeriodFilter == "THIS_MONTH") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { if (canGoPrevMonth) selectedMonthOffset -= 1 },
                                    enabled = canGoPrevMonth,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ChevronLeft,
                                        contentDescription = "Previous Month",
                                        tint = if (canGoPrevMonth) RoyalBluePrimary else Color.LightGray
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(monthDisplayName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = RoyalBluePrimary)
                                    Text("Dynamic Ledger Month", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                IconButton(
                                    onClick = { if (canGoNextMonth) selectedMonthOffset += 1 },
                                    enabled = canGoNextMonth,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = "Next Month",
                                        tint = if (canGoNextMonth) RoyalBluePrimary else Color.LightGray
                                    )
                                }
                            }
                        }

                        // Filter chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val filters = listOf(
                                "TODAY" to "Today",
                                "THIS_WEEK" to "7 Days",
                                "THIS_MONTH" to "Month",
                                "ALL_TIME" to "All Time"
                            )
                            filters.forEach { (key, label) ->
                                val isSel = selectedPeriodFilter == key
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) RoyalBluePrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            selectedPeriodFilter = key
                                            if (key == "THIS_MONTH") selectedMonthOffset = 0
                                        }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSel) Color.White else TextPrimary,
                                        modifier = Modifier.padding(vertical = 7.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Executive KPI Hero Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = RoyalBluePrimary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("NET FARM REVENUE", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Bold)
                            Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.2f)) {
                                Text("LIVE DYNAMIC", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "₹${totalRevenue.toInt()}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            KpiMiniBox(label = "Milk Output", value = "${displayTotalLiters.toInt()} L", modifier = Modifier.weight(1f))
                            KpiMiniBox(label = "Collected", value = "₹${totalCollected.toInt()}", modifier = Modifier.weight(1f))
                            KpiMiniBox(
                                label = if (showFeed) "Feed Cost" else "Stock Spend",
                                value = "₹${(if (showFeed) feedCost else stockPurchasesCost).toInt()}",
                                modifier = Modifier.weight(1f)
                            )
                            KpiMiniBox(label = "Net Profit", value = "₹${netProfit.toInt()}", modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Volume & Shift Analytics Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Milk Output & Shift Distribution", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                        // Morning vs Evening Shift Bar
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🌅 Morning Shift: ${displayMorningLiters.toInt()} L", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Text("🌆 Evening Shift: ${displayEveningLiters.toInt()} L", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            val morningRatio = if (displayTotalLiters > 0) (displayMorningLiters / displayTotalLiters).toFloat() else 0.5f
                            LinearProgressIndicator(
                                progress = { morningRatio },
                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                color = FreshGold,
                                trackColor = RoyalBluePrimary
                            )
                        }

                        // Cow vs Buffalo Milk Bar (ONLY in BOTH mode)
                        if (!isCowOnly && !isBuffaloOnly && supportedMilkTypes == "BOTH") {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("🐄 Cow Milk: ${displayCowLiters.toInt()} L", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    Text("🐃 Buffalo Milk: ${displayBuffLiters.toInt()} L", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                                val cowRatio = if (displayTotalLiters > 0) (displayCowLiters / displayTotalLiters).toFloat() else 0.5f
                                LinearProgressIndicator(
                                    progress = { cowRatio },
                                    modifier = Modifier.fillMaxWidth().height(8.dp),
                                    color = DairyGreen,
                                    trackColor = GoldenOrange
                                )
                            }
                        }
                    }
                }
            }

            // Report Grid Section Header
            item {
                Text(
                    text = "SPECIALIZED AUDIT & OPERATIONAL REPORTS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
            }

            // Report Grid Items
            items(reportDefinitions, key = { it.id }) { report ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            reportSearchQuery = ""
                            activeReportModal = report
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when (report.category) {
                                "MILK" -> DairyGreenLight
                                "HERD" -> Color(0xFFE8F5E9)
                                "CUSTOMER" -> RoyalBlueLight
                                "FINANCE" -> FreshGoldLight
                                "HEALTH" -> DangerRedLight
                                else -> Color(0xFFE1BEE7)
                            }
                        ) {
                            Icon(
                                imageVector = when (report.category) {
                                    "MILK" -> Icons.Default.WaterDrop
                                    "HERD" -> Icons.Default.Pets
                                    "CUSTOMER" -> Icons.Default.People
                                    "FINANCE" -> Icons.Default.AccountBalance
                                    "HEALTH" -> Icons.Default.MedicalServices
                                    else -> Icons.Default.LocalShipping
                                },
                                contentDescription = null,
                                tint = when (report.category) {
                                    "MILK" -> DairyGreen
                                    "HERD" -> GrassGreen
                                    "CUSTOMER" -> RoyalBluePrimary
                                    "FINANCE" -> WarmHoney
                                    "HEALTH" -> DangerRed
                                    else -> Color(0xFF673AB7)
                                },
                                modifier = Modifier.padding(8.dp).size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(report.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(report.subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    // Modal Viewer for Selected Report
    activeReportModal?.let { report ->
        ReportDetailSheet(
            report = report,
            periodName = if (selectedPeriodFilter == "THIS_MONTH") monthDisplayName else selectedPeriodFilter,
            deliveries = validSalesDeliveries,
            milkingRecords = filteredMilking,
            cattleList = scopedCattle,
            breedingRecords = allBreedingRecords,
            dewormingRecords = allDewormingRecords,
            vaccinationRecords = allVaccinationRecords,
            treatmentRecords = allTreatmentRecords,
            customers = customers,
            payments = filteredPayments,
            expenses = filteredExpenses,
            inventoryItems = inventoryItems,
            isCowOnly = isCowOnly,
            isBuffaloOnly = isBuffaloOnly,
            onDismiss = { activeReportModal = null }
        )
    }
}

@Composable
fun ReportDetailSheet(
    report: DairyReportDef,
    periodName: String,
    deliveries: List<DeliveryEntity>,
    milkingRecords: List<CattleMilkingRecordEntity>,
    cattleList: List<CattleEntity>,
    breedingRecords: List<BreedingRecordEntity>,
    dewormingRecords: List<DewormingRecordEntity>,
    vaccinationRecords: List<VaccinationRecordEntity>,
    treatmentRecords: List<TreatmentRecordEntity>,
    customers: List<CustomerEntity>,
    payments: List<PaymentEntity>,
    expenses: List<ExpenseEntity>,
    inventoryItems: List<InventoryItemEntity>,
    isCowOnly: Boolean,
    isBuffaloOnly: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.90f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().background(RoyalBluePrimary).padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(report.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                        Text(periodName, fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search records by name, tag, category...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                    when (report.id) {
                        "MILK_PRODUCTION" -> {
                            val items = milkingRecords.filter {
                                searchQuery.isBlank() || it.cattleTag.contains(searchQuery, true) || it.shift.contains(searchQuery, true)
                            }
                            if (items.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("No milking records found for $periodName", fontSize = 12.sp, color = TextSecondary)
                                }
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(items, key = { it.id }) { rec ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text("Tag: ${rec.cattleTag}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text("${sdf.format(Date(rec.dateEpochMidnight))} • Shift: ${rec.shift} • FAT: ${rec.fat}% • SNF: ${rec.snf}%", fontSize = 11.sp, color = TextSecondary)
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text("${rec.quantityLiters} L", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = RoyalBluePrimary)
                                                    Text("Recorded by ${rec.recordedBy}", fontSize = 10.sp, color = TextSecondary)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        "HERD_PERFORMANCE" -> {
                            val items = cattleList.filter {
                                searchQuery.isBlank() || it.tagNumber.contains(searchQuery, true) || it.name.contains(searchQuery, true) || it.breed.contains(searchQuery, true)
                            }
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(items, key = { it.id }) { c ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("${c.name.ifBlank { c.tagNumber }} (${c.tagNumber})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text("${c.breed} • Stage: ${c.lactationStage} • Lact #${c.lactationCount}", fontSize = 11.sp, color = TextSecondary)
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("${c.dailyYieldLiters} L/day", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RoyalBluePrimary)
                                                Text(c.breedingStatus, fontSize = 10.sp, color = WarmHoney)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        "FEED_ECONOMICS" -> {
                            val feedExp = expenses.filter { 
                                val cat = it.category.uppercase()
                                cat.contains("FEED") || cat.contains("FODDER") || cat.contains("BHUSA") || cat.contains("SILAGE") || it.isInventoryPurchase
                            }
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = FreshGoldLight)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text("🌾 Feed Stock & Supplement Summary", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = WarmHoney)
                                            Text("Total Feed Purchases: ₹${feedExp.sumOf { it.amount }.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            Text("Tracked SKUs in Barn: ${inventoryItems.size}", fontSize = 11.sp, color = TextSecondary)
                                        }
                                    }
                                }
                                items(feedExp, key = { it.id }) { exp ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(exp.notes.ifBlank { exp.category }, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text("${sdf.format(Date(exp.date))} • Paid via ${exp.paymentMethod}", fontSize = 11.sp, color = TextSecondary)
                                            }
                                            Text("₹${exp.amount.toInt()}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DangerRed)
                                        }
                                    }
                                }
                            }
                        }

                        "HEALTH_VACCINATION" -> {
                            val dewormList = dewormingRecords
                            val vaccList = vaccinationRecords
                            val treatList = treatmentRecords
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (dewormList.isNotEmpty()) {
                                    item { Text("💊 DEWORMING LOGS", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = GrassGreen) }
                                    items(dewormList, key = { it.id }) { dw ->
                                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Column {
                                                    Text("Tag: ${dw.cattleTag} • Salt: ${dw.dewormerSalt}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    Text("Administered: ${sdf.format(Date(dw.date))} • Next Due: ${sdf.format(Date(dw.nextDueDate))}", fontSize = 10.sp, color = TextSecondary)
                                                }
                                                Text("₹${dw.cost.toInt()}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DangerRed)
                                            }
                                        }
                                    }
                                }
                                if (vaccList.isNotEmpty()) {
                                    item { Text("💉 VACCINATION SCHEDULE", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = RoyalBluePrimary) }
                                    items(vaccList, key = { it.id }) { vc ->
                                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Column {
                                                    Text("Tag: ${vc.cattleTag} • ${vc.vaccineName}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    Text("Given: ${sdf.format(Date(vc.date))} • Next Due: ${sdf.format(Date(vc.nextDueDate))}", fontSize = 10.sp, color = TextSecondary)
                                                }
                                                Text("₹${vc.cost.toInt()}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DangerRed)
                                            }
                                        }
                                    }
                                }
                                if (treatList.isNotEmpty()) {
                                    item { Text("🩺 TREATMENT & MEDICAL RECORDS", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DangerRed) }
                                    items(treatList, key = { it.id }) { tr ->
                                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Column {
                                                    Text("Tag: ${tr.cattleTag} • ${tr.diseaseName}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    Text("Med: ${tr.medicationName} • Checked: ${sdf.format(Date(tr.checkupDate))}", fontSize = 10.sp, color = TextSecondary)
                                                }
                                                Text("₹${tr.treatmentCost.toInt()}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DangerRed)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        "BREEDING_CALVING" -> {
                            val items = breedingRecords
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(items, key = { it.id }) { br ->
                                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Column {
                                                Text("Tag: ${br.cattleTag} • Bull/Straw: ${br.bullIdOrName}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text("Event: ${br.eventType} • Date: ${sdf.format(Date(br.date))} • PD: ${br.pdStatus}", fontSize = 11.sp, color = TextSecondary)
                                                if (br.expectedCalvingDate > 0) {
                                                    Text("Expected Calving: ${sdf.format(Date(br.expectedCalvingDate))}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = GrassGreen)
                                                }
                                            }
                                            Text("${br.semenStrawsUsed} Straws", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RoyalBluePrimary)
                                        }
                                    }
                                }
                            }
                        }

                        "CUST_OUTSTANDING" -> {
                            val debtors = customers.filter { it.outstandingBalance > 0 && (searchQuery.isBlank() || it.name.contains(searchQuery, true)) }
                            if (debtors.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("All customer accounts settled! No dues pending. ✓", fontSize = 13.sp, color = DairyGreen, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(debtors, key = { it.id }) { cust ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(cust.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text("Phone: ${cust.mobile} • ${cust.route}", fontSize = 11.sp, color = TextSecondary)
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text("₹${cust.outstandingBalance.toInt()}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DangerRed)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    FilledTonalButton(
                                                        onClick = {
                                                            val msg = "Hello ${cust.name}, gentle reminder from dairy for your outstanding milk bill of ₹${cust.outstandingBalance.toInt()}. Thank you!"
                                                            val uri = Uri.parse("https://api.whatsapp.com/send?phone=91${cust.mobile}&text=${Uri.encode(msg)}")
                                                            try { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) } catch (_: Exception) {}
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(28.dp),
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Text("WhatsApp 💬", fontSize = 10.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        else -> {
                            // Deliveries / Dispatches
                            val items = deliveries.filter { searchQuery.isBlank() || it.customerName.contains(searchQuery, true) || it.shift.contains(searchQuery, true) }
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(items, key = { it.id }) { d ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(d.customerName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text("${d.shift} • ${d.milkType} • ${sdf.format(Date(d.deliveryDate))}", fontSize = 11.sp, color = TextSecondary)
                                            }
                                            Text("${d.quantityLiters} L • ₹${d.totalAmount.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Action Footer
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
fun KpiMiniBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.15f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}

