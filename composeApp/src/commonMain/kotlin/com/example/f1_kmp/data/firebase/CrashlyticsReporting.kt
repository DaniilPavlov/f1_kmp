package com.example.f1_kmp.data.firebase

import com.example.f1_kmp.domain.AppException
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.io.IOException

/**
 * Сетевые/временные ошибки не отправляем в Crashlytics — они не баги приложения.
 */
object CrashlyticsReporting {
    fun shouldReportUncaughtError(error: Throwable): Boolean {
        if (isBenignNetworkError(error)) return false
        if (error is AppException) {
            val cause = error.cause
            if (cause != null && isBenignNetworkError(cause)) return false
        }
        val cause = error.cause
        if (cause != null && cause !== error && isBenignNetworkError(cause)) return false
        return true
    }

    private fun isBenignNetworkError(error: Throwable): Boolean =
        error is IOException ||
            error is SocketTimeoutException ||
            error is ConnectTimeoutException ||
            error is HttpRequestTimeoutException ||
            error is UnresolvedAddressException ||
            error is ResponseException
}
