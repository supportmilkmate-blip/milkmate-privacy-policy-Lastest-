package com.example.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.example.ui.theme.LocalAppLanguage
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date

object LocalizedDateTimeFormatter {

    fun formatDate(timestamp: Long, pattern: String = "dd MMM yyyy", langCode: String = "en"): String {
        val locale = LocaleHelper.getLocaleForLanguage(langCode)
        val sdf = SimpleDateFormat(pattern, locale)
        return sdf.format(Date(timestamp))
    }

    fun formatCurrentMonth(yearMonth: String, langCode: String = "en"): String {
        // yearMonth is like "2026-10"
        return try {
            val parts = yearMonth.split("-")
            val year = parts[0]
            val month = parts[1].toInt()
            val cal = java.util.Calendar.getInstance()
            cal.set(year.toInt(), month - 1, 1)
            val locale = LocaleHelper.getLocaleForLanguage(langCode)
            val sdf = SimpleDateFormat("MMMM yyyy", locale)
            sdf.format(cal.time)
        } catch (e: Exception) {
            yearMonth
        }
    }

    fun formatCurrency(amount: Double, langCode: String = "en"): String {
        val locale = LocaleHelper.getLocaleForLanguage(langCode)
        return try {
            val formatter = NumberFormat.getCurrencyInstance(locale)
            formatter.format(amount)
        } catch (e: Exception) {
            "₹%.2f".format(amount)
        }
    }
}

@Composable
@ReadOnlyComposable
fun formatLocalizedDate(timestamp: Long, pattern: String = "dd MMMM yyyy"): String {
    val lang = LocalAppLanguage.current
    return LocalizedDateTimeFormatter.formatDate(timestamp, pattern, lang)
}

@Composable
@ReadOnlyComposable
fun formatLocalizedMonth(yearMonth: String): String {
    val lang = LocalAppLanguage.current
    return LocalizedDateTimeFormatter.formatCurrentMonth(yearMonth, lang)
}
