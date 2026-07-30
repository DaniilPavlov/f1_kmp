package com.example.f1_kmp.data.analytics

interface AnalyticsGateway {
    fun log(event: AnalyticsEvent)
}

/** Platform Firebase + AppMetrica implementation. */
expect class AppAnalyticsGateway() : AnalyticsGateway {
    override fun log(event: AnalyticsEvent)
}
