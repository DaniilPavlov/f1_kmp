package com.example.f1_kmp.domain

import com.example.f1_kmp.data.analytics.AnalyticsEvent
import com.example.f1_kmp.data.analytics.AnalyticsGateway
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Локальные предпочтения напоминаний.
 */
class NotificationsPreference(
    private val store: NotificationsPreferenceStore,
    private val analytics: AnalyticsGateway,
) {
    private val _raceRemindersEnabled = MutableStateFlow(store.getBoolean(KEY_RACE, true))
    val raceRemindersEnabled: StateFlow<Boolean> = _raceRemindersEnabled.asStateFlow()

    private val _practiceRemindersEnabled = MutableStateFlow(store.getBoolean(KEY_PRACTICE, true))
    val practiceRemindersEnabled: StateFlow<Boolean> = _practiceRemindersEnabled.asStateFlow()

    val canTogglePractice: Boolean
        get() = _raceRemindersEnabled.value

    val effectivelyEnabled: Boolean
        get() = _raceRemindersEnabled.value

    /** Practice on только если race-reminders тоже включены. */
    val practiceRemindersEffectivelyEnabled: Boolean
        get() = effectivelyEnabled && _practiceRemindersEnabled.value

    fun setRaceRemindersEnabled(enabled: Boolean) {
        store.putBoolean(KEY_RACE, enabled)
        _raceRemindersEnabled.update { enabled }
        analytics.log(AnalyticsEvent.RaceReminderToggled(enabled))
    }

    fun setPracticeRemindersEnabled(enabled: Boolean) {
        store.putBoolean(KEY_PRACTICE, enabled)
        _practiceRemindersEnabled.update { enabled }
        analytics.log(AnalyticsEvent.PracticeReminderToggled(enabled))
    }

    companion object {
        const val KEY_RACE = "race_reminders_enabled"
        const val KEY_PRACTICE = "practice_reminders_enabled"
    }
}
