package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.BusinessEntity
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.DeliveryEntity
import com.example.data.local.entity.InventoryItemEntity
import com.example.data.local.entity.StaffAttendanceEntity
import com.example.data.local.entity.StaffEntity
import com.example.data.local.entity.StaffPaymentEntity
import com.example.data.repository.UserSession
import com.example.ui.MilkMateViewModel
import com.example.ui.theme.*
import com.example.ui.util.BusinessModeFeatures
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class ManageTab(val index: Int, val title: String, val icon: ImageVector, val tag: String) {
    object MilkRates : ManageTab(0, "Milk Rates & Formula", Icons.Default.Science, "rates")
    object StaffRoster : ManageTab(1, "Staff & Attendance", Icons.Default.Badge, "staff")
    object RouteMatrix : ManageTab(2, "Route Allotment", Icons.Default.AltRoute, "routes")
    object ShiftsDispatch : ManageTab(3, "Shifts & Dispatch", Icons.Default.LocalShipping, "dispatch")
    object ProductsCatalog : ManageTab(4, "Products Catalog", Icons.Default.Inventory2, "catalog")
    object SmartAlerts : ManageTab(5, "Smart Notifications", Icons.Default.NotificationsActive, "alerts")
    object DairySettings : ManageTab(6, "Dairy & Center Settings", Icons.Default.Settings, "settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageHubScreen(
    viewModel: MilkMateViewModel,
    onBack: () -> Unit,
    initialTab: Int = 0,
    onOpenSettings: () -> Unit = {}
) {
    var activeTabIndex by remember { mutableIntStateOf(initialTab) }
    val business by viewModel.currentBusiness.collectAsStateWithLifecycle()
    val session by viewModel.sessionState.collectAsStateWithLifecycle()
    val staffList by viewModel.staffList.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val todayAttendance by viewModel.todayAttendance.collectAsStateWithLifecycle()
    val allAttendance by viewModel.allAttendance.collectAsStateWithLifecycle()
    val staffPayments by viewModel.staffPayments.collectAsStateWithLifecycle()
    val inventoryItems by viewModel.inventoryItems.collectAsStateWithLifecycle()
    val todayDeliveries by viewModel.todayDeliveries.collectAsStateWithLifecycle()

    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = RoyalBluePrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = "Dairy Operations & Management Hub",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${business?.businessName ?: "MilkMate Dairy"} • Master Control",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = DairyGreenLight
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(DairyGreen, CircleShape)
                                    )
                                    Text(
                                        text = "Live Sync",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = DairyGreen
                                    )
                                }
                            }

                            IconButton(
                                onClick = onOpenSettings,
                                modifier = Modifier.size(34.dp).testTag("manage_hub_top_settings_btn")
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Settings,
                                            contentDescription = "Dairy Settings",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Modern Interactive Tab Selector
                    val bMode = business?.businessMode
                    val tabs = remember(bMode) {
                        buildList {
                            add(ManageTab.MilkRates)
                            add(ManageTab.StaffRoster)
                            if (BusinessModeFeatures.showDeliveryRoutes(bMode)) {
                                add(ManageTab.RouteMatrix)
                            }
                            add(ManageTab.ShiftsDispatch)
                            if (BusinessModeFeatures.showInventoryManagement(bMode)) {
                                add(ManageTab.ProductsCatalog)
                            }
                            add(ManageTab.SmartAlerts)
                            add(ManageTab.DairySettings)
                        }
                    }

                    LaunchedEffect(tabs) {
                        if (tabs.none { it.index == activeTabIndex }) {
                            activeTabIndex = tabs.firstOrNull()?.index ?: 0
                        }
                    }

                    val selectedTabIndexInRow = tabs.indexOfFirst { it.index == activeTabIndex }.coerceAtLeast(0)

                    ScrollableTabRow(
                        selectedTabIndex = selectedTabIndexInRow,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = RoyalBluePrimary,
                        edgePadding = 12.dp,
                        divider = {}
                    ) {
                        tabs.forEach { tab ->
                            val isSelected = activeTabIndex == tab.index
                            Tab(
                                selected = isSelected,
                                onClick = { activeTabIndex = tab.index },
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = tab.title,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (isSelected) RoyalBluePrimary else TextSecondary
                                        )
                                        Text(
                                            text = tab.title,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = if (isSelected) RoyalBluePrimary else TextSecondary
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SurfaceBg)
        ) {
            AnimatedContent(
                targetState = activeTabIndex,
                label = "ManageTabAnimation"
            ) { targetTab ->
                when (targetTab) {
                    0 -> RatesAndFormulaTab(
                        viewModel = viewModel,
                        business = business,
                        customers = customers
                    )
                    1 -> StaffAndAttendanceTab(
                        viewModel = viewModel,
                        staffList = staffList,
                        todayAttendance = todayAttendance,
                        allAttendance = allAttendance,
                        staffPayments = staffPayments
                    )
                    2 -> RouteAllotmentTab(
                        viewModel = viewModel,
                        customers = customers,
                        staffList = staffList
                    )
                    3 -> ShiftsAndDispatchTab(
                        viewModel = viewModel,
                        business = business,
                        todayDeliveries = todayDeliveries
                    )
                    4 -> ProductsCatalogTab(
                        viewModel = viewModel,
                        inventoryItems = inventoryItems
                    )
                    5 -> SmartNotificationsTab(
                        viewModel = viewModel,
                        session = session,
                        customers = customers,
                        inventoryItems = inventoryItems,
                        todayDeliveries = todayDeliveries
                    )
                    6 -> DairySettingsHubTab(
                        viewModel = viewModel,
                        business = business,
                        onOpenSettings = onOpenSettings
                    )
                }
            }
        }
    }
}

