package com.example.f1_kmp.data.appmetrica

import android.util.Log
import com.example.f1_kmp.BuildConfig
import com.example.f1_kmp.platform.AndroidContextHolder
import io.appmetrica.analytics.AppMetrica
import io.appmetrica.analytics.AppMetricaConfig

/**
 * Android AppMetrica activate.
 *
 * GoF Creational Singleton — [bootstrap] идемпотентен для процесса.
 */
actual object AppMetricaBootstrap {
    private const val TAG = "AppMetricaBootstrap"

    actual fun bootstrap() {
        val apiKey = BuildConfig.APPMETRICA_API_KEY
        if (apiKey.isBlank()) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "AppMetrica skipped (no appmetrica.apiKey in local.properties)")
            }
            return
        }

        try {
            val builder = AppMetricaConfig.newConfigBuilder(apiKey)
                .withCrashReporting(false)
                .withNativeCrashReporting(false)
                .withLocationTracking(false)
            if (BuildConfig.DEBUG) {
                builder.withLogs()
            }
            AppMetrica.activate(AndroidContextHolder.applicationContext, builder.build())
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "AppMetrica activated")
            }
        } catch (e: Exception) {
            Log.e(TAG, "AppMetrica activate failed", e)
        }
    }
}
