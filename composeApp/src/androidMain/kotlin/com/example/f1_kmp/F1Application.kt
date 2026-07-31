package com.example.f1_kmp

import android.app.Application
import com.example.f1_kmp.data.appmetrica.AppMetricaBootstrap
import com.example.f1_kmp.data.firebase.FirebaseBootstrap
import com.example.f1_kmp.data.firebase.IRemoteConfigService
import com.example.f1_kmp.di.androidModule
import com.example.f1_kmp.di.appModule
import com.example.f1_kmp.domain.ForceUpdateGate
import com.example.f1_kmp.domain.LocaleController
import com.example.f1_kmp.domain.LocalePreferences
import com.example.f1_kmp.domain.ThemeController
import com.example.f1_kmp.domain.ThemePreferences
import com.example.f1_kmp.notifications.RaceReminderScheduler
import com.example.f1_kmp.platform.AndroidContextHolder
import com.example.f1_kmp.platform.OsmdroidInitializer
import com.example.f1_kmp.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * Точка входа Android-приложения (указана в AndroidManifest).
 *
 * Здесь:
 * 1. сохраняем Application Context для файлового кэша и openUrl;
 * 2. инициализируем локаль и тему;
 * 3. настраиваем OSMDroid;
 * 4. стартуем Koin;
 * 5. Firebase / AppMetrica (sync) + Remote Config (IO);
 * 6. синхронизируем напоминания о сессиях (если нет force-update).
 */
class F1Application : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        AndroidContextHolder.init(this)
        LocaleController.init(LocalePreferences())
        ThemeController.init(ThemePreferences())
        OsmdroidInitializer.ensureInitialized(this)
        startKoin {
            androidContext(this@F1Application)
            modules(appModule, androidModule)
        }

        // Sync only — Remote Config fetch must not block main (was ANR / failed startup).
        runCatching { FirebaseBootstrap.initializeSync() }
            .onFailure { e -> AppLogger.e(TAG, "Firebase core init failed", e) }
        AppMetricaBootstrap.bootstrap()

        val remoteConfig = get<IRemoteConfigService>()
        val forceUpdateGate = get<ForceUpdateGate>()
        val reminderScheduler = get<RaceReminderScheduler>()

        applicationScope.launch {
            runCatching { FirebaseBootstrap.fetchRemoteConfig(remoteConfig) }
                .onFailure { e -> AppLogger.e(TAG, "Remote Config bootstrap failed", e) }
            forceUpdateGate.check()
            if (!forceUpdateGate.required.value) {
                reminderScheduler.sync()
            }
        }
    }

    private companion object {
        const val TAG = "F1Application"
    }
}
