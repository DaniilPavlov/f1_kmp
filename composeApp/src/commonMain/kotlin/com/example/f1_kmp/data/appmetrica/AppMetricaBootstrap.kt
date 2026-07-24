package com.example.f1_kmp.data.appmetrica

/**
 * Инициализация AppMetrica.
 *
 * GoF Creational Singleton — одна точка activate на процесс.
 *
 * Android: ключ из [BuildConfig] / `local.properties` (`appmetrica.apiKey`).
 * iOS: ключ из Info.plist (`AppMetricaApiKey`) / Config.xcconfig.
 * Пустой ключ → skip. Краши отдаём Firebase Crashlytics.
 */
expect object AppMetricaBootstrap {
    fun bootstrap()
}
