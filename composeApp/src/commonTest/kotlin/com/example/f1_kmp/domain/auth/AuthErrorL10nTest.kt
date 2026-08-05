package com.example.f1_kmp.domain.auth

import com.example.f1_kmp.data.repository.AuthErrorKeys
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AuthErrorL10nTest {
    @Test
    fun authErrorStringKey_mapsAllKnownKeys() {
        val cases = listOf(
            AuthErrorKeys.EMPTY_FIELDS to AuthErrorStringKeys.EMPTY_FIELDS,
            AuthErrorKeys.EMPTY_EMAIL to AuthErrorStringKeys.EMPTY_EMAIL,
            AuthErrorKeys.INVALID_EMAIL to AuthErrorStringKeys.INVALID_EMAIL,
            AuthErrorKeys.USER_DISABLED to AuthErrorStringKeys.USER_DISABLED,
            AuthErrorKeys.USER_NOT_FOUND to AuthErrorStringKeys.USER_NOT_FOUND,
            AuthErrorKeys.WRONG_PASSWORD to AuthErrorStringKeys.WRONG_PASSWORD,
            AuthErrorKeys.INVALID_CREDENTIAL to AuthErrorStringKeys.INVALID_CREDENTIAL,
            AuthErrorKeys.EMAIL_IN_USE to AuthErrorStringKeys.EMAIL_IN_USE,
            AuthErrorKeys.WEAK_PASSWORD to AuthErrorStringKeys.WEAK_PASSWORD,
            AuthErrorKeys.DISPOSABLE_EMAIL to AuthErrorStringKeys.DISPOSABLE_EMAIL,
            AuthErrorKeys.TOO_MANY_REQUESTS to AuthErrorStringKeys.TOO_MANY_REQUESTS,
            AuthErrorKeys.NETWORK to AuthErrorStringKeys.NETWORK,
            AuthErrorKeys.GENERIC to AuthErrorStringKeys.GENERIC,
        )
        cases.forEach { (key, expected) ->
            assertEquals(expected, authErrorStringKey(key), key)
        }
        assertEquals(AuthErrorStringKeys.GENERIC, authErrorStringKey("unknown_key"))
        assertNull(authErrorStringKey(null))
    }
}
