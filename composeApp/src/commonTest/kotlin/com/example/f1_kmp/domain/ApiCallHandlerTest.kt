package com.example.f1_kmp.domain

import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit-тесты [ApiCallHandler]:
 * - успешный ответ;
 * - один повтор при сетевой ошибке;
 * - понятные русские сообщения для UI.
 */
class ApiCallHandlerTest {

    @Test
    fun safeCall_success_returnsValue() = runTest {
        val result = ApiCallHandler.safeCall { "ok" }
        assertEquals("ok", result.getOrNull())
    }

    @Test
    fun safeCall_retriesIOException_thenSucceeds() = runTest {
        var attempts = 0
        val result = ApiCallHandler.safeCall {
            attempts++
            if (attempts == 1) throw SocketTimeoutException("timeout", null)
            "ok"
        }
        assertEquals("ok", result.getOrNull())
        assertEquals(2, attempts)
    }

    @Test
    fun safeCall_timeoutAfterRetries_returnsServerSlowMessage() = runTest {
        val result = ApiCallHandler.safeCall(retries = 1) {
            throw SocketTimeoutException("timeout", null)
        }
        val error = result.exceptionOrNull() as AppException
        assertEquals("Сервер долго не отвечает", error.title)
    }

    @Test
    fun safeCall_unresolvedHost_returnsNoConnectionMessage() = runTest {
        val result = ApiCallHandler.safeCall { throw UnresolvedAddressException() }
        val error = result.exceptionOrNull() as AppException
        assertEquals("Соединение отсутствует", error.title)
    }

    @Test
    fun safeCall_genericException_returnsUnexpectedError() = runTest {
        val result = ApiCallHandler.safeCall { throw IllegalStateException("bad json") }
        val error = result.exceptionOrNull() as AppException
        assertEquals("Неожиданная ошибка", error.title)
        assertEquals("Попробуйте обновить экран.", error.subtitle)
    }

    @Test
    fun safeCall_ioException_returnsNoConnectionMessage() = runTest {
        val result = ApiCallHandler.safeCall { throw IOException("broken pipe") }
        val error = result.exceptionOrNull() as AppException
        assertEquals("Соединение отсутствует", error.title)
    }

    @Test
    fun errorStrings_tooManyRequests_defaultsToRussian() {
        assertEquals("Слишком много запросов", ErrorStrings.tooManyRequests)
    }
}
