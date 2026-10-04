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
import com.example.data.local.entity.CattleMilkingRecordEntity
import com.example.ui.theme.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CattleMilkingDialog(
    cattleList: List<CattleEntity>,
    preselectedCattleId: String? = null,
    onDismiss: () -> Unit,
    onSave: (CattleMilkingRecordEntity) -> Unit
) {
    val lactatingCattle = remember(cattleList) {
        val lactating = cattleList.filter { it.lactationStage == "LACTATING" }
        if (lactating.isNotEmpty()) lactating else cattleList
    }
    var selectedCattle by remember {
        mutableStateOf(lactatingCattle.find { it.id == preselectedCattleId } ?: lactatingCattle.firstOrNull())
    }
    var shift by remember { mutableStateOf("MORNING") } // MORNING, EVENING
    var quantityText by remember {
        mutableStateOf(
            if ((selectedCattle?.dailyYieldLiters ?: 0.0) > 0.0) {
                String.format(Locale.getDefault(), "%.1f", (selectedCattle?.dailyYieldLiters ?: 12.0) / 2.0)
            } else "6.5"
        )
    }
    var fatText by remember { mutableStateOf(if (selectedCattle?.type == "BUFFALO") "7.2" else "4.2") }
    var snfText by remember { mutableStateOf(if (selectedCattle?.type == "BUFFALO") "9.1" else "8.5") }
    var recordedBy by remember { mutableStateOf("Myself / Milker") }

    val now = System.currentTimeMillis()
    val dayMillis = 86400000L
    val dateEpochMidnight = now - (now % dayMillis)

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
                        text = "🥛 Individual Cattle Milking Log",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Morning / evening shift milk yield, FAT & SNF logging",
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
                    Text("Select Milking Animal", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(lactatingCattle) { c ->
                            val isSel = selectedCattle?.id == c.id
                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    selectedCattle = c
                                    if (c.dailyYieldLiters > 0.0) {
                                        quantityText = String.format(Locale.getDefault(), "%.1f", c.dailyYieldLiters / 2.0)
                                    }
                                    if (c.type == "BUFFALO") {
                                        fatText = "7.2"
                                        snfText = "9.1"
                                    } else {
                                        fatText = "4.2"
                                        snfText = "8.5"
                                    }
                                },
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

                // Shift Selector
                item {
                    Text("Milking Shift", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = shift == "MORNING",
                            onClick = { shift = "MORNING" },
                            label = { Text("🌅 Morning Shift", fontWeight = if (shift == "MORNING") FontWeight.Bold else FontWeight.Normal) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = shift == "EVENING",
                            onClick = { shift = "EVENING" },
                            label = { Text("🌙 Evening Shift", fontWeight = if (shift == "EVENING") FontWeight.Bold else FontWeight.Normal) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("Milk Yield (Liters)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().testTag("cattle_milking_qty_input"),
                        shape = RoundedCornerShape(10.dp),
                        leadingIcon = { Icon(Icons.Default.WaterDrop, contentDescription = null, tint = RoyalBluePrimary) },
                        singleLine = true
                    )
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = fatText,
                            onValueChange = { fatText = it },
                            label = { Text("FAT % (Optional)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = snfText,
                            onValueChange = { snfText = it },
                            label = { Text("SNF % (Optional)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = recordedBy,
                        onValueChange = { recordedBy = it },
                        label = { Text("Recorded By") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }
            }

            Button(
                onClick = {
                    val c = selectedCattle ?: return@Button
                    val qty = quantityText.toDoubleOrNull() ?: 0.0
                    val record = CattleMilkingRecordEntity(
                        id = "MILK_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}",
                        businessId = c.businessId,
                        cattleId = c.id,
                        cattleTag = c.tagNumber,
                        dateEpochMidnight = dateEpochMidnight,
                        shift = shift,
                        quantityLiters = qty,
                        fat = fatText.toDoubleOrNull() ?: 4.2,
                        snf = snfText.toDoubleOrNull() ?: 8.5,
                        recordedBy = recordedBy,
                        createdAt = now
                    )
                    onSave(record)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().testTag("save_cattle_milking_record_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                enabled = selectedCattle != null && (quantityText.toDoubleOrNull() ?: 0.0) > 0.0
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Milking Record", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}
