package com.example.f1_kmp.data.firebase

import com.example.f1_kmp.util.AppLogger
import com.example.f1_kmp.util.AppVersion
import com.example.f1_kmp.util.appVersionName

/**
 * iOS Remote Config: значения приходят из Swift после fetch Firebase RC
 * ([IosRemoteConfigBridge.apply]).
 *
 * GoF Structural Adapter — Swift/Firebase → [IRemoteConfigService].
 * До первого apply работают defaults (как в Android при фейле сети).
 */
class RemoteConfigService : IRemoteConfigService {
    override val localNotificationsEnabled: Boolean
        get() = IosRemoteConfigBridge.localNotificationsEnabled

    override val minAppVersion: String
        get() = IosRemoteConfigBridge.minAppVersion

    override suspend fun init() {
        AppLogger.d(
            TAG,
            "RemoteConfig iOS values: " +
                "${IRemoteConfigService.LOCAL_NOTIFICATIONS_ENABLED_KEY}=$localNotificationsEnabled, " +
                "${IRemoteConfigService.MIN_APP_VERSION_KEY}=$minAppVersion",
        )
    }

    override suspend fun refresh() {
        // Повторный fetch делает Swift при старте; здесь перечитываем уже применённые значения.
        AppLogger.d(TAG, "RemoteConfig iOS refresh: using last applied bridge values")
    }

    override fun isUpdateRequired(): Boolean {
        val installed = appVersionName()
        val required = AppVersion.isLowerThan(installed, minAppVersion)
        AppLogger.d(
            TAG,
            "Version check: installed=$installed, min=$minAppVersion, updateRequired=$required",
        )
        return required
    }

    private companion object {
        const val TAG = "RemoteConfig"
    }
}

/**
 * Мост Kotlin ↔ Swift для Remote Config.
 * Swift вызывает [apply] после `fetchAndActivate`.
 */
object IosRemoteConfigBridge {
    var localNotificationsEnabled: Boolean = true
        private set
    var minAppVersion: String = "0.0.0"
        private set

    fun apply(localNotificationsEnabled: Boolean, minAppVersion: String) {
        this.localNotificationsEnabled = localNotificationsEnabled
        this.minAppVersion = minAppVersion.ifBlank { "0.0.0" }
    }
}

actual object FirebaseBootstrap {
    private const val TAG = "FirebaseBootstrap"

    actual fun initializeSync() {
        // FirebaseApp.configure() вызывается из Swift до Compose (см. AnalyticsBootstrap).
        AppLogger.d(TAG, "FirebaseBootstrap iOS: core init delegated to Swift")
    }

    actual suspend fun fetchRemoteConfig(remoteConfig: IRemoteConfigService) {
        remoteConfig.init()
    }

    actual fun recordNonFatal(throwable: Throwable) {
        if (!CrashlyticsReporting.shouldReportUncaughtError(throwable)) return
        // Crashlytics record на iOS — через Swift bridge при необходимости.
        AppLogger.w(TAG, "recordNonFatal skipped on iOS Kotlin side: ${throwable.message}")
    }
}
