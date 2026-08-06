package com.example.f1_kmp.domain

import android.content.Context
import com.example.f1_kmp.platform.AndroidContextHolder

actual class NotificationsPreferenceStore {
    private val prefs =
        AndroidContextHolder.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    actual fun getBoolean(key: String, default: Boolean): Boolean =
        prefs.getBoolean(key, default)

    actual fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    private companion object {
        const val PREFS = "f1_preferences"
    }
}
