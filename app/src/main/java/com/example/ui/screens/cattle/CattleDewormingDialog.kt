package com.example.ui.screens.cattle

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import com.example.data.local.entity.CattleEntity
import com.example.data.local.entity.DewormingRecordEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CattleDewormingDialog(
    cattleList: List<CattleEntity>,
    preselectedCattleId: String? = null,
    onDismiss: () -> Unit,
    onSave: (DewormingRecordEntity) -> Unit
) {
    var selectedCattle by remember {
        mutableStateOf(cattleList.find { it.id == preselectedCattleId } ?: cattleList.firstOrNull())
    }
    var dewormerSalt by remember { mutableStateOf("Albendazole Oral Suspension") }
    var dose by remember { mutableStateOf("100 ml (3000 mg)") }
    var repeatDays by remember { mutableStateOf("90") }
    var administeredBy by remember { mutableStateOf("Self / Farm Manager") }
    var cost by remember { mutableStateOf("150") }
    var remarks by remember { mutableStateOf("") }

    val now = System.currentTimeMillis()
    val dayMillis = 86400000L
    val repeatDaysInt = repeatDays.toIntOrNull() ?: 90
    val nextDueDate = now + (repeatDaysInt * dayMillis)
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
                        text = "💊 Record Deworming Dose",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Internal parasite protection & seasonal deworming schedule",
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

                // Dewormer Salt Quick Presets
                item {
                    Text("Dewormer Salt / Medicine", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            "Albendazole Oral (Broad Spectrum)" to "100 ml (3000 mg)",
                            "Fenbendazole Bolus (Safe in Pregnancy)" to "3000 mg Bolus",
                            "Ivermectin 1% Injection (Endo/Ecto)" to "10 ml S/C",
                            "Oxyclozanide (Flukicide / Liver Fluke)" to "90 ml Drench",
                            "Levamisole Oral" to "60 ml"
                        ).forEach { (salt, defaultDose) ->
                            val isSel = dewormerSalt == salt
                            item {
                                FilterChip(
                                    selected = isSel,
                                    onClick = {
                                        dewormerSalt = salt
                                        dose = defaultDose
                                    },
                                    label = { Text(salt, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = dewormerSalt,
                        onValueChange = { dewormerSalt = it },
                        label = { Text("Dewormer Medicine / Chemical Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = dose,
                            onValueChange = { dose = it },
                            label = { Text("Dose & Route") },
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = repeatDays,
                            onValueChange = { repeatDays = it },
                            label = { Text("Repeat (Days)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.8f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = RoyalBlueLight,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Next Deworming Due: ${sdf.format(Date(nextDueDate))} (in $repeatDaysInt days)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoyalBluePrimary
                            )
                        }
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = administeredBy,
                            onValueChange = { administeredBy = it },
                            label = { Text("Administered By") },
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = cost,
                            onValueChange = { cost = it },
                            label = { Text("Cost (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.8f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = remarks,
                        onValueChange = { remarks = it },
                        label = { Text("Remarks (e.g., given before morning feed)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        minLines = 2
                    )
                }
            }

            Button(
                onClick = {
                    val c = selectedCattle ?: return@Button
                    val record = DewormingRecordEntity(
                        id = "DEW_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}",
                        businessId = c.businessId,
                        cattleId = c.id,
                        cattleTag = c.tagNumber,
                        dewormerSalt = dewormerSalt,
                        dose = dose,
                        date = now,
                        repeatAfterDays = repeatDaysInt,
                        nextDueDate = nextDueDate,
                        administeredBy = administeredBy,
                        cost = cost.toDoubleOrNull() ?: 0.0,
                        remarks = remarks,
                        createdAt = now
                    )
                    onSave(record)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().testTag("save_deworming_record_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                enabled = selectedCattle != null
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Deworming Entry", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}
