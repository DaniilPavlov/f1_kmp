package com.example.f1_kmp.data.firebase

/**
 * Контракт Remote Config для domain/UI.
 *
 * GoF Structural Adapter — реализация прячет Firebase (Android SDK / iOS Firebase)
 * за единым интерфейсом флагов приложения.
 *
 * Ключи (defaults при фейле fetch):
 * - [LOCAL_NOTIFICATIONS_ENABLED_KEY] — bool, default true
 * - [MIN_APP_VERSION_KEY] — semver string, default "0.0.0"
 */
interface IRemoteConfigService {
    val localNotificationsEnabled: Boolean
    val minAppVersion: String

    suspend fun init()

    /** Повторный fetch (например при resume приложения). */
    suspend fun refresh()

    /** `true`, если установленная версия ниже [minAppVersion]. */
    fun isUpdateRequired(): Boolean

    companion object {
        const val LOCAL_NOTIFICATIONS_ENABLED_KEY = "local_notifications_enabled"
        const val MIN_APP_VERSION_KEY = "min_app_version"
    }
}
