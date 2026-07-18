package com.example.f1_kmp.domain

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource as composeStringResource

/**
 * Обёртка над compose-resources [stringResource].
 *
 * Подписывается на [LocaleController.language], чтобы строки перечитались при смене языка
 * без `key(locale)` вокруг всего UI (тот пересоздаёт NavHost и даёт моргание экрана).
 */
@Composable
fun stringResource(resource: StringResource): String {
    val language by LocaleController.language.collectAsState()
    return key(language) { composeStringResource(resource) }
}

@Composable
fun stringResource(resource: StringResource, vararg formatArgs: Any): String {
    val language by LocaleController.language.collectAsState()
    return key(language) { composeStringResource(resource, *formatArgs) }
}
