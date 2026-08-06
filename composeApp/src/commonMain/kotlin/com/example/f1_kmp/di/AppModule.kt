package com.example.f1_kmp.di

import com.example.f1_kmp.data.api.EspnApiService
import com.example.f1_kmp.data.api.F1ApiService
import com.example.f1_kmp.data.circuits.CircuitStatsRepository
import com.example.f1_kmp.data.local.CacheDao
import com.example.f1_kmp.data.local.CacheJsonMapper
import com.example.f1_kmp.data.local.FileCacheDao
import com.example.f1_kmp.data.repository.AuthRepository
import com.example.f1_kmp.data.repository.EspnRepository
import com.example.f1_kmp.data.repository.F1Repository
import com.example.f1_kmp.data.repository.IAuthRepository
import com.example.f1_kmp.data.repository.IEspnRepository
import com.example.f1_kmp.data.repository.IF1Repository
import com.example.f1_kmp.data.repository.IPredictorLeaderboardRepository
import com.example.f1_kmp.data.repository.IPredictorRepository
import com.example.f1_kmp.data.repository.PredictorLeaderboardRepository
import com.example.f1_kmp.data.repository.PredictorRepository
import com.example.f1_kmp.domain.AppDataRefresh
import com.example.f1_kmp.domain.NetworkReachability
import com.example.f1_kmp.domain.NotificationsPreference
import com.example.f1_kmp.domain.NotificationsPreferenceStore
import com.example.f1_kmp.domain.predictor.PredictorScoringCoordinator
import com.example.f1_kmp.platform.createPlatformHttpClient
import com.example.f1_kmp.util.appVersionName
import com.example.f1_kmp.util.isDebugBuild
import com.example.f1_kmp.viewmodel.AuthViewModel
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
import com.example.f1_kmp.viewmodel.PredictorAuthGateViewModel
import com.example.f1_kmp.viewmodel.PredictorLeaderboardViewModel
import com.example.f1_kmp.viewmodel.PredictorSeasonHistoryViewModel
import com.example.f1_kmp.viewmodel.PredictorViewModel
import com.example.f1_kmp.viewmodel.PredictorWeekendDetailViewModel
import com.example.f1_kmp.viewmodel.ProfileViewModel
import com.example.f1_kmp.viewmodel.RaceInfoScreenViewModel
import com.example.f1_kmp.viewmodel.RaceSearchViewModel
import com.example.f1_kmp.viewmodel.ResultsViewModel
import com.example.f1_kmp.viewmodel.ScheduleViewModel
import com.example.f1_kmp.viewmodel.SeasonRewindViewModel
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

/** Имя приложения в User-Agent для Jolpica F1 API. */
private const val APP_USER_AGENT_NAME = "F1KMP"

/**
 * UA для ESPN Site API.
 * `site.api.espn.com/news` отдаёт 403 на Darwin / Mozilla / кастомные app-UA;
 * overview/search на `site.web.api.espn.com` при этом работают — отсюда «новости в пилоте есть, на Home нет» на iOS.
 */
private const val ESPN_USER_AGENT = "okhttp/4.12.0"

/** Jolpica F1 API — формат `AppName/version` (требование jolpica-f1 с 21.08.2026). */
private val jolpicaUserAgent: String
    get() = "$APP_USER_AGENT_NAME/${appVersionName()}"

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
    userAgent: String,
    withJolpicaHeaders: Boolean = false,
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
        // header() заменяет дефолтный UA движка (OkHttp/Darwin).
        header("User-Agent", userAgent)
        if (withJolpicaHeaders) {
            header("system", "kmp")
            header("version", appVersionName())
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
            userAgent = jolpicaUserAgent,
            withJolpicaHeaders = true,
        )
    }

    /** Отдельный HttpClient для ESPN (другой base URL и UA, допустимый для site.api). */
    single(qualifier = named("espn")) {
        createApiClient(
            json = get(),
            baseUrl = ESPN_BASE_URL,
            timeouts = EspnTimeouts,
            userAgent = ESPN_USER_AGENT,
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
    single { com.example.f1_kmp.domain.live.LiveWeekendController(get()) }
    single { com.example.f1_kmp.data.deeplink.DeepLinkBus() }

    single { NetworkReachability() }
    single { NotificationsPreferenceStore() }
    single { NotificationsPreference(get(), get()) }
    single<IAuthRepository> { AuthRepository() }
    single<IPredictorRepository> { PredictorRepository(get()) }
    single<IPredictorLeaderboardRepository> { PredictorLeaderboardRepository(get()) }
    single { PredictorScoringCoordinator(get()) }
}

val viewModelModule = module {
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { ResultsViewModel(get(), get(), get(), get()) }
    viewModel { HallOfFameViewModel(get()) }
    viewModel { ScheduleViewModel(get(), get(), get()) }
    viewModel { CircuitsViewModel(get(), get(), get()) }
    viewModel { RaceSearchViewModel(get()) }
    viewModel { NewsViewModel(get(), get()) }
    viewModel { H2hDriversViewModel(get()) }
    viewModel { H2hConstructorsViewModel(get()) }
    viewModel { FinishStatusViewModel(get()) }
    viewModel { SeasonRewindViewModel(get(), get()) }
    viewModel { AuthViewModel(get()) }
    viewModel { ProfileViewModel(get(), get(), get(), get()) }
    viewModel { PredictorAuthGateViewModel(get()) }
    viewModel { PredictorViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { (season: String, round: String, raceName: String) ->
        PredictorWeekendDetailViewModel(season, round, raceName, get(), get())
    }
    viewModel { (year: String) -> PredictorSeasonHistoryViewModel(year, get()) }
    viewModel { (year: String, myPoints: Int) -> PredictorLeaderboardViewModel(year, myPoints, get()) }
    viewModel { (season: String, round: String) -> RaceInfoScreenViewModel(season, round, get()) }
    viewModel { (circuitId: String) -> CircuitDetailViewModel(circuitId, get(), get()) }
    viewModel { (driverId: String) -> DriverDetailViewModel(driverId, get(), get()) }
    viewModel { (constructorId: String) -> ConstructorDetailViewModel(constructorId, get(), get()) }
}

val appModule = module {
    includes(networkModule, viewModelModule)
}
