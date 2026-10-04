package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainAppScaffold
import com.example.ui.MilkMateViewModel
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.MilkMateTheme

class MainActivity : FragmentActivity() {

    private val viewModel: MilkMateViewModel by viewModels {
        MilkMateViewModel.Factory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val session by viewModel.sessionState.collectAsStateWithLifecycle()
            val isDark = when (session.themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            // Synchronize Android Activity system locale dynamically
            androidx.compose.runtime.LaunchedEffect(session.languageCode) {
                com.example.ui.util.LocaleHelper.applyLocale(this@MainActivity, session.languageCode)
            }

            MilkMateTheme(
                darkTheme = isDark,
                accent = session.accentTheme
            ) {
                androidx.compose.runtime.key(session.languageCode) {
                    CompositionLocalProvider(LocalAppLanguage provides session.languageCode) {
                        Surface(modifier = Modifier.fillMaxSize()) {
                            if (session.isLoggedIn && session.businessId.isNotBlank()) {
                                if (session.isAppLocked) {
                                    AppLockScreen(viewModel = viewModel)
                                } else {
                                    MainAppScaffold(viewModel = viewModel)
                                }
                            } else {
                                AuthScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
