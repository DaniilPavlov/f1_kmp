package com.example.f1_kmp.data.local

import com.example.f1_kmp.platform.AndroidContextHolder
import java.io.File

/**
 * Android-actual файлового кэша: каталог `filesDir/f1_cache`.
 * В Android-приложении вместо Room — JSON-файлы на диске (см. [FileCacheDao]).
 */
actual class PlatformCacheStore {
    private val dir: File by lazy {
        File(AndroidContextHolder.applicationContext.filesDir, "f1_cache").also { it.mkdirs() }
    }

    actual fun readText(fileName: String): String? {
        val file = File(dir, fileName)
        return if (file.exists()) file.readText() else null
    }

    actual fun writeText(fileName: String, content: String) {
        File(dir, fileName).writeText(content)
    }
}
