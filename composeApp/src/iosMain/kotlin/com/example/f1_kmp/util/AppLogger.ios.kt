package com.example.f1_kmp.util

import platform.Foundation.NSLog

internal actual fun platformLogDebug(tag: String, message: String) {
    NSLog("D/%@: %@", tag, message)
}

internal actual fun platformLogWarn(tag: String, message: String, throwable: Throwable?) {
    if (throwable != null) {
        NSLog("W/%@: %@ — %@", tag, message, throwable.message ?: throwable.toString())
    } else {
        NSLog("W/%@: %@", tag, message)
    }
}

internal actual fun platformLogError(tag: String, message: String, throwable: Throwable?) {
    if (throwable != null) {
        NSLog("E/%@: %@ — %@", tag, message, throwable.message ?: throwable.toString())
    } else {
        NSLog("E/%@: %@", tag, message)
    }
}
