package com.example.f1_kmp.domain

import platform.Foundation.NSUserDefaults

actual class LocalePreferences {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun loadLanguage(): String {
        val saved = defaults.stringForKey(KEY_APP_LOCALE) ?: return "ru"
        return saved.takeIf { it in SUPPORTED } ?: "ru"
    }

    actual fun saveLanguage(language: String) {
        defaults.setObject(language, KEY_APP_LOCALE)
    }

    private companion object {
        const val KEY_APP_LOCALE = "app_locale"
        val SUPPORTED = setOf("ru", "en")
    }
}

internal actual fun applyPlatformLocale(language: String) {
    val langKey = "AppleLanguages"
    NSUserDefaults.standardUserDefaults.setObject(listOf(language), langKey)
}
