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
import com.example.data.local.entity.VaccinationRecordEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CattleVaccinationDialog(
    cattleList: List<CattleEntity>,
    preselectedCattleId: String? = null,
    onDismiss: () -> Unit,
    onSave: (VaccinationRecordEntity) -> Unit
) {
    var selectedCattle by remember {
        mutableStateOf(cattleList.find { it.id == preselectedCattleId } ?: cattleList.firstOrNull())
    }
    var vaccineName by remember { mutableStateOf("FMD (Foot & Mouth Disease)") }
    var manufacturer by remember { mutableStateOf("Indian Immunologicals Ltd (Raksha-Ovac)") }
    var batchNo by remember { mutableStateOf("RO-8842") }
    var boosterDueMonths by remember { mutableStateOf("6") }
    var vaccinatedBy by remember { mutableStateOf("Veterinary Assistant / Doctor") }
    var cost by remember { mutableStateOf("0") }
    var proofAttachment by remember { mutableStateOf("Ear Tag Verified (NADCP)") }

    val now = System.currentTimeMillis()
    val dayMillis = 86400000L
    val monthsInt = boosterDueMonths.toIntOrNull() ?: 6
    val nextDueDate = now + (monthsInt * 30L * dayMillis)
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
                        text = "💉 Record Cattle Vaccination",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "FMD, HS, BQ, Theileriosis, Anthrax & NADCP tracking",
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

                // Quick Indian Vaccine Presets
                item {
                    Text("Vaccine Presets", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            Triple("FMD (Foot & Mouth Disease)", "Indian Immunologicals (Raksha-Ovac)", "6"),
                            Triple("HS + BQ Combined", "Indian Immunologicals (Raksha-Biovac)", "12"),
                            Triple("Brucellosis (Heifers 4-8m)", "Bruvax S19 (Lifetime Immunity)", "0"),
                            Triple("Theileriosis (Rakshavac-T)", "Indian Immunologicals", "12"),
                            Triple("Anthrax Spore Vaccine", "State Biological Production", "12"),
                            Triple("Lumpy Skin Disease (LSD)", "Lumpi-ProVacInd", "12"),
                            Triple("Rabies Post-Exposure", "Raksharab", "1")
                        ).forEach { (vac, mfg, months) ->
                            val isSel = vaccineName.startsWith(vac.take(5))
                            item {
                                FilterChip(
                                    selected = isSel,
                                    onClick = {
                                        vaccineName = vac
                                        manufacturer = mfg
                                        boosterDueMonths = if (months == "0") "0" else months
                                    },
                                    label = { Text(vac, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = vaccineName,
                        onValueChange = { vaccineName = it },
                        label = { Text("Vaccine Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = manufacturer,
                            onValueChange = { manufacturer = it },
                            label = { Text("Manufacturer / Brand") },
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = batchNo,
                            onValueChange = { batchNo = it },
                            label = { Text("Batch #") },
                            modifier = Modifier.weight(0.7f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = boosterDueMonths,
                            onValueChange = { boosterDueMonths = it },
                            label = { Text("Booster Due (Months)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = cost,
                            onValueChange = { cost = it },
                            label = { Text("Cost (₹ 0 if Govt)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }
                }

                if (monthsInt > 0) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = GrassGreen.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = GrassGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Next Booster Due: ${sdf.format(Date(nextDueDate))} (in $monthsInt months)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GrassGreen
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = vaccinatedBy,
                        onValueChange = { vaccinatedBy = it },
                        label = { Text("Administered / Certified By") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = proofAttachment,
                        onValueChange = { proofAttachment = it },
                        label = { Text("Verification / Govt INAPH Portal Tag Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }
            }

            Button(
                onClick = {
                    val c = selectedCattle ?: return@Button
                    val record = VaccinationRecordEntity(
                        id = "VAC_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}",
                        businessId = c.businessId,
                        cattleId = c.id,
                        cattleTag = c.tagNumber,
                        vaccineName = vaccineName,
                        manufacturer = manufacturer,
                        batchNo = batchNo,
                        date = now,
                        nextDueDate = if (monthsInt > 0) nextDueDate else 0L,
                        cost = cost.toDoubleOrNull() ?: 0.0,
                        vaccinatedBy = vaccinatedBy,
                        proofAttachment = proofAttachment,
                        createdAt = now
                    )
                    onSave(record)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().testTag("save_vaccination_record_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                enabled = selectedCattle != null
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Vaccination Record", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}
