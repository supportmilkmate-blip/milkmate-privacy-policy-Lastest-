package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.example.data.local.entity.StaffAttendanceEntity
import com.example.data.local.entity.StaffEntity
import com.example.data.local.entity.StaffPaymentEntity
import com.example.ui.MilkMateViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffScreen(viewModel: MilkMateViewModel, onBack: (() -> Unit)? = null) {
    val staffList by viewModel.staffList.collectAsStateWithLifecycle()
    val todayAttendance by viewModel.todayAttendance.collectAsStateWithLifecycle()
    val allAttendance by viewModel.allAttendance.collectAsStateWithLifecycle()
    val staffPayments by viewModel.staffPayments.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val deliveries by viewModel.todayDeliveries.collectAsStateWithLifecycle()
    val business by viewModel.currentBusiness.collectAsStateWithLifecycle()

    var activeTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Staff, Team & Wage Hub", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(
                            text = "${staffList.size} team members • Live Attendance & Salary Slips",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (activeTab == 1 && staffList.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                viewModel.markAllStaffAttendance(staffList, selectedDate, "PRESENT")
                            }
                        ) {
                            Text("✓ All Present", fontWeight = FontWeight.Bold, color = DairyGreen, fontSize = 12.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (activeTab == 0) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = RoyalBluePrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_staff_fab")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Add Staff Member")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Segmented Tabs
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = RoyalBluePrimary
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("👥 Staff (${staffList.size})", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("📅 Attendance", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = { Text("💰 Wages & Slips", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
                Tab(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    text = { Text("🚚 Routes", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (activeTab) {
                    0 -> StaffDirectoryTab(
                        staffList = staffList,
                        onDeleteStaff = { viewModel.deleteStaff(it) }
                    )
                    1 -> StaffAttendanceTab(
                        staffList = staffList,
                        attendanceList = todayAttendance,
                        selectedDate = selectedDate,
                        onMarkAttendance = { staffId, name, status ->
                            viewModel.recordStaffAttendance(staffId, name, selectedDate, status)
                        },
                        onMarkAllPresent = {
                            viewModel.markAllStaffAttendance(staffList, selectedDate, "PRESENT")
                        },
                        onChangeDate = { viewModel.setDate(it) }
                    )
                    2 -> StaffSalariesTab(
                        staffList = staffList,
                        attendanceList = allAttendance,
                        paymentsList = staffPayments,
                        dairyName = business?.businessName ?: "MilkMate Dairy",
                        onRecordPayment = { staffId, name, type, amount, mode, monthYear, note ->
                            viewModel.recordStaffPayment(staffId, name, type, amount, mode, System.currentTimeMillis(), monthYear, note)
                        },
                        onDeletePayment = { viewModel.deleteStaffPayment(it) }
                    )
                    3 -> StaffRoutesTab(
                        staffList = staffList,
                        deliveries = deliveries
                    )
                }
            }
        }

        if (showAddDialog) {
            AddStaffDialog(
                onDismiss = { showAddDialog = false },
                onSave = { staffId, name, phone, pin, role, permissions, route, salary, wage, sType ->
                    viewModel.saveStaff(
                        id = staffId,
                        name = name,
                        mobile = phone,
                        pin = pin,
                        role = role,
                        permissions = permissions,
                        assignedRoute = route,
                        monthlySalary = salary,
                        dailyWage = wage,
                        salaryType = sType
                    )
                    showAddDialog = false
                }
            )
        }
    }
}

// TAB 1: STAFF DIRECTORY
@Composable
fun StaffDirectoryTab(
    staffList: List<StaffEntity>,
    onDeleteStaff: (String) -> Unit
) {
    val context = LocalContext.current

    if (staffList.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Badge,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No staff members yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap the + button below to add delivery boys, farm workers, or managers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(staffList, key = { it.id }) { staff ->
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
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (staff.role == "MANAGER") Color(0xFF673AB7).copy(alpha = 0.15f) else RoyalBlueLight
                                ) {
                                    Icon(
                                        imageVector = if (staff.role == "DELIVERY_BOY") Icons.Default.LocalShipping else Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (staff.role == "MANAGER") Color(0xFF673AB7) else RoyalBluePrimary,
                                        modifier = Modifier.padding(8.dp).size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = staff.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "${staff.role.replace("_", " ")} • PIN: ${staff.pin} • ID: ${staff.id}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(onClick = { onDeleteStaff(staff.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DangerRed)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Contact Phone", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(staff.mobile.ifBlank { "Not set" }, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }

                            Column {
                                Text("Assigned Route", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(staff.assignedRoute.ifBlank { "All Routes" }, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = RoyalBluePrimary)
                            }

                            Column {
                                Text("Base Compensation", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val comp = if (staff.salaryType == "DAILY") "₹${staff.dailyWage.toInt()}/day" else "₹${staff.monthlySalary.toInt()}/mo"
                                Text(comp, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DairyGreen)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val uri = Uri.parse("tel:${staff.mobile}")
                                    try { context.startActivity(Intent(Intent.ACTION_DIAL, uri)) } catch (_: Exception) {}
                                },
                                modifier = Modifier.weight(1f).height(36.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Call", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    val uri = Uri.parse("https://api.whatsapp.com/send?phone=91${staff.mobile}")
                                    try { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) } catch (_: Exception) {}
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                                modifier = Modifier.weight(1f).height(36.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("💬 WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// TAB 2: DAILY ATTENDANCE
@Composable
fun StaffAttendanceTab(
    staffList: List<StaffEntity>,
    attendanceList: List<StaffAttendanceEntity>,
    selectedDate: Long,
    onMarkAttendance: (String, String, String) -> Unit,
    onMarkAllPresent: () -> Unit,
    onChangeDate: (Long) -> Unit
) {
    val dateStr = remember(selectedDate) {
        SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date(selectedDate))
    }

    val presentCount = attendanceList.count { it.status == "PRESENT" }
    val halfDayCount = attendanceList.count { it.status == "HALF_DAY" }
    val absentCount = attendanceList.count { it.status == "ABSENT" }
    val leaveCount = attendanceList.count { it.status == "LEAVE" }
    val totalCount = staffList.size
    val rate = if (totalCount > 0) (presentCount * 100) / totalCount else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Date Selector Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onChangeDate(selectedDate - (24 * 3600 * 1000L)) }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day")
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Daily Attendance Register", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(dateStr, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                IconButton(onClick = { onChangeDate(selectedDate + (24 * 3600 * 1000L)) }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Day")
                }
            }
        }

        // Attendance Metric Badges
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AttendancePill(label = "Present", count = presentCount, color = DairyGreen, modifier = Modifier.weight(1f))
            AttendancePill(label = "Half-Day", count = halfDayCount, color = FreshGold, modifier = Modifier.weight(1f))
            AttendancePill(label = "Absent", count = absentCount, color = DangerRed, modifier = Modifier.weight(1f))
            AttendancePill(label = "Present Rate", count = rate, color = RoyalBluePrimary, modifier = Modifier.weight(1f))
        }

        if (staffList.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("Add staff members in the Directory tab to begin recording attendance.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(staffList, key = { it.id }) { staff ->
                    val currentRecord = attendanceList.firstOrNull { it.staffId == staff.id }
                    val status = currentRecord?.status ?: "UNMARKED"

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(staff.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    text = "${staff.role.replace("_", " ")} • ${staff.assignedRoute.ifBlank { "All Routes" }}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                StatusButton("P", status == "PRESENT", DairyGreen) {
                                    onMarkAttendance(staff.id, staff.name, "PRESENT")
                                }
                                StatusButton("H", status == "HALF_DAY", FreshGold) {
                                    onMarkAttendance(staff.id, staff.name, "HALF_DAY")
                                }
                                StatusButton("A", status == "ABSENT", DangerRed) {
                                    onMarkAttendance(staff.id, staff.name, "ABSENT")
                                }
                                StatusButton("L", status == "LEAVE", RoyalBluePrimary) {
                                    onMarkAttendance(staff.id, staff.name, "LEAVE")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// TAB 3: SALARIES & ADVANCES WITH SLIPS
@Composable
fun StaffSalariesTab(
    staffList: List<StaffEntity>,
    attendanceList: List<StaffAttendanceEntity>,
    paymentsList: List<StaffPaymentEntity>,
    dairyName: String,
    onRecordPayment: (String, String, String, Double, String, String, String) -> Unit,
    onDeletePayment: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedStaffForAction by remember { mutableStateOf<StaffEntity?>(null) }
    var actionType by remember { mutableStateOf("ADVANCE") }
    var showPaymentDialog by remember { mutableStateOf(false) }

    val currentMonthYear = remember {
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Monthly Wage & Advance Register", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = RoyalBluePrimary)
                    Text("Wages calculated dynamically from recorded attendance, minus advances during $currentMonthYear.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Staff Salary Cards
        items(staffList, key = { it.id }) { staff ->
            val staffDaysPresent = attendanceList.count { it.staffId == staff.id && it.status == "PRESENT" }
            val staffHalfDays = attendanceList.count { it.staffId == staff.id && it.status == "HALF_DAY" }
            val totalEffectiveDays = staffDaysPresent + (staffHalfDays * 0.5)

            val baseSalary = if (staff.salaryType == "DAILY") staff.dailyWage * totalEffectiveDays else staff.monthlySalary
            val advancesTaken = paymentsList.filter { it.staffId == staff.id && it.type == "ADVANCE" }.sumOf { it.amount }
            val salaryPaid = paymentsList.filter { it.staffId == staff.id && it.type == "SALARY_PAYOUT" }.sumOf { it.amount }
            val netPayable = maxOf(0.0, baseSalary - advancesTaken - salaryPaid)

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
                        Column {
                            Text(staff.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Base: ₹${staff.monthlySalary.toInt()}/mo • ₹${staff.dailyWage.toInt()}/day", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(shape = RoundedCornerShape(6.dp), color = DairyGreenLight) {
                            Text("Days: $totalEffectiveDays", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DairyGreen, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Gross Earned", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${baseSalary.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Column {
                            Text("Advances Taken", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${advancesTaken.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DangerRed)
                        }
                        Column {
                            Text("Already Paid", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${salaryPaid.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF64B5F6))
                        }
                        Column {
                            Text("Net Payable", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${netPayable.toInt()}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DairyGreen)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = {
                                selectedStaffForAction = staff
                                actionType = "ADVANCE"
                                showPaymentDialog = true
                            },
                            modifier = Modifier.weight(1f).height(36.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("💸 Advance", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                selectedStaffForAction = staff
                                actionType = "SALARY_PAYOUT"
                                showPaymentDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                            modifier = Modifier.weight(1f).height(36.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("✓ Pay Salary", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        IconButton(
                            onClick = {
                                shareStaffSalarySlipToWhatsApp(
                                    context = context,
                                    dairyName = dairyName,
                                    staffName = staff.name,
                                    staffPhone = staff.mobile,
                                    monthYear = currentMonthYear,
                                    effectiveDays = totalEffectiveDays,
                                    grossSalary = baseSalary,
                                    advances = advancesTaken,
                                    netPaid = salaryPaid,
                                    paymentMode = "Cash / UPI"
                                )
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "WhatsApp Slip", tint = DairyGreen)
                        }
                    }
                }
            }
        }

        // Recent Payment History
        item {
            Text("RECENT SALARY & ADVANCE PAYMENTS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (paymentsList.isEmpty()) {
            item {
                Text("No advance or salary payments recorded yet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(paymentsList, key = { it.id }) { pay ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("${pay.staffName} • ${pay.type.replace("_", " ")}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Mode: ${pay.paymentMode} • ${pay.notes}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("₹${pay.amount.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (pay.type == "ADVANCE") DangerRed else DairyGreen)
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(onClick = { onDeletePayment(pay.id) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Delete", tint = DangerRed, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPaymentDialog && selectedStaffForAction != null) {
        val targetStaff = selectedStaffForAction!!
        var amountText by remember { mutableStateOf("") }
        var mode by remember { mutableStateOf("CASH") }
        var noteText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            title = {
                Text(if (actionType == "ADVANCE") "Give Advance to ${targetStaff.name}" else "Disburse Salary to ${targetStaff.name}", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount (₹) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick amount chips
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf(500, 1000, 2000, 5000)) { pAmt ->
                            SuggestionChip(
                                onClick = { amountText = pAmt.toString() },
                                label = { Text("₹$pAmt", fontSize = 10.sp) }
                            )
                        }
                    }

                    Text("Payment Mode:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("CASH", "UPI", "BANK").forEach { m ->
                            FilterChip(
                                selected = mode == m,
                                onClick = { mode = m },
                                label = { Text(m) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Notes / Purpose (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            onRecordPayment(targetStaff.id, targetStaff.name, actionType, amt, mode, currentMonthYear, noteText)
                            showPaymentDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (actionType == "ADVANCE") DangerRed else DairyGreen)
                ) {
                    Text("Confirm Payment")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentDialog = false }) { Text("Cancel") }
            }
        )
    }
}
