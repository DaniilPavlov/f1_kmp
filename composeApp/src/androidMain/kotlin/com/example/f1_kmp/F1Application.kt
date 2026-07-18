package com.example.f1_kmp

import android.app.Application
import com.example.f1_kmp.di.androidModule
import com.example.f1_kmp.di.appModule
import com.example.f1_kmp.domain.LocaleController
import com.example.f1_kmp.domain.LocalePreferences
import com.example.f1_kmp.notifications.RaceReminderScheduler
import com.example.f1_kmp.platform.AndroidContextHolder
import com.example.f1_kmp.platform.OsmdroidInitializer
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * Точка входа Android-приложения (указана в AndroidManifest).
 *
 * Здесь:
 * 1. сохраняем Application Context для файлового кэша и openUrl;
 * 2. инициализируем локаль;
 * 3. настраиваем OSMDroid;
 * 4. стартуем Koin;
 * 5. синхронизируем напоминания о сессиях.
 */
class F1Application : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidContextHolder.init(this)
        LocaleController.init(LocalePreferences())
        OsmdroidInitializer.ensureInitialized(this)
        startKoin {
            androidContext(this@F1Application)
            modules(appModule, androidModule)
        }
        get<RaceReminderScheduler>().sync()
    }
}
