package com.example.f1_kmp.domain

import platform.Foundation.NSUserDefaults

actual class ThemePreferences {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun load(): AppThemePreference =
        when (defaults.stringForKey(KEY_APP_THEME)) {
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
        defaults.setObject(raw, KEY_APP_THEME)
    }

    private companion object {
        const val KEY_APP_THEME = "app_theme_preference"
    }
}
