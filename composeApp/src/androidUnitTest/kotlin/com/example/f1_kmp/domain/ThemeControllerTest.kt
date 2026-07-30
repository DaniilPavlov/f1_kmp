package com.example.f1_kmp.domain

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ThemeControllerTest {
    private lateinit var preferences: ThemePreferences

    @Before
    fun setUp() {
        preferences = mockk(relaxed = true)
        every { preferences.load() } returns AppThemePreference.System
        ThemeController.init(preferences)
    }

    @Test
    fun cycle_systemToLightToDarkToSystem() {
        assertEquals("system", ThemeController.preferenceAnalyticsValue())
        assertEquals(AppThemePreference.Light, ThemeController.cycle())
        assertEquals("light", ThemeController.preferenceAnalyticsValue())
        assertEquals(AppThemePreference.Dark, ThemeController.cycle())
        assertEquals("dark", ThemeController.preferenceAnalyticsValue())
        assertEquals(AppThemePreference.System, ThemeController.cycle())
        verify(atLeast = 3) { preferences.save(any()) }
    }
}
