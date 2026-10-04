package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.DeliveryEntity
import com.example.data.local.entity.getQuickPresetsList
import com.example.data.repository.PricingEngine
import com.example.ui.MilkMateViewModel
import com.example.ui.theme.*
import com.example.ui.util.BusinessModeFeatures
import com.example.ui.util.loc
import com.example.ui.util.appStr
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryScreen(viewModel: MilkMateViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Screen State
    var screenMode by remember { mutableStateOf("LIST") } // "LIST", "STEP_BY_STEP"
    var activeViewMode by remember { mutableStateOf("ROUTE") } // "ROUTE" (Customer Cards), "TIMELINE" (Chronological Timeline)
    var activeCategory by remember { mutableStateOf("INDIVIDUAL") } // "INDIVIDUAL", "BULK_BUYER", "SUPPLIER"
    var selectedCustomerForStep by remember { mutableStateOf<CustomerEntity?>(null) }
    var currentCustomerIndex by remember { mutableIntStateOf(0) }
    var paymentCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var showBulkDeliverConfirmDialog by remember { mutableStateOf(false) }
    var deliveryToEdit by remember { mutableStateOf<DeliveryEntity?>(null) }

    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val selectedShift by viewModel.selectedShift.collectAsStateWithLifecycle()
    val deliveries by viewModel.currentShiftDeliveries.collectAsStateWithLifecycle()
    val todayDeliveries by viewModel.todayDeliveries.collectAsStateWithLifecycle()
    val allCustomers by viewModel.customers.collectAsStateWithLifecycle()
    val business by viewModel.currentBusiness.collectAsStateWithLifecycle()

    val accountStartDate = business?.accountStartDate ?: 0L
    val defaultCowRate = business?.cowMilkRate ?: 50.0
    val defaultBuffRate = business?.buffaloMilkRate ?: 65.0

    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val fullDateDisplay = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date(selectedDate))
    val shortDateDisplay = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(selectedDate))

    // Filter customers for active category
    val relevantCustomers = remember(allCustomers, activeCategory) {
        if (activeCategory == "SUPPLIER") {
            allCustomers.filter { it.type == "SUPPLIER" }
        } else {
            allCustomers.filter { it.type == activeCategory }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (screenMode) {
                "LIST" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        // Top View Mode Switcher: Route Overview vs Chronological Timeline Feed vs Volume Analytics
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (activeViewMode == "ROUTE") RoyalBluePrimary else Color.Transparent,
                                    modifier = Modifier.weight(1f).clickable { activeViewMode = "ROUTE" }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 7.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Map,
                                            contentDescription = null,
                                            tint = if (activeViewMode == "ROUTE") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Route",
                                            fontSize = 11.sp,
                                            fontWeight = if (activeViewMode == "ROUTE") FontWeight.Bold else FontWeight.Medium,
                                            color = if (activeViewMode == "ROUTE") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (activeViewMode == "TIMELINE") FreshGold else Color.Transparent,
                                    modifier = Modifier.weight(1.1f).clickable { activeViewMode = "TIMELINE" }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 7.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.History,
                                            contentDescription = null,
                                            tint = if (activeViewMode == "TIMELINE") Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Log (${todayDeliveries.size})",
                                            fontSize = 11.sp,
                                            fontWeight = if (activeViewMode == "TIMELINE") FontWeight.ExtraBold else FontWeight.Medium,
                                            color = if (activeViewMode == "TIMELINE") Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (activeViewMode == "ANALYTICS") DairyGreen else Color.Transparent,
                                    modifier = Modifier.weight(1.1f).clickable { activeViewMode = "ANALYTICS" }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 7.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.PieChart,
                                            contentDescription = null,
                                            tint = if (activeViewMode == "ANALYTICS") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Analytics",
                                            fontSize = 11.sp,
                                            fontWeight = if (activeViewMode == "ANALYTICS") FontWeight.Bold else FontWeight.Medium,
                                            color = if (activeViewMode == "ANALYTICS") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        when (activeViewMode) {
                            "ROUTE" -> {
                                DeliveryDashboardContent(
                                    viewModel = viewModel,
                                    activeCategory = activeCategory,
                                    onCategoryChange = { activeCategory = it },
                                    selectedDate = selectedDate,
                                    selectedShift = selectedShift,
                                    fullDateDisplay = fullDateDisplay,
                                    shortDateDisplay = shortDateDisplay,
                                    accountStartDate = accountStartDate,
                                    customers = relevantCustomers,
                                    allCustomers = allCustomers,
                                    deliveries = deliveries,
                                    defaultCowRate = defaultCowRate,
                                    defaultBuffRate = defaultBuffRate,
                                    onSelectDate = { viewModel.setDate(it) },
                                    onShiftChange = { viewModel.setShift(it) },
                                    onStartStepByStep = {
                                        if (relevantCustomers.isNotEmpty()) {
                                            val firstPendingIndex = relevantCustomers.indexOfFirst { cust ->
                                                val cowDone = deliveries.any {
                                                    it.customerId == cust.id && it.deliveryDate == selectedDate && it.shift == selectedShift &&
                                                            (it.milkType == "COW" || (cust.milkType == "COW" && it.milkType != "BUFFALO")) && it.isDelivered
                                                }
                                                val buffDone = deliveries.any {
                                                    it.customerId == cust.id && it.deliveryDate == selectedDate && it.shift == selectedShift &&
                                                            (it.milkType == "BUFFALO" || (cust.milkType == "BUFFALO" && it.milkType != "COW")) && it.isDelivered
                                                }
                                                when (cust.milkType) {
                                                    "COW" -> !cowDone
                                                    "BUFFALO" -> !buffDone
                                                    "BOTH" -> !(cowDone && buffDone)
                                                    else -> !(cowDone || buffDone)
                                                }
                                            }
                                            val targetIndex = if (firstPendingIndex >= 0) firstPendingIndex else 0
                                            selectedCustomerForStep = relevantCustomers[targetIndex]
                                            currentCustomerIndex = targetIndex
                                            screenMode = "STEP_BY_STEP"
                                        }
                                    },
                                    onOpenCustomerEntry = { cust, idx ->
                                        selectedCustomerForStep = cust
                                        currentCustomerIndex = idx
                                        screenMode = "STEP_BY_STEP"
                                    },
                                    onRecordPayment = { cust ->
                                        paymentCustomer = cust
                                    },
                                    onBulkDeliverClick = {
                                        showBulkDeliverConfirmDialog = true
                                    }
                                )
                            }
                            "TIMELINE" -> {
                                ChronologicalDeliveryTimelineView(
                                    todayDeliveries = todayDeliveries,
                                    selectedDate = selectedDate,
                                    selectedShift = selectedShift,
                                    shortDateDisplay = shortDateDisplay,
                                    onSelectDate = { dateVal: Long -> viewModel.setDate(dateVal) },
                                    onShiftChange = { shiftVal: String -> viewModel.setShift(shiftVal) },
                                    onEditDelivery = { del: DeliveryEntity -> deliveryToEdit = del },
                                    onDeleteDelivery = { del: DeliveryEntity ->
                                        viewModel.deleteDelivery(del.id)
                                        coroutineScope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "Removed delivery for ${del.customerName}",
                                                actionLabel = "UNDO",
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.restoreDelivery(del)
                                            }
                                        }
                                    }
                                )
                            }
                            "ANALYTICS" -> {
                                DailyMilkOutputAnalyticsView(
                                    todayDeliveries = todayDeliveries,
                                    selectedDate = selectedDate,
                                    selectedShift = selectedShift,
                                    shortDateDisplay = shortDateDisplay,
                                    fullDateDisplay = fullDateDisplay,
                                    accountStartDate = accountStartDate,
                                    businessName = business?.businessName,
                                    onSelectDate = { dateVal: Long -> viewModel.setDate(dateVal) }
                                )
                            }
                        }
                    }
                }

                "STEP_BY_STEP" -> {
                    val customer = selectedCustomerForStep
                    if (customer != null) {
                        val existingCowDelivery = deliveries.find {
                            it.customerId == customer.id && it.deliveryDate == selectedDate && it.shift == selectedShift &&
                                    (it.milkType == "COW" || (customer.milkType == "COW" && it.milkType != "BUFFALO"))
                        }
                        val existingBuffDelivery = deliveries.find {
                            it.customerId == customer.id && it.deliveryDate == selectedDate && it.shift == selectedShift &&
                                    (it.milkType == "BUFFALO" || (customer.milkType == "BUFFALO" && it.milkType != "COW"))
                        }

                        ModernStepDeliveryScreen(
                            customer = customer,
                            currentIndex = currentCustomerIndex + 1,
                            totalCount = relevantCustomers.size,
                            existingCowDelivery = existingCowDelivery,
                            existingBuffDelivery = existingBuffDelivery,
                            selectedDate = selectedDate,
                            selectedShift = selectedShift,
                            defaultCowRate = if (customer.cowRate > 0) customer.cowRate else (if (customer.rate > 0) customer.rate else defaultCowRate),
                            defaultBuffRate = if (customer.buffaloRate > 0) customer.buffaloRate else (if (customer.rate > 0) customer.rate else defaultBuffRate),
                            supportedMilkTypes = business?.supportedMilkTypes ?: "BOTH",
                            onBack = { screenMode = "LIST" },
                            onPrevious = {
                                if (currentCustomerIndex > 0) {
                                    currentCustomerIndex--
                                    selectedCustomerForStep = relevantCustomers[currentCustomerIndex]
                                }
                            },
                            onSkipToNext = {
                                if (currentCustomerIndex + 1 < relevantCustomers.size) {
                                    currentCustomerIndex++
                                    selectedCustomerForStep = relevantCustomers[currentCustomerIndex]
                                } else {
                                    screenMode = "LIST"
                                }
                            },
                            onDeleteDelivery = { deliveryId ->
                                viewModel.deleteDelivery(deliveryId)
                            },
                            onUpdatePresets = { newPresets ->
                                viewModel.updateCustomerQuickPresets(customer.id, newPresets)
                            },
                            onSaveDual = { includeCow, cowQty, cowRate, cowFat, cowSnf, cowClr, cowSkip,
                                          includeBuff, buffQty, buffRate, buffFat, buffSnf, buffClr, buffSkip ->
                                if (includeCow) {
                                    viewModel.saveDelivery(
                                        id = existingCowDelivery?.id,
                                        customerId = customer.id,
                                        customerName = customer.name,
                                        customerType = customer.type,
                                        date = selectedDate,
                                        shift = selectedShift,
                                        milkType = "COW",
                                        quantity = if (cowSkip) 0.0 else cowQty,
                                        fat = cowFat,
                                        snf = cowSnf,
                                        clr = cowClr,
                                        customRate = cowRate,
                                        isDelivered = !cowSkip,
                                        notes = if (cowSkip) "Skipped / Holiday" else "Delivered"
                                    )
                                } else if (existingCowDelivery != null) {
                                    viewModel.deleteDelivery(existingCowDelivery.id)
                                }

                                if (includeBuff) {
                                    viewModel.saveDelivery(
                                        id = existingBuffDelivery?.id,
                                        customerId = customer.id,
                                        customerName = customer.name,
                                        customerType = customer.type,
                                        date = selectedDate,
                                        shift = selectedShift,
                                        milkType = "BUFFALO",
                                        quantity = if (buffSkip) 0.0 else buffQty,
                                        fat = buffFat,
                                        snf = buffSnf,
                                        clr = buffClr,
                                        customRate = buffRate,
                                        isDelivered = !buffSkip,
                                        notes = if (buffSkip) "Skipped / Holiday" else "Delivered"
                                    )
                                } else if (existingBuffDelivery != null) {
                                    viewModel.deleteDelivery(existingBuffDelivery.id)
                                }

                                // Advance to next customer
                                if (currentCustomerIndex + 1 < relevantCustomers.size) {
                                    currentCustomerIndex++
                                    selectedCustomerForStep = relevantCustomers[currentCustomerIndex]
                                } else {
                                    screenMode = "LIST"
                                }
                            }
                        )
                    } else {
                        screenMode = "LIST"
                    }
                }
            }
        }
    }

    // Modal: Record Payment
    paymentCustomer?.let { cust ->
        RecordCustomerPaymentDialog(
            customer = cust,
            onDismiss = { paymentCustomer = null },
            onSavePayment = { amount, method, notes ->
                viewModel.savePayment(
                    customerId = cust.id,
                    customerName = cust.name,
                    paymentType = if (cust.type == "SUPPLIER") "SUPPLIER_PAYMENT" else "CUSTOMER_PAYMENT",
                    amount = amount,
                    method = method,
                    referenceNo = "",
                    notes = notes
                )
                paymentCustomer = null
            }
        )
    }

    // Modal: Edit Single Delivery
    deliveryToEdit?.let { del: DeliveryEntity ->
        EditSingleDeliveryDialog(
            delivery = del,
            onDismiss = { deliveryToEdit = null },
            onSave = { newQty: Double, newRate: Double, newFat: Double, newSnf: Double, newClr: Double, newNotes: String ->
                viewModel.saveDelivery(
                    id = del.id,
                    customerId = del.customerId,
                    customerName = del.customerName,
                    customerType = del.customerType,
                    date = del.deliveryDate,
                    shift = del.shift,
                    milkType = del.milkType,
                    quantity = newQty,
                    fat = newFat,
                    snf = newSnf,
                    clr = newClr,
                    customRate = newRate,
                    isDelivered = true,
                    notes = newNotes
                )
                deliveryToEdit = null
            },
            onDelete = {
                viewModel.deleteDelivery(del.id)
                deliveryToEdit = null
            }
        )
    }

    // Modal: Bulk Deliver All Pending Confirmation
    if (showBulkDeliverConfirmDialog) {
        val pendingCount = relevantCustomers.count { cust ->
            !deliveries.any { it.customerId == cust.id && it.deliveryDate == selectedDate && it.shift == selectedShift && it.isDelivered }
        }
        AlertDialog(
            onDismissRequest = { showBulkDeliverConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = FreshGold)
                    Text("Deliver All Pending?", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Text(
                    "This will mark all remaining $pendingCount pending customer(s) as Delivered using their default daily milk quantity and rate.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.bulkDeliverAllPending(selectedDate, selectedShift, relevantCustomers, deliveries)
                        showBulkDeliverConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DairyGreen)
                ) {
                    Text("Yes, Deliver All ($pendingCount)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeliverConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryDashboardContent(
    viewModel: MilkMateViewModel,
    activeCategory: String,
    onCategoryChange: (String) -> Unit,
    selectedDate: Long,
    selectedShift: String,
    fullDateDisplay: String,
    shortDateDisplay: String,
    accountStartDate: Long,
    customers: List<CustomerEntity>,
    allCustomers: List<CustomerEntity>,
    deliveries: List<DeliveryEntity>,
    defaultCowRate: Double,
    defaultBuffRate: Double,
    onSelectDate: (Long) -> Unit,
    onShiftChange: (String) -> Unit,
    onStartStepByStep: () -> Unit,
    onOpenCustomerEntry: (CustomerEntity, Int) -> Unit,
    onRecordPayment: (CustomerEntity) -> Unit,
    onBulkDeliverClick: () -> Unit
) {
    val context = LocalContext.current
    val business by viewModel.currentBusiness.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var selectedRouteFilter by remember { mutableStateOf("All Routes") }
    var selectedMilkTypeFilter by remember { mutableStateOf("ALL") } // "ALL", "COW", "BUFFALO", "BOTH"
    var selectedStatusFilter by remember { mutableStateOf("ALL") } // "ALL", "PENDING", "COMPLETED"

    val availableRoutes = remember(customers) {
        listOf("All Routes") + customers.mapNotNull { it.route.trim().ifBlank { null } }.distinct()
    }

    // Calculations
    val deliveredCount = customers.count { c ->
        deliveries.any { d -> d.customerId == c.id && d.deliveryDate == selectedDate && d.shift == selectedShift && d.isDelivered }
    }
    val pendingCount = customers.size - deliveredCount
    val completionPercent = if (customers.isNotEmpty()) (deliveredCount * 100) / customers.size else 0

    val shiftDeliveries = deliveries.filter { it.deliveryDate == selectedDate && it.shift == selectedShift && it.isDelivered }
    val totalLiters = shiftDeliveries.sumOf { it.quantityLiters }
    val cowLiters = shiftDeliveries.filter { it.milkType == "COW" }.sumOf { it.quantityLiters }
    val buffLiters = shiftDeliveries.filter { it.milkType == "BUFFALO" }.sumOf { it.quantityLiters }
    val totalAmount = shiftDeliveries.sumOf { it.totalAmount }

    val filteredCustomers = customers.filter { customer ->
        val matchesSearch = searchQuery.isBlank() ||
                customer.name.contains(searchQuery, ignoreCase = true) ||
                customer.mobile.contains(searchQuery) ||
                customer.route.contains(searchQuery, ignoreCase = true)

        val matchesRoute = selectedRouteFilter == "All Routes" || customer.route.trim().equals(selectedRouteFilter, ignoreCase = true)

        val matchesMilkType = when (selectedMilkTypeFilter) {
            "COW" -> customer.milkType == "COW" || customer.milkType == "BOTH"
            "BUFFALO" -> customer.milkType == "BUFFALO" || customer.milkType == "BOTH"
            "BOTH" -> customer.milkType == "BOTH"
            else -> true
        }

        val isDone = deliveries.any { d -> d.customerId == customer.id && d.deliveryDate == selectedDate && d.shift == selectedShift && d.isDelivered }
        val matchesStatus = when (selectedStatusFilter) {
            "PENDING" -> !isDone
            "COMPLETED" -> isDone
            else -> true
        }

        matchesSearch && matchesRoute && matchesMilkType && matchesStatus
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ================= HERO SESSION COCKPIT =================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (selectedShift == "MORNING") Color(0xFF1E3A8A) else Color(0xFF1E1B4B)
            ),
            elevation = CardDefaults.cardElevation(3.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Shift & Date Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shift Switcher Pill
                    Row(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp))
                            .padding(3.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedShift == "MORNING") FreshGold else Color.Transparent,
                            modifier = Modifier.clickable { onShiftChange("MORNING") }
                        ) {
                            Text(
                                text = "☀️ Morning",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedShift == "MORNING") Color.Black else Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedShift == "EVENING") Color(0xFF6366F1) else Color.Transparent,
                            modifier = Modifier.clickable { onShiftChange("EVENING") }
                        ) {
                            Text(
                                text = "🌙 Evening",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    // Date Picker Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp))
                            .clickable {
                                val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
                                val dp = DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        val newCal = Calendar.getInstance().apply { set(y, m, d, 12, 0, 0) }
                                        onSelectDate(newCal.timeInMillis)
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                )
                                if (accountStartDate > 0) dp.datePicker.minDate = accountStartDate
                                dp.datePicker.maxDate = System.currentTimeMillis()
                                dp.show()
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = shortDateDisplay, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                // Progress Bar and Liters Breakdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "$deliveredCount / ${customers.size} Completed",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        val supportedMilkTypes = business?.supportedMilkTypes ?: "BOTH"
                        val milkLitersBreakdown = when (supportedMilkTypes) {
                            "COW_ONLY" -> "🐄 ${String.format(Locale.US, "%.1f", cowLiters)}L Cow Milk"
                            "BUFFALO_ONLY" -> "🐃 ${String.format(Locale.US, "%.1f", buffLiters)}L Buffalo Milk"
                            else -> "🐄 ${String.format(Locale.US, "%.1f", cowLiters)}L Cow  •  🐃 ${String.format(Locale.US, "%.1f", buffLiters)}L Buff"
                        }
                        Text(
                            text = milkLitersBreakdown,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = String.format(Locale.US, "%.1f L", totalLiters),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = FreshGold
                        )
                        Text(
                            text = String.format(Locale.US, "₹%.0f Bill", totalAmount),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                // Progress Indicator
                LinearProgressIndicator(
                    progress = { if (customers.isNotEmpty()) deliveredCount.toFloat() / customers.size else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = FreshGold,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )

                // Quick Cockpit Actions
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onStartStepByStep,
                        colors = ButtonDefaults.buttonColors(containerColor = FreshGold, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(38.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (pendingCount > 0) "Deliver Route ($pendingCount left)" else "Review Route",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onBulkDeliverClick,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(Color.White.copy(0.4f), Color.White.copy(0.4f)))),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Deliver All", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    IconButton(
                        onClick = {
                            val bizMilkTypes = business?.supportedMilkTypes ?: "BOTH"
                            val msg = StringBuilder().apply {
                                append("🥛 *${business?.businessName ?: "Dairy"} Shift Summary*\n")
                                append("📅 Date: $shortDateDisplay ($selectedShift Shift)\n\n")
                                append("🚚 Completed: $deliveredCount / ${customers.size} (${completionPercent}%)\n")
                                if (bizMilkTypes != "BUFFALO_ONLY") {
                                    append("🐄 Cow Milk: ${String.format(Locale.US, "%.1f", cowLiters)} Liters\n")
                                }
                                if (bizMilkTypes != "COW_ONLY") {
                                    append("🐃 Buffalo Milk: ${String.format(Locale.US, "%.1f", buffLiters)} Liters\n")
                                }
                                append("🥛 Total Volume: ${String.format(Locale.US, "%.1f", totalLiters)} Liters\n")
                                append("💰 Total Shift Bill: ₹${totalAmount.toInt()}\n")
                            }.toString()

                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, msg)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Shift Summary"))
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp))
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share Summary", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // ================= CATEGORY TABS =================
        val currentMode = business?.businessMode ?: "FARMER"
        val isTrader = currentMode == "TRADER"
        val showSuppliers = BusinessModeFeatures.showCollectionDesk(business) || currentMode in listOf("TRADER", "COLLECTION_CENTER", "INTEGRATED", "CUSTOM")

        LaunchedEffect(showSuppliers) {
            if (!showSuppliers && activeCategory == "SUPPLIER") {
                onCategoryChange("INDIVIDUAL")
            }
        }

        val categoryTabs = buildList {
            if (currentMode != "COLLECTION_CENTER") {
                add(Triple("INDIVIDUAL", if (isTrader) "👤 Household Routes" else "👤 Retail", allCustomers.count { it.type == "INDIVIDUAL" }))
            }
            add(Triple("BULK_BUYER", if (isTrader) "🏪 Tea Stalls & B2B" else "📦 Bulk Buyer", allCustomers.count { it.type == "BULK_BUYER" }))
            if (showSuppliers) {
                add(Triple("SUPPLIER", "🥛 Farmers", allCustomers.count { it.type == "SUPPLIER" }))
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            categoryTabs.forEach { (catKey, catLabel, catCount) ->
                val isSelected = activeCategory == catKey
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) RoyalBluePrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onCategoryChange(catKey) }
                ) {
                    Text(
                        text = "$catLabel ($catCount)",
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 7.dp)
                    )
                }
            }
        }

        // ================= MULTI-FACET FILTER BAR =================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedStatusFilter == "ALL" && selectedMilkTypeFilter == "ALL",
                onClick = {
                    selectedStatusFilter = "ALL"
                    selectedMilkTypeFilter = "ALL"
                },
                label = { Text("All (${customers.size})", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedStatusFilter == "PENDING",
                onClick = { selectedStatusFilter = if (selectedStatusFilter == "PENDING") "ALL" else "PENDING" },
                label = { Text("⏳ Pending ($pendingCount)", fontSize = 11.sp, fontWeight = if (selectedStatusFilter == "PENDING") FontWeight.Bold else FontWeight.Normal) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = FreshGoldLight, selectedLabelColor = FreshGold)
            )
            FilterChip(
                selected = selectedStatusFilter == "COMPLETED",
                onClick = { selectedStatusFilter = if (selectedStatusFilter == "COMPLETED") "ALL" else "COMPLETED" },
                label = { Text("✓ Done ($deliveredCount)", fontSize = 11.sp, fontWeight = if (selectedStatusFilter == "COMPLETED") FontWeight.Bold else FontWeight.Normal) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = DairyGreenLight, selectedLabelColor = DairyGreen)
            )
            val supportedTypes = business?.supportedMilkTypes ?: "BOTH"
            if (supportedTypes == "COW_ONLY" || supportedTypes == "BOTH") {
                FilterChip(
                    selected = selectedMilkTypeFilter == "COW",
                    onClick = { selectedMilkTypeFilter = if (selectedMilkTypeFilter == "COW") "ALL" else "COW" },
                    label = { Text("🐄 Cow", fontSize = 11.sp) }
                )
            }
            if (supportedTypes == "BUFFALO_ONLY" || supportedTypes == "BOTH") {
                FilterChip(
                    selected = selectedMilkTypeFilter == "BUFFALO",
                    onClick = { selectedMilkTypeFilter = if (selectedMilkTypeFilter == "BUFFALO") "ALL" else "BUFFALO" },
                    label = { Text("🐃 Buff", fontSize = 11.sp) }
                )
            }
            if (supportedTypes == "BOTH") {
                FilterChip(
                    selected = selectedMilkTypeFilter == "BOTH",
                    onClick = { selectedMilkTypeFilter = if (selectedMilkTypeFilter == "BOTH") "ALL" else "BOTH" },
                    label = { Text("🔄 Dual", fontSize = 11.sp) }
                )
            }
        }

        // Route Filter (if multiple routes exist)
        if (availableRoutes.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                availableRoutes.forEach { routeName ->
                    FilterChip(
                        selected = selectedRouteFilter == routeName,
                        onClick = { selectedRouteFilter = routeName },
                        label = { Text(if (routeName == "All Routes") "📍 All Routes" else "📍 $routeName", fontSize = 11.sp) }
                    )
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by customer, route, or mobile...", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
            trailingIcon = if (searchQuery.isNotBlank()) {
                { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp)) } }
            } else null,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
        )

        // ================= CUSTOMER CARDS LIST =================
        if (filteredCustomers.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.PeopleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(44.dp))
                    Text("No records match this filter", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(filteredCustomers, key = { _, c -> c.id }) { index, customer ->
                    ModernCustomerDeliveryCard(
                        customer = customer,
                        deliveries = deliveries,
                        viewModel = viewModel,
                        selectedDate = selectedDate,
                        selectedShift = selectedShift,
                        defaultCowRate = defaultCowRate,
                        defaultBuffRate = defaultBuffRate,
                        onOpenDetail = { onOpenCustomerEntry(customer, index) },
                        onRecordPayment = { onRecordPayment(customer) }
                    )
                }
            }
        }
    }
}

