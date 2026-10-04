package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.InventoryItemEntity
import com.example.data.local.entity.InventoryMonthClosingEntity
import com.example.data.local.entity.InventoryTransactionEntity
import com.example.data.repository.ConsumptionForecastSummary
import com.example.data.repository.ForecastUrgency
import com.example.data.repository.ItemForecastResult
import com.example.data.repository.MonthlyDeliveryStats
import com.example.ui.MilkMateViewModel
import com.example.ui.theme.*
import com.example.ui.util.ReportExportHelper
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun InventoryScreen(
    viewModel: MilkMateViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val items by viewModel.inventoryItems.collectAsStateWithLifecycle()
    val transactions by viewModel.inventoryTransactions.collectAsStateWithLifecycle()
    val business by viewModel.currentBusiness.collectAsStateWithLifecycle()
    val session by viewModel.sessionState.collectAsStateWithLifecycle()

    // 1. Account Start Date Boundary Enforcement
    val accountStartDate = remember(business, session) {
        val bStart = business?.accountStartDate ?: 0L
        if (bStart > 0L) bStart else if (session.accountStartDate > 0L) session.accountStartDate else System.currentTimeMillis()
    }

    val startCal = remember(accountStartDate) {
        Calendar.getInstance().apply {
            timeInMillis = accountStartDate
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    // Selected View Calendar Month
    var selectedMonthCalendar by remember {
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        })
    }

    val currentMonthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val yearMonthFormat = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()) }
    val yearMonthString = remember(selectedMonthCalendar) { yearMonthFormat.format(selectedMonthCalendar.time) }

    val canGoPrevious = remember(selectedMonthCalendar, startCal) {
        selectedMonthCalendar.after(startCal)
    }

    val maxForwardCal = remember {
        Calendar.getInstance().apply {
            add(Calendar.MONTH, 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }
    val canGoForward = remember(selectedMonthCalendar, maxForwardCal) {
        selectedMonthCalendar.before(maxForwardCal)
    }

    // 0: Cattle Feed & Supplements, 1: Dairy Products, 2: Stock Purchases Log, 3: Daily Feeding Log, 4: Auto Consumption & Verification, 5: 3-Month Forecast
    val showFeed = remember(business?.businessMode) {
        com.example.ui.util.BusinessModeFeatures.showCattleFeed(business?.businessMode)
    }
    var activeTab by remember(showFeed) { mutableStateOf(if (showFeed) 0 else 1) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(showFeed) {
        if (!showFeed && (activeTab == 0 || activeTab == 3)) {
            activeTab = 1
        }
    }

    // Forecasting State from ViewModel
    val forecastSummary by viewModel.forecastSummary.collectAsStateWithLifecycle()
    val isForecastLoading by viewModel.isForecastLoading.collectAsStateWithLifecycle()
    val planningHorizonDays by viewModel.planningHorizonDays.collectAsStateWithLifecycle()
    val safetyBufferDays by viewModel.safetyBufferDays.collectAsStateWithLifecycle()

    var purchaseInitialQty by remember { mutableStateOf<Double?>(null) }
    var purchaseInitialRate by remember { mutableStateOf<Double?>(null) }
    var purchaseInitialSupplier by remember { mutableStateOf<String?>(null) }
    var showWhatsappOrderDialog by remember { mutableStateOf(false) }

    LaunchedEffect(session.businessId, items, transactions) {
        if (session.businessId.isNotBlank()) {
            viewModel.refreshForecast()
        }
    }

    // Dialog state holders
    var purchaseDialogItem by remember { mutableStateOf<InventoryItemEntity?>(null) }
    var consumptionDialogItem by remember { mutableStateOf<InventoryItemEntity?>(null) }
    var usageDialogItem by remember { mutableStateOf<InventoryItemEntity?>(null) }
    var auditDialogItem by remember { mutableStateOf<InventoryItemEntity?>(null) }
    var openingStockDialogItem by remember { mutableStateOf<InventoryItemEntity?>(null) }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var showConvertMilkDialog by remember { mutableStateOf(false) }
    var showWorkingPrincipleInfo by remember { mutableStateOf(false) }

    // Calculations
    val monthStartMillis = remember(selectedMonthCalendar) {
        (selectedMonthCalendar.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis
    }

    val monthEndMillis = remember(selectedMonthCalendar) {
        (selectedMonthCalendar.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.timeInMillis
    }

    // Filter transactions by selected month
    val monthTransactions = remember(transactions, monthStartMillis, monthEndMillis) {
        transactions.filter { it.date in monthStartMillis..monthEndMillis }
    }

    // High Level Metrics
    val relevantItems = remember(items, showFeed) {
        if (!showFeed) items.filter { !com.example.ui.util.BusinessModeFeatures.isFeedItem(it.itemName) }
        else items
    }

    val totalValuation = remember(relevantItems) { relevantItems.sumOf { it.currentStock * it.costPerUnit } }
    val lowStockItems = remember(relevantItems) { relevantItems.filter { it.stockAlertStatus == "LOW_STOCK" || it.stockAlertStatus == "OUT_OF_STOCK" } }
    val healthyStockItems = remember(relevantItems) { relevantItems.filter { it.stockAlertStatus == "NORMAL" } }

    val cattleFeedItems = remember(items, searchQuery, showFeed) {
        if (!showFeed) emptyList()
        else items.filter {
            com.example.ui.util.BusinessModeFeatures.isFeedItem(it.itemName) &&
            (searchQuery.isBlank() || it.itemName.contains(searchQuery, ignoreCase = true))
        }
    }

    val dairyProductItems = remember(items, searchQuery, showFeed) {
        items.filter {
            !com.example.ui.util.BusinessModeFeatures.isFeedItem(it.itemName) &&
            (searchQuery.isBlank() || it.itemName.contains(searchQuery, ignoreCase = true))
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                if (showFeed) "DAIRY INVENTORY & FEED" else "DAIRY PRODUCTS INVENTORY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoyalBluePrimary
                            )
                            Text("Stock & Consumption Hub", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { showWorkingPrincipleInfo = true }) {
                            Icon(Icons.Default.HelpOutline, contentDescription = "How it Works", tint = RoyalBluePrimary)
                        }
                        IconButton(onClick = { showAddItemDialog = true }) {
                            Icon(Icons.Default.AddCircle, contentDescription = "Add Item", tint = RoyalBluePrimary)
                        }
                    }
                }

                // Month Navigator
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (canGoPrevious) {
                                    val newCal = (selectedMonthCalendar.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
                                    selectedMonthCalendar = newCal
                                }
                            },
                            enabled = canGoPrevious,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month", tint = if (canGoPrevious) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(16.dp))
                            Text(
                                text = currentMonthFormat.format(selectedMonthCalendar.time),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        IconButton(
                            onClick = {
                                if (canGoForward) {
                                    val newCal = (selectedMonthCalendar.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                                    selectedMonthCalendar = newCal
                                }
                            },
                            enabled = canGoForward,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = if (canGoForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (activeTab == 0 || activeTab == 1) {
                FloatingActionButton(
                    onClick = { purchaseDialogItem = if (!showFeed) (dairyProductItems.firstOrNull() ?: relevantItems.firstOrNull()) else items.firstOrNull() },
                    containerColor = FreshGold,
                    contentColor = Color.White
                ) {
                    Row(modifier = Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (showFeed) "+ Buy Feed" else "+ Buy Stock", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Command Cockpit: Valuation, Low Stock Alarm, Healthy Items
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("TOTAL STOCK VALUATION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "₹${totalValuation.toInt()}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = RoyalBluePrimary
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (lowStockItems.isNotEmpty()) DangerRedLight else DairyGreenLight
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        if (lowStockItems.isNotEmpty()) Icons.Default.Warning else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (lowStockItems.isNotEmpty()) DangerRed else DairyGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        if (lowStockItems.isNotEmpty()) "${lowStockItems.size} Low Alert" else "Stock Healthy",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (lowStockItems.isNotEmpty()) DangerRed else DairyGreen
                                    )
                                }
                            }
                        }
                    }

                    // Low Stock Alert Banner (Actionable)
                    if (lowStockItems.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = FreshGold.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    val alertTitle = if (showFeed) "⚠️ Low Feed Alert:" else "⚠️ Low Stock Alert:"
                                    Text(
                                        "$alertTitle ${lowStockItems.take(2).joinToString(", ") { "${it.itemName} (${it.currentStock.toInt()}${it.unit})" }}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB45309)
                                    )
                                    Text("WorkManager background monitor checks stock every 6h", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Button(
                                    onClick = { purchaseDialogItem = lowStockItems.firstOrNull() },
                                    colors = ButtonDefaults.buttonColors(containerColor = FreshGold),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Restock", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // Quick Working Principle Help Banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { showWorkingPrincipleInfo = true },
                color = RoyalBlueLight
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(16.dp))
                        Text(
                            "How Pre-Existing Stock, Mid-Month Audits & Profit Work",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoyalBluePrimary
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(16.dp))
                }
            }

            // Segmented Scrollable Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (showFeed) {
                    FilterChip(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        label = { Text("🌾 Cattle Feed (${cattleFeedItems.size})", fontSize = 11.sp, fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
                FilterChip(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    label = { Text("🥛 Dairy Products (${dairyProductItems.size})", fontSize = 11.sp, fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal) }
                )
                FilterChip(
                    selected = activeTab == 5,
                    onClick = { activeTab = 5 },
                    label = {
                        val critical = forecastSummary?.criticalItemsCount ?: 0
                        val warn = forecastSummary?.warningItemsCount ?: 0
                        val labelText = if (critical > 0) "🔮 3-Mo Forecast (🚨$critical)" else if (warn > 0) "🔮 3-Mo Forecast (⚠️$warn)" else "🔮 3-Mo Forecast"
                        Text(labelText, fontSize = 11.sp, fontWeight = if (activeTab == 5) FontWeight.Bold else FontWeight.Normal)
                    },
                    colors = if (activeTab == 5) FilterChipDefaults.filterChipColors(containerColor = RoyalBluePrimary.copy(alpha = 0.15f), labelColor = RoyalBluePrimary) else FilterChipDefaults.filterChipColors()
                )
                FilterChip(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    label = { Text("📦 Purchases Log", fontSize = 11.sp, fontWeight = if (activeTab == 2) FontWeight.Bold else FontWeight.Normal) }
                )
                if (showFeed) {
                    FilterChip(
                        selected = activeTab == 3,
                        onClick = { activeTab = 3 },
                        label = { Text("🐄 Daily Feeding", fontSize = 11.sp, fontWeight = if (activeTab == 3) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
                FilterChip(
                    selected = activeTab == 4,
                    onClick = { activeTab = 4 },
                    label = { Text("📊 Auto Consumption & Audit", fontSize = 11.sp, fontWeight = if (activeTab == 4) FontWeight.Bold else FontWeight.Normal) }
                )
            }

            // Smart Forecast Alert Banner on Feed & Dairy Tabs
            if ((activeTab == 0 || activeTab == 1) && forecastSummary != null && (forecastSummary!!.criticalItemsCount > 0 || forecastSummary!!.warningItemsCount > 0)) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (forecastSummary!!.criticalItemsCount > 0) DangerRedLight else Color(0xFFFFF8E1),
                    border = BorderStroke(1.dp, if (forecastSummary!!.criticalItemsCount > 0) DangerRed.copy(alpha = 0.4f) else Color(0xFFFFB300)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { activeTab = 5 }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.AutoGraph,
                                contentDescription = null,
                                tint = if (forecastSummary!!.criticalItemsCount > 0) DangerRed else Color(0xFFF57F17),
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    if (forecastSummary!!.criticalItemsCount > 0)
                                        "🚨 ${forecastSummary!!.criticalItemsCount} item(s) running out in ≤ 5 days!"
                                    else
                                        "⚠️ ${forecastSummary!!.warningItemsCount} item(s) need reordering soon",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = if (forecastSummary!!.criticalItemsCount > 0) DangerRed else Color(0xFFE65100)
                                )
                                Text(
                                    "3-Month Forecast suggests ordering ₹${forecastSummary!!.totalEstimatedReorderCost.toInt()} stock. Tap to view purchase advice →",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Search Bar
            if (activeTab != 4 && activeTab != 5) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search item, feed name, reason...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                )
            }

            // Tab Content
            when (activeTab) {
                0 -> {
                    // Cattle Feed & Fodder Tab
                    if (cattleFeedItems.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("No cattle feed items found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { showAddItemDialog = true }) {
                                    Text("+ Add Feed Item")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(cattleFeedItems, key = { it.id }) { item ->
                                InventoryItemCard(
                                    item = item,
                                    onBuyStock = { purchaseDialogItem = item },
                                    onLogConsumption = { consumptionDialogItem = item },
                                    onSetUsage = { usageDialogItem = item },
                                    onSetOpeningStock = { openingStockDialogItem = item },
                                    onAuditCount = { auditDialogItem = item }
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // Dairy Value-Added Products
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showAddItemDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(42.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("+ Add Dairy SKU", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            if (showFeed || business?.businessMode == "PROCESSING_UNIT") {
                                Button(
                                    onClick = { showConvertMilkDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).height(42.dp)
                                ) {
                                    Icon(Icons.Default.Autorenew, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("🔄 Convert Milk", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }

                        if (dairyProductItems.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    if (showFeed) "No dairy products found. Tap convert to create paneer or ghee."
                                    else "No dairy products in catalog. Tap '+ Add Dairy SKU' to add paneer, curd, ghee, or milk pouches.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(dairyProductItems, key = { it.id }) { item ->
                                    InventoryItemCard(
                                        item = item,
                                        onBuyStock = { purchaseDialogItem = item },
                                        onLogConsumption = { consumptionDialogItem = item },
                                        onSetUsage = { usageDialogItem = item },
                                        onSetOpeningStock = { openingStockDialogItem = item },
                                        onAuditCount = { auditDialogItem = item }
                                    )
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Purchases Log
                    val purchases = monthTransactions.filter {
                        it.transactionType == "PURCHASE" && (searchQuery.isBlank() || it.itemName.contains(searchQuery, ignoreCase = true) || it.supplier.contains(searchQuery, ignoreCase = true))
                    }

                    if (purchases.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No stock purchases recorded this month.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(purchases, key = { it.id }) { tx ->
                                val itemUnit = items.find { it.id == tx.itemId }?.unit ?: "units"
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(tx.itemName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(
                                                "${tx.quantity.toInt()} $itemUnit @ ₹${tx.unitPrice.toInt()}/$itemUnit • ${tx.paymentMethod}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (tx.supplier.isNotBlank()) {
                                                Text("Supplier: ${tx.supplier}", fontSize = 10.sp, color = RoyalBluePrimary)
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("₹${tx.totalAmount.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = DangerRed)
                                            Text(SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(tx.date)), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // Daily Feeding Log
                    val feedingLogs = monthTransactions.filter {
                        it.transactionType == "CONSUMPTION" && (searchQuery.isBlank() || it.itemName.contains(searchQuery, ignoreCase = true) || it.notes.contains(searchQuery, ignoreCase = true))
                    }

                    if (feedingLogs.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No cattle feeding records logged this month.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(feedingLogs, key = { it.id }) { tx ->
                                val itemUnit = items.find { it.id == tx.itemId }?.unit ?: "units"
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(modifier = Modifier.size(32.dp).background(DairyGreenLight, CircleShape), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Grass, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(16.dp))
                                            }
                                            Column {
                                                Text(tx.itemName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text("Fed: ${tx.quantity.toInt()} $itemUnit ${if (tx.reason.isNotBlank()) "• ${tx.reason}" else ""}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }

                                        Text(SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(tx.date)), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                4 -> {
                    // Automatic Monthly Stock Consumption & Reconciliation View
                    AutoConsumptionAndAuditView(
                        items = relevantItems,
                        monthTransactions = monthTransactions,
                        yearMonth = yearMonthString,
                        showFeed = showFeed,
                        onAuditItem = { auditDialogItem = it },
                        onSetOpeningStock = { openingStockDialogItem = it }
                    )
                }

                5 -> {
                    // 3-Month Smart Consumption Forecast & Reorder Advice
                    ForecastAndOrderAdvisorView(
                        forecastSummary = forecastSummary,
                        isLoading = isForecastLoading,
                        planningHorizonDays = planningHorizonDays,
                        safetyBufferDays = safetyBufferDays,
                        onSelectHorizon = { viewModel.setPlanningHorizonDays(it) },
                        onSelectBuffer = { viewModel.setSafetyBufferDays(it) },
                        onRefresh = { viewModel.refreshForecast() },
                        onBuyItem = { forecastItem ->
                            purchaseInitialQty = forecastItem.suggestedPurchaseQty
                            purchaseInitialRate = if (forecastItem.lastPurchasePrice > 0) forecastItem.lastPurchasePrice else forecastItem.costPerUnit
                            purchaseInitialSupplier = forecastItem.lastSupplier
                            purchaseDialogItem = forecastItem.item
                        },
                        onSetUsage = { forecastItem ->
                            usageDialogItem = forecastItem.item
                        },
                        onExportPdf = {
                            viewModel.exportForecastPdf { file ->
                                val intent = viewModel.repository.exportManager.shareFileIntent(file, "application/pdf")
                                context.startActivity(Intent.createChooser(intent, "Share Purchase Advice Report PDF"))
                            }
                        },
                        onExportCsv = {
                            viewModel.exportForecastCsv { file ->
                                val intent = viewModel.repository.exportManager.shareFileIntent(file, "text/csv")
                                context.startActivity(Intent.createChooser(intent, "Share Purchase Advice Report CSV"))
                            }
                        },
                        onShareWhatsapp = {
                            showWhatsappOrderDialog = true
                        }
                    )
                }
            }
        }
    }

    // ----------------- Dialogs -----------------
    purchaseDialogItem?.let { item ->
        BuyFeedStockModal(
            inventoryItems = relevantItems,
            preselectedItem = item,
            showFeed = showFeed,
            initialQuantity = purchaseInitialQty,
            initialRate = purchaseInitialRate,
            initialSupplier = purchaseInitialSupplier,
            onDismiss = {
                purchaseDialogItem = null
                purchaseInitialQty = null
                purchaseInitialRate = null
                purchaseInitialSupplier = null
            },
            onSave = { itemId, qty, rate, supplier, method, notes, date ->
                viewModel.recordStockPurchase(
                    itemId = itemId,
                    quantity = qty,
                    rate = rate,
                    date = date,
                    supplier = supplier,
                    paymentStatus = "PAID",
                    paymentMethod = method,
                    notes = notes
                ) { success, err ->
                    if (success) {
                        purchaseDialogItem = null
                        purchaseInitialQty = null
                        purchaseInitialRate = null
                        purchaseInitialSupplier = null
                    } else {
                        viewModel.showMessage(err ?: "Failed to record purchase")
                    }
                }
            }
        )
    }

    if (showWhatsappOrderDialog && forecastSummary != null && business != null) {
        ShareForecastWhatsappDialog(
            businessName = business!!.businessName,
            summary = forecastSummary!!,
            onDismiss = { showWhatsappOrderDialog = false },
            onShare = { text ->
                showWhatsappOrderDialog = false
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Purchase Order on WhatsApp"))
            }
        )
    }

    consumptionDialogItem?.let { item ->
        LogFeedingModal(
            item = item,
            supportedMilkTypes = business?.supportedMilkTypes ?: "BOTH",
            onDismiss = { consumptionDialogItem = null },
            onSave = { qty, reason, notes, date ->
                viewModel.recordStockConsumption(
                    itemId = item.id,
                    quantity = qty,
                    date = date,
                    reason = reason,
                    notes = notes
                ) { success, err ->
                    if (success) {
                        consumptionDialogItem = null
                    } else {
                        viewModel.showMessage(err ?: "Failed to log feeding")
                    }
                }
            }
        )
    }

    usageDialogItem?.let { item ->
        SetDailyUsageModal(
            item = item,
            onDismiss = { usageDialogItem = null },
            onSave = { dailyUsage ->
                viewModel.updateDailyUsage(item.id, dailyUsage)
                usageDialogItem = null
            }
        )
    }

    // Pre-Existing Opening Stock Modal
    openingStockDialogItem?.let { item ->
        SetInitialOpeningStockModal(
            item = item,
            accountStartDate = accountStartDate,
            onDismiss = { openingStockDialogItem = null },
            onSave = { openingQty, costRate, asOfDate ->
                viewModel.setInitialOpeningStock(
                    itemId = item.id,
                    openingStock = openingQty,
                    costPerUnit = costRate,
                    asOfDate = asOfDate
                ) { success, err ->
                    if (success) {
                        openingStockDialogItem = null
                    } else {
                        viewModel.showMessage(err ?: "Failed to set opening stock")
                    }
                }
            }
        )
    }

    // Dated Physical Stock Audit Modal
    auditDialogItem?.let { item ->
        AuditPhysicalStockModal(
            item = item,
            yearMonth = yearMonthString,
            monthTransactions = transactions.filter { it.itemId == item.id },
            onDismiss = { auditDialogItem = null },
            onSave = { count, notes, auditDate ->
                viewModel.verifyPhysicalClosingStock(
                    itemId = item.id,
                    yearMonth = yearMonthString,
                    physicalStock = count,
                    notes = notes,
                    verificationDate = auditDate
                ) { success, err ->
                    if (success) {
                        auditDialogItem = null
                    } else {
                        viewModel.showMessage(err ?: "Failed to save audit count")
                    }
                }
            }
        )
    }

    if (showAddItemDialog) {
        AddCustomItemModal(
            showFeed = showFeed,
            onDismiss = { showAddItemDialog = false },
            onSave = { name, unit, opening, cost, dailyUsage ->
                viewModel.saveInventoryItem(
                    id = null,
                    itemName = name,
                    unit = unit,
                    openingStock = opening,
                    costPerUnit = cost,
                    dailyUsage = dailyUsage
                )
                showAddItemDialog = false
            }
        )
    }

    if (showConvertMilkDialog) {
        ConvertMilkModal(
            items = items,
            onDismiss = { showConvertMilkDialog = false },
            onConvert = { rawMilkQty, targetItem, targetQty ->
                val rawMilkItem = items.find { it.itemName.contains("Raw", ignoreCase = true) }
                if (rawMilkItem != null) {
                    viewModel.recordStockConsumption(
                        itemId = rawMilkItem.id,
                        quantity = rawMilkQty,
                        date = System.currentTimeMillis(),
                        reason = "Milk Processing",
                        notes = "Converted to $targetQty ${targetItem.unit} ${targetItem.itemName}"
                    ) { _, _ -> }
                }
                viewModel.recordStockPurchase(
                    itemId = targetItem.id,
                    quantity = targetQty,
                    rate = 0.0,
                    date = System.currentTimeMillis(),
                    supplier = "In-House Dairy Processing",
                    paymentStatus = "PAID",
                    paymentMethod = "INTERNAL",
                    notes = "Converted from $rawMilkQty L Raw Milk"
                ) { success, _ ->
                    if (success) {
                        showConvertMilkDialog = false
                        viewModel.showMessage("Converted $rawMilkQty L milk into $targetQty ${targetItem.unit} ${targetItem.itemName} ✓")
                    }
                }
            }
        )
    }

    // Modal: Working Principle Explanation Guide
    if (showWorkingPrincipleInfo) {
        InventoryWorkingPrincipleModal(onDismiss = { showWorkingPrincipleInfo = false })
    }
}

@Composable
fun InventoryItemCard(
    item: InventoryItemEntity,
    isFeedItem: Boolean = com.example.ui.util.BusinessModeFeatures.isFeedItem(item.itemName),
    onBuyStock: () -> Unit,
    onLogConsumption: () -> Unit,
    onSetUsage: () -> Unit,
    onSetOpeningStock: () -> Unit,
    onAuditCount: () -> Unit
) {
    val statusColor = when (item.stockAlertStatus) {
        "OUT_OF_STOCK" -> DangerRed
        "LOW_STOCK" -> FreshGold
        else -> DairyGreen
    }

    val daysRemaining = item.estimatedDaysRemaining

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(item.itemName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        "Rate: ₹${item.costPerUnit.toInt()}/${item.unit} • Value: ₹${(item.currentStock * item.costPerUnit).toInt()}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${item.currentStock.toInt()} ${item.unit}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Pre-existing Opening Stock Badge with Click to Edit
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (item.isOpeningVerified) "📦 Initial Stock: ${item.openingStock.toInt()} ${item.unit}" else "📦 Initial Stock: Not entered (0 ${item.unit})",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "✏️ Set Initial Stock",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoyalBluePrimary,
                        modifier = Modifier.clickable(onClick = onSetOpeningStock)
                    )
                }
            }

            // Days remaining and daily consumption indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (daysRemaining != null) {
                    Text(
                        text = "⏳ ~${daysRemaining.toInt()} days left (@ ${item.dailyUsage?.toInt()} ${item.unit}/day)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (daysRemaining <= 7) DangerRed else RoyalBluePrimary
                    )
                } else {
                    Text(
                        text = "Daily use: Not configured",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "📋 Count",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = RoyalBluePrimary,
                        modifier = Modifier.clickable(onClick = onAuditCount)
                    )
                    Text(
                        text = "⚙️ Daily Use",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = RoyalBluePrimary,
                        modifier = Modifier.clickable(onClick = onSetUsage)
                    )
                }
            }

            HorizontalDivider()

            // Quick Action Buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onBuyStock,
                    colors = ButtonDefaults.buttonColors(containerColor = FreshGold),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.weight(1f).height(32.dp)
                ) {
                    Icon(Icons.Default.AddShoppingCart, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Buy Stock", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                FilledTonalButton(
                    onClick = onLogConsumption,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.weight(1f).height(32.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isFeedItem) "- Log Feed" else "- Record Usage", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AutoConsumptionAndAuditView(
    items: List<InventoryItemEntity>,
    monthTransactions: List<InventoryTransactionEntity>,
    yearMonth: String,
    showFeed: Boolean = true,
    onAuditItem: (InventoryItemEntity) -> Unit,
    onSetOpeningStock: (InventoryItemEntity) -> Unit
) {
    // Total aggregate calculations across all feed & dairy items
    val feedCalculationList = remember(items, monthTransactions) {
        items.map { item ->
            val purchases = monthTransactions.filter { it.itemId == item.id && it.transactionType == "PURCHASE" }
            val purchasedQty = purchases.sumOf { it.quantity }
            val purchasedSpend = purchases.sumOf { it.totalAmount }
            val loggedFeedingQty = monthTransactions.filter { it.itemId == item.id && it.transactionType == "CONSUMPTION" }.sumOf { it.quantity }

            // Automatic Consumption Formula: Opening + Purchases - Current Stock
            val calculatedActualConsumption = (item.openingStock + purchasedQty - item.currentStock).coerceAtLeast(0.0)
            val calculatedConsumptionCost = calculatedActualConsumption * item.costPerUnit
            val discrepancy = calculatedActualConsumption - loggedFeedingQty

            FeedAuditCalculation(
                item = item,
                openingStock = item.openingStock,
                purchasedQty = purchasedQty,
                purchasedSpend = purchasedSpend,
                currentStock = item.currentStock,
                calculatedConsumption = calculatedActualConsumption,
                consumptionCost = calculatedConsumptionCost,
                loggedFeeding = loggedFeedingQty,
                discrepancy = discrepancy
            )
        }
    }

    val totalMonthCalculatedConsumptionQty = remember(feedCalculationList) { feedCalculationList.sumOf { it.calculatedConsumption } }
    val totalMonthCalculatedConsumptionCost = remember(feedCalculationList) { feedCalculationList.sumOf { it.consumptionCost } }
    val totalPurchasedSpend = remember(feedCalculationList) { feedCalculationList.sumOf { it.purchasedSpend } }
    val totalLoggedFeeding = remember(feedCalculationList) { feedCalculationList.sumOf { it.loggedFeeding } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Automatic Calculation Insight Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(if (showFeed) "AUTOMATIC MONTHLY CONSUMPTION & RECONCILIATION" else "AUTOMATIC STOCK RECONCILIATION & AUDIT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                            Text("Formula: Opening Stock + Purchases − Ground Stock", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(shape = RoundedCornerShape(4.dp), color = DairyGreenLight) {
                            Text("AUTO-CALCULATED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = DairyGreen, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    // Key Calculated Metrics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = FreshGold.copy(alpha = 0.15f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(if (showFeed) "Feed Consumed" else "Stock Consumed / Sold", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${totalMonthCalculatedConsumptionQty.toInt()} units", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFB45309))
                                Text("Cost: ₹${totalMonthCalculatedConsumptionCost.toInt()}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = RoyalBlueLight
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Month Purchases", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("₹${totalPurchasedSpend.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = RoyalBluePrimary)
                                Text(if (showFeed) "Logged: ${totalLoggedFeeding.toInt()} kg" else "Active SKUs: ${items.size}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // Itemized Breakdown Cards
        items(feedCalculationList, key = { it.item.id }) { calc ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(calc.item.itemName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Rate: ₹${calc.item.costPerUnit.toInt()}/${calc.item.unit}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { onSetOpeningStock(calc.item) },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Set Opening", fontSize = 10.sp)
                            }

                            Button(
                                onClick = { onAuditItem(calc.item) },
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Count Audit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                    // Step-by-step Formula row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Opening", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${calc.openingStock.toInt()}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Text("+", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Purchased", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${calc.purchasedQty.toInt()}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = FreshGold)
                        }
                        Text("-", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Ground Stock", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${calc.currentStock.toInt()}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RoyalBluePrimary)
                        }
                        Text("=", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Actual Consumed", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DangerRed)
                            Text("${calc.calculatedConsumption.toInt()} ${calc.item.unit}", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = DangerRed)
                        }
                    }

                    // Feed Discrepancy & Value indicator
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Consumed Value: ₹${calc.consumptionCost.toInt()}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoyalBluePrimary
                            )

                            val discrText = if (calc.discrepancy.toInt() == 0) {
                                "✓ Balanced with Logged (${calc.loggedFeeding.toInt()} ${calc.item.unit})"
                            } else if (calc.discrepancy > 0) {
                                "⚠️ +${calc.discrepancy.toInt()} ${calc.item.unit} Unaccounted/Spoilage"
                            } else {
                                "ℹ️ ${calc.discrepancy.toInt()} ${calc.item.unit} variance"
                            }

                            Text(
                                text = discrText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (calc.discrepancy == 0.0) DairyGreen else Color(0xFFB45309)
                            )
                        }
                    }
                }
            }
        }
    }
}

data class FeedAuditCalculation(
    val item: InventoryItemEntity,
    val openingStock: Double,
    val purchasedQty: Double,
    val purchasedSpend: Double,
    val currentStock: Double,
    val calculatedConsumption: Double,
    val consumptionCost: Double,
    val loggedFeeding: Double,
    val discrepancy: Double
)

@Composable
fun SetInitialOpeningStockModal(
    item: InventoryItemEntity,
    accountStartDate: Long,
    onDismiss: () -> Unit,
    onSave: (Double, Double, Long) -> Unit
) {
    var openingStockText by remember { mutableStateOf(if (item.openingStock > 0) item.openingStock.toInt().toString() else "") }
    var costRateText by remember { mutableStateOf(if (item.costPerUnit > 0) item.costPerUnit.toInt().toString() else "32") }
    var asOfDateMillis by remember { mutableStateOf(if (accountStartDate > 0) accountStartDate else System.currentTimeMillis()) }

    val context = LocalContext.current
    val displayDateFormat = remember { SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()) }

    val openingQty = openingStockText.toDoubleOrNull() ?: 0.0
    val costRate = costRateText.toDoubleOrNull() ?: 0.0
    val totalValuation = openingQty * costRate

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Pre-Existing Initial Stock", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(item.itemName, fontSize = 12.sp, color = RoyalBluePrimary, fontWeight = FontWeight.SemiBold)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = null) }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = RoyalBlueLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            "💡 Before Account Opening Stock:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = RoyalBluePrimary
                        )
                        Text(
                            "If you already had cattle feed or supplies in your shed before joining MilkMate, enter that quantity here as your opening baseline.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // As of Date Selector
                Text("As-of Date *", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = asOfDateMillis }
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val newCal = Calendar.getInstance().apply {
                                    set(y, m, d, 0, 0, 0)
                                }
                                asOfDateMillis = newCal.timeInMillis
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(displayDateFormat.format(Date(asOfDateMillis)), fontWeight = FontWeight.SemiBold)
                }

                OutlinedTextField(
                    value = openingStockText,
                    onValueChange = { openingStockText = it },
                    label = { Text("Initial Stock on Hand (${item.unit}) *") },
                    placeholder = { Text("e.g. 500 kg or 10 bags") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = costRateText,
                    onValueChange = { costRateText = it },
                    label = { Text("Estimated Cost per ${item.unit} (₹)") },
                    placeholder = { Text("e.g. 32") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Total Initial Valuation Preview
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Initial Stock Asset Value:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("₹${totalValuation.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = RoyalBluePrimary)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (openingQty >= 0) {
                        onSave(openingQty, costRate, asOfDateMillis)
                    }
                },
                enabled = openingStockText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("✓ Save Initial Stock", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AuditPhysicalStockModal(
    item: InventoryItemEntity,
    yearMonth: String,
    monthTransactions: List<InventoryTransactionEntity>,
    onDismiss: () -> Unit,
    onSave: (Double, String, Long) -> Unit
) {
    val context = LocalContext.current
    var countText by remember { mutableStateOf(item.currentStock.toInt().toString()) }
    var notes by remember { mutableStateOf("") }
    var auditDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    val displayDateFormat = remember { SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()) }

    val physical = countText.toDoubleOrNull() ?: item.currentStock
    val variance = physical - item.currentStock

    // Find last verified count transaction or opening stock transaction
    val previousAuditTx = remember(monthTransactions) {
        monthTransactions.filter { (it.transactionType == "ADJUSTMENT" || it.transactionType == "OPENING_STOCK") && it.date < auditDateMillis }
            .maxByOrNull { it.date }
    }

    val elapsedDays = remember(previousAuditTx, auditDateMillis) {
        val prevDate = previousAuditTx?.date ?: (auditDateMillis - (30L * 24 * 3600 * 1000))
        val diffMs = auditDateMillis - prevDate
        (diffMs / (24 * 3600 * 1000)).coerceAtLeast(1L)
    }

    val purchasesInPeriod = remember(monthTransactions, previousAuditTx, auditDateMillis) {
        val prevDate = previousAuditTx?.date ?: 0L
        monthTransactions.filter { it.transactionType == "PURCHASE" && it.date in prevDate..auditDateMillis }
            .sumOf { it.quantity }
    }

    val calculatedPeriodConsumed = remember(previousAuditTx, item, purchasesInPeriod, physical) {
        val prevStock = previousAuditTx?.quantity ?: item.openingStock
        (prevStock + purchasesInPeriod - physical).coerceAtLeast(0.0)
    }

    val dailyBurnRate = remember(calculatedPeriodConsumed, elapsedDays) {
        if (elapsedDays > 0) calculatedPeriodConsumed / elapsedDays else 0.0
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Physical Stock Verification", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("${item.itemName} (${item.unit})", fontSize = 12.sp, color = RoyalBluePrimary, fontWeight = FontWeight.SemiBold)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = null) }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Verification Date Selector (Supports any day e.g. 5th of Month)
                Text("Verification Date (Count Date) *", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = auditDateMillis }
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val newCal = Calendar.getInstance().apply { set(y, m, d, 12, 0, 0) }
                                auditDateMillis = newCal.timeInMillis
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(displayDateFormat.format(Date(auditDateMillis)), fontWeight = FontWeight.SemiBold)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("System Book Stock: ${item.currentStock.toInt()} ${item.unit}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (previousAuditTx != null) {
                        Text("Last Count: ${previousAuditTx.quantity.toInt()} ${item.unit}", fontSize = 11.sp, color = RoyalBluePrimary)
                    }
                }

                OutlinedTextField(
                    value = countText,
                    onValueChange = { countText = it },
                    label = { Text("Actual Counted Physical Stock (${item.unit}) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Variance & Calculated Consumption Preview
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (variance == 0.0) DairyGreenLight else DangerRedLight.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Count Adjustment Variance:", fontSize = 11.sp)
                            Text(
                                "${if (variance > 0) "+" else ""}${variance.toInt()} ${item.unit}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (variance == 0.0) DairyGreen else DangerRed
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Period Consumed ($elapsedDays days):", fontSize = 11.sp)
                            Text(
                                "${calculatedPeriodConsumed.toInt()} ${item.unit} (~${String.format(Locale.US, "%.1f", dailyBurnRate)} ${item.unit}/day)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Verification Notes (e.g. 5th Oct shed count)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val c = countText.toDoubleOrNull() ?: 0.0
                    onSave(c, notes, auditDateMillis)
                },
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Confirm & Lock Verification")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun InventoryWorkingPrincipleModal(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.School, contentDescription = null, tint = RoyalBluePrimary)
                    Text("Dairy Inventory Principles", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = null) }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Point 1: Pre-existing stock
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("1️⃣ Pre-Existing Stock (Before Account Open)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RoyalBluePrimary)
                        Text(
                            "When joining MilkMate, farmers already have feed bags or mineral mixture in the shed. Use 'Set Initial Stock' to record this opening balance and cost rate. It establishes your baseline asset value without recording an artificial cash expense.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Point 2: Mid-Month Verifications
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("2️⃣ Mid-Month Audits (e.g. 5th of Month)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FreshGold)
                        Text(
                            "If you count physical stock on the 5th of October after 5th of September:\n• Total 30-Day Consumed = (Stock on 5 Sep + Purchases) − Stock on 5 Oct.\n• Daily Feed Burn Rate = Total Consumed ÷ 30 days.\n• The app allocates 25 days of burn to September and 5 days of burn to October!",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Point 3: Profit & Loss Consideration
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("3️⃣ Monthly Profit & Loss (P&L) Calculation", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DairyGreen)
                        Text(
                            "In your Monthly Profit Report, feed is calculated using true monthly consumption:\n• Month Consumed = (Opening on 1st + Month Purchases − Closing on 30th).\n• Even if you buy 3 months of feed in one bulk order, your monthly profit only charges the feed your dairy herd actually ate that month!",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary)) {
                Text("Got It!")
            }
        }
    )
}

@Composable
fun LogFeedingModal(
    item: InventoryItemEntity,
    onDismiss: () -> Unit,
    onSave: (Double, String, String, Long) -> Unit,
    supportedMilkTypes: String = "BOTH"
) {
    var qtyText by remember { mutableStateOf(if (item.dailyUsage != null && item.dailyUsage > 0) item.dailyUsage.toInt().toString() else "10") }
    var reason by remember { mutableStateOf("Morning Feeding") }
    var notes by remember { mutableStateOf("") }
    var dateMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    val qty = qtyText.toDoubleOrNull() ?: 0.0

    val headerTitle = when (supportedMilkTypes) {
        "COW_ONLY" -> "🐄 Feed Cows (${item.itemName})"
        "BUFFALO_ONLY" -> "🐃 Feed Buffaloes (${item.itemName})"
        else -> "🥣 Feed Cattle (${item.itemName})"
    }

    val milkingLabel = when (supportedMilkTypes) {
        "COW_ONLY" -> "Milking Cows"
        "BUFFALO_ONLY" -> "Milking Buffaloes"
        else -> "Milking Cattle"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(headerTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = null) }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Current Stock in Shed: ${item.currentStock.toInt()} ${item.unit}", fontSize = 12.sp, color = RoyalBluePrimary, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it },
                    label = { Text("Quantity Fed (${item.unit}) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Fast Stepper Chips
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(5, 10, 20, 50).forEach { addVal ->
                        SuggestionChip(
                            onClick = { qtyText = addVal.toString() },
                            label = { Text("$addVal ${item.unit}", fontSize = 11.sp) }
                        )
                    }
                }

                // Quick Reasons
                Text("Feeding Target / Shift:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Morning Feeding", "Evening Mash", milkingLabel, "Calves").forEach { r ->
                        FilterChip(
                            selected = reason == r,
                            onClick = { reason = r },
                            label = { Text(r, fontSize = 10.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (qty > 0) onSave(qty, reason, notes, dateMillis)
                },
                enabled = qty > 0,
                colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("✓ Log Consumption", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SetDailyUsageModal(
    item: InventoryItemEntity,
    onDismiss: () -> Unit,
    onSave: (Double?) -> Unit
) {
    var usageText by remember { mutableStateOf(item.dailyUsage?.toInt()?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Daily Herd Usage", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Specify how many ${item.unit} of ${item.itemName} are fed daily to calculate stock run-out forecast.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                OutlinedTextField(
                    value = usageText,
                    onValueChange = { usageText = it },
                    label = { Text("Daily Usage (${item.unit}/day)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val u = usageText.toDoubleOrNull()
                    onSave(u)
                },
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save Usage")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddCustomItemModal(
    onDismiss: () -> Unit,
    showFeed: Boolean = true,
    onSave: (String, String, Double, Double, Double?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf(if (showFeed) "kg" else "piece") }
    var openingStockText by remember { mutableStateOf("0") }
    var costText by remember { mutableStateOf(if (showFeed) "30" else "45") }
    var dailyUsageText by remember { mutableStateOf("5") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (showFeed) "Add New Feed / Stock Item" else "Add New Dairy SKU / Item", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (showFeed) "Item Name (e.g. Cottonseed Cake / Mustard Khali) *" else "Product Name (e.g. Desi Cow Ghee / Paneer / Pouch) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("kg", "L", "bag", "piece", "packet", "box").forEach { u ->
                        FilterChip(
                            selected = unit == u,
                            onClick = { unit = u },
                            label = { Text(u, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = openingStockText,
                    onValueChange = { openingStockText = it },
                    label = { Text(if (showFeed) "Pre-Existing Initial Stock in Shed ($unit)" else "Initial Stock on Shelf / Fridge ($unit)") },
                    placeholder = { Text("Enter current stock on hand") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = costText,
                    onValueChange = { costText = it },
                    label = { Text("Cost per $unit (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dailyUsageText,
                    onValueChange = { dailyUsageText = it },
                    label = { Text(if (showFeed) "Estimated Daily Herd Usage ($unit/day)" else "Estimated Daily Counter Sales ($unit/day)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val opening = openingStockText.toDoubleOrNull() ?: 0.0
                        val cost = costText.toDoubleOrNull() ?: 0.0
                        val daily = dailyUsageText.toDoubleOrNull()
                        onSave(name.trim(), unit.trim(), opening, cost, daily)
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Add Item")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ConvertMilkModal(
    items: List<InventoryItemEntity>,
    onDismiss: () -> Unit,
    onConvert: (Double, InventoryItemEntity, Double) -> Unit
) {
    var rawMilkLitersText by remember { mutableStateOf("20") }
    val productItems = items.filter { !it.itemName.contains("Raw", ignoreCase = true) }
    var selectedTargetItem by remember { mutableStateOf(productItems.find { it.itemName.contains("Paneer", ignoreCase = true) } ?: productItems.firstOrNull()) }
    var targetYieldText by remember { mutableStateOf("4.0") }

    val rawLiters = rawMilkLitersText.toDoubleOrNull() ?: 20.0
    val targetYield = targetYieldText.toDoubleOrNull() ?: 4.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("🔄 Convert Raw Milk into Products", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = null) }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Convert raw milk to paneer, curd, or ghee. Deducts raw milk and increases product stock.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                OutlinedTextField(
                    value = rawMilkLitersText,
                    onValueChange = {
                        rawMilkLitersText = it
                        val l = it.toDoubleOrNull() ?: 0.0
                        if (selectedTargetItem?.itemName?.contains("Paneer", ignoreCase = true) == true) {
                            targetYieldText = String.format(Locale.US, "%.1f", l * 0.20)
                        } else if (selectedTargetItem?.itemName?.contains("Ghee", ignoreCase = true) == true) {
                            targetYieldText = String.format(Locale.US, "%.1f", l * 0.06)
                        } else if (selectedTargetItem?.itemName?.contains("Curd", ignoreCase = true) == true) {
                            targetYieldText = String.format(Locale.US, "%.1f", l * 0.95)
                        }
                    },
                    label = { Text("Raw Milk Used (Liters) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Target Finished Product *", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                var expanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                        Text(selectedTargetItem?.itemName ?: "Select Product", fontWeight = FontWeight.SemiBold)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        productItems.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.itemName) },
                                onClick = {
                                    selectedTargetItem = p
                                    val l = rawMilkLitersText.toDoubleOrNull() ?: 0.0
                                    if (p.itemName.contains("Paneer", ignoreCase = true)) {
                                        targetYieldText = String.format(Locale.US, "%.1f", l * 0.20)
                                    } else if (p.itemName.contains("Ghee", ignoreCase = true)) {
                                        targetYieldText = String.format(Locale.US, "%.1f", l * 0.06)
                                    } else if (p.itemName.contains("Curd", ignoreCase = true)) {
                                        targetYieldText = String.format(Locale.US, "%.1f", l * 0.95)
                                    }
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = targetYieldText,
                    onValueChange = { targetYieldText = it },
                    label = { Text("Yield Produced (${selectedTargetItem?.unit ?: "kg"}) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = selectedTargetItem ?: return@Button
                    if (rawLiters > 0 && targetYield > 0) {
                        onConvert(rawLiters, target, targetYield)
                    }
                },
                enabled = selectedTargetItem != null && rawLiters > 0 && targetYield > 0,
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("✓ Convert & Update Stock", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// =========================================================================================
// 🔮 3-MONTH CONSUMPTION FORECASTING & PURCHASE ORDER ADVISOR COMPOSABLES
// =========================================================================================

@Composable
fun ForecastAndOrderAdvisorView(
    forecastSummary: ConsumptionForecastSummary?,
    isLoading: Boolean,
    planningHorizonDays: Int,
    safetyBufferDays: Int,
    onSelectHorizon: (Int) -> Unit,
    onSelectBuffer: (Int) -> Unit,
    onRefresh: () -> Unit,
    onBuyItem: (ItemForecastResult) -> Unit,
    onSetUsage: (ItemForecastResult) -> Unit,
    onExportPdf: () -> Unit,
    onExportCsv: () -> Unit,
    onShareWhatsapp: () -> Unit
) {
    if (isLoading && forecastSummary == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator(color = RoyalBluePrimary)
                Text("Analyzing 3 months of delivery & usage data...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    if (forecastSummary == null || forecastSummary.itemForecasts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.AutoGraph, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(48.dp))
                Text("No inventory items found to forecast", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Add cattle feed or dairy items to enable consumption forecasting.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onRefresh, colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary)) {
                    Text("Refresh Data")
                }
            }
        }
        return
    }

    var selectedStatusFilter by remember { mutableStateOf<ForecastUrgency?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf<String?>("ALL") }

    val filteredItems = remember(forecastSummary, selectedStatusFilter, selectedCategoryFilter) {
        forecastSummary.itemForecasts.filter { item ->
            val statusMatches = selectedStatusFilter == null || item.urgencyStatus == selectedStatusFilter
            val categoryMatches = when (selectedCategoryFilter) {
                "CATTLE_FEED" -> item.category == "CATTLE_FEED"
                "DAIRY_PRODUCT" -> item.category == "DAIRY_PRODUCT"
                else -> true
            }
            statusMatches && categoryMatches
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 1. Planning Horizon & Safety Buffer Config Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(16.dp))
                            Text("Forecast Planning Parameters", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = RoyalBluePrimary, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = onExportPdf, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = DangerRed, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = onShareWhatsapp, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Share, contentDescription = "Share Order", tint = DairyGreen, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // Horizon Selector
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Target Coverage Horizon:", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(15 to "15 Days", 30 to "30 Days (Rec)", 45 to "45 Days", 60 to "60 Days").forEach { (days, label) ->
                                val isSelected = planningHorizonDays == days
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) RoyalBluePrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onSelectHorizon(days) }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // Safety Buffer Selector
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Safety Stock Buffer:", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(3 to "+3 Days", 7 to "+7 Days (Standard)", 14 to "+14 Days (Conservative)").forEach { (days, label) ->
                                val isSelected = safetyBufferDays == days
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) FreshGold else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onSelectBuffer(days) }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. High-Level KPI Summary Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ForecastMetricCard(
                        title = "CRITICAL REORDERS",
                        value = "${forecastSummary.criticalItemsCount} Items",
                        subtext = "Runs out in ≤ 5 days",
                        icon = Icons.Default.Warning,
                        color = if (forecastSummary.criticalItemsCount > 0) DangerRed else DairyGreen,
                        bgColor = if (forecastSummary.criticalItemsCount > 0) DangerRedLight else DairyGreenLight,
                        modifier = Modifier.weight(1f)
                    )

                    ForecastMetricCard(
                        title = "EST. REORDER CAPITAL",
                        value = "₹${forecastSummary.totalEstimatedReorderCost.toInt()}",
                        subtext = "For ${planningHorizonDays + safetyBufferDays}d stock",
                        icon = Icons.Default.AccountBalanceWallet,
                        color = RoyalBluePrimary,
                        bgColor = Color(0xFFE8EAF6),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ForecastMetricCard(
                        title = "90-DAY MILK OUTPUT",
                        value = "${forecastSummary.total90DayMilkDelivered.toInt()} L",
                        subtext = String.format(Locale.US, "Trend: %+.1f%% growth", forecastSummary.milkTrendPercent),
                        icon = Icons.Default.LocalDrink,
                        color = DairyGreen,
                        bgColor = DairyGreenLight,
                        modifier = Modifier.weight(1f)
                    )

                    val nextExhaustionItem = forecastSummary.itemForecasts.firstOrNull { it.urgencyStatus == ForecastUrgency.CRITICAL || it.urgencyStatus == ForecastUrgency.WARNING }
                    ForecastMetricCard(
                        title = "NEXT STOCK-OUT",
                        value = if (nextExhaustionItem != null) "${nextExhaustionItem.daysRemaining} Days" else "Safe (>30d)",
                        subtext = nextExhaustionItem?.item?.itemName ?: "All stocks adequate",
                        icon = Icons.Default.Timer,
                        color = if (nextExhaustionItem != null) Color(0xFFE65100) else DairyGreen,
                        bgColor = if (nextExhaustionItem != null) Color(0xFFFFF3E0) else DairyGreenLight,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. 3-Month Milk & Consumption Correlation Analysis Card
        item {
            MonthlyDeliveryCorrelationCard(
                stats = forecastSummary.monthlyDeliveryStats,
                totalMilk = forecastSummary.total90DayMilkDelivered,
                milkTrendPercent = forecastSummary.milkTrendPercent
            )
        }

        // 4. Filter Chips Bar
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Status Filters
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedStatusFilter == null,
                        onClick = { selectedStatusFilter = null },
                        label = { Text("All (${forecastSummary.itemForecasts.size})", fontSize = 10.5.sp) }
                    )
                    FilterChip(
                        selected = selectedStatusFilter == ForecastUrgency.CRITICAL,
                        onClick = { selectedStatusFilter = if (selectedStatusFilter == ForecastUrgency.CRITICAL) null else ForecastUrgency.CRITICAL },
                        label = { Text("🚨 Critical (${forecastSummary.criticalItemsCount})", fontSize = 10.5.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DangerRedLight,
                            selectedLabelColor = DangerRed
                        )
                    )
                    FilterChip(
                        selected = selectedStatusFilter == ForecastUrgency.WARNING,
                        onClick = { selectedStatusFilter = if (selectedStatusFilter == ForecastUrgency.WARNING) null else ForecastUrgency.WARNING },
                        label = { Text("⚠️ Warning (${forecastSummary.warningItemsCount})", fontSize = 10.5.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFFF3E0),
                            selectedLabelColor = Color(0xFFE65100)
                        )
                    )
                    FilterChip(
                        selected = selectedStatusFilter == ForecastUrgency.ADEQUATE,
                        onClick = { selectedStatusFilter = if (selectedStatusFilter == ForecastUrgency.ADEQUATE) null else ForecastUrgency.ADEQUATE },
                        label = { Text("✅ Adequate (${forecastSummary.adequateItemsCount})", fontSize = 10.5.sp) }
                    )
                }

                // Category Filters
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedCategoryFilter == "ALL",
                        onClick = { selectedCategoryFilter = "ALL" },
                        label = { Text("All Categories", fontSize = 10.sp) }
                    )
                    FilterChip(
                        selected = selectedCategoryFilter == "CATTLE_FEED",
                        onClick = { selectedCategoryFilter = "CATTLE_FEED" },
                        label = { Text("🌾 Cattle Feed", fontSize = 10.sp) }
                    )
                    FilterChip(
                        selected = selectedCategoryFilter == "DAIRY_PRODUCT",
                        onClick = { selectedCategoryFilter = "DAIRY_PRODUCT" },
                        label = { Text("🥛 Dairy Products", fontSize = 10.sp) }
                    )
                }
            }
        }

        // 5. Item Forecast List
        if (filteredItems.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No items match the selected filter.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        } else {
            items(filteredItems, key = { it.item.id }) { forecastItem ->
                ItemForecastCard(
                    forecast = forecastItem,
                    planningHorizonDays = planningHorizonDays,
                    safetyBufferDays = safetyBufferDays,
                    onBuyStock = { onBuyItem(forecastItem) },
                    onSetUsage = { onSetUsage(forecastItem) }
                )
            }
        }
    }
}

@Composable
fun ForecastMetricCard(
    title: String,
    value: String,
    subtext: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(bgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }

            Column {
                Text(title, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = color)
                Text(subtext, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun MonthlyDeliveryCorrelationCard(
    stats: List<MonthlyDeliveryStats>,
    totalMilk: Double,
    milkTrendPercent: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.ShowChart, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(16.dp))
                    Text("3-Month Delivery & Herd Demand Scaling", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (milkTrendPercent >= 0) DairyGreenLight else Color(0xFFFFF3E0)
                ) {
                    Text(
                        text = String.format(Locale.US, "Trend: %+.1f%%", milkTrendPercent),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (milkTrendPercent >= 0) DairyGreen else Color(0xFFE65100),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // 3 Month Columns
            if (stats.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    stats.forEachIndexed { index, st ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (index == 2) RoyalBluePrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = if (index == 2) BorderStroke(1.dp, RoyalBluePrimary.copy(alpha = 0.3f)) else null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(st.monthName, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${st.totalLiters.toInt()} L", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (index == 2) RoyalBluePrimary else MaterialTheme.colorScheme.onSurface)
                                Text("${st.dailyAverageLiters.toInt()} L/day", fontSize = 8.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Explanation
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(14.dp))
                    Text(
                        "Biological Demand Factor: If milk delivery trend increases, the model automatically scales feed recommendations up by herd production elasticity so you order before running out.",
                        fontSize = 9.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ItemForecastCard(
    forecast: ItemForecastResult,
    planningHorizonDays: Int,
    safetyBufferDays: Int,
    onBuyStock: () -> Unit,
    onSetUsage: () -> Unit
) {
    val statusColor = when (forecast.urgencyStatus) {
        ForecastUrgency.CRITICAL -> DangerRed
        ForecastUrgency.WARNING -> Color(0xFFE65100)
        ForecastUrgency.ADEQUATE -> DairyGreen
        ForecastUrgency.WELL_STOCKED -> RoyalBluePrimary
    }

    val statusBg = when (forecast.urgencyStatus) {
        ForecastUrgency.CRITICAL -> DangerRedLight
        ForecastUrgency.WARNING -> Color(0xFFFFF3E0)
        ForecastUrgency.ADEQUATE -> DairyGreenLight
        ForecastUrgency.WELL_STOCKED -> Color(0xFFE8EAF6)
    }

    val statusLabel = when (forecast.urgencyStatus) {
        ForecastUrgency.CRITICAL -> if (forecast.currentStock <= 0) "🚨 OUT OF STOCK" else "🚨 Runs out in ${forecast.daysRemaining}d"
        ForecastUrgency.WARNING -> "⚠️ Reorder Soon (${forecast.daysRemaining}d left)"
        ForecastUrgency.ADEQUATE -> "✅ Adequate (${forecast.daysRemaining}d left)"
        ForecastUrgency.WELL_STOCKED -> "📦 Well-Stocked (${forecast.daysRemaining}d left)"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Item Name, Category, Urgency Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val icon = if (forecast.category == "CATTLE_FEED") Icons.Default.Grass else if (forecast.category == "DAIRY_PRODUCT") Icons.Default.LocalDrink else Icons.Default.Inventory2
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(statusBg, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = statusColor, modifier = Modifier.size(16.dp))
                    }

                    Column {
                        Text(forecast.item.itemName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            if (forecast.category == "CATTLE_FEED") "🌾 Cattle Feed" else if (forecast.category == "DAIRY_PRODUCT") "🥛 Dairy Product" else "📦 General Stock",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusBg
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Run-Out Date & Stock Meter
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current Stock: ${forecast.currentStock} ${forecast.unit}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (forecast.runOutDateMillis != null) {
                        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        Text(
                            text = "Exhausts: ${sdf.format(Date(forecast.runOutDateMillis))}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor
                        )
                    }
                }

                val target30dStock = (planningHorizonDays + safetyBufferDays) * forecast.effectiveProjectedDailyUsage
                val progress = if (target30dStock > 0) (forecast.currentStock / target30dStock).toFloat().coerceIn(0f, 1f) else 1f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = statusColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            }

            // 3-Month Historical Usage Breakdown Matrix
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("3-Mo Total", fontSize = 8.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${forecast.total90DayUsage.toInt()} ${forecast.unit}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Divider(modifier = Modifier.height(20.dp).width(1.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Daily Burn", fontSize = 8.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${String.format(Locale.US, "%.1f", forecast.effectiveProjectedDailyUsage)}/d", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Divider(modifier = Modifier.height(20.dp).width(1.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Usage Trend", fontSize = 8.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = String.format(Locale.US, "%+.1f%%", forecast.usageTrendPercent),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (forecast.usageTrendPercent > 5) DangerRed else DairyGreen
                        )
                    }

                    if (forecast.feedToMilkRatio != null) {
                        Divider(modifier = Modifier.height(20.dp).width(1.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Feed/Milk", fontSize = 8.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${forecast.feedToMilkRatio} kg/L", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                        }
                    }
                }
            }

            // Demand Insight narrative
            if (forecast.demandInsight.isNotBlank()) {
                Text(
                    text = forecast.demandInsight,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 13.5.sp
                )
            }

            // Purchase Order Advice Box
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (forecast.suggestedPurchaseQty > 0) RoyalBluePrimary.copy(alpha = 0.08f) else DairyGreenLight.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, if (forecast.suggestedPurchaseQty > 0) RoyalBluePrimary.copy(alpha = 0.3f) else DairyGreen.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            if (forecast.suggestedPurchaseQty > 0) "SUGGESTED REORDER QUANTITY" else "STOCK STATUS",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (forecast.suggestedPurchaseQty > 0) RoyalBluePrimary else DairyGreen
                        )

                        Text(
                            text = if (forecast.suggestedPurchaseQty > 0) forecast.suggestedPackageUnits else "Sufficient Stock on Hand",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (forecast.suggestedPurchaseQty > 0) RoyalBluePrimary else DairyGreen
                        )

                        if (forecast.suggestedPurchaseQty > 0) {
                            Text(
                                text = "Est. ₹${forecast.estimatedPurchaseCost.toInt()} (@ ₹${forecast.costPerUnit.toInt()}/${forecast.unit}) • ${forecast.lastSupplier}",
                                fontSize = 9.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = onSetUsage,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Adjust Daily Usage", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        }

                        if (forecast.suggestedPurchaseQty > 0) {
                            Button(
                                onClick = onBuyStock,
                                colors = ButtonDefaults.buttonColors(containerColor = FreshGold),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Buy Stock", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShareForecastWhatsappDialog(
    businessName: String,
    summary: ConsumptionForecastSummary,
    onDismiss: () -> Unit,
    onShare: (String) -> Unit
) {
    val itemsToOrder = summary.itemForecasts.filter { it.suggestedPurchaseQty > 0 }
    val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())

    val orderText = buildString {
        appendLine("🥛 *MILKMATE PURCHASE ORDER - ${businessName.ifBlank { "Dairy Farm" }}*")
        appendLine("📅 Date: $dateStr")
        appendLine("🎯 Coverage Plan: ${summary.planningHorizonDays} Days (+${summary.safetyBufferDays}d Safety Buffer)")
        appendLine("📊 90-Day Milk Delivered: ${summary.total90DayMilkDelivered.toInt()} Liters")
        appendLine("----------------------------------------")
        if (itemsToOrder.isEmpty()) {
            appendLine("All feed & dairy supplies are currently well-stocked!")
        } else {
            itemsToOrder.forEachIndexed { i, it ->
                appendLine("${i + 1}. *${it.item.itemName}*: ${it.suggestedPackageUnits}")
                appendLine("   • Est. Cost: ₹${it.estimatedPurchaseCost.toInt()} (@ ₹${it.costPerUnit.toInt()}/${it.unit})")
                if (it.lastSupplier.isNotBlank() && it.lastSupplier != "Local Feed Dealer") {
                    appendLine("   • Dealer: ${it.lastSupplier}")
                }
            }
        }
        appendLine("----------------------------------------")
        appendLine("💰 *Total Estimated Value: ₹${summary.totalEstimatedReorderCost.toInt()}*")
        appendLine("Generated via MilkMate Dairy OS")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("💬 Send Order via WhatsApp", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = null) }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Send formatted purchase order directly to your feed dealers / suppliers:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = orderText,
                        fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onShare(orderText) },
                colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share on WhatsApp", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

