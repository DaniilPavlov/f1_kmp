package com.example.f1_kmp.data.firebase

/**
 * Инициализация Firebase + Analytics + Crashlytics + Remote Config.
 *
 * GoF Creational Singleton — одна точка bootstrap на процесс.
 *
 * [initializeSync] — быстрый sync без сети.
 * [fetchRemoteConfig] — сеть; вызывать с background, иначе ANR / долгий старт UI.
 * [recordNonFatal] — non-fatal в Crashlytics; сетевые сбои отфильтровываются.
 */
expect object FirebaseBootstrap {
    fun initializeSync()

    suspend fun fetchRemoteConfig(remoteConfig: IRemoteConfigService)

    fun recordNonFatal(throwable: Throwable)
}
