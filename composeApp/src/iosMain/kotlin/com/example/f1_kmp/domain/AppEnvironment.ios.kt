package com.example.f1_kmp.domain

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import platform.Foundation.NSCurrentLocaleDidChangeNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSUserDefaults

private const val LANG_KEY = "AppleLanguages"

actual object LocalAppLocale {
    // Дефолт приложения — русский, не системный язык устройства.
    private val default: String = "ru"
    private val LocalAppLocaleState = staticCompositionLocalOf { default }

    actual val current: String
        @Composable get() = LocalAppLocaleState.current

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        val new = value ?: default
        val defaults = NSUserDefaults.standardUserDefaults
        if (value == null) {
            defaults.removeObjectForKey(LANG_KEY)
        } else {
            defaults.setObject(arrayListOf(new), LANG_KEY)
            defaults.synchronize()
            NSNotificationCenter.defaultCenter.postNotificationName(
                aName = NSCurrentLocaleDidChangeNotification,
                `object` = null,
            )
        }
        return LocalAppLocaleState.provides(new)
    }
}

@Composable
actual fun ProvideLocalizedContext(
    language: String,
    content: @Composable () -> Unit,
) {
    content()
}
