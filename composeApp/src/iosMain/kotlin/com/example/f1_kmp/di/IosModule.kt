package com.example.f1_kmp.di

import com.example.f1_kmp.notifications.RaceReminderScheduler
import org.koin.dsl.module

/** iOS-only зависимости (уведомления и т.п.). */
val iosModule = module {
    single { RaceReminderScheduler(get()) }
}
