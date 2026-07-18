package com.example.f1_kmp.platform

import android.content.Context
import org.osmdroid.config.Configuration
import java.io.File

/**
 * Настройка OSMDroid до первого [org.osmdroid.views.MapView].
 * load + userAgent = packageName.
 * Путь кэша — [Context.getFilesDir] (всегда доступен на API 30+).
 */
object OsmdroidInitializer {
    @Volatile
    private var initialized = false

    fun ensureInitialized(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val appContext = context.applicationContext
            val prefs = appContext.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)

            // Сбрасываем старые пути (external/cache), из‑за них часто остаётся только сетка.
            prefs.edit().clear().apply()

            val basePath = File(appContext.filesDir, "osmdroid").apply { mkdirs() }
            val tileCache = File(basePath, "tiles").apply { mkdirs() }
            prefs.edit()
                .putString("osmdroid.basePath", basePath.absolutePath)
                .putString("osmdroid.cachePath", tileCache.absolutePath)
                .apply()

            val config = Configuration.getInstance()
            config.load(appContext, prefs)
            config.userAgentValue = appContext.packageName
            config.osmdroidBasePath = basePath
            config.osmdroidTileCache = tileCache
            config.isMapTileDownloaderFollowRedirects = true
            config.save(appContext, prefs)

            initialized = true
        }
    }
}
