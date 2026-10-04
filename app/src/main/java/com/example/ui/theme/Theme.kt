package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val LocalAppLanguage = compositionLocalOf { "en" }

fun getAccentColors(accent: String, isDark: Boolean): Pair<Color, Color> {
    return when (accent.uppercase()) {
        "GREEN" -> if (isDark) Pair(Color(0xFF6EE7B7), Color(0xFF064E3B)) else Pair(Color(0xFF059669), Color(0xFFECFDF5))
        "GOLD" -> if (isDark) Pair(Color(0xFFFCD34D), Color(0xFF78350F)) else Pair(Color(0xFFD97706), Color(0xFFFFFBEB))
        "PURPLE" -> if (isDark) Pair(Color(0xFFC4B5FD), Color(0xFF4C1D95)) else Pair(Color(0xFF7C3AED), Color(0xFFF5F3FF))
        "TEAL" -> if (isDark) Pair(Color(0xFF5EEAD4), Color(0xFF134E4A)) else Pair(Color(0xFF0F766E), Color(0xFFF0FDFA))
        "RED" -> if (isDark) Pair(Color(0xFFFCA5A5), Color(0xFF7F1D1D)) else Pair(Color(0xFFDC2626), Color(0xFFFEF2F2))
        else -> if (isDark) Pair(Color(0xFF93C5FD), Color(0xFF1E3A8A)) else Pair(Color(0xFF1E3A8A), Color(0xFFEFF6FF))
    }
}

fun createCustomLightColorScheme(accent: String): ColorScheme {
    val (primary, primaryContainer) = getAccentColors(accent, false)
    return lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = primaryContainer,
        onPrimaryContainer = primary,
        secondary = FreshGold,
        onSecondary = Color.White,
        secondaryContainer = FreshGoldLight,
        onSecondaryContainer = Color(0xFFE65100),
        tertiary = DairyGreen,
        onTertiary = Color.White,
        tertiaryContainer = DairyGreenLight,
        onTertiaryContainer = DairyGreen,
        background = Color(0xFFF8FAFC),
        onBackground = Color(0xFF0F172A),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF0F172A),
        surfaceVariant = Color(0xFFF1F5F9),
        onSurfaceVariant = Color(0xFF475569),
        outline = Color(0xFFE2E8F0),
        outlineVariant = Color(0xFFCBD5E1)
    )
}

fun createCustomDarkColorScheme(accent: String): ColorScheme {
    val (primary, primaryContainer) = getAccentColors(accent, true)
    return darkColorScheme(
        primary = primary,
        onPrimary = Color(0xFF0F172A),
        primaryContainer = primaryContainer,
        onPrimaryContainer = Color(0xFFE2E8F0),
        secondary = Color(0xFFFBBF24),
        onSecondary = Color(0xFF451A03),
        secondaryContainer = Color(0xFF78350F),
        onSecondaryContainer = Color(0xFFFEF3C7),
        tertiary = Color(0xFF34D399),
        onTertiary = Color(0xFF064E3B),
        tertiaryContainer = Color(0xFF065F46),
        onTertiaryContainer = Color(0xFFD1FAE5),
        background = Color(0xFF0F172A),
        onBackground = Color(0xFFF8FAFC),
        surface = Color(0xFF1E293B),
        onSurface = Color(0xFFF8FAFC),
        surfaceVariant = Color(0xFF334155),
        onSurfaceVariant = Color(0xFF94A3B8),
        outline = Color(0xFF475569),
        outlineVariant = Color(0xFF334155)
    )
}

@Composable
fun MilkMateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accent: String = "BLUE",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> createCustomDarkColorScheme(accent)
        else -> createCustomLightColorScheme(accent)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MilkMateTheme(darkTheme = darkTheme, accent = "BLUE", dynamicColor = dynamicColor, content = content)
}
