package com.example.f1_kmp.domain

import androidx.compose.runtime.Composable

/** Android: подмена Context/Configuration для stringResource. iOS: no-op. */
@Composable
expect fun ProvideLocalizedContext(
    language: String,
    content: @Composable () -> Unit,
)
