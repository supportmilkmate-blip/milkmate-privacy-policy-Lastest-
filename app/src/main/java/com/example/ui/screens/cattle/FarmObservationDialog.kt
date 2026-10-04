package com.example.ui.screens.cattle

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.data.local.entity.CattleEntity
import com.example.data.local.entity.FarmObservationEntity
import com.example.ui.theme.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmObservationDialog(
    cattleList: List<CattleEntity>,
    preselectedCattleId: String? = null,
    onDismiss: () -> Unit,
    onSave: (FarmObservationEntity) -> Unit
) {
    var isWholeFarm by remember { mutableStateOf(preselectedCattleId == null) }
    var selectedCattle by remember {
        mutableStateOf(cattleList.find { it.id == preselectedCattleId } ?: cattleList.firstOrNull())
    }
    var activityType by remember { mutableStateOf("HEAT_SIGNS") }
    var description by remember { mutableStateOf("Clear mucous discharge and mounting behavior noted during morning shed inspection.") }
    var hasAlert by remember { mutableStateOf(true) }
    var loggedBy by remember { mutableStateOf("Farm Manager / Staff") }

    val now = System.currentTimeMillis()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "👁️ Log Farm & Cattle Observation",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Heat detection, rumination, feeding intake & clinical alerts",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Whole Farm vs Individual Animal
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = isWholeFarm,
                            onClick = { isWholeFarm = true },
                            label = { Text("🚜 Whole Farm / Shed", fontSize = 11.sp, fontWeight = if (isWholeFarm) FontWeight.Bold else FontWeight.Normal) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = !isWholeFarm,
                            onClick = { isWholeFarm = false },
                            label = { Text("🐄 Specific Animal", fontSize = 11.sp, fontWeight = if (!isWholeFarm) FontWeight.Bold else FontWeight.Normal) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // If Specific Animal, show selector
                if (!isWholeFarm) {
                    item {
                        Text("Select Animal", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(cattleList) { c ->
                                val isSel = selectedCattle?.id == c.id
                                FilterChip(
                                    selected = isSel,
                                    onClick = { selectedCattle = c },
                                    label = {
                                        Text(
                                            text = "#${c.tagNumber} (${c.name.ifBlank { c.type }})",
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.sp
                                        )
                                    },
                                    leadingIcon = {
                                        Text(if (c.type == "BUFFALO") "🐃" else "🐄", fontSize = 12.sp)
                                    }
                                )
                            }
                        }
                    }
                }

                // Activity Type Presets
                item {
                    Text("Observation Category", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            Triple("HEAT_SIGNS", "🔥 Heat / Bellowing", "Standing heat detected, clear mucosal discharge. Ready for A.I. in 12 hours."),
                            Triple("RUMINATION", "🌾 Cud Chewing / Rumination", "Active rumination observed (>50 chews per bolus). Normal gut health."),
                            Triple("DUNG_CONSISTENCY", "💩 Dung / Stool Quality", "Normal consistency (Score 3). Well-digested roughage without grain passage."),
                            Triple("LAMENESS", "🩹 Foot / Limping Alert", "Slight limping on right hind leg. Cleaned hoof & checked for stone."),
                            Triple("FEEDING", "🌽 Feed & Silage Intake", "Clean manger observed. Total mixed ration consumed fully."),
                            Triple("TEMPERATURE", "🌡️ Body Temperature", "Rectal temperature 101.5°F (Normal). Eye and muzzle bright."),
                            Triple("GENERAL_NOTE", "📝 Shed Note", "Shed bedding refreshed with dry straw and lime dusting for hygiene.")
                        ).forEach { (code, label, defaultDesc) ->
                            val isSel = activityType == code
                            item {
                                FilterChip(
                                    selected = isSel,
                                    onClick = {
                                        activityType = code
                                        description = defaultDesc
                                        hasAlert = code in listOf("HEAT_SIGNS", "LAMENESS")
                                    },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Observation Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        minLines = 3
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("🚨 Mark as Action Alert", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (hasAlert) DangerRed else TextPrimary)
                            Text("Highlights in red for veterinary visit or heat timing", fontSize = 11.sp, color = TextSecondary)
                        }
                        Switch(
                            checked = hasAlert,
                            onCheckedChange = { hasAlert = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = DangerRed, checkedTrackColor = DangerRed.copy(alpha = 0.3f))
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = loggedBy,
                        onValueChange = { loggedBy = it },
                        label = { Text("Logged By (Staff / Owner)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }
            }

            Button(
                onClick = {
                    val bizId = cattleList.firstOrNull()?.businessId ?: ""
                    val c = if (!isWholeFarm) selectedCattle else null
                    val record = FarmObservationEntity(
                        id = "OBS_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}",
                        businessId = bizId,
                        cattleId = c?.id ?: "",
                        cattleTag = c?.tagNumber ?: "Whole Farm",
                        activityType = activityType,
                        date = now,
                        description = description,
                        hasAlert = hasAlert,
                        loggedBy = loggedBy,
                        createdAt = now
                    )
                    onSave(record)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().testTag("save_farm_observation_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WarmHoney)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Log Farm Observation", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}
