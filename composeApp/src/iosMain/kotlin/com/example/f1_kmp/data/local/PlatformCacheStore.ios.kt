package com.example.f1_kmp.data.local

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSFileManager
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

/**
 * iOS-actual файлового кэша: `Documents/f1_cache`.
 * Те же JSON-ключи, что и на Android — offline-режим без платформенной БД.
 */
@OptIn(ExperimentalForeignApi::class)
actual class PlatformCacheStore {
    private val dirPath: String by lazy {
        val cacheDir = NSHomeDirectory() + "/Documents/f1_cache"
        NSFileManager.defaultManager.createDirectoryAtPath(
            path = cacheDir,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
        cacheDir
    }

    actual fun readText(fileName: String): String? {
        val path = "$dirPath/$fileName"
        return NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null)
    }

    actual fun writeText(fileName: String, content: String) {
        val path = "$dirPath/$fileName"
        (content as NSString).writeToFile(
            path = path,
            atomically = true,
            encoding = NSUTF8StringEncoding,
            error = null,
        )
    }
}
