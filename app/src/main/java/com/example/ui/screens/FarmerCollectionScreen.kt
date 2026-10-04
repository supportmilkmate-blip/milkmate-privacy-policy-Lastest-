package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.data.local.entity.BulkDispatchEntity
import com.example.data.local.entity.FarmerEntity
import com.example.data.local.entity.MilkCollectionEntity
import com.example.data.repository.PricingEngine
import com.example.ui.MilkMateViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmerCollectionScreen(
    viewModel: MilkMateViewModel,
    initialTab: Int = 0,
    showInternalTabs: Boolean = false,
    onOpenSettings: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) } // 0: Collection Desk, 1: Farmers & Passbooks, 2: Bulk Dispatches
    var showAddFarmerDialog by remember { mutableStateOf(false) }
    var showAddDispatchDialog by remember { mutableStateOf(false) }
    var selectedFarmerForPassbook by remember { mutableStateOf<FarmerEntity?>(null) }
    var showRecordFarmerPaymentDialog by remember { mutableStateOf<FarmerEntity?>(null) }
    var showCustomFormulaStudio by remember { mutableStateOf(false) }

    val farmers by viewModel.farmers.collectAsStateWithLifecycle()
    val todayCollections by viewModel.todayMilkCollections.collectAsStateWithLifecycle()
    val allCollections by viewModel.allMilkCollections.collectAsStateWithLifecycle()
    val bulkDispatches by viewModel.bulkDispatches.collectAsStateWithLifecycle()
    val selectedShift by viewModel.selectedShift.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val business by viewModel.currentBusiness.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val formattedDate = sdf.format(Date(selectedDate))

    val isTrader = business?.businessMode == "TRADER"

    val (headerTitle, headerSubtitle) = when {
        isTrader && selectedTab == 0 -> "Milk Sourcing Desk" to "Daily farmer milk intake • FAT/SNF testing"
        isTrader && selectedTab == 1 -> "Supplying Farmers & Khata" to "Farmer accounts, passbooks & payment settlements"
        isTrader && selectedTab == 2 -> "Bulk Commercial Supply" to "Wholesale milk dispatches & bulk accounts"
        selectedTab == 1 -> "Farmers Directory & Khata" to "Member Farmers • Passbooks • Cycle Payouts"
        selectedTab == 2 -> "Bulk Tanker Dispatches" to "Chilling Unit Outward Challans & Factory Invoicing"
        else -> "Milk Collection Desk" to "Shift-wise Farmer Intake • FAT/SNF Testing • Receipts"
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            if (onBack != null) {
                                IconButton(
                                    onClick = onBack,
                                    modifier = Modifier.size(36.dp).testTag("collection_back_button")
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = TextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Column {
                                Text(
                                    text = headerTitle,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = headerSubtitle,
                                    fontSize = 10.5.sp,
                                    color = TextSecondary,
                                    maxLines = 1
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Shift Switcher Pill (shown on Collection desk)
                            if (selectedTab == 0) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (selectedShift == "MORNING") FreshGoldLight else RoyalBlueLight,
                                    modifier = Modifier.clickable {
                                        val next = if (selectedShift == "MORNING") "EVENING" else "MORNING"
                                        viewModel.setShift(next)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (selectedShift == "MORNING") Icons.Default.WbSunny else Icons.Default.NightsStay,
                                            contentDescription = null,
                                            tint = if (selectedShift == "MORNING") WarmHoney else RoyalBluePrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = selectedShift,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedShift == "MORNING") WarmHoney else RoyalBluePrimary
                                        )
                                    }
                                }
                            }

                            // Rates & Formula Studio Quick Button
                            IconButton(
                                onClick = { showCustomFormulaStudio = true },
                                modifier = Modifier.size(34.dp).testTag("bmc_top_formula_btn")
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = RoyalBlueLight,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Science,
                                            contentDescription = "FAT/SNF Rates",
                                            tint = RoyalBluePrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            // BMC Center Settings Button
                            IconButton(
                                onClick = { onOpenSettings?.invoke() },
                                modifier = Modifier.size(34.dp).testTag("bmc_top_settings_button")
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Settings,
                                            contentDescription = "BMC Center Settings",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Tab Selector Pills (Only shown if showInternalTabs is true)
                    if (showInternalTabs) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val tabsList = if (isTrader) {
                                listOf(
                                    Triple(0, "🥛 Sourcing", Icons.Default.WaterDrop),
                                    Triple(1, "👥 Farmers", Icons.Default.People),
                                    Triple(2, "🚚 Bulk Supply", Icons.Default.LocalShipping)
                                )
                            } else {
                                listOf(
                                    Triple(0, "🥛 Collection", Icons.Default.WaterDrop),
                                    Triple(1, "👥 Farmers", Icons.Default.People),
                                    Triple(2, "🚚 Dispatches", Icons.Default.LocalShipping)
                                )
                            }
                            tabsList.forEach { (idx, title, icon) ->
                                val isSel = selectedTab == idx
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSel) RoyalBluePrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedTab = idx }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isSel) Color.White else TextSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = title.split(" ")[1],
                                            fontSize = 11.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSel) Color.White else TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = { showAddFarmerDialog = true },
                    containerColor = RoyalBluePrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_farmer_fab")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Add Farmer")
                }
            } else if (selectedTab == 2) {
                FloatingActionButton(
                    onClick = { showAddDispatchDialog = true },
                    containerColor = GrassGreen,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_dispatch_fab")
                ) {
                    Icon(Icons.Default.LocalShipping, contentDescription = "New Bulk Dispatch")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> CollectionDeskTab(
                    viewModel = viewModel,
                    farmers = farmers,
                    todayCollections = todayCollections,
                    selectedDate = formattedDate,
                    selectedShift = selectedShift,
                    onOpenFormulaStudio = { showCustomFormulaStudio = true },
                    onOpenSettings = { onOpenSettings?.invoke() }
                )
                1 -> FarmersDirectoryTab(
                    farmers = farmers,
                    collections = allCollections,
                    onOpenPassbook = { selectedFarmerForPassbook = it },
                    onRecordPayment = { showRecordFarmerPaymentDialog = it },
                    onDeleteFarmer = { viewModel.deleteFarmer(it.id) }
                )
                2 -> BulkDispatchesTab(
                    dispatches = bulkDispatches,
                    onDeleteDispatch = { viewModel.deleteBulkDispatch(it.id) }
                )
            }
        }
    }

    // Add Farmer Dialog
    if (showAddFarmerDialog) {
        AddFarmerDialog(
            onDismiss = { showAddFarmerDialog = false },
            onSave = { farmer ->
                viewModel.saveFarmer(farmer) {
                    showAddFarmerDialog = false
                }
            },
            businessId = business?.id ?: ""
        )
    }

    // Add Bulk Dispatch Dialog
    if (showAddDispatchDialog) {
        AddBulkDispatchDialog(
            onDismiss = { showAddDispatchDialog = false },
            onSave = { dispatch ->
                viewModel.saveBulkDispatch(dispatch) {
                    showAddDispatchDialog = false
                }
            },
            businessId = business?.id ?: "",
            selectedDateEpoch = selectedDate,
            shift = selectedShift
        )
    }

    // Farmer Passbook Modal
    selectedFarmerForPassbook?.let { farmer ->
        FarmerPassbookDialog(
            farmer = farmer,
            collections = allCollections.filter { it.farmerId == farmer.id },
            onDismiss = { selectedFarmerForPassbook = null },
            onPay = {
                selectedFarmerForPassbook = null
                showRecordFarmerPaymentDialog = farmer
            }
        )
    }

    // Record Farmer Payment Dialog
    showRecordFarmerPaymentDialog?.let { farmer ->
        RecordFarmerPaymentDialog(
            farmer = farmer,
            onDismiss = { showRecordFarmerPaymentDialog = null },
            onConfirmPayment = { amount, mode, ref ->
                viewModel.recordFarmerPayment(farmer.id, amount, mode, ref)
                showRecordFarmerPaymentDialog = null
            }
        )
    }

    if (showCustomFormulaStudio) {
        CustomFormulaStudioDialog(
            business = business,
            viewModel = viewModel,
            onDismiss = { showCustomFormulaStudio = false }
        )
    }
}

