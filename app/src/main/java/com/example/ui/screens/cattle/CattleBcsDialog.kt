package com.example.ui.screens.cattle

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CattleBcsEntity
import com.example.data.local.entity.CattleEntity
import com.example.ui.theme.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CattleBcsDialog(
    businessId: String,
    cattleList: List<CattleEntity>,
    preselectedCattleId: String? = null,
    onDismiss: () -> Unit,
    onSaveBcs: (CattleBcsEntity) -> Unit
) {
    var selectedCattleId by remember {
        mutableStateOf(preselectedCattleId ?: cattleList.firstOrNull()?.id ?: "")
    }
    val selectedCattle = cattleList.find { it.id == selectedCattleId }

    var score by remember { mutableFloatStateOf(selectedCattle?.latestBcs?.toFloat() ?: 3.0f) }
    var notesText by remember { mutableStateOf("") }

    val roundedScore = remember(score) {
        (Math.round(score * 4) / 4.0) // snap to 0.25 steps
    }

    val (category, catColor, catDesc, advice) = remember(roundedScore) {
        when {
            roundedScore < 2.0 -> {
                Quadruple(
                    "VERY_THIN",
                    DangerRed,
                    "Deep hollow around tailhead, sharp vertebrae ridges, individual ribs very prominent.",
                    "⚠️ Severe energy deficit! Increase bypass fat, add 1.5kg steam-flaked grain/concentrate, check for parasite load or chronic illness."
                )
            }
            roundedScore in 2.0..2.5 -> {
                Quadruple(
                    "THIN",
                    Color(0xFFF57C00),
                    "Spine ridge visible, ribs felt easily with light touch, angular hook & pin bones (V-shape).",
                    "Energy deficit in progress. Boost concentrate ration by 1.0kg/day, supply high-quality green fodder like Lucerne/Berseem."
                )
            }
            roundedScore in 2.75..3.5 -> {
                Quadruple(
                    "IDEAL",
                    GrassGreen,
                    "Optimal condition. Backbone rounded, gentle fat cushion over short ribs, smooth U-shape between hooks and pins.",
                    "✅ Ideal dairy herd condition! Maintain balanced dry matter intake with 50g mineral mix and clean water ad libitum."
                )
            }
            roundedScore in 3.75..4.25 -> {
                Quadruple(
                    "OVERWEIGHT",
                    WarmHoney,
                    "Short ribs flat and buried, fat deposits noticeable on tailhead, hook bones rounded.",
                    "Risk of fatty liver and ketosis at calving. Gradually reduce cereal grain concentrates, increase fibrous dry fodder."
                )
            }
            else -> {
                Quadruple(
                    "OBESE",
                    DangerRed,
                    "Thick fat pads around tailhead and brisket, spine buried in fatty crease, excessive flesh.",
                    "⚠️ Overconditioned (BCS > 4.5). High dystocia & metabolic disorder risk. Restrict concentrates; feed maintenance fiber ration only."
                )
            }
        }
    }

    val targetBcsForStage = remember(selectedCattle?.lactationStage) {
        when (selectedCattle?.lactationStage) {
            "LACTATING" -> "2.75 - 3.25 (Mid Lactation Target)"
            "DRY", "PREGNANT_DRY" -> "3.25 - 3.50 (Dry Period / Calving Target)"
            "HEIFER" -> "2.75 - 3.00 (Breeding Heifer Target)"
            else -> "3.00 - 3.25"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = WarmHoney.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MonitorHeart, contentDescription = null, tint = WarmHoney, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Quick Body Condition (BCS)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("1.0 - 5.0 Dairy Cattle Condition Score", fontSize = 11.sp, color = TextSecondary)
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cattle Selection
                item {
                    Text("Select Cattle / Animal *", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedCattle?.let { "#${it.tagNumber} (${it.name.ifBlank { it.type }}) - ${it.breed}" } ?: "Select Cattle",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("bcs_cattle_dropdown"),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            cattleList.forEach { cattle ->
                                DropdownMenuItem(
                                    text = {
                                        Text("#${cattle.tagNumber} - ${cattle.name.ifBlank { cattle.type }} (BCS: ${cattle.latestBcs})")
                                    },
                                    onClick = {
                                        selectedCattleId = cattle.id
                                        score = cattle.latestBcs.toFloat().coerceIn(1.0f, 5.0f)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Stage Target Pill
                item {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Target for Stage (${selectedCattle?.lactationStage ?: "Ideal"}):", fontSize = 11.sp, color = TextSecondary)
                            Text(targetBcsForStage, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                        }
                    }
                }

                // Score Slider & Live Value Card
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, catColor.copy(alpha = 0.4f)),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Body Condition Score", fontSize = 11.sp, color = TextSecondary)
                                    Text(
                                        text = String.format(Locale.getDefault(), "%.2f", roundedScore),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = catColor
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = catColor.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = category.replace("_", " "),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = catColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // Slider
                            Slider(
                                value = score,
                                onValueChange = { score = it },
                                valueRange = 1.0f..5.0f,
                                steps = 15, // 0.25 step increments
                                colors = SliderDefaults.colors(
                                    thumbColor = catColor,
                                    activeTrackColor = catColor,
                                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("bcs_slider")
                            )

                            // Slider Scale Marks
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                listOf("1.0 (Thin)", "2.0", "3.0 (Ideal)", "4.0", "5.0 (Fat)").forEach { mark ->
                                    Text(mark, fontSize = 9.sp, color = TextSecondary)
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                            Text(
                                text = catDesc,
                                fontSize = 11.sp,
                                color = TextPrimary,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                // Anatomy Inspection Quick Checkpoints
                item {
                    Text("Anatomy Inspection Reference:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        AnatomyItem(
                            part = "🦴 Backbone / Spine",
                            detail = if (roundedScore <= 2.5) "Vertebrae jagged & prominent" else if (roundedScore <= 3.5) "Rounded ridge, felt with firm touch" else "Hidden in fat layer"
                        )
                        AnatomyItem(
                            part = "🥩 Short Ribs",
                            detail = if (roundedScore <= 2.5) "Individual bones visible like shelf" else if (roundedScore <= 3.5) "Smooth wave, fat cover present" else "Ribs completely buried"
                        )
                        AnatomyItem(
                            part = "🍑 Hook & Pin Bones",
                            detail = if (roundedScore <= 2.5) "Sharp V-angle, prominent hollows" else if (roundedScore <= 3.5) "Rounded U-shape, balanced" else "Thick rounded fat cushions"
                        )
                    }
                }

                // Feeding & Nutrition Advice
                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = RoyalBlueLight.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RoyalBluePrimary.copy(alpha = 0.25f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("🌾 Nutritional Advice", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                            Text(advice, fontSize = 11.sp, color = TextPrimary, lineHeight = 15.sp)
                        }
                    }
                }

                // Notes
                item {
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("Assessment Notes") },
                        placeholder = { Text("e.g. Post-calving checkup, good hair gloss, active rumination") },
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
                    if (selectedCattleId.isBlank()) return@Button
                    val record = CattleBcsEntity(
                        id = UUID.randomUUID().toString(),
                        businessId = businessId,
                        cattleId = selectedCattleId,
                        tagNumber = selectedCattle?.tagNumber ?: "",
                        date = System.currentTimeMillis(),
                        score = roundedScore,
                        category = category,
                        spineAssessment = if (roundedScore <= 2.5) "Sharp & visible" else if (roundedScore <= 3.5) "Rounded with fat cover" else "Buried",
                        ribsAssessment = if (roundedScore <= 2.5) "Prominent shelf" else if (roundedScore <= 3.5) "Smooth cushion" else "Flat buried",
                        hooksAndPins = if (roundedScore <= 2.5) "V-shape profile" else if (roundedScore <= 3.5) "U-shape profile" else "Heavy fat pads",
                        nutritionAdvice = advice,
                        notes = notesText.trim()
                    )
                    onSaveBcs(record)
                },
                colors = ButtonDefaults.buttonColors(containerColor = catColor),
                modifier = Modifier.testTag("save_bcs_btn")
            ) {
                Text("Save BCS Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AnatomyItem(part: String, detail: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(part, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Text(detail, fontSize = 10.sp, color = TextSecondary)
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
