package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.PaymentEntity
import com.example.ui.MilkMateViewModel
import com.example.ui.theme.*
import com.example.ui.util.BusinessModeFeatures
import com.example.ui.util.QrCodeView
import com.example.ui.util.ReportExportHelper
import com.example.ui.util.loc
import com.example.ui.util.appStr
import java.text.SimpleDateFormat
import java.util.*

enum class PaymentPeriod {
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    ALL_TIME
}

enum class CustomerSortOption {
    HIGHEST_DUE,
    LOWEST_DUE,
    NAME_AZ,
    ROUTE_WISE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentsScreen(viewModel: MilkMateViewModel) {
    val context = LocalContext.current
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val business by viewModel.currentBusiness.collectAsStateWithLifecycle()
    val session by viewModel.sessionState.collectAsStateWithLifecycle()

    // Navigation & State
    var activeTab by remember { mutableStateOf("CUSTOMERS") } // CUSTOMERS, ROUTE_COLLECT, SUPPLIERS, UPI_QR, HISTORY
    var selectedPeriod by remember { mutableStateOf(PaymentPeriod.THIS_MONTH) }
    var customerFilter by remember { mutableStateOf("ALL") } // ALL, DUE, HIGH_DUE, ADVANCE, ZERO
    var sortOption by remember { mutableStateOf(CustomerSortOption.HIGHEST_DUE) }
    var historyFilter by remember { mutableStateOf("ALL") } // ALL, RECEIVED, PAID, UPI, CASH, TODAY
    var searchQuery by remember { mutableStateOf("") }
    var selectedRouteFilter by remember { mutableStateOf("ALL") }

    // Modals
    var showReceiveDialogForCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var showQuickReceiveDialog by remember { mutableStateOf(false) }
    var showPaySupplierDialog by remember { mutableStateOf(false) }
    var showQrForCustomer by remember { mutableStateOf<Pair<CustomerEntity?, Double?>?>(null) }
    var showPassbookForCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var showReceiptModal by remember { mutableStateOf<Pair<PaymentEntity, Double>?>(null) }
    var showConfigureUpiModal by remember { mutableStateOf(false) }

    // Date calculations
    val todayMidnight = remember { MilkMateViewModel.getTodayMidnightMillis() }
    val weekAgo = remember { todayMidnight - 7 * 86400000L }
    val monthStart = remember {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        cal.timeInMillis
    }

    // Filter payments by selected time period
    val periodPayments = remember(payments, selectedPeriod) {
        when (selectedPeriod) {
            PaymentPeriod.TODAY -> payments.filter { MilkMateViewModel.normalizeToMidnight(it.date) == todayMidnight }
            PaymentPeriod.THIS_WEEK -> payments.filter { it.date >= weekAgo }
            PaymentPeriod.THIS_MONTH -> payments.filter { it.date >= monthStart }
            PaymentPeriod.ALL_TIME -> payments
        }
    }

    val periodReceived = periodPayments.filter { it.paymentType == "CUSTOMER_PAYMENT" || it.paymentType == "RECEIVED" }.sumOf { it.amount }
    val periodPaidOut = periodPayments.filter { it.paymentType == "SUPPLIER_PAYMENT" || it.paymentType == "PAID_OUT" }.sumOf { it.amount }
    val periodNetFlow = periodReceived - periodPaidOut

    // Aggregates for All Customers & Suppliers
    val nonSuppliers = remember(customers) { customers.filter { it.type != "SUPPLIER" } }
    val suppliers = remember(customers) { customers.filter { it.type == "SUPPLIER" } }

    val totalCustomerDue = nonSuppliers.filter { it.outstandingBalance > 0 }.sumOf { it.outstandingBalance }
    val totalCustomerAdvance = nonSuppliers.filter { it.outstandingBalance < 0 }.sumOf { -it.outstandingBalance }
    val totalSupplierPayable = suppliers.sumOf { if (it.outstandingBalance < 0) -it.outstandingBalance else it.outstandingBalance }
    val dueCustomersCount = nonSuppliers.count { it.outstandingBalance > 0 }
    val highDueCustomersCount = nonSuppliers.count { it.outstandingBalance >= 1000 }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Header
        val currentMode = business?.businessMode ?: "FARMER"
        val isTrader = currentMode == "TRADER"
        val showSuppliers = BusinessModeFeatures.showCollectionDesk(business) || currentMode in listOf("TRADER", "COLLECTION_CENTER", "INTEGRATED", "CUSTOM")

        LaunchedEffect(showSuppliers) {
            if (!showSuppliers && activeTab == "SUPPLIERS") {
                activeTab = "CUSTOMERS"
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (isTrader) "TRADING LEDGER & DUAL KHATA" else "MONEY & KHATA HUB",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isTrader) GoldenOrange else RoyalBluePrimary
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isTrader) GoldenOrange.copy(alpha = 0.15f) else RoyalBlueLight
                    ) {
                        Text(
                            text = "LIVE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isTrader) GoldenOrange else RoyalBluePrimary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(
                    text = if (isTrader) "Receivables & Farmer Khata" else "Payments & Khata",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // Export Statement / PDF Button
            IconButton(
                onClick = {
                    val headers = listOf("Date", "Party Name", "Type", "Method", "Amount (₹)", "Ref No")
                    val rows = periodPayments.map { p ->
                        listOf(
                            dateFormat.format(Date(p.date)),
                            p.customerName,
                            if (p.paymentType == "CUSTOMER_PAYMENT" || p.paymentType == "RECEIVED") "Received" else "Paid Out",
                            p.method,
                            "₹${p.amount.toInt()}",
                            p.referenceNo.ifBlank { "-" }
                        )
                    }
                    ReportExportHelper.generateAndSharePdf(
                        context = context,
                        title = "${business?.businessName ?: "MilkMate"} - Payment Ledger",
                        period = when (selectedPeriod) {
                            PaymentPeriod.TODAY -> "Today"
                            PaymentPeriod.THIS_WEEK -> "This Week"
                            PaymentPeriod.THIS_MONTH -> "This Month"
                            PaymentPeriod.ALL_TIME -> "All Time"
                        },
                        headers = headers,
                        rows = rows,
                        summaryMetrics = listOf(
                            "Total Collected" to "₹${periodReceived.toInt()}",
                            "Total Paid Out" to "₹${periodPaidOut.toInt()}",
                            "Customer Due" to "₹${totalCustomerDue.toInt()}",
                            "Net Cash Flow" to "₹${periodNetFlow.toInt()}"
                        )
                    )
                },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .size(38.dp)
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = DangerRed, modifier = Modifier.size(20.dp))
            }
        }

