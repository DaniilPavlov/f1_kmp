package com.example.f1_kmp.domain

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue

/**
 * Оборачивает UI для runtime-смены локали compose-resources.
 *
 * Без `key(locale)`: иначе пересоздаётся весь NavHost и экран моргает
 * (как Activity.recreate). Строки обновляются через
 * [stringResource], который слушает [LocaleController].
 *
 * См. https://kotlinlang.org/docs/multiplatform/compose-resource-environment.html
 */
@Composable
fun AppEnvironment(
    locale: String,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalAppLocale provides locale,
    ) {
        content()
    }
}

/** CompositionLocal с текущей локалью для compose-resources. */
expect object LocalAppLocale {
    val current: String
        @Composable get

    @Composable
    infix fun provides(value: String?): ProvidedValue<*>
}
