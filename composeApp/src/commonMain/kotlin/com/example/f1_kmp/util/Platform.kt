package com.example.f1_kmp.util

/**
 * Открывает [url] во внешнем браузере / системном обработчике.
 * Реализации: Android Intent, iOS `UIApplication.openURL`.
 */
expect fun openUrl(url: String)

/** true в debug-сборке — для логов Ktor и прочих dev-only вещей. */
expect fun isDebugBuild(): Boolean

/**
 * После смены языка: на Android пересобираем напоминания (тексты сессий зависят от локали).
 * На iOS — no-op.
 */
expect fun onLocaleChanged()