        // Quick Period Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                PaymentPeriod.TODAY to "today".loc(),
                PaymentPeriod.THIS_WEEK to "this_week".loc(),
                PaymentPeriod.THIS_MONTH to "this_month".loc(),
                PaymentPeriod.ALL_TIME to "all".loc()
            ).forEach { (period, label) ->
                FilterChip(
                    selected = selectedPeriod == period,
                    onClick = { selectedPeriod = period },
                    label = { Text(label, fontSize = 11.sp, fontWeight = if (selectedPeriod == period) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 4-Card Dynamic KPI Dashboard
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    // Collected in Period
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Collected", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${periodReceived.toInt()}", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = DairyGreen)
                        Text("${periodPayments.count { it.paymentType != "SUPPLIER_PAYMENT" }} receipts", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // Total Receivables (Customer Due)
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (isTrader) "Buyer Due (Receivables)" else "Customer Due", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${totalCustomerDue.toInt()}", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = DangerRed)
                        Text("$dueCustomersCount parties due", fontSize = 10.sp, color = DangerRed)
                    }

                    // 3rd KPI Column: Supplier Payables OR Period Net Cash Flow
                    if (showSuppliers) {
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text(if (isTrader) "Farmer Payables" else "Supplier Payables", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${totalSupplierPayable.toInt()}", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = RoyalBluePrimary)
                            Text("${suppliers.size} suppliers", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text("Advance Held", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${totalCustomerAdvance.toInt()}", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = DairyGreen)
                            Text("Credits in hand", fontSize = 10.sp, color = DairyGreen)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                // Secondary metrics: Advance & Net Flow
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Savings, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Total Collection: ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${periodReceived.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DairyGreen)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Period Net Flow: ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${if (periodNetFlow >= 0) "+" else ""}₹${periodNetFlow.toInt()}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (periodNetFlow >= 0) DairyGreen else DangerRed
                        )
                    }
                }
            }
        }

        // Primary Action Buttons Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showQuickReceiveDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1.3f)
                    .height(44.dp)
            ) {
                Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("+ Receive ₹", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            if (showSuppliers) {
                Button(
                    onClick = { showPaySupplierDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1.1f)
                        .height(44.dp)
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pay Supplier", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            OutlinedButton(
                onClick = { showQrForCustomer = Pair(null, null) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(0.9f)
                    .height(44.dp)
            ) {
                Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp), tint = RoyalBluePrimary)
                Spacer(modifier = Modifier.width(4.dp))
                Text("UPI QR", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // Main Tab Pills (Scrollable)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ShiftPill(
                title = if (isTrader) "👥 Buyers (${nonSuppliers.size})" else "👥 Customers (${nonSuppliers.size})",
                isSelected = activeTab == "CUSTOMERS",
                onClick = { activeTab = "CUSTOMERS" }
            )
            ShiftPill(
                title = "⚡ Route Collect",
                isSelected = activeTab == "ROUTE_COLLECT",
                onClick = { activeTab = "ROUTE_COLLECT" }
            )
            if (showSuppliers) {
                ShiftPill(
                    title = if (isTrader) "🚚 Farmers (${suppliers.size})" else "🚚 Suppliers (${suppliers.size})",
                    isSelected = activeTab == "SUPPLIERS",
                    onClick = { activeTab = "SUPPLIERS" }
                )
            }
            ShiftPill(
                title = "📲 Bharat UPI QR",
                isSelected = activeTab == "UPI_QR",
                onClick = { activeTab = "UPI_QR" }
            )
            ShiftPill(
                title = if (isTrader) "📜 Cashbook (${payments.size})" else "📜 Passbook (${payments.size})",
                isSelected = activeTab == "HISTORY",
                onClick = { activeTab = "HISTORY" }
            )
        }

        // Contextual Controls & Content
        when (activeTab) {
            "CUSTOMERS" -> {
                // Search & Filter Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search name, mobile or route...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    )

                    // Sort Dropdown
                    var sortExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { sortExpanded = true },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                .size(48.dp)
                        ) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort", tint = RoyalBluePrimary)
                        }
                        DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("🔥 Highest Due First") },
                                onClick = { sortOption = CustomerSortOption.HIGHEST_DUE; sortExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("⬇ Lowest Due First") },
                                onClick = { sortOption = CustomerSortOption.LOWEST_DUE; sortExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("🔤 Name (A to Z)") },
                                onClick = { sortOption = CustomerSortOption.NAME_AZ; sortExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("📍 Route-wise") },
                                onClick = { sortOption = CustomerSortOption.ROUTE_WISE; sortExpanded = false }
                            )
                        }
                    }
                }

                // Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "ALL" to "All (${nonSuppliers.size})",
                        "DUE" to "⚠️ Due ($dueCustomersCount)",
                        "HIGH_DUE" to "🔥 High Due >₹1k ($highDueCustomersCount)",
                        "ADVANCE" to "💎 Advance (${nonSuppliers.count { it.outstandingBalance < 0 }})",
                        "ZERO" to "✅ Settled (${nonSuppliers.count { it.outstandingBalance == 0.0 }})"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = customerFilter == key,
                            onClick = { customerFilter = key },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = if (key == "DUE" && customerFilter == key) {
                                FilterChipDefaults.filterChipColors(selectedContainerColor = DangerRedLight, selectedLabelColor = DangerRed)
                            } else if (key == "ADVANCE" && customerFilter == key) {
                                FilterChipDefaults.filterChipColors(selectedContainerColor = DairyGreenLight, selectedLabelColor = DairyGreen)
                            } else FilterChipDefaults.filterChipColors()
                        )
                    }
                }

                // Customer List
                val filteredCustomers = remember(nonSuppliers, customerFilter, searchQuery, sortOption) {
                    var list = nonSuppliers.filter { cust ->
                        val matchesFilter = when (customerFilter) {
                            "DUE" -> cust.outstandingBalance > 0
                            "HIGH_DUE" -> cust.outstandingBalance >= 1000
                            "ADVANCE" -> cust.outstandingBalance < 0
                            "ZERO" -> cust.outstandingBalance == 0.0
                            else -> true
                        }
                        val matchesSearch = searchQuery.isBlank() ||
                                cust.name.contains(searchQuery, ignoreCase = true) ||
                                cust.mobile.contains(searchQuery) ||
                                cust.route.contains(searchQuery, ignoreCase = true)
                        matchesFilter && matchesSearch
                    }

                    list = when (sortOption) {
                        CustomerSortOption.HIGHEST_DUE -> list.sortedByDescending { it.outstandingBalance }
                        CustomerSortOption.LOWEST_DUE -> list.sortedBy { it.outstandingBalance }
                        CustomerSortOption.NAME_AZ -> list.sortedBy { it.name.lowercase() }
                        CustomerSortOption.ROUTE_WISE -> list.sortedWith(compareBy({ it.route.lowercase() }, { it.name.lowercase() }))
                    }
                    list
                }

                if (filteredCustomers.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No customers match your filter or search.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredCustomers, key = { it.id }) { cust ->
                            CustomerReceivableCard(
                                customer = cust,
                                onReceive = { showReceiveDialogForCustomer = cust },
                                onQrCode = { showQrForCustomer = Pair(cust, if (cust.outstandingBalance > 0) cust.outstandingBalance else null) },
                                onStatement = { showPassbookForCustomer = cust },
                                onWhatsApp = {
                                    val phone = cust.mobile.replace("+", "").replace(" ", "")
                                    val dueText = if (cust.outstandingBalance > 0) "Pending Due: ₹${cust.outstandingBalance.toInt()}" else "Account is Settled"
                                    val upiText = if (session.upiId.isNotBlank()) "💳 *Pay via UPI*: ${session.upiId}" else ""
                                    val msg = """
                                        🥛 *MilkMate Dairy Bill*
                                        Hello ${cust.name},
                                        
                                        Your milk supply account status:
                                        • Milk Type: ${cust.milkType} (${cust.defaultQuantity} L)
                                        • Rate: ₹${cust.rate}/L
                                        • *$dueText*
                                        
                                        $upiText
                                        Thank you! 🙏
                                    """.trimIndent()
                                    val uri = Uri.parse("https://api.whatsapp.com/send?phone=$phone&text=${Uri.encode(msg)}")
                                    try { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) } catch (e: Exception) { viewModel.showMessage("WhatsApp not installed") }
                                },
                                onCall = {
                                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${cust.mobile}"))
                                    try { context.startActivity(dialIntent) } catch (_: Exception) {}
                                }
                            )
                        }
                    }
                }
            }

            "ROUTE_COLLECT" -> {
                // High-Speed Route Batch Collection Mode
                val routes = remember(nonSuppliers) {
                    val rSet = nonSuppliers.map { it.route.trim() }.filter { it.isNotBlank() }.toSortedSet()
                    listOf("ALL") + rSet.toList()
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Route Selector Header
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("⚡ Fast Route Collection", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Tick off dues during milk delivery", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            // Route Chips
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                routes.forEach { r ->
                                    FilterChip(
                                        selected = selectedRouteFilter == r,
                                        onClick = { selectedRouteFilter = r },
                                        label = { Text(if (r == "ALL") "All Routes" else r, fontSize = 10.sp) }
                                    )
                                }
                            }
                        }
                    }

                    val routeCustomers = remember(nonSuppliers, selectedRouteFilter) {
                        nonSuppliers.filter {
                            (selectedRouteFilter == "ALL" || it.route.equals(selectedRouteFilter, ignoreCase = true)) && it.outstandingBalance > 0
                        }.sortedByDescending { it.outstandingBalance }
                    }

                    if (routeCustomers.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(40.dp))
                                Text("All caught up! No pending dues on this route.", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Text("${routeCustomers.size} customers with pending dues (Total: ₹${routeCustomers.sumOf { it.outstandingBalance }.toInt()})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = DangerRed)

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(routeCustomers, key = { it.id }) { cust ->
                                RouteQuickCollectRow(
                                    customer = cust,
                                    onCollect = { amount, method ->
                                        viewModel.savePayment(
                                            customerId = cust.id,
                                            customerName = cust.name,
                                            paymentType = "CUSTOMER_PAYMENT",
                                            amount = amount,
                                            method = method,
                                            referenceNo = "Route Collect",
                                            notes = "Collected on route ${cust.route}"
                                        )
                                        viewModel.showMessage("✓ ₹${amount.toInt()} collected from ${cust.name} ($method)")
                                    },
                                    onShowQr = { showQrForCustomer = Pair(cust, cust.outstandingBalance) }
                                )
                            }
                        }
                    }
                }
            }

            "SUPPLIERS" -> {
                // Supplier Payables
                val filteredSuppliers = remember(suppliers, searchQuery) {
                    suppliers.filter {
                        searchQuery.isBlank() ||
                                it.name.contains(searchQuery, ignoreCase = true) ||
                                it.mobile.contains(searchQuery)
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search supplier / feed mill...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                )

                if (filteredSuppliers.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No suppliers found. Add suppliers from the Customer/Parties screen.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredSuppliers, key = { it.id }) { sup ->
                            val payable = if (sup.outstandingBalance < 0) -sup.outstandingBalance else sup.outstandingBalance
                            SupplierPayableCard(
                                supplier = sup,
                                payableAmount = payable,
                                onPayout = {
                                    showPaySupplierDialog = true
                                },
                                onStatement = { showPassbookForCustomer = sup },
                                onWhatsApp = {
                                    val phone = sup.mobile.replace("+", "").replace(" ", "")
                                    val msg = "Hello ${sup.name}, supplier account balance with ${business?.businessName ?: "MilkMate"} is ₹${payable.toInt()}. Thank you!"
                                    val uri = Uri.parse("https://api.whatsapp.com/send?phone=$phone&text=${Uri.encode(msg)}")
                                    try { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) } catch (e: Exception) { viewModel.showMessage("WhatsApp not installed") }
                                }
                            )
                        }
                    }
                }
            }

            "UPI_QR" -> {
                // Interactive Bharat UPI QR Studio & Counter Display
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    var counterAmountText by remember { mutableStateOf("") }
                    val upiId = session.upiId
                    val bizName = business?.businessName ?: "MilkMate Dairy"
                    val parsedAmt = counterAmountText.toDoubleOrNull()

                    val dynamicUpiUrl = remember(upiId, bizName, parsedAmt) {
                        if (upiId.isBlank()) ""
                        else {
                            val amtStr = if (parsedAmt != null && parsedAmt > 0) "&am=${String.format(Locale.US, "%.2f", parsedAmt)}" else ""
                            "upi://pay?pa=${Uri.encode(upiId)}&pn=${Uri.encode(bizName)}&tn=${Uri.encode("Milk Payment")}$amtStr&cu=INR"
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("🏪 Dairy Counter UPI QR", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Display on screen for customers to scan & pay", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                TextButton(onClick = { showConfigureUpiModal = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Edit UPI", fontSize = 11.sp)
                                }
                            }

                            if (upiId.isBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFFF3E0),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("No UPI ID Configured", fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                                        Button(
                                            onClick = { showConfigureUpiModal = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary)
                                        ) {
                                            Text("Set UPI ID")
                                        }
                                    }
                                }
                            } else {
                                // Amount input
                                OutlinedTextField(
                                    value = counterAmountText,
                                    onValueChange = { counterAmountText = it },
                                    label = { Text("Enter Amount (Leave empty for customer to choose)") },
                                    leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = RoyalBluePrimary) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Preset Chips
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("100", "200", "500", "1000").forEach { preset ->
                                        FilterChip(
                                            selected = counterAmountText == preset,
                                            onClick = { counterAmountText = preset },
                                            label = { Text("₹$preset", fontSize = 11.sp) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                // QR Code Box
                                Box(
                                    modifier = Modifier
                                        .background(Color.White, shape = RoundedCornerShape(12.dp))
                                        .padding(14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    QrCodeView(
                                        content = dynamicUpiUrl,
                                        size = 220.dp,
                                        darkColor = Color(0xFF1B5E20)
                                    )
                                }

                                Text(
                                    text = if (parsedAmt != null && parsedAmt > 0) "Pay ₹${parsedAmt.toInt()} to $bizName" else "Scan & Pay any amount to $bizName",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = RoyalBluePrimary
                                )

                                Text("UPI: $upiId", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            "HISTORY" -> {
                // Transaction Ledger & Audit Passbook
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Search & Filter
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by party or reference #...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "ALL" to "All (${periodPayments.size})",
                            "RECEIVED" to "Received",
                            "PAID" to "Paid Out",
                            "UPI" to "UPI",
                            "CASH" to "Cash"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = historyFilter == key,
                                onClick = { historyFilter = key },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    val filteredHistory = remember(periodPayments, historyFilter, searchQuery) {
                        periodPayments.filter { p ->
                            val matchesType = when (historyFilter) {
                                "RECEIVED" -> p.paymentType == "CUSTOMER_PAYMENT" || p.paymentType == "RECEIVED"
                                "PAID" -> p.paymentType == "SUPPLIER_PAYMENT" || p.paymentType == "PAID_OUT"
                                "UPI" -> p.method.contains("UPI", ignoreCase = true)
                                "CASH" -> p.method.contains("CASH", ignoreCase = true)
                                else -> true
                            }
                            val matchesSearch = searchQuery.isBlank() ||
                                    p.customerName.contains(searchQuery, ignoreCase = true) ||
                                    p.referenceNo.contains(searchQuery, ignoreCase = true)
                            matchesType && matchesSearch
                        }
                    }

                    if (filteredHistory.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No transactions found for this period.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredHistory, key = { it.id }) { p ->
                                val isReceived = p.paymentType == "CUSTOMER_PAYMENT" || p.paymentType == "RECEIVED"
                                val matchedCust = customers.find { it.id == p.customerId }
                                val remainingBal = matchedCust?.outstandingBalance ?: 0.0

                                AdvancedPaymentCard(
                                    payment = p,
                                    dateStr = dateFormat.format(Date(p.date)),
                                    isReceived = isReceived,
                                    onDelete = { viewModel.deletePayment(p.id) },
                                    onShareReceipt = {
                                        showReceiptModal = Pair(p, remainingBal)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Implementations
    showReceiveDialogForCustomer?.let { cust ->
        AdvancedReceivePaymentModal(
            preselectedCustomer = cust,
            allCustomers = customers,
            onDismiss = { showReceiveDialogForCustomer = null },
            onSave = { targetCust, amt, method, ref, notes, date, autoShare ->
                viewModel.savePayment(
                    customerId = targetCust.id,
                    customerName = targetCust.name,
                    paymentType = if (targetCust.type == "SUPPLIER") "SUPPLIER_PAYMENT" else "CUSTOMER_PAYMENT",
                    amount = amt,
                    method = method,
                    referenceNo = ref,
                    notes = notes
                )
                showReceiveDialogForCustomer = null
                val newBal = targetCust.outstandingBalance - amt
                val savedPayment = PaymentEntity(
                    id = UUID.randomUUID().toString(),
                    businessId = targetCust.businessId,
                    customerId = targetCust.id,
                    customerName = targetCust.name,
                    paymentType = if (targetCust.type == "SUPPLIER") "SUPPLIER_PAYMENT" else "CUSTOMER_PAYMENT",
                    date = date,
                    amount = amt,
                    method = method,
                    referenceNo = ref,
                    notes = notes
                )
                if (autoShare) {
                    showReceiptModal = Pair(savedPayment, newBal)
                }
            }
        )
    }

    if (showQuickReceiveDialog) {
        AdvancedReceivePaymentModal(
            preselectedCustomer = null,
            allCustomers = customers,
            onDismiss = { showQuickReceiveDialog = false },
            onSave = { targetCust, amt, method, ref, notes, date, autoShare ->
                viewModel.savePayment(
                    customerId = targetCust.id,
                    customerName = targetCust.name,
                    paymentType = if (targetCust.type == "SUPPLIER") "SUPPLIER_PAYMENT" else "CUSTOMER_PAYMENT",
                    amount = amt,
                    method = method,
                    referenceNo = ref,
                    notes = notes
                )
                showQuickReceiveDialog = false
                val newBal = targetCust.outstandingBalance - amt
                val savedPayment = PaymentEntity(
                    id = UUID.randomUUID().toString(),
                    businessId = targetCust.businessId,
                    customerId = targetCust.id,
                    customerName = targetCust.name,
                    paymentType = if (targetCust.type == "SUPPLIER") "SUPPLIER_PAYMENT" else "CUSTOMER_PAYMENT",
                    date = date,
                    amount = amt,
                    method = method,
                    referenceNo = ref,
                    notes = notes
                )
                if (autoShare) {
                    showReceiptModal = Pair(savedPayment, newBal)
                }
            }
        )
    }

    if (showPaySupplierDialog) {
        RecordSupplierPayoutModal(
            preselectedSupplier = suppliers.firstOrNull(),
            allSuppliers = if (suppliers.isNotEmpty()) suppliers else customers,
            onDismiss = { showPaySupplierDialog = false },
            onSave = { sup, amt, method, ref, notes ->
                viewModel.savePayment(
                    customerId = sup.id,
                    customerName = sup.name,
                    paymentType = "SUPPLIER_PAYMENT",
                    amount = amt,
                    method = method,
                    referenceNo = ref,
                    notes = notes
                )
                showPaySupplierDialog = false
            }
        )
    }

    showQrForCustomer?.let { (cust, amount) ->
        DynamicUpiQrDialog(
            businessName = business?.businessName ?: "MilkMate Dairy",
            upiId = session.upiId,
            customer = cust,
            defaultAmount = amount,
            onDismiss = { showQrForCustomer = null },
            onConfigureUpi = {
                showQrForCustomer = null
                showConfigureUpiModal = true
            }
        )
    }

    showPassbookForCustomer?.let { cust ->
        CustomerKhataPassbookModal(
            customer = cust,
            viewModel = viewModel,
            onDismiss = { showPassbookForCustomer = null },
            onCollectPayment = {
                showPassbookForCustomer = null
                showReceiveDialogForCustomer = cust
            }
        )
    }

    showReceiptModal?.let { (payment, remainingBal) ->
        PaymentReceiptModal(
            payment = payment,
            businessName = business?.businessName ?: "MilkMate Dairy",
            remainingBalance = remainingBal,
            onDismiss = { showReceiptModal = null }
        )
    }

    if (showConfigureUpiModal) {
        ConfigureUpiModal(
            currentUpiId = session.upiId,
            onDismiss = { showConfigureUpiModal = false },
            onSave = { upi ->
                viewModel.setUpiId(upi)
                showConfigureUpiModal = false
            }
        )
    }
}

/**
 * Customer Receivable Card with multi-actions
 */
@Composable
fun CustomerReceivableCard(
    customer: CustomerEntity,
    onReceive: () -> Unit,
    onQrCode: () -> Unit,
    onStatement: () -> Unit,
    onWhatsApp: () -> Unit,
    onCall: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(RoyalBlueLight, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = customer.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (customer.route.isNotBlank()) {
                                Text("📍 ${customer.route}", fontSize = 11.sp, color = RoyalBluePrimary, fontWeight = FontWeight.Medium)
                                Text("•", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("${customer.milkType} (${customer.defaultQuantity}L)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Balance Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (customer.outstandingBalance > 0) DangerRedLight else if (customer.outstandingBalance < 0) DairyGreenLight else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (customer.outstandingBalance > 0) "Due: ₹${customer.outstandingBalance.toInt()}" else if (customer.outstandingBalance < 0) "Adv: ₹${Math.abs(customer.outstandingBalance).toInt()}" else "Settled",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (customer.outstandingBalance > 0) DangerRed else if (customer.outstandingBalance < 0) DairyGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onReceive,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(36.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Collect ₹", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onQrCode,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(14.dp), tint = RoyalBluePrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("UPI QR", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = onStatement,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                ) {
                    Text("Khata", fontSize = 11.sp)
                }

                IconButton(
                    onClick = onWhatsApp,
                    modifier = Modifier
                        .background(Color(0xFFE8F5E9), CircleShape)
                        .size(36.dp)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = "WhatsApp", tint = DairyGreen, modifier = Modifier.size(18.dp))
                }

                if (customer.mobile.isNotBlank()) {
                    IconButton(
                        onClick = onCall,
                        modifier = Modifier
                            .background(RoyalBlueLight, CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "Call", tint = RoyalBluePrimary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

/**
 * Route Quick-Collect Row for rapid batch collection
 */
@Composable
fun RouteQuickCollectRow(
    customer: CustomerEntity,
    onCollect: (Double, String) -> Unit,
    onShowQr: () -> Unit
) {
    var quickMethod by remember { mutableStateOf("CASH") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    text = "Due: ₹${customer.outstandingBalance.toInt()} • ${customer.route.ifBlank { "Direct" }}",
                    fontSize = 11.sp,
                    color = DangerRed,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                // Method Switcher
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (quickMethod == "CASH") DairyGreenLight else RoyalBlueLight,
                    modifier = Modifier.clickable {
                        quickMethod = if (quickMethod == "CASH") "UPI" else "CASH"
                    }
                ) {
                    Text(
                        text = if (quickMethod == "CASH") "💵 Cash" else "📱 UPI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (quickMethod == "CASH") DairyGreen else RoyalBluePrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }

                // 1-Tap Full Settle
                Button(
                    onClick = { onCollect(customer.outstandingBalance, quickMethod) },
                    colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("✓ Settle ₹${customer.outstandingBalance.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(onClick = onShowQr, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.QrCode, contentDescription = "QR", tint = RoyalBluePrimary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

/**
 * Supplier Payable Card
 */
@Composable
fun SupplierPayableCard(
    supplier: CustomerEntity,
    payableAmount: Double,
    onPayout: () -> Unit,
    onStatement: () -> Unit,
    onWhatsApp: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(DangerRedLight, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.LocalShipping, contentDescription = null, tint = DangerRed, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = supplier.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(supplier.mobile.ifBlank { "Feed mill / Supplier" }, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = DangerRedLight
                ) {
                    Text(
                        "Payable: ₹${payableAmount.toInt()}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = DangerRed,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onPayout,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(36.dp)
                ) {
                    Text("Record Payout", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onStatement,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                ) {
                    Text("Statement", fontSize = 11.sp)
                }

                IconButton(
                    onClick = onWhatsApp,
                    modifier = Modifier
                        .background(Color(0xFFE8F5E9), CircleShape)
                        .size(36.dp)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = "WhatsApp", tint = DairyGreen, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

/**
 * Advanced Payment Card in Transaction History
 */
@Composable
fun AdvancedPaymentCard(
    payment: PaymentEntity,
    dateStr: String,
    isReceived: Boolean,
    onDelete: () -> Unit,
    onShareReceipt: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(if (isReceived) DairyGreenLight else DangerRedLight, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isReceived) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = if (isReceived) DairyGreen else DangerRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = payment.customerName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        text = "${payment.method} • $dateStr",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (payment.referenceNo.isNotBlank()) {
                        Text("Ref: ${payment.referenceNo}", fontSize = 10.sp, color = RoyalBluePrimary, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "${if (isReceived) "+" else "-"}₹${payment.amount.toInt()}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = if (isReceived) DairyGreen else DangerRed
                )
                IconButton(onClick = onShareReceipt, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = "Receipt", tint = RoyalBluePrimary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = DangerRed.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
