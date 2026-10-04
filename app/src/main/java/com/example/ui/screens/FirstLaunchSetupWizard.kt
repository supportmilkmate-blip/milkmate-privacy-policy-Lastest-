package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MilkMateViewModel
import com.example.ui.theme.*
import com.example.ui.util.BusinessModeFeatures
import com.example.ui.util.DairyBusinessType

/**
 * Multi-Step Dairy Business Setup Wizard
 * Step 1: Business Role (Farmer, Collection Center, Trader, Integrated, etc.)
 * Step 2: Milk Sourcing & Intake Activities (Own Cattle vs Buying from Suppliers)
 * Step 3: Sales & Distribution Activities (Households, Center/Trader, Bulk, Factory Tankers)
 * Step 4: Milk Types & Dairy Processing Activities (Paneer, Ghee, Curd, Khoya, Feed)
 * Step 5: Business Profile & Dynamic Feature Preview
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirstLaunchSetupWizard(
    viewModel: MilkMateViewModel,
    onComplete: () -> Unit,
    onCancel: (() -> Unit)? = null
) {
    var currentStep by remember { mutableIntStateOf(1) }
    val totalSteps = 5

    // Role Selection
    var selectedRoleCode by remember { mutableStateOf("FARMER") }

    // Step 2: Sourcing Activities
    var sourceOwnCattle by remember { mutableStateOf(true) }
    var sourceVillageFarmers by remember { mutableStateOf(false) }

    // Step 3: Sales Activities
    var destHouseholds by remember { mutableStateOf(true) }
    var destCollectionCenter by remember { mutableStateOf(false) }
    var destBulkCommercial by remember { mutableStateOf(false) }
    var destFactoryTankers by remember { mutableStateOf(false) }

    // Step 4: Milk & Processing Activities
    var selectedMilkType by remember { mutableStateOf("BOTH") } // COW_ONLY, BUFFALO_ONLY, BOTH, MIXED
    var hasPaneer by remember { mutableStateOf(true) }
    var hasGheeButter by remember { mutableStateOf(true) }
    var hasCurdChaas by remember { mutableStateOf(true) }
    var hasKhoyaSweets by remember { mutableStateOf(false) }
    var hasCattleFeed by remember { mutableStateOf(false) }

    // Step 5: Business Details
    var businessName by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var cityArea by remember { mutableStateOf("") }
    var selectedPricingMode by remember { mutableStateOf("DIRECT") } // DIRECT, FAT_SNF, FAT_ONLY
    var showCustomFormulaDialog by remember { mutableStateOf(false) }

    // When role is picked in Step 1, auto-configure matching activity presets
    fun applyRoleDefaults(roleCode: String) {
        selectedRoleCode = roleCode
        when (roleCode) {
            "FARMER_WHOLESALE" -> {
                sourceOwnCattle = true
                sourceVillageFarmers = false
                destHouseholds = false
                destCollectionCenter = true
                destBulkCommercial = false
                destFactoryTankers = false
                selectedPricingMode = "FAT_SNF"
            }
            "FARMER" -> {
                sourceOwnCattle = true
                sourceVillageFarmers = false
                destHouseholds = true
                destCollectionCenter = true
                destBulkCommercial = false
                destFactoryTankers = false
                selectedPricingMode = "DIRECT"
            }
            "INTEGRATED" -> {
                sourceOwnCattle = true
                sourceVillageFarmers = true
                destHouseholds = true
                destCollectionCenter = true
                destBulkCommercial = true
                destFactoryTankers = false
                hasPaneer = true
                hasGheeButter = true
                selectedPricingMode = "FAT_SNF"
            }
            "TRADER" -> {
                sourceOwnCattle = false
                sourceVillageFarmers = true
                destHouseholds = true
                destCollectionCenter = false
                destBulkCommercial = true
                destFactoryTankers = false
                selectedPricingMode = "FAT_SNF"
            }
            "COLLECTION_CENTER" -> {
                sourceOwnCattle = false
                sourceVillageFarmers = true
                destHouseholds = false
                destCollectionCenter = false
                destBulkCommercial = false
                destFactoryTankers = true
                hasCattleFeed = true
                selectedPricingMode = "FAT_SNF"
            }
            "PROCESSING_UNIT" -> {
                sourceOwnCattle = false
                sourceVillageFarmers = false
                destHouseholds = false
                destCollectionCenter = false
                destBulkCommercial = true
                destFactoryTankers = false
                hasPaneer = true
                hasGheeButter = true
                hasCurdChaas = true
                hasKhoyaSweets = true
                selectedPricingMode = "DIRECT"
            }
            "RETAIL_PARLOUR" -> {
                sourceOwnCattle = false
                sourceVillageFarmers = false
                destHouseholds = true
                destCollectionCenter = false
                destBulkCommercial = false
                destFactoryTankers = false
                hasPaneer = true
                hasCurdChaas = true
                selectedPricingMode = "DIRECT"
            }
            "GAUSHALA" -> {
                sourceOwnCattle = true
                sourceVillageFarmers = false
                destHouseholds = true
                destCollectionCenter = false
                destBulkCommercial = false
                destFactoryTankers = false
                selectedMilkType = "COW_ONLY"
                hasGheeButter = true
                selectedPricingMode = "DIRECT"
            }
        }
    }

    val computedModeCode = remember(
        selectedRoleCode,
        sourceOwnCattle,
        sourceVillageFarmers,
        destHouseholds,
        destCollectionCenter,
        destBulkCommercial,
        destFactoryTankers
    ) {
        BusinessModeFeatures.computeModeFromCapabilities(
            sourceOwnCattle = sourceOwnCattle,
            sourceVillageFarmers = sourceVillageFarmers,
            destHouseholds = destHouseholds,
            destCollectionCenter = destCollectionCenter,
            destBulkCommercial = destBulkCommercial,
            destFactoryTankers = destFactoryTankers
        )
    }

    val computedMode = remember(computedModeCode) {
        BusinessModeFeatures.getByType(computedModeCode)
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (onCancel != null) {
                                IconButton(onClick = onCancel, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Column {
                                Text(
                                    text = "MilkMate Tailored Setup",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = RoyalBluePrimary
                                )
                                Text(
                                    text = when (currentStep) {
                                        1 -> "Step 1: Dairy Business Role"
                                        2 -> "Step 2: Sourcing Activities"
                                        3 -> "Step 3: Sales Channels"
                                        4 -> "Step 4: Milk & Processing"
                                        else -> "Step 5: Profile & Feature Review"
                                    },
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = RoyalBlueLight
                        ) {
                            Text(
                                text = "$currentStep / $totalSteps",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = RoyalBluePrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 5-Step Progress Indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        (1..totalSteps).forEach { step ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (step <= currentStep) RoyalBluePrimary else MaterialTheme.colorScheme.surfaceVariant
                                    )
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Previous")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Button(
                        onClick = {
                            if (currentStep < totalSteps) {
                                // Default fallbacks to prevent empty state
                                if (currentStep == 2 && !sourceOwnCattle && !sourceVillageFarmers) {
                                    sourceOwnCattle = true
                                }
                                if (currentStep == 3 && !destHouseholds && !destCollectionCenter && !destBulkCommercial && !destFactoryTankers) {
                                    destHouseholds = true
                                }
                                currentStep++
                            } else {
                                val finalName = if (businessName.isBlank()) {
                                    when {
                                        sourceOwnCattle && destCollectionCenter && !destHouseholds -> "Kisan Dairy Farm"
                                        sourceVillageFarmers && destFactoryTankers -> "Village Milk Collection Centre"
                                        sourceVillageFarmers && !sourceOwnCattle -> "Sai Milk Traders & Distributors"
                                        sourceOwnCattle && sourceVillageFarmers -> "Integrated Megadairy Enterprise"
                                        else -> "Gokul Dairy Farm"
                                    }
                                } else businessName.trim()

                                viewModel.completeFirstLaunchWithCapabilities(
                                    businessName = finalName,
                                    ownerName = ownerName.trim().ifBlank { "Owner" },
                                    phone = phone.trim(),
                                    sourceOwnCattle = sourceOwnCattle,
                                    sourceVillageFarmers = sourceVillageFarmers,
                                    destHouseholds = destHouseholds,
                                    destCollectionCenter = destCollectionCenter,
                                    destBulkCommercial = destBulkCommercial,
                                    destFactoryTankers = destFactoryTankers,
                                    hasPaneer = hasPaneer,
                                    hasCurdChaas = hasCurdChaas,
                                    hasGheeButter = hasGheeButter,
                                    hasKhoyaSweets = hasKhoyaSweets,
                                    hasCattleFeed = hasCattleFeed,
                                    supportedMilkTypes = selectedMilkType,
                                    cowRate = 50.0,
                                    buffaloRate = 65.0,
                                    pricingMode = selectedPricingMode
                                )
                                onComplete()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentStep == totalSteps) GrassGreen else RoyalBluePrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("wizard_next_btn")
                    ) {
                        Text(
                            text = if (currentStep == totalSteps) "🚀 Launch Customized App" else "Next Step",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            if (currentStep == totalSteps) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SurfaceBg)
        ) {
            when (currentStep) {
                1 -> WizardStepRoleSelection(
                    selectedRoleCode = selectedRoleCode,
                    onSelectRole = { applyRoleDefaults(it) }
                )
                2 -> WizardStepSourcingActivities(
                    sourceOwnCattle = sourceOwnCattle,
                    onToggleOwnCattle = { sourceOwnCattle = it },
                    sourceVillageFarmers = sourceVillageFarmers,
                    onToggleVillageFarmers = { sourceVillageFarmers = it }
                )
                3 -> WizardStepSalesActivities(
                    destHouseholds = destHouseholds,
                    onToggleHouseholds = { destHouseholds = it },
                    destCollectionCenter = destCollectionCenter,
                    onToggleCollectionCenter = { destCollectionCenter = it },
                    destBulkCommercial = destBulkCommercial,
                    onToggleBulkCommercial = { destBulkCommercial = it },
                    destFactoryTankers = destFactoryTankers,
                    onToggleFactoryTankers = { destFactoryTankers = it }
                )
                4 -> WizardStepMilkAndProcessingActivities(
                    selectedMilkType = selectedMilkType,
                    onSelectMilkType = { selectedMilkType = it },
                    hasPaneer = hasPaneer,
                    onTogglePaneer = { hasPaneer = it },
                    hasGheeButter = hasGheeButter,
                    onToggleGheeButter = { hasGheeButter = it },
                    hasCurdChaas = hasCurdChaas,
                    onToggleCurdChaas = { hasCurdChaas = it },
                    hasKhoyaSweets = hasKhoyaSweets,
                    onToggleKhoyaSweets = { hasKhoyaSweets = it },
                    hasCattleFeed = hasCattleFeed,
                    onToggleCattleFeed = { hasCattleFeed = it }
                )
                5 -> WizardStepProfileAndLiveReview(
                    computedMode = computedMode,
                    sourceOwnCattle = sourceOwnCattle,
                    sourceVillageFarmers = sourceVillageFarmers,
                    destHouseholds = destHouseholds,
                    destCollectionCenter = destCollectionCenter,
                    destBulkCommercial = destBulkCommercial,
                    destFactoryTankers = destFactoryTankers,
                    hasPaneer = hasPaneer,
                    hasGheeButter = hasGheeButter,
                    hasCurdChaas = hasCurdChaas,
                    hasKhoyaSweets = hasKhoyaSweets,
                    hasCattleFeed = hasCattleFeed,
                    selectedMilkType = selectedMilkType,
                    businessName = businessName,
                    onBusinessNameChange = { businessName = it },
                    ownerName = ownerName,
                    onOwnerNameChange = { ownerName = it },
                    phone = phone,
                    onPhoneChange = { phone = it },
                    pricingMode = selectedPricingMode,
                    onPricingModeChange = { selectedPricingMode = it },
                    onOpenCustomFormulaStudio = { showCustomFormulaDialog = true }
                )
            }
        }
    }

    if (showCustomFormulaDialog) {
        CustomFormulaStudioDialog(
            business = null,
            viewModel = viewModel,
            onDismiss = {
                selectedPricingMode = "CUSTOM"
                showCustomFormulaDialog = false
            }
        )
    }
}

// ----------------- STEP 1: BUSINESS ROLE SELECTION -----------------
@Composable
private fun WizardStepRoleSelection(
    selectedRoleCode: String,
    onSelectRole: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "1. Select Your Primary Dairy Role",
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Text(
                text = "Choose the description that best fits your daily business. This configures standard defaults that you can fine-tune in the next steps.",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
        }

        items(BusinessModeFeatures.ALL_TYPES) { role ->
            val isSelected = selectedRoleCode == role.code
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) role.themeColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                ),
                border = if (isSelected) BorderStroke(2.dp, role.themeColor) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectRole(role.code) }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) role.themeColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(role.icon, contentDescription = null, tint = role.themeColor, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(role.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                Text(role.subtitle, fontSize = 10.5.sp, color = TextSecondary, lineHeight = 13.sp)
                            }
                        }

                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelectRole(role.code) },
                            colors = RadioButtonDefaults.colors(selectedColor = role.themeColor)
                        )
                    }

                    if (isSelected) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = role.themeColor.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = role.themeColor, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Selected Profile: ${role.badge}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = role.themeColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------- STEP 2: SOURCING ACTIVITIES -----------------
@Composable
private fun WizardStepSourcingActivities(
    sourceOwnCattle: Boolean,
    onToggleOwnCattle: (Boolean) -> Unit,
    sourceVillageFarmers: Boolean,
    onToggleVillageFarmers: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "2. Where Does Your Milk Come From?",
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Text(
                text = "Select your milk sourcing & intake activities. MilkMate turns on cattle herd breeding or supplier procurement desks accordingly.",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
        }

        item {
            InteractiveActivityCard(
                title = "🐄 Milking Own Cattle Herd (Cows / Buffaloes)",
                subtitle = "I maintain my own dairy animals and record daily milking yields.",
                activeFeatures = listOf(
                    "Cattle Tag IDs & Herd Profiles",
                    "Morning/Evening Milking Yield Recording",
                    "Breeding Stages, A.I. Alarms & Calving Countdown",
                    "Cattle Feed, Fodder & Veterinary Expenses"
                ),
                checked = sourceOwnCattle,
                onToggle = onToggleOwnCattle,
                accentColor = GrassGreen
            )
        }

        item {
            InteractiveActivityCard(
                title = "🧑‍🌾 Buying / Procuring from Other Farmers & Suppliers",
                subtitle = "I purchase raw milk cans from local village farmers or outside suppliers.",
                activeFeatures = listOf(
                    "High-Speed Farmer Collection Desk (FAT, SNF, CLR)",
                    "Dynamic Shift Rate Charts & Slip Printing",
                    "Farmer Khatas & 10-Day / Monthly Payout Sheets",
                    "Cattle Feed Advance & Loan Deductions"
                ),
                checked = sourceVillageFarmers,
                onToggle = onToggleVillageFarmers,
                accentColor = RoyalBluePrimary
            )
        }
    }
}

// ----------------- STEP 3: SALES ACTIVITIES -----------------
@Composable
private fun WizardStepSalesActivities(
    destHouseholds: Boolean,
    onToggleHouseholds: (Boolean) -> Unit,
    destCollectionCenter: Boolean,
    onToggleCollectionCenter: (Boolean) -> Unit,
    destBulkCommercial: Boolean,
    onToggleBulkCommercial: (Boolean) -> Unit,
    destFactoryTankers: Boolean,
    onToggleFactoryTankers: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "3. Which Sales Channels Do You Use?",
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Text(
                text = "Select all the ways you sell or dispatch milk. We will only show relevant delivery and billing screens.",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
        }

        item {
            InteractiveActivityCard(
                title = "🏠 Household Doorstep Deliveries (Subscribers)",
                subtitle = "Morning/evening residential delivery routes with daily quantity marking.",
                activeFeatures = listOf(
                    "Delivery Boy Route Navigation",
                    "Monthly Customer Khatas & WhatsApp Invoices",
                    "Bottle / Can Deposit Tracking"
                ),
                checked = destHouseholds,
                onToggle = onToggleHouseholds,
                accentColor = RoyalBluePrimary
            )
        }

        item {
            InteractiveActivityCard(
                title = "🏢 Selling to Dairy Collection Center / Trader",
                subtitle = "I supply daily milk cans to a chilling center, BMC, or milk vendor.",
                activeFeatures = listOf(
                    "Daily Outward Supply Slip Tracker",
                    "FAT/SNF & Rate Received Ledger",
                    "Payment Reconciliation from Center"
                ),
                checked = destCollectionCenter,
                onToggle = onToggleCollectionCenter,
                accentColor = GrassGreen
            )
        }

        item {
            InteractiveActivityCard(
                title = "🏨 Bulk Commercial Buyers (Hotels, Cafes, Dairies)",
                subtitle = "Supply bulk milk cans to restaurants, tea stalls, sweet shops, and institutions.",
                activeFeatures = listOf(
                    "B2B Commercial Accounts & Special Rates",
                    "Weekly / Monthly Consolidated Invoicing",
                    "Payment Reminders & Outstanding Balance"
                ),
                checked = destBulkCommercial,
                onToggle = onToggleBulkCommercial,
                accentColor = WarmHoney
            )
        }

        item {
            InteractiveActivityCard(
                title = "🚛 Bulk Tankers to Dairy Factories & Plants",
                subtitle = "Dispatch large tankers or chilling center output to dairy processing plants.",
                activeFeatures = listOf(
                    "Bulk Tanker Dispatch Challans",
                    "Factory Net Payout & BMC Chilling Margin",
                    "Temperature & Dip Rod Calibration Logs"
                ),
                checked = destFactoryTankers,
                onToggle = onToggleFactoryTankers,
                accentColor = DeepOceanNavy
            )
        }
    }
}

// ----------------- STEP 4: MILK & PROCESSING ACTIVITIES -----------------
@Composable
private fun WizardStepMilkAndProcessingActivities(
    selectedMilkType: String,
    onSelectMilkType: (String) -> Unit,
    hasPaneer: Boolean,
    onTogglePaneer: (Boolean) -> Unit,
    hasGheeButter: Boolean,
    onToggleGheeButter: (Boolean) -> Unit,
    hasCurdChaas: Boolean,
    onToggleCurdChaas: (Boolean) -> Unit,
    hasKhoyaSweets: Boolean,
    onToggleKhoyaSweets: (Boolean) -> Unit,
    hasCattleFeed: Boolean,
    onToggleCattleFeed: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "4. Milk Types & Dairy Processing Activities",
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Text(
                text = "Select the milk varieties you handle and any value-added dairy byproducts you produce or sell.",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
        }

        // Milk Type Selector
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Milk Varieties Handled:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    listOf(
                        "COW_ONLY" to "🐄 Cow Milk Only (Desi / HF / Jersey)",
                        "BUFFALO_ONLY" to "🐃 Buffalo Milk Only (Murrah / Jafrabadi)",
                        "BOTH" to "🥛 Both Cow & Buffalo (Separate Tracking & Rates)",
                        "MIXED" to "🔄 Mixed Milk (Common Standard Rate)"
                    ).forEach { (code, label) ->
                        val isSel = selectedMilkType == code
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectMilkType(code) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSel,
                                onClick = { onSelectMilkType(code) },
                                colors = RadioButtonDefaults.colors(selectedColor = RoyalBluePrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, fontSize = 12.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal, color = TextPrimary)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text("Dairy Byproduct Processing Activities:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("Auto-seeds your stock catalog with yield conversion calculators.", fontSize = 11.sp, color = TextSecondary)
        }

        item {
            ProductCardOption(
                title = "🧀 Fresh Paneer (Cottage Cheese)",
                subtitle = "Batch yield from raw milk & kg block sales",
                checked = hasPaneer,
                onToggle = onTogglePaneer
            )
        }

        item {
            ProductCardOption(
                title = "🧈 Pure Desi Ghee & Fresh Butter (Makhan)",
                subtitle = "Clarified butter jars, yellow & white makhan tubs",
                checked = hasGheeButter,
                onToggle = onToggleGheeButter
            )
        }

        item {
            ProductCardOption(
                title = "🍶 Curd (Dahi) & Buttermilk (Chaas / Lassi)",
                subtitle = "Probiotic cups, pouches, matka curd & chilled chaas",
                checked = hasCurdChaas,
                onToggle = onToggleCurdChaas
            )
        }

        item {
            ProductCardOption(
                title = "🍬 Khoya / Mawa & Base Sweets",
                subtitle = "Condensed milk solids for confectionery",
                checked = hasKhoyaSweets,
                onToggle = onToggleKhoyaSweets
            )
        }

        item {
            ProductCardOption(
                title = "🌾 Cattle Feed & Mineral Supplements",
                subtitle = "Animal nutrition bags, chuni & khali",
                checked = hasCattleFeed,
                onToggle = onToggleCattleFeed
            )
        }
    }
}

// ----------------- STEP 5: PROFILE & LIVE FEATURE PREVIEW -----------------
@Composable
private fun WizardStepProfileAndLiveReview(
    computedMode: DairyBusinessType,
    sourceOwnCattle: Boolean,
    sourceVillageFarmers: Boolean,
    destHouseholds: Boolean,
    destCollectionCenter: Boolean,
    destBulkCommercial: Boolean,
    destFactoryTankers: Boolean,
    hasPaneer: Boolean,
    hasGheeButter: Boolean,
    hasCurdChaas: Boolean,
    hasKhoyaSweets: Boolean,
    hasCattleFeed: Boolean,
    selectedMilkType: String,
    businessName: String,
    onBusinessNameChange: (String) -> Unit,
    ownerName: String,
    onOwnerNameChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    pricingMode: String,
    onPricingModeChange: (String) -> Unit,
    onOpenCustomFormulaStudio: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "5. Business Details & Feature Preview",
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Text(
                text = "Review your tailored workspace configuration below. Only the features you need will be shown!",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
        }

        // Live Feature Preview Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = OceanMidnight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(computedMode.icon, contentDescription = null, tint = FreshGold, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tailored Workspace Profile",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = FreshGold.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = computedMode.badge,
                                color = FreshGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = computedMode.description,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

                    Text("✨ Active Workspace Modules:", color = FreshGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    ActiveFeatureLine(label = "Cattle Herd Milking & Breeding", isActive = sourceOwnCattle)
                    ActiveFeatureLine(label = "Farmer Procurement Desk & Payouts", isActive = sourceVillageFarmers)
                    ActiveFeatureLine(label = "Household Delivery Routes & Monthly Invoices", isActive = destHouseholds)
                    ActiveFeatureLine(label = "Collection Center / BMC Supply Slips", isActive = destCollectionCenter)
                    ActiveFeatureLine(label = "Bulk Commercial B2B Accounts", isActive = destBulkCommercial)
                    ActiveFeatureLine(label = "Factory Bulk Tanker Logistics", isActive = destFactoryTankers)
                    ActiveFeatureLine(
                        label = "Value-Added Products & Inventory",
                        isActive = hasPaneer || hasGheeButter || hasCurdChaas || hasKhoyaSweets || hasCattleFeed
                    )
                }
            }
        }

        // Pricing Rate Calculation Mode
        item {
            Text("Milk Pricing & Yield Calculation Method:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("Select how milk buy/sale prices are calculated (per liter, fat/snf, or byproduct yield):", fontSize = 11.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    Triple("DIRECT", "💵 Flat Rate per Liter", "Fixed ₹ / Liter for Cow & Buffalo"),
                    Triple("FAT_SNF", "🧪 FAT + SNF Matrix Chart", "Standard Dairy Board two-axis calculation"),
                    Triple("FAT_ONLY", "🧈 Pure FAT Rate / Per Kg FAT", "Rate calculated directly from Fat percentage"),
                    Triple("PANEER_YIELD", "🧀 Paneer Yield Pricing", "Rate derived from grams of Paneer made per Liter"),
                    Triple("KHOA_YIELD", "🍬 Khoya / Mawa Yield Pricing", "Rate derived from grams of Khoya/Mawa made per Liter"),
                    Triple("GHEE_YIELD", "🏺 Ghee Yield Pricing", "Rate derived from Fat recovery % into Desi Ghee"),
                    Triple("CLR_TS", "🔬 CLR Lactometer Reading Formula", "SNF = (CLR/4) + (0.25*FAT) + 0.36"),
                    Triple("CUSTOM", "⚙️ Custom Formula", "Configurable base + multipliers & bonuses")
                ).forEach { (mode, label, desc) ->
                    val isSel = pricingMode == mode
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSel) RoyalBluePrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, if (isSel) RoyalBluePrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPricingModeChange(mode) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSel,
                                onClick = { onPricingModeChange(mode) },
                                colors = RadioButtonDefaults.colors(selectedColor = RoyalBluePrimary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(desc, fontSize = 10.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }

            if (pricingMode == "CUSTOM") {
                Spacer(modifier = Modifier.height(6.dp))
                FilledTonalButton(
                    onClick = onOpenCustomFormulaStudio,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = OceanMidnight, contentColor = FreshGold),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Icon(Icons.Default.Functions, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("📐 Open Custom Formula Studio & Formula Maker →", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Business Form Fields
        item {
            Text("Dairy / Farm Details:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = businessName,
                onValueChange = onBusinessNameChange,
                label = { Text("Dairy / Farm Name *") },
                placeholder = { Text("e.g. Gokul Dairy Farm") },
                leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null, tint = RoyalBluePrimary) },
                modifier = Modifier.fillMaxWidth().testTag("wizard_final_biz_name"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = ownerName,
                onValueChange = onOwnerNameChange,
                label = { Text("Proprietor / Owner Name") },
                placeholder = { Text("e.g. Ramesh Patel") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = RoyalBluePrimary) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = phone,
                onValueChange = onPhoneChange,
                label = { Text("Mobile Phone Number *") },
                placeholder = { Text("e.g. 9876543210") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = RoyalBluePrimary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }
    }
}

// ----------------- REUSABLE COMPONENT HELPERS -----------------
@Composable
private fun InteractiveActivityCard(
    title: String,
    subtitle: String,
    activeFeatures: List<String>,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    accentColor: Color
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (checked) accentColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        border = if (checked) BorderStroke(2.dp, accentColor) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = TextPrimary)
                    Text(subtitle, fontSize = 11.sp, color = TextSecondary)
                }
                Checkbox(
                    checked = checked,
                    onCheckedChange = onToggle,
                    colors = CheckboxDefaults.colors(checkedColor = accentColor)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (checked) accentColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (checked) "✨ Activated Modules:" else "🔒 Modules when enabled:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (checked) accentColor else TextMuted
                    )
                    activeFeatures.forEach { feat ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (checked) Icons.Default.CheckCircle else Icons.Default.Circle,
                                contentDescription = null,
                                tint = if (checked) accentColor else TextMuted,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = feat,
                                fontSize = 11.sp,
                                color = if (checked) TextPrimary else TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveFeatureLine(label: String, isActive: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (isActive) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (isActive) GrassGreen else TextMuted,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$label: ${if (isActive) "Active" else "Hidden"}",
            color = if (isActive) Color.White else TextMuted,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun ProductCardOption(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (checked) RoyalBluePrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(
            1.dp,
            if (checked) RoyalBluePrimary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onToggle,
                colors = CheckboxDefaults.colors(checkedColor = RoyalBluePrimary)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                Text(subtitle, fontSize = 11.sp, color = TextSecondary)
            }
        }
    }
}
