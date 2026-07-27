package com.example.f1_kmp.util

/**
 * Единый логгер приложения (только в debug).
 *
 * В release ничего не пишет — меньше шума и риска утечки данных.
 */
object AppLogger {
    fun d(tag: String, message: String) {
        if (isDebugBuild()) platformLogDebug(tag, message)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (!isDebugBuild()) return
        platformLogWarn(tag, message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (!isDebugBuild()) return
        platformLogError(tag, message, throwable)
    }
}

internal expect fun platformLogDebug(tag: String, message: String)

internal expect fun platformLogWarn(tag: String, message: String, throwable: Throwable?)

internal expect fun platformLogError(tag: String, message: String, throwable: Throwable?)
