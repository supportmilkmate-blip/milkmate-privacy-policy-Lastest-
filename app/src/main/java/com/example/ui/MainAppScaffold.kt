package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.util.AppStrings
import com.example.ui.util.BusinessModeFeatures
import com.example.ui.util.loc
import kotlinx.coroutines.launch

sealed class BottomTab(val index: Int, val titleKey: String, val title: String, val icon: ImageVector) {
    object Home : BottomTab(0, "home", "Home", Icons.Default.Home)
    object Delivery : BottomTab(1, "delivery", "Delivery", Icons.Default.LocalShipping)
    object Customers : BottomTab(2, "customers", "Customers", Icons.Default.People)
    object Payment : BottomTab(3, "payment", "Payments", Icons.Default.Payment)
    object Manage : BottomTab(4, "manage", "Operations", Icons.Default.Tune)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(viewModel: MilkMateViewModel) {
    var activeBottomTab by remember { mutableIntStateOf(0) }
    var subScreen by remember { mutableStateOf<String?>(null) } // "PROFIT", "INVENTORY", "REPORTS", "SETTINGS", "SUBSCRIPTION", "STAFF", "AI_ASSISTANT", "EXPENSES", "ORDERS", "MANAGE", "COLLECTION_DESK"
    var aiInitialPrompt by remember { mutableStateOf<String?>(null) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var showBusinessModeSelector by remember { mutableStateOf(false) }

    val session by viewModel.sessionState.collectAsStateWithLifecycle()
    val business by viewModel.currentBusiness.collectAsStateWithLifecycle()
    val currentMode = business?.businessMode ?: "FARMER"
    val selectedShift by viewModel.selectedShift.collectAsStateWithLifecycle()
    val pendingSync by viewModel.pendingSyncCount.collectAsStateWithLifecycle()
    val snackbarMsg by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val inventoryItems by viewModel.inventoryItems.collectAsStateWithLifecycle()
    val staffList by viewModel.staffList.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    // BackHandler: Navigate back from subScreens or return to Home tab
    BackHandler(enabled = subScreen != null || activeBottomTab != 0) {
        if (subScreen != null) {
            subScreen = null
        } else {
            activeBottomTab = 0
        }
    }

        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(320.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // 1. DRAWER HERO HEADER
                    Surface(
                        color = OceanMidnight,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .statusBarsPadding()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = FreshGold,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.WaterDrop,
                                            contentDescription = null,
                                            tint = OceanMidnight,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = (business?.businessName?.ifBlank { "MilkMate Dairy" } ?: "MilkMate Dairy").uppercase(),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "Smart Dairy Operating System",
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            // Active Business Model Chip
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.15f),
                                modifier = Modifier.clickable {
                                    coroutineScope.launch { drawerState.close() }
                                    showBusinessModeSelector = true
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val modeLabel = when (currentMode) {
                                        "FARMER_WHOLESALE" -> "🚜 Farm (Center Supply)"
                                        "COLLECTION_CENTER" -> "🏢 Collection Centre (BMC)"
                                        "TRADER" -> "🚚 Milk Trader / Vendor"
                                        "INTEGRATED" -> "🌐 Integrated Farm"
                                        "PROCESSING_UNIT" -> "🧀 Dairy Processor"
                                        "RETAIL_PARLOUR" -> "🛒 Dairy Parlour Booth"
                                        "GAUSHALA" -> "🐄 Gaushala A2 Farm"
                                        else -> "🚜 Dairy Farm"
                                    }
                                    Text(modeLabel, color = FreshGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("⇄ Switch", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                                }
                            }

                            // Shift Switcher inside Drawer
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedShift == "MORNING") FreshGoldLight else RoyalBlueLight,
                                modifier = Modifier.clickable {
                                    val next = if (selectedShift == "MORNING") "EVENING" else "MORNING"
                                    viewModel.setShift(next)
                                    viewModel.showMessage("Switched to $next Shift")
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = if (selectedShift == "MORNING") "🌅 Active: MORNING SHIFT ⇄" else "🌆 Active: EVENING SHIFT ⇄",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedShift == "MORNING") WarmHoney else RoyalBluePrimary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // ================= HOME DASHBOARD =================
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = null, tint = RoyalBluePrimary) },
                        label = { Text("Home Dashboard", fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp) },
                        badge = {
                            Surface(shape = CircleShape, color = RoyalBlueLight) {
                                Text("Live", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        },
                        selected = subScreen == null && activeBottomTab == 0,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            subScreen = null
                            activeBottomTab = 0
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = BorderSubtle)

                    // ================= GROUP 1: MANAGE SALES =================
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MANAGE SALES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DairyGreen,
                            letterSpacing = 0.5.sp
                        )
                        Surface(shape = RoundedCornerShape(4.dp), color = DairyGreenLight) {
                            Text("Sales & Intake", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = DairyGreen, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                        }
                    }

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.LocalDrink, contentDescription = null, tint = DairyGreen) },
                        label = {
                            Column {
                                Text("Milk Entry & Collection Desk", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Record shift collection & FAT/SNF", fontSize = 10.sp, color = TextSecondary)
                            }
                        },
                        selected = subScreen == "COLLECTION_DESK",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            subScreen = "COLLECTION_DESK"
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.LocalShipping, contentDescription = null, tint = RoyalBluePrimary) },
                        label = {
                            Column {
                                Text("Deliveries & Distribution", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Routes, drop run-sheets & tankers", fontSize = 10.sp, color = TextSecondary)
                            }
                        },
                        selected = subScreen == null && activeBottomTab == 1,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            subScreen = null
                            activeBottomTab = 1
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.People, contentDescription = null, tint = Color(0xFF673AB7)) },
                        label = {
                            Column {
                                Text("Customers & Farmers Khata", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Ledgers, receivables & balances", fontSize = 10.sp, color = TextSecondary)
                            }
                        },
                        badge = {
                            if (customers.isNotEmpty()) {
                                Surface(shape = CircleShape, color = Color(0xFFEDE7F6)) {
                                    Text("${customers.size}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF673AB7), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        },
                        selected = subScreen == null && activeBottomTab == 2,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            subScreen = null
                            activeBottomTab = 2
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = WarmHoney) },
                        label = {
                            Column {
                                Text("Customer Orders & Slips", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Advance bookings & delivery notes", fontSize = 10.sp, color = TextSecondary)
                            }
                        },
                        selected = subScreen == "ORDERS",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            subScreen = "ORDERS"
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = BorderSubtle)

                    // ================= GROUP 2: INVENTORY CONTROL =================
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "INVENTORY CONTROL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = WarmHoney,
                            letterSpacing = 0.5.sp
                        )
                        Surface(shape = RoundedCornerShape(4.dp), color = FreshGoldLight) {
                            Text("Stock & Ops", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = WarmHoney, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                        }
                    }

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Inventory2, contentDescription = null, tint = WarmHoney) },
                        label = {
                            Column {
                                Text("Feed & Product Stock Inventory", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Cattle feed bags & low-stock alerts", fontSize = 10.sp, color = TextSecondary)
                            }
                        },
                        badge = {
                            if (inventoryItems.isNotEmpty()) {
                                Surface(shape = CircleShape, color = FreshGoldLight) {
                                    Text("${inventoryItems.size}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = WarmHoney, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        },
                        selected = subScreen == "INVENTORY",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            subScreen = "INVENTORY"
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    if (BusinessModeFeatures.showHerdBreeding(currentMode)) {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Pets, contentDescription = null, tint = GrassGreen) },
                            label = {
                                Column {
                                    Text("Cattle Herd & Breeding Lifecycle", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("Lactation, AI breeding & vaccination", fontSize = 10.sp, color = TextSecondary)
                                }
                            },
                            selected = subScreen == "CATTLE_BREEDING",
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                subScreen = "CATTLE_BREEDING"
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Tune, contentDescription = null, tint = RoyalBluePrimary) },
                        label = {
                            Column {
                                Text("Operations Control Hub", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Routes, shifts, rates & catalog", fontSize = 10.sp, color = TextSecondary)
                            }
                        },
                        selected = subScreen == "MANAGE",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            subScreen = "MANAGE"
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = DangerRed) },
                        label = {
                            Column {
                                Text("Milk Wastage & Reconciliation", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Daily volume balance & spoilage loss", fontSize = 10.sp, color = TextSecondary)
                            }
                        },
                        selected = subScreen == "MILK_WASTAGE",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            subScreen = "MILK_WASTAGE"
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = BorderSubtle)

                    // ================= GROUP 3: FINANCIAL REPORTS =================
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FINANCIAL REPORTS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = RoyalBluePrimary,
                            letterSpacing = 0.5.sp
                        )
                        Surface(shape = RoundedCornerShape(4.dp), color = RoyalBlueLight) {
                            Text("P&L & Cash", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                        }
                    }

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Payment, contentDescription = null, tint = DairyGreen) },
                        label = {
                            Column {
                                Text("Payments Hub (Cash & UPI)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Record cash in, pay farmers & dues", fontSize = 10.sp, color = TextSecondary)
                            }
                        },
                        selected = subScreen == null && activeBottomTab == 3,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            subScreen = null
                            activeBottomTab = 3
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Receipt, contentDescription = null, tint = DangerRed) },
                        label = {
                            Column {
                                Text("Dairy Operating Expenses", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Feed purchase, fuel & overheads", fontSize = 10.sp, color = TextSecondary)
                            }
                        },
                        selected = subScreen == "EXPENSES",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            subScreen = "EXPENSES"
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = GoldenOrange) },
                        label = {
                            Column {
                                Text("Profit & Loss (P&L) Spread", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Revenue vs expenses & net margins", fontSize = 10.sp, color = TextSecondary)
                            }
                        },
                        selected = subScreen == "PROFIT",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            subScreen = "PROFIT"
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Analytics, contentDescription = null, tint = RoyalBluePrimary) },
                        label = {
                            Column {
                                Text("Analytics & Export Reports", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Periodic reports & PDF/Excel exports", fontSize = 10.sp, color = TextSecondary)
                            }
                        },
                        selected = subScreen == "REPORTS",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            subScreen = "REPORTS"
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = BorderSubtle)

                    // ================= GROUP 4: SETTINGS =================
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SETTINGS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF9C27B0),
                            letterSpacing = 0.5.sp
                        )
                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFF3E5F5)) {
                            Text("System & Tools", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9C27B0), modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                        }
                    }

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF9C27B0)) },
                        label = {
                            Column {
                                Text("Dairy Settings & Milk Rates", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Configure formulas, rates & profile", fontSize = 10.sp, color = TextSecondary)
                            }
                        },
                        selected = subScreen == "SETTINGS",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            subScreen = "SETTINGS"
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = FreshGold) },
                        label = {
                            Column {
                                Text("AI Dairy Advisor", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Gemini AI smart yield & profit advice", fontSize = 10.sp, color = TextSecondary)
                            }
                        },
                        selected = subScreen == "AI_ASSISTANT",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            aiInitialPrompt = null
                            subScreen = "AI_ASSISTANT"
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Badge, contentDescription = null, tint = RoyalBluePrimary) },
                        label = {
                            Column {
                                Text("Staff & Attendance", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Daily worker roster & wage ledger", fontSize = 10.sp, color = TextSecondary)
                            }
                        },
                        badge = {
                            if (staffList.isNotEmpty()) {
                                Surface(shape = CircleShape, color = RoyalBlueLight) {
                                    Text("${staffList.size}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        },
                        selected = subScreen == "STAFF",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            subScreen = "STAFF"
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Star, contentDescription = null, tint = FreshGold) },
                        label = {
                            Column {
                                Text("MilkMate Pro Subscription", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Multi-user sync & premium tools", fontSize = 10.sp, color = TextSecondary)
                            }
                        },
                        selected = subScreen == "SUBSCRIPTION",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            subScreen = "SUBSCRIPTION"
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Logout Action
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Logout, contentDescription = null, tint = DangerRed) },
                        label = { Text("Logout Account", fontWeight = FontWeight.Bold, color = DangerRed, fontSize = 13.sp) },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            showLogoutConfirm = true
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "MilkMate Dairy OS v2.0 • Offline-Ready",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    ) {
        Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left side: Menu / Back Button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (subScreen != null) MaterialTheme.colorScheme.primaryContainer else RoyalBlueLight,
                        modifier = Modifier
                            .testTag("drawer_menu_btn")
                            .clickable {
                                if (subScreen != null) {
                                    subScreen = null
                                } else {
                                    coroutineScope.launch { drawerState.open() }
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (subScreen != null) Icons.Default.ArrowBack else Icons.Default.Menu,
                                contentDescription = if (subScreen != null) "Back" else "Open Menu",
                                tint = RoyalBluePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (subScreen != null) "Back" else "Menu",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = RoyalBluePrimary
                            )
                        }
                    }

                    // Center: Brand Logo + MilkMate Title + Shift Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                            .clickable {
                                subScreen = null
                                activeBottomTab = 0
                            }
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = RoyalBlueLight,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.WaterDrop,
                                    contentDescription = "MilkMate",
                                    tint = RoyalBluePrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MilkMate",
                            fontWeight = FontWeight.ExtraBold,
                            color = RoyalBluePrimary,
                            fontSize = 16.sp,
                            maxLines = 1
                        )
                    }

                    // Right side: Top Action Icons (Compact 32dp size)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // AI Advisor Button
                        IconButton(
                            onClick = {
                                aiInitialPrompt = null
                                subScreen = "AI_ASSISTANT"
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = RoyalBlueLight,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = "MilkMate AI Assistant",
                                        tint = RoyalBluePrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }

                        // Notification Bell Button with Badge
                        IconButton(
                            onClick = { showNotificationDialog = true },
                            modifier = Modifier.size(34.dp)
                        ) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = GoldenOrange,
                                        contentColor = Color.White
                                    ) {
                                        Text("!", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = "Notifications & Reminders",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Language Switcher Button
                        IconButton(
                            onClick = { showLanguageDialog = true },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Default.Translate,
                                contentDescription = "Language",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Settings Button
                        IconButton(
                            onClick = { subScreen = "SETTINGS" },
                            modifier = Modifier.size(34.dp).testTag("top_bar_settings_button")
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (subScreen == "SETTINGS") RoyalBluePrimary else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = if (subScreen == "SETTINGS") Color.White else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Logout Button
                        IconButton(
                            onClick = { showLogoutConfirm = true },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Default.ExitToApp,
                                contentDescription = "Sign Out",
                                tint = DangerRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                tonalElevation = 4.dp
            ) {
                val tabs = listOf(
                    BottomTab.Home,
                    BottomTab.Delivery,
                    BottomTab.Customers,
                    BottomTab.Payment,
                    BottomTab.Manage
                )

                tabs.forEachIndexed { index, tab ->
                    val isSelected = subScreen == null && activeBottomTab == index
                    val (rawTitle, tabIcon) = when {
                        currentMode == "FARMER_WHOLESALE" && index == 1 -> "Herd & Milking" to Icons.Default.Pets
                        currentMode == "FARMER_WHOLESALE" && index == 2 -> "Dispatches" to Icons.Default.LocalShipping
                        currentMode == "FARMER_WHOLESALE" && index == 3 -> "Center Slips" to Icons.Default.ReceiptLong
                        currentMode == "PROCESSING_UNIT" && index == 1 -> "Inventory" to Icons.Default.Inventory2
                        currentMode == "PROCESSING_UNIT" && index == 2 -> "Merchants" to Icons.Default.Storefront
                        currentMode == "PROCESSING_UNIT" && index == 3 -> "Invoicing" to Icons.Default.ReceiptLong
                        currentMode == "RETAIL_PARLOUR" && index == 1 -> "Counter POS" to Icons.Default.PointOfSale
                        currentMode == "RETAIL_PARLOUR" && index == 2 -> "Inventory" to Icons.Default.Inventory2
                        currentMode == "RETAIL_PARLOUR" && index == 3 -> "Sales" to Icons.Default.ReceiptLong
                        currentMode == "COLLECTION_CENTER" && index == 1 -> "Collection" to Icons.Default.WaterDrop
                        currentMode == "COLLECTION_CENTER" && index == 2 -> "Farmers" to Icons.Default.People
                        currentMode == "COLLECTION_CENTER" && index == 3 -> "Dispatches" to Icons.Default.LocalShipping
                        currentMode == "TRADER" && index == 1 -> "Procurement" to Icons.Default.AddShoppingCart
                        currentMode == "TRADER" && index == 2 -> "Routes" to Icons.Default.LocalShipping
                        currentMode == "TRADER" && index == 3 -> "Ledger" to Icons.Default.ReceiptLong
                        currentMode == "INTEGRATED" && index == 1 -> "Delivery" to Icons.Default.LocalShipping
                        currentMode == "INTEGRATED" && index == 2 -> "Procurement" to Icons.Default.WaterDrop
                        currentMode == "INTEGRATED" && index == 3 -> "Ledger" to Icons.Default.ReceiptLong
                        else -> tab.title to tab.icon
                    }
                    val localizedTitle = AppStrings.get(rawTitle, session.languageCode).ifBlank { rawTitle.loc() }

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            subScreen = null
                            activeBottomTab = index
                        },
                        icon = {
                            Icon(
                                tabIcon,
                                contentDescription = localizedTitle,
                                modifier = Modifier.size(20.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        label = {
                            Text(
                                text = localizedTitle,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                subScreen == "COLLECTION_DESK" -> {
                    FarmerCollectionScreen(
                        viewModel = viewModel,
                        initialTab = 0,
                        showInternalTabs = false,
                        onOpenSettings = { subScreen = "SETTINGS" },
                        onBack = { subScreen = null }
                    )
                }
                subScreen == "CATTLE_BREEDING" -> {
                    CattleBreedingScreen(viewModel = viewModel, onBack = { subScreen = null })
                }
                subScreen == "MANAGE" -> {
                    ManageHubScreen(
                        viewModel = viewModel,
                        onBack = { subScreen = null },
                        onOpenSettings = { subScreen = "SETTINGS" }
                    )
                }
                subScreen == "PROFIT" -> {
                    ProfitScreen(viewModel = viewModel, onBack = { subScreen = null })
                }
                subScreen == "INVENTORY" -> {
                    InventoryScreen(viewModel = viewModel, onBack = { subScreen = null })
                }
                subScreen == "REPORTS" -> {
                    ReportsScreen(viewModel = viewModel, onBack = { subScreen = null })
                }
                subScreen == "MILK_WASTAGE" -> {
                    MilkWastageReconciliationScreen(viewModel = viewModel, onBack = { subScreen = null })
                }
                subScreen == "EXPENSES" -> {
                    ExpensesScreen(viewModel = viewModel)
                }
                subScreen == "ORDERS" -> {
                    OrdersScreen(viewModel = viewModel)
                }
                subScreen == "SETTINGS" -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateToSubscription = { subScreen = "SUBSCRIPTION" },
                        onNavigateToStaff = { subScreen = "STAFF" },
                        onBack = { subScreen = null }
                    )
                }
                subScreen == "SUBSCRIPTION" -> {
                    SubscriptionScreen(viewModel = viewModel, onBack = { subScreen = null })
                }
                subScreen == "STAFF" -> {
                    StaffScreen(viewModel = viewModel, onBack = { subScreen = null })
                }
                subScreen == "AI_ASSISTANT" -> {
                    GeminiChatScreen(
                        viewModel = viewModel,
                        onBack = { subScreen = null },
                        initialPrompt = aiInitialPrompt
                    )
                }
                else -> {
                    when (activeBottomTab) {
                        0 -> DashboardScreen(
                            viewModel = viewModel,
                            onNavigateToTab = { activeBottomTab = it },
                            onOpenProfit = { subScreen = "PROFIT" },
                            onOpenInventory = { subScreen = "INVENTORY" },
                            onOpenReports = { subScreen = "REPORTS" },
                            onOpenManage = { activeBottomTab = 4 },
                            onOpenExpenses = { subScreen = "EXPENSES" },
                            onOpenPayments = { activeBottomTab = 3 },
                            onOpenOrders = { subScreen = "ORDERS" },
                            onOpenCollectionDesk = { subScreen = "COLLECTION_DESK" },
                            onOpenCattleBreeding = { subScreen = "CATTLE_BREEDING" },
                            onOpenBusinessModeSelector = { showBusinessModeSelector = true },
                            onOpenAIAssistant = { prompt ->
                                aiInitialPrompt = prompt
                                subScreen = "AI_ASSISTANT"
                            },
                            onOpenSettings = { subScreen = "SETTINGS" },
                            onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                        )
                        1 -> {
                            when (currentMode) {
                                "FARMER_WHOLESALE" -> CattleBreedingScreen(viewModel = viewModel, onBack = { activeBottomTab = 0 })
                                "PROCESSING_UNIT" -> InventoryScreen(viewModel = viewModel, onBack = { activeBottomTab = 0 })
                                "COLLECTION_CENTER" -> FarmerCollectionScreen(
                                    viewModel = viewModel,
                                    initialTab = 0,
                                    showInternalTabs = false,
                                    onOpenSettings = { subScreen = "SETTINGS" }
                                )
                                "TRADER" -> FarmerCollectionScreen(
                                    viewModel = viewModel,
                                    initialTab = 0,
                                    showInternalTabs = true,
                                    onOpenSettings = { subScreen = "SETTINGS" }
                                )
                                else -> DeliveryScreen(viewModel = viewModel)
                            }
                        }
                        2 -> {
                            when (currentMode) {
                                "FARMER_WHOLESALE" -> DeliveryScreen(viewModel = viewModel)
                                "TRADER" -> DeliveryScreen(viewModel = viewModel)
                                "INTEGRATED" -> FarmerCollectionScreen(
                                    viewModel = viewModel,
                                    initialTab = 0,
                                    showInternalTabs = true,
                                    onOpenSettings = { subScreen = "SETTINGS" }
                                )
                                "RETAIL_PARLOUR" -> InventoryScreen(viewModel = viewModel, onBack = { activeBottomTab = 0 })
                                "COLLECTION_CENTER" -> FarmerCollectionScreen(
                                    viewModel = viewModel,
                                    initialTab = 1,
                                    showInternalTabs = false,
                                    onOpenSettings = { subScreen = "SETTINGS" }
                                )
                                else -> CustomersScreen(viewModel = viewModel)
                            }
                        }
                        3 -> {
                            if (currentMode == "COLLECTION_CENTER") {
                                FarmerCollectionScreen(
                                    viewModel = viewModel,
                                    initialTab = 2,
                                    showInternalTabs = false,
                                    onOpenSettings = { subScreen = "SETTINGS" }
                                )
                            } else {
                                PaymentsScreen(viewModel = viewModel)
                            }
                        }
                        4 -> ManageHubScreen(
                            viewModel = viewModel,
                            onBack = { activeBottomTab = 0 },
                            onOpenSettings = { subScreen = "SETTINGS" }
                        )
                        else -> DashboardScreen(
                            viewModel = viewModel,
                            onNavigateToTab = { activeBottomTab = it },
                            onOpenProfit = { subScreen = "PROFIT" },
                            onOpenInventory = { subScreen = "INVENTORY" },
                            onOpenReports = { subScreen = "REPORTS" },
                            onOpenManage = { activeBottomTab = 4 },
                            onOpenExpenses = { subScreen = "EXPENSES" },
                            onOpenPayments = { activeBottomTab = 3 },
                            onOpenOrders = { subScreen = "ORDERS" },
                            onOpenCollectionDesk = { subScreen = "COLLECTION_DESK" },
                            onOpenCattleBreeding = { subScreen = "CATTLE_BREEDING" },
                            onOpenBusinessModeSelector = { showBusinessModeSelector = true },
                            onOpenAIAssistant = { prompt ->
                                aiInitialPrompt = prompt
                                subScreen = "AI_ASSISTANT"
                            },
                            onOpenSettings = { subScreen = "SETTINGS" },
                            onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                        )
                    }
                }
            }
        }
    }
    }

    // Business Profile Switcher Dialog
    if (showBusinessModeSelector) {
        BusinessModeSelectorDialog(
            currentMode = currentMode,
            onDismiss = { showBusinessModeSelector = false },
            onSelectMode = { newMode ->
                viewModel.switchBusinessMode(newMode)
                showBusinessModeSelector = false
            }
        )
    }

    // Logout Confirm Dialog
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Logout Confirmation", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to sign out from this MilkMate account?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.logout()
                        showLogoutConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) { Text("Cancel") }
            }
        )
    }

    // Language Selection Modal
    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLang = session.languageCode,
            onDismiss = { showLanguageDialog = false },
            onSelect = {
                viewModel.repository.sessionManager.setLanguage(it)
                viewModel.showMessage("Language switched to ${it.uppercase()} ✓")
                showLanguageDialog = false
            }
        )
    }

    // Notification & Reminders Dialog
    if (showNotificationDialog) {
        NotificationCenterDialog(
            viewModel = viewModel,
            onDismiss = { showNotificationDialog = false },
            onNavigateToDelivery = {
                showNotificationDialog = false
                subScreen = null
                activeBottomTab = 1
            },
            onNavigateToPayments = {
                showNotificationDialog = false
                subScreen = null
                activeBottomTab = 3
            },
            onNavigateToExpenses = {
                showNotificationDialog = false
                subScreen = "EXPENSES"
            },
            onNavigateToFarm = {
                showNotificationDialog = false
                subScreen = "CATTLE_BREEDING"
            },
            onNavigateToInventory = {
                showNotificationDialog = false
                subScreen = "INVENTORY"
            }
        )
    }
}