@Composable
fun DairySettingsHubTab(
    viewModel: MilkMateViewModel,
    business: BusinessEntity?,
    onOpenSettings: () -> Unit
) {
    val session by viewModel.sessionState.collectAsStateWithLifecycle()
    val bMode = business?.businessMode ?: "COLLECTION_CENTER"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth().testTag("manage_hub_settings_card")
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = RoyalBlueLight,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = RoyalBluePrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Dairy Settings & Operations",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "${business?.businessName ?: "My Dairy"} • $bMode",
                                fontSize = 11.5.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Button(
                        onClick = onOpenSettings,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                        modifier = Modifier.testTag("hub_open_settings_btn")
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open Settings", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = BorderSubtle)

                Text(
                    text = "Configure collection center workflows, pricing formulas, Bluetooth slip printers, automated app security locks, and farmer accounting preferences.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )

                // Quick setting shortcut rows
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenSettings() }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(18.dp))
                                Column {
                                    Text("App Lock & Security PIN", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    Text(
                                        text = if (session.appLockEnabled) "Enabled • 4-Digit PIN Active" else "Disabled • Click to setup PIN / Biometrics",
                                        fontSize = 11.sp,
                                        color = if (session.appLockEnabled) GrassGreen else TextSecondary
                                    )
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenSettings() }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Science, contentDescription = null, tint = WarmHoney, modifier = Modifier.size(18.dp))
                                Column {
                                    Text("Milk Pricing Formula & FAT/SNF Rates", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    Text("Mode: ${business?.pricingMode ?: "DIRECT"} • Base: ₹${business?.defaultRatePerLiter ?: 50.0}/L", fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenSettings() }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(18.dp))
                                Column {
                                    Text("Bluetooth Thermal Slip Printer & Scale", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    Text("Automatic receipt printing & digital weighing auto-capture", fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenSettings() }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Translate, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(18.dp))
                                Column {
                                    Text("Language & Region (${session.languageCode.uppercase()})", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    Text("100% UI localized • Instant language switching", fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// MODULE 1: MILK RATES & FORMULA STUDIO
// -------------------------------------------------------------
@Composable
fun RatesAndFormulaTab(
    viewModel: MilkMateViewModel,
    business: BusinessEntity?,
    customers: List<CustomerEntity>
) {
    var pricingMode by remember(business?.pricingMode) {
        mutableStateOf(business?.pricingMode ?: "FAT_SNF")
    }
    var cowRateText by remember(business?.cowMilkRate) {
        mutableStateOf(String.format(Locale.US, "%.1f", business?.cowMilkRate ?: 55.0))
    }
    var buffaloRateText by remember(business?.buffaloMilkRate) {
        mutableStateOf(String.format(Locale.US, "%.1f", business?.buffaloMilkRate ?: 75.0))
    }
    var baseFatText by remember(business?.fatBaseRate) {
        mutableStateOf(String.format(Locale.US, "%.1f", business?.fatBaseRate ?: 3.5))
    }
    var baseSnfText by remember(business?.snfBaseRate) {
        mutableStateOf(String.format(Locale.US, "%.1f", business?.snfBaseRate ?: 8.5))
    }
    var fatRateFactorText by remember(business?.fatDiffRate) {
        mutableStateOf(String.format(Locale.US, "%.1f", business?.fatDiffRate ?: 7.5))
    }
    var snfRateFactorText by remember(business?.snfDiffRate) {
        mutableStateOf(String.format(Locale.US, "%.1f", business?.snfDiffRate ?: 4.0))
    }
    var paneerRateText by remember(business?.paneerRatePerKg) {
        mutableStateOf(String.format(Locale.US, "%.0f", if ((business?.paneerRatePerKg ?: 0.0) > 0) business?.paneerRatePerKg else 360.0))
    }
    var khoaRateText by remember(business?.khoaRatePerKg) {
        mutableStateOf(String.format(Locale.US, "%.0f", if ((business?.khoaRatePerKg ?: 0.0) > 0) business?.khoaRatePerKg else 330.0))
    }

    // Formula Simulator State
    var simFat by remember { mutableFloatStateOf(4.5f) }
    var simSnf by remember { mutableFloatStateOf(8.8f) }
    var simMilkType by remember { mutableStateOf("COW") }

    val baseRate = (if (simMilkType == "COW") cowRateText.toDoubleOrNull() else buffaloRateText.toDoubleOrNull()) ?: 55.0
    val stdFat = baseFatText.toDoubleOrNull() ?: 3.5
    val stdSnf = baseSnfText.toDoubleOrNull() ?: 8.5
    val fatFactor = fatRateFactorText.toDoubleOrNull() ?: 7.5
    val snfFactor = snfRateFactorText.toDoubleOrNull() ?: 4.0
    val paneerRate = paneerRateText.toDoubleOrNull() ?: 360.0
    val khoaRate = khoaRateText.toDoubleOrNull() ?: 330.0

    val simBusiness = remember(business, pricingMode, cowRateText, buffaloRateText, baseFatText, baseSnfText, fatRateFactorText, snfRateFactorText, paneerRateText, khoaRateText) {
        (business ?: BusinessEntity(id = "preview", ownerUid = "preview", businessName = "Dairy", ownerName = "Owner", phone = "", accountStartDate = 0L)).copy(
            pricingMode = pricingMode,
            cowMilkRate = cowRateText.toDoubleOrNull() ?: 55.0,
            buffaloMilkRate = buffaloRateText.toDoubleOrNull() ?: 75.0,
            fatBaseRate = stdFat,
            snfBaseRate = stdSnf,
            fatDiffRate = fatFactor,
            snfDiffRate = snfFactor,
            paneerRatePerKg = paneerRate,
            khoaRatePerKg = khoaRate
        )
    }

    val calculatedPrice = remember(simBusiness, simMilkType, simFat, simSnf) {
        com.example.data.repository.PricingEngine.calculateRatePerLiter(
            business = simBusiness,
            customer = null,
            milkType = simMilkType,
            fat = simFat.toDouble(),
            snf = simSnf.toDouble()
        )
    }

    val livePaneerYield = remember(simFat, simSnf) {
        com.example.data.repository.PricingEngine.calculatePaneerYieldGrams(simFat.toDouble(), simSnf.toDouble())
    }
    val liveKhoaYield = remember(simFat, simSnf) {
        com.example.data.repository.PricingEngine.calculateKhoaYieldGrams(simFat.toDouble(), simSnf.toDouble())
    }
    val liveGheeYield = remember(simFat) {
        com.example.data.repository.PricingEngine.calculateGheeYieldGrams(simFat.toDouble())
    }

    var showCustomFormulaStudio by remember { mutableStateOf(false) }

    if (showCustomFormulaStudio) {
        CustomFormulaStudioDialog(
            business = business,
            viewModel = viewModel,
            onDismiss = { showCustomFormulaStudio = false }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Hero Header Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = RoyalBluePrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Dynamic Pricing & Formula Engine",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Configured for ${business?.businessName ?: "Dairy"}",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = when (pricingMode) {
                                    "FAT_SNF" -> "FAT + SNF Matrix"
                                    "PANEER_YIELD" -> "Paneer Yield / L"
                                    "KHOA_YIELD" -> "Khoya Yield / L"
                                    "GHEE_YIELD" -> "Ghee Yield / L"
                                    "FAT_ONLY" -> "Pure FAT Multiplier"
                                    "CLR_TS" -> "CLR Density"
                                    else -> "Flat Rate"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = FreshGoldLight,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatMetricTile(
                            title = "Cow Base",
                            value = "₹$cowRateText/L",
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricTile(
                            title = "Buff Base",
                            value = "₹$buffaloRateText/L",
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricTile(
                            title = "Active Customers",
                            value = "${customers.size}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Pricing Mode Selector
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Select Pricing & Sales Method",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Choose how milk rates are calculated across collection desks and invoices:",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PricingModeChip(
                                title = "FAT + SNF",
                                subtitle = "Cooperative Standard",
                                isSelected = pricingMode == "FAT_SNF",
                                onClick = { pricingMode = "FAT_SNF" },
                                modifier = Modifier.weight(1f)
                            )
                            PricingModeChip(
                                title = "🧀 Paneer Yield",
                                subtitle = "Grams / Liter",
                                isSelected = pricingMode == "PANEER_YIELD",
                                onClick = { pricingMode = "PANEER_YIELD" },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PricingModeChip(
                                title = "🍬 Khoya Yield",
                                subtitle = "Grams / Liter",
                                isSelected = pricingMode == "KHOA_YIELD" || pricingMode == "KHOYA_YIELD",
                                onClick = { pricingMode = "KHOA_YIELD" },
                                modifier = Modifier.weight(1f)
                            )
                            PricingModeChip(
                                title = "🏺 Ghee Yield",
                                subtitle = "Fat Recovery",
                                isSelected = pricingMode == "GHEE_YIELD",
                                onClick = { pricingMode = "GHEE_YIELD" },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PricingModeChip(
                                title = "FAT Only",
                                subtitle = "Linear Multiplier",
                                isSelected = pricingMode == "FAT_ONLY",
                                onClick = { pricingMode = "FAT_ONLY" },
                                modifier = Modifier.weight(1f)
                            )
                            PricingModeChip(
                                title = "Fixed Rate",
                                subtitle = "Per-Liter Flat",
                                isSelected = pricingMode == "DIRECT" || pricingMode == "FIXED",
                                onClick = { pricingMode = "DIRECT" },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PricingModeChip(
                                title = "⚙️ Custom Formula",
                                subtitle = "Build Your Own Equation",
                                isSelected = pricingMode == "CUSTOM",
                                onClick = {
                                    pricingMode = "CUSTOM"
                                    showCustomFormulaStudio = true
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        FilledTonalButton(
                            onClick = { showCustomFormulaStudio = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = OceanMidnight, contentColor = FreshGold),
                            modifier = Modifier.fillMaxWidth().height(42.dp)
                        ) {
                            Icon(Icons.Default.Functions, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("📐 Open Custom Formula Studio (Equation Builder) →", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Standard Rates & Yield Configuration
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Base Milk Rates & Market Yield Values",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = cowRateText,
                            onValueChange = { cowRateText = it },
                            label = { Text("Cow Base (₹/L)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = buffaloRateText,
                            onValueChange = { buffaloRateText = it },
                            label = { Text("Buffalo Base (₹/L)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    if (pricingMode == "PANEER_YIELD" || pricingMode == "KHOA_YIELD" || pricingMode == "KHOYA_YIELD") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = paneerRateText,
                                onValueChange = { paneerRateText = it },
                                label = { Text("Paneer Rate (₹/kg)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = khoaRateText,
                                onValueChange = { khoaRateText = it },
                                label = { Text("Khoya Rate (₹/kg)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    if (pricingMode == "FAT_SNF" || pricingMode == "FAT_ONLY") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = baseFatText,
                                onValueChange = { baseFatText = it },
                                label = { Text("Std FAT (%)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = fatRateFactorText,
                                onValueChange = { fatRateFactorText = it },
                                label = { Text("FAT Mult (₹/%)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        if (pricingMode == "FAT_SNF") {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = baseSnfText,
                                    onValueChange = { baseSnfText = it },
                                    label = { Text("Std SNF (%)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                OutlinedTextField(
                                    value = snfRateFactorText,
                                    onValueChange = { snfRateFactorText = it },
                                    label = { Text("SNF Mult (₹/%)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Quick Presets:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PresetChip(label = "Amul Pattern (3.5/8.5)") {
                            cowRateText = "54.0"
                            buffaloRateText = "72.0"
                            baseFatText = "3.5"
                            baseSnfText = "8.5"
                            fatRateFactorText = "7.8"
                            snfRateFactorText = "4.2"
                            pricingMode = "FAT_SNF"
                        }
                        PresetChip(label = "🧀 Paneer Yield Base") {
                            paneerRateText = "380"
                            pricingMode = "PANEER_YIELD"
                        }
                        PresetChip(label = "🍬 Khoya Yield Base") {
                            khoaRateText = "340"
                            pricingMode = "KHOA_YIELD"
                        }
                        PresetChip(label = "Premium Buffalo A2") {
                            cowRateText = "60.0"
                            buffaloRateText = "82.0"
                            baseFatText = "6.5"
                            baseSnfText = "9.0"
                            fatRateFactorText = "8.5"
                            snfRateFactorText = "4.5"
                            pricingMode = "FAT_SNF"
                        }
                        PresetChip(label = "Local Flat Rate") {
                            cowRateText = "55.0"
                            buffaloRateText = "75.0"
                            pricingMode = "DIRECT"
                        }
                    }
                }
            }
        }

        // Interactive Live Price Simulator with Product Yields
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = OceanBlueLight.copy(alpha = 0.5f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, OceanBlueAccent.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Calculate, contentDescription = null, tint = OceanBlueAccent)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Live Yield Simulator & Rate Lab",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = RoyalBluePrimary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White
                        ) {
                            Row(modifier = Modifier.padding(2.dp)) {
                                FilterChip(
                                    selected = simMilkType == "COW",
                                    onClick = { simMilkType = "COW" },
                                    label = { Text("Cow", fontSize = 11.sp) },
                                    modifier = Modifier.height(28.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                FilterChip(
                                    selected = simMilkType == "BUFFALO",
                                    onClick = { simMilkType = "BUFFALO" },
                                    label = { Text("Buffalo", fontSize = 11.sp) },
                                    modifier = Modifier.height(28.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Test FAT: ${String.format(Locale.US, "%.1f", simFat)}%", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Standard: $baseFatText%", fontSize = 11.sp, color = TextSecondary)
                        }
                        Slider(
                            value = simFat,
                            onValueChange = { simFat = it },
                            valueRange = 2.5f..10.0f,
                            steps = 75,
                            colors = SliderDefaults.colors(
                                thumbColor = OceanBlueAccent,
                                activeTrackColor = OceanBlueAccent
                            )
                        )
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Test SNF: ${String.format(Locale.US, "%.1f", simSnf)}%", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Standard: $baseSnfText%", fontSize = 11.sp, color = TextSecondary)
                        }
                        Slider(
                            value = simSnf,
                            onValueChange = { simSnf = it },
                            valueRange = 6.5f..11.0f,
                            steps = 45,
                            colors = SliderDefaults.colors(
                                thumbColor = DairyGreen,
                                activeTrackColor = DairyGreen
                            )
                        )
                    }

                    // Yield Output Diagnostics Row
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🧀 Paneer Yield", fontSize = 10.sp, color = TextSecondary)
                                Text("${String.format(Locale.US, "%.1f", livePaneerYield)} g/L", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🍬 Khoya Yield", fontSize = 10.sp, color = TextSecondary)
                                Text("${String.format(Locale.US, "%.1f", liveKhoaYield)} g/L", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🏺 Ghee Yield", fontSize = 10.sp, color = TextSecondary)
                                Text("${String.format(Locale.US, "%.1f", liveGheeYield)} g/L", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Calculated Price per Liter:", fontSize = 12.sp, color = TextSecondary)
                                Text(
                                    text = "₹${String.format(Locale.US, "%.2f", calculatedPrice)} / Liter",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DairyGreen
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("Sample 10 Liters:", fontSize = 11.sp, color = TextSecondary)
                                Text(
                                    text = "₹${String.format(Locale.US, "%.2f", calculatedPrice * 10)}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RoyalBluePrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Master Save & Apply Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val cow = cowRateText.toDoubleOrNull() ?: 55.0
                        val buff = buffaloRateText.toDoubleOrNull() ?: 75.0
                        val fat = baseFatText.toDoubleOrNull() ?: 3.5
                        val snf = baseSnfText.toDoubleOrNull() ?: 8.5
                        val fatMult = fatRateFactorText.toDoubleOrNull() ?: 7.5
                        val snfMult = snfRateFactorText.toDoubleOrNull() ?: 4.0
                        val pRate = paneerRateText.toDoubleOrNull() ?: 360.0
                        val kRate = khoaRateText.toDoubleOrNull() ?: 330.0

                        viewModel.saveCustomRateFormula(
                            cowRate = cow,
                            buffaloRate = buff,
                            baseFat = fat,
                            baseSnf = snf,
                            fatFactor = fatMult,
                            snfFactor = snfMult,
                            pricingMode = pricingMode,
                            paneerRatePerKg = pRate,
                            khoaRatePerKg = kRate
                        )
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Rates & Formula", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        val cow = cowRateText.toDoubleOrNull() ?: 55.0
                        val buff = buffaloRateText.toDoubleOrNull() ?: 75.0
                        customers.forEach { cust ->
                            viewModel.saveCustomer(
                                id = cust.id,
                                name = cust.name,
                                mobile = cust.mobile,
                                address = cust.address,
                                type = cust.type,
                                rate = cust.rate,
                                milkType = cust.milkType,
                                defaultQty = cust.defaultQuantity,
                                defaultShift = cust.defaultShift,
                                route = cust.route,
                                notes = cust.notes,
                                cowQuantity = cust.cowQuantity,
                                cowRate = cow,
                                buffaloQuantity = cust.buffaloQuantity,
                                buffaloRate = buff
                            )
                        }
                        viewModel.showMessage("✓ Batch updated base rates for all ${customers.size} customers!")
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DairyGreen)
                ) {
                    Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp), tint = DairyGreen)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Apply to All Customers", fontWeight = FontWeight.Bold, color = DairyGreen, fontSize = 12.sp)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// MODULE 2: STAFF ROSTER, ATTENDANCE & PAYROLL
// -------------------------------------------------------------
@Composable
fun StaffAndAttendanceTab(
    viewModel: MilkMateViewModel,
    staffList: List<StaffEntity>,
    todayAttendance: List<StaffAttendanceEntity>,
    allAttendance: List<StaffAttendanceEntity>,
    staffPayments: List<StaffPaymentEntity>
) {
    var showAddStaffDialog by remember { mutableStateOf(false) }
    var showAdvanceDialog by remember { mutableStateOf<StaffEntity?>(null) }
    val context = LocalContext.current

    val presentCount = todayAttendance.count { it.status == "PRESENT" }
    val totalStaff = staffList.size
    val totalPayrollDisbursed = staffPayments.sumOf { it.amount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Staff Hero Metrics
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Staff Roster & Wage Engine",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Today: $presentCount / $totalStaff Present on Duty",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }

                        Button(
                            onClick = { showAddStaffDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldenOrange),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Staff", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatMetricTile(
                            title = "Active Staff",
                            value = "$totalStaff Members",
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricTile(
                            title = "Present Today",
                            value = "$presentCount On-Field",
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricTile(
                            title = "Payroll Paid",
                            value = "₹${totalPayrollDisbursed.toInt()}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Attendance Action Bar
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Daily Attendance Register",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Quick 1-touch attendance logger",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            viewModel.markAllStaffAttendance("PRESENT")
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = DairyGreenLight),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mark All Present", fontWeight = FontWeight.Bold, color = DairyGreen, fontSize = 12.sp)
                    }
                }
            }
        }

        // Staff List Cards
        if (staffList.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Group,
                    title = "No Staff Members Added",
                    description = "Add drivers, delivery boys, and dairy workers to track daily attendance and wages."
                )
            }
        } else {
            items(staffList) { staff ->
                val currentAttendance = todayAttendance.find { it.staffId == staff.id }?.status ?: "ABSENT"
                val staffAttRecords = allAttendance.filter { it.staffId == staff.id }
                val staffPaidRecords = staffPayments.filter { it.staffId == staff.id }
                val totalAdvances = staffPaidRecords.sumOf { it.amount }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(RoyalBlueLight, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = staff.name.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = RoyalBluePrimary
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = staff.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${staff.role.replace("_", " ")} • ${staff.assignedRoute.ifBlank { "Unassigned Route" }}",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = FreshGoldLight
                            ) {
                                Text(
                                    text = if (staff.salaryType == "MONTHLY") "₹${staff.monthlySalary.toInt()}/mo" else "₹${staff.dailyWage.toInt()}/day",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WarmHoney,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick Attendance Status Selector Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Today's Status:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                StatusPillButton(
                                    label = "P",
                                    title = "Present",
                                    isSelected = currentAttendance == "PRESENT",
                                    activeColor = DairyGreen
                                ) {
                                    viewModel.markStaffAttendance(staff.id, "PRESENT")
                                }
                                StatusPillButton(
                                    label = "H",
                                    title = "Half Day",
                                    isSelected = currentAttendance == "HALF_DAY",
                                    activeColor = GoldenOrange
                                ) {
                                    viewModel.markStaffAttendance(staff.id, "HALF_DAY")
                                }
                                StatusPillButton(
                                    label = "A",
                                    title = "Absent",
                                    isSelected = currentAttendance == "ABSENT",
                                    activeColor = DangerRed
                                ) {
                                    viewModel.markStaffAttendance(staff.id, "ABSENT")
                                }
                                StatusPillButton(
                                    label = "L",
                                    title = "Leave",
                                    isSelected = currentAttendance == "LEAVE",
                                    activeColor = OceanBlueAccent
                                ) {
                                    viewModel.markStaffAttendance(staff.id, "LEAVE")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = BorderSubtle)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Actions Row: Disburse Advance, WhatsApp Salary Slip, Call
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showAdvanceDialog = staff },
                                modifier = Modifier.weight(1f).height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pay Advance", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
                                onClick = {
                                    val daysPresent = staffAttRecords.count { it.status == "PRESENT" }
                                    val halfDays = staffAttRecords.count { it.status == "HALF_DAY" }
                                    val totalEarned = if (staff.salaryType == "MONTHLY") {
                                        (staff.monthlySalary / 30.0) * (daysPresent + halfDays * 0.5)
                                    } else {
                                        staff.dailyWage * (daysPresent + halfDays * 0.5)
                                    }
                                    val netPayable = (totalEarned - totalAdvances).coerceAtLeast(0.0)

                                    val slipText = """
                                        📋 *SALARY & ATTENDANCE SLIP*
                                        👤 *Staff:* ${staff.name} (${staff.role})
                                        📅 *Month:* ${SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())}
                                        ---------------------------
                                        ✅ *Days Present:* $daysPresent days
                                        ⏳ *Half Days:* $halfDays days
                                        💵 *Gross Earned:* ₹${String.format(Locale.US, "%.0f", totalEarned)}
                                        📉 *Advance Deducted:* ₹${String.format(Locale.US, "%.0f", totalAdvances)}
                                        ---------------------------
                                        💰 *Net Balance Payable:* ₹${String.format(Locale.US, "%.0f", netPayable)}
                                        
                                        _MilkMate Dairy Management System_
                                    """.trimIndent()

                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse("https://api.whatsapp.com/send?phone=91${staff.mobile.takeLast(10)}&text=${Uri.encode(slipText)}")
                                    }
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.weight(1f).height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = DairyGreenLight),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("WhatsApp Slip", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DairyGreen)
                            }

                            if (staff.mobile.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${staff.mobile}"))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = "Call", tint = RoyalBluePrimary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Staff Dialog
    if (showAddStaffDialog) {
        AddStaffDialog(
            onDismiss = { showAddStaffDialog = false },
            onSave = { name, mobile, role, route, salaryType, monthly, daily ->
                viewModel.saveStaff(
                    id = null,
                    name = name,
                    mobile = mobile,
                    pin = "1234",
                    role = role,
                    permissions = "DELIVERY,CUSTOMERS",
                    assignedRoute = route,
                    monthlySalary = monthly,
                    dailyWage = daily,
                    salaryType = salaryType
                )
                showAddStaffDialog = false
                viewModel.showMessage("✓ Added new staff: $name")
            }
        )
    }

    // Advance Disbursement Dialog
    if (showAdvanceDialog != null) {
        val targetStaff = showAdvanceDialog!!
        var advanceAmountText by remember { mutableStateOf("1000") }
        var advanceNote by remember { mutableStateOf("Advance cash against wage") }

        AlertDialog(
            onDismissRequest = { showAdvanceDialog = null },
            title = { Text("Disburse Advance to ${targetStaff.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Record advance cash disbursement. This will automatically deduct from their next salary slip.")
                    OutlinedTextField(
                        value = advanceAmountText,
                        onValueChange = { advanceAmountText = it },
                        label = { Text("Advance Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = advanceNote,
                        onValueChange = { advanceNote = it },
                        label = { Text("Note / Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = advanceAmountText.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            viewModel.recordStaffPayment(
                                staffId = targetStaff.id,
                                staffName = targetStaff.name,
                                type = "ADVANCE",
                                amount = amount,
                                notes = advanceNote
                            )
                        }
                        showAdvanceDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldenOrange)
                ) {
                    Text("Confirm Advance")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdvanceDialog = null }) { Text("Cancel") }
            }
        )
    }
}

// -------------------------------------------------------------
// MODULE 3: SMART ROUTE & DELIVERY ALLOCATION MATRIX
// -------------------------------------------------------------
@Composable
fun RouteAllotmentTab(
    viewModel: MilkMateViewModel,
    customers: List<CustomerEntity>,
    staffList: List<StaffEntity>
) {
    var selectedRouteFilter by remember { mutableStateOf("ALL") }
    var selectedCustomerIds by remember { mutableStateOf(setOf<String>()) }
    var targetNewRoute by remember { mutableStateOf("Route 1") }
    var targetStaffId by remember { mutableStateOf("") }

    val distinctRoutes = remember(customers) {
        val routes = customers.map { it.route.ifBlank { "Unassigned" } }.distinct().sorted()
        listOf("ALL") + routes
    }

    val filteredCustomers = remember(customers, selectedRouteFilter) {
        if (selectedRouteFilter == "ALL") customers
        else customers.filter { (it.route.ifBlank { "Unassigned" }) == selectedRouteFilter }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Route Matrix Hero
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = RoyalBluePrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Smart Route Allocation & Dispatch Matrix",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Assign delivery lines, delivery partners, and re-allocate customers in bulk",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatMetricTile(
                            title = "Active Routes",
                            value = "${(distinctRoutes.size - 1).coerceAtLeast(0)} Lines",
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricTile(
                            title = "Mapped Stops",
                            value = "${customers.count { it.route.isNotBlank() }} Stops",
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricTile(
                            title = "Unassigned",
                            value = "${customers.count { it.route.isBlank() }} Users",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Bulk Reallocation Action Drawer
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Bulk Customer Route Re-allotment",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Selected: ${selectedCustomerIds.size} customers",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = targetNewRoute,
                            onValueChange = { targetNewRoute = it },
                            label = { Text("Assign to Route Name") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Button(
                            onClick = {
                                if (selectedCustomerIds.isNotEmpty()) {
                                    val targetStaff = staffList.find { it.id == targetStaffId }
                                    viewModel.allotRouteAndStaffToCustomers(
                                        customerIds = selectedCustomerIds.toList(),
                                        route = targetNewRoute,
                                        staffId = targetStaff?.id ?: "",
                                        staffName = targetStaff?.name ?: ""
                                    )
                                    selectedCustomerIds = emptySet()
                                }
                            },
                            enabled = selectedCustomerIds.isNotEmpty(),
                            modifier = Modifier.align(Alignment.CenterVertically).height(54.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DairyGreen)
                        ) {
                            Text("Allot (${selectedCustomerIds.size})", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = {
                                selectedCustomerIds = filteredCustomers.map { it.id }.toSet()
                            }
                        ) {
                            Text("Select All in View (${filteredCustomers.size})", fontSize = 12.sp)
                        }

                        if (selectedCustomerIds.isNotEmpty()) {
                            TextButton(onClick = { selectedCustomerIds = emptySet() }) {
                                Text("Clear Selection", fontSize = 12.sp, color = DangerRed)
                            }
                        }
                    }
                }
            }
        }

        // Route Filter Tabs
        item {
            ScrollableTabRow(
                selectedTabIndex = distinctRoutes.indexOf(selectedRouteFilter).coerceAtLeast(0),
                containerColor = SurfaceCard,
                edgePadding = 8.dp,
                divider = {}
            ) {
                distinctRoutes.forEach { routeName ->
                    Tab(
                        selected = selectedRouteFilter == routeName,
                        onClick = { selectedRouteFilter = routeName },
                        text = {
                            Text(
                                text = routeName,
                                fontWeight = if (selectedRouteFilter == routeName) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        }
                    )
                }
            }
        }

        // Customer Selection List
        items(filteredCustomers) { cust ->
            val isChecked = selectedCustomerIds.contains(cust.id)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isChecked) RoyalBlueLight else SurfaceCard
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isChecked) RoyalBluePrimary else BorderSubtle
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedCustomerIds = if (isChecked) {
                            selectedCustomerIds - cust.id
                        } else {
                            selectedCustomerIds + cust.id
                        }
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                selectedCustomerIds = if (checked) {
                                    selectedCustomerIds + cust.id
                                } else {
                                    selectedCustomerIds - cust.id
                                }
                            }
                        )
                        Column {
                            Text(
                                text = cust.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Route: ${cust.route.ifBlank { "Unassigned" }} • ${cust.defaultShift}",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${cust.defaultQuantity}L ${cust.milkType}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = RoyalBluePrimary
                        )
                        Text(
                            text = "₹${cust.cowRate.toInt()}/L",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// MODULE 4: SHIFTS & DISPATCH OPERATIONS
// -------------------------------------------------------------
@Composable
fun ShiftsAndDispatchTab(
    viewModel: MilkMateViewModel,
    business: BusinessEntity?,
    todayDeliveries: List<DeliveryEntity>
) {
    val selectedShift by viewModel.selectedShift.collectAsStateWithLifecycle()
    val morningDeliveries = todayDeliveries.filter { it.shift == "MORNING" }
    val eveningDeliveries = todayDeliveries.filter { it.shift == "EVENING" }

    val morningLiters = morningDeliveries.sumOf { it.quantityLiters }
    val eveningLiters = eveningDeliveries.sumOf { it.quantityLiters }
    val morningCompleted = morningDeliveries.count { it.isDelivered }
    val eveningCompleted = eveningDeliveries.count { it.isDelivered }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Shift Switcher Hero Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedShift == "MORNING") FreshGold else RoyalBluePrimary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (selectedShift == "MORNING") "🌅 Morning Shift Active" else "🌆 Evening Shift Active",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Dispatch & Counter Real-Time Monitoring",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }

                        Button(
                            onClick = {
                                val next = if (selectedShift == "MORNING") "EVENING" else "MORNING"
                                viewModel.setShift(next)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (selectedShift == "MORNING") "Switch to Evening" else "Switch to Morning",
                                color = if (selectedShift == "MORNING") FreshGold else RoyalBluePrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatMetricTile(
                            title = "Morning Volume",
                            value = "${String.format(Locale.US, "%.1f", morningLiters)} Liters",
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricTile(
                            title = "Evening Volume",
                            value = "${String.format(Locale.US, "%.1f", eveningLiters)} Liters",
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricTile(
                            title = "Total Stops",
                            value = "${todayDeliveries.size} Drops",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Shift Breakdown Comparison Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Shift Dispatch Progress & Crate Load",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    ShiftProgressBar(
                        title = "🌅 Morning Dispatch (05:00 - 09:00)",
                        completed = morningCompleted,
                        total = morningDeliveries.size,
                        liters = morningLiters,
                        color = FreshGold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ShiftProgressBar(
                        title = "🌆 Evening Dispatch (16:30 - 20:30)",
                        completed = eveningCompleted,
                        total = eveningDeliveries.size,
                        liters = eveningLiters,
                        color = RoyalBluePrimary
                    )
                }
            }
        }

        // Vehicle & Tanker Dispatch Checklist
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Pre-Dispatch Van & Crate Checklist",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    ChecklistItem(title = "Aluminum / Stainless Milk Cans Sealed & Sanitized", initial = true)
                    ChecklistItem(title = "Measuring Lactometers & FAT Sampling Kits Loaded", initial = true)
                    ChecklistItem(title = "Ice Gel Packs & Insulated Blankets Placed", initial = true)
                    ChecklistItem(title = "QR Payment Cards & Khata Bill Receipt Books Handed", initial = false)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// MODULE 5: PRODUCTS & VALUE-ADDED INVENTORY CATALOG
// -------------------------------------------------------------
@Composable
fun ProductsCatalogTab(
    viewModel: MilkMateViewModel,
    inventoryItems: List<InventoryItemEntity>
) {
    val business by viewModel.currentBusiness.collectAsStateWithLifecycle()
    val displayItems = remember(inventoryItems, business?.businessMode) {
        BusinessModeFeatures.filterInventoryForMode(inventoryItems, business?.businessMode)
    }
    var showAddProductDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Products Hero Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = ForestGreenAccent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (BusinessModeFeatures.showCattleFeed(business?.businessMode)) "Dairy Products & Cattle Feed Catalog" else "Dairy Products & Inventory Catalog",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (BusinessModeFeatures.showCattleFeed(business?.businessMode)) "Paneer, Ghee, Curd, Butter, Khoya & Feed Stock" else "Paneer, Ghee, Curd, Butter, Khoya & Fresh SKUs",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }

                        Button(
                            onClick = { showAddProductDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = ForestGreenAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Product", color = ForestGreenAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatMetricTile(
                            title = "Catalog Items",
                            value = "${displayItems.size} Products",
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricTile(
                            title = "Low Stock Alert",
                            value = "${displayItems.count { it.stockAlertStatus != "NORMAL" || it.currentStock <= 5.0 }} Critical",
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricTile(
                            title = "Stock Value",
                            value = "₹${displayItems.sumOf { (it.currentStock * it.costPerUnit).toInt() }}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        if (displayItems.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Inventory2,
                    title = "Product Catalog Empty",
                    description = if (BusinessModeFeatures.showCattleFeed(business?.businessMode))
                        "Add dairy products like Desi Ghee, Paneer, Curd, or Cattle Feeds to manage pricing and live stock."
                    else
                        "Add dairy products like Desi Ghee, Paneer, Curd, Butter, and Milk Pouches to manage pricing and live stock."
                )
            }
        } else {
            items(displayItems) { item ->
                val isLowStock = item.stockAlertStatus != "NORMAL" || item.currentStock <= item.alertDaysThreshold.toDouble()

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = item.itemName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                )
                                if (isLowStock) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = DangerRedLight
                                    ) {
                                        Text(
                                            text = "LOW STOCK",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DangerRed,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Cost: ₹${item.costPerUnit.toInt()} / ${item.unit}",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        // Inline Stock Adjuster
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SurfaceBg,
                                modifier = Modifier.size(32.dp).clickable {
                                    if (item.currentStock > 0) {
                                        viewModel.updateInventoryStock(
                                            itemId = item.id,
                                            quantityChange = -1.0,
                                            transactionType = "SALE",
                                            notes = "Catalog quick decrement"
                                        )
                                    }
                                }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DangerRed)
                                }
                            }

                            Text(
                                text = "${String.format(Locale.US, "%.1f", item.currentStock)} ${item.unit}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isLowStock) DangerRed else TextPrimary
                            )

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DairyGreenLight,
                                modifier = Modifier.size(32.dp).clickable {
                                    viewModel.updateInventoryStock(
                                        itemId = item.id,
                                        quantityChange = 1.0,
                                        transactionType = "PURCHASE",
                                        notes = "Catalog quick increment"
                                    )
                                }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DairyGreen)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddProductDialog) {
        val showFeed = BusinessModeFeatures.showCattleFeed(business?.businessMode)
        AddProductDialog(
            showFeed = showFeed,
            onDismiss = { showAddProductDialog = false },
            onSave = { name, unit, stock, cost, dailyUsage ->
                viewModel.saveInventoryItem(
                    id = null,
                    itemName = name,
                    unit = unit,
                    openingStock = stock,
                    costPerUnit = cost,
                    dailyUsage = dailyUsage
                )
                showAddProductDialog = false
                viewModel.showMessage("✓ Added $name to product catalog!")
            }
        )
    }
}

// -------------------------------------------------------------
// MODULE 6: SMART ALERTS & AUTOMATED REMINDERS
// -------------------------------------------------------------
@Composable
fun SmartNotificationsTab(
    viewModel: MilkMateViewModel,
    session: UserSession,
    customers: List<CustomerEntity>,
    inventoryItems: List<InventoryItemEntity>,
    todayDeliveries: List<DeliveryEntity>
) {
    val context = LocalContext.current
    var morningAlarm by remember(session.morningReminderEnabled) { mutableStateOf(session.morningReminderEnabled) }
    var eveningAlarm by remember(session.eveningReminderEnabled) { mutableStateOf(session.eveningReminderEnabled) }
    var overdueThresholdText by remember(session.paymentDueThreshold) { mutableStateOf(session.paymentDueThreshold.toInt().toString()) }

    val currentThreshold = overdueThresholdText.toDoubleOrNull() ?: 500.0
    val overdueCustomers = remember(customers, currentThreshold) {
        customers.filter { it.outstandingBalance > currentThreshold }.sortedByDescending { it.outstandingBalance }
    }

    val lowStockItems = remember(inventoryItems) {
        inventoryItems.filter { it.stockAlertStatus != "NORMAL" || it.currentStock <= 5.0 }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Notification Hero Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = OceanBlueAccent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Smart Alerts & WhatsApp Blaster",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Automated shift alarms, overdue payment triggers & feed alerts",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }

                        IconButton(
                            onClick = {
                                viewModel.showMessage("🔔 Test Alert Triggered: Morning Shift Dispatch is READY!")
                            }
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = "Test", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatMetricTile(
                            title = "Overdue Customers",
                            value = "${overdueCustomers.size} Pending",
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricTile(
                            title = "Low Stock Feeds",
                            value = "${lowStockItems.size} Items",
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricTile(
                            title = "Morning Alarm",
                            value = if (morningAlarm) "05:30 AM" else "Off",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Shift Alarm Preferences
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Automated Shift & Delivery Reminders",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    NotificationSwitchRow(
                        title = "🌅 Morning Dispatch Alarm (05:30 AM)",
                        subtitle = "Alerts delivery drivers and milker team 30 mins before route dispatch",
                        checked = morningAlarm,
                        onCheckedChange = {
                            morningAlarm = it
                            viewModel.updateNotificationPreferences(morningEnabled = it)
                        }
                    )

                    Divider(color = BorderSubtle, modifier = Modifier.padding(vertical = 8.dp))

                    NotificationSwitchRow(
                        title = "🌆 Evening Dispatch Alarm (05:00 PM)",
                        subtitle = "Alerts delivery drivers and counter collection staff",
                        checked = eveningAlarm,
                        onCheckedChange = {
                            eveningAlarm = it
                            viewModel.updateNotificationPreferences(eveningEnabled = it)
                        }
                    )
                }
            }
        }

        // Overdue Khata Payment Blaster
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Overdue Payment Reminders",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Threshold: Outstanding > ₹$overdueThresholdText",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        OutlinedTextField(
                            value = overdueThresholdText,
                            onValueChange = { overdueThresholdText = it },
                            label = { Text("Limit ₹") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(100.dp),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (overdueCustomers.isEmpty()) {
                        Text(
                            text = "🎉 Awesome! No customers exceed the ₹$overdueThresholdText outstanding balance limit.",
                            fontSize = 13.sp,
                            color = DairyGreen,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            overdueCustomers.take(5).forEach { cust ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = SurfaceBg,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(cust.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("Due: ₹${cust.outstandingBalance.toInt()}", fontSize = 12.sp, color = DangerRed, fontWeight = FontWeight.Bold)
                                        }

                                        FilledTonalButton(
                                            onClick = {
                                                val msg = "Namaste ${cust.name}, this is a gentle reminder from MilkMate Dairy. Your milk bill outstanding balance is ₹${cust.outstandingBalance.toInt()}. Please settle via UPI or cash. Thank you!"
                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    data = Uri.parse("https://api.whatsapp.com/send?phone=91${cust.mobile.takeLast(10)}&text=${Uri.encode(msg)}")
                                                }
                                                context.startActivity(intent)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = DairyGreenLight),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Icon(Icons.Default.Send, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Remind", fontSize = 11.sp, color = DairyGreen, fontWeight = FontWeight.Bold)
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
}

// -------------------------------------------------------------
// REUSABLE HELPER UI COMPONENTS
// -------------------------------------------------------------
@Composable
fun StatMetricTile(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = 0.15f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
            Text(text = title, fontSize = 10.sp, color = Color.White.copy(alpha = 0.85f))
        }
    }
}

@Composable
fun PricingModeChip(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) RoyalBlueLight else SurfaceBg,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) RoyalBluePrimary else BorderSubtle
        ),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isSelected) RoyalBluePrimary else TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = if (isSelected) RoyalBluePrimary.copy(alpha = 0.8f) else TextSecondary
            )
        }
    }
}

@Composable
fun PresetChip(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = RoyalBlueLight,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = RoyalBluePrimary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun StatusPillButton(
    label: String,
    title: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) activeColor else SurfaceBg,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) activeColor else BorderSubtle
        ),
        modifier = Modifier
            .size(36.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isSelected) Color.White else TextSecondary
            )
        }
    }
}

@Composable
fun ShiftProgressBar(
    title: String,
    completed: Int,
    total: Int,
    liters: Double,
    color: Color
) {
    val progress = if (total > 0) (completed.toFloat() / total).coerceIn(0f, 1f) else 0f

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("${(progress * 100).toInt()}% Done ($completed/$total)", fontSize = 12.sp, color = color, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Total volume scheduled: ${String.format(Locale.US, "%.1f", liters)} Liters",
            fontSize = 11.sp,
            color = TextSecondary
        )
    }
}

@Composable
fun ChecklistItem(title: String, initial: Boolean) {
    var checked by remember { mutableStateOf(initial) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { checked = !checked }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = { checked = it })
        Text(
            text = title,
            fontSize = 13.sp,
            color = if (checked) TextSecondary else TextPrimary,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@Composable
fun NotificationSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
            Text(subtitle, fontSize = 11.sp, color = TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = DairyGreen)
        )
    }
}

@Composable
fun EmptyStateCard(icon: ImageVector, title: String, description: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(42.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(description, fontSize = 12.sp, color = TextSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun AddStaffDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, Double, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("DELIVERY_BOY") }
    var route by remember { mutableStateOf("Route 1") }
    var salaryType by remember { mutableStateOf("MONTHLY") }
    var monthlySalaryText by remember { mutableStateOf("12000") }
    var dailyWageText by remember { mutableStateOf("400") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Dairy Staff Member", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("Mobile Number (for WhatsApp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = route,
                    onValueChange = { route = it },
                    label = { Text("Assigned Delivery Route / Area") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Wage Model:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = salaryType == "MONTHLY",
                        onClick = { salaryType = "MONTHLY" },
                        label = { Text("Monthly Salary") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = salaryType == "DAILY",
                        onClick = { salaryType = "DAILY" },
                        label = { Text("Daily Wage") },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (salaryType == "MONTHLY") {
                    OutlinedTextField(
                        value = monthlySalaryText,
                        onValueChange = { monthlySalaryText = it },
                        label = { Text("Monthly Fixed Salary (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = dailyWageText,
                        onValueChange = { dailyWageText = it },
                        label = { Text("Per Day Wage (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val monthly = monthlySalaryText.toDoubleOrNull() ?: 12000.0
                        val daily = dailyWageText.toDoubleOrNull() ?: 400.0
                        onSave(name, mobile, role, route, salaryType, monthly, daily)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary)
            ) {
                Text("Save Staff")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddProductDialog(
    showFeed: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (String, String, Double, Double, Double?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("kg") }
    var stockText by remember { mutableStateOf("10") }
    var costText by remember { mutableStateOf("350") }
    var dailyUsageText by remember { mutableStateOf("2") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (showFeed) "Add Dairy Product / Feed" else "Add Dairy Product / SKU", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name (e.g. Desi Cow Ghee) *") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit (kg/litre/pkt)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = stockText,
                        onValueChange = { stockText = it },
                        label = { Text("Initial Stock") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = costText,
                        onValueChange = { costText = it },
                        label = { Text("Cost Per Unit (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = dailyUsageText,
                        onValueChange = { dailyUsageText = it },
                        label = { Text("Daily Usage") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val stock = stockText.toDoubleOrNull() ?: 0.0
                        val cost = costText.toDoubleOrNull() ?: 0.0
                        val usage = dailyUsageText.toDoubleOrNull()
                        onSave(name, unit, stock, cost, usage)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreenAccent)
            ) {
                Text("Add to Catalog")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
