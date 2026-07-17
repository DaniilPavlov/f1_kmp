package com.example.f1_kmp.domain

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.delay
import kotlinx.io.IOException

/**
 * Единая обёртка для сетевых вызовов в Repository.
 *
 * При сетевой ошибке ([IOException] и родственные) — один повтор через [RETRY_DELAY_MS].
 * Типичный случай: первый запрос упал (холодный DNS/SSL), повтор сразу прошёл.
 *
 * Все ошибки превращаются в [AppException] с текстом на русском для UI.
 */
object ApiCallHandler {

    private const val DEFAULT_RETRIES = 1
    private const val RETRY_DELAY_MS = 400L

    suspend fun <T> safeCall(
        retries: Int = DEFAULT_RETRIES,
        block: suspend () -> T,
    ): Result<T> {
        var lastError: AppException? = null
        repeat(retries + 1) { attempt ->
            try {
                return Result.success(block())
            } catch (e: Exception) {
                lastError = mapException(e)
                val canRetry = attempt < retries && isRetryable(e)
                if (canRetry) {
                    delay(RETRY_DELAY_MS)
                } else {
                    return Result.failure(lastError!!)
                }
            }
        }
        return Result.failure(lastError!!)
    }

    /** Какие исключения имеет смысл повторить один раз. */
    private fun isRetryable(e: Exception): Boolean =
        e is IOException ||
            e is UnresolvedAddressException ||
            e is SocketTimeoutException ||
            e is ConnectTimeoutException ||
            e is HttpRequestTimeoutException

    /** Сетевые/парсинг-ошибки → понятный [AppException] для UI. */
    private fun mapException(e: Exception): AppException = when (e) {
        is SocketTimeoutException,
        is ConnectTimeoutException,
        is HttpRequestTimeoutException,
        -> AppException(
            title = "Сервер долго не отвечает",
            subtitle = "Проверьте соединение и попробуйте обновить позже",
        )
        is UnresolvedAddressException,
        is IOException,
        -> AppException(
            title = "Соединение отсутствует",
            subtitle = "Как только соединение восстановится, вы снова сможете пользоваться приложением",
        )
        else -> AppException(
            title = "Ошибка при обработке ответа от сервера",
            subtitle = e.message,
        )
    }
}
