package com.example.f1_kmp.platform

import android.content.Context
import org.osmdroid.config.Configuration
import java.io.File

/**
 * Одноразовая настройка OSMDroid до первого [org.osmdroid.views.MapView].
 *
 * Без user-agent OpenStreetMap не отдаёт тайлы (будут только маркеры на пустом фоне).
 * Кэш кладём в cacheDir приложения — на новых Android внешнее хранилище недоступно.
 */
object OsmdroidInitializer {
    @Volatile
    private var initialized = false

    fun ensureInitialized(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val appContext = context.applicationContext
            val config = Configuration.getInstance()
            config.load(
                appContext,
                appContext.getSharedPreferences("osmdroid", Context.MODE_PRIVATE),
            )
            config.userAgentValue = appContext.packageName
            val basePath = File(appContext.cacheDir, "osmdroid").apply { mkdirs() }
            config.osmdroidBasePath = basePath
            config.osmdroidTileCache = File(basePath, "tiles").apply { mkdirs() }
            initialized = true
        }
    }
}
