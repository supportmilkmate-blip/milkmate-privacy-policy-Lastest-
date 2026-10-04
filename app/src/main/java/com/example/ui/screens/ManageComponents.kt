package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.DeliveryEntity
import com.example.data.local.entity.StaffEntity
import com.example.ui.theme.*
import java.util.Locale

/**
 * Shared Attendance Pill Metric
 */
@Composable
fun AttendancePill(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (label.contains("Rate", ignoreCase = true)) "$count%" else count.toString(),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = color
            )
            Text(label, fontSize = 9.sp, color = color, fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * Shared Attendance Status Button (P, H, A, L)
 */
@Composable
fun StatusButton(label: String, isSelected: Boolean, color: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(34.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) color else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Shared Add Staff Dialog
 */
@Composable
fun AddStaffDialog(
    onDismiss: () -> Unit,
    onSave: (String?, String, String, String, String, String, String, Double, Double, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("DELIVERY_BOY") }
    var route by remember { mutableStateOf("") }
    var salaryType by remember { mutableStateOf("MONTHLY") }
    var salaryText by remember { mutableStateOf("12000") }
    var wageText by remember { mutableStateOf("400") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Staff Member", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Staff Full Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("Mobile Phone Number *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = { Text("4-Digit PIN *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Role in Dairy:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("DELIVERY_BOY" to "Delivery Boy", "MANAGER" to "Manager", "FARM_WORKER" to "Farm Worker").forEach { (rKey, rLabel) ->
                        FilterChip(
                            selected = role == rKey,
                            onClick = { role = rKey },
                            label = { Text(rLabel, fontSize = 10.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = route,
                    onValueChange = { route = it },
                    label = { Text("Assigned Route (Optional)") },
                    placeholder = { Text("e.g. Sector 14") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Wage / Compensation:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = salaryType == "MONTHLY",
                        onClick = { salaryType = "MONTHLY" },
                        label = { Text("Monthly Salary") }
                    )
                    FilterChip(
                        selected = salaryType == "DAILY",
                        onClick = { salaryType = "DAILY" },
                        label = { Text("Daily Wage") }
                    )
                }

                if (salaryType == "MONTHLY") {
                    OutlinedTextField(
                        value = salaryText,
                        onValueChange = { salaryText = it },
                        label = { Text("Monthly Salary (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = wageText,
                        onValueChange = { wageText = it },
                        label = { Text("Daily Wage Rate (₹/day)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && mobile.isNotBlank() && pin.isNotBlank()) {
                        val sal = salaryText.toDoubleOrNull() ?: 12000.0
                        val wg = wageText.toDoubleOrNull() ?: 400.0
                        onSave(null, name, mobile, pin, role, "DELIVERY,CUSTOMERS", route, sal, wg, salaryType)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary)
            ) {
                Text("Save Staff Member")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Shared Staff Routes Tab
 */
@Composable
fun StaffRoutesTab(
    staffList: List<StaffEntity>,
    deliveries: List<DeliveryEntity>
) {
    val deliveryBoys = staffList.filter { it.role == "DELIVERY_BOY" || it.role == "MANAGER" }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Delivery Routes & Staff Coverage", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = RoyalBluePrimary)
                    Text("Track delivery boy progress and assigned areas across daily morning & evening rounds.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (deliveryBoys.isEmpty()) {
            item {
                Text("No delivery boys registered in your staff team.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(deliveryBoys, key = { it.id }) { boy ->
                val assignedRoute = boy.assignedRoute.ifBlank { "All Areas" }
                val completedDeliveries = deliveries.count { it.isDelivered }
                val totalDeliveries = deliveries.size

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
                                Text(boy.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Route: $assignedRoute", fontSize = 12.sp, color = RoyalBluePrimary, fontWeight = FontWeight.SemiBold)
                            }
                            Surface(shape = RoundedCornerShape(6.dp), color = DairyGreenLight) {
                                Text("ACTIVE DISPATCH", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = DairyGreen, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Assigned Round", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$totalDeliveries Households", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Column {
                                Text("Completed Today", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$completedDeliveries Delivered", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DairyGreen)
                            }

                            Column {
                                Text("Completion", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val pct = if (totalDeliveries > 0) (completedDeliveries * 100) / totalDeliveries else 0
                                Text("$pct%", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RoyalBluePrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Shared Notification Toggle Row
 */
@Composable
fun NotificationToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = RoyalBluePrimary)
        )
    }
}

/**
 * Shared Alert Notification Card
 */
@Composable
fun AlertNotificationCard(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    message: String,
    timeText: String,
    actionLabel: String,
    onAction: () -> Unit
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
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconBg.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconBg, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(timeText, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.height(3.dp))
                Text(message, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$actionLabel →",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = RoyalBluePrimary,
                    modifier = Modifier.clickable { onAction() }
                )
            }
        }
    }
}

/**
 * 2D FAT × SNF Rate Chart Matrix Table
 */
@Composable
fun FatSnfRateMatrixTable(
    baseRate: Double,
    fatMultiplier: Double,
    snfMultiplier: Double,
    qualityBonus: Double,
    chillingDeduction: Double,
    pricingMode: String,
    fixedCowRate: Double,
    fixedBuffaloRate: Double,
    supportedMilkTypes: String = "BOTH"
) {
    val fatValues = listOf(3.5, 4.0, 4.5, 5.0, 6.0, 7.0, 8.0, 9.0)
    val snfValues = listOf(7.5, 8.0, 8.5, 9.0, 9.5)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📊 Live FAT × SNF Rate Chart (₹/L)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = RoyalBluePrimary
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = DairyGreenLight
                ) {
                    Text(
                        text = "Dynamic Matrix",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = DairyGreen,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Scrollable 2D Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                Column {
                    // Header Row: SNF labels
                    Row(
                        modifier = Modifier
                            .background(RoyalBluePrimary, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .padding(vertical = 6.dp)
                    ) {
                        Text(
                            text = "FAT \\ SNF",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.width(64.dp).padding(start = 6.dp)
                        )
                        snfValues.forEach { snf ->
                            Text(
                                text = "${snf}%",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.width(52.dp)
                            )
                        }
                    }

                    // Data Rows: FAT and calculated prices
                    fatValues.forEachIndexed { idx, fat ->
                        val rowBg = if (idx % 2 == 0) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
                        Row(
                            modifier = Modifier
                                .background(rowBg)
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${fat}%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = RoyalBluePrimary,
                                modifier = Modifier.width(64.dp).padding(start = 6.dp)
                            )

                            snfValues.forEach { snf ->
                                val calculatedRate = when (pricingMode) {
                                    "DIRECT" -> when (supportedMilkTypes) {
                                        "COW_ONLY" -> fixedCowRate
                                        "BUFFALO_ONLY" -> fixedBuffaloRate
                                        else -> if (fat >= 6.0) fixedBuffaloRate else fixedCowRate
                                    }
                                    "FAT_ONLY" -> baseRate + (fat * fatMultiplier)
                                    "FAT_SNF" -> (fat * fatMultiplier) + (snf * snfMultiplier)
                                    else -> baseRate + (fat * fatMultiplier) + (snf * snfMultiplier) + qualityBonus - chillingDeduction
                                }
                                Text(
                                    text = "₹${String.format(Locale.US, "%.1f", calculatedRate)}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (calculatedRate >= 60.0) DairyGreen else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.width(52.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Share WhatsApp Rate Chart Formatter
 */
fun shareRateChartToWhatsApp(
    context: Context,
    dairyName: String,
    pricingMode: String,
    baseRate: Double,
    cowRate: Double,
    buffRate: Double,
    fatMul: Double,
    snfMul: Double,
    supportedMilkTypes: String = "BOTH"
) {
    val message = buildString {
        append("🥛 *${dairyName.uppercase()} - OFFICIAL MILK RATE CHART*\n")
        append("📅 Effective Date: Today\n")
        append("━━━━━━━━━━━━━━━━━━━━\n")
        if (pricingMode == "DIRECT") {
            if (supportedMilkTypes != "BUFFALO_ONLY") {
                append("🐄 *Cow Milk Fixed Rate:* ₹${cowRate.toInt()} / Liter\n")
            }
            if (supportedMilkTypes != "COW_ONLY") {
                append("🐃 *Buffalo Milk Fixed Rate:* ₹${buffRate.toInt()} / Liter\n")
            }
        } else {
            append("🧪 *Pricing Mode:* $pricingMode\n")
            append("• Base Rate: ₹$baseRate / L\n")
            append("• FAT Multiplier: ₹$fatMul per point\n")
            append("• SNF Multiplier: ₹$snfMul per point\n")
            append("\n*Sample Rates per Liter:*\n")
            if (supportedMilkTypes != "BUFFALO_ONLY") {
                append("• 3.5% FAT / 8.5% SNF (Standard): ₹${String.format(Locale.US, "%.2f", baseRate + (3.5 * fatMul) + (8.5 * snfMul))}\n")
                append("• 4.5% FAT / 8.5% SNF (Premium): ₹${String.format(Locale.US, "%.2f", baseRate + (4.5 * fatMul) + (8.5 * snfMul))}\n")
            }
            if (supportedMilkTypes != "COW_ONLY") {
                append("• 6.5% FAT / 9.0% SNF (Rich): ₹${String.format(Locale.US, "%.2f", baseRate + (6.5 * fatMul) + (9.0 * snfMul))}\n")
                append("• 7.5% FAT / 9.0% SNF (High Fat): ₹${String.format(Locale.US, "%.2f", baseRate + (7.5 * fatMul) + (9.0 * snfMul))}\n")
            }
        }
        append("━━━━━━━━━━━━━━━━━━━━\n")
        append("Generated by MilkMate Dairy Operations App")
    }

    try {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            putExtra("jid", "")
            setPackage("com.whatsapp")
        }
        context.startActivity(sendIntent)
    } catch (e: Exception) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Milk Rate Chart"))
    }
}

/**
 * Share WhatsApp Staff Salary Slip Formatter
 */
fun shareStaffSalarySlipToWhatsApp(
    context: Context,
    dairyName: String,
    staffName: String,
    staffPhone: String,
    monthYear: String,
    effectiveDays: Double,
    grossSalary: Double,
    advances: Double,
    netPaid: Double,
    paymentMode: String
) {
    val message = buildString {
        append("🧾 *${dairyName.uppercase()} - SALARY VOUCHER*\n")
        append("━━━━━━━━━━━━━━━━━━━━\n")
        append("👤 *Staff Name:* $staffName\n")
        append("📅 *Month:* $monthYear\n")
        append("📊 *Working Days Recorded:* $effectiveDays days\n")
        append("━━━━━━━━━━━━━━━━━━━━\n")
        append("💵 *Gross Earnings:* ₹${grossSalary.toInt()}\n")
        append("💸 *Advances Deducted:* ₹${advances.toInt()}\n")
        append("✅ *Net Disbursed Amount:* ₹${netPaid.toInt()}\n")
        append("💳 *Payment Mode:* $paymentMode\n")
        append("━━━━━━━━━━━━━━━━━━━━\n")
        append("Thank you for your dedicated service to ${dairyName}!\n")
        append("Sent via MilkMate Dairy Management")
    }

    try {
        val url = "https://api.whatsapp.com/send?phone=+91${staffPhone.filter { it.isDigit() }}&text=${Uri.encode(message)}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Salary Slip"))
    }
}
