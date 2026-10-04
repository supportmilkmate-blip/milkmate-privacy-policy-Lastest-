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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MilkMateViewModel
import com.example.ui.theme.*
import com.example.ui.util.BusinessModeFeatures
import com.example.ui.util.DairyBusinessType

/**
 * Enhanced, User-Friendly Post-Signup Setup Screen.
 * 2-Step Fast Flow:
 * Step 1: 1-Tap Operating Model Selection (BMC Collection Centre, Trader, Farm, etc.)
 * Step 2: Dairy Name, Verified Mobile & Optional 4-Digit Security PIN
 * Plus optional expandable customization drawer for milk species & byproducts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InitialBusinessSelectionSetupScreen(
    viewModel: MilkMateViewModel,
    initialPhone: String = "",
    onOpenWizard: () -> Unit,
    onSwitchToLogin: () -> Unit,
    onQuickDemo: () -> Unit
) {
    // Primary selected business model
    var selectedModelCode by remember { mutableStateOf("COLLECTION_CENTER") } // Default to BMC / Collection Centre

    // Capabilities (auto-configured based on selectedModelCode, but user can tweak)
    var sourceOwnCattle by remember { mutableStateOf(false) }
    var sourceVillageFarmers by remember { mutableStateOf(true) }
    var destHouseholds by remember { mutableStateOf(false) }
    var destCollectionCenter by remember { mutableStateOf(false) }
    var destBulkCommercial by remember { mutableStateOf(false) }
    var destFactoryTankers by remember { mutableStateOf(true) }

    // Milk & Products
    var selectedMilkType by remember { mutableStateOf("BOTH") } // COW_ONLY, BUFFALO_ONLY, BOTH, MIXED
    var hasPaneer by remember { mutableStateOf(true) }
    var hasCurdChaas by remember { mutableStateOf(true) }
    var hasGheeButter by remember { mutableStateOf(true) }

    // Registration Details
    var businessName by remember { mutableStateOf("Gokul Milk Collection Centre") }
    var ownerName by remember { mutableStateOf("") }
    var phone by remember(initialPhone) { mutableStateOf(initialPhone.ifBlank { "" }) }
    var securityPin by remember { mutableStateOf("") }
    var enableBiometric by remember { mutableStateOf(true) }
    var showAdvancedCustomization by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    // Quick helper to apply smart defaults when a business model is tapped
    fun applyModelPreset(code: String) {
        selectedModelCode = code
        when (code) {
            "COLLECTION_CENTER" -> {
                sourceOwnCattle = false
                sourceVillageFarmers = true
                destHouseholds = false
                destCollectionCenter = false
                destBulkCommercial = false
                destFactoryTankers = true
                businessName = "Gokul Milk Collection Centre"
            }
            "TRADER" -> {
                sourceOwnCattle = false
                sourceVillageFarmers = true
                destHouseholds = true
                destCollectionCenter = true
                destBulkCommercial = true
                destFactoryTankers = false
                businessName = "Kisan Milk Traders & Supply"
            }
            "FARMER" -> {
                sourceOwnCattle = true
                sourceVillageFarmers = false
                destHouseholds = true
                destCollectionCenter = true
                destBulkCommercial = false
                destFactoryTankers = false
                businessName = "Ananda Dairy Farm"
            }
            "FARMER_WHOLESALE" -> {
                sourceOwnCattle = true
                sourceVillageFarmers = false
                destHouseholds = false
                destCollectionCenter = true
                destBulkCommercial = false
                destFactoryTankers = false
                businessName = "Green Valley Cattle Farm"
            }
            "INTEGRATED" -> {
                sourceOwnCattle = true
                sourceVillageFarmers = true
                destHouseholds = true
                destCollectionCenter = true
                destBulkCommercial = true
                destFactoryTankers = true
                businessName = "Heritage Integrated Dairy"
            }
            "RETAIL_PARLOUR" -> {
                sourceOwnCattle = false
                sourceVillageFarmers = false
                destHouseholds = true
                destCollectionCenter = false
                destBulkCommercial = true
                destFactoryTankers = false
                businessName = "Fresh Milk & Dairy Parlour"
            }
        }
    }

    // Initialize preset on first composition
    LaunchedEffect(Unit) {
        applyModelPreset(selectedModelCode)
    }

    val activeConfig = remember(selectedModelCode) {
        BusinessModeFeatures.getByType(selectedModelCode)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ----------------- WELCOME & STEP INDICATOR -----------------
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().testTag("setup_welcome_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = RoyalBlueLight,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.WaterDrop,
                                        contentDescription = null,
                                        tint = RoyalBluePrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Welcome to MilkMate! 🥛",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Quick 1-Minute Dairy Setup",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = RoyalBluePrimary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DairyGreenLight
                        ) {
                            Text(
                                text = "Easy Setup",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DairyGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Choose how your dairy operates below. MilkMate will automatically turn on the exact features, rate charts, and tools you need, keeping everything else out of your way.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        // ----------------- STEP 1: CHOOSE BUSINESS MODEL -----------------
        item {
            Text(
                text = "STEP 1: What type of dairy business do you run? (Tap to select)",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
        }

        // The 6 Main Business Models
        val modelsList = listOf(
            Triple("COLLECTION_CENTER", "Milk Collection Centre / BMC (Kendra)", "Procure milk from village farmers with FAT/SNF testing, maintain farmer passbooks, chill in bulk vats & dispatch factory tankers."),
            Triple("TRADER", "Milk Trader / Vendor / Wholesaler", "Procure milk from farmers or chilling centres, distribute to households, bulk buyers, restaurants, hotels & tea stalls with profit spread tracking."),
            Triple("FARMER", "Dairy Farm & Tabela (Herd + Delivery)", "Own cow/buffalo herd, manage daily morning & evening milking production, cattle breeding, and doorstep household subscription routes."),
            Triple("FARMER_WHOLESALE", "Dairy Farmer (Wholesale / Supply to Center)", "Own cattle herd, supplying daily milk cans to village collection centre, BMC, or milk trader with supply slips and FAT/SNF ledger."),
            Triple("INTEGRATED", "Integrated Enterprise (All-in-One)", "Complete dairy setup: own cattle herd, village farmer procurement, milk chilling, byproduct processing, retail delivery routes & wholesale."),
            Triple("RETAIL_PARLOUR", "Dairy Parlour & Retail Outlet", "Milk retail counter, packaged milk, paneer, curd, ghee sales with instant Point-of-Sale (POS) billing and customer khata accounts.")
        )

        items(modelsList) { (code, title, desc) ->
            val isSelected = selectedModelCode == code
            val config = BusinessModeFeatures.getByType(code)

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) config.themeColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) config.themeColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { applyModelPreset(code) }
                    .testTag("model_card_$code")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) config.themeColor else config.themeColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = config.icon,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else config.themeColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) config.themeColor else TextPrimary
                            )
                            if (isSelected) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = config.themeColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = config.themeColor.copy(alpha = 0.15f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = config.badge,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = config.themeColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = desc,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }

        // ----------------- STEP 2: PROFILE & SECURITY -----------------
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "STEP 2: Dairy Details & Fast Login PIN",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().testTag("setup_profile_card")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (validationError != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = validationError ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    // Dairy Name with Quick Suggestion Chips
                    Column {
                        OutlinedTextField(
                            value = businessName,
                            onValueChange = {
                                businessName = it
                                validationError = null
                            },
                            label = { Text("Dairy / Business Name *") },
                            placeholder = { Text("e.g. Gokul Milk Collection Centre") },
                            leadingIcon = {
                                Icon(Icons.Default.Storefront, contentDescription = null, tint = activeConfig.themeColor)
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("setup_business_name_input")
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Suggested names (tap to fill):", fontSize = 10.5.sp, color = TextSecondary)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "Gokul Dairy",
                                "Kisan Milk",
                                "Shree Krishna Dairy"
                            ).forEach { suggested ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.clickable { businessName = suggested }
                                ) {
                                    Text(
                                        text = suggested,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = RoyalBluePrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Owner Name
                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { ownerName = it },
                        label = { Text("Owner / Manager Name") },
                        placeholder = { Text("e.g. Ramesh Patel") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = activeConfig.themeColor)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("setup_owner_name_input")
                    )

                    // Verified Phone Badge OR Phone Input
                    if (phone.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DairyGreenLight,
                            border = BorderStroke(1.dp, DairyGreen.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Registered Mobile: +91 $phone", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                        Text("Verified via SMS OTP ✓", fontSize = 11.sp, color = DairyGreen, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = {
                                phone = it
                                validationError = null
                            },
                            label = { Text("Registered Mobile Number *") },
                            placeholder = { Text("e.g. 9876543210") },
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = activeConfig.themeColor)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("setup_owner_phone_input")
                        )
                    }

                    // Fast Login PIN Box
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = FreshGoldLight,
                        border = BorderStroke(1.dp, WarmHoney.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = WarmHoney, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Fast Login PIN & Screen Lock (Optional)", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("Open your dairy instantly next time with 4 digits or fingerprint", fontSize = 10.5.sp, color = TextSecondary)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = securityPin,
                                onValueChange = {
                                    if (it.length <= 4) {
                                        securityPin = it.filter { c -> c.isDigit() }
                                    }
                                },
                                label = { Text("Set 4-Digit PIN") },
                                placeholder = { Text("e.g. 1234") },
                                leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null, tint = WarmHoney) },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("setup_pin_input")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Enable Fingerprint / Mobile Screen Lock", fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                Switch(
                                    checked = enableBiometric,
                                    onCheckedChange = { enableBiometric = it }
                                )
                            }
                        }
                    }

                    // Expandable Advanced Customization Drawer
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAdvancedCustomization = !showAdvancedCustomization }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Customize Sources, Species & Byproducts", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("Tweak cow/buffalo milk, paneer, curd, delivery routes", fontSize = 10.sp, color = TextSecondary)
                                }
                            }
                            Icon(
                                imageVector = if (showAdvancedCustomization) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        }
                    }

                    if (showAdvancedCustomization) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text("Milk Species Handled:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "COW_ONLY" to "🐄 Cow",
                                    "BUFFALO_ONLY" to "🐃 Buffalo",
                                    "BOTH" to "🥛 Both",
                                    "MIXED" to "🍶 Mix"
                                ).forEach { (code, label) ->
                                    val isSel = selectedMilkType == code
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { selectedMilkType = code },
                                        label = { Text(label, fontSize = 10.5.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Text("Byproducts Processing:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = hasPaneer,
                                    onClick = { hasPaneer = !hasPaneer },
                                    label = { Text("🧀 Paneer", fontSize = 10.5.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = hasCurdChaas,
                                    onClick = { hasCurdChaas = !hasCurdChaas },
                                    label = { Text("🍶 Curd", fontSize = 10.5.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = hasGheeButter,
                                    onClick = { hasGheeButter = !hasGheeButter },
                                    label = { Text("🧈 Ghee", fontSize = 10.5.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Primary Launch Button
                    Button(
                        onClick = {
                            if (businessName.isBlank()) {
                                validationError = "Please enter your Dairy / Business Name."
                            } else if (phone.isBlank()) {
                                validationError = "Please enter your mobile phone number."
                            } else {
                                viewModel.completeFirstLaunchWithCapabilities(
                                    businessName = businessName.trim(),
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
                                    hasKhoyaSweets = false,
                                    hasCattleFeed = false,
                                    supportedMilkTypes = selectedMilkType,
                                    cowRate = 50.0,
                                    buffaloRate = 65.0,
                                    pricingMode = if (selectedModelCode == "COLLECTION_CENTER" || selectedModelCode == "TRADER") "FAT_SNF" else "DIRECT"
                                )

                                if (securityPin.length == 4 || enableBiometric) {
                                    viewModel.setSecuritySettings(
                                        appLockEnabled = securityPin.length == 4,
                                        pin = if (securityPin.length == 4) securityPin else null,
                                        biometricEnabled = enableBiometric
                                    )
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = activeConfig.themeColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("launch_tailored_workspace_btn")
                    ) {
                        Text(
                            text = "🚀 Launch ${activeConfig.shortName} Workspace",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Quick Demo Button
                    FilledTonalButton(
                        onClick = onQuickDemo,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp).testTag("quick_demo_launch_btn")
                    ) {
                        Icon(Icons.Default.PlayCircle, contentDescription = null, tint = GrassGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Instant 1-Tap Quick Demo (Preloaded Dairy)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GrassGreen)
                    }
                }
            }
        }

        // ----------------- SWITCH TO LOGIN FOOTER -----------------
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Already registered an existing dairy?", fontSize = 12.sp, color = TextSecondary)
                TextButton(onClick = onSwitchToLogin) {
                    Text("Sign In Here →", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                }
            }
        }
    }
}
