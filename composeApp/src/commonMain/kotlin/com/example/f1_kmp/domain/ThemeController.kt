package com.example.f1_kmp.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemePreference {
    System,
    Light,
    Dark,
}

/**
 * Cycle system → light → dark; Compose listens to [preference].
 *
 * GoF Creational Singleton — один экземпляр; prefs через [ThemePreferences] expect/actual.
 */
object ThemeController {
    private val _preference = MutableStateFlow(AppThemePreference.System)
    val preference: StateFlow<AppThemePreference> = _preference.asStateFlow()

    private lateinit var preferences: ThemePreferences

    fun init(preferences: ThemePreferences) {
        this.preferences = preferences
        _preference.value = preferences.load()
    }

    fun cycle(): AppThemePreference {
        val next = when (_preference.value) {
            AppThemePreference.System -> AppThemePreference.Light
            AppThemePreference.Light -> AppThemePreference.Dark
            AppThemePreference.Dark -> AppThemePreference.System
        }
        preferences.save(next)
        _preference.value = next
        return next
    }

    fun preferenceAnalyticsValue(): String = when (_preference.value) {
        AppThemePreference.System -> "system"
        AppThemePreference.Light -> "light"
        AppThemePreference.Dark -> "dark"
    }
}

/** Платформенное хранилище темы (SharedPreferences / UserDefaults). */
expect class ThemePreferences {
    fun load(): AppThemePreference
    fun save(preference: AppThemePreference)
}
