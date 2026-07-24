package com.example.f1_kmp.util

import platform.Foundation.NSBundle

actual fun appVersionName(): String =
    (NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String)
        .orEmpty()
        .ifBlank { "0.0.0" }
