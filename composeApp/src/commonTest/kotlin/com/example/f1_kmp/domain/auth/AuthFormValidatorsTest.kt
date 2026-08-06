package com.example.f1_kmp.domain.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuthFormValidatorsTest {
    @Test
    fun password_requiresLengthLetterAndDigit() {
        assertFalse(AuthFormValidators.isPasswordStrongEnough("short1"))
        assertFalse(AuthFormValidators.isPasswordStrongEnough("longenough"))
        assertFalse(AuthFormValidators.isPasswordStrongEnough("12345678"))
        assertTrue(AuthFormValidators.isPasswordStrongEnough("Password1"))
        assertTrue(AuthFormValidators.isPasswordStrongEnough("пароль1ab"))
    }

    @Test
    fun email_format() {
        assertTrue(AuthFormValidators.isEmailFormatOk("a@b.co"))
        assertFalse(AuthFormValidators.isEmailFormatOk("not-an-email"))
        assertFalse(AuthFormValidators.isEmailFormatOk("@b.com"))
    }

    @Test
    fun disposable_domains() {
        assertTrue(AuthFormValidators.isDisposableEmail("x@mailinator.com"))
        assertTrue(AuthFormValidators.isDisposableEmail("x@sub.yopmail.com"))
        assertFalse(AuthFormValidators.isDisposableEmail("user@gmail.com"))
    }

    @Test
    fun emailDomain_extracts() {
        assertEquals("gmail.com", AuthFormValidators.emailDomain("User@Gmail.com"))
        assertEquals(null, AuthFormValidators.emailDomain("nodomain"))
    }
}
