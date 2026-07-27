package com.example.f1_kmp.util

import android.util.Log

internal actual fun platformLogDebug(tag: String, message: String) {
    Log.d(tag, message)
}

internal actual fun platformLogWarn(tag: String, message: String, throwable: Throwable?) {
    if (throwable != null) Log.w(tag, message, throwable) else Log.w(tag, message)
}

internal actual fun platformLogError(tag: String, message: String, throwable: Throwable?) {
    if (throwable != null) Log.e(tag, message, throwable) else Log.e(tag, message)
}
