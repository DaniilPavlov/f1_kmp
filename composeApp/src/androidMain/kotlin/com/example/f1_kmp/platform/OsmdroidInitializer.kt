package com.example.f1_kmp.platform

import android.content.Context
import org.osmdroid.config.Configuration
import java.io.File

/**
 * Настройка OSMDroid до первого [org.osmdroid.views.MapView].
 * Путь кэша — [Context.filesDir] (надёжно на API 30+).
 */
object OsmdroidInitializer {
    @Volatile
    private var initialized = false

    /** Идемпотентная инициализация конфигурации OSMDroid (кэш тайлов, user-agent). */
    fun ensureInitialized(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val appContext = context.applicationContext
            val prefs = appContext.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)

            val basePath = File(appContext.filesDir, "osmdroid").apply { mkdirs() }
            val tileCache = File(basePath, "tiles").apply { mkdirs() }

            val config = Configuration.getInstance()
            config.load(appContext, prefs)
            config.userAgentValue = "${appContext.packageName}/F1KMP"
            config.osmdroidBasePath = basePath
            config.osmdroidTileCache = tileCache
            config.isMapTileDownloaderFollowRedirects = true
            config.save(appContext, prefs)

            initialized = true
        }
    }
}
