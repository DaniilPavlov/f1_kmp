package com.example.f1_kmp.domain

import android.content.Context
import com.example.f1_kmp.platform.AndroidContextHolder
import java.util.Locale

actual class LocalePreferences {
    private val preferences =
        AndroidContextHolder.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    actual fun loadLanguage(): String =
        preferences.getString(KEY_APP_LOCALE, "ru").orEmpty().takeIf { it in SUPPORTED } ?: "ru"

    actual fun saveLanguage(language: String) {
        preferences.edit().putString(KEY_APP_LOCALE, language).apply()
    }

    private companion object {
        const val PREFERENCES = "f1_preferences"
        const val KEY_APP_LOCALE = "app_locale"
        val SUPPORTED = setOf("ru", "en")
    }
}

internal actual fun applyPlatformLocale(language: String) {
    Locale.setDefault(Locale.forLanguageTag(language))
}
