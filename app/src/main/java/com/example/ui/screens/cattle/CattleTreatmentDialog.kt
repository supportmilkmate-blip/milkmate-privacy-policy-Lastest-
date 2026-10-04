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
import com.example.data.local.entity.TreatmentRecordEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CattleTreatmentDialog(
    cattleList: List<CattleEntity>,
    preselectedCattleId: String? = null,
    onDismiss: () -> Unit,
    onSave: (TreatmentRecordEntity) -> Unit
) {
    var selectedCattle by remember {
        mutableStateOf(cattleList.find { it.id == preselectedCattleId } ?: cattleList.firstOrNull())
    }
    var diseaseName by remember { mutableStateOf("Subclinical Mastitis") }
    var symptoms by remember { mutableStateOf("Teat hardness, clots in milk, fever") }
    var medicationName by remember { mutableStateOf("Intramammary Infusion + Meloxicam 15ml") }
    var dosage by remember { mutableStateOf("1 syringe after complete stripping") }
    var timing by remember { mutableStateOf("Morning & Evening") }
    var durationDays by remember { mutableStateOf("3") }
    var milkWithdrawalDays by remember { mutableStateOf("4") }
    var treatmentCost by remember { mutableStateOf("350") }
    var performedBy by remember { mutableStateOf("Dr. Suresh Verma (Veterinary Doctor)") }
    var notes by remember { mutableStateOf("Milk discarded in withdrawal period to prevent antibiotic residues.") }

    val now = System.currentTimeMillis()
    val dayMillis = 86400000L
    val withdrawalInt = milkWithdrawalDays.toIntOrNull() ?: 0

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
                        text = "🩺 Record Cattle Medical Treatment",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Diagnosis, prescription, antibiotics & milk withdrawal safety",
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

                // Disease Presets
                item {
                    Text("Common Bovine Conditions", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            Triple("Mastitis (Udder Swelling)", "Teat hardness, clots in milk", "Intramammary Infusion + Meloxicam"),
                            Triple("Milk Fever (Hypocalcemia)", "Downer cow, S-curve neck, cold ears", "Mifex 450ml I/V (Calcium Borogluconate)"),
                            Triple("Ketosis / Acetonemia", "Drop in milk yield, rapid weight loss", "Propylene Glycol 200ml + Dextrose 25%"),
                            Triple("Foot Rot / Lameness", "Limping, interdigital swelling", "Oxytetracycline LA + Foot Bath (CuSO4)"),
                            Triple("Tympany / Bloat", "Distended left flank, labored breathing", "Bloatosil 100ml / Turpentine + Linseed oil"),
                            Triple("Indigestion / Simple Acidosis", "Off-feed, reduced rumination", "Rumen FS Powder + Buffer (Sodium Bicarbonate)")
                        ).forEach { (dis, symp, med) ->
                            val isSel = diseaseName.startsWith(dis.take(5))
                            item {
                                FilterChip(
                                    selected = isSel,
                                    onClick = {
                                        diseaseName = dis
                                        symptoms = symp
                                        medicationName = med
                                        if (dis.contains("Mastitis") || dis.contains("Foot Rot")) {
                                            milkWithdrawalDays = "4"
                                        } else {
                                            milkWithdrawalDays = "0"
                                        }
                                    },
                                    label = { Text(dis, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = diseaseName,
                        onValueChange = { diseaseName = it },
                        label = { Text("Disease / Condition Diagnosed") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = symptoms,
                        onValueChange = { symptoms = it },
                        label = { Text("Observed Symptoms") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = medicationName,
                        onValueChange = { medicationName = it },
                        label = { Text("Medication & Injections Prescribed") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = dosage,
                            onValueChange = { dosage = it },
                            label = { Text("Dosage") },
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = durationDays,
                            onValueChange = { durationDays = it },
                            label = { Text("Days") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.8f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = milkWithdrawalDays,
                            onValueChange = { milkWithdrawalDays = it },
                            label = { Text("Milk Withdrawal (Days)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = treatmentCost,
                            onValueChange = { treatmentCost = it },
                            label = { Text("Treatment Cost (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }
                }

                if (withdrawalInt > 0) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DangerRed.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = DangerRed, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "⚠️ Milk from #${selectedCattle?.tagNumber ?: "this animal"} MUST be discarded for $withdrawalInt days due to drug residue regulations.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DangerRed
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = performedBy,
                        onValueChange = { performedBy = it },
                        label = { Text("Veterinary Doctor / Practitioner") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Clinical Remarks & Diet Restrictions") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        minLines = 2
                    )
                }
            }

            Button(
                onClick = {
                    val c = selectedCattle ?: return@Button
                    val record = TreatmentRecordEntity(
                        id = "TR_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}",
                        businessId = c.businessId,
                        cattleId = c.id,
                        cattleTag = c.tagNumber,
                        diseaseName = diseaseName,
                        symptoms = symptoms,
                        medicationName = medicationName,
                        dosage = dosage,
                        timing = timing,
                        durationDays = durationDays.toIntOrNull() ?: 1,
                        checkupDate = now,
                        treatmentCost = treatmentCost.toDoubleOrNull() ?: 0.0,
                        performedBy = performedBy,
                        followUpDate = now + ((durationDays.toLongOrNull() ?: 3L) * dayMillis),
                        milkWithdrawalDays = withdrawalInt,
                        notes = notes,
                        createdAt = now
                    )
                    onSave(record)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().testTag("save_treatment_record_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                enabled = selectedCattle != null
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Treatment Record", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}
