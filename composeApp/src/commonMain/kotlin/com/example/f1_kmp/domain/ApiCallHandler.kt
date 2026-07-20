package com.example.f1_kmp.domain

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.HttpStatusCode
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.delay
import kotlinx.io.IOException

/**
 * Единая обёртка для сетевых вызовов в Repository.
 *
 * При сетевой ошибке ([IOException] и родственные) — один повтор через [RETRY_DELAY_MS].
 * Типичный случай: первый запрос упал (холодный DNS/SSL), повтор сразу прошёл.
 *
 * Все ошибки превращаются в [AppException] с локализованным текстом для UI.
 */
object ApiCallHandler {

    private const val DEFAULT_RETRIES = 1
    private const val RETRY_DELAY_MS = 400L

    /**
     * Выполняет [block] с одним повтором при сетевой ошибке.
     * Все исключения превращаются в [AppException].
     */
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
            title = ErrorStrings.serverSlow,
            subtitle = ErrorStrings.noConnectionSubtitle,
        )
        is UnresolvedAddressException,
        is IOException,
        -> AppException(
            title = ErrorStrings.noConnection,
            subtitle = ErrorStrings.noConnectionSubtitle,
        )
        is ClientRequestException -> AppException(
            title = if (e.response.status == HttpStatusCode.TooManyRequests) {
                ErrorStrings.tooManyRequests
            } else {
                ErrorStrings.responseParseError
            },
            subtitle = e.message,
        )
        else -> AppException(
            title = ErrorStrings.responseParseError,
            subtitle = e.message,
        )
    }
}

/** Локализованные сообщения для domain/repository без Context. */
object ErrorStrings {
    private val isEnglish: Boolean
        get() = LocaleController.language.value == "en"

    val noConnection get() = if (isEnglish) "No connection" else "Соединение отсутствует"
    val noConnectionSubtitle get() = if (isEnglish) {
        "Once the connection is restored, you will be able to use the app again"
    } else {
        "Как только соединение восстановится, вы снова сможете пользоваться приложением"
    }
    val serverSlow get() = if (isEnglish) "Server is taking too long to respond" else "Сервер долго не отвечает"
    val tooManyRequests get() = if (isEnglish) "Too many requests" else "Слишком много запросов"
    val responseParseError get() = if (isEnglish) "Error processing the server response" else "Ошибка при обработке ответа от сервера"
    val raceNotFoundTitle get() = if (isEnglish) "Race not found" else "Гонка не найдена"
    val raceNotFound get() = if (isEnglish) {
        "No races found for your query. Check the entered data and try again."
    } else {
        "По вашему запросу гонок не найдено. Проверьте введенные данные и попробуйте еще раз."
    }
    val circuitNotFound get() = if (isEnglish) "Circuit not found" else "Трасса не найдена"
    val driverNotFound get() = if (isEnglish) "Driver not found" else "Пилот не найден"
    val constructorNotFound get() = if (isEnglish) "Constructor not found" else "Конструктор не найден"
}

/** Локализованные названия сессий для [com.example.f1_kmp.viewmodel.ScheduleViewModel]. */
object SessionStrings {
    private val isEnglish: Boolean
        get() = LocaleController.language.value == "en"

    val race get() = if (isEnglish) "Race" else "Гонка"
    val firstPractice get() = if (isEnglish) "First practice" else "Первая практика"
    val secondPractice get() = if (isEnglish) "Second practice" else "Вторая практика"
    val thirdPractice get() = if (isEnglish) "Third practice" else "Третья практика"
    val sprintQualifying get() = if (isEnglish) "Sprint qualifying" else "Спринт-квалификация"
    val sprint get() = if (isEnglish) "Sprint" else "Спринт"
    val qualifying get() = if (isEnglish) "Qualifying" else "Квалификация"
}
