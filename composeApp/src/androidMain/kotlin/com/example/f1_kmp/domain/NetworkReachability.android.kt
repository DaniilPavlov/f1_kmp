package com.example.f1_kmp.domain

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.SystemClock
import com.example.f1_kmp.platform.AndroidContextHolder

/**
 * Android: ConnectivityManager INTERNET capability + короткий memo TTL.
 */
actual class NetworkReachability {
    @Volatile
    private var memoized: Boolean? = null

    @Volatile
    private var memoizedAtMs: Long = 0L

    /** Override for unit tests (avoid real ConnectivityManager). */
    var debugIsOfflineOverride: (() -> Boolean)? = null

    actual fun clearMemo() {
        memoized = null
        memoizedAtMs = 0L
    }

    actual fun isOffline(): Boolean {
        debugIsOfflineOverride?.let { return it() }

        val now = SystemClock.elapsedRealtime()
        val cached = memoized
        if (cached != null && now - memoizedAtMs < MEMO_TTL_MS) {
            return cached
        }

        val value = probe()
        memoized = value
        memoizedAtMs = now
        return value
    }

    private fun probe(): Boolean {
        val context = AndroidContextHolder.applicationContext
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return true
        val network = cm.activeNetwork ?: return true
        val caps = cm.getNetworkCapabilities(network) ?: return true
        return !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private companion object {
        const val MEMO_TTL_MS = 2_000L
    }
}
