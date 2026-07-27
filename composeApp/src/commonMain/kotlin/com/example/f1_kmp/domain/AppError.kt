package com.example.f1_kmp.domain

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.HttpStatusCode
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.io.IOException

/**
 * Пользовательская ошибка для UI (не Exception).
 *
 * Сетевые и прочие сбои приводятся сюда через [Throwable.toAppError].
 */
data class AppError(
    val title: String,
    val subtitle: String? = null,
) {
    fun asException(): AppException = AppException(this)

    fun toAsyncError(): AsyncValue.Error = AsyncValue.Error(title, subtitle)
}

/**
 * Throwable-обёртка для [Result.failure], чтобы донести [AppError] через стандартный Result.
 */
data class AppException(
    val error: AppError,
) : Exception(error.title) {
    val title: String get() = error.title
    val subtitle: String? get() = error.subtitle

    constructor(title: String, subtitle: String? = null) : this(AppError(title, subtitle))
}

/** Единая точка: любой [Throwable] → понятная [AppError] для экрана. */
fun Throwable.toAppError(): AppError = when (this) {
    is AppException -> error
    is SocketTimeoutException,
    is ConnectTimeoutException,
    is HttpRequestTimeoutException,
    -> AppError(
        title = ErrorStrings.serverSlow,
        subtitle = ErrorStrings.noConnectionSubtitle,
    )
    is UnresolvedAddressException,
    is IOException,
    -> AppError(
        title = ErrorStrings.noConnection,
        subtitle = ErrorStrings.noConnectionSubtitle,
    )
    is ClientRequestException -> AppError(
        title = if (response.status == HttpStatusCode.TooManyRequests) {
            ErrorStrings.tooManyRequests
        } else {
            ErrorStrings.responseParseError
        },
        subtitle = ErrorStrings.errorRetrySubtitle,
    )
    else -> AppError(
        title = ErrorStrings.unexpectedError,
        subtitle = ErrorStrings.errorRetrySubtitle,
    )
}
