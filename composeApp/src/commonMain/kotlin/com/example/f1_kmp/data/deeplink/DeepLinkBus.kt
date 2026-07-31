package com.example.f1_kmp.data.deeplink

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Holds pending deep-link targets from cold start / onNewIntent / iOS URL open.
 * [com.example.f1_kmp.ui.navigation.F1App] consumes and navigates.
 */
class DeepLinkBus {
    private val _targets = MutableSharedFlow<DeepLinkTarget>(extraBufferCapacity = 8)
    val targets: SharedFlow<DeepLinkTarget> = _targets.asSharedFlow()

    fun offer(url: String?) {
        val target = url?.toDeepLinkTarget() ?: return
        _targets.tryEmit(target)
    }

    fun offer(target: DeepLinkTarget) {
        _targets.tryEmit(target)
    }
}
