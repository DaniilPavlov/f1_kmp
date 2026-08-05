package com.example.f1_kmp.di

import com.example.f1_kmp.data.analytics.AnalyticsGateway
import com.example.f1_kmp.data.analytics.AppAnalyticsGateway
import com.example.f1_kmp.data.firebase.IRemoteConfigService
import com.example.f1_kmp.data.firebase.RemoteConfigService
import com.example.f1_kmp.domain.ForceUpdateGate
import com.example.f1_kmp.notifications.RaceReminderScheduler
import com.example.f1_kmp.widgets.AppWidgetSyncService
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** Android-only зависимости (Firebase RC, уведомления и т.п.). */
val androidModule = module {
    single<AnalyticsGateway> { AppAnalyticsGateway() }
    single<IRemoteConfigService> { RemoteConfigService() }
    single { ForceUpdateGate(get()) }
    single { RaceReminderScheduler(androidContext(), get(), get(), get()) }
    single { AppWidgetSyncService(androidContext(), get()) }
}
