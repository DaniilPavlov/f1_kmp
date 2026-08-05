package com.example.f1_kmp.domain

import platform.Foundation.NSUserDefaults

actual class NotificationsPreferenceStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun getBoolean(key: String, default: Boolean): Boolean =
        if (defaults.objectForKey(key) == null) default else defaults.boolForKey(key)

    actual fun putBoolean(key: String, value: Boolean) {
        defaults.setBool(value, key)
    }
}
