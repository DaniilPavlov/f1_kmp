package com.example.f1_kmp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.f1_kmp.domain.AppThemePreference
import com.example.f1_kmp.domain.ThemeController

private fun lightScheme(colors: AppColors) = lightColorScheme(
    primary = colors.red,
    onPrimary = F1OnChrome,
    background = colors.grayBg,
    surface = colors.grayBg,
    onBackground = colors.black,
    onSurface = colors.black,
    outline = colors.strokeGray,
    onSurfaceVariant = colors.textGray,
)

private fun darkScheme(colors: AppColors) = darkColorScheme(
    primary = colors.red,
    onPrimary = F1OnChrome,
    background = colors.grayBg,
    surface = colors.grayBg,
    onBackground = colors.black,
    onSurface = colors.black,
    outline = colors.strokeGray,
    onSurfaceVariant = colors.textGray,
)

/**
 * Корневая тема shared-UI: system / light / dark через [ThemeController].
 *
 * Статус-бар — платформенная деталь (Android MainActivity / iOS host).
 */
@Composable
fun F1Theme(
    darkTheme: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val preference by ThemeController.preference.collectAsState()
    val systemDark = isSystemInDarkTheme()
    val useDark = darkTheme ?: when (preference) {
        AppThemePreference.System -> systemDark
        AppThemePreference.Light -> false
        AppThemePreference.Dark -> true
    }
    val colors = if (useDark) AppColors.Dark else AppColors.Light
    val colorScheme = if (useDark) darkScheme(colors) else lightScheme(colors)

    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}
