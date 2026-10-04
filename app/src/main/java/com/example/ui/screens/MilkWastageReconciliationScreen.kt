package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.MilkWastageEntity
import com.example.data.repository.MilkReconciliationSummary
import com.example.ui.MilkMateViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Milk Wastage & Daily Volume Reconciliation Screen
 * 
 * Functions:
 * 1. Daily Inflow vs Outflow Volume Balance & Variance Tracking
 * 2. Milk Spoilage / Incident Logging (Curdling, Spillage, Unsold, Quality Rejection)
 * 3. Wastage Financial Loss Reporting & Analytics
 * 4. Actionable Spoilage Reduction Recommendations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MilkWastageReconciliationScreen(
    viewModel: MilkMateViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedDateEpoch by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var reconciliationSummary by remember { mutableStateOf<MilkReconciliationSummary?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showAddWastageDialog by remember { mutableStateOf(false) }

    val allWastage by viewModel.milkWastageList.collectAsStateWithLifecycle()

    // Reload reconciliation on date change or wastage list change
    LaunchedEffect(selectedDateEpoch, allWastage) {
        isRefreshing = true
        reconciliationSummary = viewModel.getDailyMilkReconciliation(selectedDateEpoch)
        isRefreshing = false
    }

    val summary = reconciliationSummary ?: MilkReconciliationSummary(date = selectedDateEpoch)

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy (EEE)", Locale.getDefault()) }
    val displayDate = remember(selectedDateEpoch) { dateFormat.format(Date(selectedDateEpoch)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Milk Wastage & Reconciliation", fontWeight = FontWeight.Bold, fontSize = 16.5.sp)
                        Text("Volume Balance & Loss Control", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showAddWastageDialog = true }) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Log Wastage", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = RoyalBluePrimary,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddWastageDialog = true },
                containerColor = Color(0xFFC62828), // Danger / alert red for loss tracking
                contentColor = Color.White,
                icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
                text = { Text("Log Milk Loss / Spoilage", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ================= DATE SELECTOR BAR =================
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                val cal = Calendar.getInstance().apply { timeInMillis = selectedDateEpoch }
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        val newCal = Calendar.getInstance().apply {
                                            set(year, month, day, 0, 0, 0)
                                            set(Calendar.MILLISECOND, 0)
                                        }
                                        selectedDateEpoch = newCal.timeInMillis
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(displayDate, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = TextPrimary)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = RoyalBluePrimary)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Quick Today Button
                            FilledTonalButton(
                                onClick = {
                                    val now = Calendar.getInstance().apply {
                                        set(Calendar.HOUR_OF_DAY, 0)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    selectedDateEpoch = now.timeInMillis
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Today", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Quick Yesterday Button
                            FilledTonalButton(
                                onClick = {
                                    val yest = Calendar.getInstance().apply {
                                        add(Calendar.DAY_OF_YEAR, -1)
                                        set(Calendar.HOUR_OF_DAY, 0)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    selectedDateEpoch = yest.timeInMillis
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Yesterday", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // ================= HERO DAILY VOLUME RECONCILIATION CARD =================
            item {
                val isBalanced = Math.abs(summary.varianceLiters) < 0.2
                val isShortage = summary.varianceLiters > 0.2
                val isSurplus = summary.varianceLiters < -0.2

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isBalanced -> Color(0xFF1B5E20)
                            summary.loggedWastageLiters > 5.0 -> Color(0xFFB71C1C)
                            else -> RoyalBluePrimary
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DAILY MILK RECONCILIATION",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.85f)
                            )

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = when {
                                    isBalanced -> Color.White.copy(alpha = 0.25f)
                                    isShortage -> Color(0xFFFFCC80)
                                    else -> Color.White.copy(alpha = 0.25f)
                                }
                            ) {
                                Text(
                                    text = when {
                                        isBalanced -> "🟢 Balanced ✓"
                                        isShortage -> "⚠️ ${summary.varianceLiters} L Unaccounted"
                                        else -> "🔵 Surplus ${Math.abs(summary.varianceLiters)} L"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isShortage) Color(0xFFE65100) else Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // 3 Big Volume Blocks
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Total Inflow
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.15f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("📥 Total Inflow", fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.85f))
                                    Text("${summary.totalInflowLiters} L", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                    Text("Farm + Farmer", fontSize = 9.5.sp, color = Color.White.copy(alpha = 0.7f))
                                }
                            }

                            // Total Outflow
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.15f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("📤 Total Outflow", fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.85f))
                                    Text("${summary.totalOutflowLiters} L", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                    Text("Routes + Tankers", fontSize = 9.5.sp, color = Color.White.copy(alpha = 0.7f))
                                }
                            }

                            // Logged Wastage
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (summary.loggedWastageLiters > 0) Color(0xFFFFEBEE) else Color.White.copy(alpha = 0.15f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("🗑️ Logged Loss", fontSize = 10.5.sp, color = if (summary.loggedWastageLiters > 0) DangerRed else Color.White.copy(alpha = 0.85f))
                                    Text("${summary.loggedWastageLiters} L", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = if (summary.loggedWastageLiters > 0) DangerRed else Color.White)
                                    Text("₹${Math.round(summary.loggedWastageLossAmount)} Loss", fontSize = 9.5.sp, color = if (summary.loggedWastageLiters > 0) DangerRed else Color.White.copy(alpha = 0.7f))
                                }
                            }
                        }

                        // Detailed Breakdown Row
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Black.copy(alpha = 0.2f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Inflow Sources: Farm Milking: ${summary.farmMilkingLiters} L • Farmers Collected: ${summary.farmerCollectionLiters} L", fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.9f))
                                Text("Outflow Channels: Delivered to Customers: ${summary.customerDeliveredLiters} L • Dispatched to Tankers: ${summary.bulkDispatchedLiters} L • Retail: ${summary.retailOrderLiters} L", fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.9f))
                            }
                        }
                    }
                }
            }

            // ================= ACTIONABLE SPOILAGE REDUCTION ADVISOR =================
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, WarmHoney.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = WarmHoney.copy(alpha = 0.15f),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = WarmHoney, modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Smart Advisor: How to Minimize Milk Wastage", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = TextPrimary)
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ReconciliationTipItem("⏱️ Rapid Chilling", "Chill morning and evening milk to below 4°C within 90 minutes to prevent bacterial curdling.")
                            ReconciliationTipItem("🚚 Route Pouch Buffering", "Pack max +2% extra buffer on delivery routes to avoid returning unchilled leftover pouches in afternoon heat.")
                            ReconciliationTipItem("🧼 Daily Can & Vat Sanitization", "Sterilize collection cans with 80°C hot water to eliminate souring bacteria.")
                            ReconciliationTipItem("🧀 Convert Leftover Milk into Paneer / Curd", "Process unsold fresh milk immediately into Paneer (₹350/kg) or Dahi rather than letting it curdle.")
                        }
                    }
                }
            }

            // ================= LOGGED WASTAGE INCIDENTS LIST =================
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Logged Spoilage & Loss Incidents",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "${allWastage.size} Incidents",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            if (allWastage.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(36.dp))
                            Text("Zero Spoilage or Losses Logged", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = TextPrimary)
                            Text("All milk produced & procured is accounted for cleanly.", fontSize = 11.5.sp, color = TextSecondary)
                        }
                    }
                }
            } else {
                items(allWastage, key = { it.id }) { item ->
                    WastageIncidentCard(
                        incident = item,
                        onDelete = { viewModel.deleteMilkWastage(item.id) }
                    )
                }
            }

            // Bottom space for FAB
            item {
                Spacer(modifier = Modifier.height(70.dp))
            }
        }
    }

    // ================= LOG WASTAGE MODAL DIALOG =================
    if (showAddWastageDialog) {
        AddMilkWastageDialog(
            defaultDate = selectedDateEpoch,
            onDismiss = { showAddWastageDialog = false },
            onSave = { quantity, reason, milkType, shift, details, costPerL, batch, action ->
                viewModel.logMilkWastage(
                    quantityLiters = quantity,
                    reason = reason,
                    milkType = milkType,
                    shift = shift,
                    reasonDetails = details,
                    costPerLiter = costPerL,
                    batchSource = batch,
                    preventativeAction = action,
                    date = selectedDateEpoch
                )
                showAddWastageDialog = false
            }
        )
    }
}

