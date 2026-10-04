package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.local.entity.DeliveryEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExportManager(private val context: Context) {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val fileTimestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    private fun getReportsDirectory(): File {
        val dir = File(context.filesDir, "reports")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Generates a real native Android PDF report for Deliveries/Collections.
     */
    fun generateDeliveriesPdf(
        businessName: String,
        reportTitle: String,
        deliveries: List<DeliveryEntity>
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 (in points: 595 x 842)
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(13, 71, 161) // Dark Blue
            textSize = 18f
            isFakeBoldText = true
        }

        val subTitlePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 12f
        }

        val headerPaint = Paint().apply {
            color = Color.rgb(21, 101, 192)
            textSize = 10f
            isFakeBoldText = true
        }

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9f
        }

        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        var y = 40f
        canvas.drawText("MILKMATE — $businessName", 40f, y, titlePaint)
        y += 20f
        canvas.drawText("$reportTitle (Generated: ${dateFormat.format(Date())})", 40f, y, subTitlePaint)
        y += 25f

        // Table Header
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 15f
        canvas.drawText("Date", 40f, y, headerPaint)
        canvas.drawText("Customer", 110f, y, headerPaint)
        canvas.drawText("Shift", 250f, y, headerPaint)
        canvas.drawText("Milk", 310f, y, headerPaint)
        canvas.drawText("Qty (L)", 370f, y, headerPaint)
        canvas.drawText("Rate", 440f, y, headerPaint)
        canvas.drawText("Total (₹)", 500f, y, headerPaint)
        y += 5f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 15f

        var totalQty = 0.0
        var totalAmt = 0.0

        for (d in deliveries) {
            if (y > 800f) break // Single page fit or truncate cleanly
            val dateStr = dateFormat.format(Date(d.deliveryDate))
            canvas.drawText(dateStr, 40f, y, textPaint)
            val nameDisplay = if (d.customerName.length > 22) d.customerName.substring(0, 20) + ".." else d.customerName
            canvas.drawText(nameDisplay, 110f, y, textPaint)
            canvas.drawText(d.shift.take(3), 250f, y, textPaint)
            canvas.drawText(d.milkType.take(4), 310f, y, textPaint)
            canvas.drawText(String.format(Locale.US, "%.1f", d.quantityLiters), 370f, y, textPaint)
            canvas.drawText(String.format(Locale.US, "%.1f", d.ratePerLiter), 440f, y, textPaint)
            canvas.drawText(String.format(Locale.US, "%.2f", d.totalAmount), 500f, y, textPaint)
            y += 16f
            totalQty += d.quantityLiters
            totalAmt += d.totalAmount
        }

        // Summary footer
        y += 10f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 15f
        val summaryPaint = Paint().apply {
            color = Color.rgb(13, 71, 161)
            textSize = 10f
            isFakeBoldText = true
        }
        canvas.drawText("GRAND TOTAL:", 40f, y, summaryPaint)
        canvas.drawText(String.format(Locale.US, "%.1f L", totalQty), 370f, y, summaryPaint)
        canvas.drawText(String.format(Locale.US, "₹%.2f", totalAmt), 500f, y, summaryPaint)

        pdfDocument.finishPage(page)

        val fileName = "milkmate_report_${fileTimestampFormat.format(Date())}.pdf"
        val file = File(getReportsDirectory(), fileName)
        val fos = FileOutputStream(file)
        pdfDocument.writeTo(fos)
        fos.flush()
        fos.close()
        pdfDocument.close()

        return file
    }

    /**
     * Generates a real Excel-compatible CSV file for reports.
     */
    fun generateDeliveriesCsv(
        reportTitle: String,
        deliveries: List<DeliveryEntity>
    ): File {
        val fileName = "milkmate_report_${fileTimestampFormat.format(Date())}.csv"
        val file = File(getReportsDirectory(), fileName)

        file.bufferedWriter().use { writer ->
            writer.write("sep=,\n") // Hint to MS Excel to parse comma delimiter cleanly
            writer.write("Date,Customer Name,Customer Type,Shift,Milk Type,Quantity Liters,Rate Per Liter,Total Amount,FAT,SNF,Status\n")
            for (d in deliveries) {
                val dateStr = dateFormat.format(Date(d.deliveryDate))
                val sanitizedName = d.customerName.replace(",", " ")
                writer.write("$dateStr,\"$sanitizedName\",${d.customerType},${d.shift},${d.milkType},${d.quantityLiters},${d.ratePerLiter},${d.totalAmount},${d.fat},${d.snf},${if (d.isDelivered) "DELIVERED" else "CANCELLED"}\n")
            }
        }
        return file
    }

    /**
     * Generates Profit & Loss Statement PDF
     */
    fun generateProfitPdf(
        businessName: String,
        profitReport: ProfitReport
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(13, 71, 161)
            textSize = 18f
            isFakeBoldText = true
        }
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
        }
        val boldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            isFakeBoldText = true
        }
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        var y = 50f
        canvas.drawText("MILKMATE — PROFIT & LOSS STATEMENT", 40f, y, titlePaint)
        y += 20f
        canvas.drawText("Business: $businessName", 40f, y, textPaint)
        y += 18f
        val periodStr = "${dateFormat.format(Date(profitReport.startDate))} to ${dateFormat.format(Date(profitReport.endDate))}"
        canvas.drawText("Period: $periodStr", 40f, y, textPaint)
        y += 25f

        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 20f

        canvas.drawText("1. REVENUE (Milk Sales OUT):", 40f, y, boldPaint)
        canvas.drawText(String.format(Locale.US, "₹%.2f", profitReport.milkSalesRevenue), 440f, y, boldPaint)
        y += 25f

        canvas.drawText("2. DIRECT COSTS (Milk Purchases IN):", 40f, y, boldPaint)
        canvas.drawText(String.format(Locale.US, "₹%.2f", profitReport.milkPurchaseCost), 440f, y, boldPaint)
        y += 25f

        canvas.drawText("3. OPERATING EXPENSES:", 40f, y, boldPaint)
        canvas.drawText(String.format(Locale.US, "₹%.2f", profitReport.operatingExpenses), 440f, y, boldPaint)
        y += 18f

        for ((cat, amt) in profitReport.expenseBreakdown) {
            canvas.drawText("   • $cat", 60f, y, textPaint)
            canvas.drawText(String.format(Locale.US, "₹%.2f", amt), 440f, y, textPaint)
            y += 16f
        }

        y += 15f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 22f

        val profitColor = if (profitReport.netProfit >= 0) Color.rgb(46, 125, 50) else Color.rgb(198, 40, 40)
        val profitPaint = Paint().apply {
            color = profitColor
            textSize = 14f
            isFakeBoldText = true
        }

        canvas.drawText("NET PROFIT / (LOSS):", 40f, y, profitPaint)
        canvas.drawText(String.format(Locale.US, "₹%.2f", profitReport.netProfit), 440f, y, profitPaint)
        y += 20f
        canvas.drawText("Profit Margin: ${profitReport.profitMarginPercent}%", 40f, y, boldPaint)

        pdfDocument.finishPage(page)

        val fileName = "milkmate_profit_${fileTimestampFormat.format(Date())}.pdf"
        val file = File(getReportsDirectory(), fileName)
        val fos = FileOutputStream(file)
        pdfDocument.writeTo(fos)
        fos.flush()
        fos.close()
        pdfDocument.close()

        return file
    }

    /**
     * Generates a native PDF report for 3-Month Consumption Forecast & Purchase Recommendations.
     */
    fun generateForecastPdf(
        businessName: String,
        summary: ConsumptionForecastSummary
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(13, 71, 161)
            textSize = 17f
            isFakeBoldText = true
        }

        val subTitlePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 11f
        }

        val headerPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9.5f
            isFakeBoldText = true
        }

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9f
        }

        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        val cardBgPaint = Paint().apply {
            color = Color.rgb(240, 244, 248)
        }

        // Header
        canvas.drawText(businessName.ifBlank { "MilkMate Dairy" }, 40f, 40f, titlePaint)
        canvas.drawText("3-Month Consumption Forecast & Purchase Order Advice", 40f, 58f, subTitlePaint)
        val dateStr = dateFormat.format(Date(summary.generatedAt))
        canvas.drawText("Generated: $dateStr • Planning Horizon: ${summary.planningHorizonDays} Days (Safety Buffer: ${summary.safetyBufferDays}d)", 40f, 72f, subTitlePaint)

        // Summary Metric Box
        canvas.drawRect(40f, 85f, 555f, 130f, cardBgPaint)
        val boldCardPaint = Paint().apply {
            color = Color.rgb(13, 71, 161)
            textSize = 10f
            isFakeBoldText = true
        }
        val cardTextPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 8.5f
        }

        canvas.drawText("Critical Reorders", 50f, 102f, cardTextPaint)
        canvas.drawText("${summary.criticalItemsCount} Items", 50f, 118f, boldCardPaint)

        canvas.drawText("Warning (5-15d)", 170f, 102f, cardTextPaint)
        canvas.drawText("${summary.warningItemsCount} Items", 170f, 118f, boldCardPaint)

        canvas.drawText("90-Day Milk Dispatched", 280f, 102f, cardTextPaint)
        canvas.drawText(String.format(Locale.US, "%.0f L (Trend: %+.1f%%)", summary.total90DayMilkDelivered, summary.milkTrendPercent), 280f, 118f, boldCardPaint)

        canvas.drawText("Est. Reorder Capital", 420f, 102f, cardTextPaint)
        canvas.drawText(String.format(Locale.US, "₹%.0f", summary.totalEstimatedReorderCost), 420f, 118f, boldCardPaint)

        var y = 150f

        // Table Header
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 12f
        canvas.drawText("ITEM / FEED NAME", 40f, y, headerPaint)
        canvas.drawText("STOCK", 175f, y, headerPaint)
        canvas.drawText("RUNS OUT", 230f, y, headerPaint)
        canvas.drawText("DAILY BURN", 295f, y, headerPaint)
        canvas.drawText("SUGGESTED ORDER", 370f, y, headerPaint)
        canvas.drawText("EST. COST", 485f, y, headerPaint)
        y += 6f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 14f

        for (item in summary.itemForecasts) {
            if (y > 780f) break

            val statusColor = when (item.urgencyStatus) {
                ForecastUrgency.CRITICAL -> Color.rgb(198, 40, 40)
                ForecastUrgency.WARNING -> Color.rgb(230, 81, 0)
                ForecastUrgency.ADEQUATE -> Color.rgb(46, 125, 50)
                ForecastUrgency.WELL_STOCKED -> Color.rgb(21, 101, 192)
            }
            val statusPaint = Paint().apply {
                color = statusColor
                textSize = 8.5f
                isFakeBoldText = true
            }

            canvas.drawText(item.item.itemName.take(22), 40f, y, textPaint)
            canvas.drawText("${String.format(Locale.US, "%.1f", item.currentStock)} ${item.unit}", 175f, y, textPaint)
            
            val daysStr = if (item.daysRemaining <= 0) "OUT OF STOCK" else "${String.format(Locale.US, "%.1f", item.daysRemaining)}d"
            canvas.drawText(daysStr, 230f, y, statusPaint)

            canvas.drawText("${String.format(Locale.US, "%.1f", item.effectiveProjectedDailyUsage)}/d", 295f, y, textPaint)
            canvas.drawText(item.suggestedPackageUnits.take(18), 370f, y, statusPaint)
            canvas.drawText(String.format(Locale.US, "₹%.0f", item.estimatedPurchaseCost), 485f, y, textPaint)

            y += 14f
            if (item.demandInsight.isNotBlank()) {
                val insightPaint = Paint().apply {
                    color = Color.GRAY
                    textSize = 7.5f
                }
                canvas.drawText("→ ${item.demandInsight.take(90)}", 50f, y, insightPaint)
                y += 12f
            }
            canvas.drawLine(40f, y - 4f, 555f, y - 4f, linePaint)
        }

        // Footer
        val footerPaint = Paint().apply {
            color = Color.GRAY
            textSize = 8f
        }
        canvas.drawText("Forecast model analyzed 90 days of daily usage logs & milk delivery volumes with regression trend modeling.", 40f, 810f, footerPaint)

        pdfDocument.finishPage(page)

        val fileName = "milkmate_forecast_${fileTimestampFormat.format(Date())}.pdf"
        val file = File(getReportsDirectory(), fileName)
        val fos = FileOutputStream(file)
        pdfDocument.writeTo(fos)
        fos.flush()
        fos.close()
        pdfDocument.close()

        return file
    }

    /**
     * Generates CSV for Consumption Forecast & Purchase Recommendations.
     */
    fun generateForecastCsv(summary: ConsumptionForecastSummary): File {
        val fileName = "milkmate_forecast_${fileTimestampFormat.format(Date())}.csv"
        val file = File(getReportsDirectory(), fileName)
        val writer = file.bufferedWriter()

        writer.appendLine("Item Name,Category,Current Stock,Unit,Days Remaining,Run Out Date,30d Daily Burn,Projected Daily Burn,Suggested Order Qty,Suggested Unit/Bags,Est Cost (INR),Supplier,Demand Insight")

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        for (item in summary.itemForecasts) {
            val runOutDateStr = if (item.runOutDateMillis != null) sdf.format(Date(item.runOutDateMillis)) else "N/A"
            val line = listOf(
                "\"${item.item.itemName}\"",
                "\"${item.category}\"",
                item.currentStock.toString(),
                "\"${item.unit}\"",
                item.daysRemaining.toString(),
                "\"$runOutDateStr\"",
                item.historicalDailyUsage.toString(),
                item.effectiveProjectedDailyUsage.toString(),
                item.suggestedPurchaseQty.toString(),
                "\"${item.suggestedPackageUnits}\"",
                item.estimatedPurchaseCost.toString(),
                "\"${item.lastSupplier}\"",
                "\"${item.demandInsight.replace("\"", "'")}\""
            ).joinToString(",")
            writer.appendLine(line)
        }

        writer.flush()
        writer.close()
        return file
    }

    /**
     * Creates an Android Share / View Intent using FileProvider.
     */
    fun shareFileIntent(file: File, mimeType: String): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
