package com.example.ui.screens.cattle

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
import com.example.data.local.entity.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CattleDetailSheet(
    cattle: CattleEntity,
    breedingHistory: List<BreedingRecordEntity> = emptyList(),
    milkingHistory: List<CattleMilkingRecordEntity> = emptyList(),
    dewormingHistory: List<DewormingRecordEntity> = emptyList(),
    vaccinationHistory: List<VaccinationRecordEntity> = emptyList(),
    treatmentHistory: List<TreatmentRecordEntity> = emptyList(),
    observationHistory: List<FarmObservationEntity> = emptyList(),
    weightHistory: List<CattleWeightEntity> = emptyList(),
    cmtHistory: List<CattleCmtEntity> = emptyList(),
    bcsHistory: List<CattleBcsEntity> = emptyList(),
    onDismiss: () -> Unit,
    onAddBreeding: () -> Unit = {},
    onAddMilking: () -> Unit = {},
    onAddDeworming: () -> Unit = {},
    onAddVaccination: () -> Unit = {},
    onAddTreatment: () -> Unit = {},
    onAddObservation: () -> Unit = {},
    onAddWeight: () -> Unit = {},
    onAddCmt: () -> Unit = {},
    onAddBcs: () -> Unit = {},
    onDeleteCattle: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val now = System.currentTimeMillis()
    val dayMillis = 86400000L

    val ageYearsMonths = remember(cattle.birthDate) {
        if (cattle.birthDate > 0L) {
            val totalDays = (now - cattle.birthDate) / dayMillis
            val years = totalDays / 365
            val months = (totalDays % 365) / 30
            "${years}y ${months}m"
        } else {
            "Adult"
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.fillMaxHeight(0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header Profile
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = if (cattle.type == "BUFFALO") DeepOceanNavy else GrassGreen.copy(alpha = 0.15f),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (cattle.type == "BUFFALO") "🐃" else "🐄",
                                fontSize = 24.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Tag #${cattle.tagNumber}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                            if (cattle.name.isNotBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(${cattle.name})",
                                    fontSize = 13.sp,
                                    color = RoyalBluePrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "${cattle.breed} • Age: $ageYearsMonths",
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
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (cattle.breedingStatus) {
                            "CONFIRMED_PREGNANT" -> GrassGreen
                            "INSEMINATED" -> RoyalBluePrimary
                            else -> WarmHoney
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Metrics Row (Weight, BCS, Daily Yield, Lactation)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricChip(
                    title = "Weight",
                    value = if (cattle.currentWeightKg > 0) "${cattle.currentWeightKg.toInt()} kg" else "N/A",
                    icon = Icons.Default.Scale,
                    tint = RoyalBluePrimary,
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    title = "Latest BCS",
                    value = String.format(Locale.getDefault(), "%.1f", cattle.latestBcs),
                    icon = Icons.Default.MonitorHeart,
                    tint = WarmHoney,
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    title = "Daily Yield",
                    value = "${cattle.dailyYieldLiters} L",
                    icon = Icons.Default.WaterDrop,
                    tint = FreshGold,
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    title = "Lactation",
                    value = "#${cattle.lactationCount}",
                    icon = Icons.Default.Refresh,
                    tint = GrassGreen,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Scrollable Tab Navigation across all categories
            val tabs = listOf(
                "Breeding (${breedingHistory.size})",
                "Milking (${milkingHistory.size})",
                "Deworming (${dewormingHistory.size})",
                "Vaccines (${vaccinationHistory.size})",
                "Treatments (${treatmentHistory.size})",
                "Observations (${observationHistory.size})",
                "Weights (${weightHistory.size})",
                "CMT Udder (${cmtHistory.size})",
                "BCS (${bcsHistory.size})"
            )

            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                contentColor = RoyalBluePrimary,
                edgePadding = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tab Content
            when (selectedTab) {
                0 -> BreedingTab(cattle, breedingHistory, sdf, now, dayMillis, onAddBreeding, onDeleteCattle)
                1 -> MilkingTab(cattle, milkingHistory, sdf, onAddMilking)
                2 -> DewormingTab(cattle, dewormingHistory, sdf, onAddDeworming)
                3 -> VaccinationTab(cattle, vaccinationHistory, sdf, onAddVaccination)
                4 -> TreatmentTab(cattle, treatmentHistory, sdf, onAddTreatment)
                5 -> ObservationTab(cattle, observationHistory, sdf, onAddObservation)
                6 -> WeightHistoryTab(cattle, weightHistory, sdf, onAddWeight)
                7 -> CmtHistoryTab(cattle, cmtHistory, sdf, onAddCmt)
                8 -> BcsHistoryTab(cattle, bcsHistory, sdf, onAddBcs)
            }
        }
    }
}

@Composable
private fun BreedingTab(
    cattle: CattleEntity,
    history: List<BreedingRecordEntity>,
    sdf: SimpleDateFormat,
    now: Long,
    dayMillis: Long,
    onAddBreeding: () -> Unit,
    onDelete: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Breeding Timeline & Inseminations", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                FilledTonalButton(
                    onClick = onAddBreeding,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New AI/PD Record", fontSize = 10.sp)
                }
            }
        }

        // Gestation & Calving Card
        if (cattle.breedingStatus == "CONFIRMED_PREGNANT" || cattle.expectedCalvingDate > 0L) {
            item {
                val totalGestationDays = if (cattle.type == "BUFFALO") 310L else 283L
                val daysElapsed = if (cattle.lastInseminationDate > 0) ((now - cattle.lastInseminationDate) / dayMillis).coerceIn(0L, totalGestationDays) else 0L
                val daysLeft = if (cattle.expectedCalvingDate > 0) ((cattle.expectedCalvingDate - now) / dayMillis).coerceAtLeast(0L) else 0L
                val progress = if (totalGestationDays > 0) (daysElapsed.toFloat() / totalGestationDays.toFloat()).coerceIn(0f, 1f) else 0f

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = FreshGoldLight),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FreshGold.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🤰 Gestation & Calving Timeline", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WarmHoney)
                            Text("Due in $daysLeft days", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = DangerRed)
                        }

                        LinearProgressIndicator(
                            progress = { progress },
                            color = FreshGold,
                            trackColor = Color.White,
                            modifier = Modifier.fillMaxWidth().height(6.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Elapsed: $daysElapsed / $totalGestationDays days", fontSize = 10.sp, color = TextSecondary)
                            Text("Exp. Calving: ${if (cattle.expectedCalvingDate > 0) sdf.format(Date(cattle.expectedCalvingDate)) else "N/A"}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }
                }
            }
        }

        // Breeding History Records
        if (history.isNotEmpty()) {
            items(history) { r ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("🧬 ${r.eventType.replace("_", " ")}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RoyalBluePrimary)
                            Text(sdf.format(Date(r.date)), fontSize = 11.sp, color = TextSecondary)
                        }
                        if (r.bullIdOrName.isNotBlank()) {
                            Text("Bull / Semen: ${r.bullIdOrName} (${r.inseminationType})", fontSize = 11.sp, color = TextPrimary)
                        }
                        if (r.pdStatus != "PENDING") {
                            Text("PD Result: ${r.pdStatus}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (r.pdStatus.contains("POSITIVE")) GrassGreen else DangerRed)
                        }
                        if (r.doneBy.isNotBlank()) {
                            Text("Done by: ${r.doneBy}", fontSize = 10.sp, color = TextSecondary)
                        }
                        if (r.notes.isNotBlank()) {
                            Text("Notes: ${r.notes}", fontSize = 10.sp, color = TextSecondary)
                        }
                    }
                }
            }
        }

        // Ancestry & Sire Details
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🧬 Pedigree & Lineage", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    DetailRow("Last Insemination Date:", if (cattle.lastInseminationDate > 0) sdf.format(Date(cattle.lastInseminationDate)) else "Not recorded")
                    DetailRow("Insemination Method:", cattle.inseminationType.replace("_", " "))
                    DetailRow("Sire / Bull ID / Semen:", cattle.bullIdOrSemenBrand.ifBlank { "Not specified" })
                    if (cattle.sireTagOrName.isNotBlank()) DetailRow("Father (Sire):", cattle.sireTagOrName)
                    if (cattle.damTagOrName.isNotBlank()) DetailRow("Mother (Dam):", cattle.damTagOrName)
                    DetailRow("Stage:", cattle.lactationStage.replace("_", " "))
                    DetailRow("Daily Milking Capacity:", "${cattle.dailyYieldLiters} Liters/day")
                }
            }
        }

        // Delete Cattle Action
        item {
            OutlinedButton(
                onClick = onDelete,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Remove Cattle Record")
            }
        }
    }
}

