package com.example.f1_kmp.domain

/** Платформенное хранилище флагов напоминаний (SharedPreferences / UserDefaults). */
expect class NotificationsPreferenceStore() {
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
}
