package com.example.f1_kmp.data.analytics

import com.example.f1_kmp.util.AppLogger
import com.example.f1_kmp.util.isDebugBuild

/**
 * iOS: Firebase Analytics живёт в Swift SPM ([AnalyticsBootstrap]).
 * Kotlin шлёт события в [IosAnalyticsBridge]; Swift может подписаться.
 * Без подписчика — только debug-лог (как Crashlytics recordNonFatal на iOS).
 */
actual class AppAnalyticsGateway actual constructor() : AnalyticsGateway {
    actual override fun log(event: AnalyticsEvent) {
        val stringParams = event.params.mapValues { (_, v) -> v.toString() }
        IosAnalyticsBridge.log(event.name, stringParams)
        if (isDebugBuild()) {
            AppLogger.d(TAG, "${event.name} ${event.params}")
        }
    }

    private companion object {
        const val TAG = "Analytics"
    }
}

/**
 * Мост Kotlin → Swift для typed analytics.
 * Swift может выставить [handler] и пробросить в FirebaseAnalytics / AppMetrica.
 */
object IosAnalyticsBridge {
    var handler: ((String, Map<String, String>) -> Unit)? = null

    fun log(name: String, params: Map<String, String>) {
        handler?.invoke(name, params)
    }
}
