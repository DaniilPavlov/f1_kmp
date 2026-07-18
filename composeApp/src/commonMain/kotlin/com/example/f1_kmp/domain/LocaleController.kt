package com.example.f1_kmp.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Смена языка без пересоздания Activity/ViewController и без `key(locale)`
 * вокруг всего UI (иначе моргание, как при recreate).
 *
 * Compose слушает [language] через [stringResource] / [AppEnvironment].
 */
object LocaleController {
    private val _language = MutableStateFlow("ru")
    val language: StateFlow<String> = _language.asStateFlow()

    private lateinit var preferences: LocalePreferences

    fun init(preferences: LocalePreferences) {
        this.preferences = preferences
        val saved = preferences.loadLanguage()
        applyLanguage(saved, persist = false)
    }

    fun toggle(): String {
        val next = if (_language.value == "ru") "en" else "ru"
        applyLanguage(next, persist = true)
        return next
    }

    private fun applyLanguage(language: String, persist: Boolean) {
        val normalized = language.takeIf { it in SUPPORTED } ?: "ru"
        if (persist) {
            preferences.saveLanguage(normalized)
        }
        // Сначала платформенный Locale, потом StateFlow — к моменту рекомпозиции
        // compose-resources уже видит новый Locale.current / preferredLanguages.
        applyPlatformLocale(normalized)
        _language.value = normalized
    }

    private val SUPPORTED = setOf("ru", "en")
}

expect class LocalePreferences {
    fun loadLanguage(): String
    fun saveLanguage(language: String)
}

internal expect fun applyPlatformLocale(language: String)
