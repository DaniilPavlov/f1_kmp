package com.example.f1_kmp.data.firebase

import com.example.f1_kmp.BuildConfig
import com.example.f1_kmp.platform.AndroidContextHolder
import com.example.f1_kmp.util.AppLogger
import com.example.f1_kmp.util.AppVersion
import com.example.f1_kmp.util.appVersionName
import com.google.android.gms.tasks.Task
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Android-обёртка над Firebase Remote Config.
 *
 * GoF Structural Adapter — Task/Firebase API → [IRemoteConfigService].
 */
class RemoteConfigService : IRemoteConfigService {
    private val remoteConfig: FirebaseRemoteConfig by lazy { FirebaseRemoteConfig.getInstance() }

    override val localNotificationsEnabled: Boolean
        get() = remoteConfig.getBoolean(IRemoteConfigService.LOCAL_NOTIFICATIONS_ENABLED_KEY)

    override val minAppVersion: String
        get() = remoteConfig.getString(IRemoteConfigService.MIN_APP_VERSION_KEY)

    override suspend fun init() {
        awaitTask(
            remoteConfig.setConfigSettingsAsync(
                remoteConfigSettings {
                    fetchTimeoutInSeconds = 10
                    minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 0 else 3_600
                },
            ),
        )
        awaitTask(
            remoteConfig.setDefaultsAsync(
                mapOf(
                    IRemoteConfigService.LOCAL_NOTIFICATIONS_ENABLED_KEY to true,
                    IRemoteConfigService.MIN_APP_VERSION_KEY to "0.0.0",
                ),
            ),
        )
        try {
            val activated = awaitTask(remoteConfig.fetchAndActivate())
            AppLogger.d(
                TAG,
                "Remote Config activated=$activated, " +
                    "${IRemoteConfigService.LOCAL_NOTIFICATIONS_ENABLED_KEY}=$localNotificationsEnabled, " +
                    "${IRemoteConfigService.MIN_APP_VERSION_KEY}=$minAppVersion",
            )
        } catch (e: Exception) {
            AppLogger.w(TAG, "Remote Config fetch failed, using defaults", e)
        }
    }

    override suspend fun refresh() {
        try {
            awaitTask(remoteConfig.fetchAndActivate())
        } catch (e: Exception) {
            AppLogger.w(TAG, "Remote Config refresh failed", e)
        }
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

        private suspend fun <T> awaitTask(task: Task<T>): T =
            suspendCancellableCoroutine { cont ->
                task.addOnCompleteListener { completed ->
                    if (completed.isSuccessful) {
                        cont.resume(completed.result)
                    } else {
                        cont.resumeWithException(
                            completed.exception ?: Exception("Firebase Task failed"),
                        )
                    }
                }
            }
    }
}

actual object FirebaseBootstrap {
    private const val TAG = "FirebaseBootstrap"

    actual fun initializeSync() {
        val context = AndroidContextHolder.applicationContext
        if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
            com.google.firebase.FirebaseApp.initializeApp(context)
        }

        com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance()
            .setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        com.google.firebase.analytics.FirebaseAnalytics.getInstance(context)
            .setAnalyticsCollectionEnabled(!BuildConfig.DEBUG)

        AppLogger.d(
            TAG,
            "Firebase core initialized (${com.google.firebase.FirebaseApp.getInstance().options.projectId})",
        )
    }

    actual suspend fun fetchRemoteConfig(remoteConfig: IRemoteConfigService) {
        remoteConfig.init()
    }

    /** Non-fatal: сетевые сбои не отправляем. */
    actual fun recordNonFatal(throwable: Throwable) {
        if (!CrashlyticsReporting.shouldReportUncaughtError(throwable)) return
        runCatching {
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance()
                .recordException(throwable)
        }
    }
}
