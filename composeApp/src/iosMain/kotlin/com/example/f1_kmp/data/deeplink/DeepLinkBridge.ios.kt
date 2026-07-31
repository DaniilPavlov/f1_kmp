package com.example.f1_kmp.data.deeplink

import org.koin.mp.KoinPlatform.getKoin

/**
 * Swift вызывает [offer] из `onOpenURL` для `f1pet://` links.
 */
object DeepLinkBridge {
    fun offer(url: String) {
        runCatching {
            getKoin().get<DeepLinkBus>().offer(url)
        }
    }
}
