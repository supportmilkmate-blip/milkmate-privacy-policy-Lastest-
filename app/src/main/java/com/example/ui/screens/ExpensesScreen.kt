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
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.InventoryItemEntity
import com.example.data.local.entity.InventoryTransactionEntity
import com.example.data.repository.DEFAULT_EXPENSE_CATEGORIES
import com.example.ui.MilkMateViewModel
import com.example.ui.theme.*
import com.example.ui.util.AppStrings
import com.example.ui.util.ReportExportHelper
import java.text.SimpleDateFormat
import java.util.*

val DAIRY_FARM_CATEGORIES = listOf(
    "Cattle Feed (Khali/Pellets)",
    "Supplements & Minerals",
    "Veterinary Doctor & Meds",
    "Milker & Labor Salary",
    "Diesel & Delivery Fuel",
    "Electricity & BMC Power",
    "Shed & Equipment Repair",
    "Packaging & Milk Pouches",
    "Other Farm Expense"
)

val PERSONAL_EXPENSE_CATEGORIES = listOf(
    "Household & Grocery",
    "Family Medical & Health",
    "Children School & Education",
    "Personal Travel & Fuel",
    "Other Personal Drawing"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(viewModel: MilkMateViewModel) {
    val context = LocalContext.current
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val inventoryItems by viewModel.inventoryItems.collectAsStateWithLifecycle()
    val inventoryTransactions by viewModel.inventoryTransactions.collectAsStateWithLifecycle()
    val session by viewModel.sessionState.collectAsStateWithLifecycle()
    val business by viewModel.currentBusiness.collectAsStateWithLifecycle()

    // Account Start Date Enforcement
    val accountStartDate = remember(session.accountStartDate, business) {
        val bStart = business?.accountStartDate ?: 0L
        if (bStart > 0L) bStart else if (session.accountStartDate > 0L) session.accountStartDate else System.currentTimeMillis()
    }

    val displayDateFormat = remember { SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()) }
    val monthYearFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val yearMonthNumFormat = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()) }

    val todayCalendar = remember { Calendar.getInstance() }
    val currentYearMonth = remember { yearMonthNumFormat.format(todayCalendar.time) }

    val startCalendar = remember(accountStartDate) {
        Calendar.getInstance().apply { timeInMillis = accountStartDate }
    }
    val startYearMonth = remember(accountStartDate) {
        yearMonthNumFormat.format(Date(accountStartDate))
    }

    var selectedMonthOffset by remember { mutableIntStateOf(0) }
    val viewingCalendar = remember(selectedMonthOffset) {
        Calendar.getInstance().apply { add(Calendar.MONTH, selectedMonthOffset) }
    }
    val viewingYearMonth = remember(viewingCalendar) {
        yearMonthNumFormat.format(viewingCalendar.time)
    }

    val canGoPreviousMonth = remember(viewingYearMonth, startYearMonth) { viewingYearMonth > startYearMonth }
    val canGoNextMonth = remember(viewingYearMonth, currentYearMonth) { viewingYearMonth < currentYearMonth }

    val showFeed = remember(business?.businessMode) {
        com.example.ui.util.BusinessModeFeatures.showCattleFeed(business?.businessMode)
    }

    // Sub-Screen Modes: "FEED", "OPERATIONS", "PERSONAL", "ANALYTICS", "LEDGER"
    var activeViewMode by remember(showFeed) { mutableStateOf(if (showFeed) "FEED" else "OPERATIONS") }
    var searchQuery by remember { mutableStateOf("") }

    // Dialog state
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var initialExpenseIsPersonal by remember { mutableStateOf(false) }
    var expenseToEdit by remember { mutableStateOf<ExpenseEntity?>(null) }
    var expenseToDelete by remember { mutableStateOf<ExpenseEntity?>(null) }

    var showBuyFeedStockDialog by remember { mutableStateOf(false) }
    var preselectedInventoryItem by remember { mutableStateOf<InventoryItemEntity?>(null) }

    LaunchedEffect(inventoryItems) {
        if (inventoryItems.isEmpty()) {
            viewModel.initializeDefaultInventoryItems()
        }
    }

    // Filter valid expenses by account start date and current month
    val validExpenses = remember(expenses, accountStartDate) {
        expenses.filter { it.date >= accountStartDate }
    }
    val monthExpenses = remember(validExpenses, viewingYearMonth) {
        validExpenses.filter { exp -> yearMonthNumFormat.format(Date(exp.date)) == viewingYearMonth }
    }

    val businessExpenses = remember(monthExpenses) { monthExpenses.filter { it.isBusiness() } }
    val personalExpenses = remember(monthExpenses) { monthExpenses.filter { it.isPersonal() } }
    val feedExpenses = remember(monthExpenses, showFeed) {
        monthExpenses.filter {
            if (showFeed) {
                it.isInventoryPurchase || it.category.contains("Feed", ignoreCase = true) || it.category.contains("Supplement", ignoreCase = true) || it.category.contains("INVENTORY", ignoreCase = true)
            } else {
                it.isInventoryPurchase || it.category.contains("Stock", ignoreCase = true) || it.category.contains("Purchase", ignoreCase = true)
            }
        }
    }
    val operationsExpenses = remember(businessExpenses, feedExpenses) {
        businessExpenses.filter { it !in feedExpenses }
    }

    val totalMonthOutflow = remember(monthExpenses) { monthExpenses.sumOf { it.amount } }
    val totalBusinessSpend = remember(businessExpenses) { businessExpenses.sumOf { it.amount } }
    val totalPersonalDrawings = remember(personalExpenses) { personalExpenses.sumOf { it.amount } }
    val totalFeedSpend = remember(feedExpenses) { feedExpenses.sumOf { it.amount } }
    val totalOperationsSpend = remember(operationsExpenses) { operationsExpenses.sumOf { it.amount } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ------------------ Top Bar Header ------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (showFeed) "EXPENSES & FEED OUTFLOW" else "BUSINESS EXPENSES & OUTFLOW",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoyalBluePrimary
                )
                Text(
                    text = if (showFeed) "Dairy Resource Center" else "Expense & P&L Manager",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Export Actions
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = {
                    val headers = listOf("Date", "Type", "Category", "Payment Mode", "Notes", "Amount (₹)")
                    val rows = monthExpenses.map { exp ->
                        listOf(
                            displayDateFormat.format(Date(exp.date)),
                            if (exp.isPersonal()) "Personal" else "Business",
                            exp.category,
                            exp.paymentMethod,
                            exp.notes.ifBlank { exp.safeInventoryName() },
                            "₹${exp.amount.toInt()}"
                        )
                    }
                    ReportExportHelper.generateAndSharePdf(
                        context = context,
                        title = "${business?.businessName ?: "Dairy"} - Expense Statement",
                        period = monthYearFormat.format(viewingCalendar.time),
                        headers = headers,
                        rows = rows,
                        summaryMetrics = listOf(
                            "Total Outflow" to "₹${totalMonthOutflow.toInt()}",
                            "Feed & Stock Spend" to "₹${totalFeedSpend.toInt()}",
                            "Farm Operations" to "₹${totalOperationsSpend.toInt()}",
                            "Personal Drawings" to "₹${totalPersonalDrawings.toInt()}"
                        )
                    )
                }) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF Statement", tint = DangerRed)
                }

                IconButton(onClick = {
                    val headers = listOf("ID", "Date", "Category", "Mode", "Notes", "Amount")
                    val rows = monthExpenses.map { exp ->
                        listOf(
                            exp.id.take(8),
                            displayDateFormat.format(Date(exp.date)),
                            exp.category,
                            exp.paymentMethod,
                            exp.notes,
                            exp.amount.toString()
                        )
                    }
                    ReportExportHelper.generateAndShareExcel(
                        context = context,
                        title = "Expenses_${viewingYearMonth}",
                        headers = headers,
                        rows = rows
                    )
                }) {
                    Icon(Icons.Default.FileDownload, contentDescription = "CSV Export", tint = DairyGreen)
                }
            }
        }

        // ------------------ Month Period Navigator ------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (canGoPreviousMonth) selectedMonthOffset -= 1 },
                    enabled = canGoPreviousMonth
                ) {
                    Icon(
                        Icons.Default.ChevronLeft,
                        contentDescription = "Previous Month",
                        tint = if (canGoPreviousMonth) RoyalBluePrimary else Color.LightGray
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = monthYearFormat.format(viewingCalendar.time),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = RoyalBluePrimary
                    )
                    Text(
                        text = "Total Spend: ₹${totalMonthOutflow.toInt()}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { if (canGoNextMonth) selectedMonthOffset += 1 },
                    enabled = canGoNextMonth
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Next Month",
                        tint = if (canGoNextMonth) RoyalBluePrimary else Color.LightGray
                    )
                }
            }
        }

        // ------------------ Financial Radar Strip ------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExpenseRadarStat(
                    label = if (showFeed) "🌾 Feed & Stock" else "🥛 Product Stock",
                    value = "₹${totalFeedSpend.toInt()}",
                    color = FreshGold,
                    onClick = { activeViewMode = "FEED" }
                )
                ExpenseRadarStat(
                    label = if (showFeed) "🚜 Farm Ops" else "🏢 Business Ops",
                    value = "₹${totalOperationsSpend.toInt()}",
                    color = Color(0xFF0284C7),
                    onClick = { activeViewMode = "OPERATIONS" }
                )
                ExpenseRadarStat(
                    label = "👤 Personal",
                    value = "₹${totalPersonalDrawings.toInt()}",
                    color = Color(0xFF9333EA),
                    onClick = { activeViewMode = "PERSONAL" }
                )
                ExpenseRadarStat(
                    label = "📊 Total Outflow",
                    value = "₹${totalMonthOutflow.toInt()}",
                    color = DangerRed,
                    onClick = { activeViewMode = "ANALYTICS" }
                )
            }
        }

        // ------------------ Fast Action Buttons ------------------
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    preselectedInventoryItem = inventoryItems.firstOrNull()
                    showBuyFeedStockDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = FreshGold),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            ) {
                Icon(Icons.Default.AddShoppingCart, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (showFeed) "+ Buy Feed Stock" else "+ Buy Stock / SKUs", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Button(
                onClick = {
                    expenseToEdit = null
                    initialExpenseIsPersonal = false
                    showAddExpenseDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (showFeed) "+ Farm Expense" else "+ Business Expense", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // ------------------ Mode Selector Tabs ------------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = activeViewMode == "FEED",
                onClick = { activeViewMode = "FEED" },
                label = { Text(if (showFeed) "🌾 Feed & Stock (${feedExpenses.size})" else "🥛 Stock Purchases (${feedExpenses.size})", fontSize = 11.sp, fontWeight = if (activeViewMode == "FEED") FontWeight.Bold else FontWeight.Normal) }
            )
            FilterChip(
                selected = activeViewMode == "OPERATIONS",
                onClick = { activeViewMode = "OPERATIONS" },
                label = { Text(if (showFeed) "🚜 Farm Ops (${operationsExpenses.size})" else "🏢 Operations (${operationsExpenses.size})", fontSize = 11.sp, fontWeight = if (activeViewMode == "OPERATIONS") FontWeight.Bold else FontWeight.Normal) }
            )
            FilterChip(
                selected = activeViewMode == "PERSONAL",
                onClick = { activeViewMode = "PERSONAL" },
                label = { Text("👤 Personal (${personalExpenses.size})", fontSize = 11.sp, fontWeight = if (activeViewMode == "PERSONAL") FontWeight.Bold else FontWeight.Normal) }
            )
            FilterChip(
                selected = activeViewMode == "ANALYTICS",
                onClick = { activeViewMode = "ANALYTICS" },
                label = { Text("📊 Breakdown & Donut", fontSize = 11.sp, fontWeight = if (activeViewMode == "ANALYTICS") FontWeight.Bold else FontWeight.Normal) }
            )
            FilterChip(
                selected = activeViewMode == "LEDGER",
                onClick = { activeViewMode = "LEDGER" },
                label = { Text("📋 All Ledger (${monthExpenses.size})", fontSize = 11.sp, fontWeight = if (activeViewMode == "LEDGER") FontWeight.Bold else FontWeight.Normal) }
            )
        }

        // Search Bar for Ledger & Lists
        if (activeViewMode != "ANALYTICS") {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("🔎 Search expenses by note, item or category...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )
        }

        // ------------------ Tab Content ------------------
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (activeViewMode) {
                "FEED" -> {
                    val feedList = feedExpenses.filter {
                        searchQuery.isBlank() || it.category.contains(searchQuery, ignoreCase = true) || it.notes.contains(searchQuery, ignoreCase = true) || it.safeInventoryName().contains(searchQuery, ignoreCase = true)
                    }

                    if (feedList.isEmpty()) {
                        EmptyExpenseFeedState(
                            showFeed = showFeed,
                            onBuyStock = {
                                preselectedInventoryItem = inventoryItems.firstOrNull()
                                showBuyFeedStockDialog = true
                            }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                // Stock Alert Card
                                val lowStockItems = inventoryItems.filter { it.stockAlertStatus != "NORMAL" }
                                if (lowStockItems.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = FreshGold.copy(alpha = 0.12f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, FreshGold.copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                                                Column {
                                                    val lowTitle = if (showFeed) "${lowStockItems.size} Feeds Low on Stock" else "${lowStockItems.size} SKUs Low on Stock"
                                                    Text(lowTitle, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFB45309))
                                                    Text(lowStockItems.joinToString(", ") { "${it.itemName} (${it.currentStock.toInt()}${it.unit})" }, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                            Button(
                                                onClick = {
                                                    preselectedInventoryItem = lowStockItems.firstOrNull()
                                                    showBuyFeedStockDialog = true
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = FreshGold),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text("Order Now", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }

                            items(feedList, key = { it.id }) { exp ->
                                ExpenseRowItemCard(
                                    expense = exp,
                                    dateStr = displayDateFormat.format(Date(exp.date)),
                                    onEdit = {
                                        expenseToEdit = exp
                                        showAddExpenseDialog = true
                                    },
                                    onDelete = { expenseToDelete = exp }
                                )
                            }
                        }
                    }
                }

                "OPERATIONS" -> {
                    val opsList = operationsExpenses.filter {
                        searchQuery.isBlank() || it.category.contains(searchQuery, ignoreCase = true) || it.notes.contains(searchQuery, ignoreCase = true)
                    }

                    if (opsList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("No farm operations expenses recorded this month.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        expenseToEdit = null
                                        initialExpenseIsPersonal = false
                                        showAddExpenseDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                                ) {
                                    Text("+ Add Farm Operation Expense")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(opsList, key = { it.id }) { exp ->
                                ExpenseRowItemCard(
                                    expense = exp,
                                    dateStr = displayDateFormat.format(Date(exp.date)),
                                    onEdit = {
                                        expenseToEdit = exp
                                        showAddExpenseDialog = true
                                    },
                                    onDelete = { expenseToDelete = exp }
                                )
                            }
                        }
                    }
                }

                "PERSONAL" -> {
                    val personalList = personalExpenses.filter {
                        searchQuery.isBlank() || it.category.contains(searchQuery, ignoreCase = true) || it.notes.contains(searchQuery, ignoreCase = true)
                    }

                    if (personalList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("No household or personal drawings recorded this month.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        expenseToEdit = null
                                        initialExpenseIsPersonal = true
                                        showAddExpenseDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA))
                                ) {
                                    Text("+ Add Personal Drawing")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(personalList, key = { it.id }) { exp ->
                                ExpenseRowItemCard(
                                    expense = exp,
                                    dateStr = displayDateFormat.format(Date(exp.date)),
                                    onEdit = {
                                        expenseToEdit = exp
                                        showAddExpenseDialog = true
                                    },
                                    onDelete = { expenseToDelete = exp }
                                )
                            }
                        }
                    }
                }

                "ANALYTICS" -> {
                    ExpenseAnalyticsView(
                        totalMonth = totalMonthOutflow,
                        totalBusiness = totalBusinessSpend,
                        totalPersonal = totalPersonalDrawings,
                        totalFeed = totalFeedSpend,
                        totalOps = totalOperationsSpend,
                        expenses = monthExpenses
                    )
                }

                "LEDGER" -> {
                    val allList = monthExpenses.filter {
                        searchQuery.isBlank() || it.category.contains(searchQuery, ignoreCase = true) || it.notes.contains(searchQuery, ignoreCase = true) || it.paymentMethod.contains(searchQuery, ignoreCase = true)
                    }

                    if (allList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No expenses found matching filter.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(allList, key = { it.id }) { exp ->
                                ExpenseRowItemCard(
                                    expense = exp,
                                    dateStr = displayDateFormat.format(Date(exp.date)),
                                    onEdit = {
                                        expenseToEdit = exp
                                        showAddExpenseDialog = true
                                    },
                                    onDelete = { expenseToDelete = exp }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ------------------ Modals & Dialogs ------------------
    if (showAddExpenseDialog) {
        val modeCategories = com.example.ui.util.BusinessModeFeatures.getExpenseCategoriesForMode(business?.businessMode)
        val activeCategories = if (!showFeed) {
            session.customExpenseCategories.filter { cat ->
                !cat.contains("Feed", ignoreCase = true) &&
                !cat.contains("Fodder", ignoreCase = true) &&
                !cat.contains("Silage", ignoreCase = true) &&
                !cat.contains("Khali", ignoreCase = true) &&
                !cat.contains("Cattle", ignoreCase = true) &&
                !cat.contains("Veterinary", ignoreCase = true) &&
                !cat.contains("Doctor & Meds", ignoreCase = true) &&
                !cat.contains("Milker", ignoreCase = true)
            }.ifEmpty { modeCategories }
        } else {
            session.customExpenseCategories.ifEmpty { modeCategories }
        }

        AddEditExpenseModal(
            expense = expenseToEdit,
            initialIsPersonal = initialExpenseIsPersonal,
            customCategories = activeCategories,
            showFeed = showFeed,
            onAddCategory = { viewModel.addExpenseCategory(it) },
            onDismiss = {
                showAddExpenseDialog = false
                expenseToEdit = null
            },
            onSave = { amount, category, notes, method, isPersonal, date ->
                viewModel.saveExpense(
                    id = expenseToEdit?.id,
                    amount = amount,
                    category = category,
                    subcategory = if (isPersonal) "PERSONAL" else "BUSINESS",
                    notes = notes,
                    paymentMethod = method,
                    date = date,
                    expenseType = if (isPersonal) "PERSONAL" else "BUSINESS"
                )
                showAddExpenseDialog = false
                expenseToEdit = null
            }
        )
    }

    if (showBuyFeedStockDialog) {
        BuyFeedStockModal(
            inventoryItems = inventoryItems,
            preselectedItem = preselectedInventoryItem,
            showFeed = showFeed,
            onDismiss = {
                showBuyFeedStockDialog = false
                preselectedInventoryItem = null
            },
            onSave = { itemId, qty, rate, supplier, method, notes, date ->
                viewModel.recordStockPurchase(
                    itemId = itemId,
                    quantity = qty,
                    rate = rate,
                    date = date,
                    supplier = supplier,
                    paymentStatus = "PAID",
                    paymentMethod = method,
                    notes = notes
                ) { success, err ->
                    if (success) {
                        showBuyFeedStockDialog = false
                        preselectedInventoryItem = null
                    } else {
                        viewModel.showMessage(err ?: "Failed to save stock purchase")
                    }
                }
            }
        )
    }

    // Delete Confirmation
    expenseToDelete?.let { exp ->
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("Delete Expense Record?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${exp.category}' of ₹${exp.amount.toInt()}? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteExpense(exp.id)
                        expenseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ExpenseRadarStat(
    label: String,
    value: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun ExpenseRowItemCard(
    expense: ExpenseEntity,
    dateStr: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isFeed = expense.isInventoryPurchase || expense.category.contains("Feed", ignoreCase = true) || expense.category.contains("Supplement", ignoreCase = true)
    val isPersonal = expense.isPersonal()

    val badgeColor = when {
        isPersonal -> Color(0xFF9333EA)
        isFeed -> FreshGold
        else -> Color(0xFF0284C7)
    }

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
                        .background(badgeColor.copy(alpha = 0.15f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            isPersonal -> Icons.Default.Person
                            isFeed -> Icons.Default.Grass
                            else -> Icons.Default.ReceiptLong
                        },
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (isFeed && expense.safeInventoryName().isNotBlank()) expense.safeInventoryName() else expense.category,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = badgeColor.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = if (isPersonal) "Personal" else if (isFeed) "Feed Stock" else "Farm Ops",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "${expense.paymentMethod} • $dateStr ${if (expense.notes.isNotBlank()) "• ${expense.notes}" else ""}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "₹${expense.amount.toInt()}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = DangerRed
                )

                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DangerRed.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun EmptyExpenseFeedState(showFeed: Boolean, onBuyStock: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(if (showFeed) Icons.Default.Grass else Icons.Default.Inventory2, contentDescription = null, tint = FreshGold.copy(alpha = 0.6f), modifier = Modifier.size(56.dp))
            Text(
                if (showFeed) "No Feed or Stock Purchases recorded this month." else "No Product Stock Purchases recorded this month.",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                if (showFeed) "Purchase cattle feed, mustard cake, silage, or medicines to log stock & expense." else "Log purchases of milk pouches, paneer, curd, ghee, or packaging supplies.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = onBuyStock,
                colors = ButtonDefaults.buttonColors(containerColor = FreshGold),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (showFeed) "+ Record Feed / Stock Purchase" else "+ Record Stock Purchase", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun ExpenseAnalyticsView(
    totalMonth: Double,
    totalBusiness: Double,
    totalPersonal: Double,
    totalFeed: Double,
    totalOps: Double,
    expenses: List<ExpenseEntity>
) {
    val categoryTotals = remember(expenses) {
        expenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Dairy Outflow Breakdown", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                    // Visual Category Distribution Bars
                    categoryTotals.take(6).forEach { (cat, amt) ->
                        val percent = if (totalMonth > 0) (amt / totalMonth).toFloat() else 0f
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(cat, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("₹${amt.toInt()} (${(percent * 100).toInt()}%)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                            }
                            LinearProgressIndicator(
                                progress = { percent },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (cat.contains("Feed", ignoreCase = true)) FreshGold else RoyalBluePrimary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            // AI Cost Optimization Tip Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DairyGreen.copy(alpha = 0.1f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, DairyGreen.copy(alpha = 0.3f))
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(24.dp))
                    Column {
                        Text("Dairy Profit Tip", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DairyGreen)
                        val feedRatio = if (totalMonth > 0) ((totalFeed / totalMonth) * 100).toInt() else 0
                        Text(
                            "Feed accounts for $feedRatio% of your total spend. Buying cattle feed in bulk 50kg bags or seasonal silage reduces unit cost by 12–18%.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditExpenseModal(
    expense: ExpenseEntity?,
    initialIsPersonal: Boolean,
    customCategories: List<String> = DEFAULT_EXPENSE_CATEGORIES,
    showFeed: Boolean = true,
    onAddCategory: (String) -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (Double, String, String, String, Boolean, Long) -> Unit
) {
    val context = LocalContext.current
    val defaultBusinessCategory = customCategories.firstOrNull() ?: if (showFeed) "Cattle Feed" else "Dairy Stock Purchase"
    var amountText by remember { mutableStateOf(expense?.amount?.toInt()?.toString() ?: "") }
    var isPersonal by remember { mutableStateOf(expense?.isPersonal() ?: initialIsPersonal) }
    var category by remember {
        mutableStateOf(
            expense?.category ?: if (isPersonal) PERSONAL_EXPENSE_CATEGORIES.first() else defaultBusinessCategory
        )
    }
    var notes by remember { mutableStateOf(expense?.notes ?: "") }
    var paymentMethod by remember { mutableStateOf(expense?.paymentMethod ?: "CASH") }
    var dateMillis by remember { mutableStateOf(expense?.date ?: System.currentTimeMillis()) }

    var showInlineAddCategory by remember { mutableStateOf(false) }
    var newCategoryInput by remember { mutableStateOf("") }

    val displayDateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (expense == null) "Add Expense Entry" else "Edit Expense", fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = null) }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Business vs Personal Toggle
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !isPersonal,
                        onClick = {
                            isPersonal = false
                            category = defaultBusinessCategory
                        },
                        label = { Text(if (showFeed) "🚜 Dairy Farm Business" else "🏢 Dairy Business", fontSize = 11.sp, fontWeight = if (!isPersonal) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = isPersonal,
                        onClick = {
                            isPersonal = true
                            category = PERSONAL_EXPENSE_CATEGORIES.first()
                        },
                        label = { Text("👤 Personal Drawing", fontSize = 11.sp, fontWeight = if (isPersonal) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Amount
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
                    listOf("100", "200", "500", "1000", "2000", "5000").forEach { p ->
                        FilterChip(
                            selected = amountText == p,
                            onClick = { amountText = p },
                            label = { Text("₹$p", fontSize = 10.sp) }
                        )
                    }
                }

                // Category Selector
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Expense Category *", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    if (!isPersonal) {
                        Text(
                            text = "+ New Category",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoyalBluePrimary,
                            modifier = Modifier.clickable { showInlineAddCategory = true }
                        )
                    }
                }

                val categories = if (isPersonal) PERSONAL_EXPENSE_CATEGORIES else customCategories
                var expandedCat by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expandedCat = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(category, fontWeight = FontWeight.SemiBold)
                    }
                    DropdownMenu(
                        expanded = expandedCat,
                        onDismissRequest = { expandedCat = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        categories.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c) },
                                onClick = {
                                    category = c
                                    expandedCat = false
                                }
                            )
                        }
                    }
                }

                // Inline add category box
                if (showInlineAddCategory) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newCategoryInput,
                                onValueChange = { newCategoryInput = it },
                                placeholder = { Text("New category name...", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f).height(46.dp)
                            )
                            Button(
                                onClick = {
                                    if (newCategoryInput.isNotBlank()) {
                                        onAddCategory(newCategoryInput.trim())
                                        category = newCategoryInput.trim()
                                        newCategoryInput = ""
                                        showInlineAddCategory = false
                                    }
                                },
                                enabled = newCategoryInput.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.height(46.dp)
                            ) {
                                Text("Add", fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Payment Mode
                Text("Payment Method *", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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

                // Date Picker
                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val newCal = Calendar.getInstance().apply { set(y, m, d, 0, 0, 0) }
                                dateMillis = newCal.timeInMillis
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Date: ${displayDateFormat.format(Date(dateMillis))}", fontSize = 12.sp)
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Vendor / Note (e.g. 2 bags mustard cake from Kisan Kendra)") },
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
                        onSave(amt, category, notes, paymentMethod, isPersonal, dateMillis)
                    }
                },
                enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0.0,
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("✓ Save Expense", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun BuyFeedStockModal(
    inventoryItems: List<InventoryItemEntity>,
    preselectedItem: InventoryItemEntity?,
    showFeed: Boolean = true,
    initialQuantity: Double? = null,
    initialRate: Double? = null,
    initialSupplier: String? = null,
    onDismiss: () -> Unit,
    onSave: (String, Double, Double, String, String, String, Long) -> Unit
) {
    val context = LocalContext.current
    var selectedItem by remember { mutableStateOf(preselectedItem ?: inventoryItems.firstOrNull()) }
    var quantityText by remember {
        mutableStateOf(
            if (initialQuantity != null && initialQuantity > 0) {
                if (initialQuantity % 1.0 == 0.0) initialQuantity.toInt().toString() else initialQuantity.toString()
            } else "50"
        )
    }
    var rateText by remember {
        mutableStateOf(
            if (initialRate != null && initialRate > 0) {
                if (initialRate % 1.0 == 0.0) initialRate.toInt().toString() else initialRate.toString()
            } else (selectedItem?.costPerUnit?.toInt()?.toString() ?: "35")
        )
    }
    var supplierName by remember { mutableStateOf(initialSupplier ?: "") }
    var paymentMethod by remember { mutableStateOf("CASH") }
    var notes by remember { mutableStateOf("") }
    var dateMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    val qty = quantityText.toDoubleOrNull() ?: 50.0
    val rate = rateText.toDoubleOrNull() ?: 35.0
    val totalCost = qty * rate

    val displayDateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(if (showFeed) "🌾 Purchase Cattle Feed / Stock" else "🥛 Purchase Product Stock", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Updates inventory stock & registers expense", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = null) }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Item Selector
                Text(if (showFeed) "Select Feed / Supply Item *" else "Select Dairy / Inventory Item *", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RoyalBluePrimary)
                var expandedItem by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expandedItem = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(selectedItem?.let { "${it.itemName} (Stock: ${it.currentStock.toInt()}${it.unit})" } ?: "Select Item", fontWeight = FontWeight.SemiBold)
                    }
                    DropdownMenu(
                        expanded = expandedItem,
                        onDismissRequest = { expandedItem = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        inventoryItems.forEach { item ->
                            DropdownMenuItem(
                                text = { Text("${item.itemName} • Current Stock: ${item.currentStock.toInt()} ${item.unit}") },
                                onClick = {
                                    selectedItem = item
                                    rateText = if (item.costPerUnit > 0) item.costPerUnit.toInt().toString() else "35"
                                    expandedItem = false
                                }
                            )
                        }
                    }
                }

                // Quantity & Rate
                val unit = selectedItem?.unit ?: "kg"
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("Quantity ($unit) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = rateText,
                        onValueChange = { rateText = it },
                        label = { Text("Rate (₹/$unit) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Preset Quantity Chips
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("10", "25", "50", "100", "500").forEach { q ->
                        FilterChip(
                            selected = quantityText == q,
                            onClick = { quantityText = q },
                            label = { Text("$q $unit", fontSize = 10.sp) }
                        )
                    }
                }

                // Total Cost Banner
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = FreshGold.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Purchase Amount:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("₹${totalCost.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFB45309))
                    }
                }

                OutlinedTextField(
                    value = supplierName,
                    onValueChange = { supplierName = it },
                    label = { Text(if (showFeed) "Supplier Name (e.g. Kisan Agro Center)" else "Vendor / Supplier (e.g. Dairy Plant)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Supplier suggestions
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val suppliers = if (showFeed) listOf("Kisan Agro", "Dairy Mandi", "Local Feed Store")
                                    else listOf("Dairy Mandi", "Wholesale Plant", "Packaging Mart")
                    suppliers.forEach { s ->
                        SuggestionChip(
                            onClick = { supplierName = s },
                            label = { Text(s, fontSize = 10.sp) }
                        )
                    }
                }

                // Payment Mode
                Text("Payment Method *", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val item = selectedItem ?: return@Button
                    if (qty > 0 && rate > 0) {
                        onSave(item.id, qty, rate, supplierName, paymentMethod, notes, dateMillis)
                    }
                },
                enabled = selectedItem != null && qty > 0 && rate > 0,
                colors = ButtonDefaults.buttonColors(containerColor = FreshGold),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("✓ Record Purchase & Expense", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
