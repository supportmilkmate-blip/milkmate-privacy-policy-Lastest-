package com.example.ui.screens.cattle

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.data.local.entity.BreedingRecordEntity
import com.example.data.local.entity.CattleEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CattleBreedingDialog(
    cattleList: List<CattleEntity>,
    preselectedCattleId: String? = null,
    onDismiss: () -> Unit,
    onSave: (BreedingRecordEntity) -> Unit
) {
    var selectedCattle by remember {
        mutableStateOf(cattleList.find { it.id == preselectedCattleId } ?: cattleList.firstOrNull())
    }
    var eventType by remember { mutableStateOf("INSEMINATION") } // HEAT, INSEMINATION, PREGNANCY_DIAGNOSIS, CALVING, DRY_OFF
    var inseminationType by remember { mutableStateOf("ARTIFICIAL") }
    var bullOrSemen by remember { mutableStateOf("ABS Sexed Semen Bull #402") }
    var strawsUsed by remember { mutableStateOf("1") }
    var doneBy by remember { mutableStateOf("Veterinary Doctor / AI Tech") }
    var pdStatus by remember { mutableStateOf("POSITIVE_PREGNANT") }
    var calfGender by remember { mutableStateOf("FEMALE") }
    var calfTag by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val now = System.currentTimeMillis()
    val dayMillis = 86400000L
    val isBuffalo = selectedCattle?.type == "BUFFALO"
    val gestationDays = if (isBuffalo) 310L else 283L
    val estimatedCalvingDate = now + (gestationDays * dayMillis)
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

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
                        text = "🧬 Record Breeding & AI Event",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Insemination, PD check, Heat cycle & Calving record",
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
                // Cattle Selector
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

                // Event Type Selector
                item {
                    Text("Event Type", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            "INSEMINATION" to "💉 A.I. Insemination",
                            "PREGNANCY_DIAGNOSIS" to "🩺 PD Check",
                            "HEAT" to "🔥 Standing Heat",
                            "CALVING" to "🐣 Calving",
                            "DRY_OFF" to "🌾 Dry-Off"
                        ).forEach { (code, label) ->
                            val isSel = eventType == code
                            item {
                                FilterChip(
                                    selected = isSel,
                                    onClick = { eventType = code },
                                    label = { Text(label, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }

                // Event-specific fields
                if (eventType == "INSEMINATION") {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = inseminationType == "ARTIFICIAL",
                                onClick = { inseminationType = "ARTIFICIAL" },
                                label = { Text("Artificial (A.I.)", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = inseminationType == "NATURAL",
                                onClick = { inseminationType = "NATURAL" },
                                label = { Text("Natural Service", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = bullOrSemen,
                            onValueChange = { bullOrSemen = it },
                            label = { Text("Bull ID / Semen Straw Brand") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = strawsUsed,
                            onValueChange = { strawsUsed = it },
                            label = { Text("Straws Used") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = FreshGoldLight,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Event, contentDescription = null, tint = WarmHoney, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Est. Calving Date ($gestationDays days): ${sdf.format(Date(estimatedCalvingDate))}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WarmHoney
                                )
                            }
                        }
                    }
                } else if (eventType == "PREGNANCY_DIAGNOSIS") {
                    item {
                        Text("PD Result", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = pdStatus == "POSITIVE_PREGNANT",
                                onClick = { pdStatus = "POSITIVE_PREGNANT" },
                                label = { Text("🤰 Positive (Pregnant)", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = pdStatus == "NEGATIVE",
                                onClick = { pdStatus = "NEGATIVE" },
                                label = { Text("❌ Negative (Open)", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else if (eventType == "CALVING") {
                    item {
                        Text("Calf Gender", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = calfGender == "FEMALE",
                                onClick = { calfGender = "FEMALE" },
                                label = { Text("👧 Heifer (Female)", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = calfGender == "MALE",
                                onClick = { calfGender = "MALE" },
                                label = { Text("👦 Bull Calf (Male)", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = calfTag,
                            onValueChange = { calfTag = it },
                            label = { Text("New Calf Tag Number (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = doneBy,
                        onValueChange = { doneBy = it },
                        label = { Text("Performed By (Doctor / Technician / Inseminator)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Clinical Notes / Remarks") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        minLines = 2
                    )
                }
            }

            Button(
                onClick = {
                    val c = selectedCattle ?: return@Button
                    val record = BreedingRecordEntity(
                        id = "BR_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}",
                        businessId = c.businessId,
                        cattleId = c.id,
                        cattleTag = c.tagNumber,
                        eventType = eventType,
                        date = now,
                        inseminationType = inseminationType,
                        bullIdOrName = bullOrSemen,
                        semenStrawsUsed = strawsUsed.toIntOrNull() ?: 1,
                        doneBy = doneBy,
                        pdStatus = pdStatus,
                        expectedCalvingDate = if (eventType == "INSEMINATION" || (eventType == "PREGNANCY_DIAGNOSIS" && pdStatus == "POSITIVE_PREGNANT")) estimatedCalvingDate else 0L,
                        actualCalvingDate = if (eventType == "CALVING") now else 0L,
                        calfGender = if (eventType == "CALVING") calfGender else "NONE",
                        calfTag = calfTag,
                        notes = notes,
                        createdAt = now
                    )
                    onSave(record)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().testTag("save_breeding_record_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                enabled = selectedCattle != null
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Breeding Record", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}
