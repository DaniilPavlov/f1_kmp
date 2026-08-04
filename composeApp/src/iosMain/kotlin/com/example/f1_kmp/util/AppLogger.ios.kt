package com.example.f1_kmp.util

import platform.Foundation.NSLog

// Do not pass Kotlin String as %@ to NSLog — K/N treats the char buffer as an
// ObjC object pointer and crashes (EXC_BAD_ACCESS in objc_opt_respondsToSelector).
internal actual fun platformLogDebug(tag: String, message: String) {
    NSLog("D/$tag: $message")
}

internal actual fun platformLogWarn(tag: String, message: String, throwable: Throwable?) {
    if (throwable != null) {
        NSLog("W/$tag: $message — ${throwable.message ?: throwable}")
    } else {
        NSLog("W/$tag: $message")
    }
}

internal actual fun platformLogError(tag: String, message: String, throwable: Throwable?) {
    if (throwable != null) {
        NSLog("E/$tag: $message — ${throwable.message ?: throwable}")
    } else {
        NSLog("E/$tag: $message")
    }
}
