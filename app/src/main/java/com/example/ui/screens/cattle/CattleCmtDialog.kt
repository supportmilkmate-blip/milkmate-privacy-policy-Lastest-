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
import com.example.data.local.entity.CattleCmtEntity
import com.example.data.local.entity.CattleEntity
import com.example.ui.theme.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CattleCmtDialog(
    businessId: String,
    cattleList: List<CattleEntity>,
    preselectedCattleId: String? = null,
    onDismiss: () -> Unit,
    onSaveCmt: (CattleCmtEntity) -> Unit,
    supportedMilkTypes: String = "BOTH"
) {
    // California Mastitis Test is performed on lactating/milking cattle
    val milkingCattle = remember(cattleList) {
        val filtered = cattleList.filter { it.lactationStage == "LACTATING" }
        if (filtered.isNotEmpty()) filtered else cattleList
    }

    var selectedCattleId by remember {
        mutableStateOf(preselectedCattleId ?: milkingCattle.firstOrNull()?.id ?: "")
    }
    val selectedCattle = milkingCattle.find { it.id == selectedCattleId }

    var testerName by remember { mutableStateOf("Farm In-Charge") }

    // 4 Quarters: LF, RF, LH, RH
    // Scores: N (Negative), T (Trace), 1 (Score 1), 2 (Score 2), 3 (Score 3)
    var scoreLf by remember { mutableStateOf("N") }
    var scoreRf by remember { mutableStateOf("N") }
    var scoreLh by remember { mutableStateOf("N") }
    var scoreRh by remember { mutableStateOf("N") }

    var milkWithheld by remember { mutableStateOf(false) }
    var notesText by remember { mutableStateOf("") }

    // Diagnostic calculation
    val (overallDiagnosis, autoRecommendation) = remember(scoreLf, scoreRf, scoreLh, scoreRh) {
        val scores = listOf(scoreLf, scoreRf, scoreLh, scoreRh)
        when {
            scores.any { it == "2" || it == "3" } -> {
                "CLINICAL_MASTITIS" to "⚠️ High Risk! Clinical mastitis detected. Isolate animal from main milking parlor, withhold milk from bulk tank, strip affected quarters 4x daily, and consult veterinarian for antibiotic sensitivity."
            }
            scores.any { it == "T" || it == "1" } -> {
                "SUBCLINICAL_MASTITIS" to "⚡ Subclinical Mastitis detected. Apply post-milking iodine teat dip (0.5%), check vacuum pressure on milking cluster, and re-test in 3 days."
            }
            else -> {
                "NORMAL" to "✅ Healthy Udder. All 4 quarters test negative (N) for somatic cell elevation. Continue regular parlor hygiene."
            }
        }
    }

    // Auto update milk withholding on high severity
    LaunchedEffect(overallDiagnosis) {
        if (overallDiagnosis == "CLINICAL_MASTITIS") {
            milkWithheld = true
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = DangerRed.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Science, contentDescription = null, tint = DangerRed, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("CMT Mastitis Udder Test", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("California Mastitis Test (4 Quarters)", fontSize = 11.sp, color = TextSecondary)
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
                    val cattleSelectLabel = when (supportedMilkTypes) {
                        "COW_ONLY" -> "Select Milking Cow *"
                        "BUFFALO_ONLY" -> "Select Milking Buffalo *"
                        else -> "Select Milking Cow / Buffalo *"
                    }
                    Text(cattleSelectLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                                .testTag("cmt_cattle_dropdown"),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            milkingCattle.forEach { cattle ->
                                DropdownMenuItem(
                                    text = {
                                        Text("#${cattle.tagNumber} - ${cattle.name.ifBlank { cattle.type }} (${cattle.lactationStage})")
                                    },
                                    onClick = {
                                        selectedCattleId = cattle.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Tester Name
                item {
                    OutlinedTextField(
                        value = testerName,
                        onValueChange = { testerName = it },
                        label = { Text("Tester / Technician Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                // Visual 4-Quarter Udder Diagram & Selector
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🧪 4-Quarter CMT Paddle Test", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = when (overallDiagnosis) {
                                        "CLINICAL_MASTITIS" -> DangerRed.copy(alpha = 0.15f)
                                        "SUBCLINICAL_MASTITIS" -> WarmHoney.copy(alpha = 0.15f)
                                        else -> GrassGreen.copy(alpha = 0.15f)
                                    }
                                ) {
                                    Text(
                                        text = overallDiagnosis.replace("_", " "),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = when (overallDiagnosis) {
                                            "CLINICAL_MASTITIS" -> DangerRed
                                            "SUBCLINICAL_MASTITIS" -> WarmHoney
                                            else -> GrassGreen
                                        },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text("Tap each quarter below to adjust its reaction score:", fontSize = 11.sp, color = TextSecondary)

                            // 2x2 Udder Quarters Grid
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Front Quarters: LF and RF
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    QuarterCard(
                                        label = "Left Front (LF)",
                                        sublabel = "Front Left Quarter",
                                        score = scoreLf,
                                        onScoreSelected = { scoreLf = it },
                                        modifier = Modifier.weight(1f)
                                    )
                                    QuarterCard(
                                        label = "Right Front (RF)",
                                        sublabel = "Front Right Quarter",
                                        score = scoreRf,
                                        onScoreSelected = { scoreRf = it },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                // Hind Quarters: LH and RH
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    QuarterCard(
                                        label = "Left Hind (LH)",
                                        sublabel = "Rear Left Quarter",
                                        score = scoreLh,
                                        onScoreSelected = { scoreLh = it },
                                        modifier = Modifier.weight(1f)
                                    )
                                    QuarterCard(
                                        label = "Right Hind (RH)",
                                        sublabel = "Rear Right Quarter",
                                        score = scoreRh,
                                        onScoreSelected = { scoreRh = it },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // Score Legend
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                listOf(
                                    "N" to ("Negative" to GrassGreen),
                                    "T" to ("Trace" to WarmHoney),
                                    "1" to ("Mild 1" to Color(0xFFF57C00)),
                                    "2" to ("Mod 2" to Color(0xFFE64A19)),
                                    "3" to ("Severe 3" to DangerRed)
                                ).forEach { (code, pair) ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = pair.second,
                                            modifier = Modifier.size(8.dp)
                                        ) {}
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("$code: ${pair.first}", fontSize = 9.sp, color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }

                // Treatment & Management Recommendation Card
                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = when (overallDiagnosis) {
                            "CLINICAL_MASTITIS" -> DangerRed.copy(alpha = 0.08f)
                            "SUBCLINICAL_MASTITIS" -> FreshGoldLight
                            else -> GrassGreen.copy(alpha = 0.08f)
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            when (overallDiagnosis) {
                                "CLINICAL_MASTITIS" -> DangerRed.copy(alpha = 0.4f)
                                "SUBCLINICAL_MASTITIS" -> FreshGold.copy(alpha = 0.4f)
                                else -> GrassGreen.copy(alpha = 0.4f)
                            }
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "📋 Action & Treatment Recommendation",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (overallDiagnosis) {
                                    "CLINICAL_MASTITIS" -> DangerRed
                                    "SUBCLINICAL_MASTITIS" -> WarmHoney
                                    else -> GrassGreen
                                }
                            )
                            Text(
                                text = autoRecommendation,
                                fontSize = 11.sp,
                                color = TextPrimary,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                // Milk Withholding Switch
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { milkWithheld = !milkWithheld }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Withhold Milk from Tank / Supply", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Prevent antibiotic or high-SCC milk from reaching collection tank", fontSize = 10.sp, color = TextSecondary)
                        }
                        Switch(
                            checked = milkWithheld,
                            onCheckedChange = { milkWithheld = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = DangerRed, checkedTrackColor = DangerRed.copy(alpha = 0.3f))
                        )
                    }
                }

                // Notes
                item {
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("Clinical Notes / Symptoms") },
                        placeholder = { Text("e.g. Swollen quarter, clots in strip cup, reduced milk yield") },
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
                    val record = CattleCmtEntity(
                        id = UUID.randomUUID().toString(),
                        businessId = businessId,
                        cattleId = selectedCattleId,
                        tagNumber = selectedCattle?.tagNumber ?: "",
                        date = System.currentTimeMillis(),
                        testerName = testerName.trim(),
                        quarterLf = scoreLf,
                        quarterRf = scoreRf,
                        quarterLh = scoreLh,
                        quarterRh = scoreRh,
                        overallDiagnosis = overallDiagnosis,
                        treatmentRecommendation = autoRecommendation,
                        milkWithheld = milkWithheld,
                        notes = notesText.trim()
                    )
                    onSaveCmt(record)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (overallDiagnosis == "CLINICAL_MASTITIS") DangerRed else GrassGreen
                ),
                modifier = Modifier.testTag("save_cmt_btn")
            ) {
                Text("Save CMT Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun QuarterCard(
    label: String,
    sublabel: String,
    score: String,
    onScoreSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val (scoreColor, scoreName) = when (score) {
        "N" -> GrassGreen to "Negative (0)"
        "T" -> WarmHoney to "Trace (T)"
        "1" -> Color(0xFFF57C00) to "Mild (1)"
        "2" -> Color(0xFFE64A19) to "Moderate (2)"
        "3" -> DangerRed to "Severe (3)"
        else -> TextSecondary to "Unknown"
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, scoreColor.copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextPrimary)
                Surface(
                    shape = CircleShape,
                    color = scoreColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = score,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = scoreColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(scoreName, fontSize = 9.sp, color = scoreColor, fontWeight = FontWeight.Bold)

            // Selector Chips Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("N", "T", "1", "2", "3").forEach { sc ->
                    val isSel = score == sc
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isSel) scoreColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onScoreSelected(sc) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = sc,
                                fontSize = 10.sp,
                                fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.Normal,
                                color = if (isSel) Color.White else TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
