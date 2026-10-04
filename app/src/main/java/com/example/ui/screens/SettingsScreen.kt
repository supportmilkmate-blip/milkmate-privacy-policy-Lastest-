package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.ui.MilkMateViewModel
import com.example.ui.theme.*
import com.example.ui.util.BusinessModeFeatures
import com.example.ui.util.loc
import com.example.ui.util.appStr
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettingsScreen(
    viewModel: MilkMateViewModel,
    onNavigateToSubscription: () -> Unit,
    onNavigateToStaff: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val business by viewModel.currentBusiness.collectAsStateWithLifecycle()
    val session by viewModel.sessionState.collectAsStateWithLifecycle()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsStateWithLifecycle()
    val deletedCustomers by viewModel.deletedCustomers.collectAsStateWithLifecycle()
    val deletedExpenses by viewModel.deletedExpenses.collectAsStateWithLifecycle()

    var showBusinessProfileDialog by remember { mutableStateOf(false) }
    var showEditStartDateDialog by remember { mutableStateOf(false) }
    var showUpiDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showAppearanceDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showExpenseCategoriesDialog by remember { mutableStateOf(false) }
    var showTrashDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showSecurityDialog by remember { mutableStateOf(false) }
    var showCattleBreedingScreen by remember { mutableStateOf(false) }
    var showSetupWizard by remember { mutableStateOf(false) }
    var showBusinessModeSelector by remember { mutableStateOf(false) }
    var showCustomFormulaStudio by remember { mutableStateOf(false) }

    if (showCustomFormulaStudio) {
        CustomFormulaStudioDialog(
            business = business,
            viewModel = viewModel,
            onDismiss = { showCustomFormulaStudio = false }
        )
    }

    val currentMode = business?.businessMode ?: "FARMER"
    val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
    val startDateStr = dateFormat.format(Date(business?.accountStartDate ?: System.currentTimeMillis()))

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

    if (showSetupWizard) {
        FirstLaunchSetupWizard(
            viewModel = viewModel,
            onComplete = { showSetupWizard = false },
            onCancel = { showSetupWizard = false }
        )
        return
    }

    if (showCattleBreedingScreen) {
        CattleBreedingScreen(
            viewModel = viewModel,
            onBack = { showCattleBreedingScreen = false }
        )
        return
    }

    // JSON file picker for Backup Restore
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val jsonContent = reader.readText()
                reader.close()

                viewModel.restoreBackup(jsonContent) { success, msg ->
                    viewModel.showMessage(msg)
                }
            } catch (e: Exception) {
                viewModel.showMessage("Failed to read backup: ${e.message}")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Back Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text("DAIRY CONTROL CENTER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                    Text("Settings & Operations", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }

            // Quick Status Chip
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (pendingSyncCount == 0) DairyGreenLight else FreshGoldLight
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(6.dp).background(if (pendingSyncCount == 0) DairyGreen else WarmHoney, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (pendingSyncCount == 0) "Synced ✓" else "$pendingSyncCount Sync",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (pendingSyncCount == 0) DairyGreen else WarmHoney
                    )
                }
            }
        }

        // ================= INTUITIVE 4-TAB CATEGORY SELECTOR =================
        var selectedTab by remember { mutableStateOf(0) }
        val settingsTabs = listOf(
            Triple(0, "🏢 Profile & Model", Icons.Default.Storefront),
            Triple(1, "🥛 Rates & Pricing", Icons.Default.Science),
            Triple(2, "⚙️ Hardware & App", Icons.Default.Tune),
            Triple(3, "🔒 Backup & Safety", Icons.Default.Security)
        )

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 0.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = RoyalBluePrimary,
            modifier = Modifier.fillMaxWidth()
        ) {
            settingsTabs.forEach { (idx, tabTitle, icon) ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = {
                        Text(
                            tabTitle,
                            fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.5.sp
                        )
                    },
                    icon = {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )
            }
        }

        when (selectedTab) {
            // ================= TAB 0: BUSINESS PROFILE & OPERATING MODEL =================
            0 -> {
                // 🌟 Primary Business Operating Model (8 Specialized Modes)
                val activeModeOption = BUSINESS_MODES.find { it.code == currentMode } ?: BUSINESS_MODES.first()
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("settings_profile_toggle_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = activeModeOption.themeColor.copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(activeModeOption.icon, contentDescription = null, tint = activeModeOption.themeColor, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Business Operating Model", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                    Text(activeModeOption.title, fontSize = 11.sp, color = activeModeOption.themeColor, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = activeModeOption.themeColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = activeModeOption.badge,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = activeModeOption.themeColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = activeModeOption.description,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )

                        // Feature Highlights
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "✨ Active Model Workflows:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = activeModeOption.themeColor
                                )

                                activeModeOption.primaryFeatures.forEach { feat ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = activeModeOption.themeColor, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(feat, fontSize = 11.sp, color = TextPrimary)
                                    }
                                }
                            }
                        }

                        // Actions: Change Model / Manage Cattle / Run Wizard
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showBusinessModeSelector = true },
                                colors = ButtonDefaults.buttonColors(containerColor = activeModeOption.themeColor),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(44.dp).testTag("change_operating_model_btn")
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Switch Model", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            if (BusinessModeFeatures.showHerdBreeding(currentMode)) {
                                FilledTonalButton(
                                    onClick = { showCattleBreedingScreen = true },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).height(44.dp).testTag("open_cattle_breeding_btn")
                                ) {
                                    Icon(Icons.Default.Pets, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cattle Herd", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        FilledTonalButton(
                            onClick = { showSetupWizard = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = RoyalBlueLight, contentColor = RoyalBluePrimary),
                            modifier = Modifier.fillMaxWidth().height(42.dp).testTag("rerun_wizard_btn")
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("🛠️ Customize Modules & Sourcing/Sales Setup", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Dairy Shop Information Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Storefront, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Dairy Shop Profile", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            IconButton(onClick = { showBusinessProfileDialog = true }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = RoyalBluePrimary, modifier = Modifier.size(16.dp))
                            }
                        }

                        Text(
                            text = business?.businessName?.ifBlank { "My Dairy Farm" } ?: "My Dairy Farm",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "👤 Owner: ${business?.ownerName ?: "Owner"} • 📱 Contact: ${business?.phone ?: "Not set"}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = { showBusinessProfileDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(38.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Dairy Profile Details", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Master Account Start Date Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = DangerRed, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Account Start Date", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Surface(shape = RoundedCornerShape(4.dp), color = DairyGreenLight) {
                                Text("ACTIVE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = DairyGreen, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }

                        Text(text = startDateStr, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            text = "Synchronizes your starting balances. Records cannot be created before this date.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = { showEditStartDateDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(38.dp)
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Change Account Start Date", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Digital UPI Payments & WhatsApp Billing
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.QrCode, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("UPI QR Payments & WhatsApp Note", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (session.upiId.isNotBlank()) DairyGreenLight else Color(0xFFFFF3E0)
                            ) {
                                Text(
                                    text = if (session.upiId.isNotBlank()) "ACTIVE" else "NOT SET",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (session.upiId.isNotBlank()) DairyGreen else FreshGold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = if (session.upiId.isNotBlank()) "UPI ID: ${session.upiId}" else "No UPI ID set. Add your GPay, PhonePe, or Paytm UPI ID for auto QR payments.",
                            fontSize = 12.sp,
                            fontWeight = if (session.upiId.isNotBlank()) FontWeight.Bold else FontWeight.Normal,
                            color = if (session.upiId.isNotBlank()) RoyalBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "Greeting note: \"${session.billNote}\"",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = { showUpiDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(38.dp)
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Configure UPI ID & WhatsApp Bill Note", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Subscription Plan Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("SUBSCRIPTION STATUS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Surface(shape = RoundedCornerShape(4.dp), color = DairyGreenLight) {
                                Text(
                                    text = business?.subscriptionPlan ?: "FREE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DairyGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text("MilkMate Pro Experience", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            text = "Unlimited customers, bulk buyers, multi-route delivery personnel management, advanced P&L reports, and automatic Cloud Sync.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = onNavigateToSubscription,
                            colors = ButtonDefaults.buttonColors(containerColor = FreshGold),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(38.dp)
                        ) {
                            Text("⭐ View Plans & Upgrade to Pro", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            }

            // ================= TAB 1: MILK RATES & PRICING ENGINE =================
            1 -> {
                // Dairy Rates & Milk Specialization
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CurrencyRupee, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Standard Milk Rates & Pricing", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Surface(shape = RoundedCornerShape(4.dp), color = RoyalBlueLight) {
                                Text(
                                    text = business?.pricingMode ?: "DIRECT",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RoyalBluePrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Milk Type Specialization Filter Pills
                        val supportedTypes = business?.supportedMilkTypes ?: "BOTH"
                        Text("Dairy Milk Specialization:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "COW_ONLY" to "🐄 Cow Only",
                                "BUFFALO_ONLY" to "🐃 Buffalo Only",
                                "BOTH" to "🥛 Both (Cow & Buff)"
                            ).forEach { (code, label) ->
                                val isSel = supportedTypes == code
                                FilterChip(
                                    selected = isSel,
                                    onClick = { viewModel.updateSupportedMilkTypes(code) },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }

                        // Rate Indicators
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (supportedTypes == "COW_ONLY" || supportedTypes == "BOTH") {
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Cow Milk Base Rate", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("₹${business?.cowMilkRate ?: 50.0}/L", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = RoyalBluePrimary)
                                    }
                                }
                            }

                            if (supportedTypes == "BUFFALO_ONLY" || supportedTypes == "BOTH") {
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Buffalo Milk Base Rate", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("₹${business?.buffaloMilkRate ?: 65.0}/L", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DairyGreen)
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { showBusinessProfileDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Base Rates & Pricing Model", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Custom Milk Pricing Formula & Equation Studio Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Functions, contentDescription = null, tint = FreshGold, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Formula Studio & Dynamic Rates", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (business?.pricingMode == "CUSTOM") FreshGold.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = if (business?.pricingMode == "CUSTOM") "CUSTOM ACTIVE" else business?.pricingMode ?: "STANDARD",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (business?.pricingMode == "CUSTOM") DeepOceanNavy else TextSecondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        val currentBiz = business
                        Text(
                            text = "Active Formula: ${currentBiz?.customFormulaName ?: "FAT/SNF Matrix Equation"}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = OceanMidnight,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Rate = ₹${currentBiz?.customFormulaBase ?: 22.0} + (FAT × ${currentBiz?.customFormulaFatMultiplier ?: 5.5}) + (SNF × ${currentBiz?.customFormulaSnfMultiplier ?: 3.5})" +
                                        (if ((currentBiz?.customFormulaTsMultiplier ?: 0.0) > 0) " + (TS × ${currentBiz?.customFormulaTsMultiplier})" else "") +
                                        (if ((currentBiz?.customFormulaClrMultiplier ?: 0.0) > 0) " + (CLR × ${currentBiz?.customFormulaClrMultiplier})" else "") +
                                        (if (currentBiz?.customFormulaYieldType != null && currentBiz.customFormulaYieldType != "NONE" && currentBiz.customFormulaYieldMultiplier > 0) " + (${currentBiz.customFormulaYieldType} × ${currentBiz.customFormulaYieldMultiplier})" else "") +
                                        (if ((currentBiz?.customFormulaQualityBonus ?: 0.0) > 0) " + Bonus(₹${currentBiz?.customFormulaQualityBonus})" else ""),
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.5.sp,
                                lineHeight = 14.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        FilledTonalButton(
                            onClick = { showCustomFormulaStudio = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = DeepOceanNavy, contentColor = Color.White),
                            modifier = Modifier.fillMaxWidth().height(42.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(15.dp), tint = FreshGold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Custom Formula Studio (All Bases) →", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ================= TAB 2: HARDWARE & APP CONTROLS =================
            2 -> {
                // BMC Kendra & Chiller Setup
                if (currentMode == "COLLECTION_CENTER") {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("bmc_settings_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(shape = CircleShape, color = RoyalBlueLight, modifier = Modifier.size(36.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Tune, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    Column {
                                        Text("BMC Collection Centre & Kendra", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                        Text("Chilling unit, society code, hardware", fontSize = 11.sp, color = TextSecondary)
                                    }
                                }
                                Surface(shape = RoundedCornerShape(8.dp), color = RoyalBlueLight) {
                                    Text("🏢 BMC", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }

                            HorizontalDivider(color = BorderSubtle)

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Chiller Vat Specifications", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                        Text("Vat Capacity: 5,000 Liters • Target Temp: 4.0°C • Auto Agitator: Active", fontSize = 11.sp, color = GrassGreen)
                                    }
                                    Icon(Icons.Default.AcUnit, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }

                // Hardware Integration Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Devices, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Dairy Hardware Integrations", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.Bluetooth, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(18.dp))
                                        Column {
                                            Text("Bluetooth Milk Analyzer & Scale", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                            Text("Auto-capture FAT, SNF, and weight into intake desk", fontSize = 10.5.sp, color = TextSecondary)
                                        }
                                    }
                                    Surface(shape = RoundedCornerShape(6.dp), color = DairyGreenLight) {
                                        Text("PAIRED", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = DairyGreen, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }

                                HorizontalDivider()

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.Print, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(18.dp))
                                        Column {
                                            Text("Thermal Slip Printer (2\" / 3\")", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                            Text("Instant farmer intake slip with FAT, Rate & Amount", fontSize = 10.5.sp, color = TextSecondary)
                                        }
                                    }
                                    Surface(shape = RoundedCornerShape(6.dp), color = FreshGoldLight) {
                                        Text("AUTO-PRINT ON", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = WarmHoney, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // App Controls Grid
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column {
                        SettingsGridRow(
                            leftTitle = "Language",
                            leftSub = "Language (${session.languageCode.uppercase()})",
                            leftIcon = Icons.Default.Translate,
                            onLeftClick = { showLanguageDialog = true },
                            rightTitle = "Appearance",
                            rightSub = "${session.themeMode.lowercase().replaceFirstChar { it.uppercase() }} • ${session.accentTheme}",
                            rightIcon = Icons.Default.Palette,
                            onRightClick = { showAppearanceDialog = true }
                        )
                        HorizontalDivider()
                        SettingsGridRow(
                            leftTitle = "Notifications",
                            leftSub = if (session.notificationsEnabled) "Active alerts" else "Muted",
                            leftIcon = Icons.Default.Notifications,
                            onLeftClick = { showNotificationsDialog = true },
                            rightTitle = "Expense Categories",
                            rightSub = "${session.customExpenseCategories.size} categories configured",
                            rightIcon = Icons.Default.Category,
                            onRightClick = { showExpenseCategoriesDialog = true }
                        )
                        HorizontalDivider()
                        SettingsGridRow(
                            leftTitle = "Help & Support",
                            leftSub = "FAQs & WhatsApp chat",
                            leftIcon = Icons.Default.Help,
                            onLeftClick = { showHelpDialog = true },
                            rightTitle = "Staff & Access",
                            rightSub = "Roles, PINs & routes",
                            rightIcon = Icons.Default.People,
                            onRightClick = { onNavigateToStaff() }
                        )
                    }
                }
            }

            // ================= TAB 3: BACKUP, SECURITY & ACCOUNT =================
            3 -> {
                // Cloud Sync & Data Safety Hub
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cloud Sync & JSON Backup", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (pendingSyncCount == 0) DairyGreenLight else Color(0xFFFFF3E0)
                            ) {
                                Text(
                                    text = if (pendingSyncCount == 0) "ALL SYNCED ✓" else "$pendingSyncCount QUEUED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (pendingSyncCount == 0) DairyGreen else FreshGold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "Local offline-first Room database is synchronized with cloud storage. Export backups to safeguard against phone loss.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.triggerManualSync() },
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Text("🔄 Sync Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    viewModel.exportBackup { file ->
                                        val intent = viewModel.repository.exportManager.shareFileIntent(file, "application/json")
                                        context.startActivity(Intent.createChooser(intent, "Save MilkMate Backup JSON"))
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1.2f).height(38.dp)
                            ) {
                                Text("📥 Export Backup", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = { filePickerLauncher.launch("application/json") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(38.dp)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("📤 Restore from Backup JSON File", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // App Lock & PIN Security
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "APP SECURITY & PIN PROTECTION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("App Lock PIN", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                Text(
                                    text = if (session.appLockEnabled) "Active: 4-digit PIN required on opening" else "Disabled: Instant direct access",
                                    fontSize = 11.sp,
                                    color = if (session.appLockEnabled) DairyGreen else TextSecondary
                                )
                            }
                            Switch(
                                checked = session.appLockEnabled,
                                onCheckedChange = { isChecked ->
                                    if (isChecked && session.securityPin.isBlank()) {
                                        showSecurityDialog = true
                                    } else {
                                        viewModel.setSecuritySettings(appLockEnabled = isChecked)
                                    }
                                }
                            )
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showSecurityDialog = true }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Pin, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Change 4-Digit Security PIN", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(
                                        if (session.securityPin.isNotBlank()) "PIN Configured (••••)" else "Not Set - Tap to configure",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Text("Configure →", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                        }
                    }
                }

                // Delete Safety Confirmation Toggle
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Confirm Before Deletion", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Displays a safety confirmation dialog before removing deliveries, purchases, or expenses.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = session.confirmDelete,
                            onCheckedChange = { viewModel.setConfirmDelete(it) }
                        )
                    }
                }

                // Recycle Bin & Trash
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTrashDialog = true }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = WarmHoney, modifier = Modifier.size(20.dp))
                            Column {
                                Text("Recently Deleted / Recycle Bin", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Restore ${deletedCustomers.size + deletedExpenses.size} deleted records", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                        Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Text("Open →", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }

                // Logout & Session Reset
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Business Session ID: ${session.businessId}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Role: ${session.role} • Registered: ${session.registeredPhone.ifBlank { "Mobile Verified" }}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Button(
                            onClick = { showLogoutConfirm = true },
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(38.dp)
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Logout / Switch Account", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------
    // MODAL DIALOGS
    // -------------------------------------------------------------

    // Modal: Edit Business Profile & Base Milk Rates
    if (showBusinessProfileDialog) {
        var nameText by remember { mutableStateOf(business?.businessName ?: "") }
        var ownerText by remember { mutableStateOf(business?.ownerName ?: "") }
        var phoneText by remember { mutableStateOf(business?.phone ?: "") }
        var cowRateText by remember { mutableStateOf((business?.cowMilkRate ?: 50.0).toString()) }
        var buffaloRateText by remember { mutableStateOf((business?.buffaloMilkRate ?: 65.0).toString()) }
        var modeText by remember { mutableStateOf(business?.pricingMode ?: "DIRECT") }

        AlertDialog(
            onDismissRequest = { showBusinessProfileDialog = false },
            title = { Text("Edit Business & Rates", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = nameText,
                        onValueChange = { nameText = it },
                        label = { Text("Dairy Farm / Business Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = ownerText,
                        onValueChange = { ownerText = it },
                        label = { Text("Owner Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = phoneText,
                        onValueChange = { phoneText = it },
                        label = { Text("Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )

                    val bizSupportedTypes = business?.supportedMilkTypes ?: "BOTH"
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (bizSupportedTypes != "BUFFALO_ONLY") {
                            OutlinedTextField(
                                value = cowRateText,
                                onValueChange = { cowRateText = it },
                                label = { Text("Cow Rate (₹/L)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (bizSupportedTypes != "COW_ONLY") {
                            OutlinedTextField(
                                value = buffaloRateText,
                                onValueChange = { buffaloRateText = it },
                                label = { Text("Buffalo Rate (₹/L)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Text("Rate Calculation & Yield Mode:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "DIRECT" to "💵 Direct",
                            "FAT_SNF" to "🧪 FAT+SNF",
                            "PANEER_YIELD" to "🧀 Paneer Yield",
                            "KHOA_YIELD" to "🍬 Khoya Yield",
                            "GHEE_YIELD" to "🏺 Ghee Yield",
                            "FAT_ONLY" to "🧈 FAT Only"
                        ).forEach { (m, lbl) ->
                            FilterChip(
                                selected = modeText == m,
                                onClick = { modeText = m },
                                label = { Text(lbl, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cow = cowRateText.toDoubleOrNull() ?: 50.0
                        val buf = buffaloRateText.toDoubleOrNull() ?: 65.0
                        viewModel.updateBusinessProfile(nameText, ownerText, phoneText, cow, buf, modeText)
                        showBusinessProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary)
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBusinessProfileDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Modal: Edit Master Account Start Date
    if (showEditStartDateDialog) {
        val currentStart = business?.accountStartDate ?: System.currentTimeMillis()
        val c = Calendar.getInstance().apply { timeInMillis = currentStart }
        var selYear by remember { mutableIntStateOf(c.get(Calendar.YEAR)) }
        var selMonth by remember { mutableIntStateOf(c.get(Calendar.MONTH)) }
        var selDay by remember { mutableIntStateOf(c.get(Calendar.DAY_OF_MONTH)) }

        val previewCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selYear)
            set(Calendar.MONTH, selMonth)
            set(Calendar.DAY_OF_MONTH, selDay.coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH)))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val previewDateStr = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(previewCal.time)

        AlertDialog(
            onDismissRequest = { showEditStartDateDialog = false },
            title = { Text("Master Account Start Date", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Choose the exact date when your dairy started tracking on MilkMate. All inventory, milk collections, and month selectors strictly enforce this starting boundary.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = DairyGreenLight)
                    ) {
                        Text(
                            text = "New Start Date: $previewDateStr",
                            fontWeight = FontWeight.Bold,
                            color = DairyGreen,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    // Year Selection
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        (2024..2030).forEach { yr ->
                            FilterChip(
                                selected = selYear == yr,
                                onClick = { selYear = yr },
                                label = { Text(yr.toString(), fontSize = 11.sp) }
                            )
                        }
                    }

                    // Month Selection
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                        months.forEachIndexed { idx, m ->
                            FilterChip(
                                selected = selMonth == idx,
                                onClick = { selMonth = idx },
                                label = { Text(m, fontSize = 11.sp) }
                            )
                        }
                    }

                    // Day Selection
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        (1..31).forEach { d ->
                            FilterChip(
                                selected = selDay == d,
                                onClick = { selDay = d },
                                label = { Text(d.toString(), fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateAccountStartDate(previewCal.timeInMillis)
                        showEditStartDateDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DairyGreen)
                ) {
                    Text("✓ Confirm Start Date")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditStartDateDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Modal: UPI ID & Bill Note Configuration
    if (showUpiDialog) {
        var upiText by remember { mutableStateOf(session.upiId) }
        var noteText by remember { mutableStateOf(session.billNote) }

        AlertDialog(
            onDismissRequest = { showUpiDialog = false },
            title = { Text("UPI & WhatsApp Billing", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter your dairy UPI ID (Google Pay, PhonePe, Paytm, or BHIM). Customers will see this directly on their WhatsApp bills to pay seamlessly.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = upiText,
                        onValueChange = { upiText = it },
                        label = { Text("Dairy UPI ID (e.g. 9876543210@paytm)") },
                        placeholder = { Text("yourdairy@okaxis") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Bill Greeting / Thank You Note") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setUpiId(upiText)
                        viewModel.setBillNote(noteText)
                        showUpiDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary)
                ) {
                    Text("Save UPI Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpiDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Modal: Appearance & Color Themes
    if (showAppearanceDialog) {
        val themeModes = listOf("SYSTEM" to "System Default", "LIGHT" to "Always Light", "DARK" to "Always Dark")
        val accents = listOf(
            "BLUE" to ("Royal Blue" to Color(0xFF1E3A8A)),
            "GREEN" to ("Dairy Green" to Color(0xFF059669)),
            "GOLD" to ("Golden Butter" to Color(0xFFD97706)),
            "PURPLE" to ("Deep Violet" to Color(0xFF7C3AED)),
            "TEAL" to ("Ocean Teal" to Color(0xFF0F766E)),
            "RED" to ("Crimson Red" to Color(0xFFDC2626))
        )

        AlertDialog(
            onDismissRequest = { showAppearanceDialog = false },
            title = { Text("Appearance & Theme", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Display Mode:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        themeModes.forEach { (mode, label) ->
                            FilterChip(
                                selected = session.themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                label = { Text(label, fontSize = 10.sp, fontWeight = if (session.themeMode == mode) FontWeight.Bold else FontWeight.Normal) }
                            )
                        }
                    }

                    HorizontalDivider()

                    Text("Accent Color Palette:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        accents.forEach { (key, pair) ->
                            val (label, col) = pair
                            val isSelected = session.accentTheme.equals(key, true)
                            Card(
                                modifier = Modifier
                                    .width(85.dp)
                                    .clickable { viewModel.setAccentTheme(key) },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) col.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                ),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, col) else null
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(modifier = Modifier.size(24.dp).background(col, shape = CircleShape))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(label, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAppearanceDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Done")
                }
            }
        )
    }

    // Modal: Notifications (Fully dynamic notification radar and shift scheduler)
    if (showNotificationsDialog) {
        NotificationCenterDialog(
            viewModel = viewModel,
            onDismiss = { showNotificationsDialog = false },
            onNavigateToDelivery = {
                showNotificationsDialog = false
                onBack()
            },
            onNavigateToPayments = {
                showNotificationsDialog = false
                onBack()
            },
            onNavigateToExpenses = {
                showNotificationsDialog = false
                onBack()
            },
            onNavigateToFarm = {
                showNotificationsDialog = false
                showCattleBreedingScreen = true
            },
            onNavigateToInventory = {
                showNotificationsDialog = false
                onBack()
            }
        )
    }

    // Modal: Expense Categories Manager (Add, Edit, Delete custom categories)
    if (showExpenseCategoriesDialog) {
        ExpenseCategoriesManagerModal(
            session = session,
            viewModel = viewModel,
            onDismiss = { showExpenseCategoriesDialog = false }
        )
    }

    // Modal: Recently Deleted (Trash Recovery)
    if (showTrashDialog) {
        AlertDialog(
            onDismissRequest = { showTrashDialog = false },
            title = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("🗑️ Trash / Recently Deleted", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    IconButton(onClick = { showTrashDialog = false }) { Icon(Icons.Default.Close, contentDescription = null) }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Recover records deleted by mistake. Tap 'Restore' to return them to active lists.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    if (deletedCustomers.isEmpty() && deletedExpenses.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Trash is empty. No deleted records found. ✓", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(deletedCustomers, key = { it.id }) { cust ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Customer: ${cust.name}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("${cust.type} • Due: ₹${cust.outstandingBalance.toInt()}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Button(
                                            onClick = { viewModel.restoreCustomer(cust.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Text("Restore", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }

                            items(deletedExpenses, key = { it.id }) { exp ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Expense: ${exp.category}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("₹${exp.amount.toInt()} • ${exp.notes}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Button(
                                            onClick = { viewModel.restoreExpense(exp.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Text("Restore", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTrashDialog = false }) { Text("Close") }
            }
        )
    }

    // Modal: Language Dialog
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

    // Modal: Help & Support
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("Dairy Help & FAQs", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Frequently Asked Dairy Questions:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("• Deliveries: Select Morning/Evening shift and tap Enter or +/- 0.5L.")
                    Text("• WhatsApp Bills: Open Customer statement and tap Send Bill.")
                    Text("• Feeds: Stock consumption reduces inventory but is not counted twice in profit.")
                    Text("• Start Date: Governs the starting month for all ledgers and inventory audits.")
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            val uri = Uri.parse("https://api.whatsapp.com/send?phone=919876543210&text=Hello%20MilkMate%20Dairy%20Support")
                            try { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) } catch (_: Exception) {}
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("💬 Direct WhatsApp Support")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) { Text("Close") }
            }
        )
    }

    // Modal: Security & PIN Settings
    if (showSecurityDialog) {
        SecuritySettingsDialog(
            currentPin = session.securityPin,
            currentPassword = session.securityPassword,
            biometricEnabled = session.biometricEnabled,
            appLockEnabled = session.appLockEnabled,
            onDismiss = { showSecurityDialog = false },
            onSave = { newPin, newPass, bioEnabled, lockEnabled ->
                viewModel.setSecuritySettings(
                    appLockEnabled = lockEnabled,
                    pin = newPin,
                    password = newPass,
                    biometricEnabled = bioEnabled
                )
                showSecurityDialog = false
            }
        )
    }

    // Modal: Logout Confirm
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Logout Confirmation", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to sign out of this dairy business account on this device?") },
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
}

@Composable
fun SettingsGridRow(
    leftTitle: String,
    leftSub: String,
    leftIcon: ImageVector,
    onLeftClick: () -> Unit,
    rightTitle: String,
    rightSub: String,
    rightIcon: ImageVector,
    onRightClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.weight(1f).clickable { onLeftClick() }.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = leftIcon, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(leftTitle.loc(), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(leftSub, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
        Row(
            modifier = Modifier.weight(1f).clickable { onRightClick() }.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = rightIcon, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(rightTitle.loc(), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(rightSub, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

@Composable
fun LanguageSelectionDialog(
    currentLang: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val languages = listOf(
        "en" to "English",
        "hi" to "हिन्दी — Hindi",
        "or" to "ଓଡ଼ିଆ — Odia",
        "bn" to "বাংলা — Bengali",
        "mr" to "मराठी — Marathi",
        "gu" to "ગુજરાતી — Gujarati",
        "te" to "తెలుగు — Telugu",
        "ta" to "தமிழ் — Tamil",
        "kn" to "ಕನ್ನಡ — Kannada",
        "ml" to "മലയാളം — Malayalam",
        "pa" to "ਪੰਜਾਬੀ — Punjabi",
        "as" to "অসমীয়া — Assamese",
        "hr" to "हरियाणवी — Haryanvi"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("🌐 Language", fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = null) }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Choose your preferred regional dairy operations language.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                languages.forEach { (code, label) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(code) },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (currentLang == code) RoyalBlueLight else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = label, fontWeight = if (currentLang == code) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
                            if (currentLang == code) {
                                Text("Selected ✓", color = RoyalBluePrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
fun ExpenseCategoriesManagerModal(
    session: com.example.data.repository.UserSession,
    viewModel: MilkMateViewModel,
    onDismiss: () -> Unit
) {
    var newCategoryText by remember { mutableStateOf("") }
    var editingCategory by remember { mutableStateOf<String?>(null) }
    var editedCategoryText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Expense Categories", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("${session.customExpenseCategories.size} Active Categories", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = null) }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Add, rename, or delete categories used in your dairy farm expense records and P&L statements.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Add New Category Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Add New Category", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newCategoryText,
                                onValueChange = { newCategoryText = it },
                                placeholder = { Text("e.g. Silage, Doctor Fees", fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f).height(50.dp)
                            )
                            Button(
                                onClick = {
                                    if (newCategoryText.isNotBlank()) {
                                        viewModel.addExpenseCategory(newCategoryText)
                                        newCategoryText = ""
                                    }
                                },
                                enabled = newCategoryText.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(50.dp)
                            ) {
                                Text("+ Add", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Category List
                Text("Configured Categories:", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                session.customExpenseCategories.forEach { category ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        if (editingCategory == category) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = editedCategoryText,
                                    onValueChange = { editedCategoryText = it },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).height(46.dp)
                                )
                                IconButton(onClick = {
                                    if (editedCategoryText.isNotBlank()) {
                                        viewModel.editExpenseCategory(category, editedCategoryText)
                                        editingCategory = null
                                    }
                                }) {
                                    Icon(Icons.Default.Check, contentDescription = "Save", tint = DairyGreen)
                                }
                                IconButton(onClick = { editingCategory = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancel", tint = DangerRed)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(if (category.contains("Feed", ignoreCase = true)) FreshGold else RoyalBluePrimary, CircleShape)
                                    )
                                    Text(category, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            editingCategory = category
                                            editedCategoryText = category
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                    }

                                    if (session.customExpenseCategories.size > 1) {
                                        IconButton(
                                            onClick = { viewModel.deleteExpenseCategory(category) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DangerRed.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedButton(
                    onClick = { viewModel.resetExpenseCategories() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset Categories to Default", fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary)) {
                Text("Done")
            }
        }
    )
}

@Composable
fun SecuritySettingsDialog(
    currentPin: String,
    currentPassword: String,
    biometricEnabled: Boolean,
    appLockEnabled: Boolean,
    onDismiss: () -> Unit,
    onSave: (newPin: String, newPassword: String, bioEnabled: Boolean, lockEnabled: Boolean) -> Unit
) {
    var pin by remember { mutableStateOf(currentPin) }
    var confirmPin by remember { mutableStateOf(currentPin) }
    var password by remember { mutableStateOf(currentPassword) }
    var bioState by remember { mutableStateOf(biometricEnabled) }
    var lockState by remember { mutableStateOf(appLockEnabled) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("App Lock & Security PIN", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Secure MilkMate on this device with a 4-digit PIN and fingerprint/screen lock.",
                    fontSize = 11.5.sp,
                    color = TextSecondary
                )

                if (errorMsg != null) {
                    Text(errorMsg ?: "", color = DangerRed, fontSize = 11.sp)
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Enable App Lock Protection", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Switch(
                            checked = lockState,
                            onCheckedChange = { lockState = it }
                        )
                    }
                }

                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 4) {
                            pin = it.filter { c -> c.isDigit() }
                            errorMsg = null
                        }
                    },
                    label = { Text("4-Digit Security PIN *") },
                    placeholder = { Text("e.g. 1234") },
                    leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null, tint = RoyalBluePrimary) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = {
                        if (it.length <= 4) {
                            confirmPin = it.filter { c -> c.isDigit() }
                            errorMsg = null
                        }
                    },
                    label = { Text("Confirm 4-Digit PIN *") },
                    placeholder = { Text("Re-enter PIN") },
                    leadingIcon = { Icon(Icons.Default.LockClock, contentDescription = null, tint = RoyalBluePrimary) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Fingerprint / Screen Lock", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("Use device biometric sensor to unlock", fontSize = 10.sp, color = TextSecondary)
                        }
                        Switch(
                            checked = bioState,
                            onCheckedChange = { bioState = it }
                        )
                    }
                }

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMsg = null },
                    label = { Text("Master Password (Optional)") },
                    placeholder = { Text("For fallback account recovery") },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = RoyalBluePrimary) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (lockState && pin.length != 4) {
                        errorMsg = "PIN must be exactly 4 digits."
                    } else if (lockState && pin != confirmPin) {
                        errorMsg = "PIN codes do not match."
                    } else {
                        onSave(pin, password, bioState, lockState)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary)
            ) {
                Text("Save Security Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

