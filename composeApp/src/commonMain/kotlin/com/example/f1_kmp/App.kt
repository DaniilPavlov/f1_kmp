package com.example.f1_kmp

import androidx.compose.runtime.Composable
import com.example.f1_kmp.ui.navigation.F1App
import com.example.f1_kmp.ui.theme.F1Theme

/**
 * Корневой Composable shared-UI (и Android, и iOS вызывают его).
 *
 * Слои снаружи внутрь:
 * 1. DI (Koin) — уже стартовал в [F1Application] / [MainViewController];
 * 2. [F1Theme] — Material + брендовые цвета;
 * 3. [F1App] — Scaffold, нижние вкладки, NavHost.
 */
@Composable
fun App() {
    F1Theme {
        F1App()
    }
}