@Composable
fun ReconciliationTipItem(title: String, desc: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = RoyalBluePrimary)
            Text(desc, fontSize = 10.5.sp, color = TextSecondary, lineHeight = 14.sp)
        }
    }
}

@Composable
fun WastageIncidentCard(
    incident: MilkWastageEntity,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val timeStr = remember(incident.date) { dateFormat.format(Date(incident.date)) }

    val (reasonTitle, badgeColor) = when (incident.reason) {
        "CURDLING_SPOILAGE" -> Pair("🥛 Curdled / Milk Soured (दूध फटा)", Color(0xFFD32F2F))
        "TRANSIT_SPILLAGE" -> Pair("🚚 Transit Spillage (छलकाव)", Color(0xFFE65100))
        "UNSOLD_EXPIRED" -> Pair("⏱️ Unsold Leftover (बचा दूध)", Color(0xFFF57C00))
        "QUALITY_REJECTION" -> Pair("🧪 Quality & FAT Rejection", Color(0xFF7B1FA2))
        "CHILLING_FAILURE" -> Pair("❄️ Power / Chilling Failure", Color(0xFF0288D1))
        "CALF_FEEDING_EXCESS" -> Pair("🐄 Calf Feeding Excess", Color(0xFF388E3C))
        else -> Pair("🛠️ Equipment Leak / Other", Color(0xFF616161))
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = reasonTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = timeStr,
                    fontSize = 10.5.sp,
                    color = TextSecondary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${incident.quantityLiters} Liters (${incident.milkType} • ${incident.shift})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Source: ${incident.batchSource}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${Math.round(incident.totalLossAmount)} Loss",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = DangerRed
                    )
                    Text(
                        text = "@ ₹${incident.costPerLiter}/L",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }

            if (incident.preventativeAction.isNotBlank() || incident.reasonDetails.isNotBlank()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                if (incident.reasonDetails.isNotBlank()) {
                    Text("Notes: ${incident.reasonDetails}", fontSize = 10.5.sp, color = TextSecondary)
                }
                if (incident.preventativeAction.isNotBlank()) {
                    Text("Action to prevent: ${incident.preventativeAction}", fontSize = 10.5.sp, color = RoyalBluePrimary, fontWeight = FontWeight.Medium)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onDelete,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Remove", fontSize = 11.sp, color = TextSecondary)
                }
            }
        }
    }
}

