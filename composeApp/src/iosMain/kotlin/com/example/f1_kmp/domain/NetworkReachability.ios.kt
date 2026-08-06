package com.example.f1_kmp.domain

/**
 * iOS: консервативно считаем online, если не уверены
 * (баннер offline-cache не показываем зря).
 */
actual class NetworkReachability {
    actual fun clearMemo() = Unit

    actual fun isOffline(): Boolean = false
}
