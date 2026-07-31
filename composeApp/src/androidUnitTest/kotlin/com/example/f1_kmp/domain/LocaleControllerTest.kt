package com.example.f1_kmp.domain

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class LocaleControllerTest {
    private lateinit var preferences: LocalePreferences

    @Before
    fun setUp() {
        preferences = mockk(relaxed = true)
        every { preferences.loadLanguage() } returns "ru"
        LocaleController.init(preferences)
    }

    @Test
    fun toggle_switchesRuEn() {
        assertEquals("ru", LocaleController.language.value)
        assertEquals("en", LocaleController.toggle())
        assertEquals("en", LocaleController.language.value)
        assertEquals("ru", LocaleController.toggle())
        verify(atLeast = 2) { preferences.saveLanguage(any()) }
    }
}
