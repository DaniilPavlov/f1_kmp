package com.example.f1_kmp.domain

import android.content.Context
import com.example.f1_kmp.platform.AndroidContextHolder

actual class ThemePreferences {
    private val preferences =
        AndroidContextHolder.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    actual fun load(): AppThemePreference =
        when (preferences.getString(KEY_APP_THEME, "system")) {
            "light" -> AppThemePreference.Light
            "dark" -> AppThemePreference.Dark
            else -> AppThemePreference.System
        }

    actual fun save(preference: AppThemePreference) {
        val raw = when (preference) {
            AppThemePreference.System -> "system"
            AppThemePreference.Light -> "light"
            AppThemePreference.Dark -> "dark"
        }
        preferences.edit().putString(KEY_APP_THEME, raw).apply()
    }

    private companion object {
        const val PREFERENCES = "f1_preferences"
        const val KEY_APP_THEME = "app_theme_preference"
    }
}
