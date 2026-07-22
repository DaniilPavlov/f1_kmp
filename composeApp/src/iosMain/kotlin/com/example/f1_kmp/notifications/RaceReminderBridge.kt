package com.example.f1_kmp.notifications

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Доступ к [RaceReminderScheduler] из iOS entry point без JVM-only [GlobalContext]. */
internal object RaceReminderBridge : KoinComponent {
    private val scheduler: RaceReminderScheduler by inject()

    fun sync() = scheduler.sync()
}
