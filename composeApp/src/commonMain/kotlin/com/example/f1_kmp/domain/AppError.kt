package com.example.f1_kmp.domain

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.JsonConvertException
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException

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
fun Throwable.toAppError(): AppError =
    mapThrowableToAppError(this)
        ?: generateSequence(cause) { it.cause }
            .firstNotNullOfOrNull(::mapThrowableToAppError)
        ?: AppError(
            title = ErrorStrings.unexpectedError,
            subtitle = ErrorStrings.errorRetrySubtitle,
        )

/** Маппинг одного уровня (без обхода cause). */
private fun mapThrowableToAppError(error: Throwable): AppError? = when (error) {
    is AppException -> error.error
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
    is ClientRequestException -> httpStatusError(error.response.status)
    is ResponseException -> httpStatusError(error.response.status)
    is JsonConvertException,
    is SerializationException,
    -> parseError()
    else -> null
}

private fun parseError() = AppError(
    title = ErrorStrings.responseParseError,
    subtitle = ErrorStrings.errorRetrySubtitle,
)

private fun httpStatusError(status: HttpStatusCode) = AppError(
    title = if (status == HttpStatusCode.TooManyRequests) {
        ErrorStrings.tooManyRequests
    } else {
        ErrorStrings.responseParseError
    },
    subtitle = ErrorStrings.errorRetrySubtitle,
)
