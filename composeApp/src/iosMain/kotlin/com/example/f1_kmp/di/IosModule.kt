package com.example.f1_kmp.di

import com.example.f1_kmp.data.analytics.AnalyticsGateway
import com.example.f1_kmp.data.analytics.AppAnalyticsGateway
import com.example.f1_kmp.data.firebase.IRemoteConfigService
import com.example.f1_kmp.data.firebase.RemoteConfigService
import com.example.f1_kmp.domain.ForceUpdateGate
import com.example.f1_kmp.notifications.RaceReminderScheduler
import org.koin.dsl.module

/** iOS-only зависимости (Firebase RC bridge, уведомления и т.п.). */
val iosModule = module {
    single<AnalyticsGateway> { AppAnalyticsGateway() }
    single<IRemoteConfigService> { RemoteConfigService() }
    single { ForceUpdateGate(get()) }
    single { RaceReminderScheduler(get(), get()) }
}
