package com.example.f1_kmp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.f1_kmp.domain.AppEnvironment
import com.example.f1_kmp.domain.ForceUpdateGate
import com.example.f1_kmp.domain.LocaleController
import com.example.f1_kmp.domain.ProvideLocalizedContext
import com.example.f1_kmp.ui.navigation.F1App
import com.example.f1_kmp.ui.screens.ForceUpdateScreen
import com.example.f1_kmp.ui.theme.F1Theme
import org.koin.compose.koinInject

/**
 * Корневой Composable shared-UI (и Android, и iOS вызывают его).
 *
 * Слои снаружи внутрь:
 * 1. Локаль — [AppEnvironment] + [ProvideLocalizedContext] (Android), без key/recreate;
 * 2. [F1Theme] — Material + брендовые цвета;
 * 3. Force-update gate или [F1App] — Scaffold / вкладки.
 */
@Composable
fun App() {
    val language by LocaleController.language.collectAsState()
    val forceUpdateGate = koinInject<ForceUpdateGate>()
    val forceUpdate by forceUpdateGate.required.collectAsState()

    ProvideLocalizedContext(language) {
        AppEnvironment(locale = language) {
            F1Theme {
                if (forceUpdate) {
                    ForceUpdateScreen()
                } else {
                    F1App()
                }
            }
        }
    }
}