@Composable
fun CollectionDeskTab(
    viewModel: MilkMateViewModel,
    farmers: List<FarmerEntity>,
    todayCollections: List<MilkCollectionEntity>,
    selectedDate: String,
    selectedShift: String,
    onOpenFormulaStudio: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    var searchFarmerCode by remember { mutableStateOf("") }
    var selectedFarmer by remember { mutableStateOf<FarmerEntity?>(null) }
    var milkType by remember { mutableStateOf("COW") }
    var quantityText by remember { mutableStateOf("5.0") }
    var fatText by remember { mutableStateOf("4.2") }
    var snfText by remember { mutableStateOf("8.5") }
    var clrText by remember { mutableStateOf("28.0") }
    var sampleNo by remember { mutableStateOf("") }
    var showWhatsAppSlipModal by remember { mutableStateOf<MilkCollectionEntity?>(null) }

    val business by viewModel.currentBusiness.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Auto calculate Rate & Total
    val qty = quantityText.toDoubleOrNull() ?: 0.0
    val fatVal = fatText.toDoubleOrNull() ?: 4.0
    val snfVal = snfText.toDoubleOrNull() ?: 8.5
    val clrVal = clrText.toDoubleOrNull() ?: 0.0

    val calculatedRate = viewModel.calculateProcurementRate(milkType, fatVal, snfVal, clrVal) + (selectedFarmer?.rateAdjustmentPerLiter ?: 0.0)
    val totalAmount = qty * calculatedRate

    val filteredCollections = todayCollections.filter { it.shift == selectedShift }
    val totalShiftLiters = filteredCollections.sumOf { it.quantityLiters }
    val totalShiftPayout = filteredCollections.sumOf { it.totalAmount }
    val avgFat = if (filteredCollections.isNotEmpty()) filteredCollections.map { it.fat }.average() else 0.0
    val avgSnf = if (filteredCollections.isNotEmpty()) filteredCollections.map { it.snf }.average() else 0.0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // BMC Center Quick Settings & Rate Formula Bar
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth().testTag("bmc_quick_settings_strip")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = RoyalBlueLight,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = RoyalBluePrimary,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "BMC Kendra & Settings",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Society Code • Rate Formulas • Printer Setup",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalButton(
                            onClick = onOpenFormulaStudio,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp).testTag("bmc_formula_studio_btn")
                        ) {
                            Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rates", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onOpenSettings,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                            modifier = Modifier.height(34.dp).testTag("bmc_open_settings_btn")
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Settings", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Shift Inflow Summary Bar
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = OceanMidnight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚡ $selectedShift Collection Live Stats",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = selectedDate,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Liters", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                            Text(
                                text = "${String.format(Locale.getDefault(), "%.1f", totalShiftLiters)} L",
                                color = FreshGold,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Column {
                            Text("Avg FAT / SNF", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                            Text(
                                text = "${String.format(Locale.getDefault(), "%.1f", avgFat)}% / ${String.format(Locale.getDefault(), "%.1f", avgSnf)}%",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text("Total Payout", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                            Text(
                                text = "₹${String.format(Locale.getDefault(), "%.0f", totalShiftPayout)}",
                                color = GrassGreen,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Column {
                            Text("Farmers", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                            Text(
                                text = "${filteredCollections.size}",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Fast Entry Desk Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
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
                        Text(
                            text = "🥛 Fast Farmer Entry Desk",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (selectedFarmer != null) {
                            Text(
                                text = "Code #${selectedFarmer?.farmerCode}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoyalBluePrimary
                            )
                        }
                    }

                    // Farmer Search / Select Bar
                    OutlinedTextField(
                        value = searchFarmerCode,
                        onValueChange = { query ->
                            searchFarmerCode = query
                            val match = farmers.find {
                                it.farmerCode.equals(query.trim(), ignoreCase = true) ||
                                it.name.contains(query, ignoreCase = true) ||
                                it.mobile.contains(query)
                            }
                            if (match != null) {
                                selectedFarmer = match
                                milkType = if (match.milkType == "BUFFALO") "BUFFALO" else "COW"
                                quantityText = match.defaultQuantityEstimate.toString()
                            }
                        },
                        label = { Text("Search Farmer by Code, Name, or Mobile") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (selectedFarmer != null) {
                                IconButton(onClick = {
                                    selectedFarmer = null
                                    searchFarmerCode = ""
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("farmer_search_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Quick Select Farmer Chips
                    if (farmers.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(farmers.take(8)) { f ->
                                val isSelected = selectedFarmer?.id == f.id
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) RoyalBlueLight else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, RoyalBluePrimary) else null,
                                    modifier = Modifier.clickable {
                                        selectedFarmer = f
                                        searchFarmerCode = f.farmerCode
                                        milkType = if (f.milkType == "BUFFALO") "BUFFALO" else "COW"
                                        quantityText = f.defaultQuantityEstimate.toString()
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "#${f.farmerCode} ${f.name.take(10)}",
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) RoyalBluePrimary else TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Selected Farmer Details Banner
                    selectedFarmer?.let { f ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = RoyalBlueLight.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = f.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RoyalBluePrimary
                                    )
                                    Text(
                                        text = "Village: ${f.village.ifBlank { "Local" }} • Mobile: ${f.mobile}",
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Payable Due", fontSize = 9.sp, color = TextSecondary)
                                    Text(
                                        text = "₹${String.format(Locale.getDefault(), "%.1f", f.balancePayable)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DangerRed
                                    )
                                }
                            }
                        }
                    }

                    // Milk Type Toggle (Only if Both Cow & Buffalo are supported)
                    val supportedTypes = business?.supportedMilkTypes ?: "BOTH"
                    if (supportedTypes == "BOTH") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("COW" to "🐄 Cow Milk", "BUFFALO" to "🐃 Buffalo Milk").forEach { (type, label) ->
                                val isSel = milkType == type
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSel) RoyalBluePrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { milkType = type }
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSel) Color.White else TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // Quantity Input + Quick Steppers
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Quantity (Liters)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("+1L" to 1.0, "+2L" to 2.0, "+5L" to 5.0, "+10L" to 10.0).forEach { (btnText, addQty) ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = RoyalBlueLight,
                                        modifier = Modifier.clickable {
                                            val current = quantityText.toDoubleOrNull() ?: 0.0
                                            quantityText = String.format(Locale.getDefault(), "%.1f", current + addQty)
                                        }
                                    ) {
                                        Text(
                                            text = btnText,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RoyalBluePrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedTextField(
                            value = quantityText,
                            onValueChange = { quantityText = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth().testTag("procure_quantity_input"),
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = { Text("Liters", modifier = Modifier.padding(end = 12.dp), color = TextSecondary, fontSize = 12.sp) }
                        )
                    }

                    // Quality Parameters: FAT, SNF, CLR
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = fatText,
                            onValueChange = { fatText = it },
                            label = { Text("FAT %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f).testTag("procure_fat_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = snfText,
                            onValueChange = { snfText = it },
                            label = { Text("SNF %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f).testTag("procure_snf_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = clrText,
                            onValueChange = {
                                clrText = it
                                val clr = it.toDoubleOrNull() ?: 0.0
                                val fat = fatText.toDoubleOrNull() ?: 4.0
                                if (clr > 0) {
                                    val calcSnf = (clr / 4.0) + (fat * 0.25) + 0.36
                                    snfText = String.format(Locale.getDefault(), "%.2f", calcSnf)
                                }
                            },
                            label = { Text("CLR (Lacto)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Live Calculated Rate & Amount Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = RoyalBluePrimary.copy(alpha = 0.15f)
                                    ) {
                                        val modeStr = business?.pricingMode ?: "STANDARD"
                                        val yieldInfo = when (modeStr.uppercase()) {
                                            "KHOA_YIELD", "KHOYA_YIELD" -> " • 🍬 Khoa: ${String.format(Locale.getDefault(), "%.1f", PricingEngine.calculateKhoaYieldGrams(fatVal, snfVal))}g/L"
                                            "PANEER_YIELD" -> " • 🧀 Paneer: ${String.format(Locale.getDefault(), "%.1f", PricingEngine.calculatePaneerYieldGrams(fatVal, snfVal))}g/L"
                                            "GHEE_YIELD" -> " • 🏺 Ghee: ${String.format(Locale.getDefault(), "%.1f", PricingEngine.calculateGheeYieldGrams(fatVal))}g/L"
                                            else -> ""
                                        }
                                        Text(
                                            text = "⚙️ $modeStr$yieldInfo",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RoyalBluePrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                TextButton(
                                    onClick = onOpenFormulaStudio,
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(12.dp), tint = RoyalBluePrimary)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Tweak Formula", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Formula Rate / L", fontSize = 10.sp, color = TextSecondary)
                                    Text(
                                        text = "₹${String.format(Locale.getDefault(), "%.2f", calculatedRate)} / L",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RoyalBluePrimary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Total Procurement Cost", fontSize = 10.sp, color = TextSecondary)
                                    Text(
                                        text = "₹${String.format(Locale.getDefault(), "%.2f", totalAmount)}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = GrassGreen
                                    )
                                }
                            }
                        }
                    }

                    // Save Button
                    Button(
                        onClick = {
                            val f = selectedFarmer
                            if (f == null) {
                                viewModel.showMessage("Please select or search a farmer first")
                                return@Button
                            }
                            if (qty <= 0) {
                                viewModel.showMessage("Please enter valid milk quantity")
                                return@Button
                            }

                            val collection = MilkCollectionEntity(
                                id = UUID.randomUUID().toString(),
                                businessId = business?.id ?: "",
                                farmerId = f.id,
                                farmerCode = f.farmerCode,
                                farmerName = f.name,
                                dateEpochMidnight = MilkMateViewModel.getTodayMidnightMillis(),
                                shift = selectedShift,
                                milkType = milkType,
                                quantityLiters = qty,
                                fat = fatVal,
                                snf = snfVal,
                                clr = clrVal,
                                ratePerLiter = calculatedRate,
                                totalAmount = totalAmount,
                                paymentStatus = "PENDING",
                                sampleNo = sampleNo
                            )

                            viewModel.saveMilkCollection(collection) {
                                showWhatsAppSlipModal = collection
                                // Reset form for next farmer
                                searchFarmerCode = ""
                                selectedFarmer = null
                                sampleNo = ""
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_procure_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save & Log Collection Entry", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }

        // Today's Shift Procurement Log
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📋 Today's $selectedShift Entries (${filteredCollections.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }

        if (filteredCollections.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No collection entries logged for this shift yet.", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(filteredCollections) { entry ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = if (entry.milkType == "BUFFALO") DeepOceanNavy else RoyalBlueLight,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "#${entry.farmerCode}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (entry.milkType == "BUFFALO") Color.White else RoyalBluePrimary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(entry.farmerName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                Text(
                                    text = "${entry.milkType} • FAT: ${entry.fat}% | SNF: ${entry.snf}% • @₹${String.format(Locale.getDefault(), "%.1f", entry.ratePerLiter)}/L",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${entry.quantityLiters} L",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = RoyalBluePrimary
                                )
                                Text(
                                    text = "₹${String.format(Locale.getDefault(), "%.2f", entry.totalAmount)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GrassGreen
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { viewModel.deleteMilkCollection(entry.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = DangerRed, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Receipt / WhatsApp Slip Dialog
    showWhatsAppSlipModal?.let { entry ->
        AlertDialog(
            onDismissRequest = { showWhatsAppSlipModal = null },
            title = { Text("🥛 Milk Procurement Slip Generated") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Farmer: ${entry.farmerName} (#${entry.farmerCode})", fontWeight = FontWeight.Bold)
                    Text("Shift: ${entry.shift} | Date: $selectedDate")
                    Text("Milk: ${entry.milkType} | Volume: ${entry.quantityLiters} Liters")
                    Text("FAT: ${entry.fat}% | SNF: ${entry.snf}%")
                    Text("Rate / L: ₹${String.format(Locale.getDefault(), "%.2f", entry.ratePerLiter)}")
                    Text("Total Amount: ₹${String.format(Locale.getDefault(), "%.2f", entry.totalAmount)}", fontWeight = FontWeight.ExtraBold, color = GrassGreen)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val message = "🥛 *${business?.businessName ?: "Dairy Centre"} Procurement Slip*\n" +
                                "Farmer: ${entry.farmerName} (#${entry.farmerCode})\n" +
                                "Date: $selectedDate (${entry.shift})\n" +
                                "Milk: ${entry.milkType}\n" +
                                "Qty: ${entry.quantityLiters} L\n" +
                                "FAT: ${entry.fat}% | SNF: ${entry.snf}%\n" +
                                "Rate: ₹${String.format(Locale.getDefault(), "%.2f", entry.ratePerLiter)}/L\n" +
                                "Amount: ₹${String.format(Locale.getDefault(), "%.2f", entry.totalAmount)}\n" +
                                "Thank you!"
                        val sendIntent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://api.whatsapp.com/send?text=" + Uri.encode(message))
                        }
                        context.startActivity(sendIntent)
                        showWhatsAppSlipModal = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GrassGreen)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share WhatsApp Receipt")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWhatsAppSlipModal = null }) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
fun FarmersDirectoryTab(
    farmers: List<FarmerEntity>,
    collections: List<MilkCollectionEntity>,
    onOpenPassbook: (FarmerEntity) -> Unit,
    onRecordPayment: (FarmerEntity) -> Unit,
    onDeleteFarmer: (FarmerEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = farmers.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.farmerCode.contains(searchQuery, ignoreCase = true) ||
        it.village.contains(searchQuery, ignoreCase = true)
    }

    val totalPayableDues = farmers.sumOf { it.balancePayable }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Dues Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Registered Farmers", fontSize = 11.sp, color = TextSecondary)
                        Text("${farmers.size} Farmers", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Total Pending Farmer Payouts", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            "₹${String.format(Locale.getDefault(), "%.2f", totalPayableDues)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DangerRed
                        )
                    }
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Farmers by Code, Name, Village") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().testTag("farmer_directory_search"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        if (filtered.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No farmers found. Tap '+' to add a new farmer.", color = TextSecondary)
                }
            }
        } else {
            items(filtered) { farmer ->
                val farmerCollections = collections.filter { it.farmerId == farmer.id }
                val totalLitersProcured = farmerCollections.sumOf { it.quantityLiters }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenPassbook(farmer) }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = RoyalBlueLight,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "#${farmer.farmerCode}",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp,
                                            color = RoyalBluePrimary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(farmer.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                    Text(
                                        text = "${farmer.village.ifBlank { "Village" }} • ${farmer.milkType} • ${farmer.mobile}",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("Balance Payable", fontSize = 10.sp, color = TextSecondary)
                                Text(
                                    text = "₹${String.format(Locale.getDefault(), "%.2f", farmer.balancePayable)}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = if (farmer.balancePayable > 0) DangerRed else GrassGreen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Supplied: ${String.format(Locale.getDefault(), "%.1f", totalLitersProcured)}L",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = { onOpenPassbook(farmer) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("📖 Passbook", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = { onRecordPayment(farmer) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = GrassGreen),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("💰 Pay Farmer", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BulkDispatchesTab(
    dispatches: List<BulkDispatchEntity>,
    onDeleteDispatch: (BulkDispatchEntity) -> Unit
) {
    val totalDispatchedLiters = dispatches.sumOf { it.totalLiters }
    val totalDispatchBilling = dispatches.sumOf { it.totalAmount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DeepOceanNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🚚 Bulk Dispatches & Tanker Outwards", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Shipments to Chilling Centres, Dairies & Wholesale Buyers", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Dispatched", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                            Text(
                                text = "${String.format(Locale.getDefault(), "%.1f", totalDispatchedLiters)} L",
                                color = FreshGold,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Bulk Billing", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                            Text(
                                text = "₹${String.format(Locale.getDefault(), "%.2f", totalDispatchBilling)}",
                                color = GrassGreen,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Recent Tanker & Bulk Shipments (${dispatches.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        if (dispatches.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No bulk dispatches recorded yet. Tap '+' to log a shipment.", color = TextSecondary)
                }
            }
        } else {
            items(dispatches) { d ->
                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                val dateStr = sdf.format(Date(d.dateEpochMidnight))

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(d.buyerOrPlantName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                Text(
                                    text = "$dateStr (${d.shift}) • Vehicle: ${d.vehicleNo.ifBlank { "Direct" }}",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${d.totalLiters} L",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = RoyalBluePrimary
                                )
                                Text(
                                    text = "₹${String.format(Locale.getDefault(), "%.2f", d.totalAmount)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = GrassGreen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Avg FAT: ${d.avgFat}% | SNF: ${d.avgSnf}% • @₹${d.ratePerLiter}/L",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )

                            IconButton(
                                onClick = { onDeleteDispatch(d) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = DangerRed, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddFarmerDialog(
    onDismiss: () -> Unit,
    onSave: (FarmerEntity) -> Unit,
    businessId: String
) {
    var farmerCode by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var village by remember { mutableStateOf("") }
    var milkType by remember { mutableStateOf("COW") }
    var paymentMode by remember { mutableStateOf("UPI") }
    var upiId by remember { mutableStateOf("") }
    var bankAccountNo by remember { mutableStateOf("") }
    var ifscCode by remember { mutableStateOf("") }
    var defaultQty by remember { mutableStateOf("5.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("👥 Register New Farmer") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = farmerCode,
                            onValueChange = { farmerCode = it },
                            label = { Text("Code (e.g. 104)") },
                            modifier = Modifier.weight(1f).testTag("farmer_code_field"),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = defaultQty,
                            onValueChange = { defaultQty = it },
                            label = { Text("Est. Qty (L)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Farmer Full Name *") },
                        modifier = Modifier.fillMaxWidth().testTag("farmer_name_field"),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { mobile = it },
                        label = { Text("Mobile Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = village,
                        onValueChange = { village = it },
                        label = { Text("Village / Area") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                item {
                    Text("Milk Type Supplied:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("COW", "BUFFALO", "BOTH").forEach { type ->
                            val isSel = milkType == type
                            FilterChip(
                                selected = isSel,
                                onClick = { milkType = type },
                                label = { Text(type, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                item {
                    Text("Payment Preference:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("UPI", "CASH", "BANK_TRANSFER").forEach { mode ->
                            val isSel = paymentMode == mode
                            FilterChip(
                                selected = isSel,
                                onClick = { paymentMode = mode },
                                label = { Text(mode.replace("_", " "), fontSize = 11.sp) }
                            )
                        }
                    }
                }

                if (paymentMode == "UPI") {
                    item {
                        OutlinedTextField(
                            value = upiId,
                            onValueChange = { upiId = it },
                            label = { Text("UPI ID (e.g. name@upi)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                } else if (paymentMode == "BANK_TRANSFER") {
                    item {
                        OutlinedTextField(
                            value = bankAccountNo,
                            onValueChange = { bankAccountNo = it },
                            label = { Text("Bank Account Number") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = ifscCode,
                            onValueChange = { ifscCode = it },
                            label = { Text("IFSC Code") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || farmerCode.isBlank()) return@Button
                    val entity = FarmerEntity(
                        id = UUID.randomUUID().toString(),
                        businessId = businessId,
                        farmerCode = farmerCode.trim(),
                        name = name.trim(),
                        mobile = mobile.trim(),
                        village = village.trim(),
                        milkType = milkType,
                        defaultQuantityEstimate = defaultQty.toDoubleOrNull() ?: 5.0,
                        paymentMode = paymentMode,
                        upiId = upiId.trim(),
                        bankAccountNo = bankAccountNo.trim(),
                        ifscCode = ifscCode.trim()
                    )
                    onSave(entity)
                },
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                modifier = Modifier.testTag("submit_farmer_button")
            ) {
                Text("Save Farmer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddBulkDispatchDialog(
    onDismiss: () -> Unit,
    onSave: (BulkDispatchEntity) -> Unit,
    businessId: String,
    selectedDateEpoch: Long,
    shift: String
) {
    var plantName by remember { mutableStateOf("") }
    var vehicleNo by remember { mutableStateOf("") }
    var driverName by remember { mutableStateOf("") }
    var totalLitersText by remember { mutableStateOf("500.0") }
    var avgFatText by remember { mutableStateOf("4.5") }
    var avgSnfText by remember { mutableStateOf("8.8") }
    var ratePerLiterText by remember { mutableStateOf("56.0") }

    val liters = totalLitersText.toDoubleOrNull() ?: 0.0
    val rate = ratePerLiterText.toDoubleOrNull() ?: 0.0
    val totalBilling = liters * rate

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🚚 New Bulk / Tanker Dispatch") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    OutlinedTextField(
                        value = plantName,
                        onValueChange = { plantName = it },
                        label = { Text("Buyer / Dairy Plant Name *") },
                        placeholder = { Text("e.g. Mother Dairy Chilling Plant") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = totalLitersText,
                            onValueChange = { totalLitersText = it },
                            label = { Text("Total Liters *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = ratePerLiterText,
                            onValueChange = { ratePerLiterText = it },
                            label = { Text("Rate / Liter (₹) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = avgFatText,
                            onValueChange = { avgFatText = it },
                            label = { Text("Avg FAT %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = avgSnfText,
                            onValueChange = { avgSnfText = it },
                            label = { Text("Avg SNF %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = vehicleNo,
                            onValueChange = { vehicleNo = it },
                            label = { Text("Tanker / Vehicle No") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = driverName,
                            onValueChange = { driverName = it },
                            label = { Text("Driver Name") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = RoyalBlueLight,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total Invoice Amount:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RoyalBluePrimary)
                            Text(
                                "₹${String.format(Locale.getDefault(), "%.2f", totalBilling)}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = GrassGreen
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (plantName.isBlank() || liters <= 0 || rate <= 0) return@Button
                    val entity = BulkDispatchEntity(
                        id = UUID.randomUUID().toString(),
                        businessId = businessId,
                        buyerOrPlantName = plantName.trim(),
                        dateEpochMidnight = selectedDateEpoch,
                        shift = shift,
                        totalLiters = liters,
                        ratePerLiter = rate,
                        totalAmount = totalBilling,
                        avgFat = avgFatText.toDoubleOrNull() ?: 4.5,
                        avgSnf = avgSnfText.toDoubleOrNull() ?: 8.8,
                        vehicleNo = vehicleNo.trim(),
                        driverName = driverName.trim(),
                        paymentStatus = "PENDING"
                    )
                    onSave(entity)
                },
                colors = ButtonDefaults.buttonColors(containerColor = GrassGreen)
            ) {
                Text("Confirm Dispatch")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun FarmerPassbookDialog(
    farmer: FarmerEntity,
    collections: List<MilkCollectionEntity>,
    onDismiss: () -> Unit,
    onPay: () -> Unit
) {
    val totalEarnings = collections.sumOf { it.totalAmount }
    val totalLiters = collections.sumOf { it.quantityLiters }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("📖 Farmer Passbook", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("${farmer.name} (Code #${farmer.farmerCode})", fontSize = 12.sp, color = RoyalBluePrimary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Summary Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = OceanMidnight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Liters", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                            Text("${String.format(Locale.getDefault(), "%.1f", totalLiters)} L", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Column {
                            Text("Total Earnings", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                            Text("₹${String.format(Locale.getDefault(), "%.0f", totalEarnings)}", color = FreshGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Balance Due", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                            Text("₹${String.format(Locale.getDefault(), "%.0f", farmer.balancePayable)}", color = DangerRed, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        }
                    }
                }

                Text("Supply History:", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                if (collections.isEmpty()) {
                    Text("No collection logs yet for this farmer.", color = TextSecondary, fontSize = 12.sp)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(collections) { c ->
                            val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
                            val dStr = sdf.format(Date(c.dateEpochMidnight))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("$dStr • ${c.shift} (${c.milkType})", fontWeight = FontWeight.Medium, fontSize = 11.sp)
                                        Text("${c.quantityLiters}L • FAT: ${c.fat}% • @₹${c.ratePerLiter}/L", fontSize = 10.sp, color = TextSecondary)
                                    }
                                    Text(
                                        "₹${String.format(Locale.getDefault(), "%.2f", c.totalAmount)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = GrassGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onPay,
                colors = ButtonDefaults.buttonColors(containerColor = GrassGreen)
            ) {
                Text("💰 Record Payment to Farmer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun RecordFarmerPaymentDialog(
    farmer: FarmerEntity,
    onDismiss: () -> Unit,
    onConfirmPayment: (amount: Double, mode: String, ref: String) -> Unit
) {
    var amountText by remember { mutableStateOf(String.format(Locale.getDefault(), "%.2f", farmer.balancePayable)) }
    var paymentMode by remember { mutableStateOf(farmer.paymentMode) }
    var refText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("💰 Pay Farmer Payout") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Farmer: ${farmer.name} (Code #${farmer.farmerCode})", fontWeight = FontWeight.Bold)
                Text("Pending Balance Due: ₹${String.format(Locale.getDefault(), "%.2f", farmer.balancePayable)}", color = DangerRed)

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Payment Amount (₹) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Text("Payment Method:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("UPI", "CASH", "BANK_TRANSFER").forEach { m ->
                        val isSel = paymentMode == m
                        FilterChip(
                            selected = isSel,
                            onClick = { paymentMode = m },
                            label = { Text(m.replace("_", " "), fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = refText,
                    onValueChange = { refText = it },
                    label = { Text("Transaction Reference / Note") },
                    placeholder = { Text("e.g. UPI Ref #827394") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt <= 0) return@Button
                    onConfirmPayment(amt, paymentMode, refText)
                },
                colors = ButtonDefaults.buttonColors(containerColor = GrassGreen)
            ) {
                Text("Confirm Payout")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
