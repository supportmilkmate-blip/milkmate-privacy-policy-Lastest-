package com.example.ui.screens.cattle

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Floating Farm Action Modal / Speed Dial providing quick access to:
 * 1. Register Cattle
 * 2. Milking Record (Morning/Evening individual logging)
 * 3. Breeding & AI / Insemination / PD / Calving
 * 4. Deworming Schedule
 * 5. Vaccination (FMD, HS, BQ, etc.)
 * 6. Treatment Record (Mastitis, Milk fever, etc.)
 * 7. Farm Observation (Heat, rumination, alerts)
 * 8. Add Weight & Girth Calculator (Shaeffer)
 * 9. Quick Body Condition (BCS)
 * 10. CMT Mastitis Udder Health
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmActionSpeedDialModal(
    onDismiss: () -> Unit,
    onAddCattle: () -> Unit,
    onRecordMilking: () -> Unit = {},
    onBreedingAction: () -> Unit = {},
    onDewormingAction: () -> Unit = {},
    onVaccinationAction: () -> Unit = {},
    onTreatmentAction: () -> Unit = {},
    onObservationAction: () -> Unit = {},
    onAddWeight: () -> Unit,
    onQuickBcs: () -> Unit,
    onCmtRecord: () -> Unit,
    supportedMilkTypes: String = "BOTH"
) {
    val (addCattleTitle, addCattleSubtitle) = when (supportedMilkTypes) {
        "COW_ONLY" -> "Register New Cow" to "Tag ID, cow profile, breed, lactation stage, sire/dam & yield"
        "BUFFALO_ONLY" -> "Register New Buffalo" to "Tag ID, buffalo profile, breed, lactation stage, sire/dam & yield"
        else -> "Register New Cattle" to "Tag ID, cow/buffalo, breed, lactation stage, sire/dam & yield"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.fillMaxHeight(0.88f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🚜 Quick Farm Actions",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Milking, breeding, health, veterinary & herd monitoring",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(vertical = 10.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Action 1: Add Cattle
                item {
                    FarmActionCard(
                        title = addCattleTitle,
                        subtitle = addCattleSubtitle,
                        icon = Icons.Default.Pets,
                        iconTint = GrassGreen,
                        badgeText = "NEW ANIMAL",
                        badgeColor = GrassGreen,
                        testTag = "speed_dial_add_cattle",
                        onClick = {
                            onDismiss()
                            onAddCattle()
                        }
                    )
                }

                // Action 2: Milking Record
                item {
                    FarmActionCard(
                        title = "Log Cow / Buffalo Milking",
                        subtitle = "Individual animal morning/evening yield, fat & SNF session record",
                        icon = Icons.Default.WaterDrop,
                        iconTint = RoyalBluePrimary,
                        badgeText = "MILKING",
                        badgeColor = RoyalBluePrimary,
                        testTag = "speed_dial_milking_record",
                        onClick = {
                            onDismiss()
                            onRecordMilking()
                        }
                    )
                }

                // Action 3: Breeding & AI Event
                item {
                    FarmActionCard(
                        title = "Breeding & A.I. / PD / Calving",
                        subtitle = "Record insemination, semen straw, PD pregnancy check & calving events",
                        icon = Icons.Default.Favorite,
                        iconTint = Color(0xFFE91E63),
                        badgeText = "BREEDING & A.I.",
                        badgeColor = Color(0xFFE91E63),
                        testTag = "speed_dial_breeding_event",
                        onClick = {
                            onDismiss()
                            onBreedingAction()
                        }
                    )
                }

                // Action 4: Deworming Schedule
                item {
                    FarmActionCard(
                        title = "Deworming Schedule & Dose",
                        subtitle = "Albendazole, Fenbendazole, Ivermectin, dose & repeat due date",
                        icon = Icons.Default.Medication,
                        iconTint = FreshGold,
                        badgeText = "DEWORMING",
                        badgeColor = FreshGold,
                        testTag = "speed_dial_deworming_event",
                        onClick = {
                            onDismiss()
                            onDewormingAction()
                        }
                    )
                }

                // Action 5: Vaccination Record
                item {
                    FarmActionCard(
                        title = "Cattle Vaccination (FMD / HS / BQ)",
                        subtitle = "NADCP vaccines, booster due date, batch number & immunity records",
                        icon = Icons.Default.Vaccines,
                        iconTint = RoyalBlueSecondary,
                        badgeText = "VACCINATION",
                        badgeColor = RoyalBlueSecondary,
                        testTag = "speed_dial_vaccination_event",
                        onClick = {
                            onDismiss()
                            onVaccinationAction()
                        }
                    )
                }

                // Action 6: Medical Treatment
                item {
                    FarmActionCard(
                        title = "Medical Treatment & Prescriptions",
                        subtitle = "Mastitis, milk fever, ketosis, drugs & milk withdrawal warnings",
                        icon = Icons.Default.LocalHospital,
                        iconTint = DangerRed,
                        badgeText = "TREATMENT",
                        badgeColor = DangerRed,
                        testTag = "speed_dial_treatment_event",
                        onClick = {
                            onDismiss()
                            onTreatmentAction()
                        }
                    )
                }

                // Action 7: Farm Observation
                item {
                    FarmActionCard(
                        title = "Farm & Cattle Observation",
                        subtitle = "Heat signs, rumination/cud chewing, dung consistency & shed notes",
                        icon = Icons.Default.Visibility,
                        iconTint = WarmHoney,
                        badgeText = "OBSERVATION",
                        badgeColor = WarmHoney,
                        testTag = "speed_dial_observation_event",
                        onClick = {
                            onDismiss()
                            onObservationAction()
                        }
                    )
                }

                // Action 8: Weight & Girth Calculator
                item {
                    FarmActionCard(
                        title = "Add Weight & Girth Calculator",
                        subtitle = "Measure by chest girth & length, or enter scale weight directly",
                        icon = Icons.Default.Scale,
                        iconTint = RoyalBluePrimary,
                        badgeText = "SHAEFFER FORMULA",
                        badgeColor = RoyalBluePrimary,
                        testTag = "speed_dial_add_weight",
                        onClick = {
                            onDismiss()
                            onAddWeight()
                        }
                    )
                }

                // Action 9: Quick Body Condition (BCS)
                item {
                    FarmActionCard(
                        title = "Quick Body Condition (BCS)",
                        subtitle = "1.0 - 5.0 scoring for spine, ribs, hook & pin bones with nutrition tips",
                        icon = Icons.Default.MonitorHeart,
                        iconTint = WarmHoney,
                        badgeText = "BCS 1-5",
                        badgeColor = WarmHoney,
                        testTag = "speed_dial_quick_bcs",
                        onClick = {
                            onDismiss()
                            onQuickBcs()
                        }
                    )
                }

                // Action 10: CMT Record
                item {
                    FarmActionCard(
                        title = "CMT Mastitis Udder Record",
                        subtitle = "4-Quarter paddle test (LF, RF, LH, RH) for subclinical & clinical detection",
                        icon = Icons.Default.Science,
                        iconTint = DangerRed,
                        badgeText = "UDDER HEALTH",
                        badgeColor = DangerRed,
                        testTag = "speed_dial_cmt_record",
                        onClick = {
                            onDismiss()
                            onCmtRecord()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FarmActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    badgeText: String,
    badgeColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, iconTint.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = iconTint.copy(alpha = 0.12f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = badgeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 14.sp
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        }
    }
}
