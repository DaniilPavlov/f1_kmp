package com.example.f1_kmp.platform

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

/**
 * Создаёт Ktor [HttpClient] с платформенным движком.
 * Android — OkHttp, iOS — Darwin (NSURLSession).
 * Общая конфигурация (таймауты, JSON, base URL) задаётся в [com.example.f1_kmp.di.appModule].
 */
expect fun createPlatformHttpClient(config: HttpClientConfig<*>.() -> Unit = {}): HttpClient
