package com.example.f1_kmp.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppErrorTest {

    @Test
    fun appError_helpers() {
        val error = AppError("title", "sub")
        assertEquals("title", error.asException().title)
        assertEquals("sub", error.asException().subtitle)
        val async = error.toAsyncError()
        assertTrue(async is AsyncValue.Error)
        assertEquals("title", async.message)
        assertEquals("sub", async.subtitle)
    }

    @Test
    fun toAppError_mapsKnownTypes() {
        val wrapped = AppException(AppError("a", "b")).toAppError()
        assertEquals("a", wrapped.title)
        assertEquals("b", wrapped.subtitle)

        val unexpected = RuntimeException("x").toAppError()
        assertEquals(ErrorStrings.unexpectedError, unexpected.title)

        val io = kotlinx.io.IOException("offline").toAppError()
        assertEquals(ErrorStrings.noConnection, io.title)
        assertNull(AppError("only").subtitle)
    }

    @Test
    fun asyncValue_getOrNull() {
        assertEquals(1, AsyncValue.Value(1).getOrNull())
        assertNull((AsyncValue.Loading as AsyncValue<Int>).getOrNull())
        assertNull((AsyncValue.Error("t") as AsyncValue<Int>).getOrNull())
    }
}
