package com.example.f1_kmp.domain

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults

private const val LANG_KEY = "AppleLanguages"

actual object LocalAppLocale {
    private val default = NSLocale.preferredLanguages.first() as String
    private val LocalAppLocaleState = staticCompositionLocalOf { default }

    actual val current: String
        @Composable get() = LocalAppLocaleState.current

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        val new = value ?: default
        if (value == null) {
            NSUserDefaults.standardUserDefaults.removeObjectForKey(LANG_KEY)
        } else {
            NSUserDefaults.standardUserDefaults.setObject(listOf(new), LANG_KEY)
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
