package com.example.f1_kmp.di

import com.example.f1_kmp.data.firebase.IRemoteConfigService
import com.example.f1_kmp.data.firebase.RemoteConfigService
import com.example.f1_kmp.domain.ForceUpdateGate
import com.example.f1_kmp.notifications.RaceReminderScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** Android-only зависимости (Firebase RC, уведомления и т.п.). */
val androidModule = module {
    single<IRemoteConfigService> { RemoteConfigService() }
    single { ForceUpdateGate(get()) }
    single { RaceReminderScheduler(androidContext(), get(), get()) }
}
