package com.example.f1_kmp.di

import com.example.f1_kmp.data.api.F1ApiService
import com.example.f1_kmp.data.local.CacheDao
import com.example.f1_kmp.data.local.CacheJsonMapper
import com.example.f1_kmp.data.local.FileCacheDao
import com.example.f1_kmp.data.repository.F1Repository
import com.example.f1_kmp.viewmodel.CircuitDetailViewModel
import com.example.f1_kmp.viewmodel.CircuitsViewModel
import com.example.f1_kmp.viewmodel.HallOfFameViewModel
import com.example.f1_kmp.viewmodel.HomeViewModel
import com.example.f1_kmp.viewmodel.RaceInfoScreenViewModel
import com.example.f1_kmp.viewmodel.RaceSearchViewModel
import com.example.f1_kmp.viewmodel.ResultsViewModel
import com.example.f1_kmp.platform.createPlatformHttpClient
import com.example.f1_kmp.util.isDebugBuild
import com.example.f1_kmp.viewmodel.ScheduleViewModel
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

private const val BASE_URL = "https://api.jolpi.ca/ergast/f1/"

/**
 * Koin-модуль: как создавать зависимости приложения.
 *
 * [single] — один экземпляр на всё приложение (HttpClient, Repository…).
 * [viewModel] — ViewModel с жизненным циклом экрана; параметры маршрута
 * (season/round/circuitId) передаются через `parametersOf(...)` из NavHost.
 */
val appModule = module {
    /** JSON-парсер: неизвестные поля API игнорируем, null не обязателен. */
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        }
    }

    /**
     * Ktor HttpClient: таймауты, логи, base URL Jolpica.
     * Движок платформы (OkHttp / Darwin) подключается в [createPlatformHttpClient].
     */
    single {
        val json: Json = get()
        createPlatformHttpClient {
            expectSuccess = true
            install(ContentNegotiation) {
                json(json)
            }
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                requestTimeoutMillis = 45_000
                socketTimeoutMillis = 30_000
            }
            install(Logging) {
                logger = Logger.SIMPLE
                level = if (isDebugBuild()) LogLevel.INFO else LogLevel.NONE
            }
            defaultRequest {
                url(BASE_URL)
                header("system", "kmp")
                header("version", "1.0")
                header("build-number", "1")
                header("device-id", "deviceID")
            }
        }
    }

    single { F1ApiService(get()) }
    // region offline-кэш (файлы вместо Room)
    single<CacheDao> { FileCacheDao() }
    single { CacheJsonMapper(get()) }
    single { F1Repository(get(), get(), get()) }

    // region ViewModel'и экранов
    viewModel { HomeViewModel(get()) }
    viewModel { ResultsViewModel(get()) }
    viewModel { HallOfFameViewModel(get()) }
    viewModel { ScheduleViewModel(get()) }
    viewModel { CircuitsViewModel(get()) }
    viewModel { RaceSearchViewModel(get()) }
    viewModel { (season: String, round: String) -> RaceInfoScreenViewModel(season, round, get()) }
    viewModel { (circuitId: String) -> CircuitDetailViewModel(circuitId, get()) }
}
