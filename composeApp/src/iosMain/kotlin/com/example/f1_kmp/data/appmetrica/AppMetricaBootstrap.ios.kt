package com.example.f1_kmp.data.appmetrica

import com.example.f1_kmp.util.isDebugBuild
import platform.Foundation.NSLog

/**
 * iOS AppMetrica activate делает Swift ([AnalyticsBootstrap]) по ключу Info.plist.
 *
 * GoF Creational Singleton — [bootstrap] на Kotlin — no-op (host уже активировал SDK).
 */
actual object AppMetricaBootstrap {
    actual fun bootstrap() {
        if (isDebugBuild()) {
            NSLog("AppMetrica iOS: activate delegated to Swift AnalyticsBootstrap")
        }
    }
}
