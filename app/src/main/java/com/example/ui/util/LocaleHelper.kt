package com.example.ui.util

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

object LocaleHelper {

    fun getLocaleForLanguage(langCode: String): Locale {
        return when (langCode) {
            "hi" -> Locale("hi", "IN")
            "or" -> Locale("or", "IN")
            "bn" -> Locale("bn", "IN")
            "mr" -> Locale("mr", "IN")
            "gu" -> Locale("gu", "IN")
            "te" -> Locale("te", "IN")
            "ta" -> Locale("ta", "IN")
            "kn" -> Locale("kn", "IN")
            "ml" -> Locale("ml", "IN")
            "pa" -> Locale("pa", "IN")
            "as" -> Locale("as", "IN")
            "hr" -> Locale("hi", "IN") // Haryanvi falls back to Hindi for standard formats
            else -> Locale("en", "IN")
        }
    }

    fun applyLocale(activity: Activity, langCode: String) {
        val targetLocale = getLocaleForLanguage(langCode)
        Locale.setDefault(targetLocale)

        val resources = activity.resources
        val config = Configuration(resources.configuration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(targetLocale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = targetLocale
        }

        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    fun wrapContext(context: Context, langCode: String): Context {
        val targetLocale = getLocaleForLanguage(langCode)
        Locale.setDefault(targetLocale)

        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(targetLocale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = targetLocale
        }

        return context.createConfigurationContext(config)
    }
}
