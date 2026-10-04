package com.example.ui.screens.cattle

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import com.example.data.local.entity.CattleEntity
import com.example.data.local.entity.CattleWeightEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CattleWeightDialog(
    businessId: String,
    cattleList: List<CattleEntity>,
    preselectedCattleId: String? = null,
    previousWeights: List<CattleWeightEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSaveWeight: (CattleWeightEntity) -> Unit
) {
    var selectedCattleId by remember {
        mutableStateOf(preselectedCattleId ?: cattleList.firstOrNull()?.id ?: "")
    }
    val selectedCattle = cattleList.find { it.id == selectedCattleId }

    var selectedStage by remember {
        mutableStateOf(
            when (selectedCattle?.lactationStage) {
                "HEIFER" -> "HEIFER"
                else -> "ADULT"
            }
        )
    }

    var entryMode by remember { mutableIntStateOf(1) } // 0: Manual Scale, 1: Calculate by Girth
    var unitMode by remember { mutableStateOf("CM") } // "CM" or "INCHES"

    // Inputs
    var manualWeightText by remember { mutableStateOf(selectedCattle?.currentWeightKg?.takeIf { it > 0 }?.toString() ?: "450.0") }
    var heartGirthText by remember { mutableStateOf("185.0") } // in cm
    var bodyLengthText by remember { mutableStateOf("145.0") } // in cm
    var notesText by remember { mutableStateOf("") }

    // Live Girth Calculation
    val calculatedWeightKg = remember(heartGirthText, bodyLengthText, unitMode) {
        val girth = heartGirthText.toDoubleOrNull() ?: 0.0
        val length = bodyLengthText.toDoubleOrNull() ?: 0.0
        if (girth <= 0.0 || length <= 0.0) {
            0.0
        } else if (unitMode == "INCHES") {
            // Shaeffer's formula: Weight (lbs) = (Girth² × Length) / 300
            // Weight (kg) = lbs * 0.45359237
            val lbs = (girth * girth * length) / 300.0
            (lbs * 0.45359237 * 10).toInt() / 10.0
        } else {
            // Metric Livestock formula: Weight (kg) = (Girth_cm² × Length_cm) / 10840
            val kg = (girth * girth * length) / 10840.0
            (kg * 10).toInt() / 10.0
        }
    }

    val finalWeight = if (entryMode == 0) (manualWeightText.toDoubleOrNull() ?: 0.0) else calculatedWeightKg

    // Find previous weigh-in for this animal
    val lastWeighIn = remember(selectedCattleId, previousWeights) {
        previousWeights.filter { it.cattleId == selectedCattleId }.maxByOrNull { it.date }
    }

    val dailyGainRate = remember(finalWeight, lastWeighIn) {
        if (lastWeighIn != null && lastWeighIn.weightKg > 0.0 && finalWeight > 0.0) {
            val daysDiff = ((System.currentTimeMillis() - lastWeighIn.date) / 86400000L).coerceAtLeast(1)
            val gain = finalWeight - lastWeighIn.weightKg
            val rate = gain / daysDiff
            (rate * 100).toInt() / 100.0
        } else {
            0.0
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = RoyalBluePrimary.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Scale, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Add Weight Record", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Livestock scale or Girth calculation", fontSize = 11.sp, color = TextSecondary)
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
                                .testTag("weight_cattle_dropdown"),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            cattleList.forEach { cattle ->
                                DropdownMenuItem(
                                    text = {
                                        Text("#${cattle.tagNumber} - ${cattle.name.ifBlank { cattle.type }} (${cattle.breed})")
                                    },
                                    onClick = {
                                        selectedCattleId = cattle.id
                                        selectedStage = if (cattle.lactationStage == "HEIFER") "HEIFER" else "ADULT"
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Stage Dropdown (Calf, Heifer, Adult)
                item {
                    Text("Animal Stage:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("CALF" to "Calf", "HEIFER" to "Heifer", "ADULT" to "Adult").forEach { (code, label) ->
                            val isSel = selectedStage == code
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedStage = code },
                                label = { Text(label, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) }
                            )
                        }
                    }
                }

                // Entry Mode Tab: Manual Entry vs Calculate by Girth
                item {
                    TabRow(
                        selectedTabIndex = entryMode,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        contentColor = RoyalBluePrimary,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Tab(
                            selected = entryMode == 0,
                            onClick = { entryMode = 0 },
                            text = { Text("Manual Entry", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = entryMode == 1,
                            onClick = { entryMode = 1 },
                            text = { Text("📐 Calculate by Girth", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                if (entryMode == 0) {
                    // Manual Scale Entry
                    item {
                        OutlinedTextField(
                            value = manualWeightText,
                            onValueChange = { manualWeightText = it },
                            label = { Text("Body Weight (kg) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            leadingIcon = { Icon(Icons.Default.Scale, contentDescription = null, tint = RoyalBluePrimary) },
                            modifier = Modifier.fillMaxWidth().testTag("manual_weight_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                } else {
                    // Calculate by Girth
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = RoyalBlueLight.copy(alpha = 0.5f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, RoyalBluePrimary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Shaeffer's Livestock Formula", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RoyalBluePrimary)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        FilterChip(
                                            selected = unitMode == "CM",
                                            onClick = { unitMode = "CM" },
                                            label = { Text("cm", fontSize = 10.sp) }
                                        )
                                        FilterChip(
                                            selected = unitMode == "INCHES",
                                            onClick = { unitMode = "INCHES" },
                                            label = { Text("inches", fontSize = 10.sp) }
                                        )
                                    }
                                }

                                Text(
                                    text = if (unitMode == "CM") "Weight (kg) = (Heart Girth² × Length) ÷ 10,840"
                                    else "Weight (lbs) = (Heart Girth² × Length) ÷ 300",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = heartGirthText,
                                        onValueChange = { heartGirthText = it },
                                        label = { Text("Heart Girth ($unitMode)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f).testTag("girth_input"),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    OutlinedTextField(
                                        value = bodyLengthText,
                                        onValueChange = { bodyLengthText = it },
                                        label = { Text("Body Length ($unitMode)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f).testTag("length_input"),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, RoyalBluePrimary.copy(alpha = 0.2f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Calculated Weight:", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                        Text(
                                            text = "$calculatedWeightKg kg (~${(calculatedWeightKg * 2.20462).toInt()} lbs)",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = RoyalBluePrimary
                                        )
                                    }
                                }

                                Text(
                                    text = "💡 Tip: Measure Heart Girth directly behind the front legs. Measure Body Length from point of shoulder to point of pin bone.",
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }

                // Growth Analysis / Average Daily Gain (ADG)
                if (lastWeighIn != null) {
                    item {
                        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = GrassGreen.copy(alpha = 0.1f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GrassGreen.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "📈 Growth & Average Daily Gain (ADG)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GrassGreen
                                )
                                Text(
                                    text = "Previous: ${lastWeighIn.weightKg} kg on ${sdf.format(Date(lastWeighIn.date))}",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                                if (dailyGainRate != 0.0) {
                                    val isGain = dailyGainRate > 0
                                    Text(
                                        text = "${if (isGain) "+" else ""}$dailyGainRate kg/day ($finalWeight kg)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isGain) GrassGreen else DangerRed
                                    )
                                }
                            }
                        }
                    }
                }

                // Notes
                item {
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("Weighing Notes / Observations") },
                        placeholder = { Text("e.g. Fed before weighing, healthy coat, steady growth") },
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
                    if (selectedCattleId.isBlank() || finalWeight <= 0.0) return@Button
                    val girthCm = if (unitMode == "INCHES") (heartGirthText.toDoubleOrNull() ?: 0.0) * 2.54 else (heartGirthText.toDoubleOrNull() ?: 0.0)
                    val lenCm = if (unitMode == "INCHES") (bodyLengthText.toDoubleOrNull() ?: 0.0) * 2.54 else (bodyLengthText.toDoubleOrNull() ?: 0.0)

                    val record = CattleWeightEntity(
                        id = UUID.randomUUID().toString(),
                        businessId = businessId,
                        cattleId = selectedCattleId,
                        tagNumber = selectedCattle?.tagNumber ?: "",
                        date = System.currentTimeMillis(),
                        stage = selectedStage,
                        weightKg = finalWeight,
                        method = if (entryMode == 0) "MANUAL" else "GIRTH_CALCULATION",
                        heartGirthCm = if (entryMode == 1) girthCm else 0.0,
                        bodyLengthCm = if (entryMode == 1) lenCm else 0.0,
                        dailyGainKg = dailyGainRate,
                        notes = notesText.trim()
                    )
                    onSaveWeight(record)
                },
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                modifier = Modifier.testTag("save_weight_btn")
            ) {
                Text("Save Weight Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
