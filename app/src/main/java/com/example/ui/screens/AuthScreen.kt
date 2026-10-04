package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MilkMateViewModel
import com.example.ui.theme.*
import com.example.ui.util.BiometricAuthHelper
import com.example.ui.util.BusinessModeFeatures
import kotlinx.coroutines.delay

/**
 * Redesigned, User-Friendly Authentication & Signup Onboarding Screen
 * 
 * Features:
 * 1. Top Mode Switcher: [✨ Create New Dairy (Sign Up)] vs [🔑 Quick Log In]
 * 2. Activity-Based Customization during Signup:
 *    - Manage Herd (Yes/No -> Cow, Buffalo, or Both; Feed & Consumables Compulsory)
 *    - Source Procurement (Village Farmers, Bulk Inward)
 *    - Selling Channels (Individual Households, Restaurants/B2B, Traders, BMC Collection Centre, Retail Counter)
 *    - Own Processing Plant (Paneer, Curd, Ghee, Khoya)
 *    - Dynamic AI / Smart Model Recommendation & Live Capabilities Preview
 * 3. Comprehensive Login Options:
 *    - Mobile OTP Login
 *    - 4-Digit Fast PIN & Biometric Fingerprint Unlock
 *    - Staff & Delivery Boy Login
 *    - Multi-Dairy Local Account Switcher
 *    - Instant Sample Demo Mode
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(viewModel: MilkMateViewModel) {
    val session by viewModel.sessionState.collectAsStateWithLifecycle()

    // Top Auth Mode: "SIGNUP" (New Dairy Onboarding) or "LOGIN" (Returning Owner / Staff)
    var topAuthMode by remember { mutableStateOf("SIGNUP") }

    // Signup Wizard Step: 1 = Phone & Identity, 2 = Activities & Best Fit ("What's Best For You"), 3 = Customise Rules, 4 = Review & Launch
    var signupStep by remember { mutableIntStateOf(1) }

    // Login Sub-Tab: "OTP", "PIN", "STAFF", "ACCOUNTS"
    var loginSubTab by remember { mutableStateOf("OTP") }

    // Identity Inputs
    var phoneInput by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    var generatedOtp by remember { mutableStateOf<String?>(null) }
    var isOtpVerified by remember { mutableStateOf(false) }
    var timerSeconds by remember { mutableIntStateOf(30) }
    var isVerifying by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var dairyName by remember { mutableStateOf("Gokul Milk Dairy & Farm") }
    var ownerName by remember { mutableStateOf("") }
    var securityPin by remember { mutableStateOf("") }
    var enableBiometric by remember { mutableStateOf(true) }

    // ================= ACTIVITY-BASED SELECTIONS =================
    // 1. Source Activities
    var actManageHerd by remember { mutableStateOf(true) }
    var actHerdSpecies by remember { mutableStateOf("BOTH") } // COW, BUFFALO, BOTH
    var actProcureFromFarmers by remember { mutableStateOf(false) }
    var actBulkInward by remember { mutableStateOf(false) }

    // 2. Sales & Distribution Channels (Multi-Select)
    var actSellHouseholds by remember { mutableStateOf(true) }
    var actSellRestaurants by remember { mutableStateOf(false) }
    var actSellTraders by remember { mutableStateOf(false) }
    var actSellCollectionCentre by remember { mutableStateOf(false) }
    var actSellRetailCounter by remember { mutableStateOf(false) }

    // 3. Processing & Value-Add Products
    var actOwnProcessingPlant by remember { mutableStateOf(false) }
    var customHasPaneer by remember { mutableStateOf(true) }
    var customHasCurd by remember { mutableStateOf(true) }
    var customHasGhee by remember { mutableStateOf(true) }
    var customHasKhoya by remember { mutableStateOf(false) }

    // Computed Model Code based on activities
    val computedModelCode = remember(
        actManageHerd, actProcureFromFarmers, actSellHouseholds,
        actSellRestaurants, actSellTraders, actSellCollectionCentre, actOwnProcessingPlant
    ) {
        when {
            actManageHerd && actProcureFromFarmers -> "INTEGRATED"
            actManageHerd && actSellHouseholds -> "FARMER"
            actManageHerd && !actSellHouseholds && (actSellCollectionCentre || actSellTraders || actSellRestaurants) -> "FARMER_WHOLESALE"
            !actManageHerd && actProcureFromFarmers && actSellCollectionCentre -> "COLLECTION_CENTER"
            !actManageHerd && actProcureFromFarmers -> "TRADER"
            actOwnProcessingPlant -> "PROCESSING_UNIT"
            else -> "FARMER"
        }
    }

    var selectedModelCode by remember { mutableStateOf("FARMER") }
    LaunchedEffect(computedModelCode) {
        selectedModelCode = computedModelCode
    }

    // Customisation Options during Signup
    var customMilkType by remember { mutableStateOf("BOTH") } // COW_ONLY, BUFFALO_ONLY, BOTH, MIXED
    var customPricingMode by remember { mutableStateOf("DIRECT") } // FAT_SNF, DIRECT, FAT_ONLY, PANEER_YIELD
    var customCowRate by remember { mutableStateOf("50.0") }
    var customBuffaloRate by remember { mutableStateOf("65.0") }
    var selectedLanguage by remember { mutableStateOf(session.languageCode) }

    // Language selection dialog
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showAdvisorDialog by remember { mutableStateOf(false) }

    // Timer countdown for OTP resend
    LaunchedEffect(signupStep, loginSubTab, timerSeconds) {
        if (timerSeconds > 0 && generatedOtp != null) {
            delay(1000)
            timerSeconds -= 1
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.WaterDrop,
                                    contentDescription = "MilkMate Logo",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "MilkMate Dairy OS",
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                fontSize = 17.sp
                            )
                            Text(
                                text = "Smart Milk Delivery, Collection & Farm Accounting",
                                fontSize = 10.5.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                },
                actions = {
                    // Quick Language Switcher Action
                    IconButton(onClick = { showLanguageDialog = true }) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Language, contentDescription = "Language", tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = selectedLanguage.uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = RoyalBluePrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // ================= TOP MAIN MODE SELECTOR (SIGN UP vs LOG IN) =================
            Surface(
                color = RoyalBluePrimary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Sign Up Tab
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (topAuthMode == "SIGNUP") Color.White else Color.White.copy(alpha = 0.15f),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                topAuthMode = "SIGNUP"
                                errorMessage = null
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddBusiness,
                                contentDescription = null,
                                tint = if (topAuthMode == "SIGNUP") RoyalBluePrimary else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Create Dairy (Sign Up)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (topAuthMode == "SIGNUP") RoyalBluePrimary else Color.White
                            )
                        }
                    }

                    // Log In Tab
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (topAuthMode == "LOGIN") Color.White else Color.White.copy(alpha = 0.15f),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                topAuthMode = "LOGIN"
                                errorMessage = null
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Login,
                                contentDescription = null,
                                tint = if (topAuthMode == "LOGIN") RoyalBluePrimary else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Quick Log In",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (topAuthMode == "LOGIN") RoyalBluePrimary else Color.White
                            )
                        }
                    }
                }
            }

            // Error Banner if present
            if (errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { errorMessage = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // ================= BODY CONTENT =================
            if (topAuthMode == "SIGNUP") {
                // ================= SIGN UP WORKFLOW =================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Signup Multi-Step Stepper Header
                    SignupStepperHeader(currentStep = signupStep, onStepClick = { targetStep ->
                        if (targetStep < signupStep) {
                            signupStep = targetStep
                            errorMessage = null
                        }
                    })

                    AnimatedContent(
                        targetState = signupStep,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> -width } + fadeOut()
                                )
                            } else {
                                (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> width } + fadeOut()
                                )
                            }
                        },
                        label = "SignupStepAnim"
                    ) { step ->
                        when (step) {
                            // STEP 1: Phone, OTP & Dairy Identity
                            1 -> SignupStep1Identity(
                                phoneInput = phoneInput,
                                onPhoneChange = { phoneInput = it; errorMessage = null },
                                otpInput = otpInput,
                                onOtpChange = { otpInput = it; errorMessage = null },
                                generatedOtp = generatedOtp,
                                isOtpVerified = isOtpVerified,
                                timerSeconds = timerSeconds,
                                isVerifying = isVerifying,
                                dairyName = dairyName,
                                onDairyNameChange = { dairyName = it },
                                ownerName = ownerName,
                                onOwnerNameChange = { ownerName = it },
                                securityPin = securityPin,
                                onPinChange = { securityPin = it },
                                enableBiometric = enableBiometric,
                                onBiometricChange = { enableBiometric = it },
                                onSendOtp = {
                                    if (phoneInput.length < 10) {
                                        errorMessage = "Please enter a valid 10-digit mobile number."
                                    } else {
                                        errorMessage = null
                                        viewModel.sendOtp(phoneInput) { code ->
                                            generatedOtp = code
                                            timerSeconds = 30
                                        }
                                    }
                                },
                                onVerifyOtp = {
                                    if (otpInput.length < 6) {
                                        errorMessage = "Please enter the 6-digit OTP."
                                    } else {
                                        isVerifying = true
                                        errorMessage = null
                                        viewModel.verifyOtpAndLogin(phoneInput, otpInput) { isNewUser, matchedBiz ->
                                            isVerifying = false
                                            if (!isNewUser && matchedBiz != null) {
                                                // Already registered
                                            } else {
                                                isOtpVerified = true
                                            }
                                        }
                                    }
                                },
                                onNext = {
                                    if (phoneInput.length < 10) {
                                        errorMessage = "Please enter a valid 10-digit phone number."
                                    } else if (dairyName.isBlank()) {
                                        errorMessage = "Please enter your Dairy / Business Name."
                                    } else {
                                        errorMessage = null
                                        signupStep = 2
                                    }
                                },
                                onQuickDemo = { viewModel.quickDemoLogin() }
                            )

                            // STEP 2: Activity-Based Customization ("What's Best For You")
                            2 -> SignupStep2ActivityCustomization(
                                manageHerd = actManageHerd,
                                onManageHerdChange = { actManageHerd = it },
                                herdSpecies = actHerdSpecies,
                                onHerdSpeciesChange = {
                                    actHerdSpecies = it
                                    customMilkType = if (it == "COW") "COW_ONLY" else if (it == "BUFFALO") "BUFFALO_ONLY" else "BOTH"
                                },
                                procureFromFarmers = actProcureFromFarmers,
                                onProcureFarmersChange = { actProcureFromFarmers = it },
                                bulkInward = actBulkInward,
                                onBulkInwardChange = { actBulkInward = it },
                                sellHouseholds = actSellHouseholds,
                                onSellHouseholdsChange = { actSellHouseholds = it },
                                sellRestaurants = actSellRestaurants,
                                onSellRestaurantsChange = { actSellRestaurants = it },
                                sellTraders = actSellTraders,
                                onSellTradersChange = { actSellTraders = it },
                                sellCollectionCentre = actSellCollectionCentre,
                                onSellCollectionCentreChange = { actSellCollectionCentre = it },
                                sellRetailCounter = actSellRetailCounter,
                                onSellRetailCounterChange = { actSellRetailCounter = it },
                                ownProcessingPlant = actOwnProcessingPlant,
                                onOwnProcessingPlantChange = { actOwnProcessingPlant = it },
                                selectedModelCode = selectedModelCode,
                                onSelectPresetModel = { code ->
                                    selectedModelCode = code
                                    when (code) {
                                        "COLLECTION_CENTER" -> {
                                            actManageHerd = false
                                            actProcureFromFarmers = true
                                            actSellHouseholds = false
                                            actSellCollectionCentre = true
                                            customPricingMode = "FAT_SNF"
                                        }
                                        "FARMER" -> {
                                            actManageHerd = true
                                            actProcureFromFarmers = false
                                            actSellHouseholds = true
                                            customPricingMode = "DIRECT"
                                        }
                                        "FARMER_WHOLESALE" -> {
                                            actManageHerd = true
                                            actProcureFromFarmers = false
                                            actSellHouseholds = false
                                            actSellCollectionCentre = true
                                            customPricingMode = "DIRECT"
                                        }
                                        "TRADER" -> {
                                            actManageHerd = false
                                            actProcureFromFarmers = true
                                            actSellHouseholds = true
                                            actSellRestaurants = true
                                            customPricingMode = "FAT_SNF"
                                        }
                                        "INTEGRATED" -> {
                                            actManageHerd = true
                                            actProcureFromFarmers = true
                                            actSellHouseholds = true
                                            actOwnProcessingPlant = true
                                            customPricingMode = "FAT_SNF"
                                        }
                                    }
                                },
                                onOpenAdvisor = { showAdvisorDialog = true },
                                onBack = { signupStep = 1 },
                                onNext = { signupStep = 3 }
                            )

                            // STEP 3: Deep Customisation (Rates, Pricing Mode, Byproducts, Language)
                            3 -> SignupStep3CustomiseRules(
                                selectedModelCode = selectedModelCode,
                                milkType = customMilkType,
                                onMilkTypeChange = { customMilkType = it },
                                pricingMode = customPricingMode,
                                onPricingModeChange = { customPricingMode = it },
                                cowRate = customCowRate,
                                onCowRateChange = { customCowRate = it },
                                buffaloRate = customBuffaloRate,
                                onBuffaloRateChange = { customBuffaloRate = it },
                                hasPaneer = customHasPaneer,
                                onPaneerChange = { customHasPaneer = it },
                                hasCurd = customHasCurd,
                                onCurdChange = { customHasCurd = it },
                                hasGhee = customHasGhee,
                                onGheeChange = { customHasGhee = it },
                                hasKhoya = customHasKhoya,
                                onKhoyaChange = { customHasKhoya = it },
                                hasCattleFeed = actManageHerd, // Compulsory if herd is managed
                                onCattleFeedChange = { /* Compulsory when managing herd */ },
                                selectedLanguage = selectedLanguage,
                                onLanguageClick = { showLanguageDialog = true },
                                onBack = { signupStep = 2 },
                                onNext = { signupStep = 4 }
                            )

                            // STEP 4: Summary & 1-Tap Dairy Workspace Launch
                            4 -> SignupStep4SummaryLaunch(
                                dairyName = dairyName,
                                ownerName = ownerName,
                                phone = phoneInput,
                                selectedModelCode = selectedModelCode,
                                milkType = customMilkType,
                                pricingMode = customPricingMode,
                                cowRate = customCowRate,
                                buffaloRate = customBuffaloRate,
                                hasPaneer = customHasPaneer,
                                hasCurd = customHasCurd,
                                hasGhee = customHasGhee,
                                hasKhoya = customHasKhoya,
                                hasCattleFeed = actManageHerd,
                                securityPin = securityPin,
                                enableBiometric = enableBiometric,
                                selectedLanguage = selectedLanguage,
                                onBack = { signupStep = 3 },
                                onLaunch = {
                                    val cRate = customCowRate.toDoubleOrNull() ?: 50.0
                                    val bRate = customBuffaloRate.toDoubleOrNull() ?: 65.0

                                    // Save preferred language
                                    viewModel.repository.sessionManager.setLanguage(selectedLanguage)

                                    viewModel.completeFirstLaunchWithCapabilities(
                                        businessName = dairyName.trim(),
                                        ownerName = ownerName.trim().ifBlank { "Dairy Owner" },
                                        phone = phoneInput.trim(),
                                        sourceOwnCattle = actManageHerd,
                                        sourceVillageFarmers = actProcureFromFarmers || actBulkInward,
                                        destHouseholds = actSellHouseholds || actSellRetailCounter,
                                        destCollectionCenter = actSellCollectionCentre,
                                        destBulkCommercial = actSellRestaurants || actSellTraders,
                                        destFactoryTankers = actSellCollectionCentre,
                                        hasPaneer = customHasPaneer,
                                        hasCurdChaas = customHasCurd,
                                        hasGheeButter = customHasGhee,
                                        hasKhoyaSweets = customHasKhoya,
                                        hasCattleFeed = actManageHerd, // Mandatory for herd
                                        supportedMilkTypes = customMilkType,
                                        cowRate = cRate,
                                        buffaloRate = bRate,
                                        pricingMode = customPricingMode
                                    )

                                    if (securityPin.length == 4 || enableBiometric) {
                                        viewModel.setSecuritySettings(
                                            appLockEnabled = securityPin.length == 4,
                                            pin = if (securityPin.length == 4) securityPin else null,
                                            biometricEnabled = enableBiometric
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                // ================= LOGIN WORKFLOW =================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Login Sub-Tabs
                    ScrollableTabRow(
                        selectedTabIndex = when (loginSubTab) {
                            "OTP" -> 0
                            "PIN" -> 1
                            "STAFF" -> 2
                            "ACCOUNTS" -> 3
                            else -> 0
                        },
                        edgePadding = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Tab(
                            selected = loginSubTab == "OTP",
                            onClick = { loginSubTab = "OTP"; errorMessage = null },
                            text = { Text("📱 Phone OTP", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        )
                        Tab(
                            selected = loginSubTab == "PIN",
                            onClick = { loginSubTab = "PIN"; errorMessage = null },
                            text = { Text("🔢 4-Digit PIN", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        )
                        Tab(
                            selected = loginSubTab == "STAFF",
                            onClick = { loginSubTab = "STAFF"; errorMessage = null },
                            text = { Text("🛵 Staff Login", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        )
                        Tab(
                            selected = loginSubTab == "ACCOUNTS",
                            onClick = {
                                loginSubTab = "ACCOUNTS"
                                viewModel.loadExistingBusinesses()
                                errorMessage = null
                            },
                            text = { Text("🏢 Saved Dairies", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        )
                    }

                    when (loginSubTab) {
                        "OTP" -> {
                            Step1PhoneInput(
                                phoneInput = phoneInput,
                                onPhoneChange = { phoneInput = it; errorMessage = null },
                                onSendOtp = {
                                    if (phoneInput.length < 10) {
                                        errorMessage = "Please enter a valid 10-digit mobile number."
                                    } else {
                                        errorMessage = null
                                        viewModel.sendOtp(phoneInput) { code ->
                                            generatedOtp = code
                                            timerSeconds = 30
                                        }
                                    }
                                },
                                onSwitchToStaff = { loginSubTab = "STAFF" },
                                onSwitchToPin = { loginSubTab = "PIN" },
                                onSwitchToAccounts = {
                                    viewModel.loadExistingBusinesses()
                                    loginSubTab = "ACCOUNTS"
                                },
                                onQuickDemo = { viewModel.quickDemoLogin() }
                            )

                            if (generatedOtp != null) {
                                Step2OtpVerification(
                                    phone = phoneInput,
                                    otpInput = otpInput,
                                    generatedOtp = generatedOtp,
                                    timerSeconds = timerSeconds,
                                    isVerifying = isVerifying,
                                    onOtpChange = { otpInput = it; errorMessage = null },
                                    onEditPhone = { generatedOtp = null; otpInput = "" },
                                    onResendOtp = {
                                        viewModel.sendOtp(phoneInput) { code ->
                                            generatedOtp = code
                                            timerSeconds = 30
                                        }
                                    },
                                    onVerifyOtp = {
                                        if (otpInput.length < 6) {
                                            errorMessage = "Please enter 6-digit OTP code."
                                        } else {
                                            isVerifying = true
                                            errorMessage = null
                                            viewModel.verifyOtpAndLogin(phoneInput, otpInput) { isNewUser, _ ->
                                                isVerifying = false
                                                if (isNewUser) {
                                                    // Move to Signup flow
                                                    topAuthMode = "SIGNUP"
                                                    signupStep = 2
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        "PIN" -> {
                            PinBiometricLoginCard(
                                viewModel = viewModel,
                                onSwitchToOtp = { loginSubTab = "OTP" },
                                onSwitchToRegister = {
                                    topAuthMode = "SIGNUP"
                                    signupStep = 1
                                }
                            )
                        }

                        "STAFF" -> {
                            StaffAuthCard(viewModel = viewModel)
                        }

                        "ACCOUNTS" -> {
                            OwnerLoginCard(
                                viewModel = viewModel,
                                onSwitchToRegister = {
                                    topAuthMode = "SIGNUP"
                                    signupStep = 1
                                }
                            )
                        }
                    }

                    // Demo banner
                    FilledTonalButton(
                        onClick = { viewModel.quickDemoLogin() },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = FreshGoldLight, contentColor = Color(0xFF7A5800)),
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("⚡ Explore Instant Demo Dairy (No Login Needed)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // ================= SMART DAIRY ADVISOR QUIZ DIALOG =================
    if (showAdvisorDialog) {
        SmartDairyAdvisorDialog(
            onDismiss = { showAdvisorDialog = false },
            onApplyRecommendation = { recommendedCode ->
                selectedModelCode = recommendedCode
                when (recommendedCode) {
                    "COLLECTION_CENTER" -> {
                        actManageHerd = false
                        actProcureFromFarmers = true
                        actSellHouseholds = false
                        actSellCollectionCentre = true
                        customPricingMode = "FAT_SNF"
                    }
                    "FARMER" -> {
                        actManageHerd = true
                        actProcureFromFarmers = false
                        actSellHouseholds = true
                        customPricingMode = "DIRECT"
                    }
                    "FARMER_WHOLESALE" -> {
                        actManageHerd = true
                        actProcureFromFarmers = false
                        actSellHouseholds = false
                        actSellCollectionCentre = true
                        customPricingMode = "DIRECT"
                    }
                    "TRADER" -> {
                        actManageHerd = false
                        actProcureFromFarmers = true
                        actSellHouseholds = true
                        actSellRestaurants = true
                        customPricingMode = "FAT_SNF"
                    }
                    "INTEGRATED" -> {
                        actManageHerd = true
                        actProcureFromFarmers = true
                        actSellHouseholds = true
                        actOwnProcessingPlant = true
                        customPricingMode = "FAT_SNF"
                    }
                }
                showAdvisorDialog = false
            }
        )
    }

    // ================= LANGUAGE SELECTION MODAL =================
    if (showLanguageDialog) {
        SignupLanguageModal(
            currentLang = selectedLanguage,
            onSelectLang = { langCode ->
                selectedLanguage = langCode
                viewModel.repository.sessionManager.setLanguage(langCode)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }
}

/**
 * Visual Stepper for New Dairy Signup
 */
@Composable
fun SignupStepperHeader(currentStep: Int, onStepClick: (Int) -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SignupStepPill(step = 1, title = "1. Identity", active = currentStep == 1, completed = currentStep > 1, onClick = { onStepClick(1) })
                HorizontalDivider(modifier = Modifier.weight(1f).padding(horizontal = 4.dp), color = if (currentStep > 1) DairyGreen else MaterialTheme.colorScheme.outlineVariant)
                SignupStepPill(step = 2, title = "2. Activities", active = currentStep == 2, completed = currentStep > 2, onClick = { onStepClick(2) })
                HorizontalDivider(modifier = Modifier.weight(1f).padding(horizontal = 4.dp), color = if (currentStep > 2) DairyGreen else MaterialTheme.colorScheme.outlineVariant)
                SignupStepPill(step = 3, title = "3. Customise", active = currentStep == 3, completed = currentStep > 3, onClick = { onStepClick(3) })
                HorizontalDivider(modifier = Modifier.weight(1f).padding(horizontal = 4.dp), color = if (currentStep > 3) DairyGreen else MaterialTheme.colorScheme.outlineVariant)
                SignupStepPill(step = 4, title = "4. Launch", active = currentStep == 4, completed = false, onClick = { onStepClick(4) })
            }
        }
    }
}

@Composable
fun SignupStepPill(step: Int, title: String, active: Boolean, completed: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(enabled = completed, onClick = onClick)
    ) {
        Surface(
            shape = CircleShape,
            color = when {
                completed -> DairyGreen
                active -> RoyalBluePrimary
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.size(22.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (completed) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                } else {
                    Text(
                        text = "$step",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = if (active) RoyalBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * SIGNUP STEP 1: Phone, Verified OTP & Dairy Profile
 */
@Composable
fun SignupStep1Identity(
    phoneInput: String,
    onPhoneChange: (String) -> Unit,
    otpInput: String,
    onOtpChange: (String) -> Unit,
    generatedOtp: String?,
    isOtpVerified: Boolean,
    timerSeconds: Int,
    isVerifying: Boolean,
    dairyName: String,
    onDairyNameChange: (String) -> Unit,
    ownerName: String,
    onOwnerNameChange: (String) -> Unit,
    securityPin: String,
    onPinChange: (String) -> Unit,
    enableBiometric: Boolean,
    onBiometricChange: (Boolean) -> Unit,
    onSendOtp: () -> Unit,
    onVerifyOtp: () -> Unit,
    onNext: () -> Unit,
    onQuickDemo: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("signup_step1_card")
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = RoyalBlueLight,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.AddBusiness, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Step 1: Dairy & Account Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Set up your registered phone & dairy profile",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            HorizontalDivider()

            // 1. Mobile Phone Input & Instant OTP Verify
            Column {
                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = {
                        if (it.length <= 10) onPhoneChange(it.filter { c -> c.isDigit() })
                    },
                    label = { Text("10-Digit Mobile Phone *") },
                    placeholder = { Text("e.g. 9876543210") },
                    leadingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 12.dp, end = 6.dp)
                        ) {
                            Text("+91", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = RoyalBluePrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(modifier = Modifier.height(20.dp).width(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                        }
                    },
                    trailingIcon = {
                        if (isOtpVerified) {
                            Icon(Icons.Default.Verified, contentDescription = "Verified", tint = DairyGreen)
                        } else if (phoneInput.length == 10) {
                            TextButton(onClick = onSendOtp) {
                                Text("Get OTP", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RoyalBluePrimary)
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("signup_phone_input")
                )

                // If OTP was dispatched
                if (generatedOtp != null && !isOtpVerified) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = RoyalBlueLight.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, RoyalBluePrimary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("SMS OTP sent: $generatedOtp", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RoyalBluePrimary)
                                if (timerSeconds > 0) {
                                    Text("${timerSeconds}s", fontSize = 11.sp, color = TextSecondary)
                                } else {
                                    TextButton(onClick = onSendOtp, contentPadding = PaddingValues(0.dp)) {
                                        Text("Resend", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = otpInput,
                                    onValueChange = { if (it.length <= 6) onOtpChange(it.filter { c -> c.isDigit() }) },
                                    label = { Text("6-Digit OTP") },
                                    placeholder = { Text("123456") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = onVerifyOtp,
                                    enabled = otpInput.length == 6,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                                    modifier = Modifier.height(52.dp)
                                ) {
                                    Text("Verify", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 2. Dairy Name & Quick Suggestions
            Column {
                OutlinedTextField(
                    value = dairyName,
                    onValueChange = onDairyNameChange,
                    label = { Text("Dairy / Business Name *") },
                    placeholder = { Text("e.g. Gokul Milk Collection Centre") },
                    leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null, tint = RoyalBluePrimary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("signup_dairy_name_input")
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Gokul Dairy", "Kisan Milk", "Shree Krishna", "Green Valley Farm").forEach { suggestion ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.clickable { onDairyNameChange(suggestion) }
                        ) {
                            Text(
                                text = suggestion,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = RoyalBluePrimary,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // 3. Owner Name & Optional Fast 4-Digit PIN
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = ownerName,
                    onValueChange = onOwnerNameChange,
                    label = { Text("Owner / Incharge Name") },
                    placeholder = { Text("Optional") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = RoyalBluePrimary) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = securityPin,
                    onValueChange = { if (it.length <= 4) onPinChange(it.filter { c -> c.isDigit() }) },
                    label = { Text("4-Digit PIN") },
                    placeholder = { Text("Optional") },
                    leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null, tint = RoyalBluePrimary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // Biometric Toggle if PIN provided
            if (securityPin.length == 4) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enable Fingerprint / Face Unlock", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Switch(
                            checked = enableBiometric,
                            onCheckedChange = onBiometricChange,
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // CTA Button to Next Step
            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("signup_step1_next_btn")
            ) {
                Text("Next: Customize Based on Your Activity", fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/**
 * SIGNUP STEP 2: Activity-Based Customization ("What fits you best")
 */
@Composable
fun SignupStep2ActivityCustomization(
    manageHerd: Boolean,
    onManageHerdChange: (Boolean) -> Unit,
    herdSpecies: String,
    onHerdSpeciesChange: (String) -> Unit,
    procureFromFarmers: Boolean,
    onProcureFarmersChange: (Boolean) -> Unit,
    bulkInward: Boolean,
    onBulkInwardChange: (Boolean) -> Unit,
    sellHouseholds: Boolean,
    onSellHouseholdsChange: (Boolean) -> Unit,
    sellRestaurants: Boolean,
    onSellRestaurantsChange: (Boolean) -> Unit,
    sellTraders: Boolean,
    onSellTradersChange: (Boolean) -> Unit,
    sellCollectionCentre: Boolean,
    onSellCollectionCentreChange: (Boolean) -> Unit,
    sellRetailCounter: Boolean,
    onSellRetailCounterChange: (Boolean) -> Unit,
    ownProcessingPlant: Boolean,
    onOwnProcessingPlantChange: (Boolean) -> Unit,
    selectedModelCode: String,
    onSelectPresetModel: (String) -> Unit,
    onOpenAdvisor: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    var showPresetSelector by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("signup_step2_card")
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = FreshGoldLight,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = FreshGold, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Step 2: Activity-Based Customization",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Choose the exact activities you perform in your dairy",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // 🌟 10-Second Quick Quiz Button
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = RoyalBlueLight.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, RoyalBluePrimary.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenAdvisor)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Need help deciding?", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RoyalBluePrimary)
                        Text("Take our 10-Second Dairy Quiz to auto-configure", fontSize = 10.5.sp, color = TextSecondary)
                    }
                    Text("Try Quiz →", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                }
            }

            HorizontalDivider()

            // ================= SECTION 1: MILK SOURCE & PRODUCTION =================
            Text("1. MILK PRODUCTION & PROCUREMENT:", fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, color = RoyalBluePrimary)

            // Activity 1.1: Own Herd Management
            ActivityOptionCard(
                icon = Icons.Default.Pets,
                title = "Manage Own Cattle Herd / Dairy Farm",
                subtitle = "You own cows/buffaloes and milk them daily",
                isSelected = manageHerd,
                onClick = { onManageHerdChange(!manageHerd) }
            )

            // If Herd is managed, show species selector and compulsory feed notice
            AnimatedVisibility(visible = manageHerd) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = GrassGreen.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, GrassGreen.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth().padding(start = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Select Cattle Species on Farm:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GrassGreen)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = herdSpecies == "COW",
                                onClick = { onHerdSpeciesChange("COW") },
                                label = { Text("🐄 Cow Only", fontSize = 10.5.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = herdSpecies == "BUFFALO",
                                onClick = { onHerdSpeciesChange("BUFFALO") },
                                label = { Text("🐃 Buffalo Only", fontSize = 10.5.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = herdSpecies == "BOTH",
                                onClick = { onHerdSpeciesChange("BOTH") },
                                label = { Text("🥛 Both (Dual)", fontSize = 10.5.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Compulsory notice for feed
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DairyGreenLight,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Compulsory: Cattle Feed, Fodder & Vet Consumables are auto-enabled for your herd.", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1B5E20))
                            }
                        }
                    }
                }
            }

            // Activity 1.2: Procure from Village Farmers
            ActivityOptionCard(
                icon = Icons.Default.Groups,
                title = "Procure Milk from Village Farmers",
                subtitle = "Farmers bring milk morning/evening for FAT/SNF testing",
                isSelected = procureFromFarmers,
                onClick = { onProcureFarmersChange(!procureFromFarmers) }
            )

            // Activity 1.3: Buy Bulk Milk from Traders
            ActivityOptionCard(
                icon = Icons.Default.Science,
                title = "Purchase Bulk Milk Inward (Tankers / Wholesale)",
                subtitle = "Receive large bulk milk shipments from chilling centers",
                isSelected = bulkInward,
                onClick = { onBulkInwardChange(!bulkInward) }
            )

            HorizontalDivider()

            // ================= SECTION 2: SELLING & DISTRIBUTION CHANNELS =================
            Text("2. SELLING & DISTRIBUTION CHANNELS (Select All You Use):", fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, color = RoyalBluePrimary)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ActivityOptionCard(
                    icon = Icons.Default.LocalShipping,
                    title = "🏠 Individual Doorstep Households (B2C)",
                    subtitle = "Morning/evening routes, bottle/packet accounts & monthly bills",
                    isSelected = sellHouseholds,
                    onClick = { onSellHouseholdsChange(!sellHouseholds) }
                )

                ActivityOptionCard(
                    icon = Icons.Default.Restaurant,
                    title = "🍽️ Commercial: Restaurants, Hotels & Cafes (B2B)",
                    subtitle = "Daily commercial bulk supply with weekly/monthly statements",
                    isSelected = sellRestaurants,
                    onClick = { onSellRestaurantsChange(!sellRestaurants) }
                )

                ActivityOptionCard(
                    icon = Icons.Default.Storefront,
                    title = "🏪 Milk Traders & Wholesale Buyers",
                    subtitle = "Bulk trade supply with wholesale pricing",
                    isSelected = sellTraders,
                    onClick = { onSellTradersChange(!sellTraders) }
                )

                ActivityOptionCard(
                    icon = Icons.Default.Factory,
                    title = "🏢 BMC Collection Centre / Dairy Factory Tankers",
                    subtitle = "Deliver milk directly to chilling vat & dairy plants",
                    isSelected = sellCollectionCentre,
                    onClick = { onSellCollectionCentreChange(!sellCollectionCentre) }
                )

                ActivityOptionCard(
                    icon = Icons.Default.PointOfSale,
                    title = "🛍️ Direct Retail Counter / Own Dairy Parlour",
                    subtitle = "Walk-in cash/UPI sales of milk, paneer and ghee",
                    isSelected = sellRetailCounter,
                    onClick = { onSellRetailCounterChange(!sellRetailCounter) }
                )
            }

            HorizontalDivider()

            // ================= SECTION 3: PROCESSING & BYPRODUCTS =================
            Text("3. OWN PROCESSING PLANT & BYPRODUCTS:", fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, color = RoyalBluePrimary)

            ActivityOptionCard(
                icon = Icons.Default.DinnerDining,
                title = "Own Milk Processing & Byproduct Plant",
                subtitle = "Manufacture Paneer, Curd, Ghee, Khoya, Sweets, etc.",
                isSelected = ownProcessingPlant,
                onClick = { onOwnProcessingPlantChange(!ownProcessingPlant) }
            )

            HorizontalDivider()

            // ================= DYNAMIC TAILORED RECOMMENDATION PREVIEW =================
            val tailoredTitle = when (selectedModelCode) {
                "COLLECTION_CENTER" -> "🏢 BMC Milk Collection Centre & Kendra"
                "FARMER" -> "🚚 Doorstep Milk Delivery & Route Vendor"
                "FARMER_WHOLESALE" -> "🐄 Dairy Cattle Farm & Herd Management"
                "TRADER" -> "🏪 Commercial Milk Trader & Supply Hub"
                "PROCESSING_UNIT" -> "🧀 Dairy Processing & Byproduct Plant"
                else -> "🔄 Integrated Dairy Farm & Multi-Channel Enterprise"
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DairyGreenLight,
                border = BorderStroke(1.dp, DairyGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tailored Recommendation For Your Dairy:", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = Color(0xFF1B5E20))
                    }
                    Text(tailoredTitle, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = TextPrimary)
                    Text("Auto-configured with: Herd records, delivery routes, feeds, pricing engine & waste tracking.", fontSize = 11.sp, color = TextSecondary)
                }
            }

            // Or Choose Standard Preset Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { showPresetSelector = !showPresetSelector }) {
                    Text(if (showPresetSelector) "Hide Standard Presets ▲" else "Choose from Standard Presets ▼", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }

            AnimatedVisibility(visible = showPresetSelector) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        Pair("COLLECTION_CENTER", "🏢 BMC Milk Collection Centre"),
                        Pair("FARMER", "🚚 Doorstep Route Delivery Vendor"),
                        Pair("FARMER_WHOLESALE", "🐄 Dairy Cattle Farm & Herd"),
                        Pair("TRADER", "🏪 Commercial Milk Trader & Supply"),
                        Pair("INTEGRATED", "🔄 Integrated Full Enterprise")
                    ).forEach { (code, title) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedModelCode == code) RoyalBlueLight else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, if (selectedModelCode == code) RoyalBluePrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectPresetModel(code) }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(title, fontSize = 12.sp, fontWeight = if (selectedModelCode == code) FontWeight.Bold else FontWeight.Normal)
                                if (selectedModelCode == code) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = RoyalBluePrimary)
                                }
                            }
                        }
                    }
                }
            }

            // Navigation Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(0.8f).height(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Back", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1.2f).height(48.dp).testTag("signup_step2_next_btn")
                ) {
                    Text("Next: Customise Rules", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun ActivityOptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) RoyalBlueLight.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) RoyalBluePrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (isSelected) RoyalBluePrimary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = if (isSelected) Color.White else TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = if (isSelected) RoyalBluePrimary else TextPrimary)
                Text(subtitle, fontSize = 10.5.sp, color = TextSecondary)
            }
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                colors = CheckboxDefaults.colors(checkedColor = RoyalBluePrimary)
            )
        }
    }
}

/**
 * SIGNUP STEP 3: Deep Customisation (Milk Species, Pricing Formula, Baseline Rates, Byproducts & Language)
 */
@Composable
fun SignupStep3CustomiseRules(
    selectedModelCode: String,
    milkType: String,
    onMilkTypeChange: (String) -> Unit,
    pricingMode: String,
    onPricingModeChange: (String) -> Unit,
    cowRate: String,
    onCowRateChange: (String) -> Unit,
    buffaloRate: String,
    onBuffaloRateChange: (String) -> Unit,
    hasPaneer: Boolean,
    onPaneerChange: (Boolean) -> Unit,
    hasCurd: Boolean,
    onCurdChange: (Boolean) -> Unit,
    hasGhee: Boolean,
    onGheeChange: (Boolean) -> Unit,
    hasKhoya: Boolean,
    onKhoyaChange: (Boolean) -> Unit,
    hasCattleFeed: Boolean,
    onCattleFeedChange: (Boolean) -> Unit,
    selectedLanguage: String,
    onLanguageClick: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("signup_step3_card")
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = RoyalBlueLight,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Step 3: Customise Your Rules",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Tailor species, rates, pricing formula & modules",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            HorizontalDivider()

            // 1. Supported Milk Species Selection
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("1. Milk Species / Varieties Handled:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        Triple("COW_ONLY", "🐄 Cow Only", "Cow milk"),
                        Triple("BUFFALO_ONLY", "🐃 Buffalo Only", "Buffalo"),
                        Triple("BOTH", "🥛 Dual (Cow & Buffalo)", "Both")
                    ).forEach { (code, label, _) ->
                        val isSelected = milkType == code
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) RoyalBlueLight else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (isSelected) RoyalBluePrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onMilkTypeChange(code) }
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) RoyalBluePrimary else TextPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // 2. Milk Rate & Pricing Method Selection
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("2. Milk Rate & Pricing Calculation Method:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        Triple("FAT_SNF", "🧪 FAT & SNF Chart", "Standard Dairy testing formula"),
                        Triple("DIRECT", "🏷️ Fixed Flat Rate/L", "Fixed ₹ per liter for each customer"),
                        Triple("FAT_ONLY", "⚖️ FAT-Only Rate", "Formula on FAT % only")
                    ).forEach { (mode, label, _) ->
                        val isSelected = pricingMode == mode
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) DairyGreenLight else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (isSelected) DairyGreen else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onPricingModeChange(mode) }
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF1B5E20) else TextPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // 3. Baseline Rates Configuration
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("3. Baseline Default Milk Rates (₹ / Liter):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = cowRate,
                        onValueChange = onCowRateChange,
                        label = { Text("🐄 Cow Rate (₹/L)") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = RoyalBluePrimary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = buffaloRate,
                        onValueChange = onBuffaloRateChange,
                        label = { Text("🐃 Buffalo Rate (₹/L)") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = RoyalBluePrimary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 4. Products & Byproduct Modules
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("4. Enable Value-Added Dairy Products & Modules:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = hasPaneer,
                        onClick = { onPaneerChange(!hasPaneer) },
                        label = { Text("🧀 Paneer", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = hasCurd,
                        onClick = { onCurdChange(!hasCurd) },
                        label = { Text("🥛 Curd/Chaas", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = hasGhee,
                        onClick = { onGheeChange(!hasGhee) },
                        label = { Text("🏺 Ghee", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = hasKhoya,
                        onClick = { onKhoyaChange(!hasKhoya) },
                        label = { Text("🍬 Khoya", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (hasCattleFeed) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DairyGreenLight,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("🌾 Cattle Feed & Consumables module is active for your cattle herd.", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1B5E20))
                        }
                    }
                }
            }

            // 5. Preferred App Language
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onLanguageClick)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Preferred App Language", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Current: ${getLanguageName(selectedLanguage)}", fontSize = 10.5.sp, color = TextSecondary)
                        }
                    }
                    Text("Change", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RoyalBluePrimary)
                }
            }

            // Navigation Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(0.8f).height(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Back", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1.2f).height(48.dp).testTag("signup_step3_next_btn")
                ) {
                    Text("Next: Review & Launch", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

/**
 * SIGNUP STEP 4: Live Setup Summary & 1-Tap Launch
 */
@Composable
fun SignupStep4SummaryLaunch(
    dairyName: String,
    ownerName: String,
    phone: String,
    selectedModelCode: String,
    milkType: String,
    pricingMode: String,
    cowRate: String,
    buffaloRate: String,
    hasPaneer: Boolean,
    hasCurd: Boolean,
    hasGhee: Boolean,
    hasKhoya: Boolean,
    hasCattleFeed: Boolean,
    securityPin: String,
    enableBiometric: Boolean,
    selectedLanguage: String,
    onBack: () -> Unit,
    onLaunch: () -> Unit
) {
    val modelTitle = when (selectedModelCode) {
        "COLLECTION_CENTER" -> "🏢 BMC Milk Collection Centre"
        "FARMER" -> "🚚 Doorstep Milk Delivery & Route Vendor"
        "FARMER_WHOLESALE" -> "🐄 Dairy Cattle Farm & Herd Management"
        "TRADER" -> "🏪 Milk Trader, Dairy Shop & Byproducts"
        "PROCESSING_UNIT" -> "🧀 Dairy Processing & Byproduct Plant"
        else -> "🔄 Integrated Multi-Feature Dairy Enterprise"
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("signup_step4_card")
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = DairyGreenLight,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Ready to Launch Your Dairy!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Review your tailored workspace settings",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            HorizontalDivider()

            // Summary Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = RoyalBlueLight.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, RoyalBluePrimary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("WORKSPACE CONFIGURATION:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Dairy Name:", fontSize = 12.sp, color = TextSecondary)
                        Text(dairyName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Owner / Incharge:", fontSize = 12.sp, color = TextSecondary)
                        Text(ownerName.ifBlank { "Owner" }, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Mobile Contact:", fontSize = 12.sp, color = TextSecondary)
                        Text("+91 $phone", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Operating Model:", fontSize = 12.sp, color = TextSecondary)
                        Text(modelTitle, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                    }

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Milk Species:", fontSize = 12.sp, color = TextSecondary)
                        Text(when (milkType) {
                            "COW_ONLY" -> "🐄 Cow Only"
                            "BUFFALO_ONLY" -> "🐃 Buffalo Only"
                            else -> "🥛 Cow & Buffalo"
                        }, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Rate Calculation:", fontSize = 12.sp, color = TextSecondary)
                        Text(when (pricingMode) {
                            "FAT_SNF" -> "🧪 FAT & SNF Chart"
                            "FAT_ONLY" -> "⚖️ FAT-Only Rate"
                            else -> "🏷️ Fixed Flat Rate"
                        }, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Baseline Rates:", fontSize = 12.sp, color = TextSecondary)
                        Text("Cow: ₹$cowRate/L • Buffalo: ₹$buffaloRate/L", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }

                    val activeProducts = listOfNotNull(
                        if (hasPaneer) "Paneer" else null,
                        if (hasCurd) "Curd" else null,
                        if (hasGhee) "Ghee" else null,
                        if (hasKhoya) "Khoya" else null,
                        if (hasCattleFeed) "Feed Store" else null
                    )
                    if (activeProducts.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Active Modules:", fontSize = 12.sp, color = TextSecondary)
                            Text(activeProducts.joinToString(", "), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = DairyGreen)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Volume Reconciliation:", fontSize = 12.sp, color = TextSecondary)
                        Text("Active (Wastage & Losses)", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = DangerRed)
                    }

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Fast Login PIN:", fontSize = 12.sp, color = TextSecondary)
                        Text(if (securityPin.length == 4) "Set (4-Digit PIN)" else "None", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                }
            }

            // Big Launch CTA
            Button(
                onClick = onLaunch,
                colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("launch_dairy_workspace_btn")
            ) {
                Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("🚀 Launch My Tailored Dairy Workspace", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Back to Edit Customisation", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }
    }
}

/**
 * 10-Second Smart Dairy Advisor Dialog
 */
@Composable
fun SmartDairyAdvisorDialog(
    onDismiss: () -> Unit,
    onApplyRecommendation: (String) -> Unit
) {
    var quizStep by remember { mutableIntStateOf(1) }
    var answerSource by remember { mutableStateOf<String?>(null) } // "CATTLE", "FARMERS", "BOTH"
    var answerSupply by remember { mutableStateOf<String?>(null) } // "DOORSTEP", "FACTORY_BMC", "SHOP_BYPRODUCTS", "ALL"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = RoyalBluePrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("10-Second Dairy Advisor", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (quizStep == 1) {
                    Text("Question 1 of 2:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                    Text("Where does your milk come from?", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AdvisorQuizOption(
                            icon = Icons.Default.Pets,
                            title = "Own Cows & Buffaloes on Farm",
                            subtitle = "You own the herd, feed & milk them daily",
                            selected = answerSource == "CATTLE",
                            onClick = { answerSource = "CATTLE" }
                        )
                        AdvisorQuizOption(
                            icon = Icons.Default.Groups,
                            title = "Village Dairy Farmers",
                            subtitle = "Farmers bring milk to your collection center morning/evening",
                            selected = answerSource == "FARMERS",
                            onClick = { answerSource = "FARMERS" }
                        )
                        AdvisorQuizOption(
                            icon = Icons.Default.Hub,
                            title = "Both Own Cattle + Village Farmers",
                            subtitle = "Mixed procurement & large dairy operations",
                            selected = answerSource == "BOTH",
                            onClick = { answerSource = "BOTH" }
                        )
                    }
                } else if (quizStep == 2) {
                    Text("Question 2 of 2:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                    Text("How do you distribute or sell the milk?", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AdvisorQuizOption(
                            icon = Icons.Default.LocalShipping,
                            title = "Doorstep Route Delivery to Homes",
                            subtitle = "Morning/evening packets & bottles to households",
                            selected = answerSupply == "DOORSTEP",
                            onClick = { answerSupply = "DOORSTEP" }
                        )
                        AdvisorQuizOption(
                            icon = Icons.Default.Science,
                            title = "BMC Chilling Plant & Dairy Factory Tankers",
                            subtitle = "Fat/SNF testing & dispatching in big tankers",
                            selected = answerSupply == "FACTORY_BMC",
                            onClick = { answerSupply = "FACTORY_BMC" }
                        )
                        AdvisorQuizOption(
                            icon = Icons.Default.Storefront,
                            title = "Retail Dairy Counter & Byproducts",
                            subtitle = "Selling milk, Paneer, Curd, Ghee & Sweets at shop",
                            selected = answerSupply == "SHOP_BYPRODUCTS",
                            onClick = { answerSupply = "SHOP_BYPRODUCTS" }
                        )
                        AdvisorQuizOption(
                            icon = Icons.Default.AllInclusive,
                            title = "All of the above (Full Enterprise)",
                            subtitle = "Multi-channel distribution & commercial supply",
                            selected = answerSupply == "ALL",
                            onClick = { answerSupply = "ALL" }
                        )
                    }
                } else {
                    // Result Step
                    val recommendedCode = when {
                        answerSource == "FARMERS" && answerSupply == "FACTORY_BMC" -> "COLLECTION_CENTER"
                        answerSource == "CATTLE" && answerSupply == "DOORSTEP" -> "FARMER"
                        answerSource == "CATTLE" && answerSupply == "FACTORY_BMC" -> "FARMER_WHOLESALE"
                        answerSupply == "SHOP_BYPRODUCTS" -> "TRADER"
                        else -> "INTEGRATED"
                    }

                    val title = when (recommendedCode) {
                        "COLLECTION_CENTER" -> "🏢 BMC Milk Collection Centre"
                        "FARMER" -> "🚚 Doorstep Milk Delivery & Route Vendor"
                        "FARMER_WHOLESALE" -> "🐄 Dairy Cattle Farm & Herd Management"
                        "TRADER" -> "🏪 Milk Trader, Dairy Shop & Byproducts"
                        else -> "🔄 Integrated Multi-Feature Dairy Enterprise"
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DairyGreenLight,
                        border = BorderStroke(1.dp, DairyGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = DairyGreen)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Recommended Setup For You:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1B5E20))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = TextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("This will auto-configure FAT/SNF testing, rates, and modules suited for your daily workflow.", fontSize = 11.5.sp, color = TextSecondary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (quizStep == 1) {
                Button(
                    onClick = { quizStep = 2 },
                    enabled = answerSource != null,
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary)
                ) {
                    Text("Next Question")
                }
            } else if (quizStep == 2) {
                Button(
                    onClick = { quizStep = 3 },
                    enabled = answerSupply != null,
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary)
                ) {
                    Text("See Recommendation")
                }
            } else {
                val recommendedCode = when {
                    answerSource == "FARMERS" && answerSupply == "FACTORY_BMC" -> "COLLECTION_CENTER"
                    answerSource == "CATTLE" && answerSupply == "DOORSTEP" -> "FARMER"
                    answerSource == "CATTLE" && answerSupply == "FACTORY_BMC" -> "FARMER_WHOLESALE"
                    answerSupply == "SHOP_BYPRODUCTS" -> "TRADER"
                    else -> "INTEGRATED"
                }
                Button(
                    onClick = { onApplyRecommendation(recommendedCode) },
                    colors = ButtonDefaults.buttonColors(containerColor = DairyGreen)
                ) {
                    Text("Apply & Continue")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AdvisorQuizOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) RoyalBlueLight else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, if (selected) RoyalBluePrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = if (selected) RoyalBluePrimary else TextSecondary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (selected) RoyalBluePrimary else TextPrimary)
                Text(subtitle, fontSize = 10.sp, color = TextSecondary)
            }
            if (selected) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/**
 * Language Selection Dialog for Signup
 */
@Composable
fun SignupLanguageModal(
    currentLang: String,
    onSelectLang: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val languages = listOf(
        Pair("en", "English"),
        Pair("hi", "हिंदी (Hindi)"),
        Pair("mr", "मराठी (Marathi)"),
        Pair("gu", "ગુજરાતી (Gujarati)"),
        Pair("ta", "தமிழ் (Tamil)"),
        Pair("te", "తెలుగు (Telugu)"),
        Pair("kn", "ಕನ್ನಡ (Kannada)"),
        Pair("pa", "ਪੰਜਾਬੀ (Punjabi)"),
        Pair("bn", "বাংলা (Bengali)")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select App Language", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                languages.forEach { (code, name) ->
                    val isSelected = currentLang == code
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) RoyalBlueLight else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, if (isSelected) RoyalBluePrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectLang(code) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = RoyalBluePrimary)
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

fun getLanguageName(code: String): String {
    return when (code) {
        "hi" -> "हिंदी (Hindi)"
        "mr" -> "मराठी (Marathi)"
        "gu" -> "ગુજરાતી (Gujarati)"
        "ta" -> "தமிழ் (Tamil)"
        "te" -> "తెలుగు (Telugu)"
        "kn" -> "ಕನ್ನಡ (Kannada)"
        "pa" -> "ਪੰਜਾਬੀ (Punjabi)"
        "bn" -> "বাংলা (Bengali)"
        else -> "English"
    }
}

/**
 * STEP 1 Phone Input Helper
 */
@Composable
fun Step1PhoneInput(
    phoneInput: String,
    onPhoneChange: (String) -> Unit,
    onSendOtp: () -> Unit,
    onSwitchToStaff: () -> Unit,
    onSwitchToPin: () -> Unit,
    onSwitchToAccounts: () -> Unit,
    onQuickDemo: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("auth_step1_card")
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = RoyalBlueLight,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Enter Mobile Number",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "We will send an instant SMS verification code",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            OutlinedTextField(
                value = phoneInput,
                onValueChange = {
                    if (it.length <= 10) {
                        onPhoneChange(it.filter { c -> c.isDigit() })
                    }
                },
                label = { Text("10-Digit Mobile Phone *") },
                placeholder = { Text("e.g. 9876543210") },
                leadingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 12.dp, end = 6.dp)
                    ) {
                        Text("+91", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = RoyalBluePrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .height(20.dp)
                                .width(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("otp_phone_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onSendOtp,
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("send_otp_btn")
            ) {
                Text("Get Verification OTP", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(14.dp))

            Text("OTHER ACCESS OPTIONS:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSwitchToStaff,
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Staff Route", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onSwitchToPin,
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Pin, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PIN Unlock", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            FilledTonalButton(
                onClick = onQuickDemo,
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = FreshGoldLight, contentColor = Color(0xFF7A5800)),
                modifier = Modifier.fillMaxWidth().height(42.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("⚡ Test Instant Demo Dairy (No Phone Required)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * STEP 2: Fast OTP Verification Card
 */
@Composable
fun Step2OtpVerification(
    phone: String,
    otpInput: String,
    generatedOtp: String?,
    timerSeconds: Int,
    isVerifying: Boolean,
    onOtpChange: (String) -> Unit,
    onEditPhone: () -> Unit,
    onResendOtp: () -> Unit,
    onVerifyOtp: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("auth_step2_card")
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = DairyGreenLight,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MarkEmailRead, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Verify OTP Code",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Sent to +91 $phone",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Auto-Fill Helper Chip for easy test
            if (generatedOtp != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = RoyalBlueLight,
                    border = BorderStroke(1.dp, RoyalBluePrimary.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOtpChange(generatedOtp) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simulated SMS OTP: $generatedOtp", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = RoyalBluePrimary)
                        }
                        Text("Tap to Fill", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = RoyalBluePrimary)
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            OutlinedTextField(
                value = otpInput,
                onValueChange = {
                    if (it.length <= 6) {
                        onOtpChange(it.filter { c -> c.isDigit() })
                    }
                },
                label = { Text("6-Digit Verification Code *") },
                placeholder = { Text("123456") },
                leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null, tint = RoyalBluePrimary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("otp_code_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onVerifyOtp,
                enabled = otpInput.length == 6 && !isVerifying,
                colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("verify_otp_btn")
            ) {
                if (isVerifying) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Verify & Continue", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onEditPhone) {
                    Text("Edit Mobile Number", fontSize = 12.sp)
                }

                if (timerSeconds > 0) {
                    Text("Resend code in ${timerSeconds}s", fontSize = 12.sp, color = TextSecondary)
                } else {
                    TextButton(onClick = onResendOtp) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Resend OTP Code", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/**
 * PIN & Biometric Fast Login Card
 */
@Composable
fun PinBiometricLoginCard(
    viewModel: MilkMateViewModel,
    onSwitchToOtp: () -> Unit,
    onSwitchToRegister: () -> Unit
) {
    val session by viewModel.sessionState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pinText by remember { mutableStateOf("") }
    var passwordText by remember { mutableStateOf("") }
    var isPasswordMode by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = FreshGoldLight,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = FreshGold, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Security PIN & Biometric Unlock", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                    Text("Unlock saved dairy session securely", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // Biometric Fingerprint Button
            Button(
                onClick = {
                    if (context is FragmentActivity) {
                        BiometricAuthHelper.authenticate(
                            activity = context,
                            title = "MilkMate Biometric Login",
                            subtitle = "Verify Fingerprint or Device Screen Lock",
                            onSuccess = {
                                viewModel.unlockAppWithBiometrics { }
                            },
                            onError = { err ->
                                errorMessage = err
                            }
                        )
                    } else {
                        errorMessage = "Biometrics requires device support."
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Login with Fingerprint / Screen Lock", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(" OR ENTER PIN / PASSWORD ", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                HorizontalDivider(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (!isPasswordMode) {
                OutlinedTextField(
                    value = pinText,
                    onValueChange = {
                        if (it.length <= 4) pinText = it.filter { c -> c.isDigit() }
                        errorMessage = null
                    },
                    label = { Text("4-Digit Security PIN") },
                    placeholder = { Text("••••") },
                    leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null, tint = RoyalBluePrimary) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        if (pinText.length == 4) {
                            viewModel.loginWithPin(pinText) { ok ->
                                if (!ok) errorMessage = "Invalid PIN. Please try again."
                            }
                        } else {
                            errorMessage = "Please enter full 4-digit PIN"
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DairyGreen)
                ) {
                    Text("Unlock with PIN", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            } else {
                OutlinedTextField(
                    value = passwordText,
                    onValueChange = {
                        passwordText = it
                        errorMessage = null
                    },
                    label = { Text("Master Password") },
                    leadingIcon = { Icon(Icons.Default.Password, contentDescription = null, tint = RoyalBluePrimary) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        if (passwordText.isNotBlank()) {
                            viewModel.loginWithPassword(passwordText) { ok ->
                                if (!ok) errorMessage = "Invalid password."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DairyGreen)
                ) {
                    Text("Unlock with Password", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { isPasswordMode = !isPasswordMode }) {
                    Text(if (isPasswordMode) "Use 4-Digit PIN instead" else "Use Password instead", fontSize = 12.sp)
                }

                TextButton(onClick = onSwitchToOtp) {
                    Text("← Login via Mobile OTP", fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * Staff & Delivery Boy Login Card
 */
@Composable
fun StaffAuthCard(viewModel: MilkMateViewModel) {
    var businessId by remember { mutableStateOf("") }
    var staffId by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Staff & Delivery Boy Login",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = RoyalBluePrimary
            )
            Text(
                text = "Access your assigned delivery routes and daily customer entries using your credentials.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            if (errorMsg != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Text(
                        text = errorMsg ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            OutlinedTextField(
                value = businessId,
                onValueChange = {
                    businessId = it
                    errorMsg = null
                },
                label = { Text("Business Code / ID (e.g. BIZ-XXXX)") },
                placeholder = { Text("Provided by dairy owner") },
                leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null, tint = RoyalBluePrimary) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("staff_biz_id_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = staffId,
                onValueChange = {
                    staffId = it
                    errorMsg = null
                },
                label = { Text("Staff ID or Registered Mobile") },
                placeholder = { Text("e.g. STF-101 or 9876543210") },
                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = RoyalBluePrimary) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("staff_id_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = {
                    pin = it
                    errorMsg = null
                },
                label = { Text("4-Digit Security PIN") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = RoyalBluePrimary) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("staff_pin_input")
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (businessId.isNotBlank() && staffId.isNotBlank() && pin.isNotBlank()) {
                        viewModel.loginStaff(businessId, staffId, pin) { success, err ->
                            if (!success) {
                                errorMsg = err
                            }
                        }
                    } else {
                        errorMsg = "Please fill in Business ID, Staff ID, and PIN"
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("login_as_staff_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Login, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Login as Staff", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Saved Dairies Card (Multi-Dairy Switcher)
 */
@Composable
fun OwnerLoginCard(
    viewModel: MilkMateViewModel,
    onSwitchToRegister: () -> Unit
) {
    val existingBusinesses by viewModel.existingBusinesses.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadExistingBusinesses()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Saved Dairy Accounts",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = RoyalBluePrimary
            )
            Text(
                text = "Switch between multiple saved dairy workspaces on this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            if (existingBusinesses.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No local dairy accounts found.", fontSize = 13.sp, color = TextSecondary)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    existingBusinesses.forEach { biz ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.loginOwnerDirect(biz.id) },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Storefront, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(biz.businessName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("${biz.ownerName} • ${biz.phone}", fontSize = 11.sp, color = TextSecondary)
                                    }
                                }
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
