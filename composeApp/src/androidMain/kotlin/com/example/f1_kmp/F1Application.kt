package com.example.f1_kmp

import android.app.Application
import com.example.f1_kmp.di.appModule
import com.example.f1_kmp.platform.AndroidContextHolder
import com.example.f1_kmp.platform.OsmdroidInitializer
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * Точка входа Android-приложения (указана в AndroidManifest).
 *
 * Здесь:
 * 1. сохраняем Application Context для файлового кэша и openUrl;
 * 2. настраиваем OSMDroid (user-agent + путь кэша тайлов) до любой карты;
 * 3. стартуем Koin с [appModule] — дальше ViewModel получают Repository через DI.
 */
class F1Application : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidContextHolder.init(this)
        OsmdroidInitializer.ensureInitialized(this)
        startKoin {
            androidContext(this@F1Application)
            modules(appModule)
        }
    }
}
