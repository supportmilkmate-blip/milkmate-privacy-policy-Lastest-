package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MilkMateViewModel
import com.example.ui.theme.*
import com.example.ui.util.appStr
import java.text.SimpleDateFormat
import java.util.*

enum class AlertFilter {
    ALL, HERD_HEALTH, MILKING, DELIVERIES, PAYMENTS, FEED_STOCK
}

data class OperationalAlert(
    val id: String,
    val category: AlertFilter,
    val title: String,
    val message: String,
    val timeLabel: String,
    val priority: AlertPriority,
    val icon: ImageVector,
    val iconBg: Color,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

enum class AlertPriority {
    CRITICAL, HIGH, NORMAL, INFO
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterDialog(
    viewModel: MilkMateViewModel,
    onDismiss: () -> Unit,
    onNavigateToDelivery: () -> Unit,
    onNavigateToPayments: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToFarm: () -> Unit = {},
    onNavigateToInventory: () -> Unit = {}
) {
    val context = LocalContext.current
    val session by viewModel.sessionState.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val deliveries by viewModel.todayDeliveries.collectAsStateWithLifecycle()
    val inventory by viewModel.inventoryItems.collectAsStateWithLifecycle()
    val cattleList by viewModel.cattleList.collectAsStateWithLifecycle()
    val vaccinations by viewModel.allVaccinationRecords.collectAsStateWithLifecycle()
    val dewormings by viewModel.allDewormingRecords.collectAsStateWithLifecycle()
    val breedings by viewModel.allBreedingRecords.collectAsStateWithLifecycle()
    val milkingRecords by viewModel.allMilkingRecords.collectAsStateWithLifecycle()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Alert Radar, 1: Schedule & Shift Alarms, 2: Notification Controls
    var selectedFilter by remember { mutableStateOf(AlertFilter.ALL) }

    // Notification Preferences Local State
    var notifMasterEnabled by remember(session.notificationsEnabled) { mutableStateOf(session.notificationsEnabled) }
    var morningEnabled by remember(session.morningReminderEnabled) { mutableStateOf(session.morningReminderEnabled) }
    var morningTime by remember(session.morningReminderTime) { mutableStateOf(session.morningReminderTime) }
    var afternoonEnabled by remember(session.afternoonReminderEnabled) { mutableStateOf(session.afternoonReminderEnabled) }
    var afternoonTime by remember(session.afternoonReminderTime) { mutableStateOf(session.afternoonReminderTime) }
    var eveningEnabled by remember(session.eveningReminderEnabled) { mutableStateOf(session.eveningReminderEnabled) }
    var eveningTime by remember(session.eveningReminderTime) { mutableStateOf(session.eveningReminderTime) }
    var advanceMinutes by remember(session.advanceAlertMinutes) { mutableIntStateOf(session.advanceAlertMinutes) }

    var milkingEnabled by remember(session.milkingReminderEnabled) { mutableStateOf(session.milkingReminderEnabled) }
    var deliveryEnabled by remember(session.deliveryReminderEnabled) { mutableStateOf(session.deliveryReminderEnabled) }
    var vaccineEnabled by remember(session.vaccineAlertEnabled) { mutableStateOf(session.vaccineAlertEnabled) }
    var dewormingEnabled by remember(session.dewormingAlertEnabled) { mutableStateOf(session.dewormingAlertEnabled) }
    var breedingEnabled by remember(session.breedingCalvingAlertEnabled) { mutableStateOf(session.breedingCalvingAlertEnabled) }
    var paymentDueEnabled by remember(session.paymentDueAlertsEnabled) { mutableStateOf(session.paymentDueAlertsEnabled) }
    var paymentDueThresholdText by remember(session.paymentDueThreshold) { mutableStateOf(session.paymentDueThreshold.toInt().toString()) }
    var lowStockEnabled by remember(session.lowStockAlertsEnabled) { mutableStateOf(session.lowStockAlertsEnabled) }
    var soundEnabled by remember(session.soundAlertsEnabled) { mutableStateOf(session.soundAlertsEnabled) }
    var vibrationEnabled by remember(session.vibrationAlertsEnabled) { mutableStateOf(session.vibrationAlertsEnabled) }
    var dailySummaryEnabled by remember(session.dailySummaryAlertsEnabled) { mutableStateOf(session.dailySummaryAlertsEnabled) }
    var dailySummaryTime by remember(session.dailySummaryTime) { mutableStateOf(session.dailySummaryTime) }

    // Dynamic Live Alerts Generation
    val now = remember { System.currentTimeMillis() }
    val sevenDaysMillis = 7L * 24L * 60L * 60L * 1000L
    val fourteenDaysMillis = 14L * 24L * 60L * 60L * 1000L
    val sdf = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }

    val currentThreshold = paymentDueThresholdText.toDoubleOrNull() ?: 500.0

    val generatedAlerts = remember(
        customers, deliveries, inventory, cattleList, vaccinations, dewormings, breedings, milkingRecords,
        currentThreshold, now
    ) {
        val list = mutableListOf<OperationalAlert>()

        // 1. Milking Logs Alerts
        val todayMidnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val todayMilking = milkingRecords.filter { it.dateEpochMidnight >= todayMidnight }
        val lactatingCattle = cattleList.filter { it.lactationStage.equals("LACTATING", true) }

        if (lactatingCattle.isNotEmpty()) {
            val morningMilked = todayMilking.filter { it.shift == "MORNING" }.map { it.cattleId }.toSet()
            val morningMissing = lactatingCattle.filter { it.id !in morningMilked }
            if (morningMissing.isNotEmpty()) {
                list.add(
                    OperationalAlert(
                        id = "MILK_MORNING_MISSING",
                        category = AlertFilter.MILKING,
                        title = "Morning Milking Pending",
                        message = "${morningMissing.size} lactating cows/buffs have not been milked or logged for Morning Shift yet.",
                        timeLabel = "Morning Shift",
                        priority = AlertPriority.HIGH,
                        icon = Icons.Default.Pets,
                        iconBg = RoyalBluePrimary,
                        actionLabel = "Record Milking",
                        onAction = {
                            onDismiss()
                            onNavigateToFarm()
                        }
                    )
                )
            }
        }

        // 2. Deliveries & Dispatches Alerts
        val morningDeliveries = deliveries.filter { it.shift == "MORNING" }
        val morningPending = morningDeliveries.filter { !it.isDelivered }
        if (morningPending.isNotEmpty()) {
            list.add(
                OperationalAlert(
                    id = "DELIVERY_MORNING_PENDING",
                    category = AlertFilter.DELIVERIES,
                    title = "Morning Deliveries Incomplete",
                    message = "${morningPending.size} of ${morningDeliveries.size} customer delivery bottles pending delivery confirmation.",
                    timeLabel = "Live Route",
                    priority = AlertPriority.HIGH,
                    icon = Icons.Default.LocalShipping,
                    iconBg = FreshGold,
                    actionLabel = "Open Route",
                    onAction = {
                        onDismiss()
                        onNavigateToDelivery()
                    }
                )
            )
        }

        // 3. Customer Payment Overdue Alerts
        val overdueCustomers = customers.filter { it.outstandingBalance > currentThreshold }
            .sortedByDescending { it.outstandingBalance }
        overdueCustomers.take(4).forEach { cust ->
            list.add(
                OperationalAlert(
                    id = "PAY_DUE_${cust.id}",
                    category = AlertFilter.PAYMENTS,
                    title = "Payment Overdue: ${cust.name}",
                    message = "Outstanding bill balance is ₹${cust.outstandingBalance.toInt()} (Route: ${cust.route.ifBlank { "Main" }}).",
                    timeLabel = "Due ₹${cust.outstandingBalance.toInt()}",
                    priority = if (cust.outstandingBalance > currentThreshold * 2) AlertPriority.CRITICAL else AlertPriority.HIGH,
                    icon = Icons.Default.AccountBalanceWallet,
                    iconBg = DangerRed,
                    actionLabel = "WhatsApp Bill 💬",
                    onAction = {
                        sendWhatsAppDueReminder(context, cust.name, cust.mobile, cust.outstandingBalance)
                    }
                )
            )
        }

        // 4. Vaccination Due & Overdue Alerts
        vaccinations.forEach { vac ->
            val daysDiff = (vac.nextDueDate - now) / (1000L * 60L * 60L * 24L)
            if (vac.nextDueDate > 0 && daysDiff <= 7) {
                val isOverdue = daysDiff < 0
                list.add(
                    OperationalAlert(
                        id = "VAC_${vac.id}",
                        category = AlertFilter.HERD_HEALTH,
                        title = if (isOverdue) "🚨 Vaccination Overdue: ${vac.vaccineName}" else "💉 Vaccine Due Soon: ${vac.vaccineName}",
                        message = "Cattle Tag #${vac.cattleTag} due for ${vac.vaccineName} booster (${if (isOverdue) "${-daysDiff}d overdue" else "in $daysDiff days, ${sdf.format(Date(vac.nextDueDate))}"}).",
                        timeLabel = if (isOverdue) "Overdue" else "Due ${sdf.format(Date(vac.nextDueDate))}",
                        priority = if (isOverdue) AlertPriority.CRITICAL else AlertPriority.HIGH,
                        icon = Icons.Default.MedicalServices,
                        iconBg = if (isOverdue) DangerRed else RoyalBluePrimary,
                        actionLabel = "View Health",
                        onAction = {
                            onDismiss()
                            onNavigateToFarm()
                        }
                    )
                )
            }
        }

        // 5. Deworming Due Alerts
        dewormings.forEach { dw ->
            val daysDiff = (dw.nextDueDate - now) / (1000L * 60L * 60L * 24L)
            if (dw.nextDueDate > 0 && daysDiff <= 7) {
                val isOverdue = daysDiff < 0
                list.add(
                    OperationalAlert(
                        id = "DEW_${dw.id}",
                        category = AlertFilter.HERD_HEALTH,
                        title = if (isOverdue) "💊 Deworming Overdue" else "💊 Deworming Due",
                        message = "Cattle Tag #${dw.cattleTag} due for 90-day repeat dose (${dw.dewormerSalt}).",
                        timeLabel = if (isOverdue) "Overdue" else "Due ${sdf.format(Date(dw.nextDueDate))}",
                        priority = if (isOverdue) AlertPriority.HIGH else AlertPriority.NORMAL,
                        icon = Icons.Default.Vaccines,
                        iconBg = GrassGreen,
                        actionLabel = "Administer Dose",
                        onAction = {
                            onDismiss()
                            onNavigateToFarm()
                        }
                    )
                )
            }
        }

        // 6. Breeding & Calving Radar
        cattleList.forEach { c ->
            if (c.expectedCalvingDate > 0) {
                val daysToCalve = (c.expectedCalvingDate - now) / (1000L * 60L * 60L * 24L)
                if (daysToCalve in -5..14) {
                    list.add(
                        OperationalAlert(
                            id = "CALV_${c.id}",
                            category = AlertFilter.HERD_HEALTH,
                            title = if (daysToCalve <= 0) "🍼 Expected Calving Alert!" else "🤰 Calving Countdown (${c.tagNumber})",
                            message = "${c.name.ifBlank { "Animal" }} (#${c.tagNumber}) expected to calve ${if (daysToCalve <= 0) "today / overdue" else "in $daysToCalve days (${sdf.format(Date(c.expectedCalvingDate))})"}. Prepare maternity stall.",
                            timeLabel = "Maternity Alert",
                            priority = AlertPriority.CRITICAL,
                            icon = Icons.Default.ChildCare,
                            iconBg = WarmHoney,
                            actionLabel = "Calving Logs",
                            onAction = {
                                onDismiss()
                                onNavigateToFarm()
                            }
                        )
                    )
                }
            }
            if (c.breedingStatus.equals("INSEMINATED", true) && c.lastInseminationDate > 0) {
                val daysSinceAI = (now - c.lastInseminationDate) / (1000L * 60L * 60L * 24L)
                if (daysSinceAI in 60..90) {
                    list.add(
                        OperationalAlert(
                            id = "PD_${c.id}",
                            category = AlertFilter.HERD_HEALTH,
                            title = "🔍 Pregnancy Diagnosis (PD) Due",
                            message = "Cattle #${c.tagNumber} was inseminated $daysSinceAI days ago. Call veterinary doctor for manual/ultrasound PD check.",
                            timeLabel = "${daysSinceAI}d Post-AI",
                            priority = AlertPriority.NORMAL,
                            icon = Icons.Default.Search,
                            iconBg = OceanBlueAccent,
                            actionLabel = "Log PD Result",
                            onAction = {
                                onDismiss()
                                onNavigateToFarm()
                            }
                        )
                    )
                }
            }
        }

        // 7. Low Feed Stock Alert
        val lowStockItems = inventory.filter { it.stockAlertStatus != "NORMAL" || it.currentStock <= 5.0 }
        if (lowStockItems.isNotEmpty()) {
            list.add(
                OperationalAlert(
                    id = "INV_LOW_FEED",
                    category = AlertFilter.FEED_STOCK,
                    title = "🌾 Low Feed Stock Warning",
                    message = "${lowStockItems.size} items (${lowStockItems.take(2).joinToString { it.itemName }}) running low in barn inventory.",
                    timeLabel = "Stock Buffer Low",
                    priority = AlertPriority.HIGH,
                    icon = Icons.Default.Inventory2,
                    iconBg = GoldenOrange,
                    actionLabel = "Order / Restock",
                    onAction = {
                        onDismiss()
                        onNavigateToInventory()
                    }
                )
            )
        }

        // 8. End-of-Day Closing Alert
        list.add(
            OperationalAlert(
                id = "EOD_CLOSING",
                category = AlertFilter.ALL,
                title = "📊 Daily Ledger & Expense Closing",
                message = "Log all daily feed, diesel, labour wages, and vendor cash expenses before closing the dairy books.",
                timeLabel = "$dailySummaryTime Summary",
                priority = AlertPriority.INFO,
                icon = Icons.Default.ReceiptLong,
                iconBg = ForestGreenAccent,
                actionLabel = "Open Expense Ledger",
                onAction = {
                    onDismiss()
                    onNavigateToExpenses()
                }
            )
        )

        list
    }

    val filteredAlerts = remember(generatedAlerts, selectedFilter) {
        if (selectedFilter == AlertFilter.ALL) generatedAlerts
        else generatedAlerts.filter { it.category == selectedFilter }
    }

    // Expanded Hour Choices (Comprehensive Dairy Operations Schedule)
    val morningHours = listOf("03:00", "03:30", "04:00", "04:30", "05:00", "05:30", "06:00", "06:30", "07:00", "07:30", "08:00", "08:30", "09:00", "10:00", "11:00", "12:00")
    val afternoonHours = listOf("12:00", "12:30", "13:00", "13:30", "14:00", "14:30", "15:00", "15:30", "16:00")
    val eveningHours = listOf("16:00", "16:30", "17:00", "17:30", "18:00", "18:30", "19:00", "19:30", "20:00", "20:30", "21:00", "21:30", "22:00", "23:00")
    val closingHours = listOf("18:00", "19:00", "20:00", "20:30", "21:00", "21:30", "22:00", "22:30", "23:00")
    val advanceOptions = listOf(
        0 to "At event time (0m)",
        15 to "15 min before",
        30 to "30 min before",
        60 to "1 hour before",
        120 to "2 hours before",
        240 to "4 hours before",
        360 to "6 hours before",
        720 to "12 hours before",
        1440 to "1 day before"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Dairy Smart Alerts & Radar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${generatedAlerts.size} active signals • Dynamic time control",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // 3 Interactive Tabs
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🚨 Live Radar", fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal)
                                Spacer(modifier = Modifier.width(4.dp))
                                Badge(containerColor = if (generatedAlerts.any { it.priority == AlertPriority.CRITICAL }) DangerRed else FreshGold) {
                                    Text("${generatedAlerts.size}", color = Color.White, fontSize = 10.sp)
                                }
                            }
                        }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = {
                            Text("⏰ Shift Hours", fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal)
                        }
                    )
                    Tab(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        text = {
                            Text("⚙️ Alert Setup", fontWeight = if (activeTab == 2) FontWeight.Bold else FontWeight.Normal)
                        }
                    )
                }

                when (activeTab) {
                    0 -> {
                        // --- TAB 0: Live Alert Radar ---
                        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                            // Filter Chips Row
                            LazyRow(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(AlertFilter.entries) { filter ->
                                    val label = when (filter) {
                                        AlertFilter.ALL -> "All (${generatedAlerts.size})"
                                        AlertFilter.HERD_HEALTH -> "🩺 Health"
                                        AlertFilter.MILKING -> "🥛 Milking"
                                        AlertFilter.DELIVERIES -> "🚚 Delivery"
                                        AlertFilter.PAYMENTS -> "💰 Due Bills"
                                        AlertFilter.FEED_STOCK -> "🌾 Feed"
                                    }
                                    FilterChip(
                                        selected = selectedFilter == filter,
                                        onClick = { selectedFilter = filter },
                                        label = { Text(label, fontSize = 11.sp, fontWeight = if (selectedFilter == filter) FontWeight.Bold else FontWeight.Normal) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            }

                            if (filteredAlerts.isEmpty()) {
                                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(48.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("All Operational Items Clear! ✓", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("No pending alerts in this category.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(filteredAlerts, key = { it.id }) { alert ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = when (alert.priority) {
                                                    AlertPriority.CRITICAL -> DangerRedLight
                                                    AlertPriority.HIGH -> FreshGoldLight
                                                    AlertPriority.NORMAL -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                                    AlertPriority.INFO -> MintGreenLight
                                                }
                                            ),
                                            border = if (alert.priority == AlertPriority.CRITICAL) androidx.compose.foundation.BorderStroke(1.dp, DangerRed.copy(alpha = 0.4f)) else null
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .background(alert.iconBg, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(alert.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(alert.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = alert.iconBg.copy(alpha = 0.15f)
                                                        ) {
                                                            Text(alert.timeLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = alert.iconBg, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(3.dp))
                                                    Text(alert.message, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 15.sp)

                                                    if (alert.actionLabel != null && alert.onAction != null) {
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        FilledTonalButton(
                                                            onClick = alert.onAction,
                                                            shape = RoundedCornerShape(6.dp),
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                            modifier = Modifier.height(28.dp)
                                                        ) {
                                                            Text(alert.actionLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // --- TAB 1: Expanded Shift Hours & Time Control ---
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "⏰ Expanded Shift Timing & Reminder Clock",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Configure the exact operational hours for your dairy farm rounds, dispatches, and daily closing.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // 1. Morning Shift Time Control (03:00 to 12:00)
                            ShiftTimeCard(
                                title = "☀️ Morning Shift & Milking Hour",
                                subtitle = "Reminder triggers at $morningTime for morning herd milking & routes",
                                enabled = morningEnabled,
                                onToggle = { morningEnabled = it },
                                selectedTime = morningTime,
                                timeOptions = morningHours,
                                onSelectTime = { morningTime = it }
                            )

                            // 2. Afternoon Shift Time Control (12:00 to 16:00)
                            ShiftTimeCard(
                                title = "🌤️ Afternoon Milking & Chilling Round",
                                subtitle = "Mid-day reminder at $afternoonTime for high-yield dairy herds",
                                enabled = afternoonEnabled,
                                onToggle = { afternoonEnabled = it },
                                selectedTime = afternoonTime,
                                timeOptions = afternoonHours,
                                onSelectTime = { afternoonTime = it }
                            )

                            // 3. Evening Shift Time Control (16:00 to 23:00)
                            ShiftTimeCard(
                                title = "🌙 Evening Shift & Distribution Hour",
                                subtitle = "Evening reminder at $eveningTime for second shift delivery & milk collection",
                                enabled = eveningEnabled,
                                onToggle = { eveningEnabled = it },
                                selectedTime = eveningTime,
                                timeOptions = eveningHours,
                                onSelectTime = { eveningTime = it }
                            )

                            // 4. Daily Closing Hour (18:00 to 23:30)
                            ShiftTimeCard(
                                title = "📊 End-of-Day Accounts & P&L Closing",
                                subtitle = "Daily summary and ledger wrap-up alert at $dailySummaryTime",
                                enabled = dailySummaryEnabled,
                                onToggle = { dailySummaryEnabled = it },
                                selectedTime = dailySummaryTime,
                                timeOptions = closingHours,
                                onSelectTime = { dailySummaryTime = it }
                            )

                            // 5. Advance Notice Offset
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.HourglassTop, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Advance Reminder Lead Time", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Text("Get alerted ahead of time to prepare cans, feed, or transport vehicles.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(advanceOptions) { (mins, label) ->
                                            FilterChip(
                                                selected = advanceMinutes == mins,
                                                onClick = { advanceMinutes = mins },
                                                label = { Text(label, fontSize = 10.sp) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // --- TAB 2: Operational Categories & Master Switch ---
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Master Toggle
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = if (notifMasterEnabled) DairyGreenLight else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Dairy Notification Radar", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text(if (notifMasterEnabled) "🟢 Master Alert Engine Active" else "🔴 All Notifications Muted", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(checked = notifMasterEnabled, onCheckedChange = { notifMasterEnabled = it })
                                }
                            }

                            if (notifMasterEnabled) {
                                Text("Select Alert Modules to Activate:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)

                                CategoryToggleCard(
                                    title = "🥛 Milking & Herd Yield Logs",
                                    subtitle = "Alarms when shifts are active and cows haven't been milked",
                                    checked = milkingEnabled,
                                    onCheckedChange = { milkingEnabled = it }
                                )

                                CategoryToggleCard(
                                    title = "🚚 Milk Routes & Dispatches",
                                    subtitle = "Undelivered milk alerts and delivery completion status",
                                    checked = deliveryEnabled,
                                    onCheckedChange = { deliveryEnabled = it }
                                )

                                CategoryToggleCard(
                                    title = "💉 Vaccination Booster Schedule",
                                    subtitle = "7-day and 3-day advance alerts for FMD, HS, BQ, Anthrax, Brucellosis",
                                    checked = vaccineEnabled,
                                    onCheckedChange = { vaccineEnabled = it }
                                )

                                CategoryToggleCard(
                                    title = "💊 90-Day Deworming Cycle",
                                    subtitle = "Periodic salt rotation alerts (Albendazole, Fenbendazole, Ivermectin)",
                                    checked = dewormingEnabled,
                                    onCheckedChange = { dewormingEnabled = it }
                                )

                                CategoryToggleCard(
                                    title = "🧬 Breeding, Calving & PD Check",
                                    subtitle = "Heat cycle, 60-day PD diagnosis, and 14-day calving countdowns",
                                    checked = breedingEnabled,
                                    onCheckedChange = { breedingEnabled = it }
                                )

                                // Customer Payment Overdue Threshold
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("💰 Customer Payment Due Alert", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                Text("Alerts when customer outstanding balance exceeds limit", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Switch(checked = paymentDueEnabled, onCheckedChange = { paymentDueEnabled = it })
                                        }

                                        if (paymentDueEnabled) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Text("Threshold (₹):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                listOf("100", "200", "500", "1000", "2000", "5000").forEach { amt ->
                                                    FilterChip(
                                                        selected = paymentDueThresholdText == amt,
                                                        onClick = { paymentDueThresholdText = amt },
                                                        label = { Text("₹$amt", fontSize = 10.sp) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                CategoryToggleCard(
                                    title = "🌾 Low Feed Stock Warnings",
                                    subtitle = "Alert when cattle feed, cake, bran, or mineral bags fall below safety buffer",
                                    checked = lowStockEnabled,
                                    onCheckedChange = { lowStockEnabled = it }
                                )

                                // Sound & Vibration Controls
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text("🔊 Sound Alert", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Switch(checked = soundEnabled, onCheckedChange = { soundEnabled = it })
                                        }
                                    }
                                    Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text("📳 Vibration", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Switch(checked = vibrationEnabled, onCheckedChange = { vibrationEnabled = it })
                                        }
                                    }
                                }

                                // Test Notification Trigger
                                OutlinedButton(
                                    onClick = {
                                        viewModel.showMessage("🔔 Smart Notification Triggered: Morning Milking & Delivery radar active ($morningTime)!")
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Trigger Live Test Notification", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Footer Save & Close Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            viewModel.updateNotificationPreferences(
                                notificationsEnabled = notifMasterEnabled,
                                morningEnabled = morningEnabled,
                                morningTime = morningTime,
                                afternoonEnabled = afternoonEnabled,
                                afternoonTime = afternoonTime,
                                eveningEnabled = eveningEnabled,
                                eveningTime = eveningTime,
                                advanceMinutes = advanceMinutes,
                                milkingEnabled = milkingEnabled,
                                deliveryEnabled = deliveryEnabled,
                                vaccineEnabled = vaccineEnabled,
                                dewormingEnabled = dewormingEnabled,
                                breedingEnabled = breedingEnabled,
                                paymentDueEnabled = paymentDueEnabled,
                                paymentDueThreshold = paymentDueThresholdText.toDoubleOrNull() ?: 500.0,
                                lowStockEnabled = lowStockEnabled,
                                soundEnabled = soundEnabled,
                                vibrationEnabled = vibrationEnabled,
                                dailySummaryEnabled = dailySummaryEnabled,
                                dailySummaryTime = dailySummaryTime
                            )
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Settings", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ShiftTimeCard(
    title: String,
    subtitle: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    selectedTime: String,
    timeOptions: List<String>,
    onSelectTime: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = enabled, onCheckedChange = onToggle)
            }

            if (enabled) {
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(timeOptions) { t ->
                        FilterChip(
                            selected = selectedTime == t,
                            onClick = { onSelectTime(t) },
                            label = { Text(t, fontSize = 10.sp, fontWeight = if (selectedTime == t) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

private fun sendWhatsAppDueReminder(context: Context, name: String, mobile: String, balance: Double) {
    try {
        val cleanMobile = mobile.filter { it.isDigit() }
        val message = "Namaste $name ji, gentle reminder from dairy for your outstanding milk bill of ₹${balance.toInt()}. Kindly arrange payment via UPI or cash. Thank you!"
        val url = "https://api.whatsapp.com/send?phone=+91$cleanMobile&text=${Uri.encode(message)}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Payment Reminder for $name: Pending amount is ₹${balance.toInt()}.")
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Payment Reminder"))
    }
}
