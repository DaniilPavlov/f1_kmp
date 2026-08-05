package com.example.f1_kmp.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineCachedBannerTest {
    @Test
    fun shouldShow_trueOnlyWhenCachedAndOffline() {
        val offline = NetworkReachability().apply { debugIsOfflineOverride = { true } }
        val online = NetworkReachability().apply { debugIsOfflineOverride = { false } }
        assertTrue(shouldShowOfflineCachedBanner(hasCachedContent = true, reachability = offline))
        assertFalse(shouldShowOfflineCachedBanner(hasCachedContent = false, reachability = offline))
        assertFalse(shouldShowOfflineCachedBanner(hasCachedContent = true, reachability = online))
    }

    @Test
    fun clearIfOnline_returnsFalseWhenNetworkBack() {
        val reachability = NetworkReachability().apply { debugIsOfflineOverride = { false } }
        assertFalse(
            clearOfflineBannerIfOnline(currentlyShowing = true, reachability = reachability),
        )
    }

    @Test
    fun clearIfOnline_keepsTrueWhenStillOffline() {
        val reachability = NetworkReachability().apply { debugIsOfflineOverride = { true } }
        assertTrue(
            clearOfflineBannerIfOnline(currentlyShowing = true, reachability = reachability),
        )
    }

    @Test
    fun clearIfOnline_noopWhenNotShowing() {
        val reachability = NetworkReachability().apply { debugIsOfflineOverride = { true } }
        assertFalse(
            clearOfflineBannerIfOnline(currentlyShowing = false, reachability = reachability),
        )
    }
}
