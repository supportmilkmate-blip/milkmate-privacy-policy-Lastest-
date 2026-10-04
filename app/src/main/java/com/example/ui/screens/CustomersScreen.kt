package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CustomerEntity
import com.example.ui.MilkMateViewModel
import com.example.ui.theme.*
import com.example.ui.util.BusinessModeFeatures
import com.example.ui.util.loc
import com.example.ui.util.appStr
import java.util.Locale

@Composable
fun CustomersScreen(viewModel: MilkMateViewModel) {
    val context = LocalContext.current
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val staffList by viewModel.staffList.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf("INDIVIDUAL") } // INDIVIDUAL, BULK_BUYER, SUPPLIER
    var balanceFilter by remember { mutableStateOf("ALL") } // ALL, DUE, ADVANCE, ZERO
    var searchQuery by remember { mutableStateOf("") }
    var showAddOptionsDialog by remember { mutableStateOf(false) }
    var showManualAddDialog by remember { mutableStateOf(false) }
    var showImportContactsDialog by remember { mutableStateOf(false) }
    var editingCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var statementCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var collectingCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var deliveringCustomer by remember { mutableStateOf<CustomerEntity?>(null) }

    val currentBiz by viewModel.currentBusiness.collectAsStateWithLifecycle()
    val currentMode = currentBiz?.businessMode ?: "FARMER"
    val supportedMilkTypes = currentBiz?.supportedMilkTypes ?: "BOTH"

    val showSuppliers = BusinessModeFeatures.showCollectionDesk(currentBiz) || currentMode in listOf("TRADER", "COLLECTION_CENTER", "INTEGRATED", "CUSTOM")
    val showIndividual = currentMode != "COLLECTION_CENTER"

    LaunchedEffect(showSuppliers, showIndividual) {
        if (!showSuppliers && selectedTab == "SUPPLIER") {
            selectedTab = if (showIndividual) "INDIVIDUAL" else "BULK_BUYER"
        } else if (!showIndividual && selectedTab == "INDIVIDUAL") {
            selectedTab = if (showSuppliers) "SUPPLIER" else "BULK_BUYER"
        }
    }

    val tabCustomers = customers.filter { it.type == selectedTab }
    val totalDueInTab = tabCustomers.filter { it.outstandingBalance > 0 }.sumOf { it.outstandingBalance }
    val totalAdvanceInTab = tabCustomers.filter { it.outstandingBalance < 0 }.sumOf { -it.outstandingBalance }

    val filteredCustomers = tabCustomers.filter { cust ->
        val matchesBalance = when (balanceFilter) {
            "DUE" -> cust.outstandingBalance > 0
            "ADVANCE" -> cust.outstandingBalance < 0
            "ZERO" -> cust.outstandingBalance == 0.0
            else -> true
        }
        val matchesSearch = searchQuery.isBlank() || cust.name.contains(searchQuery, ignoreCase = true) || cust.mobile.contains(searchQuery) || cust.route.contains(searchQuery, ignoreCase = true)
        matchesBalance && matchesSearch
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header
        Column {
            Text(
                text = "customers_and_parties".loc(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = RoyalBluePrimary
            )
            Text(
                text = "customer_crm_khata".loc(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        // Tabs: Individual | Bulk Buyer | Supplier (Scoped to active Business Mode)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val indCount = customers.count { it.type == "INDIVIDUAL" }
            val bulkCount = customers.count { it.type == "BULK_BUYER" }
            val supCount = customers.count { it.type == "SUPPLIER" }

            if (showIndividual) {
                ShiftPill(
                    title = "Individual ($indCount)".loc(),
                    isSelected = selectedTab == "INDIVIDUAL",
                    onClick = { selectedTab = "INDIVIDUAL" },
                    modifier = Modifier.weight(1f)
                )
            }
            ShiftPill(
                title = if (currentMode == "PROCESSING_UNIT") "Merchants ($bulkCount)".loc() else "Bulk Buyer ($bulkCount)".loc(),
                isSelected = selectedTab == "BULK_BUYER",
                onClick = { selectedTab = "BULK_BUYER" },
                modifier = Modifier.weight(1f)
            )
            if (showSuppliers) {
                ShiftPill(
                    title = if (currentMode == "TRADER" || currentMode == "COLLECTION_CENTER") "Farmers ($supCount)".loc() else "Supplier ($supCount)".loc(),
                    isSelected = selectedTab == "SUPPLIER",
                    onClick = { selectedTab = "SUPPLIER" },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Financial Summary Strip for Current Tab
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("total_due".loc(), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹${totalDueInTab.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DangerRed)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("in_advance".loc(), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹${totalAdvanceInTab.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DairyGreen)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("parties_count".loc(), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${tabCustomers.size}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                }
            }
        }

        // Big Green Add Button
        Button(
            onClick = {
                editingCustomer = null
                showAddOptionsDialog = true
            },
            colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(46.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            val btnText = when (selectedTab) {
                "BULK_BUYER" -> "+ Add Bulk Buyer"
                "SUPPLIER" -> "+ Add Supplier"
                else -> "+ Add Individual Customer"
            }
            Text(
                text = btnText.loc(),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("🔎 Search by name, phone or route...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        )

        // Balance Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val dueCount = tabCustomers.count { it.outstandingBalance > 0 }
            val advCount = tabCustomers.count { it.outstandingBalance < 0 }

            FilterChip(
                selected = balanceFilter == "ALL",
                onClick = { balanceFilter = "ALL" },
                label = { Text("All (${tabCustomers.size})", fontSize = 11.sp) }
            )
            FilterChip(
                selected = balanceFilter == "DUE",
                onClick = { balanceFilter = "DUE" },
                label = { Text("Due ($dueCount)", fontSize = 11.sp, color = if (dueCount > 0) DangerRed else Color.Unspecified) }
            )
            FilterChip(
                selected = balanceFilter == "ADVANCE",
                onClick = { balanceFilter = "ADVANCE" },
                label = { Text("Advance ($advCount)", fontSize = 11.sp, color = if (advCount > 0) DairyGreen else Color.Unspecified) }
            )
            FilterChip(
                selected = balanceFilter == "ZERO",
                onClick = { balanceFilter = "ZERO" },
                label = { Text("Settled", fontSize = 11.sp) }
            )
        }

        // Customer Cards List
        if (filteredCustomers.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No ${selectedTab.lowercase().replace("_", " ")} found matching filters.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredCustomers, key = { it.id }) { customer ->
                    PartyCard(
                        customer = customer,
                        supportedMilkTypes = supportedMilkTypes,
                        onEdit = {
                            editingCustomer = customer
                            showManualAddDialog = true
                        },
                        onStatement = { statementCustomer = customer },
                        onDelete = { viewModel.deleteCustomer(customer.id) },
                        onWhatsApp = {
                            val phone = customer.mobile.replace("+", "").replace(" ", "")
                            val msg = "Hello ${customer.name}, your current outstanding balance with MilkMate is ₹${customer.outstandingBalance}. Thank you!"
                            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$phone&text=${Uri.encode(msg)}")
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            } catch (e: Exception) {
                                viewModel.showMessage("WhatsApp not installed")
                            }
                        },
                        onCall = {
                            val uri = Uri.parse("tel:${customer.mobile}")
                            try {
                                context.startActivity(Intent(Intent.ACTION_DIAL, uri))
                            } catch (e: Exception) {
                                viewModel.showMessage("Cannot place call")
                            }
                        },
                        onQuickDeliver = {
                            deliveringCustomer = customer
                        },
                        onQuickCollect = {
                            collectingCustomer = customer
                        }
                    )
                }
            }
        }

        // Profile-driven setup explanation card at bottom
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Profile-driven setup", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "Milk type, pricing method, delivery schedule and payment terms are configured once in each profile.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Modal: Choose Add Manually or Import Contacts
    if (showAddOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showAddOptionsDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        when (selectedTab) {
                            "BULK_BUYER" -> "Add Bulk Buyer"
                            "SUPPLIER" -> "Add Supplier"
                            else -> "Add Individual"
                        },
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { showAddOptionsDialog = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                showAddOptionsDialog = false
                                showManualAddDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Text("✏️ Add Manually", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                showAddOptionsDialog = false
                                showImportContactsDialog = true
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Text("📱 Import Contacts", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        "Add this record specifically to ${selectedTab.replace("_", " ")}. MilkMate will keep Individual, Bulk Buyer and Supplier records separate.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {}
        )
    }

    // Modal: Full Profile Manual Add / Edit
    if (showManualAddDialog) {
        ManualPartyDialog(
            customer = editingCustomer,
            defaultType = selectedTab,
            staffList = staffList.map { it.name },
            supportedMilkTypes = supportedMilkTypes,
            businessMode = currentMode,
            onDismiss = { showManualAddDialog = false },
            onSave = { name, mobile, address, type, rate, milkType, defaultQty, defaultShift, route, notes, cowQty, cowRate, buffQty, buffRate, quickPresets, rateMethod ->
                viewModel.saveCustomer(
                    id = editingCustomer?.id,
                    name = name,
                    mobile = mobile,
                    address = address,
                    type = type,
                    rate = rate,
                    milkType = milkType,
                    defaultQty = defaultQty,
                    defaultShift = defaultShift,
                    route = route,
                    notes = notes,
                    cowQuantity = cowQty,
                    cowRate = cowRate,
                    buffaloQuantity = buffQty,
                    buffaloRate = buffRate,
                    quickPresets = quickPresets,
                    rateMethod = rateMethod
                ) { success, err ->
                    if (success) {
                        showManualAddDialog = false
                    } else {
                        viewModel.showMessage(err ?: "Cannot save party")
                    }
                }
            }
        )
    }

    // Modal: Import Contacts
    if (showImportContactsDialog) {
        ImportContactsDialog(
            defaultType = selectedTab,
            supportedMilkTypes = supportedMilkTypes,
            businessMode = currentMode,
            onDismiss = { showImportContactsDialog = false },
            onImport = { name, phone, type, milkType, qty, rate, route, rateMethod ->
                viewModel.saveCustomer(
                    id = null,
                    name = name,
                    mobile = phone,
                    address = "",
                    type = type,
                    rate = rate,
                    milkType = milkType,
                    defaultQty = qty,
                    defaultShift = "MORNING",
                    route = route,
                    notes = "Imported from Phone Contacts",
                    rateMethod = rateMethod
                ) { success, _ ->
                    if (success) {
                        showImportContactsDialog = false
                    }
                }
            }
        )
    }

    // Modal: Statement
    statementCustomer?.let { cust ->
        CustomerStatementModal(
            customer = cust,
            viewModel = viewModel,
            onDismiss = { statementCustomer = null },
            supportedMilkTypes = supportedMilkTypes
        )
    }

    // Modal: Quick Collect Payment
    collectingCustomer?.let { cust ->
        QuickCollectCustomerModal(
            customer = cust,
            onDismiss = { collectingCustomer = null },
            onSave = { amount, method, ref, notes ->
                viewModel.savePayment(
                    customerId = cust.id,
                    customerName = cust.name,
                    paymentType = if (cust.type == "SUPPLIER") "SUPPLIER_PAYMENT" else "CUSTOMER_PAYMENT",
                    amount = amount,
                    method = method,
                    referenceNo = ref,
                    notes = notes
                )
                collectingCustomer = null
                viewModel.showMessage("Payment of ₹${amount.toInt()} recorded for ${cust.name}")
            }
        )
    }

    // Modal: Quick Deliver Milk
    deliveringCustomer?.let { cust ->
        QuickDeliverCustomerModal(
            customer = cust,
            supportedMilkTypes = supportedMilkTypes,
            onDismiss = { deliveringCustomer = null },
            onSave = { shift, milkType, qty, rate, notes ->
                viewModel.saveDelivery(
                    id = null,
                    customerId = cust.id,
                    customerName = cust.name,
                    customerType = cust.type,
                    date = viewModel.selectedDate.value,
                    shift = shift,
                    milkType = milkType,
                    quantity = qty,
                    fat = 0.0,
                    snf = 0.0,
                    clr = 0.0,
                    customRate = rate,
                    isDelivered = true,
                    notes = notes
                )
                deliveringCustomer = null
                viewModel.showMessage("Recorded $qty L $milkType milk for ${cust.name}")
            }
        )
    }
}

@Composable
fun PartyCard(
    customer: CustomerEntity,
    onEdit: () -> Unit,
    onStatement: () -> Unit,
    onDelete: () -> Unit,
    onWhatsApp: () -> Unit,
    onCall: () -> Unit,
    onQuickDeliver: () -> Unit,
    onQuickCollect: () -> Unit,
    supportedMilkTypes: String = "BOTH"
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(RoyalBlueLight, shape = CircleShape)
                            .clickable(onClick = onCall),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "Call Customer", tint = RoyalBluePrimary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = customer.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = customer.type.replace("_", " "),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        val milkDetails = when {
                            supportedMilkTypes == "COW_ONLY" -> "🐄 Cow Milk • ${customer.cowQuantity} L @ ₹${customer.cowRate}/L"
                            supportedMilkTypes == "BUFFALO_ONLY" -> "🐃 Buffalo Milk • ${customer.buffaloQuantity} L @ ₹${customer.buffaloRate}/L"
                            customer.milkType == "COW" -> "🐄 Cow Milk • ${customer.cowQuantity} L @ ₹${customer.cowRate}/L"
                            customer.milkType == "BUFFALO" -> "🐃 Buffalo Milk • ${customer.buffaloQuantity} L @ ₹${customer.buffaloRate}/L"
                            customer.milkType == "BOTH" -> "🐄 ${customer.cowQuantity}L @ ₹${customer.cowRate} • 🐃 ${customer.buffaloQuantity}L @ ₹${customer.buffaloRate}"
                            else -> "🥛 ${customer.milkType} • ${customer.defaultQuantity} L @ ₹${customer.rate}/L"
                        }
                        Text(
                            text = milkDetails,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Rate calculation method indicator badge
                        val rateMethodBadge = when (customer.rateMethod.uppercase()) {
                            "FAT_SNF" -> "🧪 FAT+SNF Formula Based"
                            "FAT_ONLY" -> "⚖️ FAT-Only Percentage Basis"
                            "PANEER_YIELD" -> "🧀 Paneer Yield Basis"
                            "KHOA_YIELD" -> "🍬 Khoa Yield Basis"
                            "DEFAULT" -> "🏢 Dairy Default Setting"
                            else -> "🏷️ Fixed Flat Rate"
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (customer.rateMethod.uppercase() == "FAT_SNF" || customer.rateMethod.uppercase() == "FAT_ONLY") RoyalBlueLight else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = rateMethodBadge,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (customer.rateMethod.uppercase() == "FAT_SNF" || customer.rateMethod.uppercase() == "FAT_ONLY") RoyalBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    val bal = customer.outstandingBalance
                    Text(
                        text = "₹${Math.abs(bal).toInt()}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = if (bal > 0) DangerRed else if (bal < 0) DairyGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (bal > 0) "⚠️ Due to Pay" else if (bal < 0) "✓ In Advance" else "Settled",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (bal > 0) DangerRed else if (bal < 0) DairyGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = if (customer.route.isNotBlank()) "📍 ${customer.route}" else "📍 Route: Default",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "📱 ${customer.mobile}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick Deliver Button
                FilledTonalButton(
                    onClick = onQuickDeliver,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.weight(1.1f).height(34.dp)
                ) {
                    Text("🥛 Deliver", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Quick Collect Button
                Button(
                    onClick = onQuickCollect,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (customer.outstandingBalance > 0) DangerRed else DairyGreen),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.weight(1.1f).height(34.dp)
                ) {
                    Text(if (customer.type == "SUPPLIER") "💸 Pay" else "💳 Collect", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // WhatsApp
                IconButton(onClick = onWhatsApp, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Chat, contentDescription = "WhatsApp", tint = DairyGreen, modifier = Modifier.size(18.dp))
                }

                // Statement
                IconButton(onClick = onStatement, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = "Statement", tint = RoyalBluePrimary, modifier = Modifier.size(18.dp))
                }

                // Edit
                IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }

                // Delete
                IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DangerRed.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun QuickCollectCustomerModal(
    customer: CustomerEntity,
    onDismiss: () -> Unit,
    onSave: (Double, String, String, String) -> Unit
) {
    var amountText by remember {
        mutableStateOf(if (customer.outstandingBalance > 0) customer.outstandingBalance.toInt().toString() else "500")
    }
    var paymentMethod by remember { mutableStateOf("CASH") } // CASH, UPI, BANK, CHEQUE
    var referenceNo by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(if (customer.type == "SUPPLIER") "Pay Supplier" else "Collect Payment", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(customer.name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = null) }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Outstanding preview
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (customer.outstandingBalance > 0) DangerRed.copy(alpha = 0.1f) else DairyGreen.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Current Balance:", fontSize = 12.sp)
                        Text(
                            "₹${customer.outstandingBalance.toInt()} ${if (customer.outstandingBalance > 0) "(Due)" else "(Advance)"}",
                            fontWeight = FontWeight.Bold,
                            color = if (customer.outstandingBalance > 0) DangerRed else DairyGreen
                        )
                    }
                }

                // Amount Field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Preset Chips
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (customer.outstandingBalance > 0) {
                        FilterChip(
                            selected = amountText == customer.outstandingBalance.toInt().toString(),
                            onClick = { amountText = customer.outstandingBalance.toInt().toString() },
                            label = { Text("Full Due", fontSize = 10.sp) }
                        )
                    }
                    listOf("200", "500", "1000", "2000").forEach { p ->
                        FilterChip(
                            selected = amountText == p,
                            onClick = { amountText = p },
                            label = { Text("₹$p", fontSize = 10.sp) }
                        )
                    }
                }

                // Payment Mode Chips
                Text("Payment Mode *", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("CASH", "UPI", "BANK").forEach { m ->
                        FilterChip(
                            selected = paymentMethod == m,
                            onClick = { paymentMethod = m },
                            label = { Text(m, fontSize = 11.sp, fontWeight = if (paymentMethod == m) FontWeight.Bold else FontWeight.Normal) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = referenceNo,
                    onValueChange = { referenceNo = it },
                    label = { Text("Ref / Transaction ID (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onSave(amt, paymentMethod, referenceNo, notes)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("✓ Confirm & Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun QuickDeliverCustomerModal(
    customer: CustomerEntity,
    onDismiss: () -> Unit,
    supportedMilkTypes: String = "BOTH",
    onSave: (String, String, Double, Double, String) -> Unit
) {
    var shift by remember { mutableStateOf(customer.defaultShift.ifBlank { "MORNING" }) }
    var milkType by remember {
        mutableStateOf(
            if (supportedMilkTypes == "BUFFALO_ONLY") "BUFFALO"
            else if (supportedMilkTypes == "COW_ONLY") "COW"
            else if (customer.milkType == "BOTH") "COW"
            else customer.milkType
        )
    }
    var quantityText by remember {
        mutableStateOf(
            if (milkType == "BUFFALO") (if (customer.buffaloQuantity > 0) customer.buffaloQuantity.toString() else "1.0")
            else (if (customer.cowQuantity > 0) customer.cowQuantity.toString() else "1.0")
        )
    }
    var rateText by remember {
        mutableStateOf(
            if (milkType == "BUFFALO") customer.buffaloRate.toString()
            else customer.cowRate.toString()
        )
    }
    var notes by remember { mutableStateOf("") }

    val qty = quantityText.toDoubleOrNull() ?: 1.0
    val rate = rateText.toDoubleOrNull() ?: 55.0
    val totalAmount = qty * rate

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Record Milk Delivery", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(customer.name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = null) }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Shift Selector
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShiftPill(
                        title = "🌅 Morning",
                        isSelected = shift == "MORNING",
                        onClick = { shift = "MORNING" },
                        modifier = Modifier.weight(1f)
                    )
                    ShiftPill(
                        title = "🌆 Evening",
                        isSelected = shift == "EVENING",
                        onClick = { shift = "EVENING" },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Milk Type Selector (Only if workspace supports Both)
                if (supportedMilkTypes == "BOTH") {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = milkType == "COW",
                            onClick = {
                                milkType = "COW"
                                quantityText = if (customer.cowQuantity > 0) customer.cowQuantity.toString() else "1.0"
                                rateText = customer.cowRate.toString()
                            },
                            label = { Text("🐄 Cow Milk", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = milkType == "BUFFALO",
                            onClick = {
                                milkType = "BUFFALO"
                                quantityText = if (customer.buffaloQuantity > 0) customer.buffaloQuantity.toString() else "1.5"
                                rateText = customer.buffaloRate.toString()
                            },
                            label = { Text("🐃 Buffalo Milk", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Quantity with Stepper
                Text("Quantity (Liters)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledIconButton(
                        onClick = {
                            val current = quantityText.toDoubleOrNull() ?: 1.0
                            if (current > 0.5) quantityText = String.format(Locale.US, "%.1f", current - 0.5)
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    FilledIconButton(
                        onClick = {
                            val current = quantityText.toDoubleOrNull() ?: 1.0
                            quantityText = String.format(Locale.US, "%.1f", current + 0.5)
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Rate & Total Info
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = rateText,
                        onValueChange = { rateText = it },
                        label = { Text("Rate (₹/L)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DairyGreen.copy(alpha = 0.15f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Total Amount", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${totalAmount.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = DairyGreen)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (qty > 0) {
                        onSave(shift, milkType, qty, rate, notes)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("✓ Record Delivery", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ManualPartyDialog(
    customer: CustomerEntity?,
    defaultType: String,
    staffList: List<String>,
    onDismiss: () -> Unit,
    supportedMilkTypes: String = "BOTH",
    businessMode: String = "FARMER",
    onSave: (String, String, String, String, Double, String, Double, String, String, String, Double, Double, Double, Double, String, String) -> Unit
) {
    val context = LocalContext.current
    val showSuppliers = BusinessModeFeatures.showCollectionDesk(businessMode) || businessMode in listOf("TRADER", "COLLECTION_CENTER", "INTEGRATED", "CUSTOM")
    val showIndividual = businessMode != "COLLECTION_CENTER"
    
    val initialType = if (customer != null) {
        customer.type
    } else {
        if (defaultType == "SUPPLIER" && !showSuppliers) "INDIVIDUAL"
        else if (defaultType == "INDIVIDUAL" && !showIndividual) "BULK_BUYER"
        else defaultType
    }

    var partyType by remember { mutableStateOf(initialType) }
    var name by remember { mutableStateOf(customer?.name ?: "") }
    var mobile by remember { mutableStateOf(customer?.mobile ?: "") }
    var areaRoute by remember { mutableStateOf(customer?.route ?: "") }

    // Rate calculation method for this customer
    var rateMethod by remember { mutableStateOf(customer?.rateMethod ?: "FLAT") }
    var showContactPickerSheet by remember { mutableStateOf(false) }

    if (showContactPickerSheet) {
        DeviceContactPickerSheet(
            onDismiss = { showContactPickerSheet = false },
            onContactSelected = { cName, cPhone ->
                if (cName.isNotBlank()) name = cName
                if (cPhone.isNotBlank()) mobile = cPhone
                showContactPickerSheet = false
                Toast.makeText(context, "Contact imported: $cName ($cPhone)", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Milk selection: COW, BUFFALO, BOTH
    val initialMilkChoice = when (supportedMilkTypes) {
        "BUFFALO_ONLY" -> "BUFFALO"
        "COW_ONLY" -> "COW"
        else -> customer?.milkType ?: "COW"
    }
    var selectedMilkChoice by remember {
        mutableStateOf(initialMilkChoice)
    }

    var cowQtyText by remember { mutableStateOf(customer?.cowQuantity?.toString() ?: "1.0") }
    var cowRateText by remember { mutableStateOf(customer?.cowRate?.toString() ?: "55.0") }

    var buffQtyText by remember { mutableStateOf(customer?.buffaloQuantity?.toString() ?: "1.5") }
    var buffRateText by remember { mutableStateOf(customer?.buffaloRate?.toString() ?: "70.0") }

    var defaultShift by remember { mutableStateOf(customer?.defaultShift ?: "MORNING") }
    var assignedStaff by remember { mutableStateOf(customer?.assignedStaffName.takeIf { !it.isNullOrBlank() } ?: "Not assigned") }
    var notes by remember { mutableStateOf(customer?.notes ?: "") }

    // Quick Add Presets
    var quickPresetsText by remember {
        mutableStateOf(customer?.quickPresets ?: "0.5, 1.0, 1.5, 2.0")
    }

    val cowQ = cowQtyText.toDoubleOrNull() ?: 1.0
    val cowR = cowRateText.toDoubleOrNull() ?: 55.0
    val buffQ = buffQtyText.toDoubleOrNull() ?: 1.5
    val buffR = buffRateText.toDoubleOrNull() ?: 70.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (customer == null) "Add Party / Customer" else "Edit Party Profile", fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = null) }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Configure party details and subscribed milk preferences.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                // Party Type Selector (Scoped to active Business Model)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (showIndividual) {
                        FilterChip(
                            selected = partyType == "INDIVIDUAL",
                            onClick = { partyType = "INDIVIDUAL" },
                            label = { Text("👤 Household", fontSize = 10.5.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    FilterChip(
                        selected = partyType == "BULK_BUYER",
                        onClick = { partyType = "BULK_BUYER" },
                        label = { Text(if (businessMode == "PROCESSING_UNIT") "🏢 Merchant" else "🏨 Hotel / B2B", fontSize = 10.5.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    if (showSuppliers) {
                        FilterChip(
                            selected = partyType == "SUPPLIER",
                            onClick = { partyType = "SUPPLIER" },
                            label = { Text(if (businessMode == "TRADER" || businessMode == "COLLECTION_CENTER") "👨‍🌾 Farmer" else "🏢 Supplier", fontSize = 10.5.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Quick Import from Phone Contacts Button
                Button(
                    onClick = { showContactPickerSheet = true },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlueLight, contentColor = RoyalBluePrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Contacts, contentDescription = null, modifier = Modifier.size(18.dp), tint = RoyalBluePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("📱 Select / Auto-Fill from Phone Contacts", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Customer Name / Business *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("Mobile Number *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = areaRoute,
                    onValueChange = { areaRoute = it },
                    label = {
                        Text(
                            when (partyType) {
                                "SUPPLIER" -> "Village / Supply Area"
                                "BULK_BUYER" -> "Business Address / Dispatch Location"
                                else -> "Area / Route / Street"
                            }
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // ================= MILK RATE BASIS SELECTION =================
                HorizontalDivider()
                Text(
                    text = "Milk Rate Calculation Method for this Party *",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = RoyalBluePrimary
                )
                Text(
                    text = "Different customers can have different pricing methods. Select the rate basis for ${name.ifBlank { "this customer" }}:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val rateOptions = listOf(
                        Triple("FLAT", "🏷️ Fixed Flat Rate per Liter", "Fixed price per liter (e.g. ₹55/L Cow, ₹70/L Buffalo) regardless of FAT/SNF"),
                        Triple("FAT_SNF", "🧪 FAT + SNF Formula Based", "Rate calculated dynamically from daily FAT% and SNF% milk testing"),
                        Triple("FAT_ONLY", "⚖️ FAT Only Percentage Basis", "Rate calculated purely from FAT% content (proportional to Fat units)"),
                        Triple("PANEER_YIELD", "🧀 Paneer Yield Basis", "Rate benchmarked to estimated paneer/solids yield per liter"),
                        Triple("DEFAULT", "🏢 Dairy Default Setting", "Inherits standard pricing rule configured in Dairy Settings")
                    )

                    rateOptions.forEach { (code, optTitle, optDesc) ->
                        val isSel = rateMethod.equals(code, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) RoyalBlueLight else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(if (isSel) 1.5.dp else 1.dp, if (isSel) RoyalBluePrimary else BorderSubtle),
                            modifier = Modifier.fillMaxWidth().clickable { rateMethod = code }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSel,
                                    onClick = { rateMethod = code },
                                    colors = RadioButtonDefaults.colors(selectedColor = RoyalBluePrimary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(optTitle, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isSel) RoyalBluePrimary else MaterialTheme.colorScheme.onSurface)
                                    Text(optDesc, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                if (rateMethod != "FLAT") {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = FreshGoldLight,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Science, contentDescription = null, tint = WarmHoney, modifier = Modifier.size(18.dp))
                            Text(
                                "⚡ Dynamic Pricing Enabled: When recording milk entry for this customer, the final rate will be calculated automatically based on testing parameters.",
                                fontSize = 11.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }

                // Milk Type Selector
                if (supportedMilkTypes == "BOTH") {
                    Text(
                        when (partyType) {
                            "SUPPLIER" -> "Supplied Milk Type *"
                            "BULK_BUYER" -> "Contracted Milk Type *"
                            else -> "Select Subscribed Milk Type *"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = RoyalBluePrimary
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = selectedMilkChoice == "COW",
                            onClick = { selectedMilkChoice = "COW" },
                            label = { Text("🐄 Cow Only", fontSize = 11.sp, fontWeight = if (selectedMilkChoice == "COW") FontWeight.Bold else FontWeight.Normal) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedMilkChoice == "BUFFALO",
                            onClick = { selectedMilkChoice = "BUFFALO" },
                            label = { Text("🐃 Buffalo Only", fontSize = 11.sp, fontWeight = if (selectedMilkChoice == "BUFFALO") FontWeight.Bold else FontWeight.Normal) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedMilkChoice == "BOTH",
                            onClick = { selectedMilkChoice = "BOTH" },
                            label = { Text("🥛 Both (Dual)", fontSize = 11.sp, fontWeight = if (selectedMilkChoice == "BOTH") FontWeight.Bold else FontWeight.Normal) },
                            modifier = Modifier.weight(1.1f)
                        )
                    }
                }

                // Section 1: Cow Milk Details (If COW or BOTH)
                if (supportedMilkTypes != "BUFFALO_ONLY" && (selectedMilkChoice == "COW" || selectedMilkChoice == "BOTH")) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    when (partyType) {
                                        "SUPPLIER" -> "🐄 Cow Milk Purchase Rate"
                                        "BULK_BUYER" -> "🐄 Cow Milk Bulk Contract"
                                        else -> "🐄 Cow Milk Subscription"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = RoyalBluePrimary
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text("Subtotal: ₹${(cowQ * cowR).toInt()}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = DairyGreen)
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = cowQtyText,
                                    onValueChange = { cowQtyText = it },
                                    label = { Text(if (partyType == "SUPPLIER") "Est. Daily Supply (L)" else "Cow Qty (L)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = cowRateText,
                                    onValueChange = { cowRateText = it },
                                    label = { Text(if (rateMethod == "FLAT") "Fixed Rate (₹/L)" else "Base Rate (₹/L)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Presets for Cow (Only for Individual household customers)
                            if (partyType == "INDIVIDUAL") {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("0.5", "1.0", "1.5", "2.0").forEach { p ->
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (cowQtyText == p) RoyalBluePrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                            modifier = Modifier.clickable { cowQtyText = p }.padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("$p L", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 2: Buffalo Milk Details (If BUFFALO or BOTH)
                if (supportedMilkTypes != "COW_ONLY" && (selectedMilkChoice == "BUFFALO" || selectedMilkChoice == "BOTH")) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    when (partyType) {
                                        "SUPPLIER" -> "🐃 Buffalo Milk Purchase Rate"
                                        "BULK_BUYER" -> "🐃 Buffalo Milk Bulk Contract"
                                        else -> "🐃 Buffalo Milk Subscription"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF6D4C41)
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text("Subtotal: ₹${(buffQ * buffR).toInt()}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = DairyGreen)
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = buffQtyText,
                                    onValueChange = { buffQtyText = it },
                                    label = { Text(if (partyType == "SUPPLIER") "Est. Daily Supply (L)" else "Buff Qty (L)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = buffRateText,
                                    onValueChange = { buffRateText = it },
                                    label = { Text(if (rateMethod == "FLAT") "Fixed Rate (₹/L)" else "Base Rate (₹/L)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Presets for Buffalo (Only for Individual household customers)
                            if (partyType == "INDIVIDUAL") {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("0.5", "1.0", "1.5", "2.0").forEach { p ->
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (buffQtyText == p) Color(0xFF6D4C41).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                            modifier = Modifier.clickable { buffQtyText = p }.padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("$p L", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Daily Summary Banner when BOTH
                if (selectedMilkChoice == "BOTH") {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DairyGreen.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🥛 Combined Daily Total:\n${String.format(Locale.US, "%.1f", cowQ + buffQ)} Liters (${cowQ}L Cow + ${buffQ}L Buff)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "₹${(cowQ * cowR + buffQ * buffR).toInt()} / day",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = DairyGreen
                            )
                        }
                    }
                }

                // ================= TYPE-SPECIFIC SECTIONS =================

                // 1. QUICK ADD PRESETS SECTION (Only for INDIVIDUAL Household Customers)
                if (partyType == "INDIVIDUAL") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = FreshGold.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.FlashOn, contentDescription = null, tint = FreshGold, modifier = Modifier.size(16.dp))
                                Text("⚡ Delivery 'Quick Add' Presets", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RoyalBluePrimary)
                            }
                            Text(
                                "Define frequently delivered quantities for 1-tap logging in the delivery route sheet.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            val commonSizes = listOf(0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 2.0, 2.5, 3.0, 5.0)
                            val currentList = quickPresetsText.split(",")
                                .mapNotNull { it.trim().toDoubleOrNull() }
                                .toSet()

                            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                commonSizes.forEach { size ->
                                    val isSelected = currentList.contains(size)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            val updated = if (isSelected) {
                                                currentList.filter { it != size }.sorted()
                                            } else {
                                                (currentList + size).sorted()
                                            }
                                            quickPresetsText = updated.joinToString(", ")
                                        },
                                        label = { Text("${size}L", fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = quickPresetsText,
                                onValueChange = { quickPresetsText = it },
                                label = { Text("Custom Presets (comma-separated Liters)") },
                                placeholder = { Text("e.g. 0.5, 1.0, 1.5, 2.0") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // 2. DELIVERY SHIFT SELECTOR (For INDIVIDUAL & BULK_BUYER)
                if (partyType != "SUPPLIER") {
                    Text("Delivery Shift Schedule *", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = defaultShift == "MORNING",
                            onClick = { defaultShift = "MORNING" },
                            label = { Text("☀️ Morning", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = defaultShift == "EVENING",
                            onClick = { defaultShift = "EVENING" },
                            label = { Text("🌙 Evening", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = defaultShift == "BOTH",
                            onClick = { defaultShift = "BOTH" },
                            label = { Text("🔄 Both", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = {
                        Text(
                            when (partyType) {
                                "SUPPLIER" -> "Procurement Notes / Payment Terms"
                                "BULK_BUYER" -> "Contract Terms / GSTIN / FSSAI (Optional)"
                                else -> "Notes / Delivery Instructions"
                            }
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val effectiveRate = when (selectedMilkChoice) {
                            "COW" -> cowR
                            "BUFFALO" -> buffR
                            "BOTH" -> if (cowQ + buffQ > 0) (cowQ * cowR + buffQ * buffR) / (cowQ + buffQ) else cowR
                            else -> cowR
                        }
                        val effectiveQty = when (selectedMilkChoice) {
                            "COW" -> cowQ
                            "BUFFALO" -> buffQ
                            "BOTH" -> cowQ + buffQ
                            else -> cowQ
                        }

                        onSave(
                            name,
                            mobile,
                            areaRoute,
                            partyType,
                            effectiveRate,
                            selectedMilkChoice,
                            effectiveQty,
                            defaultShift,
                            areaRoute,
                            notes,
                            cowQ,
                            cowR,
                            buffQ,
                            buffR,
                            quickPresetsText.trim().ifBlank { "0.5,1.0,1.5,2.0" },
                            rateMethod
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                enabled = name.isNotBlank()
            ) {
                Text(if (customer == null) "✓ Save Party" else "✓ Save Changes")
            }
        }
    )
}

/**
 * Data item representing a contact on the user's device
 */
data class DeviceContactItem(
    val id: String,
    val name: String,
    val phone: String
)

/**
 * Robust in-app Device Contact Picker with Search, Permissions, Native Picker, & Demo Contacts
 */
@Composable
fun DeviceContactPickerSheet(
    onDismiss: () -> Unit,
    onContactSelected: (name: String, phone: String) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var contactsList by remember { mutableStateOf<List<DeviceContactItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Native System Contact Picker fallback
    val systemContactPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val contactUri = result.data?.data ?: return@rememberLauncherForActivityResult
            val (cName, cPhone) = extractContactNameAndPhone(context, contactUri)
            if (cName.isNotBlank() || cPhone.isNotBlank()) {
                onContactSelected(cName, cPhone)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Contact permission needed to list address book contacts", Toast.LENGTH_SHORT).show()
        }
    }

    // Load contacts when permission is granted
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            isLoading = true
            try {
                val list = mutableListOf<DeviceContactItem>()
                val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                val projection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                )
                context.contentResolver.query(
                    uri,
                    projection,
                    null,
                    null,
                    "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                    val nameCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val phoneCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                    val seenPhones = mutableSetOf<String>()
                    while (cursor.moveToNext()) {
                        val id = if (idCol != -1) cursor.getString(idCol) ?: "" else ""
                        val name = if (nameCol != -1) cursor.getString(nameCol) ?: "" else ""
                        val rawPhone = if (phoneCol != -1) cursor.getString(phoneCol) ?: "" else ""
                        var clean = rawPhone.replace(Regex("[^0-9+]"), "").trim()
                        if (clean.startsWith("+91") && clean.length == 13) clean = clean.substring(3)
                        else if (clean.startsWith("0") && clean.length == 11) clean = clean.substring(1)
                        else if (clean.startsWith("+") && clean.length > 10) clean = clean.replace("+", "")

                        if (name.isNotBlank() && clean.isNotBlank() && seenPhones.add(clean)) {
                            list.add(DeviceContactItem(id, name, clean))
                        }
                    }
                }
                contactsList = list
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    // Quick dairy sample contacts for testing on emulators or empty contact books
    val sampleContacts = listOf(
        DeviceContactItem("s-1", "Ramesh Patel (Dairy Farmer)", "9876543210"),
        DeviceContactItem("s-2", "Sunita Sharma (Household)", "9823456789"),
        DeviceContactItem("s-3", "Gopal Krishna (Milk Vendor)", "9811223344"),
        DeviceContactItem("s-4", "Vijay Kumar (Hotel Swagat)", "9876501234"),
        DeviceContactItem("s-5", "Anand Samal (Milk Delivery)", "9845678901")
    )

    val filteredContacts = remember(searchQuery, contactsList) {
        if (searchQuery.isBlank()) {
            contactsList
        } else {
            val q = searchQuery.trim().lowercase()
            contactsList.filter { it.name.lowercase().contains(q) || it.phone.contains(q) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = CircleShape, color = RoyalBlueLight, modifier = Modifier.size(36.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Contacts, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(20.dp))
                        }
                    }
                    Column {
                        Text("Select Contact", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Import name & phone directly", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = null) }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Native System Contact App Button
                OutlinedButton(
                    onClick = {
                        try {
                            val pickIntent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                            systemContactPicker.launch(pickIntent)
                        } catch (e: Exception) {
                            try {
                                val pickIntent2 = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
                                systemContactPicker.launch(pickIntent2)
                            } catch (e2: Exception) {
                                Toast.makeText(context, "Could not open system contacts app", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(15.dp), tint = RoyalBluePrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Native Contacts App", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                }

                if (!hasPermission) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Allow MilkMate to access contacts for in-app search & 1-tap importing:",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                                modifier = Modifier.fillMaxWidth().height(36.dp)
                            ) {
                                Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Grant Contact Permission", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by name or mobile...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                    }
                } else if (hasPermission && contactsList.isNotEmpty()) {
                    Text(
                        "Found ${filteredContacts.size} contacts on device:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredContacts) { contact ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, BorderSubtle),
                                modifier = Modifier.fillMaxWidth().clickable {
                                    onContactSelected(contact.name, contact.phone)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(shape = CircleShape, color = RoyalBlueLight, modifier = Modifier.size(32.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = contact.name.take(1).uppercase(),
                                                fontWeight = FontWeight.Bold,
                                                color = RoyalBluePrimary,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(contact.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("📱 ${contact.phone}", fontSize = 11.sp, color = DairyGreen, fontWeight = FontWeight.SemiBold)
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                } else {
                    // No contacts or Permission not granted yet -> Show Quick Sample Contacts
                    Text(
                        "Quick Pick Sample Contacts (1-Tap Test):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoyalBluePrimary
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(sampleContacts) { contact ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, BorderSubtle),
                                modifier = Modifier.fillMaxWidth().clickable {
                                    onContactSelected(contact.name, contact.phone)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(shape = CircleShape, color = FreshGoldLight, modifier = Modifier.size(32.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = contact.name.take(1).uppercase(),
                                                fontWeight = FontWeight.Bold,
                                                color = WarmHoney,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(contact.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("📱 ${contact.phone}", fontSize = 11.sp, color = DairyGreen, fontWeight = FontWeight.SemiBold)
                                    }
                                    Surface(shape = RoundedCornerShape(4.dp), color = DairyGreenLight) {
                                        Text("TAP TO ADD", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = DairyGreen, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

/**
 * Robustly extracts contact Display Name and Mobile Number from any Contact URI
 * (supports Phone URI, Contacts URI, lookup URI, and different Android OEM contact providers)
 */
fun extractContactNameAndPhone(context: Context, contactUri: Uri): Pair<String, String> {
    var contactName = ""
    var contactPhone = ""

    try {
        // 1. Query the picked Uri directly
        context.contentResolver.query(contactUri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                // Find display name
                val nameColCandidates = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                    "display_name",
                    "name"
                )
                for (col in nameColCandidates) {
                    val idx = cursor.getColumnIndex(col)
                    if (idx != -1) {
                        val v = cursor.getString(idx)
                        if (!v.isNullOrBlank()) {
                            contactName = v
                            break
                        }
                    }
                }

                // Find phone number directly in cursor
                val phoneColCandidates = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER,
                    "data1",
                    "data4",
                    "phone_number",
                    "number"
                )
                for (col in phoneColCandidates) {
                    val idx = cursor.getColumnIndex(col)
                    if (idx != -1) {
                        val v = cursor.getString(idx)
                        if (!v.isNullOrBlank()) {
                            contactPhone = v
                            break
                        }
                    }
                }

                // If phone is still blank, retrieve contact ID and query CommonDataKinds.Phone table
                if (contactPhone.isBlank()) {
                    val idColCandidates = arrayOf(
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                        ContactsContract.Contacts._ID,
                        "_id"
                    )
                    var contactId: String? = null
                    for (col in idColCandidates) {
                        val idx = cursor.getColumnIndex(col)
                        if (idx != -1) {
                            val v = cursor.getString(idx)
                            if (!v.isNullOrBlank()) {
                                contactId = v
                                break
                            }
                        }
                    }

                    if (!contactId.isNullOrBlank()) {
                        context.contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            arrayOf(
                                ContactsContract.CommonDataKinds.Phone.NUMBER,
                                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                            ),
                            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                            arrayOf(contactId),
                            null
                        )?.use { pCursor ->
                            if (pCursor.moveToFirst()) {
                                val numIdx = pCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                if (numIdx != -1) {
                                    val num = pCursor.getString(numIdx)
                                    if (!num.isNullOrBlank()) contactPhone = num
                                }
                                if (contactName.isBlank()) {
                                    val nameIdx = pCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                                    if (nameIdx != -1) {
                                        val nm = pCursor.getString(nameIdx)
                                        if (!nm.isNullOrBlank()) contactName = nm
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    // Fallback: If contactPhone is still blank, try lookup by URI lastPathSegment
    if (contactPhone.isBlank()) {
        try {
            val contactId = contactUri.lastPathSegment
            if (!contactId.isNullOrBlank()) {
                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                    ),
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ? OR ${ContactsContract.CommonDataKinds.Phone._ID} = ?",
                    arrayOf(contactId, contactId),
                    null
                )?.use { pCursor ->
                    if (pCursor.moveToFirst()) {
                        val numIdx = pCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        if (numIdx != -1) {
                            val num = pCursor.getString(numIdx)
                            if (!num.isNullOrBlank()) contactPhone = num
                        }
                        if (contactName.isBlank()) {
                            val nameIdx = pCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                            if (nameIdx != -1) {
                                val nm = pCursor.getString(nameIdx)
                                if (!nm.isNullOrBlank()) contactName = nm
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Clean up phone number: remove non-digits (keeping + if international)
    var cleanPhone = contactPhone.replace(Regex("[^0-9+]"), "").trim()
    if (cleanPhone.startsWith("+91") && cleanPhone.length == 13) {
        cleanPhone = cleanPhone.substring(3)
    } else if (cleanPhone.startsWith("0") && cleanPhone.length == 11) {
        cleanPhone = cleanPhone.substring(1)
    } else if (cleanPhone.startsWith("+") && cleanPhone.length > 10) {
        cleanPhone = cleanPhone.replace("+", "")
    }

    return Pair(contactName.trim(), cleanPhone.trim())
}

@Composable
fun ImportContactsDialog(
    defaultType: String,
    supportedMilkTypes: String = "BOTH",
    businessMode: String = "FARMER",
    onDismiss: () -> Unit,
    onImport: (name: String, phone: String, type: String, milkType: String, qty: Double, rate: Double, route: String, rateMethod: String) -> Unit
) {
    val context = LocalContext.current
    val showSuppliers = BusinessModeFeatures.showCollectionDesk(businessMode) || businessMode in listOf("TRADER", "COLLECTION_CENTER", "INTEGRATED", "CUSTOM")
    val showIndividual = businessMode != "COLLECTION_CENTER"

    val initialType = if (defaultType == "SUPPLIER" && !showSuppliers) "INDIVIDUAL"
        else if (defaultType == "INDIVIDUAL" && !showIndividual) "BULK_BUYER"
        else defaultType

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var partyType by remember { mutableStateOf(initialType) }
    var rateMethod by remember { mutableStateOf("FLAT") }
    var milkType by remember {
        mutableStateOf(
            when (supportedMilkTypes) {
                "BUFFALO_ONLY" -> "BUFFALO"
                "COW_ONLY" -> "COW"
                else -> "COW"
            }
        )
    }
    var qtyText by remember { mutableStateOf("1.0") }
    var rateText by remember {
        mutableStateOf(if (milkType == "BUFFALO") "70.0" else "55.0")
    }
    var route by remember { mutableStateOf("") }
    var showContactPickerSheet by remember { mutableStateOf(false) }

    if (showContactPickerSheet) {
        DeviceContactPickerSheet(
            onDismiss = { showContactPickerSheet = false },
            onContactSelected = { cName, cPhone ->
                if (cName.isNotBlank()) name = cName
                if (cPhone.isNotBlank()) phone = cPhone
                showContactPickerSheet = false
                Toast.makeText(context, "Contact linked: $cName", Toast.LENGTH_SHORT).show()
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DairyGreenLight,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Contacts, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(20.dp))
                        }
                    }
                    Column {
                        Text("Import Phone Contact", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Save name & mobile directly", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
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
                // Select from Phonebook Button
                Button(
                    onClick = { showContactPickerSheet = true },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (phone.isBlank()) RoyalBluePrimary else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        Icons.Default.Contacts,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (phone.isBlank()) Color.White else RoyalBluePrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (phone.isBlank()) "📱 Select / Browse Address Book" else "🔄 Choose Different Contact",
                        fontWeight = FontWeight.Bold,
                        color = if (phone.isBlank()) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }

                if (phone.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DairyGreenLight,
                        border = BorderStroke(1.dp, DairyGreen.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(16.dp))
                            Text(
                                "Contact Linked: $name ($phone)",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DairyGreen
                            )
                        }
                    }
                }

                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Customer / Party Name *") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = RoyalBluePrimary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Mobile field
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Mobile Number (Directly Saved) *") },
                    placeholder = { Text("10-digit mobile number") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = DairyGreen) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Rate calculation method for this customer
                Text("Milk Rate Basis for this Customer", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = rateMethod == "FLAT",
                        onClick = { rateMethod = "FLAT" },
                        label = { Text("🏷️ Flat Rate", fontSize = 10.5.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = rateMethod == "FAT_SNF",
                        onClick = { rateMethod = "FAT_SNF" },
                        label = { Text("🧪 FAT+SNF", fontSize = 10.5.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = rateMethod == "FAT_ONLY",
                        onClick = { rateMethod = "FAT_ONLY" },
                        label = { Text("⚖️ FAT Only", fontSize = 10.5.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Party Type
                Text("Party Category", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (showIndividual) {
                        FilterChip(
                            selected = partyType == "INDIVIDUAL",
                            onClick = { partyType = "INDIVIDUAL" },
                            label = { Text("👤 Household", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    FilterChip(
                        selected = partyType == "BULK_BUYER",
                        onClick = { partyType = "BULK_BUYER" },
                        label = { Text(if (businessMode == "PROCESSING_UNIT") "🏢 Merchant" else "🏨 Bulk / Hotel", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    if (showSuppliers) {
                        FilterChip(
                            selected = partyType == "SUPPLIER",
                            onClick = { partyType = "SUPPLIER" },
                            label = { Text(if (businessMode == "TRADER" || businessMode == "COLLECTION_CENTER") "👨‍🌾 Farmer" else "🏢 Supplier", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Milk Choice
                Text("Milk Type & Default Rate", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (supportedMilkTypes != "BUFFALO_ONLY") {
                        FilterChip(
                            selected = milkType == "COW",
                            onClick = {
                                milkType = "COW"
                                if (rateText == "70.0" || rateText.isBlank()) rateText = "55.0"
                            },
                            label = { Text("🐄 Cow Milk", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (supportedMilkTypes != "COW_ONLY") {
                        FilterChip(
                            selected = milkType == "BUFFALO",
                            onClick = {
                                milkType = "BUFFALO"
                                if (rateText == "55.0" || rateText.isBlank()) rateText = "70.0"
                            },
                            label = { Text("🐃 Buffalo", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it },
                        label = { Text("Daily Qty (L)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = rateText,
                        onValueChange = { rateText = it },
                        label = { Text(if (rateMethod == "FLAT") "Rate (₹/L)" else "Base Rate (₹/L)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = route,
                    onValueChange = { route = it },
                    label = { Text("Route / Area (Optional)") },
                    leadingIcon = { Icon(Icons.Default.Route, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val q = qtyText.toDoubleOrNull() ?: 1.0
                        val r = rateText.toDoubleOrNull() ?: (if (milkType == "BUFFALO") 70.0 else 55.0)
                        onImport(name.trim(), phone.trim(), partyType, milkType, q, r, route.trim(), rateMethod)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("✓ Save Customer Directly", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CustomerStatementModal(
    customer: CustomerEntity,
    viewModel: MilkMateViewModel,
    onDismiss: () -> Unit,
    supportedMilkTypes: String = "BOTH"
) {
    val context = LocalContext.current
    val session by viewModel.sessionState.collectAsStateWithLifecycle()
    var showPaymentForm by remember { mutableStateOf(false) }
    var payAmountText by remember { mutableStateOf(if (customer.outstandingBalance > 0) customer.outstandingBalance.toInt().toString() else "500") }
    var payMethod by remember { mutableStateOf("UPI") }

    val displayMilkType = when {
        supportedMilkTypes == "COW_ONLY" -> "Cow"
        supportedMilkTypes == "BUFFALO_ONLY" -> "Buffalo"
        customer.milkType == "COW" -> "Cow"
        customer.milkType == "BUFFALO" -> "Buffalo"
        customer.milkType == "BOTH" -> "Dual"
        else -> customer.milkType
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = customer.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(
                        text = "Khata & Monthly Passbook • ${customer.type.replace("_", " ")}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Balance Highlight Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (customer.outstandingBalance > 0) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (customer.outstandingBalance > 0) "Total Due from Customer" else "Balance Settled / Advance",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format(Locale.US, "₹%.2f", customer.outstandingBalance),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (customer.outstandingBalance > 0) DangerRed else DairyGreen
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (customer.outstandingBalance > 0) DangerRed else DairyGreen
                        ) {
                            Text(
                                text = if (customer.outstandingBalance > 0) "PAYMENT DUE" else "ALL CLEAR",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Customer Details summary
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Phone Number:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = customer.mobile, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Milk Type & Rate:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "$displayMilkType • ₹${customer.rate}/L", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Default Daily Order:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "${customer.defaultQuantity} L (${customer.defaultShift})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        if (customer.route.isNotBlank()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "Route / Address:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "📍 ${customer.route}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                            }
                        }
                    }
                }

                // Quick Action Buttons (Call, WhatsApp Bill, Record Payment)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.mobile.replace(" ", "")}"))
                            try { context.startActivity(intent) } catch (_: Exception) {}
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Call", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val phone = customer.mobile.replace("+", "").replace(" ", "")
                            val dueText = if (customer.outstandingBalance > 0) "Pending Due Amount: ₹${customer.outstandingBalance.toInt()}" else "Account is Settled"
                            val upiText = if (session.upiId.isNotBlank()) "💳 *Pay via UPI*: ${session.upiId}" else "Please make payment via UPI or Cash."
                            val greeting = session.billNote.ifBlank { "Thank you for choosing our dairy!" }
                            val msg = """
                                🥛 *MilkMate Dairy Bill* 🥛
                                Hello ${customer.name},
                                
                                Your current milk account status:
                                • Milk Type: ${customer.milkType} Milk
                                • Rate: ₹${customer.rate}/Liter
                                • Daily Supply: ${customer.defaultQuantity} L
                                • *$dueText*
                                
                                $upiText
                                $greeting
                            """.trimIndent()

                            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$phone&text=${Uri.encode(msg)}")
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            } catch (e: Exception) {
                                viewModel.showMessage("WhatsApp not installed")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.3f).height(40.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WhatsApp Bill", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Quick Payment Entry Section
                if (!showPaymentForm) {
                    OutlinedButton(
                        onClick = { showPaymentForm = true },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("💵 Record Payment Received", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Record Payment from ${customer.name}", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                            OutlinedTextField(
                                value = payAmountText,
                                onValueChange = { payAmountText = it },
                                label = { Text("Amount Received (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("UPI", "CASH", "BANK").forEach { method ->
                                    FilterChip(
                                        selected = payMethod == method,
                                        onClick = { payMethod = method },
                                        label = { Text(method, fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(
                                    onClick = { showPaymentForm = false },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancel")
                                }

                                Button(
                                    onClick = {
                                        val amt = payAmountText.toDoubleOrNull() ?: 0.0
                                        if (amt > 0) {
                                            viewModel.savePayment(
                                                customerId = customer.id,
                                                customerName = customer.name,
                                                paymentType = "RECEIVED",
                                                amount = amt,
                                                method = payMethod,
                                                referenceNo = "",
                                                notes = "Received from Customer Khata"
                                            )
                                            showPaymentForm = false
                                            onDismiss()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1.5f)
                                ) {
                                    Text("✓ Save Payment", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}
