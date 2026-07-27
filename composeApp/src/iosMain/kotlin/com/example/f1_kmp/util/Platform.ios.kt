package com.example.f1_kmp.util

import com.example.f1_kmp.notifications.RaceReminderBridge
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

@OptIn(ExperimentalNativeApi::class)
actual fun isDebugBuild(): Boolean = Platform.isDebugBinary

actual fun onLocaleChanged() {
    runCatching { RaceReminderBridge.sync() }
}

/** iOS: открываем только доверенную https-ссылку через [UIApplication.openURL]. */
actual fun openUrl(url: String) {
    val normalized = TrustedUrl.parse(url)
    if (normalized == null) {
        TrustedUrl.logOpenFailure(url)
        return
    }
    val nsUrl = NSURL.URLWithString(normalized)
    if (nsUrl == null) {
        TrustedUrl.logOpenFailure(url)
        return
    }
    UIApplication.sharedApplication.openURL(
        url = nsUrl,
        options = emptyMap<Any?, Any>(),
        completionHandler = null,
    )
}
