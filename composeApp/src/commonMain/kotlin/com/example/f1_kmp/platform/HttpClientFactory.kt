package com.example.f1_kmp.platform

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

/**
 * Создаёт Ktor [HttpClient] с платформенным движком.
 *
 * GoF Creational Factory Method — создание продукта ([HttpClient]) делегировано
 * платформенным actual-реализациям (Android OkHttp / iOS Darwin); общая конфигурация
 * (таймауты, JSON, base URL) задаётся вызывающим в [com.example.f1_kmp.di.appModule].
 */
expect fun createPlatformHttpClient(config: HttpClientConfig<*>.() -> Unit = {}): HttpClient