@Composable
private fun MilkingTab(
    cattle: CattleEntity,
    milkingList: List<CattleMilkingRecordEntity>,
    sdf: SimpleDateFormat,
    onAddMilking: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Individual Milking Log", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                FilledTonalButton(
                    onClick = onAddMilking,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Log Milking", fontSize = 10.sp)
                }
            }
        }

        if (milkingList.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("No individual milking records yet. Tap 'Log Milking' to add.", color = TextSecondary, fontSize = 12.sp)
                }
            }
        } else {
            items(milkingList) { m ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (m.shift == "MORNING") "🌅 Morning" else "🌙 Evening",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(sdf.format(Date(m.dateEpochMidnight)), fontSize = 10.sp, color = TextSecondary)
                            }
                            Text("FAT: ${m.fat}% • SNF: ${m.snf}% • By: ${m.recordedBy}", fontSize = 10.sp, color = TextSecondary)
                        }
                        Text("${m.quantityLiters} L", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = RoyalBluePrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun DewormingTab(
    cattle: CattleEntity,
    dewormingList: List<DewormingRecordEntity>,
    sdf: SimpleDateFormat,
    onAddDeworming: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Deworming Protocol & Next Due", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                FilledTonalButton(
                    onClick = onAddDeworming,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Dose", fontSize = 10.sp)
                }
            }
        }

        if (dewormingList.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("No deworming history recorded yet.", color = TextSecondary, fontSize = 12.sp)
                }
            }
        } else {
            items(dewormingList) { d ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("💊 ${d.dewormerSalt}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                            Text(sdf.format(Date(d.date)), fontSize = 10.sp, color = TextSecondary)
                        }
                        Text("Dose: ${d.dose} • By: ${d.administeredBy} • Cost: ₹${d.cost.toInt()}", fontSize = 11.sp, color = TextSecondary)
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = FreshGoldLight
                        ) {
                            Text(
                                text = "⏰ Next Due: ${sdf.format(Date(d.nextDueDate))} (every ${d.repeatAfterDays}d)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarmHoney,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (d.remarks.isNotBlank()) Text("Notes: ${d.remarks}", fontSize = 10.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun VaccinationTab(
    cattle: CattleEntity,
    vacList: List<VaccinationRecordEntity>,
    sdf: SimpleDateFormat,
    onAddVaccination: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Vaccination History & Immunity", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                FilledTonalButton(
                    onClick = onAddVaccination,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Log Vaccine", fontSize = 10.sp)
                }
            }
        }

        if (vacList.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("No vaccination records found.", color = TextSecondary, fontSize = 12.sp)
                }
            }
        } else {
            items(vacList) { v ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("💉 ${v.vaccineName}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RoyalBluePrimary)
                            Text(sdf.format(Date(v.date)), fontSize = 10.sp, color = TextSecondary)
                        }
                        Text("${v.manufacturer} • Batch #${v.batchNo.ifBlank { "N/A" }}", fontSize = 11.sp, color = TextSecondary)
                        if (v.nextDueDate > 0) {
                            Surface(shape = RoundedCornerShape(4.dp), color = GrassGreen.copy(alpha = 0.15f)) {
                                Text(
                                    text = "🛡️ Booster Due: ${sdf.format(Date(v.nextDueDate))}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GrassGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (v.vaccinatedBy.isNotBlank()) Text("Certified by: ${v.vaccinatedBy}", fontSize = 10.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun TreatmentTab(
    cattle: CattleEntity,
    treatmentList: List<TreatmentRecordEntity>,
    sdf: SimpleDateFormat,
    onAddTreatment: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Clinical Treatment & Prescriptions", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                FilledTonalButton(
                    onClick = onAddTreatment,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Treatment", fontSize = 10.sp)
                }
            }
        }

        if (treatmentList.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("No medical treatments logged.", color = TextSecondary, fontSize = 12.sp)
                }
            }
        } else {
            items(treatmentList) { t ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("🩺 ${t.diseaseName}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DangerRed)
                            Text(sdf.format(Date(t.checkupDate)), fontSize = 10.sp, color = TextSecondary)
                        }
                        Text("Medication: ${t.medicationName} (${t.dosage})", fontSize = 11.sp, color = TextPrimary)
                        Text("Duration: ${t.durationDays}d • Cost: ₹${t.treatmentCost.toInt()} • Doctor: ${t.performedBy}", fontSize = 10.sp, color = TextSecondary)
                        if (t.milkWithdrawalDays > 0) {
                            Surface(shape = RoundedCornerShape(4.dp), color = DangerRed.copy(alpha = 0.15f)) {
                                Text(
                                    text = "⚠️ Milk Withdrawal: ${t.milkWithdrawalDays} days",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DangerRed,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (t.notes.isNotBlank()) Text("Notes: ${t.notes}", fontSize = 10.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun ObservationTab(
    cattle: CattleEntity,
    obsList: List<FarmObservationEntity>,
    sdf: SimpleDateFormat,
    onAddObservation: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Cattle Observations & Activity Logs", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                FilledTonalButton(
                    onClick = onAddObservation,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Log Observation", fontSize = 10.sp)
                }
            }
        }

        if (obsList.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("No observation logs for this animal.", color = TextSecondary, fontSize = 12.sp)
                }
            }
        } else {
            items(obsList) { o ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("👁️ ${o.activityType.replace("_", " ")}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (o.hasAlert) DangerRed else TextPrimary)
                            Text(sdf.format(Date(o.date)), fontSize = 10.sp, color = TextSecondary)
                        }
                        Text(o.description, fontSize = 11.sp, color = TextSecondary)
                        Text("Logged by: ${o.loggedBy}", fontSize = 10.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeightHistoryTab(
    cattle: CattleEntity,
    weights: List<CattleWeightEntity>,
    sdf: SimpleDateFormat,
    onAddWeight: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Weight Progression & Girth History", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                FilledTonalButton(
                    onClick = onAddWeight,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp).testTag("add_weight_from_detail")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Weight", fontSize = 10.sp)
                }
            }
        }

        if (weights.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No weight records yet. Tap 'New Weight' to measure using chest girth.", color = TextSecondary, fontSize = 12.sp)
                }
            }
        } else {
            items(weights) { w ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(sdf.format(Date(w.date)), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(
                                text = if (w.method == "GIRTH_CALCULATION") "Girth: ${w.heartGirthCm}cm • Length: ${w.bodyLengthCm}cm" else "Direct Scale Weighing",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                            if (w.notes.isNotBlank()) {
                                Text(w.notes, fontSize = 10.sp, color = TextSecondary)
                            }
                        }
                        Text(
                            text = "${String.format(Locale.getDefault(), "%.1f", w.weightKg)} kg",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = RoyalBluePrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CmtHistoryTab(
    cattle: CattleEntity,
    cmts: List<CattleCmtEntity>,
    sdf: SimpleDateFormat,
    onAddCmt: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("CMT 4-Quarter Udder Health History", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                FilledTonalButton(
                    onClick = onAddCmt,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp).testTag("add_cmt_from_detail")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New CMT Test", fontSize = 10.sp)
                }
            }
        }

        if (cmts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No CMT paddle records yet. Regular testing catches subclinical mastitis early.", color = TextSecondary, fontSize = 12.sp)
                }
            }
        } else {
            items(cmts) { cmt ->
                val isMastitis = cmt.overallDiagnosis != "HEALTHY" && cmt.overallDiagnosis != "NORMAL"
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isMastitis) DangerRed.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    border = if (isMastitis) androidx.compose.foundation.BorderStroke(1.dp, DangerRed.copy(alpha = 0.4f)) else null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(sdf.format(Date(cmt.date)), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isMastitis) DangerRed else GrassGreen
                            ) {
                                Text(
                                    text = cmt.overallDiagnosis.replace("_", " "),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // 4 Quarter Matrix
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            QuarterBadge("LF: ${cmt.quarterLf}", cmt.quarterLf, Modifier.weight(1f))
                            QuarterBadge("RF: ${cmt.quarterRf}", cmt.quarterRf, Modifier.weight(1f))
                            QuarterBadge("LH: ${cmt.quarterLh}", cmt.quarterLh, Modifier.weight(1f))
                            QuarterBadge("RH: ${cmt.quarterRh}", cmt.quarterRh, Modifier.weight(1f))
                        }

                        if (cmt.treatmentRecommendation.isNotBlank()) {
                            Text("Rec: ${cmt.treatmentRecommendation}", fontSize = 10.sp, color = TextSecondary)
                        }
                    }
                }
            }
        }
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

@Composable
private fun BcsHistoryTab(
    cattle: CattleEntity,
    bcsList: List<CattleBcsEntity>,
    sdf: SimpleDateFormat,
    onAddBcs: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Body Condition Scoring (BCS 1.0 - 5.0)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                FilledTonalButton(
                    onClick = onAddBcs,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp).testTag("add_bcs_from_detail")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New BCS", fontSize = 10.sp)
                }
            }
        }

        if (bcsList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No BCS scores recorded yet.", color = TextSecondary, fontSize = 12.sp)
                }
            }
        } else {
            items(bcsList) { bcs ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(sdf.format(Date(bcs.date)), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = when (bcs.category) {
                                    "IDEAL" -> GrassGreen
                                    "THIN" -> FreshGold
                                    else -> DangerRed
                                }
                            ) {
                                Text(
                                    text = "${bcs.score} • ${bcs.category}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (bcs.nutritionAdvice.isNotBlank()) {
                            Text("Feed Advice: ${bcs.nutritionAdvice}", fontSize = 10.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricChip(
    title: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = tint.copy(alpha = 0.1f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(title, fontSize = 9.sp, color = TextSecondary)
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 11.sp, color = TextSecondary)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}
