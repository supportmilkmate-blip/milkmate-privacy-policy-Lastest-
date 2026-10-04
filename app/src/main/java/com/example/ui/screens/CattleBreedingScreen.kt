package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.*
import com.example.ui.MilkMateViewModel
import com.example.ui.screens.cattle.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CattleBreedingScreen(
    viewModel: MilkMateViewModel,
    onBack: () -> Unit
) {
    var showSpeedDialModal by remember { mutableStateOf(false) }
    var showAddCattleDialog by remember { mutableStateOf(false) }
    var showAddMilkingDialog by remember { mutableStateOf(false) }
    var showAddBreedingDialog by remember { mutableStateOf(false) }
    var showAddDewormingDialog by remember { mutableStateOf(false) }
    var showAddVaccinationDialog by remember { mutableStateOf(false) }
    var showAddTreatmentDialog by remember { mutableStateOf(false) }
    var showAddObservationDialog by remember { mutableStateOf(false) }
    var showAddWeightDialog by remember { mutableStateOf(false) }
    var showAddCmtDialog by remember { mutableStateOf(false) }
    var showAddBcsDialog by remember { mutableStateOf(false) }

    var selectedCattleForDetail by remember { mutableStateOf<CattleEntity?>(null) }
    var preselectedCattleIdForAction by remember { mutableStateOf<String?>(null) }

    // 0: Herd, 1: Milking, 2: Breeding, 3: Deworming, 4: Vaccines, 5: Treatments, 6: Observations, 7: Weights, 8: CMT, 9: BCS
    var mainViewTab by remember { mutableIntStateOf(0) }
    var selectedFilterStage by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }

    val cattleList by viewModel.cattleList.collectAsStateWithLifecycle()
    val allMilking by viewModel.allMilkingRecords.collectAsStateWithLifecycle()
    val allBreeding by viewModel.allBreedingRecords.collectAsStateWithLifecycle()
    val allDeworming by viewModel.allDewormingRecords.collectAsStateWithLifecycle()
    val allVaccinations by viewModel.allVaccinationRecords.collectAsStateWithLifecycle()
    val allTreatments by viewModel.allTreatmentRecords.collectAsStateWithLifecycle()
    val allObservations by viewModel.allFarmObservations.collectAsStateWithLifecycle()
    val allWeights by viewModel.allCattleWeights.collectAsStateWithLifecycle()
    val allCmtRecords by viewModel.allCmtRecords.collectAsStateWithLifecycle()
    val allBcsRecords by viewModel.allBcsRecords.collectAsStateWithLifecycle()
    val business by viewModel.currentBusiness.collectAsStateWithLifecycle()

    val supportedMilkTypes = business?.supportedMilkTypes ?: "BOTH"

    // Strictly scope cattle to supported milk type (Cow Only vs Buffalo Only vs Both)
    val scopedCattleList = remember(cattleList, supportedMilkTypes) {
        when (supportedMilkTypes) {
            "COW_ONLY" -> cattleList.filter { it.type.equals("COW", ignoreCase = true) }
            "BUFFALO_ONLY" -> cattleList.filter { it.type.equals("BUFFALO", ignoreCase = true) }
            else -> cattleList
        }
    }
    val scopedCattleIds = remember(scopedCattleList) { scopedCattleList.map { it.id }.toSet() }

    val scopedMilking = remember(allMilking, scopedCattleIds) { allMilking.filter { it.cattleId in scopedCattleIds } }
    val scopedBreeding = remember(allBreeding, scopedCattleIds) { allBreeding.filter { it.cattleId in scopedCattleIds } }
    val scopedDeworming = remember(allDeworming, scopedCattleIds) { allDeworming.filter { it.cattleId in scopedCattleIds } }
    val scopedVaccinations = remember(allVaccinations, scopedCattleIds) { allVaccinations.filter { it.cattleId in scopedCattleIds } }
    val scopedTreatments = remember(allTreatments, scopedCattleIds) { allTreatments.filter { it.cattleId in scopedCattleIds } }
    val scopedObservations = remember(allObservations, scopedCattleIds) { allObservations.filter { it.cattleId in scopedCattleIds || it.cattleId.isBlank() } }
    val scopedWeights = remember(allWeights, scopedCattleIds) { allWeights.filter { it.cattleId in scopedCattleIds } }
    val scopedCmtRecords = remember(allCmtRecords, scopedCattleIds) { allCmtRecords.filter { it.cattleId in scopedCattleIds } }
    val scopedBcsRecords = remember(allBcsRecords, scopedCattleIds) { allBcsRecords.filter { it.cattleId in scopedCattleIds } }

    val totalCattle = scopedCattleList.size
    val lactatingCount = scopedCattleList.count { it.lactationStage == "LACTATING" }
    val pregnantCount = scopedCattleList.count { it.breedingStatus == "CONFIRMED_PREGNANT" }
    val inseminatedCount = scopedCattleList.count { it.breedingStatus == "INSEMINATED" }
    val totalDailyYield = scopedCattleList.filter { it.lactationStage == "LACTATING" }.sumOf { it.dailyYieldLiters }

    // Seed initial cattle if empty
    LaunchedEffect(business?.id) {
        business?.id?.let { bId ->
            if (cattleList.isEmpty()) {
                viewModel.repository.seedInitialCattle(bId, supportedMilkTypes)
            }
        }
    }

    val filteredList = scopedCattleList.filter { cattle ->
        val matchesSearch = cattle.tagNumber.contains(searchQuery, ignoreCase = true) ||
                cattle.name.contains(searchQuery, ignoreCase = true) ||
                cattle.breed.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (selectedFilterStage) {
            "LACTATING" -> cattle.lactationStage == "LACTATING"
            "PREGNANT" -> cattle.breedingStatus == "CONFIRMED_PREGNANT"
            "INSEMINATED" -> cattle.breedingStatus == "INSEMINATED"
            "DRY" -> cattle.lactationStage == "DRY" || cattle.lactationStage == "PREGNANT_DRY"
            "CALF_HEIFER" -> cattle.lactationStage == "CALF" || cattle.lactationStage == "HEIFER"
            else -> true
        }

        matchesSearch && matchesFilter
    }

    val now = System.currentTimeMillis()
    val dayMillis = 86400000L
    val upcomingCalvings = scopedCattleList.filter {
        it.expectedCalvingDate > 0L && it.expectedCalvingDate >= now && (it.expectedCalvingDate - now) <= (60 * dayMillis)
    }.sortedBy { it.expectedCalvingDate }

    val activeMastitisCount = scopedCmtRecords.count { it.overallDiagnosis == "CLINICAL_MASTITIS" }
    val upcomingDewormingCount = scopedDeworming.count { it.nextDueDate in now..(now + 30 * dayMillis) }
    val upcomingVaccineCount = scopedVaccinations.count { it.nextDueDate in now..(now + 60 * dayMillis) }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("cattle_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = when (supportedMilkTypes) {
                                "COW_ONLY" -> "🐄 Cow Farm & Herd Cockpit"
                                "BUFFALO_ONLY" -> "🐃 Buffalo Farm & Herd Cockpit"
                                else -> "🚜 Dairy Farm & Herd Cockpit"
                            },
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Milking • Breeding • Deworming • Vaccines • Rx • Observations",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showSpeedDialModal = true },
                containerColor = GrassGreen,
                contentColor = Color.White,
                modifier = Modifier.testTag("farm_action_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Farm Actions")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Farm Action", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Herd Cockpit Summary Banner
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = OceanMidnight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🚜 Dairy Herd Cockpit", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (activeMastitisCount > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = DangerRed.copy(alpha = 0.25f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed)
                                    ) {
                                        Text(
                                            text = "⚠️ $activeMastitisCount Mastitis",
                                            color = DangerRed,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = GrassGreen.copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, GrassGreen)
                                ) {
                                    Text(
                                        text = "$totalCattle Animals",
                                        color = GrassGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Lactating", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                                val lactatingAnimalLabel = when (supportedMilkTypes) {
                                    "COW_ONLY" -> "Cows"
                                    "BUFFALO_ONLY" -> "Buffaloes"
                                    else -> "Milking"
                                }
                                Text(
                                    text = "$lactatingCount $lactatingAnimalLabel",
                                    color = FreshGold,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Column {
                                Text("Daily Capacity", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                                Text(
                                    text = "${String.format(Locale.getDefault(), "%.1f", totalDailyYield)} L/d",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Column {
                                Text("In-Calf / A.I.", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                                Text(
                                    text = "$pregnantCount In-Calf",
                                    color = GrassGreen,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("A.I. Inseminated", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                                Text(
                                    text = "$inseminatedCount A.I.",
                                    color = RoyalBlueSecondary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Calving Alerts Card
            if (upcomingCalvings.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = FreshGoldLight),
                        border = androidx.compose.foundation.BorderStroke(1.dp, FreshGold.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = WarmHoney, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "⏰ Calving Due Alerts (${upcomingCalvings.size} Animals)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WarmHoney
                                )
                            }

                            upcomingCalvings.forEach { c ->
                                val daysLeft = ((c.expectedCalvingDate - now) / dayMillis).coerceAtLeast(0)
                                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                val dStr = sdf.format(Date(c.expectedCalvingDate))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "• #${c.tagNumber} (${c.name.ifBlank { c.type }})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Due in $daysLeft days ($dStr)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DangerRed
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Scrollable Main Navigation View Tabs (10 Sections)
            item {
                val tabLabels = listOf(
                    "Herd (${scopedCattleList.size})",
                    "Milking (${scopedMilking.size})",
                    "Breeding & AI (${scopedBreeding.size})",
                    "Deworming (${scopedDeworming.size})",
                    "Vaccines (${scopedVaccinations.size})",
                    "Treatments (${scopedTreatments.size})",
                    "Observations (${scopedObservations.size})",
                    "Weights (${scopedWeights.size})",
                    "CMT Udder (${scopedCmtRecords.size})",
                    "BCS (${scopedBcsRecords.size})"
                )

                ScrollableTabRow(
                    selectedTabIndex = mainViewTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    contentColor = RoyalBluePrimary,
                    edgePadding = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabLabels.forEachIndexed { idx, title ->
                        Tab(
                            selected = mainViewTab == idx,
                            onClick = { mainViewTab = idx },
                            text = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }

            // View 0: Herd Records
            if (mainViewTab == 0) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search by Tag Number, Name, or Breed") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("cattle_search_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            "ALL" to "All Herd",
                            "LACTATING" to "🥛 Lactating",
                            "PREGNANT" to "🤰 In-Calf",
                            "INSEMINATED" to "🧪 Inseminated A.I.",
                            "DRY" to "🌾 Dry Period",
                            "CALF_HEIFER" to "🌱 Calf & Heifer"
                        ).forEach { (code, label) ->
                            val isSel = selectedFilterStage == code
                            item {
                                FilterChip(
                                    selected = isSel,
                                    onClick = { selectedFilterStage = code },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }
                    }
                }

                if (filteredList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No cattle records match this filter. Tap 'Farm Action' to add an animal.", color = TextSecondary)
                        }
                    }
                } else {
                    items(filteredList) { cattle ->
                        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        val calvingStr = if (cattle.expectedCalvingDate > 0) sdf.format(Date(cattle.expectedCalvingDate)) else "N/A"

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cattle_card_${cattle.tagNumber}")
                                .clickable { selectedCattleForDetail = cattle }
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (cattle.type == "BUFFALO") DeepOceanNavy else GrassGreen.copy(alpha = 0.15f),
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(if (cattle.type == "BUFFALO") "🐃" else "🐄", fontSize = 20.sp)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "Tag #${cattle.tagNumber}",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 15.sp,
                                                    color = TextPrimary
                                                )
                                                if (cattle.name.isNotBlank()) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "(${cattle.name})",
                                                        fontSize = 12.sp,
                                                        color = RoyalBluePrimary,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${cattle.breed} • ${cattle.lactationStage.replace("_", " ")}",
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = when (cattle.breedingStatus) {
                                            "CONFIRMED_PREGNANT" -> GrassGreen.copy(alpha = 0.15f)
                                            "INSEMINATED" -> RoyalBlueLight
                                            else -> FreshGoldLight
                                        }
                                    ) {
                                        Text(
                                            text = cattle.breedingStatus.replace("_", " "),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (cattle.breedingStatus) {
                                                "CONFIRMED_PREGNANT" -> GrassGreen
                                                "INSEMINATED" -> RoyalBluePrimary
                                                else -> WarmHoney
                                            },
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Capacity: ${cattle.dailyYieldLiters} L/d", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                                    Text("Weight: ${if (cattle.currentWeightKg > 0) "${cattle.currentWeightKg.toInt()}kg" else "--"}", fontSize = 11.sp, color = TextSecondary)
                                    Text("BCS: ${cattle.latestBcs}", fontSize = 11.sp, color = WarmHoney, fontWeight = FontWeight.Bold)
                                    if (cattle.expectedCalvingDate > 0) {
                                        Text("Calving: $calvingStr", fontSize = 11.sp, color = DangerRed, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // View 1: Milking Logs
            if (mainViewTab == 1) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Individual Cow & Buffalo Milking Logs", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                        Button(
                            onClick = { showAddMilkingDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Log Milking", fontSize = 11.sp)
                        }
                    }
                }

                if (scopedMilking.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No milking sessions logged yet.", color = TextSecondary)
                        }
                    }
                } else {
                    items(scopedMilking) { m ->
                        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("#${m.cattleTag}", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = TextPrimary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (m.shift == "MORNING") FreshGoldLight else RoyalBlueLight
                                        ) {
                                            Text(
                                                text = if (m.shift == "MORNING") "🌅 MORNING" else "🌙 EVENING",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (m.shift == "MORNING") WarmHoney else RoyalBluePrimary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("FAT: ${m.fat}% • SNF: ${m.snf}% • Date: ${sdf.format(Date(m.dateEpochMidnight))}", fontSize = 11.sp, color = TextSecondary)
                                }
                                Text("${m.quantityLiters} L", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = RoyalBluePrimary)
                            }
                        }
                    }
                }
            }

            // View 2: Breeding & AI Records
            if (mainViewTab == 2) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Artificial Insemination & Calving History", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                        Button(
                            onClick = { showAddBreedingDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New AI/PD", fontSize = 11.sp)
                        }
                    }
                }

                if (scopedBreeding.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No breeding records logged yet.", color = TextSecondary)
                        }
                    }
                } else {
                    items(scopedBreeding) { b ->
                        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("#${b.cattleTag}", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = TextPrimary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(shape = RoundedCornerShape(4.dp), color = RoyalBlueLight) {
                                            Text(
                                                text = b.eventType.replace("_", " "),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = RoyalBluePrimary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(sdf.format(Date(b.date)), fontSize = 11.sp, color = TextSecondary)
                                }

                                if (b.bullIdOrName.isNotBlank()) {
                                    Text("Bull / Semen: ${b.bullIdOrName} (${b.inseminationType})", fontSize = 11.sp, color = TextPrimary)
                                }
                                if (b.pdStatus != "PENDING") {
                                    Text("PD Status: ${b.pdStatus}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (b.pdStatus.contains("POSITIVE")) GrassGreen else DangerRed)
                                }
                                if (b.expectedCalvingDate > 0) {
                                    Text("Expected Calving: ${sdf.format(Date(b.expectedCalvingDate))}", fontSize = 11.sp, color = WarmHoney, fontWeight = FontWeight.Bold)
                                }
                                if (b.notes.isNotBlank()) {
                                    Text("Notes: ${b.notes}", fontSize = 10.sp, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }

            // View 3: Deworming Schedule
            if (mainViewTab == 3) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Deworming Protocol & Schedules", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                        Button(
                            onClick = { showAddDewormingDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = FreshGold),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Dose", fontSize = 11.sp)
                        }
                    }
                }

                if (scopedDeworming.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No deworming records logged yet.", color = TextSecondary)
                        }
                    }
                } else {
                    items(scopedDeworming) { d ->
                        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("#${d.cattleTag} • ${d.dewormerSalt}", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = TextPrimary)
                                    Text(sdf.format(Date(d.date)), fontSize = 11.sp, color = TextSecondary)
                                }
                                Text("Dose: ${d.dose} • By: ${d.administeredBy} • Cost: ₹${d.cost.toInt()}", fontSize = 11.sp, color = TextSecondary)
                                Surface(shape = RoundedCornerShape(4.dp), color = FreshGoldLight) {
                                    Text(
                                        text = "⏰ Next Due: ${sdf.format(Date(d.nextDueDate))} (Repeat every ${d.repeatAfterDays} days)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WarmHoney,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // View 4: Vaccination History
            if (mainViewTab == 4) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("NADCP & Herd Vaccines (FMD, HS, BQ)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                        Button(
                            onClick = { showAddVaccinationDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlueSecondary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Log Vaccine", fontSize = 11.sp)
                        }
                    }
                }

                if (scopedVaccinations.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No vaccination records logged yet.", color = TextSecondary)
                        }
                    }
                } else {
                    items(scopedVaccinations) { v ->
                        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("#${v.cattleTag} • 💉 ${v.vaccineName}", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = RoyalBluePrimary)
                                    Text(sdf.format(Date(v.date)), fontSize = 11.sp, color = TextSecondary)
                                }
                                Text("Manufacturer: ${v.manufacturer} • Batch #${v.batchNo.ifBlank { "N/A" }}", fontSize = 11.sp, color = TextSecondary)
                                if (v.nextDueDate > 0) {
                                    Surface(shape = RoundedCornerShape(4.dp), color = GrassGreen.copy(alpha = 0.15f)) {
                                        Text(
                                            text = "🛡️ Next Booster Due: ${sdf.format(Date(v.nextDueDate))}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GrassGreen,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // View 5: Treatment Records
            if (mainViewTab == 5) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Clinical Veterinary Treatments & Prescriptions", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                        Button(
                            onClick = { showAddTreatmentDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Rx", fontSize = 11.sp)
                        }
                    }
                }

                if (scopedTreatments.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No medical treatments logged yet.", color = TextSecondary)
                        }
                    }
                } else {
                    items(scopedTreatments) { t ->
                        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("#${t.cattleTag} • 🩺 ${t.diseaseName}", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = DangerRed)
                                    Text(sdf.format(Date(t.checkupDate)), fontSize = 11.sp, color = TextSecondary)
                                }
                                Text("Rx: ${t.medicationName} (${t.dosage})", fontSize = 11.sp, color = TextPrimary)
                                Text("Duration: ${t.durationDays}d • Cost: ₹${t.treatmentCost.toInt()} • Doctor: ${t.performedBy}", fontSize = 11.sp, color = TextSecondary)
                                if (t.milkWithdrawalDays > 0) {
                                    Surface(shape = RoundedCornerShape(4.dp), color = DangerRed.copy(alpha = 0.15f)) {
                                        Text(
                                            text = "⚠️ Milk Withdrawal Alert: ${t.milkWithdrawalDays} days",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DangerRed,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // View 6: Farm Observations
            if (mainViewTab == 6) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Farm Observations, Heat Signs & Activity Logs", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                        Button(
                            onClick = { showAddObservationDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = WarmHoney),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Log Note", fontSize = 11.sp)
                        }
                    }
                }

                if (scopedObservations.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No observations logged yet.", color = TextSecondary)
                        }
                    }
                } else {
                    items(scopedObservations) { o ->
                        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = if (o.hasAlert) androidx.compose.foundation.BorderStroke(1.dp, DangerRed.copy(alpha = 0.4f)) else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${if (o.cattleTag.isNotBlank()) "#${o.cattleTag}" else "Whole Farm"} • 👁️ ${o.activityType.replace("_", " ")}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = if (o.hasAlert) DangerRed else TextPrimary
                                    )
                                    Text(sdf.format(Date(o.date)), fontSize = 11.sp, color = TextSecondary)
                                }
                                Text(o.description, fontSize = 11.sp, color = TextSecondary)
                                Text("Logged by: ${o.loggedBy}", fontSize = 10.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }

            // View 7: Weight Ledger
            if (mainViewTab == 7) {
                if (scopedWeights.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No weight records found. Tap 'Farm Action' -> 'Add Weight'.", color = TextSecondary)
                        }
                    }
                } else {
                    items(scopedWeights) { w ->
                        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("#${w.tagNumber}", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = TextPrimary)
                                    Text(
                                        text = if (w.method == "GIRTH_CALCULATION") "Girth: ${w.heartGirthCm}cm • Length: ${w.bodyLengthCm}cm" else "Direct Scale Weighing",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                    Text(sdf.format(Date(w.date)), fontSize = 10.sp, color = TextSecondary)
                                }
                                Text("${String.format(Locale.getDefault(), "%.1f", w.weightKg)} kg", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = RoyalBluePrimary)
                            }
                        }
                    }
                }
            }

            // View 8: CMT Health
            if (mainViewTab == 8) {
                if (scopedCmtRecords.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No CMT paddle records found.", color = TextSecondary)
                        }
                    }
                } else {
                    items(scopedCmtRecords) { cmt ->
                        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        val isMastitis = cmt.overallDiagnosis != "HEALTHY" && cmt.overallDiagnosis != "NORMAL"
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isMastitis) DangerRed.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                            ),
                            border = if (isMastitis) androidx.compose.foundation.BorderStroke(1.dp, DangerRed.copy(alpha = 0.4f)) else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("#${cmt.tagNumber} • CMT Paddle Test", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = TextPrimary)
                                    Surface(shape = RoundedCornerShape(4.dp), color = if (isMastitis) DangerRed else GrassGreen) {
                                        Text(
                                            text = cmt.overallDiagnosis.replace("_", " "),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    QuarterBadge("LF: ${cmt.quarterLf}", cmt.quarterLf, Modifier.weight(1f))
                                    QuarterBadge("RF: ${cmt.quarterRf}", cmt.quarterRf, Modifier.weight(1f))
                                    QuarterBadge("LH: ${cmt.quarterLh}", cmt.quarterLh, Modifier.weight(1f))
                                    QuarterBadge("RH: ${cmt.quarterRh}", cmt.quarterRh, Modifier.weight(1f))
                                }
                                Text("Tested on ${sdf.format(Date(cmt.date))} by ${cmt.testerName}", fontSize = 10.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }

            // View 9: BCS Condition
            if (mainViewTab == 9) {
                if (scopedBcsRecords.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No BCS scores recorded.", color = TextSecondary)
                        }
                    }
                } else {
                    items(scopedBcsRecords) { bcs ->
                        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("#${bcs.tagNumber} • Score ${bcs.score}", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = TextPrimary)
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = when (bcs.category) {
                                            "IDEAL" -> GrassGreen
                                            "THIN" -> FreshGold
                                            else -> DangerRed
                                        }
                                    ) {
                                        Text(
                                            text = bcs.category,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                if (bcs.nutritionAdvice.isNotBlank()) {
                                    Text("Nutrition: ${bcs.nutritionAdvice}", fontSize = 11.sp, color = TextPrimary)
                                }
                                Text("Scored on ${sdf.format(Date(bcs.date))}", fontSize = 10.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }

    // Floating Speed Dial Action Modal
    if (showSpeedDialModal) {
        FarmActionSpeedDialModal(
            onDismiss = { showSpeedDialModal = false },
            onAddCattle = { showAddCattleDialog = true },
            onRecordMilking = { showAddMilkingDialog = true },
            onBreedingAction = { showAddBreedingDialog = true },
            onDewormingAction = { showAddDewormingDialog = true },
            onVaccinationAction = { showAddVaccinationDialog = true },
            onTreatmentAction = { showAddTreatmentDialog = true },
            onObservationAction = { showAddObservationDialog = true },
            onAddWeight = { showAddWeightDialog = true },
            onQuickBcs = { showAddBcsDialog = true },
            onCmtRecord = { showAddCmtDialog = true },
            supportedMilkTypes = supportedMilkTypes
        )
    }

    // Modal Dialogs
    if (showAddCattleDialog) {
        AddCattleDialog(
            businessId = business?.id ?: "",
            supportedMilkTypes = supportedMilkTypes,
            onDismiss = { showAddCattleDialog = false },
            onSave = { c ->
                viewModel.saveCattle(c)
                showAddCattleDialog = false
            }
        )
    }

    if (showAddMilkingDialog) {
        CattleMilkingDialog(
            cattleList = scopedCattleList,
            preselectedCattleId = preselectedCattleIdForAction,
            onDismiss = { showAddMilkingDialog = false },
            onSave = { record ->
                viewModel.saveMilkingRecord(record)
                showAddMilkingDialog = false
            }
        )
    }

    if (showAddBreedingDialog) {
        CattleBreedingDialog(
            cattleList = scopedCattleList,
            preselectedCattleId = preselectedCattleIdForAction,
            onDismiss = { showAddBreedingDialog = false },
            onSave = { record ->
                viewModel.saveBreedingRecord(record)
                showAddBreedingDialog = false
            }
        )
    }

    if (showAddDewormingDialog) {
        CattleDewormingDialog(
            cattleList = scopedCattleList,
            preselectedCattleId = preselectedCattleIdForAction,
            onDismiss = { showAddDewormingDialog = false },
            onSave = { record ->
                viewModel.saveDewormingRecord(record)
                showAddDewormingDialog = false
            }
        )
    }

    if (showAddVaccinationDialog) {
        CattleVaccinationDialog(
            cattleList = scopedCattleList,
            preselectedCattleId = preselectedCattleIdForAction,
            onDismiss = { showAddVaccinationDialog = false },
            onSave = { record ->
                viewModel.saveVaccinationRecord(record)
                showAddVaccinationDialog = false
            }
        )
    }

    if (showAddTreatmentDialog) {
        CattleTreatmentDialog(
            cattleList = scopedCattleList,
            preselectedCattleId = preselectedCattleIdForAction,
            onDismiss = { showAddTreatmentDialog = false },
            onSave = { record ->
                viewModel.saveTreatmentRecord(record)
                showAddTreatmentDialog = false
            }
        )
    }

    if (showAddObservationDialog) {
        FarmObservationDialog(
            cattleList = scopedCattleList,
            preselectedCattleId = preselectedCattleIdForAction,
            onDismiss = { showAddObservationDialog = false },
            onSave = { record ->
                viewModel.saveFarmObservation(record)
                showAddObservationDialog = false
            }
        )
    }

    if (showAddWeightDialog) {
        CattleWeightDialog(
            businessId = business?.id ?: "",
            cattleList = scopedCattleList,
            preselectedCattleId = preselectedCattleIdForAction,
            onDismiss = { showAddWeightDialog = false },
            onSaveWeight = { w ->
                viewModel.saveCattleWeight(w)
                showAddWeightDialog = false
            }
        )
    }

    if (showAddCmtDialog) {
        CattleCmtDialog(
            businessId = business?.id ?: "",
            cattleList = scopedCattleList,
            preselectedCattleId = preselectedCattleIdForAction,
            onDismiss = { showAddCmtDialog = false },
            onSaveCmt = { cmt ->
                viewModel.saveCmtRecord(cmt)
                showAddCmtDialog = false
            },
            supportedMilkTypes = supportedMilkTypes
        )
    }

    if (showAddBcsDialog) {
        CattleBcsDialog(
            businessId = business?.id ?: "",
            cattleList = scopedCattleList,
            preselectedCattleId = preselectedCattleIdForAction,
            onDismiss = { showAddBcsDialog = false },
            onSaveBcs = { bcs ->
                viewModel.saveBcsRecord(bcs)
                showAddBcsDialog = false
            }
        )
    }

    // Detail Sheet for Selected Cattle
    selectedCattleForDetail?.let { cattle ->
        val cattleWeights = remember(scopedWeights, cattle.id) { scopedWeights.filter { it.cattleId == cattle.id } }
        val cattleCmt = remember(scopedCmtRecords, cattle.id) { scopedCmtRecords.filter { it.cattleId == cattle.id } }
        val cattleBcs = remember(scopedBcsRecords, cattle.id) { scopedBcsRecords.filter { it.cattleId == cattle.id } }
        val cattleBreeding = remember(scopedBreeding, cattle.id) { scopedBreeding.filter { it.cattleId == cattle.id } }
        val cattleMilking = remember(scopedMilking, cattle.id) { scopedMilking.filter { it.cattleId == cattle.id } }
        val cattleDeworming = remember(scopedDeworming, cattle.id) { scopedDeworming.filter { it.cattleId == cattle.id } }
        val cattleVac = remember(scopedVaccinations, cattle.id) { scopedVaccinations.filter { it.cattleId == cattle.id } }
        val cattleTreat = remember(scopedTreatments, cattle.id) { scopedTreatments.filter { it.cattleId == cattle.id } }
        val cattleObs = remember(scopedObservations, cattle.id) { scopedObservations.filter { it.cattleId == cattle.id } }

        CattleDetailSheet(
            cattle = cattle,
            breedingHistory = cattleBreeding,
            milkingHistory = cattleMilking,
            dewormingHistory = cattleDeworming,
            vaccinationHistory = cattleVac,
            treatmentHistory = cattleTreat,
            observationHistory = cattleObs,
            weightHistory = cattleWeights,
            cmtHistory = cattleCmt,
            bcsHistory = cattleBcs,
            onDismiss = { selectedCattleForDetail = null },
            onAddBreeding = {
                preselectedCattleIdForAction = cattle.id
                showAddBreedingDialog = true
            },
            onAddMilking = {
                preselectedCattleIdForAction = cattle.id
                showAddMilkingDialog = true
            },
            onAddDeworming = {
                preselectedCattleIdForAction = cattle.id
                showAddDewormingDialog = true
            },
            onAddVaccination = {
                preselectedCattleIdForAction = cattle.id
                showAddVaccinationDialog = true
            },
            onAddTreatment = {
                preselectedCattleIdForAction = cattle.id
                showAddTreatmentDialog = true
            },
            onAddObservation = {
                preselectedCattleIdForAction = cattle.id
                showAddObservationDialog = true
            },
            onAddWeight = {
                preselectedCattleIdForAction = cattle.id
                showAddWeightDialog = true
            },
            onAddCmt = {
                preselectedCattleIdForAction = cattle.id
                showAddCmtDialog = true
            },
            onAddBcs = {
                preselectedCattleIdForAction = cattle.id
                showAddBcsDialog = true
            },
            onDeleteCattle = {
                viewModel.deleteCattle(cattle.id)
                selectedCattleForDetail = null
            }
        )
    }
}

@Composable
private fun QuarterBadge(label: String, score: String, modifier: Modifier = Modifier) {
    val isAlert = score in listOf("TRACE", "+", "++", "+++")
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (isAlert) DangerRed.copy(alpha = 0.15f) else GrassGreen.copy(alpha = 0.12f),
        modifier = modifier
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 4.dp)) {
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isAlert) DangerRed else GrassGreen
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCattleDialog(
    businessId: String,
    supportedMilkTypes: String = "BOTH",
    onDismiss: () -> Unit,
    onSave: (CattleEntity) -> Unit
) {
    val defaultType = when (supportedMilkTypes) {
        "BUFFALO_ONLY" -> "BUFFALO"
        else -> "COW"
    }
    var type by remember { mutableStateOf(defaultType) }
    var tagNumber by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var breed by remember { mutableStateOf(if (defaultType == "BUFFALO") "Murrah" else "Holstein Friesian Cross") }
    var lactationStage by remember { mutableStateOf("LACTATING") }
    var dailyYieldText by remember { mutableStateOf(if (defaultType == "BUFFALO") "10.0" else "15.0") }
    var breedingStatus by remember { mutableStateOf("OPEN") }
    var sireName by remember { mutableStateOf("") }
    var damName by remember { mutableStateOf("") }
    var inseminationDaysAgo by remember { mutableStateOf("") }
    var ageMonthsText by remember { mutableStateOf("36") }
    var initialWeightText by remember { mutableStateOf(if (defaultType == "BUFFALO") "550" else "450") }
    var initialBcsText by remember { mutableStateOf("3.25") }
    var healthNotes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (supportedMilkTypes) {
                    "COW_ONLY" -> "Register New Cow"
                    "BUFFALO_ONLY" -> "Register New Buffalo"
                    else -> "Register New Cattle"
                },
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Type selector if BOTH
                if (supportedMilkTypes == "BOTH") {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = type == "COW",
                                onClick = {
                                    type = "COW"
                                    breed = "Holstein Friesian Cross"
                                    dailyYieldText = "15.0"
                                    initialWeightText = "450"
                                },
                                label = { Text("🐄 Cow") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = type == "BUFFALO",
                                onClick = {
                                    type = "BUFFALO"
                                    breed = "Murrah"
                                    dailyYieldText = "10.0"
                                    initialWeightText = "550"
                                },
                                label = { Text("🐃 Buffalo") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = tagNumber,
                        onValueChange = { tagNumber = it },
                        label = { Text("Ear Tag Number *") },
                        modifier = Modifier.fillMaxWidth().testTag("cattle_tag_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Animal Name (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = breed,
                        onValueChange = { breed = it },
                        label = { Text("Breed (e.g. HF, Jersey, Gir, Sahiwal, Murrah)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = dailyYieldText,
                            onValueChange = { dailyYieldText = it },
                            label = { Text("Daily Milk (L)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = ageMonthsText,
                            onValueChange = { ageMonthsText = it },
                            label = { Text("Age (Months)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = initialWeightText,
                            onValueChange = { initialWeightText = it },
                            label = { Text("Weight (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = initialBcsText,
                            onValueChange = { initialBcsText = it },
                            label = { Text("BCS (1.0-5.0)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                item {
                    Text("Lactation Stage", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("LACTATING", "DRY", "HEIFER", "CALF", "PREGNANT_DRY").forEach { stage ->
                            item {
                                FilterChip(
                                    selected = lactationStage == stage,
                                    onClick = { lactationStage = stage },
                                    label = { Text(stage.replace("_", " "), fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                }

                item {
                    Text("Breeding Status", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("OPEN", "INSEMINATED", "CONFIRMED_PREGNANT", "HEAT_DETECTED").forEach { status ->
                            item {
                                FilterChip(
                                    selected = breedingStatus == status,
                                    onClick = { breedingStatus = status },
                                    label = { Text(status.replace("_", " "), fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = sireName,
                            onValueChange = { sireName = it },
                            label = { Text("Sire / Bull / Semen") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = damName,
                            onValueChange = { damName = it },
                            label = { Text("Dam (Mother Tag)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = inseminationDaysAgo,
                        onValueChange = { inseminationDaysAgo = it },
                        label = { Text("Days Since Insemination (A.I.)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = healthNotes,
                        onValueChange = { healthNotes = it },
                        label = { Text("Health & Vaccination Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        maxLines = 2
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (tagNumber.isBlank()) return@Button
                    val now = System.currentTimeMillis()
                    val daysAgo = inseminationDaysAgo.toLongOrNull() ?: 0L
                    val insemDate = if (daysAgo > 0) now - (daysAgo * 86400000L) else 0L
                    val gestationDays = if (type == "BUFFALO") 310L else 283L
                    val expectedCalv = if (insemDate > 0) insemDate + (gestationDays * 86400000L) else 0L
                    val ageMonths = ageMonthsText.toLongOrNull() ?: 36L
                    val birthDateEpoch = now - (ageMonths * 30L * 86400000L)

                    val cattle = CattleEntity(
                        id = UUID.randomUUID().toString(),
                        businessId = businessId,
                        tagNumber = tagNumber.trim().uppercase(),
                        name = name.trim(),
                        type = type,
                        breed = breed.trim(),
                        lactationStage = lactationStage,
                        dailyYieldLiters = dailyYieldText.toDoubleOrNull() ?: 0.0,
                        breedingStatus = breedingStatus,
                        lastInseminationDate = insemDate,
                        bullIdOrSemenBrand = sireName.trim(),
                        expectedCalvingDate = expectedCalv,
                        birthDate = birthDateEpoch,
                        currentWeightKg = initialWeightText.toDoubleOrNull() ?: 0.0,
                        latestBcs = initialBcsText.toDoubleOrNull() ?: 3.0,
                        sireTagOrName = sireName.trim(),
                        damTagOrName = damName.trim(),
                        healthNotes = healthNotes.trim()
                    )
                    onSave(cattle)
                },
                colors = ButtonDefaults.buttonColors(containerColor = GrassGreen),
                modifier = Modifier.testTag("submit_cattle_button")
            ) {
                Text("Save Cattle Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
