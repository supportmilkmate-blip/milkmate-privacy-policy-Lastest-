package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.BusinessEntity
import com.example.data.repository.PricingEngine
import com.example.ui.MilkMateViewModel
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomFormulaStudioDialog(
    business: BusinessEntity?,
    viewModel: MilkMateViewModel,
    onDismiss: () -> Unit
) {
    var formulaName by remember { mutableStateOf(business?.customFormulaName ?: "My Custom Formula") }
    var baseRateText by remember { mutableStateOf(String.format(Locale.US, "%.1f", business?.customFormulaBase ?: 45.0)) }
    var fatMultiplierText by remember { mutableStateOf(String.format(Locale.US, "%.1f", business?.customFormulaFatMultiplier ?: 0.0)) }
    var snfMultiplierText by remember { mutableStateOf(String.format(Locale.US, "%.1f", business?.customFormulaSnfMultiplier ?: 0.0)) }
    var tsMultiplierText by remember { mutableStateOf(String.format(Locale.US, "%.1f", business?.customFormulaTsMultiplier ?: 0.0)) }
    var clrMultiplierText by remember { mutableStateOf(String.format(Locale.US, "%.1f", business?.customFormulaClrMultiplier ?: 0.0)) }
    var qualityBonusText by remember { mutableStateOf(String.format(Locale.US, "%.1f", business?.customFormulaQualityBonus ?: 0.0)) }
    var chillingDeductionText by remember { mutableStateOf(String.format(Locale.US, "%.1f", business?.customFormulaChillingDeduction ?: 0.0)) }
    var minFloorRateText by remember { mutableStateOf(String.format(Locale.US, "%.1f", business?.customFormulaMinFloorRate ?: 15.0)) }
    var yieldType by remember { mutableStateOf(business?.customFormulaYieldType ?: "KHOA") } // NONE, PANEER, KHOA, GHEE
    var yieldMultiplierText by remember { mutableStateOf(String.format(Locale.US, "%.1f", business?.customFormulaYieldMultiplier ?: 0.0)) }
    var baseYieldGramsText by remember { mutableStateOf(String.format(Locale.US, "%.1f", business?.customFormulaBaseYieldGrams ?: 160.0)) }
    var diffRatePer10gText by remember { mutableStateOf(String.format(Locale.US, "%.1f", business?.customFormulaDiffRatePer10g ?: 2.8)) }
    var differentialMode by remember { mutableStateOf(business?.customFormulaDifferentialMode ?: false) }

    // Simulator testing states
    var simFat by remember { mutableFloatStateOf(6.5f) }
    var simSnf by remember { mutableFloatStateOf(9.0f) }
    var simClr by remember { mutableFloatStateOf(28.0f) }

    val baseRate = baseRateText.toDoubleOrNull() ?: 45.0
    val fatMult = fatMultiplierText.toDoubleOrNull() ?: 0.0
    val snfMult = snfMultiplierText.toDoubleOrNull() ?: 0.0
    val tsMult = tsMultiplierText.toDoubleOrNull() ?: 0.0
    val clrMult = clrMultiplierText.toDoubleOrNull() ?: 0.0
    val bonus = qualityBonusText.toDoubleOrNull() ?: 0.0
    val deduction = chillingDeductionText.toDoubleOrNull() ?: 0.0
    val minFloor = minFloorRateText.toDoubleOrNull() ?: 15.0
    val yieldMult = yieldMultiplierText.toDoubleOrNull() ?: 0.0
    val baseYieldGrams = baseYieldGramsText.toDoubleOrNull() ?: 160.0
    val diffRatePer10g = diffRatePer10gText.toDoubleOrNull() ?: 2.8

    fun calculateFormulaPrice(f: Double, s: Double, c: Double): Double {
        val fatComp = if (differentialMode) (f - (business?.fatBaseRate ?: 3.5)) * fatMult else f * fatMult
        val snfComp = if (differentialMode) (s - (business?.snfBaseRate ?: 8.5)) * snfMult else s * snfMult
        val tsComp = (f + s) * tsMult
        val clrComp = c * clrMult
        val yieldComp = when (yieldType.uppercase()) {
            "PANEER" -> {
                val yieldGrams = PricingEngine.calculatePaneerYieldGrams(f, s)
                (yieldGrams - baseYieldGrams) * (diffRatePer10g / 10.0)
            }
            "KHOA", "KHOYA" -> {
                val yieldGrams = PricingEngine.calculateKhoaYieldGrams(f, s)
                (yieldGrams - baseYieldGrams) * (diffRatePer10g / 10.0)
            }
            "GHEE" -> {
                val yieldGrams = PricingEngine.calculateGheeYieldGrams(f)
                (yieldGrams - baseYieldGrams) * (diffRatePer10g / 10.0)
            }
            else -> {
                if (yieldMult > 0) (PricingEngine.calculatePaneerYieldGrams(f, s) / 1000.0) * yieldMult else 0.0
            }
        }
        val raw = baseRate + fatComp + snfComp + tsComp + clrComp + yieldComp + bonus - deduction
        return maxOf(minFloor, Math.round(raw * 100.0) / 100.0)
    }

    // Live calculated test rates
    val simulatedPrice = remember(
        baseRate, fatMult, snfMult, tsMult, clrMult, bonus, deduction, minFloor, yieldType, yieldMult, differentialMode,
        baseYieldGrams, diffRatePer10g, simFat, simSnf, simClr
    ) {
        calculateFormulaPrice(simFat.toDouble(), simSnf.toDouble(), simClr.toDouble())
    }

    val simulatedYieldGrams = remember(yieldType, simFat, simSnf) {
        when (yieldType.uppercase()) {
            "PANEER" -> PricingEngine.calculatePaneerYieldGrams(simFat.toDouble(), simSnf.toDouble())
            "KHOA", "KHOYA" -> PricingEngine.calculateKhoaYieldGrams(simFat.toDouble(), simSnf.toDouble())
            "GHEE" -> PricingEngine.calculateGheeYieldGrams(simFat.toDouble())
            else -> 0.0
        }
    }

    val cowBenchmarkPrice = remember(
        baseRate, fatMult, snfMult, tsMult, clrMult, bonus, deduction, minFloor, yieldType, yieldMult, differentialMode,
        baseYieldGrams, diffRatePer10g
    ) {
        calculateFormulaPrice(4.0, 8.5, 28.0)
    }

    val buffaloBenchmarkPrice = remember(
        baseRate, fatMult, snfMult, tsMult, clrMult, bonus, deduction, minFloor, yieldType, yieldMult, differentialMode,
        baseYieldGrams, diffRatePer10g
    ) {
        calculateFormulaPrice(6.8, 9.0, 30.0)
    }

    val richSamplePrice = remember(
        baseRate, fatMult, snfMult, tsMult, clrMult, bonus, deduction, minFloor, yieldType, yieldMult, differentialMode,
        baseYieldGrams, diffRatePer10g
    ) {
        calculateFormulaPrice(7.5, 9.2, 31.0)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Surface(
                    color = DeepOceanNavy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = FreshGold.copy(alpha = 0.2f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Functions, contentDescription = null, tint = FreshGold, modifier = Modifier.size(22.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Custom Milk Formula Studio", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                Text("Design your rate formula (Khoa/Paneer Yields or FAT/SNF)", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                // Scrollable Equation Canvas & Inputs
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Quick Starter Template Presets (Including Indian Khoa/Paneer standard benchmarks)
                    item {
                        Text("⚡ Popular Indian Dairy Rate Templates (1-Tap Apply):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PresetTemplateChip("🍬 Khoa: 160g = ₹45/L", "±₹2.8 per 10g Khoa") {
                                formulaName = "Indian Khoa Standard (160g = ₹45)"
                                baseRateText = "45.0"
                                yieldType = "KHOA"
                                baseYieldGramsText = "160.0"
                                diffRatePer10gText = "2.8"
                                fatMultiplierText = "0.0"
                                snfMultiplierText = "0.0"
                                tsMultiplierText = "0.0"
                                clrMultiplierText = "0.0"
                                qualityBonusText = "0.0"
                                chillingDeductionText = "0.0"
                                minFloorRateText = "20.0"
                                differentialMode = false
                            }

                            PresetTemplateChip("🧀 Paneer: 140g = ₹44/L", "±₹3.0 per 10g Paneer") {
                                formulaName = "Indian Paneer Standard (140g = ₹44)"
                                baseRateText = "44.0"
                                yieldType = "PANEER"
                                baseYieldGramsText = "140.0"
                                diffRatePer10gText = "3.0"
                                fatMultiplierText = "0.0"
                                snfMultiplierText = "0.0"
                                tsMultiplierText = "0.0"
                                clrMultiplierText = "0.0"
                                qualityBonusText = "0.0"
                                chillingDeductionText = "0.0"
                                minFloorRateText = "20.0"
                                differentialMode = false
                            }

                            PresetTemplateChip("🧪 Cooperative FAT+SNF", "₹24 Base + Fat/SNF") {
                                formulaName = "Cooperative FAT+SNF Standard"
                                baseRateText = "24.0"
                                yieldType = "NONE"
                                fatMultiplierText = "5.8"
                                snfMultiplierText = "3.6"
                                tsMultiplierText = "0.0"
                                clrMultiplierText = "0.0"
                                qualityBonusText = "1.0"
                                chillingDeductionText = "0.0"
                                minFloorRateText = "15.0"
                                differentialMode = false
                            }

                            PresetTemplateChip("🏺 Desi Ghee: 60g = ₹42/L", "±₹7.0 per 10g Ghee") {
                                formulaName = "Ghee Fat Recovery Model"
                                baseRateText = "42.0"
                                yieldType = "GHEE"
                                baseYieldGramsText = "60.0"
                                diffRatePer10gText = "7.0"
                                fatMultiplierText = "0.0"
                                snfMultiplierText = "0.0"
                                tsMultiplierText = "0.0"
                                clrMultiplierText = "0.0"
                                minFloorRateText = "15.0"
                                differentialMode = false
                            }

                            PresetTemplateChip("🔬 Richmond CLR + Fat", "Base ₹22 + CLR Dip") {
                                formulaName = "Richmond CLR & Fat Model"
                                baseRateText = "22.0"
                                yieldType = "NONE"
                                fatMultiplierText = "5.0"
                                snfMultiplierText = "0.0"
                                tsMultiplierText = "0.0"
                                clrMultiplierText = "0.75"
                                qualityBonusText = "1.0"
                                chillingDeductionText = "0.0"
                                minFloorRateText = "15.0"
                                differentialMode = false
                            }

                            PresetTemplateChip("🥛 Total Solids (TS) Rate", "Base ₹20 + TS %") {
                                formulaName = "Total Solids Direct Model"
                                baseRateText = "20.0"
                                yieldType = "NONE"
                                fatMultiplierText = "0.0"
                                snfMultiplierText = "0.0"
                                tsMultiplierText = "4.2"
                                clrMultiplierText = "0.0"
                                qualityBonusText = "1.0"
                                minFloorRateText = "15.0"
                                differentialMode = false
                            }
                        }
                    }

                    // Live Equation Formula Card
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = OceanMidnight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("📐 Live Rate Equation:", color = FreshGold, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = FreshGold.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "Active Expression",
                                            color = FreshGold,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                val equationText = when {
                                    yieldType != "NONE" -> {
                                        "Rate (₹/L) = ₹$baseRateText (at ${baseYieldGramsText}g $yieldType) " +
                                                "+ ((${yieldType}_g - ${baseYieldGramsText}g) ÷ 10) × ₹$diffRatePer10gText" +
                                                (if (fatMult > 0) " + (FAT × $fatMultiplierText)" else "") +
                                                (if (snfMult > 0) " + (SNF × $snfMultiplierText)" else "") +
                                                (if (bonus > 0) " + Bonus(₹$qualityBonusText)" else "") +
                                                (if (deduction > 0) " - Deduction(₹$chillingDeductionText)" else "") +
                                                " [Floor: ₹$minFloorRateText]"
                                    }
                                    else -> {
                                        "Rate (₹/L) = ₹$baseRateText " +
                                                "+ (${if (differentialMode) "(FAT - Std)" else "FAT"} × $fatMultiplierText) " +
                                                "+ (${if (differentialMode) "(SNF - Std)" else "SNF"} × $snfMultiplierText)" +
                                                (if (tsMult > 0) " + (TS × $tsMultiplierText)" else "") +
                                                (if (clrMult > 0) " + (CLR × $clrMultiplierText)" else "") +
                                                (if (bonus > 0) " + Bonus(₹$qualityBonusText)" else "") +
                                                (if (deduction > 0) " - Deduction(₹$chillingDeductionText)" else "") +
                                                " [Floor: ₹$minFloorRateText]"
                                    }
                                }

                                Text(
                                    text = equationText,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.5.sp,
                                    lineHeight = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Formula Name
                    item {
                        OutlinedTextField(
                            value = formulaName,
                            onValueChange = { formulaName = it },
                            label = { Text("Formula Name / ID *") },
                            placeholder = { Text("e.g. My Farm Khoa Rate Chart") },
                            leadingIcon = { Icon(Icons.Default.Label, contentDescription = null, tint = RoyalBluePrimary) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    // 1. Byproduct Yield Benchmarks (Indian Halwai / Mandi standard)
                    item {
                        Text("1. Primary Rate Basis (Yield or Fat/SNF):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Choose if milk price is derived from Khoa/Paneer yield in grams or standard FAT/SNF testing:", fontSize = 11.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "KHOA" to "🍬 Khoa / Mawa",
                                "PANEER" to "🧀 Paneer",
                                "GHEE" to "🏺 Ghee",
                                "NONE" to "🧪 FAT / SNF Only"
                            ).forEach { (type, lbl) ->
                                val isSel = yieldType == type
                                FilterChip(
                                    selected = isSel,
                                    onClick = {
                                        yieldType = type
                                        if (type == "KHOA") {
                                            baseYieldGramsText = "160.0"
                                            baseRateText = "45.0"
                                            diffRatePer10gText = "2.8"
                                        } else if (type == "PANEER") {
                                            baseYieldGramsText = "140.0"
                                            baseRateText = "44.0"
                                            diffRatePer10gText = "3.0"
                                        } else if (type == "GHEE") {
                                            baseYieldGramsText = "60.0"
                                            baseRateText = "42.0"
                                            diffRatePer10gText = "7.0"
                                        }
                                    },
                                    label = { Text(lbl, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Yield Base & Differential Settings
                    if (yieldType != "NONE") {
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "🌾 $yieldType Yield Benchmark Parameters:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = RoyalBluePrimary
                                    )

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = baseYieldGramsText,
                                            onValueChange = { baseYieldGramsText = it },
                                            label = { Text("Standard Yield (g/L)") },
                                            placeholder = { Text("e.g. 160 or 140") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp)
                                        )

                                        OutlinedTextField(
                                            value = baseRateText,
                                            onValueChange = { baseRateText = it },
                                            label = { Text("Base Price (₹/L)") },
                                            placeholder = { Text("e.g. 45 or 44") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }

                                    OutlinedTextField(
                                        value = diffRatePer10gText,
                                        onValueChange = { diffRatePer10gText = it },
                                        label = { Text("Rate Adjustment per 10 grams difference (±₹ / 10g)") },
                                        placeholder = { Text("e.g. 2.80 or 3.00") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.White,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "💡 Benchmark Rule: If milk yields $baseYieldGramsText gm $yieldType → Price is ₹$baseRateText/L. For every +10g higher yield, price increases by ₹$diffRatePer10gText/L. For every -10g lower yield, price decreases by ₹$diffRatePer10gText/L.",
                                            fontSize = 10.5.sp,
                                            color = TextSecondary,
                                            lineHeight = 14.sp,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Base Starting Rate (If FAT/SNF mode)
                    if (yieldType == "NONE") {
                        item {
                            Text("2. Base Starting Rate (₹ / Liter):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            OutlinedTextField(
                                value = baseRateText,
                                onValueChange = { baseRateText = it },
                                label = { Text("Base Amount (₹)") },
                                leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null, tint = RoyalBluePrimary) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    // 3. FAT Component
                    item {
                        Text("3. FAT (%) Multiplier & Differential:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = !differentialMode,
                                onClick = { differentialMode = false },
                                label = { Text("Direct Multiplier (FAT × X)", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = differentialMode,
                                onClick = { differentialMode = true },
                                label = { Text("Differential ((FAT-Std) × X)", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = fatMultiplierText,
                            onValueChange = { fatMultiplierText = it },
                            label = { Text("FAT Multiplier (₹ per 1.0% Fat, 0 if using yield)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // 4. SNF Component
                    item {
                        Text("4. SNF (%) Multiplier:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        OutlinedTextField(
                            value = snfMultiplierText,
                            onValueChange = { snfMultiplierText = it },
                            label = { Text("SNF Multiplier (₹ per 1.0% SNF, 0 if using yield)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // 5. Total Solids & CLR
                    item {
                        Text("5. Total Solids (TS) & Lactometer (CLR) Weights:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = tsMultiplierText,
                                onValueChange = { tsMultiplierText = it },
                                label = { Text("TS (Fat+SNF) ₹/%") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = clrMultiplierText,
                                onValueChange = { clrMultiplierText = it },
                                label = { Text("CLR ₹/point") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    // 6. Quality Bonus, Chilling & Floor Protection
                    item {
                        Text("6. Quality Bonus, Chilling & Floor Protection:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = qualityBonusText,
                                onValueChange = { qualityBonusText = it },
                                label = { Text("Bonus (+₹/L)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = chillingDeductionText,
                                onValueChange = { chillingDeductionText = it },
                                label = { Text("Chilling (–₹/L)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = minFloorRateText,
                            onValueChange = { minFloorRateText = it },
                            label = { Text("Minimum Floor Protection Rate (₹/L)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Live Standard Benchmark Matrix
                    item {
                        Text("📊 Live Benchmark Test Matrix:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            BenchmarkCard(
                                title = "🐄 Cow Milk",
                                subtitle = "4.0% FAT / 8.5% SNF",
                                price = cowBenchmarkPrice,
                                modifier = Modifier.weight(1f)
                            )
                            BenchmarkCard(
                                title = "🐃 Buffalo",
                                subtitle = "6.8% FAT / 9.0% SNF",
                                price = buffaloBenchmarkPrice,
                                modifier = Modifier.weight(1f)
                            )
                            BenchmarkCard(
                                title = "🍶 Rich Sample",
                                subtitle = "7.5% FAT / 9.2% SNF",
                                price = richSamplePrice,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Interactive Live Simulator with Yield Grams & Payout
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = OceanBlueLight.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, OceanBlueAccent.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🧪 Live Yield & Rate Simulator:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RoyalBluePrimary)
                                    Text("FAT: ${String.format(Locale.US, "%.1f", simFat)}% | SNF: ${String.format(Locale.US, "%.1f", simSnf)}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }

                                if (yieldType != "NONE") {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = RoyalBluePrimary.copy(alpha = 0.1f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Calculated $yieldType Yield:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                                            Text("${String.format(Locale.US, "%.1f", simulatedYieldGrams)} gm / Liter", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = RoyalBluePrimary)
                                        }
                                    }
                                }

                                Text("Slide FAT % to test payout:", fontSize = 10.5.sp, color = TextSecondary)
                                Slider(
                                    value = simFat,
                                    onValueChange = { simFat = it },
                                    valueRange = 2.5f..10.0f,
                                    steps = 75,
                                    colors = SliderDefaults.colors(thumbColor = RoyalBluePrimary, activeTrackColor = RoyalBluePrimary)
                                )

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Resulting Price for Sample:", fontSize = 11.sp, color = TextSecondary)
                                            Text(
                                                text = "₹${String.format(Locale.US, "%.2f", simulatedPrice)} / L",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = GrassGreen
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("10 L Payout / Bill:", fontSize = 11.sp, color = TextSecondary)
                                            Text(
                                                text = "₹${String.format(Locale.US, "%.2f", simulatedPrice * 10)}",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = RoyalBluePrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Footer Save Action
                Surface(
                    tonalElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                viewModel.saveFullCustomFormula(
                                    formulaName = formulaName.trim().ifBlank { "Custom Rate Formula" },
                                    baseRate = baseRate,
                                    fatMultiplier = fatMult,
                                    snfMultiplier = snfMult,
                                    tsMultiplier = tsMult,
                                    clrMultiplier = clrMult,
                                    qualityBonus = bonus,
                                    chillingDeduction = deduction,
                                    minFloorRate = minFloor,
                                    yieldType = yieldType,
                                    yieldMultiplier = yieldMult,
                                    differentialMode = differentialMode,
                                    baseYieldGrams = baseYieldGrams,
                                    diffRatePer10g = diffRatePer10g
                                )
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.3f).height(48.dp).testTag("save_custom_formula_btn")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Apply Formula ✓", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetTemplateChip(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(subtitle, fontSize = 9.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun BenchmarkCard(
    title: String,
    subtitle: String,
    price: Double,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(subtitle, fontSize = 8.5.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(3.dp))
            Text("₹${String.format(Locale.US, "%.2f", price)}/L", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = RoyalBluePrimary)
        }
    }
}