@Composable
fun ModernCustomerDeliveryCard(
    customer: CustomerEntity,
    deliveries: List<DeliveryEntity>,
    viewModel: MilkMateViewModel,
    selectedDate: Long,
    selectedShift: String,
    defaultCowRate: Double,
    defaultBuffRate: Double,
    onOpenDetail: () -> Unit,
    onRecordPayment: () -> Unit
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showPresetsModal by remember { mutableStateOf(false) }

    val quickPresets = customer.getQuickPresetsList()

    if (showPresetsModal) {
        ManageCustomerPresetsModal(
            customer = customer,
            onDismiss = { showPresetsModal = false },
            onSavePresets = { newPresets ->
                viewModel.updateCustomerQuickPresets(customer.id, newPresets)
                showPresetsModal = false
            }
        )
    }

    val customerDeliveries by viewModel.getCustomerDeliveriesFlow(customer.id).collectAsStateWithLifecycle(emptyList())
    val currentBiz by viewModel.currentBusiness.collectAsStateWithLifecycle()
    val supportedMilkTypes = currentBiz?.supportedMilkTypes ?: "BOTH"

    // Lookups for deliveries for this customer on selectedDate and selectedShift
    val cowDelivery = deliveries.find {
        it.customerId == customer.id && it.deliveryDate == selectedDate && it.shift == selectedShift &&
                (it.milkType == "COW" || (customer.milkType == "COW" && it.milkType != "BUFFALO"))
    }
    val buffDelivery = deliveries.find {
        it.customerId == customer.id && it.deliveryDate == selectedDate && it.shift == selectedShift &&
                (it.milkType == "BUFFALO" || (customer.milkType == "BUFFALO" && it.milkType != "COW"))
    }

    val isCowDelivered = cowDelivery?.isDelivered == true
    val isBuffDelivered = buffDelivery?.isDelivered == true

    val cowRate = if (customer.cowRate > 0) customer.cowRate else (if (customer.rate > 0) customer.rate else defaultCowRate)
    val buffRate = if (customer.buffaloRate > 0) customer.buffaloRate else (if (customer.rate > 0) customer.rate else defaultBuffRate)

    val currentCowQty = cowDelivery?.quantityLiters ?: if (customer.cowQuantity > 0) customer.cowQuantity else customer.defaultQuantity
    val currentBuffQty = buffDelivery?.quantityLiters ?: if (customer.buffaloQuantity > 0) customer.buffaloQuantity else customer.defaultQuantity

    val cowTotal = if (isCowDelivered) (cowDelivery?.totalAmount ?: (currentCowQty * cowRate)) else 0.0
    val buffTotal = if (isBuffDelivered) (buffDelivery?.totalAmount ?: (currentBuffQty * buffRate)) else 0.0
    val totalRecordedAmount = cowTotal + buffTotal

    val isDeliveredOverall = when (customer.milkType) {
        "COW" -> isCowDelivered
        "BUFFALO" -> isBuffDelivered
        "BOTH" -> isCowDelivered && isBuffDelivered
        else -> isCowDelivered || isBuffDelivered
    }

    val isPartiallyDelivered = customer.milkType == "BOTH" && (isCowDelivered != isBuffDelivered)

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = DangerRed)
                    Text("Delete Delivery Record?", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Text(
                    "Delete today's ($selectedShift shift) recorded delivery for ${customer.name}? This will reset status back to Pending and adjust customer balance.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (cowDelivery != null && buffDelivery != null) {
                        Button(
                            onClick = {
                                viewModel.deleteDelivery(cowDelivery.id)
                                viewModel.deleteDelivery(buffDelivery.id)
                                showDeleteConfirmDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Delete Both (Cow + Buff)")
                        }
                    } else {
                        Button(
                            onClick = {
                                if (cowDelivery != null) viewModel.deleteDelivery(cowDelivery.id)
                                if (buffDelivery != null) viewModel.deleteDelivery(buffDelivery.id)
                                showDeleteConfirmDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                        ) {
                            Text("Yes, Delete Record")
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDeliveredOverall) 1.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header Row: Status Icon, Name, Milk Type, Amount Pill, Trash Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                when {
                                    isDeliveredOverall -> DairyGreenLight
                                    isPartiallyDelivered -> FreshGoldLight
                                    else -> RoyalBlueLight
                                },
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isDeliveredOverall -> Icons.Default.Check
                                isPartiallyDelivered -> Icons.Default.HourglassTop
                                else -> Icons.Default.WaterDrop
                            },
                            contentDescription = null,
                            tint = when {
                                isDeliveredOverall -> DairyGreen
                                isPartiallyDelivered -> FreshGold
                                else -> RoyalBluePrimary
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = customer.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = when {
                                    supportedMilkTypes == "COW_ONLY" -> "🐄 Cow (${customer.cowQuantity}L)"
                                    supportedMilkTypes == "BUFFALO_ONLY" -> "🐃 Buff (${customer.buffaloQuantity}L)"
                                    customer.milkType == "COW" -> "🐄 Cow (${customer.cowQuantity}L)"
                                    customer.milkType == "BUFFALO" -> "🐃 Buff (${customer.buffaloQuantity}L)"
                                    customer.milkType == "BOTH" -> "🥛 Dual (${customer.cowQuantity}L Cow + ${customer.buffaloQuantity}L Buff)"
                                    else -> "🥛 ${customer.milkType}"
                                },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (customer.route.isNotBlank()) {
                                Text(text = "• 📍 ${customer.route}", fontSize = 10.sp, color = RoyalBluePrimary)
                            }
                        }
                    }
                }

                // Amount / Status Pill & Trash
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            isDeliveredOverall -> DairyGreenLight
                            isPartiallyDelivered -> FreshGoldLight
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = if (isDeliveredOverall || isPartiallyDelivered) String.format(Locale.US, "₹%.0f", totalRecordedAmount) else "Pending",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = when {
                                isDeliveredOverall -> DairyGreen
                                isPartiallyDelivered -> FreshGold
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    if (cowDelivery != null || buffDelivery != null) {
                        IconButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Delivery", tint = DangerRed, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

            // ================= INLINE MILK CONTROLS =================
            // Cow Control (if Cow or Both)
            if (supportedMilkTypes != "BUFFALO_ONLY" && (customer.milkType == "COW" || customer.milkType == "BOTH")) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isCowDelivered) DairyGreen.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("🐄 Cow", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(
                            text = "₹${cowRate.toInt()}/L",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Stepper
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (cowDelivery != null) {
                                    viewModel.quickAdjustQuantity(cowDelivery, -0.5)
                                } else {
                                    val newQty = (currentCowQty - 0.5).coerceAtLeast(0.25)
                                    viewModel.saveDelivery(
                                        id = null, customerId = customer.id, customerName = customer.name, customerType = customer.type,
                                        date = selectedDate, shift = selectedShift, milkType = "COW", quantity = newQty,
                                        fat = 0.0, snf = 0.0, clr = 0.0, customRate = cowRate, isDelivered = true, notes = "Delivered"
                                    )
                                }
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Text("−", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        Text(
                            text = String.format(Locale.US, "%.1f L", currentCowQty),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )

                        IconButton(
                            onClick = {
                                if (cowDelivery != null) {
                                    viewModel.quickAdjustQuantity(cowDelivery, 0.5)
                                } else {
                                    val newQty = currentCowQty + 0.5
                                    viewModel.saveDelivery(
                                        id = null, customerId = customer.id, customerName = customer.name, customerType = customer.type,
                                        date = selectedDate, shift = selectedShift, milkType = "COW", quantity = newQty,
                                        fat = 0.0, snf = 0.0, clr = 0.0, customRate = cowRate, isDelivered = true, notes = "Delivered"
                                    )
                                }
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Text("+", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = RoyalBluePrimary)
                        }
                    }

                    // 1-Tap Toggle Cow Deliver Button
                    Button(
                        onClick = {
                            if (cowDelivery != null) {
                                viewModel.quickToggleDelivery(cowDelivery)
                            } else {
                                viewModel.saveDelivery(
                                    id = null, customerId = customer.id, customerName = customer.name, customerType = customer.type,
                                    date = selectedDate, shift = selectedShift, milkType = "COW", quantity = currentCowQty,
                                    fat = 0.0, snf = 0.0, clr = 0.0, customRate = cowRate, isDelivered = true, notes = "Delivered"
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCowDelivered) DairyGreen else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(if (isCowDelivered) "✓ Done" else "Deliver", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Cow Quick Add Presets Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.clickable { showPresetsModal = true }
                    ) {
                        Icon(Icons.Default.FlashOn, contentDescription = null, tint = FreshGold, modifier = Modifier.size(12.dp))
                        Text("Quick Add:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                    }
                    quickPresets.forEach { presetQty ->
                        val isCurrent = isCowDelivered && Math.abs(currentCowQty - presetQty) < 0.01
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isCurrent) DairyGreen else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable {
                                viewModel.saveDelivery(
                                    id = cowDelivery?.id,
                                    customerId = customer.id,
                                    customerName = customer.name,
                                    customerType = customer.type,
                                    date = selectedDate,
                                    shift = selectedShift,
                                    milkType = "COW",
                                    quantity = presetQty,
                                    fat = 0.0, snf = 0.0, clr = 0.0,
                                    customRate = cowRate,
                                    isDelivered = true,
                                    notes = "Quick Add ${presetQty}L"
                                )
                            }
                        ) {
                            Text(
                                text = "${if (presetQty % 1.0 == 0.0) presetQty.toInt().toString() else presetQty.toString()}L",
                                fontSize = 10.sp,
                                fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { showPresetsModal = true },
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = "Edit Quick Presets", tint = RoyalBluePrimary, modifier = Modifier.size(13.dp))
                    }
                }
            }

            // Buffalo Control (if Buffalo or Both)
            if (supportedMilkTypes != "COW_ONLY" && (customer.milkType == "BUFFALO" || customer.milkType == "BOTH")) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isBuffDelivered) DairyGreen.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("🐃 Buff", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(
                            text = "₹${buffRate.toInt()}/L",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Stepper
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (buffDelivery != null) {
                                    viewModel.quickAdjustQuantity(buffDelivery, -0.5)
                                } else {
                                    val newQty = (currentBuffQty - 0.5).coerceAtLeast(0.25)
                                    viewModel.saveDelivery(
                                        id = null, customerId = customer.id, customerName = customer.name, customerType = customer.type,
                                        date = selectedDate, shift = selectedShift, milkType = "BUFFALO", quantity = newQty,
                                        fat = 0.0, snf = 0.0, clr = 0.0, customRate = buffRate, isDelivered = true, notes = "Delivered"
                                    )
                                }
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Text("−", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        Text(
                            text = String.format(Locale.US, "%.1f L", currentBuffQty),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )

                        IconButton(
                            onClick = {
                                if (buffDelivery != null) {
                                    viewModel.quickAdjustQuantity(buffDelivery, 0.5)
                                } else {
                                    val newQty = currentBuffQty + 0.5
                                    viewModel.saveDelivery(
                                        id = null, customerId = customer.id, customerName = customer.name, customerType = customer.type,
                                        date = selectedDate, shift = selectedShift, milkType = "BUFFALO", quantity = newQty,
                                        fat = 0.0, snf = 0.0, clr = 0.0, customRate = buffRate, isDelivered = true, notes = "Delivered"
                                    )
                                }
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Text("+", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = RoyalBluePrimary)
                        }
                    }

                    // 1-Tap Toggle Buff Deliver Button
                    Button(
                        onClick = {
                            if (buffDelivery != null) {
                                viewModel.quickToggleDelivery(buffDelivery)
                            } else {
                                viewModel.saveDelivery(
                                    id = null, customerId = customer.id, customerName = customer.name, customerType = customer.type,
                                    date = selectedDate, shift = selectedShift, milkType = "BUFFALO", quantity = currentBuffQty,
                                    fat = 0.0, snf = 0.0, clr = 0.0, customRate = buffRate, isDelivered = true, notes = "Delivered"
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isBuffDelivered) DairyGreen else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(if (isBuffDelivered) "✓ Done" else "Deliver", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Buffalo Quick Add Presets Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.clickable { showPresetsModal = true }
                    ) {
                        Icon(Icons.Default.FlashOn, contentDescription = null, tint = FreshGold, modifier = Modifier.size(12.dp))
                        Text("Quick Add:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6D4C41))
                    }
                    quickPresets.forEach { presetQty ->
                        val isCurrent = isBuffDelivered && Math.abs(currentBuffQty - presetQty) < 0.01
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isCurrent) DairyGreen else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable {
                                viewModel.saveDelivery(
                                    id = buffDelivery?.id,
                                    customerId = customer.id,
                                    customerName = customer.name,
                                    customerType = customer.type,
                                    date = selectedDate,
                                    shift = selectedShift,
                                    milkType = "BUFFALO",
                                    quantity = presetQty,
                                    fat = 0.0, snf = 0.0, clr = 0.0,
                                    customRate = buffRate,
                                    isDelivered = true,
                                    notes = "Quick Add ${presetQty}L"
                                )
                            }
                        ) {
                            Text(
                                text = "${if (presetQty % 1.0 == 0.0) presetQty.toInt().toString() else presetQty.toString()}L",
                                fontSize = 10.sp,
                                fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { showPresetsModal = true },
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = "Edit Quick Presets", tint = RoyalBluePrimary, modifier = Modifier.size(13.dp))
                    }
                }
            }

            // Quick Actions Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Balance Tag
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Due:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = String.format(Locale.US, "₹%.0f", customer.outstandingBalance),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (customer.outstandingBalance > 0) DangerRed else DairyGreen
                    )
                }

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Fast Absent / 0L
                    OutlinedButton(
                        onClick = {
                            if (supportedMilkTypes != "BUFFALO_ONLY" && (customer.milkType == "COW" || customer.milkType == "BOTH")) {
                                viewModel.saveDelivery(
                                    id = cowDelivery?.id, customerId = customer.id, customerName = customer.name, customerType = customer.type,
                                    date = selectedDate, shift = selectedShift, milkType = "COW", quantity = 0.0,
                                    fat = 0.0, snf = 0.0, clr = 0.0, customRate = cowRate, isDelivered = false, notes = "Absent / Holiday"
                                )
                            }
                            if (supportedMilkTypes != "COW_ONLY" && (customer.milkType == "BUFFALO" || customer.milkType == "BOTH")) {
                                viewModel.saveDelivery(
                                    id = buffDelivery?.id, customerId = customer.id, customerName = customer.name, customerType = customer.type,
                                    date = selectedDate, shift = selectedShift, milkType = "BUFFALO", quantity = 0.0,
                                    fat = 0.0, snf = 0.0, clr = 0.0, customRate = buffRate, isDelivered = false, notes = "Absent / Holiday"
                                )
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("🏖️ Absent", fontSize = 10.sp)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Collect Payment
                    OutlinedButton(
                        onClick = onRecordPayment,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("💰 Pay", fontSize = 10.sp)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Open Full Detail Modal / Testing
                    Button(
                        onClick = onOpenDetail,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Details 📝", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    // Expand / Collapse history
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Expanded Ledger History & Quick Contacts
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Contact Buttons Row
                    if (customer.mobile.isNotBlank()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.mobile}"))
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(34.dp)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Call ${customer.mobile}", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    val cleanNum = customer.mobile.replace("+", "").replace(" ", "").trim()
                                    val msg = "Namaste ${customer.name}, your balance with us is ₹${customer.outstandingBalance.toInt()}."
                                    val waIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$cleanNum?text=${Uri.encode(msg)}"))
                                    context.startActivity(waIntent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(34.dp)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("WhatsApp", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }

                    // Past Deliveries History
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🥛 Past Delivery History", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = RoyalBluePrimary)
                                val totalLitersPast = customerDeliveries.filter { it.isDelivered }.sumOf { it.quantityLiters }
                                Text("Total: ${String.format(Locale.US, "%.1f", totalLitersPast)}L", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DairyGreen)
                            }

                            if (customerDeliveries.isNotEmpty()) {
                                customerDeliveries.take(6).forEach { d ->
                                    val dateStr = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(d.deliveryDate))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "$dateStr (${d.shift.take(1)}) • ${if (d.milkType == "COW") "🐄 Cow" else "🐃 Buff"}",
                                            fontSize = 11.sp
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = "${d.quantityLiters}L • ₹${d.totalAmount.toInt()}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(3.dp),
                                                color = if (d.isDelivered) DairyGreen.copy(alpha = 0.15f) else DangerRed.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = if (d.isDelivered) "✓" else "Absent",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (d.isDelivered) DairyGreen else DangerRed,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }

                                            // Quick delete button for individual history row
                                            IconButton(
                                                onClick = { viewModel.deleteDelivery(d.id) },
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Delete this delivery record",
                                                    tint = DangerRed.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                Text("No delivery history logged yet", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernStepDeliveryScreen(
    customer: CustomerEntity,
    currentIndex: Int,
    totalCount: Int,
    existingCowDelivery: DeliveryEntity?,
    existingBuffDelivery: DeliveryEntity?,
    selectedDate: Long,
    selectedShift: String,
    defaultCowRate: Double,
    defaultBuffRate: Double,
    onBack: () -> Unit,
    onPrevious: () -> Unit,
    onSkipToNext: () -> Unit,
    onDeleteDelivery: (String) -> Unit,
    onUpdatePresets: (String) -> Unit = {},
    supportedMilkTypes: String = "BOTH",
    onSaveDual: (
        includeCow: Boolean,
        cowQty: Double,
        cowRate: Double,
        cowFat: Double,
        cowSnf: Double,
        cowClr: Double,
        cowSkip: Boolean,
        includeBuff: Boolean,
        buffQty: Double,
        buffRate: Double,
        buffFat: Double,
        buffSnf: Double,
        buffClr: Double,
        buffSkip: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    val isCowAlreadyDone = existingCowDelivery?.isDelivered == true
    val isBuffAlreadyDone = existingBuffDelivery?.isDelivered == true
    val isAnyAlreadyDone = isCowAlreadyDone || isBuffAlreadyDone

    var showStepPresetsModal by remember { mutableStateOf(false) }
    val quickPresets = customer.getQuickPresetsList()

    if (showStepPresetsModal) {
        ManageCustomerPresetsModal(
            customer = customer,
            onDismiss = { showStepPresetsModal = false },
            onSavePresets = { newPresets ->
                onUpdatePresets(newPresets)
                showStepPresetsModal = false
            }
        )
    }

    var includeCow by remember {
        mutableStateOf(supportedMilkTypes != "BUFFALO_ONLY" && (customer.milkType == "COW" || customer.milkType == "BOTH" || existingCowDelivery != null))
    }
    var includeBuff by remember {
        mutableStateOf(supportedMilkTypes != "COW_ONLY" && (customer.milkType == "BUFFALO" || customer.milkType == "BOTH" || existingBuffDelivery != null))
    }

    var cowQuantityText by remember {
        mutableStateOf(
            existingCowDelivery?.quantityLiters?.toString()
                ?: if (customer.cowQuantity > 0) customer.cowQuantity.toString() else "1.0"
        )
    }
    var cowRateText by remember {
        mutableStateOf(
            existingCowDelivery?.ratePerLiter?.toString()
                ?: (if (customer.cowRate > 0) customer.cowRate else defaultCowRate).toString()
        )
    }
    var cowFatText by remember {
        mutableStateOf(if ((existingCowDelivery?.fat ?: 0.0) > 0) existingCowDelivery!!.fat.toString() else "4.0")
    }
    var cowSnfText by remember {
        mutableStateOf(if ((existingCowDelivery?.snf ?: 0.0) > 0) existingCowDelivery!!.snf.toString() else "8.5")
    }
    var cowClrText by remember {
        mutableStateOf(if ((existingCowDelivery?.clr ?: 0.0) > 0) existingCowDelivery!!.clr.toString() else "28")
    }

    var buffQuantityText by remember {
        mutableStateOf(
            existingBuffDelivery?.quantityLiters?.toString()
                ?: if (customer.buffaloQuantity > 0) customer.buffaloQuantity.toString() else "1.0"
        )
    }
    var buffRateText by remember {
        mutableStateOf(
            existingBuffDelivery?.ratePerLiter?.toString()
                ?: (if (customer.buffaloRate > 0) customer.buffaloRate else defaultBuffRate).toString()
        )
    }
    var buffFatText by remember {
        mutableStateOf(if ((existingBuffDelivery?.fat ?: 0.0) > 0) existingBuffDelivery!!.fat.toString() else "6.5")
    }
    var buffSnfText by remember {
        mutableStateOf(if ((existingBuffDelivery?.snf ?: 0.0) > 0) existingBuffDelivery!!.snf.toString() else "9.0")
    }
    var buffClrText by remember {
        mutableStateOf(if ((existingBuffDelivery?.clr ?: 0.0) > 0) existingBuffDelivery!!.clr.toString() else "30")
    }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val cowQty = cowQuantityText.toDoubleOrNull() ?: 0.0
    val cowRate = cowRateText.toDoubleOrNull() ?: 0.0
    val cowFat = cowFatText.toDoubleOrNull() ?: 0.0
    val cowSnf = cowSnfText.toDoubleOrNull() ?: 0.0
    val cowClr = cowClrText.toDoubleOrNull() ?: 0.0
    val cowAmount = if (includeCow) cowQty * cowRate else 0.0

    val buffQty = buffQuantityText.toDoubleOrNull() ?: 0.0
    val buffRate = buffRateText.toDoubleOrNull() ?: 0.0
    val buffFat = buffFatText.toDoubleOrNull() ?: 0.0
    val buffSnf = buffSnfText.toDoubleOrNull() ?: 0.0
    val buffClr = buffClrText.toDoubleOrNull() ?: 0.0
    val buffAmount = if (includeBuff) buffQty * buffRate else 0.0

    val totalLiters = (if (includeCow) cowQty else 0.0) + (if (includeBuff) buffQty else 0.0)
    val totalAmount = cowAmount + buffAmount

    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = DangerRed)
                    Text("Delete Delivery Record?", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Text(
                    "Are you sure you want to remove this delivery for ${customer.name}? This will reset today's ($selectedShift) delivery status to Pending and adjust balance.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (existingCowDelivery != null) onDeleteDelivery(existingCowDelivery.id)
                        if (existingBuffDelivery != null) onDeleteDelivery(existingBuffDelivery.id)
                        showDeleteConfirmDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Yes, Delete Record")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Route Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = customer.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showStepPresetsModal = true }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.FlashOn, contentDescription = "Edit Quick Presets", tint = FreshGold, modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(
                        text = "📍 ${customer.route.ifBlank { "Main Route" }} • ${customer.type.replace("_", " ")}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = RoyalBluePrimary.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "$currentIndex / $totalCount",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = RoyalBluePrimary
                )
            }
        }

        // Status Card: Already Recorded vs Pending Alert
        if (isAnyAlreadyDone) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DairyGreen.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(18.dp))
                            Text("Already Recorded for $selectedShift Shift", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DairyGreen)
                        }

                        IconButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Record", tint = DangerRed, modifier = Modifier.size(18.dp))
                        }
                    }

                    Text(
                        text = "Saved: " + listOfNotNull(
                            if (existingCowDelivery != null) "🐄 Cow: ${existingCowDelivery.quantityLiters}L (₹${existingCowDelivery.totalAmount.toInt()})" else null,
                            if (existingBuffDelivery != null) "🐃 Buff: ${existingBuffDelivery.quantityLiters}L (₹${existingBuffDelivery.totalAmount.toInt()})" else null
                        ).joinToString(" + "),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onSkipToNext,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            Text("⏭️ Keep & Next", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showDeleteConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed.copy(alpha = 0.12f), contentColor = DangerRed),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            Text("🗑️ Delete Record", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⏳ $selectedShift Shift Delivery (${dateFormat.format(Date(selectedDate))})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = RoyalBluePrimary
                    )
                    TextButton(onClick = onSkipToNext, contentPadding = PaddingValues(0.dp)) {
                        Text("Skip ⏭️", fontSize = 11.sp)
                    }
                }
            }
        }

        // ================= SECTION 1: COW MILK =================
        if (supportedMilkTypes != "BUFFALO_ONLY") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (includeCow) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                elevation = if (includeCow) CardDefaults.cardElevation(2.dp) else CardDefaults.cardElevation(0.dp)
            ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Checkbox(
                            checked = includeCow,
                            onCheckedChange = { includeCow = it }
                        )
                        Column {
                            Text("🐄 Cow Milk", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (includeCow && cowQty > 0) {
                                Text(
                                    text = String.format(Locale.US, "%.1f L @ ₹%.1f = ₹%.2f", cowQty, cowRate, cowAmount),
                                    fontSize = 11.sp,
                                    color = RoyalBluePrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    if (isCowAlreadyDone) {
                        Surface(shape = RoundedCornerShape(6.dp), color = DairyGreenLight) {
                            Text("✓ Saved", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DairyGreen, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }

                if (includeCow) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = cowQuantityText,
                            onValueChange = { cowQuantityText = it },
                            label = { Text("Cow Qty (L)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1.2f)
                        )
                        OutlinedTextField(
                            value = cowRateText,
                            onValueChange = { cowRateText = it },
                            label = { Text("Rate (₹/L)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Cow Quick Qty Chips
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚡ Quick Add:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                        quickPresets.forEach { p ->
                            val pStr = if (p % 1.0 == 0.0) p.toInt().toString() else p.toString()
                            val isSelected = cowQuantityText == pStr || (cowQuantityText.toDoubleOrNull() == p)
                            FilterChip(
                                selected = isSelected,
                                onClick = { cowQuantityText = pStr },
                                label = { Text("${pStr}L", fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                            )
                        }
                        IconButton(onClick = { showStepPresetsModal = true }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Tune, contentDescription = "Edit Presets", tint = RoyalBluePrimary, modifier = Modifier.size(14.dp))
                        }
                    }

                    // Supplier FAT/SNF testing for Cow
                    if (customer.type == "SUPPLIER") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = cowFatText,
                                onValueChange = { cowFatText = it },
                                label = { Text("FAT %") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = cowSnfText,
                                onValueChange = { cowSnfText = it },
                                label = { Text("SNF %") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = cowClrText,
                                onValueChange = {
                                    cowClrText = it
                                    val c = it.toDoubleOrNull() ?: 0.0
                                    val f = cowFatText.toDoubleOrNull() ?: 0.0
                                    if (c > 0 && f > 0) {
                                        cowSnfText = String.format(Locale.US, "%.1f", (c / 4.0) + (0.21 * f) + 0.36)
                                    }
                                },
                                label = { Text("CLR") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }

        // ================= SECTION 2: BUFFALO MILK =================
        if (supportedMilkTypes != "COW_ONLY") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (includeBuff) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                elevation = if (includeBuff) CardDefaults.cardElevation(2.dp) else CardDefaults.cardElevation(0.dp)
            ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Checkbox(
                            checked = includeBuff,
                            onCheckedChange = { includeBuff = it }
                        )
                        Column {
                            Text("🐃 Buffalo Milk", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (includeBuff && buffQty > 0) {
                                Text(
                                    text = String.format(Locale.US, "%.1f L @ ₹%.1f = ₹%.2f", buffQty, buffRate, buffAmount),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    if (isBuffAlreadyDone) {
                        Surface(shape = RoundedCornerShape(6.dp), color = DairyGreenLight) {
                            Text("✓ Saved", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DairyGreen, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }

                if (includeBuff) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = buffQuantityText,
                            onValueChange = { buffQuantityText = it },
                            label = { Text("Buff Qty (L)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1.2f)
                        )
                        OutlinedTextField(
                            value = buffRateText,
                            onValueChange = { buffRateText = it },
                            label = { Text("Rate (₹/L)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Buffalo Quick Qty Chips
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚡ Quick Add:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        quickPresets.forEach { p ->
                            val pStr = if (p % 1.0 == 0.0) p.toInt().toString() else p.toString()
                            val isSelected = buffQuantityText == pStr || (buffQuantityText.toDoubleOrNull() == p)
                            FilterChip(
                                selected = isSelected,
                                onClick = { buffQuantityText = pStr },
                                label = { Text("${pStr}L", fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                            )
                        }
                        IconButton(onClick = { showStepPresetsModal = true }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Tune, contentDescription = "Edit Presets", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                        }
                    }

                    // Supplier FAT/SNF testing for Buffalo
                    if (customer.type == "SUPPLIER") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = buffFatText,
                                onValueChange = { buffFatText = it },
                                label = { Text("FAT %") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = buffSnfText,
                                onValueChange = { buffSnfText = it },
                                label = { Text("SNF %") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = buffClrText,
                                onValueChange = {
                                    buffClrText = it
                                    val c = it.toDoubleOrNull() ?: 0.0
                                    val f = buffFatText.toDoubleOrNull() ?: 0.0
                                    if (c > 0 && f > 0) {
                                        buffSnfText = String.format(Locale.US, "%.1f", (c / 4.0) + (0.21 * f) + 0.36)
                                    }
                                },
                                label = { Text("CLR") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }

        // ================= TOTAL SUMMARY BANNER =================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (customer.type == "SUPPLIER") "Total Farmer Milk" else "Total Delivery Volume",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f Liters", totalLiters),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (customer.type == "SUPPLIER") "Total Payout" else "Session Bill",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.US, "₹%.2f", totalAmount),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = RoyalBluePrimary
                    )
                }
            }
        }

        // ================= BOTTOM STICKY ACTION BUTTONS =================
        Button(
            onClick = {
                onSaveDual(
                    includeCow,
                    cowQty,
                    cowRate,
                    cowFat,
                    cowSnf,
                    cowClr,
                    false,
                    includeBuff,
                    buffQty,
                    buffRate,
                    buffFat,
                    buffSnf,
                    buffClr,
                    false
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = includeCow || includeBuff
        ) {
            Text(
                if (isAnyAlreadyDone) "✓ Update Entry & Next Customer →" else "✓ Save Delivery & Next Customer →",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onPrevious,
                enabled = currentIndex > 1,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("◀ Prev", fontSize = 11.sp)
            }

            OutlinedButton(
                onClick = {
                    onSaveDual(
                        includeCow,
                        0.0,
                        cowRate,
                        cowFat,
                        cowSnf,
                        cowClr,
                        true,
                        includeBuff,
                        0.0,
                        buffRate,
                        buffFat,
                        buffSnf,
                        buffClr,
                        true
                    )
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1.2f)
            ) {
                Text("🏖️ Absent (0L)", fontSize = 11.sp)
            }

            OutlinedButton(
                onClick = onSkipToNext,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Skip ▶", fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun RecordCustomerPaymentDialog(
    customer: CustomerEntity,
    onDismiss: () -> Unit,
    onSavePayment: (Double, String, String) -> Unit
) {
    var amountText by remember {
        mutableStateOf(if (customer.outstandingBalance > 0) customer.outstandingBalance.toInt().toString() else "500")
    }
    var selectedMethod by remember { mutableStateOf("CASH") }
    var notesText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Collect Payment", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Due: ₹${customer.outstandingBalance.toInt()}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (customer.outstandingBalance > 0) DangerRed else DairyGreen
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Customer: ${customer.name}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Payment Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Amount Chips
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("100", "200", "500", "1000").forEach { p ->
                        FilterChip(
                            selected = amountText == p,
                            onClick = { amountText = p },
                            label = { Text("₹$p", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Payment Method Chips
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("CASH" to "💵 Cash", "UPI" to "📱 UPI", "BANK" to "🏦 Bank").forEach { (method, label) ->
                        FilterChip(
                            selected = selectedMethod == method,
                            onClick = { selectedMethod = method },
                            label = { Text(label, fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Note / Transaction ID (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onSavePayment(amt, selectedMethod, notesText)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0.0
            ) {
                Text("✓ Save Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ShiftPill(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) RoyalBluePrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier.clickable { onClick() }
    ) {
        Text(
            text = title.loc(),
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManageCustomerPresetsModal(
    customer: CustomerEntity,
    onDismiss: () -> Unit,
    onSavePresets: (String) -> Unit
) {
    var presetsList by remember {
        mutableStateOf(customer.getQuickPresetsList())
    }
    var customQtyText by remember { mutableStateOf("") }
    val commonQuantities = listOf(0.25, 0.5, 0.75, 1.0, 1.5, 2.0, 2.5, 3.0, 4.0, 5.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = FreshGold.copy(alpha = 0.2f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = FreshGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Quick Add Presets",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Customer: ${customer.name}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Configure frequently delivered milk quantities (in Liters) for ${customer.name}. These will appear as 1-tap quick buttons on the delivery sheet.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )

                // Current Active Presets
                Text(
                    text = "ACTIVE PRESETS (TAP ✕ TO REMOVE)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoyalBluePrimary
                )

                if (presetsList.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No presets configured. Select from common quantities below or add custom.",
                            fontSize = 11.sp,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetsList.forEach { qty ->
                            val label = if (qty % 1.0 == 0.0) "${qty.toInt()}L" else "${qty}L"
                            InputChip(
                                selected = true,
                                onClick = {
                                    presetsList = presetsList.filter { it != qty }
                                },
                                label = { Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove",
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                colors = InputChipDefaults.inputChipColors(
                                    selectedContainerColor = RoyalBluePrimary.copy(alpha = 0.15f),
                                    selectedLabelColor = RoyalBluePrimary
                                )
                            )
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Common Presets Quick Add
                Text(
                    text = "QUICK TEMPLATES (TAP TO ADD)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    commonQuantities.forEach { qty ->
                        val isAdded = presetsList.contains(qty)
                        val label = if (qty % 1.0 == 0.0) "${qty.toInt()}L" else "${qty}L"
                        FilterChip(
                            selected = isAdded,
                            onClick = {
                                presetsList = if (isAdded) {
                                    presetsList.filter { it != qty }
                                } else {
                                    (presetsList + qty).sorted()
                                }
                            },
                            label = {
                                Text(
                                    text = if (isAdded) "✓ $label" else "+ $label",
                                    fontSize = 11.sp,
                                    fontWeight = if (isAdded) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DairyGreen.copy(alpha = 0.15f),
                                selectedLabelColor = DairyGreen
                            )
                        )
                    }
                }

                // Add Custom Liters Input
                Text(
                    text = "ADD CUSTOM QUANTITY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customQtyText,
                        onValueChange = { customQtyText = it },
                        label = { Text("Liters (e.g. 1.25)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = {
                            val parsed = customQtyText.toDoubleOrNull()
                            if (parsed != null && parsed > 0 && !presetsList.contains(parsed)) {
                                presetsList = (presetsList + parsed).sorted()
                                customQtyText = ""
                            }
                        },
                        enabled = (customQtyText.toDoubleOrNull() ?: 0.0) > 0.0,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("+ Add")
                    }
                }

                // Presets Quick Reset
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            presetsList = listOf(0.5, 1.0, 1.5, 2.0)
                        }
                    ) {
                        Text("🔄 Reset Defaults (0.5-2L)", fontSize = 11.sp)
                    }

                    TextButton(
                        onClick = {
                            presetsList = listOf(1.0, 2.0, 3.0, 5.0)
                        }
                    ) {
                        Text("🥛 Bulk Preset (1-5L)", fontSize = 11.sp)
                    }
                }

                // Live Preview
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Live Preview on Delivery Sheet:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⚡ Quick Add:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                            if (presetsList.isEmpty()) {
                                Text("(no presets)", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                            } else {
                                presetsList.forEach { p ->
                                    val pStr = if (p % 1.0 == 0.0) p.toInt().toString() else p.toString()
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = RoyalBluePrimary.copy(alpha = 0.1f)
                                    ) {
                                        Text(
                                            "${pStr}L",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RoyalBluePrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val presetsString = presetsList.joinToString(",") {
                        if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
                    }
                    onSavePresets(presetsString)
                },
                colors = ButtonDefaults.buttonColors(containerColor = DairyGreen)
            ) {
                Text("✓ Save Presets", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

