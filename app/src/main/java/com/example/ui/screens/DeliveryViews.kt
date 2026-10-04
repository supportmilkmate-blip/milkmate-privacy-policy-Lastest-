package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
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
import com.example.data.local.entity.DeliveryEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Modern Chronological Delivery Timeline View with swipe-to-delete,
 * search, filters, duplicate detection, and quick error-correction.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChronologicalDeliveryTimelineView(
    todayDeliveries: List<DeliveryEntity>,
    selectedDate: Long,
    selectedShift: String,
    shortDateDisplay: String,
    onSelectDate: (Long) -> Unit,
    onShiftChange: (String) -> Unit,
    onEditDelivery: (DeliveryEntity) -> Unit,
    onDeleteDelivery: (DeliveryEntity) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedMilkFilter by remember { mutableStateOf("ALL") } // "ALL", "COW", "BUFFALO"
    var selectedShiftFilter by remember { mutableStateOf(selectedShift) } // "ALL", "MORNING", "EVENING"
    var selectedStatusFilter by remember { mutableStateOf("ALL") } // "ALL", "DELIVERED", "ABSENT"
    var sortDescending by remember { mutableStateOf(true) }

    // Date Picker
    val calendar = Calendar.getInstance().apply { timeInMillis = selectedDate }
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance()
                cal.set(year, month, dayOfMonth, 0, 0, 0)
                cal.set(Calendar.MILLISECOND, 0)
                onSelectDate(cal.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    // Filter & Sort deliveries
    val filteredDeliveries = remember(todayDeliveries, searchQuery, selectedMilkFilter, selectedShiftFilter, selectedStatusFilter, sortDescending) {
        todayDeliveries.filter { d ->
            val matchesQuery = searchQuery.isBlank() ||
                    d.customerName.contains(searchQuery, ignoreCase = true) ||
                    d.notes.contains(searchQuery, ignoreCase = true)
            val matchesMilk = selectedMilkFilter == "ALL" || d.milkType == selectedMilkFilter
            val matchesShift = selectedShiftFilter == "ALL" || d.shift.equals(selectedShiftFilter, ignoreCase = true)
            val matchesStatus = when (selectedStatusFilter) {
                "DELIVERED" -> d.isDelivered
                "ABSENT" -> !d.isDelivered
                else -> true
            }
            matchesQuery && matchesMilk && matchesShift && matchesStatus
        }.sortedWith { a, b ->
            val timeA = if (a.updatedAt > 0) a.updatedAt else a.createdAt
            val timeB = if (b.updatedAt > 0) b.updatedAt else b.createdAt
            if (sortDescending) timeB.compareTo(timeA) else timeA.compareTo(timeB)
        }
    }

    // Duplicate detection: Customers with multiple entries in the same shift and milk type
    val duplicatesList = remember(todayDeliveries) {
        todayDeliveries
            .groupBy { "${it.customerId}_${it.shift}_${it.milkType}" }
            .filter { it.value.size > 1 }
            .values
            .flatten()
    }

    // Metrics for the filtered list
    val totalLiters = filteredDeliveries.filter { it.isDelivered }.sumOf { it.quantityLiters }
    val totalAmount = filteredDeliveries.filter { it.isDelivered }.sumOf { it.totalAmount }
    val cowLiters = filteredDeliveries.filter { it.isDelivered && it.milkType == "COW" }.sumOf { it.quantityLiters }
    val buffLiters = filteredDeliveries.filter { it.isDelivered && it.milkType == "BUFFALO" }.sumOf { it.quantityLiters }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ------------------ Top Bar: Date & Quick Actions ------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date Switcher Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.clickable { datePickerDialog.show() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp), tint = RoyalBluePrimary)
                    Text(shortDateDisplay, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }

            // Quick Shift Switchers
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("ALL" to "All Shifts", "MORNING" to "🌅 Morning", "EVENING" to "🌆 Evening").forEach { (sh, label) ->
                    val isSelected = selectedShiftFilter == sh
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) RoyalBluePrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.clickable {
                            selectedShiftFilter = sh
                            if (sh != "ALL") onShiftChange(sh)
                        }
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // ------------------ Summary Metric Card ------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(shape = CircleShape, color = DairyGreen.copy(alpha = 0.15f), modifier = Modifier.size(24.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(14.dp))
                            }
                        }
                        Text(
                            text = "Today's Delivery Log (${filteredDeliveries.size} Records)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    // Sort Toggle
                    Row(
                        modifier = Modifier.clickable { sortDescending = !sortDescending },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            if (sortDescending) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                            contentDescription = "Sort order",
                            modifier = Modifier.size(14.dp),
                            tint = RoyalBluePrimary
                        )
                        Text(
                            text = if (sortDescending) "Newest First" else "Oldest First",
                            fontSize = 10.sp,
                            color = RoyalBluePrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Volume and revenue indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Total Volume", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = String.format(Locale.US, "%.1f L", totalLiters),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = RoyalBluePrimary
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🐄 Cow / 🐃 Buff", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${String.format(Locale.US, "%.1f", cowLiters)}L / ${String.format(Locale.US, "%.1f", buffLiters)}L",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Total Amount", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = String.format(Locale.US, "₹%.0f", totalAmount),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = DairyGreen
                        )
                    }
                }
            }
        }

        // ------------------ Duplicate Prevention Warning Banner ------------------
        if (duplicatesList.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = DangerRed.copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = DangerRed, modifier = Modifier.size(18.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Potential Duplicates Detected (${duplicatesList.size} items)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = DangerRed
                        )
                        Text(
                            text = "Swipe left or tap trash on any duplicated record below to correct.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // ------------------ Search & Filter Row ------------------
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            placeholder = { Text("Search by customer name or notes...", fontSize = 11.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(18.dp)) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        // Filter chips row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Milk Type Filter
            listOf("ALL" to "🥛 All Milk", "COW" to "🐄 Cow", "BUFFALO" to "🐃 Buffalo").forEach { (type, label) ->
                FilterChip(
                    selected = selectedMilkFilter == type,
                    onClick = { selectedMilkFilter = type },
                    label = { Text(label, fontSize = 11.sp) },
                    modifier = Modifier.height(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Status Filter
            listOf("ALL" to "All Status", "DELIVERED" to "✓ Delivered", "ABSENT" to "🏖️ Absent").forEach { (st, label) ->
                FilterChip(
                    selected = selectedStatusFilter == st,
                    onClick = { selectedStatusFilter = st },
                    label = { Text(label, fontSize = 11.sp) },
                    modifier = Modifier.height(32.dp)
                )
            }
        }

        // ------------------ Chronological Timeline List ------------------
        if (filteredDeliveries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🥛", fontSize = 28.sp)
                        }
                    }
                    Text(
                        text = "No deliveries recorded yet",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Deliveries entered from the Route tab or Step-by-Step will appear here chronologically with instant swipe-to-delete.",
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredDeliveries, key = { it.id }) { delivery ->
                    val isDuplicate = duplicatesList.any { it.id == delivery.id }
                    SwipeToDismissDeliveryItem(
                        delivery = delivery,
                        isDuplicate = isDuplicate,
                        onEdit = { onEditDelivery(delivery) },
                        onDelete = { onDeleteDelivery(delivery) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

/**
 * Swipeable Delivery Item with SwipeToDismissBox
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDismissDeliveryItem(
    delivery: DeliveryEntity,
    isDuplicate: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart || value == SwipeToDismissBoxValue.StartToEnd) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val color = DangerRed
            val alignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                Alignment.CenterStart
            } else {
                Alignment.CenterEnd
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                    Text("Delete Record", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    ) {
        ChronologicalDeliveryCard(
            delivery = delivery,
            isDuplicate = isDuplicate,
            onEdit = onEdit,
            onDelete = onDelete
        )
    }
}

/**
 * Detailed Delivery Card inside the Chronological List
 */
@Composable
fun ChronologicalDeliveryCard(
    delivery: DeliveryEntity,
    isDuplicate: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val timestamp = if (delivery.updatedAt > 0) delivery.updatedAt else delivery.createdAt
    val timeStr = timeFormat.format(Date(timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDuplicate) DangerRed.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
        ),
        border = if (isDuplicate) androidx.compose.foundation.BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Row 1: Time, Shift, Milk Badge, and Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Shift icon & time
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (delivery.shift.equals("MORNING", ignoreCase = true)) FreshGold.copy(alpha = 0.2f) else RoyalBluePrimary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (delivery.shift.equals("MORNING", ignoreCase = true)) "🌅 $timeStr" else "🌆 $timeStr",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (delivery.shift.equals("MORNING", ignoreCase = true)) Color(0xFFD97706) else RoyalBluePrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Milk Type Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (delivery.milkType == "COW") RoyalBluePrimary.copy(alpha = 0.12f) else Color(0xFF8D6E63).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (delivery.milkType == "COW") "🐄 Cow Milk" else "🐃 Buffalo Milk",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (delivery.milkType == "COW") RoyalBluePrimary else Color(0xFF5D4037),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (isDuplicate) {
                        Surface(shape = RoundedCornerShape(4.dp), color = DangerRed) {
                            Text("DUPLICATE", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                        }
                    }
                }

                // Edit & Delete actions
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(15.dp), tint = RoyalBluePrimary)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = DangerRed)
                    }
                }
            }

            // Row 2: Customer Name, Quantity, Rate, Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = delivery.customerName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (delivery.customerType.isNotBlank() && delivery.customerType != "INDIVIDUAL") {
                        Text(
                            text = delivery.customerType,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Quantity pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (delivery.isDelivered) DairyGreen.copy(alpha = 0.12f) else DangerRed.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = if (delivery.isDelivered) "${delivery.quantityLiters} L" else "Absent (0L)",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = if (delivery.isDelivered) DairyGreen else DangerRed,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Total ₹
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "₹${delivery.totalAmount.toInt()}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = RoyalBluePrimary
                        )
                        Text(
                            text = "₹${delivery.ratePerLiter.toInt()}/L",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Optional Quality parameters (Fat, SNF, CLR, Notes)
            val hasQuality = (delivery.fat > 0 || delivery.snf > 0 || delivery.clr > 0)
            val hasNotes = delivery.notes.isNotBlank() && delivery.notes != "Delivered"

            if (hasQuality || hasNotes) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), shape = RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasQuality) {
                        Text(
                            text = "Fat: ${delivery.fat}% | SNF: ${delivery.snf}% | CLR: ${delivery.clr.toInt()}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (hasNotes) {
                        Text(
                            text = delivery.notes,
                            fontSize = 10.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Interactive Daily Milk Output Analytics View (Dashboard Charting for Cow vs. Buffalo)
 */
@Composable
fun DailyMilkOutputAnalyticsView(
    todayDeliveries: List<DeliveryEntity>,
    selectedDate: Long,
    selectedShift: String,
    shortDateDisplay: String,
    fullDateDisplay: String,
    accountStartDate: Long,
    businessName: String?,
    onSelectDate: (Long) -> Unit
) {
    val context = LocalContext.current
    var analyticsShiftFilter by remember { mutableStateOf("ALL") } // "ALL", "MORNING", "EVENING"
    var selectedChartSegment by remember { mutableStateOf<String?>(null) } // "COW", "BUFFALO", null

    // Date Picker
    val calendar = Calendar.getInstance().apply { timeInMillis = selectedDate }
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance()
                cal.set(year, month, dayOfMonth, 0, 0, 0)
                cal.set(Calendar.MILLISECOND, 0)
                onSelectDate(cal.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    // Filter deliveries based on shift for analytics
    val relevantDeliveries = remember(todayDeliveries, analyticsShiftFilter) {
        if (analyticsShiftFilter == "ALL") todayDeliveries
        else todayDeliveries.filter { it.shift.equals(analyticsShiftFilter, ignoreCase = true) }
    }

    // Key metrics calculations
    val deliveredList = relevantDeliveries.filter { it.isDelivered }
    val cowDeliveries = deliveredList.filter { it.milkType == "COW" }
    val buffDeliveries = deliveredList.filter { it.milkType == "BUFFALO" }

    val cowLiters = cowDeliveries.sumOf { it.quantityLiters }
    val buffLiters = buffDeliveries.sumOf { it.quantityLiters }
    val totalLiters = cowLiters + buffLiters

    val cowAmount = cowDeliveries.sumOf { it.totalAmount }
    val buffAmount = buffDeliveries.sumOf { it.totalAmount }
    val totalAmount = cowAmount + buffAmount

    val cowPercentage = if (totalLiters > 0) (cowLiters / totalLiters * 100).toFloat() else 0f
    val buffPercentage = if (totalLiters > 0) (buffLiters / totalLiters * 100).toFloat() else 0f

    val avgCowRate = if (cowLiters > 0) cowAmount / cowLiters else 0.0
    val avgBuffRate = if (buffLiters > 0) buffAmount / buffLiters else 0.0

    // Shift breakdown
    val morningCow = deliveredList.filter { it.shift.equals("MORNING", ignoreCase = true) && it.milkType == "COW" }.sumOf { it.quantityLiters }
    val morningBuff = deliveredList.filter { it.shift.equals("MORNING", ignoreCase = true) && it.milkType == "BUFFALO" }.sumOf { it.quantityLiters }
    val eveningCow = deliveredList.filter { it.shift.equals("EVENING", ignoreCase = true) && it.milkType == "COW" }.sumOf { it.quantityLiters }
    val eveningBuff = deliveredList.filter { it.shift.equals("EVENING", ignoreCase = true) && it.milkType == "BUFFALO" }.sumOf { it.quantityLiters }

    // Average Fat & SNF
    val cowWithFat = cowDeliveries.filter { it.fat > 0 }
    val avgCowFat = if (cowWithFat.isNotEmpty()) cowWithFat.map { it.fat }.average() else 4.0
    val buffWithFat = buffDeliveries.filter { it.fat > 0 }
    val avgBuffFat = if (buffWithFat.isNotEmpty()) buffWithFat.map { it.fat }.average() else 6.5

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ------------------ Top Date Bar & Shift Selector ------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.clickable { datePickerDialog.show() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp), tint = RoyalBluePrimary)
                    Text(shortDateDisplay, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }

            // Shift filter tabs
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("ALL" to "All Day", "MORNING" to "🌅 Morning", "EVENING" to "🌆 Evening").forEach { (sh, label) ->
                    val isSelected = analyticsShiftFilter == sh
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) DairyGreen else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.clickable { analyticsShiftFilter = sh }
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // ------------------ Total Day Output Hero Card ------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                RoyalBluePrimary.copy(alpha = 0.08f),
                                DairyGreen.copy(alpha = 0.05f)
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Daily Output Volume", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = String.format(Locale.US, "%.1f Liters", totalLiters),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = RoyalBluePrimary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DairyGreen.copy(alpha = 0.15f)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text("Day Output Value", fontSize = 10.sp, color = DairyGreen, fontWeight = FontWeight.Bold)
                                Text(
                                    text = String.format(Locale.US, "₹%.0f", totalAmount),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DairyGreen
                                )
                            }
                        }
                    }

                    // Quick Breakdown Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "🐄 Cow: ${String.format(Locale.US, "%.1f", cowLiters)}L (${String.format(Locale.US, "%.0f", cowPercentage)}%)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = RoyalBluePrimary
                        )
                        Text(
                            text = "🐃 Buff: ${String.format(Locale.US, "%.1f", buffLiters)}L (${String.format(Locale.US, "%.0f", buffPercentage)}%)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFD97706)
                        )
                        Text(
                            text = "${deliveredList.size} deliveries",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // ------------------ Interactive Volume Donut Visualization ------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🐄 Cow vs 🐃 Buffalo Volume Share",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Interactive Chart",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (totalLiters <= 0.0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No deliveries logged for selected filter", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    // Donut Chart + Legend in Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        // Canvas Donut Chart
                        Box(
                            modifier = Modifier.size(130.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(120.dp)) {
                                val strokeWidth = 24.dp.toPx()
                                val radius = (size.minDimension - strokeWidth) / 2
                                val center = Offset(size.width / 2, size.height / 2)
                                val arcSize = Size(radius * 2, radius * 2)
                                val topLeft = Offset(center.x - radius, center.y - radius)

                                val cowSweepAngle = (cowPercentage / 100f) * 360f
                                val buffSweepAngle = (buffPercentage / 100f) * 360f

                                // Cow arc (Royal Blue)
                                if (cowSweepAngle > 0f) {
                                    drawArc(
                                        color = Color(0xFF2563EB),
                                        startAngle = -90f,
                                        sweepAngle = cowSweepAngle,
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = arcSize,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )
                                }

                                // Buffalo arc (Amber Orange)
                                if (buffSweepAngle > 0f) {
                                    drawArc(
                                        color = Color(0xFFF59E0B),
                                        startAngle = -90f + cowSweepAngle,
                                        sweepAngle = buffSweepAngle,
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = arcSize,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )
                                }
                            }

                            // Center Text inside Donut
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = String.format(Locale.US, "%.0f", totalLiters),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text("Liters", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Breakdown Legend Cards
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Cow Card
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF2563EB).copy(alpha = 0.1f),
                                modifier = Modifier
                                    .width(150.dp)
                                    .clickable { selectedChartSegment = if (selectedChartSegment == "COW") null else "COW" }
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Surface(shape = CircleShape, color = Color(0xFF2563EB), modifier = Modifier.size(8.dp)) {}
                                        Text("Cow Milk", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF1D4ED8))
                                    }
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", cowLiters)} L (${String.format(Locale.US, "%.1f", cowPercentage)}%)",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp
                                    )
                                    Text("Avg ₹${avgCowRate.toInt()}/L • ₹${cowAmount.toInt()}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            // Buffalo Card
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF59E0B).copy(alpha = 0.1f),
                                modifier = Modifier
                                    .width(150.dp)
                                    .clickable { selectedChartSegment = if (selectedChartSegment == "BUFFALO") null else "BUFFALO" }
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Surface(shape = CircleShape, color = Color(0xFFF59E0B), modifier = Modifier.size(8.dp)) {}
                                        Text("Buffalo Milk", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFB45309))
                                    }
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", buffLiters)} L (${String.format(Locale.US, "%.1f", buffPercentage)}%)",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp
                                    )
                                    Text("Avg ₹${avgBuffRate.toInt()}/L • ₹${buffAmount.toInt()}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ------------------ Shift Output Bar Chart (Morning vs Evening) ------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Shift Output Comparison",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                val maxShiftVolume = maxOf(morningCow + morningBuff, eveningCow + eveningBuff, 1.0)

                // Morning Bar
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🌅 Morning Shift", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "${String.format(Locale.US, "%.1f", morningCow + morningBuff)} L (🐄 ${String.format(Locale.US, "%.1f", morningCow)}L | 🐃 ${String.format(Locale.US, "%.1f", morningBuff)}L)",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Horizontal Progress Bar
                    val morningFraction = ((morningCow + morningBuff) / maxShiftVolume).toFloat().coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(7.dp))
                    ) {
                        Row(modifier = Modifier.fillMaxHeight().fillMaxWidth(morningFraction)) {
                            val cowPart = if (morningCow + morningBuff > 0) (morningCow / (morningCow + morningBuff)).toFloat() else 0f
                            val buffPart = if (morningCow + morningBuff > 0) (morningBuff / (morningCow + morningBuff)).toFloat() else 0f

                            if (cowPart > 0f) {
                                Box(modifier = Modifier.fillMaxHeight().weight(cowPart).background(Color(0xFF2563EB), shape = RoundedCornerShape(topStart = 7.dp, bottomStart = 7.dp)))
                            }
                            if (buffPart > 0f) {
                                Box(modifier = Modifier.fillMaxHeight().weight(buffPart).background(Color(0xFFF59E0B), shape = RoundedCornerShape(topEnd = 7.dp, bottomEnd = 7.dp)))
                            }
                        }
                    }
                }

                // Evening Bar
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🌆 Evening Shift", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "${String.format(Locale.US, "%.1f", eveningCow + eveningBuff)} L (🐄 ${String.format(Locale.US, "%.1f", eveningCow)}L | 🐃 ${String.format(Locale.US, "%.1f", eveningBuff)}L)",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Horizontal Progress Bar
                    val eveningFraction = ((eveningCow + eveningBuff) / maxShiftVolume).toFloat().coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(7.dp))
                    ) {
                        Row(modifier = Modifier.fillMaxHeight().fillMaxWidth(eveningFraction)) {
                            val cowPart = if (eveningCow + eveningBuff > 0) (eveningCow / (eveningCow + eveningBuff)).toFloat() else 0f
                            val buffPart = if (eveningCow + eveningBuff > 0) (eveningBuff / (eveningCow + eveningBuff)).toFloat() else 0f

                            if (cowPart > 0f) {
                                Box(modifier = Modifier.fillMaxHeight().weight(cowPart).background(Color(0xFF2563EB), shape = RoundedCornerShape(topStart = 7.dp, bottomStart = 7.dp)))
                            }
                            if (buffPart > 0f) {
                                Box(modifier = Modifier.fillMaxHeight().weight(buffPart).background(Color(0xFFF59E0B), shape = RoundedCornerShape(topEnd = 7.dp, bottomEnd = 7.dp)))
                            }
                        }
                    }
                }
            }
        }

        // ------------------ Quality Parameters & Averages ------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Cow Fat Card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("🐄 Cow Avg Fat", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${String.format(Locale.US, "%.1f", avgCowFat)} %",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoyalBluePrimary
                    )
                    Text("Benchmark: 4.0%", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Buff Fat Card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("🐃 Buff Avg Fat", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${String.format(Locale.US, "%.1f", avgBuffFat)} %",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD97706)
                    )
                    Text("Benchmark: 6.5%", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Dialog for Quick Single Delivery Editing
 */
@Composable
fun EditSingleDeliveryDialog(
    delivery: DeliveryEntity,
    onDismiss: () -> Unit,
    onSave: (quantity: Double, rate: Double, fat: Double, snf: Double, clr: Double, notes: String) -> Unit,
    onDelete: () -> Unit
) {
    var quantityText by remember { mutableStateOf(delivery.quantityLiters.toString()) }
    var rateText by remember { mutableStateOf(delivery.ratePerLiter.toString()) }
    var fatText by remember { mutableStateOf(if (delivery.fat > 0) delivery.fat.toString() else "4.0") }
    var snfText by remember { mutableStateOf(if (delivery.snf > 0) delivery.snf.toString() else "8.5") }
    var clrText by remember { mutableStateOf(if (delivery.clr > 0) delivery.clr.toString() else "28") }
    var notesText by remember { mutableStateOf(delivery.notes) }

    val qty = quantityText.toDoubleOrNull() ?: 0.0
    val rate = rateText.toDoubleOrNull() ?: 0.0
    val total = qty * rate

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Edit Delivery Record",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.DeleteForever, contentDescription = "Delete", tint = DangerRed)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${delivery.customerName} • ${if (delivery.milkType == "COW") "🐄 Cow" else "🐃 Buffalo"} (${delivery.shift})",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("Quantity (Liters)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Add Quantity Presets Chips
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚡ Quick:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                    listOf("0.5", "1.0", "1.5", "2.0", "2.5", "3.0", "5.0").forEach { p ->
                        val isSelected = quantityText == p
                        FilterChip(
                            selected = isSelected,
                            onClick = { quantityText = p },
                            label = { Text("${p}L", fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }

                OutlinedTextField(
                    value = rateText,
                    onValueChange = { rateText = it },
                    label = { Text("Rate (₹/Liter)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = fatText,
                        onValueChange = { fatText = it },
                        label = { Text("Fat %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = snfText,
                        onValueChange = { snfText = it },
                        label = { Text("SNF %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Calculated Total:", fontSize = 12.sp)
                        Text("₹${String.format(Locale.US, "%.2f", total)}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = RoyalBluePrimary)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        qty,
                        rate,
                        fatText.toDoubleOrNull() ?: 0.0,
                        snfText.toDoubleOrNull() ?: 0.0,
                        clrText.toDoubleOrNull() ?: 0.0,
                        notesText
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = DairyGreen)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
