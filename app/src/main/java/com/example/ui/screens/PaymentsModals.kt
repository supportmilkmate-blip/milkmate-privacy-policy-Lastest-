package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.DeliveryEntity
import com.example.data.local.entity.PaymentEntity
import com.example.ui.MilkMateViewModel
import com.example.ui.theme.*
import com.example.ui.util.QrCodeView
import com.example.ui.util.ReportExportHelper
import java.text.SimpleDateFormat
import java.util.*

/**
 * Advanced Receive Payment Modal with smart presets, live projected balance,
 * payment method selection, date selector, and automated receipt generation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedReceivePaymentModal(
    preselectedCustomer: CustomerEntity?,
    allCustomers: List<CustomerEntity>,
    onDismiss: () -> Unit,
    onSave: (CustomerEntity, Double, String, String, String, Long, Boolean) -> Unit
) {
    val context = LocalContext.current
    var selectedCustomer by remember { mutableStateOf(preselectedCustomer ?: allCustomers.firstOrNull { it.type != "SUPPLIER" }) }
    var customerDropdownExpanded by remember { mutableStateOf(false) }

    val currentBal = selectedCustomer?.outstandingBalance ?: 0.0
    var amountText by remember(selectedCustomer) {
        val defaultAmt = if (currentBal > 0) currentBal.toInt().toString() else "500"
        mutableStateOf(defaultAmt)
    }

    var selectedMethod by remember { mutableStateOf("UPI") } // UPI, CASH, BANK, CHEQUE
    var referenceNo by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var paymentDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var autoSendWhatsApp by remember { mutableStateOf(true) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val projectedBalance = currentBal - parsedAmount

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(DairyGreenLight, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Receive Payment", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("Record receipt & update khata", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
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
                // Customer Selector
                if (preselectedCustomer == null) {
                    ExposedDropdownMenuBox(
                        expanded = customerDropdownExpanded,
                        onExpandedChange = { customerDropdownExpanded = !customerDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCustomer?.name ?: "Select Customer",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Customer *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = customerDropdownExpanded,
                            onDismissRequest = { customerDropdownExpanded = false }
                        ) {
                            allCustomers.filter { it.type != "SUPPLIER" }.forEach { cust ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(cust.name, fontWeight = FontWeight.Medium)
                                            Text(
                                                text = if (cust.outstandingBalance > 0) "Due: ₹${cust.outstandingBalance.toInt()}" else if (cust.outstandingBalance < 0) "Adv: ₹${Math.abs(cust.outstandingBalance).toInt()}" else "Settled",
                                                fontSize = 11.sp,
                                                color = if (cust.outstandingBalance > 0) DangerRed else DairyGreen
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedCustomer = cust
                                        customerDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // Preselected Customer Card
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(selectedCustomer?.name ?: "", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(
                                    text = "${selectedCustomer?.mobile ?: "No phone"} • ${selectedCustomer?.milkType ?: "Cow"} Milk",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (currentBal > 0) DangerRedLight else DairyGreenLight
                            ) {
                                Text(
                                    text = if (currentBal > 0) "Due: ₹${currentBal.toInt()}" else if (currentBal < 0) "Adv: ₹${Math.abs(currentBal).toInt()}" else "Settled",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentBal > 0) DangerRed else DairyGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Balance Projection Banner
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (projectedBalance > 0) DangerRed.copy(alpha = 0.08f) else DairyGreen.copy(alpha = 0.08f),
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
                            Text("Current Due:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${currentBal.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Projected After Payment:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = if (projectedBalance > 0) "₹${projectedBalance.toInt()} Due" else if (projectedBalance < 0) "₹${Math.abs(projectedBalance).toInt()} in Advance 💎" else "₹0 (Fully Settled) ✅",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (projectedBalance > 0) DangerRed else DairyGreen
                            )
                        }
                    }
                }

                // Amount Field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount Received (₹) *") },
                    leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = RoyalBluePrimary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Preset Chips
                Text("Quick Amount Presets", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (currentBal > 0) {
                        FilterChip(
                            selected = amountText == currentBal.toInt().toString(),
                            onClick = { amountText = currentBal.toInt().toString() },
                            label = { Text("Full Due (₹${currentBal.toInt()})", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DangerRedLight,
                                selectedLabelColor = DangerRed
                            )
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

                // Payment Method
                Text("Payment Method *", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("UPI" to "📱 UPI", "CASH" to "💵 Cash", "BANK" to "🏦 Bank", "CHEQUE" to "📝 Cheque").forEach { (method, label) ->
                        FilterChip(
                            selected = selectedMethod == method,
                            onClick = { selectedMethod = method },
                            label = { Text(label, fontSize = 11.sp, fontWeight = if (selectedMethod == method) FontWeight.Bold else FontWeight.Normal) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Date Picker Row
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val cal = Calendar.getInstance().apply { timeInMillis = paymentDateMillis }
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val newCal = Calendar.getInstance().apply {
                                        set(year, month, dayOfMonth)
                                    }
                                    paymentDateMillis = newCal.timeInMillis
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp), tint = RoyalBluePrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Payment Date:", fontSize = 12.sp)
                        }
                        Text(dateFormat.format(Date(paymentDateMillis)), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RoyalBluePrimary)
                    }
                }

                // Reference / UTR
                OutlinedTextField(
                    value = referenceNo,
                    onValueChange = { referenceNo = it },
                    label = { Text(if (selectedMethod == "UPI") "UPI UTR / Reference ID" else if (selectedMethod == "CHEQUE") "Cheque Number" else "Reference No (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Note / Remarks (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // WhatsApp Checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { autoSendWhatsApp = !autoSendWhatsApp },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = autoSendWhatsApp,
                        onCheckedChange = { autoSendWhatsApp = it },
                        colors = CheckboxDefaults.colors(checkedColor = DairyGreen)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Auto-open WhatsApp receipt after saving", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cust = selectedCustomer ?: return@Button
                    if (parsedAmount > 0) {
                        onSave(cust, parsedAmount, selectedMethod, referenceNo, notes, paymentDateMillis, autoSendWhatsApp)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                enabled = selectedCustomer != null && parsedAmount > 0
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("✓ Save Receipt (₹${parsedAmount.toInt()})", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Dynamic Bharat UPI QR Code Dialog with pre-encoded Amount, VPA, and Party Name.
 */