/**
 * Add / Log Milk Wastage Dialog
 */
@Composable
fun AddMilkWastageDialog(
    defaultDate: Long,
    onDismiss: () -> Unit,
    onSave: (quantity: Double, reason: String, milkType: String, shift: String, details: String, costPerL: Double, batch: String, action: String) -> Unit
) {
    var quantityText by remember { mutableStateOf("") }
    var selectedReason by remember { mutableStateOf("CURDLING_SPOILAGE") }
    var selectedMilkType by remember { mutableStateOf("COW") }
    var selectedShift by remember { mutableStateOf("MORNING") }
    var batchSource by remember { mutableStateOf("Farm Morning Milking") }
    var costPerLiterText by remember { mutableStateOf("50.0") }
    var reasonDetails by remember { mutableStateOf("") }
    var preventativeAction by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val reasons = listOf(
        Pair("CURDLING_SPOILAGE", "🥛 Curdling / Milk Souring (दूध फटा)"),
        Pair("TRANSIT_SPILLAGE", "🚚 Transit / Delivery Spillage (छलकाव)"),
        Pair("UNSOLD_EXPIRED", "⏱️ Unsold Leftover (बचा हुआ दूध खराब)"),
        Pair("QUALITY_REJECTION", "🧪 Quality / Low FAT Rejection (अस्वीकृत)"),
        Pair("CHILLING_FAILURE", "❄️ Power Outage / Chilling Failure"),
        Pair("CALF_FEEDING_EXCESS", "🐄 Calf Excess Feeding"),
        Pair("EQUIPMENT_LEAK", "🛠️ Container / Vat Leakage")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = DangerRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Milk Loss / Spoilage Incident", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (errorMessage != null) {
                    Text(errorMessage ?: "", color = DangerRed, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }

                // 1. Quantity Lost in Liters
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it; errorMessage = null },
                    label = { Text("Quantity Wasted / Lost (Liters) *") },
                    placeholder = { Text("e.g. 5.5") },
                    leadingIcon = { Icon(Icons.Default.WaterDrop, contentDescription = null, tint = DangerRed) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 2. Incident Reason Dropdown / Selection
                Text("Incident Reason:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    reasons.forEach { (code, label) ->
                        val isSelected = selectedReason == code
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) DangerRed.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, if (isSelected) DangerRed else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedReason = code }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedReason = code },
                                    colors = RadioButtonDefaults.colors(selectedColor = DangerRed)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(label, fontSize = 11.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }

                // 3. Milk Type & Shift Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Milk Type
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Milk Species:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(
                                selected = selectedMilkType == "COW",
                                onClick = { selectedMilkType = "COW" },
                                label = { Text("🐄 Cow", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedMilkType == "BUFFALO",
                                onClick = { selectedMilkType = "BUFFALO" },
                                label = { Text("🐃 Buffalo", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Shift
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Shift:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(
                                selected = selectedShift == "MORNING",
                                onClick = { selectedShift = "MORNING" },
                                label = { Text("🌅 AM", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedShift == "EVENING",
                                onClick = { selectedShift = "EVENING" },
                                label = { Text("🌇 PM", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 4. Batch Source & Cost per Liter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = batchSource,
                        onValueChange = { batchSource = it },
                        label = { Text("Batch / Source") },
                        placeholder = { Text("e.g. Route #1") },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )

                    OutlinedTextField(
                        value = costPerLiterText,
                        onValueChange = { costPerLiterText = it },
                        label = { Text("Rate (₹/L)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                }

                // 5. Preventative Action
                OutlinedTextField(
                    value = preventativeAction,
                    onValueChange = { preventativeAction = it },
                    label = { Text("Action to Prevent Next Time (Optional)") },
                    placeholder = { Text("e.g. Clean chilling vat filter, reduce route buffer") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val q = quantityText.toDoubleOrNull()
                    val rate = costPerLiterText.toDoubleOrNull() ?: 50.0
                    if (q == null || q <= 0.0) {
                        errorMessage = "Please enter valid liters lost."
                    } else {
                        onSave(q, selectedReason, selectedMilkType, selectedShift, reasonDetails, rate, batchSource, preventativeAction)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
            ) {
                Text("Save Loss Record", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
