package com.example.f1_kmp.domain

import com.example.f1_kmp.data.analytics.AnalyticsEvent
import com.example.f1_kmp.data.analytics.AnalyticsGateway
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NotificationsPreferenceTest {
    private lateinit var store: NotificationsPreferenceStore
    private lateinit var analytics: AnalyticsGateway
    private lateinit var prefs: NotificationsPreference

    @Before
    fun setUp() {
        store = mockk(relaxed = true)
        every { store.getBoolean(any(), any()) } answers { secondArg() }
        analytics = mockk(relaxed = true)
        prefs = NotificationsPreference(store, analytics)
    }

    @Test
    fun defaults_enabled() {
        assertTrue(prefs.effectivelyEnabled)
        assertTrue(prefs.practiceRemindersEffectivelyEnabled)
        assertTrue(prefs.canTogglePractice)
    }

    @Test
    fun setRaceRemindersDisabled_disablesPracticeGate() {
        prefs.setRaceRemindersEnabled(false)
        assertFalse(prefs.effectivelyEnabled)
        assertFalse(prefs.canTogglePractice)
        assertFalse(prefs.practiceRemindersEffectivelyEnabled)
        verify { analytics.log(AnalyticsEvent.RaceReminderToggled(false)) }
        verify { store.putBoolean(NotificationsPreference.KEY_RACE, false) }
    }

    @Test
    fun setPracticeReminders_logsAnalytics() {
        prefs.setPracticeRemindersEnabled(false)
        assertFalse(prefs.practiceRemindersEffectivelyEnabled)
        verify { analytics.log(AnalyticsEvent.PracticeReminderToggled(false)) }
        verify { store.putBoolean(NotificationsPreference.KEY_PRACTICE, false) }
    }
}