@Composable
fun DynamicUpiQrDialog(
    businessName: String,
    upiId: String,
    customer: CustomerEntity?,
    defaultAmount: Double?,
    onDismiss: () -> Unit,
    onConfigureUpi: () -> Unit
) {
    val context = LocalContext.current
    var customAmountText by remember {
        mutableStateOf(if (defaultAmount != null && defaultAmount > 0) defaultAmount.toInt().toString() else "")
    }

    val parsedAmount = customAmountText.toDoubleOrNull()
    val note = if (customer != null) "Milk bill - ${customer.name}" else "Dairy payment"

    // Construct standard UPI URL
    val upiUrl = remember(upiId, businessName, parsedAmount, note) {
        if (upiId.isBlank()) ""
        else {
            val amountParam = if (parsedAmount != null && parsedAmount > 0) "&am=${String.format(Locale.US, "%.2f", parsedAmount)}" else ""
            "upi://pay?pa=${Uri.encode(upiId)}&pn=${Uri.encode(businessName)}&tn=${Uri.encode(note)}$amountParam&cu=INR"
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("📲 Scan & Pay via UPI", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(businessName.ifBlank { "MilkMate Dairy" }, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                if (upiId.isBlank()) {
                    // No UPI ID Warning
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFFF3E0),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(32.dp))
                            Text("No UPI ID Configured", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFE65100))
                            Text(
                                "Add your Google Pay / PhonePe UPI ID in Settings to start receiving instant payments.",
                                fontSize = 12.sp,
                                color = Color(0xFF5D4037)
                            )
                            Button(
                                onClick = onConfigureUpi,
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Configure UPI ID Now")
                            }
                        }
                    }
                } else {
                    // Amount Input
                    OutlinedTextField(
                        value = customAmountText,
                        onValueChange = { customAmountText = it },
                        label = { Text("Payment Amount (₹)") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = RoyalBluePrimary) },
                        trailingIcon = {
                            if (customer != null && customer.outstandingBalance > 0) {
                                TextButton(onClick = { customAmountText = customer.outstandingBalance.toInt().toString() }) {
                                    Text("Full Due", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Sharp Crisp Vector QR Code
                    Box(
                        modifier = Modifier
                            .background(Color.White, shape = RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        QrCodeView(
                            content = upiUrl,
                            size = 200.dp,
                            darkColor = Color(0xFF1B5E20)
                        )
                    }

                    // UPI ID Badge & Copy
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = RoyalBlueLight,
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
                                Text("UPI ID", fontSize = 10.sp, color = RoyalBluePrimary)
                                Text(upiId, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RoyalBluePrimary)
                            }
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("UPI ID", upiId))
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = RoyalBluePrimary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // Supported Apps Pill
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Accepts:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("GPay • PhonePe • Paytm • BHIM • Cred", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = RoyalBluePrimary)
                    }

                    // Share Buttons
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val sendIntent = Intent(Intent.ACTION_VIEW, Uri.parse(upiUrl))
                                try { context.startActivity(sendIntent) } catch (_: Exception) {}
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open UPI", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val amountStr = if (parsedAmount != null && parsedAmount > 0) "₹${parsedAmount.toInt()}" else ""
                                val shareMsg = """
                                    🥛 *Payment Request from $businessName*
                                    Amount: $amountStr
                                    UPI ID: $upiId
                                    
                                    Click to pay directly via UPI:
                                    $upiUrl
                                    
                                    Thank you for your business! 🙏
                                """.trimIndent()

                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, shareMsg)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Payment Link"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share Link", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Customer Khata & Passbook Modal showing complete delivery vs payment timeline.
 */
@Composable
fun CustomerKhataPassbookModal(
    customer: CustomerEntity,
    viewModel: MilkMateViewModel,
    onDismiss: () -> Unit,
    onCollectPayment: () -> Unit
) {
    val context = LocalContext.current
    val business by viewModel.currentBusiness.collectAsStateWithLifecycle()
    val session by viewModel.sessionState.collectAsStateWithLifecycle()

    val deliveries by viewModel.getCustomerDeliveriesFlow(customer.id).collectAsStateWithLifecycle(emptyList())
    val payments by viewModel.getCustomerPaymentsFlow(customer.id).collectAsStateWithLifecycle(emptyList())

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    // Combine into ledger entries
    data class LedgerEntry(
        val id: String,
        val date: Long,
        val title: String,
        val subtitle: String,
        val debit: Double, // Deliveries
        val credit: Double // Payments
    )

    val ledgerEntries = remember(deliveries, payments) {
        val list = mutableListOf<LedgerEntry>()
        deliveries.forEach { d ->
            list.add(
                LedgerEntry(
                    id = "del_${d.id}",
                    date = d.deliveryDate,
                    title = "🥛 ${d.milkType} Milk (${d.shift})",
                    subtitle = "${d.quantityLiters} L @ ₹${d.ratePerLiter}/L",
                    debit = d.totalAmount,
                    credit = 0.0
                )
            )
        }
        payments.forEach { p ->
            list.add(
                LedgerEntry(
                    id = "pay_${p.id}",
                    date = p.date,
                    title = "💵 Payment Received (${p.method})",
                    subtitle = if (p.referenceNo.isNotBlank()) "Ref: ${p.referenceNo}" else "Direct Receipt",
                    debit = 0.0,
                    credit = p.amount
                )
            )
        }
        list.sortedByDescending { it.date }
    }

    val totalBilled = ledgerEntries.sumOf { it.debit }
    val totalPaid = ledgerEntries.sumOf { it.credit }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(
                        text = "Khata Passbook • ${customer.milkType} Milk (${customer.defaultQuantity} L)",
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
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Balance Highlight Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (customer.outstandingBalance > 0) DangerRedLight else DairyGreenLight
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Current Outstanding Balance:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = if (customer.outstandingBalance > 0) "₹${customer.outstandingBalance.toInt()} Due" else if (customer.outstandingBalance < 0) "₹${Math.abs(customer.outstandingBalance).toInt()} in Advance" else "₹0 Settled",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (customer.outstandingBalance > 0) DangerRed else DairyGreen
                            )
                        }
                        Button(
                            onClick = onCollectPayment,
                            colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("+ Receive ₹", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Summary Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Billed", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${totalBilled.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RoyalBluePrimary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Received", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${totalPaid.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DairyGreen)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Transactions", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${ledgerEntries.size}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                HorizontalDivider()

                // Ledger Timeline List
                if (ledgerEntries.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No deliveries or payments recorded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(ledgerEntries, key = { it.id }) { entry ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(entry.title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                        Text("${dateFormat.format(Date(entry.date))} • ${entry.subtitle}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (entry.debit > 0) {
                                        Text("-₹${entry.debit.toInt()}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DangerRed)
                                    } else {
                                        Text("+₹${entry.credit.toInt()}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DairyGreen)
                                    }
                                }
                            }
                        }
                    }
                }

                // Export & WhatsApp Share Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            val headers = listOf("Date", "Description", "Debit (₹)", "Credit (₹)")
                            val rows = ledgerEntries.map { e ->
                                listOf(
                                    dateFormat.format(Date(e.date)),
                                    "${e.title} (${e.subtitle})",
                                    if (e.debit > 0) "₹${e.debit.toInt()}" else "-",
                                    if (e.credit > 0) "₹${e.credit.toInt()}" else "-"
                                )
                            }
                            ReportExportHelper.generateAndSharePdf(
                                context = context,
                                title = "${customer.name} - Statement",
                                period = "Complete History",
                                headers = headers,
                                rows = rows,
                                summaryMetrics = listOf(
                                    "Total Billed" to "₹${totalBilled.toInt()}",
                                    "Total Paid" to "₹${totalPaid.toInt()}",
                                    "Net Balance" to "₹${customer.outstandingBalance.toInt()}"
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp), tint = DangerRed)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF Passbook", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            val phone = customer.mobile.replace("+", "").replace(" ", "")
                            val dueText = if (customer.outstandingBalance > 0) "Pending Due: ₹${customer.outstandingBalance.toInt()}" else "Balance is Clear"
                            val upiText = if (session.upiId.isNotBlank()) "💳 *Pay via UPI*: ${session.upiId}" else ""
                            val msg = """
                                🥛 *Milk Statement - ${business?.businessName ?: "MilkMate"}*
                                Hello ${customer.name},
                                
                                Current Statement Summary:
                                • Total Billed: ₹${totalBilled.toInt()}
                                • Total Paid: ₹${totalPaid.toInt()}
                                • *$dueText*
                                
                                $upiText
                                Thank you for your continued trust! 🙏
                            """.trimIndent()
                            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$phone&text=${Uri.encode(msg)}")
                            try { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) } catch (e: Exception) { viewModel.showMessage("WhatsApp not installed") }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                        modifier = Modifier.weight(1.2f),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WhatsApp Bill", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

/**
 * Payout modal for paying suppliers, feed mills, and vendors.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordSupplierPayoutModal(
    preselectedSupplier: CustomerEntity?,
    allSuppliers: List<CustomerEntity>,
    onDismiss: () -> Unit,
    onSave: (CustomerEntity, Double, String, String, String) -> Unit
) {
    var selectedSupplier by remember { mutableStateOf(preselectedSupplier ?: allSuppliers.firstOrNull()) }
    var supplierDropdownExpanded by remember { mutableStateOf(false) }

    val payableBalance = if (selectedSupplier != null && selectedSupplier!!.outstandingBalance < 0) -selectedSupplier!!.outstandingBalance else (selectedSupplier?.outstandingBalance ?: 0.0)
    var amountText by remember(selectedSupplier) {
        mutableStateOf(if (payableBalance > 0) payableBalance.toInt().toString() else "1000")
    }

    var selectedMethod by remember { mutableStateOf("BANK") } // BANK, UPI, CASH, CHEQUE
    var referenceNo by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(DangerRedLight, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = DangerRed, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Record Supplier Payout", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Feed mills, grain, procurement", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
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
                if (preselectedSupplier == null) {
                    ExposedDropdownMenuBox(
                        expanded = supplierDropdownExpanded,
                        onExpandedChange = { supplierDropdownExpanded = !supplierDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedSupplier?.name ?: "Select Supplier",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Supplier / Vendor *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierDropdownExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = supplierDropdownExpanded,
                            onDismissRequest = { supplierDropdownExpanded = false }
                        ) {
                            allSuppliers.forEach { sup ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(sup.name, fontWeight = FontWeight.Medium)
                                            val bal = if (sup.outstandingBalance < 0) -sup.outstandingBalance else sup.outstandingBalance
                                            Text("₹${bal.toInt()}", fontSize = 11.sp, color = DangerRed, fontWeight = FontWeight.Bold)
                                        }
                                    },
                                    onClick = {
                                        selectedSupplier = sup
                                        supplierDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(selectedSupplier?.name ?: "", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(selectedSupplier?.mobile ?: "No phone", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = DangerRedLight
                            ) {
                                Text(
                                    "Payable: ₹${payableBalance.toInt()}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DangerRed,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Payout Amount (₹) *") },
                    leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DangerRed) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick presets
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (payableBalance > 0) {
                        FilterChip(
                            selected = amountText == payableBalance.toInt().toString(),
                            onClick = { amountText = payableBalance.toInt().toString() },
                            label = { Text("Full Clearance (₹${payableBalance.toInt()})", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DangerRedLight,
                                selectedLabelColor = DangerRed
                            )
                        )
                    }
                    listOf("1000", "2000", "5000", "10000").forEach { p ->
                        FilterChip(
                            selected = amountText == p,
                            onClick = { amountText = p },
                            label = { Text("₹$p", fontSize = 10.sp) }
                        )
                    }
                }

                Text("Payment Channel *", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("BANK" to "🏦 Bank", "UPI" to "📱 UPI", "CASH" to "💵 Cash", "CHEQUE" to "📝 Cheque").forEach { (method, label) ->
                        FilterChip(
                            selected = selectedMethod == method,
                            onClick = { selectedMethod = method },
                            label = { Text(label, fontSize = 11.sp, fontWeight = if (selectedMethod == method) FontWeight.Bold else FontWeight.Normal) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = referenceNo,
                    onValueChange = { referenceNo = it },
                    label = { Text("Bank UTR / Transaction ID / Cheque #") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Remarks (e.g. Soya Doc purchase batch #12)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sup = selectedSupplier ?: return@Button
                    if (parsedAmount > 0) {
                        onSave(sup, parsedAmount, selectedMethod, referenceNo, notes)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                enabled = selectedSupplier != null && parsedAmount > 0
            ) {
                Text("Confirm Payout (₹${parsedAmount.toInt()})", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Beautiful Instant Payment Receipt Modal with 1-tap WhatsApp sharing & Thermal/A4 print.
 */
@Composable
fun PaymentReceiptModal(
    payment: PaymentEntity,
    businessName: String,
    remainingBalance: Double,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(DairyGreenLight, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Payment Receipt", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Official transaction voucher", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Receipt Card Container
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(businessName.ifBlank { "MilkMate Dairy" }, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = RoyalBluePrimary)
                                Text("Voucher #: ${payment.id.take(8).uppercase()}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = DairyGreenLight
                            ) {
                                Text("SUCCESS ✓", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = DairyGreen, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }

                        HorizontalDivider()

                        // Big Amount
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("AMOUNT RECEIVED", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${payment.amount.toInt()}", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = DairyGreen)
                            Text("via ${payment.method}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        }

                        HorizontalDivider()

                        // Party details
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Received From:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(payment.customerName, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Date & Time:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(dateFormat.format(Date(payment.date)), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        if (payment.referenceNo.isNotBlank()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Reference / UTR:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(payment.referenceNo, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = RoyalBluePrimary)
                            }
                        }
                        if (payment.notes.isNotBlank()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Notes:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(payment.notes, fontSize = 12.sp)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Updated Balance:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = if (remainingBalance > 0) "₹${remainingBalance.toInt()} Due" else if (remainingBalance < 0) "₹${Math.abs(remainingBalance).toInt()} in Advance 💎" else "₹0 (Settled) ✅",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (remainingBalance > 0) DangerRed else DairyGreen
                            )
                        }
                    }
                }

                // Action Buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val headers = listOf("Receipt Field", "Details")
                            val rows = listOf(
                                listOf("Voucher ID", payment.id.take(8).uppercase()),
                                listOf("Party Name", payment.customerName),
                                listOf("Amount Received", "₹${payment.amount.toInt()}"),
                                listOf("Payment Mode", payment.method),
                                listOf("Date", dateFormat.format(Date(payment.date))),
                                listOf("Reference No", payment.referenceNo.ifBlank { "N/A" }),
                                listOf("Updated Balance", if (remainingBalance > 0) "₹${remainingBalance.toInt()} (Due)" else "Settled")
                            )
                            ReportExportHelper.generateAndSharePdf(
                                context = context,
                                title = "Payment Receipt - ${payment.customerName}",
                                period = dateFormat.format(Date(payment.date)),
                                headers = headers,
                                rows = rows,
                                summaryMetrics = listOf(
                                    "Amount" to "₹${payment.amount.toInt()}",
                                    "Mode" to payment.method,
                                    "Balance" to "₹${remainingBalance.toInt()}"
                                )
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp), tint = DangerRed)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF Slip", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val receiptText = """
                                🧾 *PAYMENT RECEIPT - ${businessName.ifBlank { "MilkMate Dairy" }}*
                                
                                Received with thanks: *₹${payment.amount.toInt()}*
                                From: *${payment.customerName}*
                                Payment Mode: *${payment.method}*
                                Date: ${dateFormat.format(Date(payment.date))}
                                ${if (payment.referenceNo.isNotBlank()) "Ref / UTR: ${payment.referenceNo}\n" else ""}
                                Outstanding Balance: *${if (remainingBalance > 0) "₹${remainingBalance.toInt()} Due" else "₹0 (Fully Settled)"}*
                                
                                Thank you for your payment! 🙏
                            """.trimIndent()

                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, receiptText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Receipt"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WhatsApp Receipt", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

/**
 * Configure UPI ID Dialog
 */
@Composable
fun ConfigureUpiModal(
    currentUpiId: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var upiText by remember { mutableStateOf(currentUpiId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Dairy UPI ID", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Enter your VPA / UPI ID (e.g. yourname@okaxis, dairy@paytm, 9876543210@ybl) to generate instant Bharat QR codes and payment links for customers.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = upiText,
                    onValueChange = { upiText = it.trim() },
                    label = { Text("UPI ID / VPA *") },
                    placeholder = { Text("e.g. 9876543210@paytm") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(upiText) },
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                enabled = upiText.isNotBlank()
            ) {
                Text("Save UPI ID")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

