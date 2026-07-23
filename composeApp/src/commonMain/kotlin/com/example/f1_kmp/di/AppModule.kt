package com.example.f1_kmp.di

import com.example.f1_kmp.data.api.EspnApiService
import com.example.f1_kmp.data.api.F1ApiService
import com.example.f1_kmp.data.circuits.CircuitStatsRepository
import com.example.f1_kmp.data.local.CacheDao
import com.example.f1_kmp.data.local.CacheJsonMapper
import com.example.f1_kmp.data.local.FileCacheDao
import com.example.f1_kmp.data.repository.EspnRepository
import com.example.f1_kmp.data.repository.F1Repository
import com.example.f1_kmp.data.repository.IEspnRepository
import com.example.f1_kmp.data.repository.IF1Repository
import com.example.f1_kmp.domain.AppDataRefresh
import com.example.f1_kmp.platform.createPlatformHttpClient
import com.example.f1_kmp.util.isDebugBuild
import com.example.f1_kmp.viewmodel.CircuitDetailViewModel
import com.example.f1_kmp.viewmodel.CircuitsViewModel
import com.example.f1_kmp.viewmodel.ConstructorDetailViewModel
import com.example.f1_kmp.viewmodel.DriverDetailViewModel
import com.example.f1_kmp.viewmodel.FinishStatusViewModel
import com.example.f1_kmp.viewmodel.H2hConstructorsViewModel
import com.example.f1_kmp.viewmodel.H2hDriversViewModel
import com.example.f1_kmp.viewmodel.HallOfFameViewModel
import com.example.f1_kmp.viewmodel.HomeViewModel
import com.example.f1_kmp.viewmodel.NewsViewModel
import com.example.f1_kmp.viewmodel.RaceInfoScreenViewModel
import com.example.f1_kmp.viewmodel.RaceSearchViewModel
import com.example.f1_kmp.viewmodel.ResultsViewModel
import com.example.f1_kmp.viewmodel.ScheduleViewModel
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
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
import org.koin.core.qualifier.named
import org.koin.dsl.module

private const val BASE_URL = "https://api.jolpi.ca/ergast/f1/"
private const val ESPN_BASE_URL = EspnApiService.BASE_URL

private data class ApiTimeouts(
    val connectMs: Long,
    val requestMs: Long,
    val socketMs: Long,
)

private val JolpicaTimeouts = ApiTimeouts(15_000, 45_000, 30_000)
private val EspnTimeouts = ApiTimeouts(20_000, 60_000, 40_000)

internal fun HttpClientConfig<*>.installJsonClient(
    json: Json,
    connectMs: Long,
    requestMs: Long,
    socketMs: Long,
) {
    install(ContentNegotiation) {
        json(json)
    }
    install(HttpTimeout) {
        connectTimeoutMillis = connectMs
        requestTimeoutMillis = requestMs
        socketTimeoutMillis = socketMs
    }
    install(Logging) {
        logger = Logger.SIMPLE
        level = if (isDebugBuild()) LogLevel.INFO else LogLevel.NONE
    }
}

/**
 * Общая фабрика Ktor-клиента (Jolpica / ESPN).
 *
 * GoF Creational Factory Method — [createApiClient] собирает [HttpClient] с таймаутами
 * и JSON; платформенный движок скрыт за [createPlatformHttpClient].
 */
private fun createApiClient(
    json: Json,
    baseUrl: String,
    timeouts: ApiTimeouts,
    withAppHeaders: Boolean,
): HttpClient = createPlatformHttpClient {
    expectSuccess = true
    installJsonClient(
        json = json,
        connectMs = timeouts.connectMs,
        requestMs = timeouts.requestMs,
        socketMs = timeouts.socketMs,
    )
    defaultRequest {
        url(baseUrl)
        if (withAppHeaders) {
            header("system", "kmp")
            header("version", "1.0")
            header("build-number", "1")
            header("device-id", "deviceID")
        }
    }
}

/**
 * Koin-модуль: как создавать зависимости приложения.
 *
 * [single] — один экземпляр на всё приложение (HttpClient, Repository…).
 * [viewModel] — ViewModel с жизненным циклом экрана; параметры маршрута
 * (season/round/circuitId) передаются через `parametersOf(...)` из NavHost.
 */
val networkModule = module {
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
        createApiClient(
            json = get(),
            baseUrl = BASE_URL,
            timeouts = JolpicaTimeouts,
            withAppHeaders = true,
        )
    }

    /** Отдельный HttpClient для ESPN (другой base URL, без заголовков Jolpica). */
    single(qualifier = named("espn")) {
        createApiClient(
            json = get(),
            baseUrl = ESPN_BASE_URL,
            timeouts = EspnTimeouts,
            withAppHeaders = false,
        )
    }

    single { F1ApiService(get()) }
    single { EspnApiService(get(named("espn"))) }
    single<IEspnRepository> { EspnRepository(get()) }
    single { CircuitStatsRepository(get()) }

    // region offline-кэш (файлы вместо Room)
    single<CacheDao> { FileCacheDao() }
    single { CacheJsonMapper(get()) }
    single<IF1Repository> { F1Repository(get(), get(), get()) }
    single { AppDataRefresh(get(), get()) }
}

val viewModelModule = module {
    viewModel { HomeViewModel(get(), get()) }
    viewModel { ResultsViewModel(get(), get(), get()) }
    viewModel { HallOfFameViewModel(get()) }
    viewModel { ScheduleViewModel(get()) }
    viewModel { CircuitsViewModel(get(), get()) }
    viewModel { RaceSearchViewModel(get()) }
    viewModel { NewsViewModel(get(), get()) }
    viewModel { H2hDriversViewModel(get()) }
    viewModel { H2hConstructorsViewModel(get()) }
    viewModel { FinishStatusViewModel(get()) }
    viewModel { (season: String, round: String) -> RaceInfoScreenViewModel(season, round, get()) }
    viewModel { (circuitId: String) -> CircuitDetailViewModel(circuitId, get(), get()) }
    viewModel { (driverId: String) -> DriverDetailViewModel(driverId, get(), get()) }
    viewModel { (constructorId: String) -> ConstructorDetailViewModel(constructorId, get(), get()) }
}

val appModule = module {
    includes(networkModule, viewModelModule)
}
