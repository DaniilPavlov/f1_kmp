package com.example.f1_kmp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Цветовая схема Material 3 — primary/кнопки/поля берут F1-красный и белый.
 * Кастомные экраны чаще используют [F1Black]/[F1Red] напрямую из [AppTheme].
 */
private val LightColorScheme = lightColorScheme(
    primary = F1Red,
    onPrimary = F1White,
    // На Android видна "общая" подложка (серый фон); на iOS без этого появляются
    // белые полосы сверху/снизу там, где Scaffold не перерисовывает safe-area.
    background = F1GrayBg,
    surface = F1GrayBg,
    onBackground = F1Black,
    onSurface = F1Black,
)

/**
 * Корневая тема shared-UI.
 *
 * В чистом Android-проекте статус-бар красили через SideEffect внутри темы.
 * В KMP статус-бар — платформенная деталь: на Android — [com.example.f1_kmp.MainActivity],
 * на iOS управляет система / Swift host. Здесь только MaterialTheme для Compose.
 */
@Composable
fun F1Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content,
    )
}
