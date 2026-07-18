package com.example.f1_kmp.di

import com.example.f1_kmp.notifications.RaceReminderScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** Android-only зависимости (уведомления и т.п.). */
val androidModule = module {
    single { RaceReminderScheduler(androidContext(), get()) }
}
