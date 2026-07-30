package com.example.f1_kmp.data.repository

import com.example.f1_kmp.data.api.F1ApiService
import com.example.f1_kmp.data.career.CareerLoader
import com.example.f1_kmp.data.local.CacheDao
import com.example.f1_kmp.data.local.CacheEntry
import com.example.f1_kmp.data.local.CacheJsonMapper
import com.example.f1_kmp.data.local.CacheKeys
import com.example.f1_kmp.data.mapper.toConstructorStandingDomain
import com.example.f1_kmp.data.mapper.toDomain
import com.example.f1_kmp.data.mapper.toDriverStandingDomain
import com.example.f1_kmp.data.mapper.toMeta
import com.example.f1_kmp.data.mapper.toRaceDomain
import com.example.f1_kmp.data.model.CareerStats
import com.example.f1_kmp.data.model.CircuitModel
import com.example.f1_kmp.data.model.CircuitRaceWin
import com.example.f1_kmp.data.model.ConstructorStandingsModel
import com.example.f1_kmp.data.model.DriverStandingsCache
import com.example.f1_kmp.data.model.FinishStatusItem
import com.example.f1_kmp.data.model.H2hStats
import com.example.f1_kmp.data.model.HistoricalStandingsCache
import com.example.f1_kmp.data.model.RaceModel
import com.example.f1_kmp.data.model.SeasonsCache
import com.example.f1_kmp.data.model.StandingsListsModel
import com.example.f1_kmp.domain.ApiCallHandler
import com.example.f1_kmp.domain.model.Circuit
import com.example.f1_kmp.domain.model.Constructor
import com.example.f1_kmp.domain.model.ConstructorStanding
import com.example.f1_kmp.domain.model.Driver
import com.example.f1_kmp.domain.model.DriverStanding
import com.example.f1_kmp.domain.model.PitStop
import com.example.f1_kmp.domain.model.QualifyingResult
import com.example.f1_kmp.domain.model.Race
import com.example.f1_kmp.domain.model.RaceResult
import com.example.f1_kmp.domain.model.StandingsMeta
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

/**
 * Repository — единая точка доступа к данным Jolpica для всех экранов.
 *
 * GoF Structural Proxy — [peek*Cache] отдаёт суррогат (файл/memory) до сети;
 * при ошибке сети снова fallback на кэш, если он уже был.
 *
 * Поток данных: Jolpica HTTP → kotlinx DTO (`data.model`) → domain (`domain.model`)
 * через `.toDomain()`. В кэш пишутся DTO; наружу (ViewModel) — domain.
 *
 * **Стратегия:**
 * 1. [peek*Cache] — сразу отдать файловый/memory-кэш (UI не висит на loader);
 * 2. Сеть через [ApiCallHandler.safeCall] с одним автоповтором;
 * 3. При ошибке — fallback на кэш, если он уже был.
 *
 * Пит-стопы: API отдаёт только driverId. Имена кэшируются в памяти,
 * параллельных getDriver не больше [MAX_DRIVER_FETCH_PARALLEL].
 */
class F1Repository(
    private val api: F1ApiService,
    private val cacheDao: CacheDao,
    private val cacheJsonMapper: CacheJsonMapper,
) : IF1Repository {
    private var circuitsMemoryCache: List<CircuitModel>? = null

    private val driverNames = mutableMapOf<String, String>()
    private val driverNamesMutex = Mutex()

    // region peek — мгновенный UI без ожидания сети

    /** Кэш текущей таблицы пилотов (список + сезон/раунд) или `null`. */
    override suspend fun peekCurrentDriversCache(): Pair<List<DriverStanding>, StandingsMeta>? =
        loadCache<DriverStandingsCache>(CacheKeys.CURRENT_DRIVERS)?.let { cached ->
            val list = StandingsListsModel(cached.season, cached.round, cached.drivers, null)
            Pair(cached.drivers.toDriverStandingDomain(), list.toMeta())
        }

    /** Кэш текущей таблицы конструкторов или `null`. */
    override suspend fun peekCurrentConstructorsCache(): List<ConstructorStanding>? =
        loadCacheList<ConstructorStandingsModel>(CacheKeys.CURRENT_CONSTRUCTORS)
            ?.toConstructorStandingDomain()

    /** Кэш последней гонки или `null`. */
    override suspend fun peekLastRaceCache(): Race? =
        loadCache<RaceModel>(CacheKeys.LAST_RACE)?.toDomain()

    /** Кэш расписания текущего сезона или `null`. */
    override suspend fun peekScheduleCache(): List<Race>? =
        loadCacheList<RaceModel>(CacheKeys.SCHEDULE)?.toRaceDomain()

    /** Кэш трасс (memory → файл) или `null`. */
    override suspend fun peekCircuitsCache(): List<Circuit>? {
        circuitsMemoryCache?.let { return it.map { circuit -> circuit.toDomain() } }
        return loadCacheList<CircuitModel>(CacheKeys.CIRCUITS)
            ?.also { circuitsMemoryCache = it }
            ?.map { it.toDomain() }
    }

    /** Кэш «Зал славы» за [year] или `null`. */
    override suspend fun peekHistoricalStandingsCache(
        year: String,
    ): Pair<List<DriverStanding>, List<ConstructorStanding>>? =
        loadCache<HistoricalStandingsCache>(CacheKeys.historicalStandings(year))?.let {
            Pair(it.drivers.toDriverStandingDomain(), it.constructors.toConstructorStandingDomain())
        }

    /** Текущая таблица пилотов: сеть → кэш; при ошибке — fallback на peek. */
    override suspend fun getCurrentDriverStandings(): Result<Pair<List<DriverStanding>, StandingsMeta>> {
        val network = ApiCallHandler.safeCall {
            val list = api.getCurrentDriverStandings().mrData.standingsTable.standingsLists.first()
            Pair(list.driverStandings.orEmpty(), list)
        }
        if (network.isSuccess) {
            network.getOrNull()?.let { (drivers, meta) ->
                saveCache(
                    CacheKeys.CURRENT_DRIVERS,
                    DriverStandingsCache(drivers, meta.season, meta.round),
                )
            }
            return network.map { (drivers, list) ->
                Pair(drivers.toDriverStandingDomain(), list.toMeta())
            }
        }
        return peekCurrentDriversCache()?.let { Result.success(it) }
            ?: network.map { (drivers, list) -> Pair(drivers.toDriverStandingDomain(), list.toMeta()) }
    }

    /** Текущая таблица конструкторов: сеть → кэш; при ошибке — fallback на peek. */
    override suspend fun getCurrentConstructorStandings(): Result<List<ConstructorStanding>> {
        val network = ApiCallHandler.safeCall {
            api.getCurrentConstructorStandings()
                .mrData.standingsTable.standingsLists.first()
                .constructorStandings.orEmpty()
        }
        if (network.isSuccess) {
            network.getOrNull()?.let { saveCacheList(CacheKeys.CURRENT_CONSTRUCTORS, it) }
            return network.map { it.toConstructorStandingDomain() }
        }
        return peekCurrentConstructorsCache()?.let { Result.success(it) }
            ?: network.map { it.toConstructorStandingDomain() }
    }

    /** Последняя завершённая гонка: сеть → кэш; при ошибке — fallback на peek. */
    override suspend fun getLastRace(): Result<Race> {
        val network = ApiCallHandler.safeCall {
            api.getLastRaceResults().mrData.raceTable.races.first()
        }
        if (network.isSuccess) {
            network.getOrNull()?.let { saveCache(CacheKeys.LAST_RACE, it) }
            return network.map { it.toDomain() }
        }
        return peekLastRaceCache()?.let { Result.success(it) }
            ?: network.map { it.toDomain() }
    }

    /** Результаты гонки за [year]/[round] без кэша (поиск / детальная карточка). */
    override suspend fun getRaceResults(year: String, round: String): Result<Race?> =
        ApiCallHandler.safeCall {
            api.getRaceResults(year, round).mrData.raceTable.races.firstOrNull()?.toDomain()
        }

    /** В не-спринтовых уик-эндах API возвращает пустой список, это не ошибка. */
    override suspend fun getSprintResults(year: String, round: String): Result<List<RaceResult>> =
        ApiCallHandler.safeCall {
            api.getSprintResults(year, round).mrData.raceTable.races
                .firstOrNull()
                ?.let { it.sprintResults ?: it.results }
                .orEmpty()
                .map { it.toDomain() }
        }

    /** Результаты квалификации за [year]/[round]; пустой список — не ошибка. */
    override suspend fun getQualifyingResults(year: String, round: String): Result<List<QualifyingResult>> =
        ApiCallHandler.safeCall {
            api.getQualifyingResults(year, round).mrData.raceTable.races
                .firstOrNull()?.qualifyingResults.orEmpty()
                .map { it.toDomain() }
        }

    /**
     * Пит-стопы: один запрос pitstops + уникальные driverId (не каждая остановка).
     * Имена кэшируются в памяти; параллельных getDriver не больше [MAX_DRIVER_FETCH_PARALLEL].
     */
    override suspend fun getPitStopsWithDriverNames(year: String, round: String): Result<List<PitStop>> =
        ApiCallHandler.safeCall {
            val stops = api.getPitStops(year, round).mrData.raceTable.races
                .firstOrNull()?.pitStops.orEmpty()
            if (stops.isEmpty()) return@safeCall emptyList()

            val uniqueDriverIds = stops.map { it.driverId }.distinct()
            val namesById = resolveDriverNames(uniqueDriverIds)
            stops.map { stop ->
                val withName = namesById[stop.driverId]?.let { stop.copy(driverId = it) } ?: stop
                withName.toDomain()
            }
        }

    /** Расписание текущего сезона: сеть → кэш; при ошибке — fallback на peek. */
    override suspend fun getCurrentSchedule(): Result<List<Race>> {
        val network = ApiCallHandler.safeCall {
            api.getCurrentSchedule().mrData.raceTable.races
        }
        if (network.isSuccess) {
            network.getOrNull()?.let { saveCacheList(CacheKeys.SCHEDULE, it) }
            return network.map { it.toRaceDomain() }
        }
        return peekScheduleCache()?.let { Result.success(it) }
            ?: network.map { it.toRaceDomain() }
    }

    /** Итоговые таблицы пилотов и конструкторов за [year] («Зал славы»). */
    override suspend fun getHistoricalStandings(
        year: String,
    ): Result<Pair<List<DriverStanding>, List<ConstructorStanding>>> {
        val network = ApiCallHandler.safeCall {
            coroutineScope {
                val driversDeferred = async {
                    api.getDriverStandings(year).mrData.standingsTable.standingsLists
                        .firstOrNull()?.driverStandings.orEmpty()
                }
                val constructorsDeferred = async {
                    api.getConstructorStandings(year).mrData.standingsTable.standingsLists
                        .firstOrNull()?.constructorStandings.orEmpty()
                }
                Pair(driversDeferred.await(), constructorsDeferred.await())
            }
        }
        if (network.isSuccess) {
            network.getOrNull()?.let { (drivers, constructors) ->
                saveCache(
                    CacheKeys.historicalStandings(year),
                    HistoricalStandingsCache(drivers, constructors),
                )
            }
            return network.map { (drivers, constructors) ->
                Pair(drivers.toDriverStandingDomain(), constructors.toConstructorStandingDomain())
            }
        }
        return peekHistoricalStandingsCache(year)?.let { Result.success(it) }
            ?: network.map { (drivers, constructors) ->
                Pair(drivers.toDriverStandingDomain(), constructors.toConstructorStandingDomain())
            }
    }

    /** Список трасс: сеть → memory + файл; при ошибке — fallback на peek. */
    override suspend fun getCircuits(): Result<List<Circuit>> {
        val network = ApiCallHandler.safeCall {
            api.getCircuits().mrData.circuitTable.circuits
        }
        if (network.isSuccess) {
            network.getOrNull()?.let {
                circuitsMemoryCache = it
                saveCacheList(CacheKeys.CIRCUITS, it)
            }
            return network.map { circuits -> circuits.map { it.toDomain() } }
        }
        return peekCircuitsCache()?.let { Result.success(it) }
            ?: network.map { circuits -> circuits.map { it.toDomain() } }
    }

    /** Трасса по [circuitId]: memory → файл → сеть. */
    override suspend fun getCircuitById(circuitId: String): Result<Circuit?> {
        circuitsMemoryCache?.find { it.circuitId == circuitId }?.let {
            return Result.success(it.toDomain())
        }
        peekCircuitsCache()?.find { it.circuitId == circuitId }?.let { return Result.success(it) }
        return getCircuits().map { circuits -> circuits.find { it.circuitId == circuitId } }
    }

    /** Годы сезонов (новые сверху), кэш на сутки. */
    override suspend fun getSeasonYears(): Result<List<String>> {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
        loadCache<SeasonsCache>(CacheKeys.SEASONS)?.takeIf { it.dayKey == today }?.years
            ?.let { return Result.success(it) }

        val network = ApiCallHandler.safeCall {
            api.getSeasons().mrData.seasonTable?.seasons.orEmpty()
                .map { it.season }
                .reversed()
        }
        if (network.isSuccess) {
            network.getOrNull()?.let { years ->
                saveCache(CacheKeys.SEASONS, SeasonsCache(today, years))
            }
            return network
        }
        return loadCache<SeasonsCache>(CacheKeys.SEASONS)?.years
            ?.let { Result.success(it) } ?: network
    }

    /** Гонки сезона [year] (для picker раунда). */
    override suspend fun getSeasonRaces(year: String): Result<List<Race>> =
        ApiCallHandler.safeCall {
            api.getSeasonSchedule(year).mrData.raceTable.races.toRaceDomain()
        }

    /** Карточка пилота по [driverId]. */
    override suspend fun getDriver(driverId: String): Result<Driver?> =
        ApiCallHandler.safeCall {
            api.getDriver(driverId).mrData.driverTable.drivers.firstOrNull()?.toDomain()
        }

    /** Карточка конструктора по [constructorId]. */
    override suspend fun getConstructor(constructorId: String): Result<Constructor?> =
        ApiCallHandler.safeCall {
            api.getConstructor(constructorId).mrData.constructorTable.constructors.firstOrNull()?.toDomain()
        }

    /** Карьера пилота: гонки/победы/подиумы/поулы и список команд. */
    override suspend fun getDriverCareerStats(
        driverId: String,
        currentConstructors: List<Constructor>,
    ): Result<CareerStats<Constructor>> =
        ApiCallHandler.safeCall {
            CareerLoader.loadDriverCareer(api, driverId, currentConstructors)
        }

    /** Карьера конструктора: гонки/победы/подиумы/поулы и список пилотов. */
    override suspend fun getConstructorCareerStats(
        constructorId: String,
        currentDrivers: List<Driver>,
    ): Result<CareerStats<Driver>> =
        ApiCallHandler.safeCall {
            CareerLoader.loadConstructorCareer(api, constructorId, currentDrivers)
        }

    override suspend fun getDriverH2hStats(driverId: String, season: String?): Result<H2hStats> =
        ApiCallHandler.safeCall {
            CareerLoader.loadH2hStats(api, "drivers/$driverId", season)
        }

    override suspend fun getConstructorH2hStats(constructorId: String, season: String?): Result<H2hStats> =
        ApiCallHandler.safeCall {
            CareerLoader.loadH2hStats(api, "constructors/$constructorId", season)
        }

    override suspend fun getStandingsAfterRound(
        year: String,
        round: String,
    ): Result<Pair<List<DriverStanding>, List<ConstructorStanding>>> =
        ApiCallHandler.safeCall {
            coroutineScope {
                val driversDeferred = async {
                    api.getDriverStandingsAfterRound(year, round).mrData.standingsTable.standingsLists
                        .firstOrNull()?.driverStandings.orEmpty()
                }
                val constructorsDeferred = async {
                    api.getConstructorStandingsAfterRound(year, round).mrData.standingsTable.standingsLists
                        .firstOrNull()?.constructorStandings.orEmpty()
                }
                Pair(
                    driversDeferred.await().toDriverStandingDomain(),
                    constructorsDeferred.await().toConstructorStandingDomain(),
                )
            }
        }

    override suspend fun getDriverH2hRoundScores(
        driverId: String,
        season: String?,
    ): Result<List<com.example.f1_kmp.viewmodel.H2hRoundScore>> =
        ApiCallHandler.safeCall {
            CareerLoader.loadH2hRoundScores(api, "drivers/$driverId", season)
        }

    override suspend fun getConstructorH2hRoundScores(
        constructorId: String,
        season: String?,
    ): Result<List<com.example.f1_kmp.viewmodel.H2hRoundScore>> =
        ApiCallHandler.safeCall {
            CareerLoader.loadH2hRoundScores(api, "constructors/$constructorId", season)
        }

    override suspend fun getSeasonFinishStatuses(year: String): Result<List<FinishStatusItem>> =
        ApiCallHandler.safeCall {
            api.getSeasonStatus(year).mrData.statusTable?.status.orEmpty()
                .map { dto ->
                    FinishStatusItem(
                        statusId = dto.statusId.orEmpty(),
                        status = dto.status.orEmpty(),
                        count = dto.count?.toIntOrNull() ?: 0,
                    )
                }
                .sortedByDescending { it.count }
        }

    override suspend fun getCurrentDrivers(): Result<List<Driver>> =
        ApiCallHandler.safeCall {
            api.getCurrentDrivers().mrData.driverTable.drivers.map { it.toDomain() }
        }

    override suspend fun getAllDrivers(): Result<List<Driver>> =
        ApiCallHandler.safeCall {
            val all = mutableListOf<Driver>()
            var offset = 0
            var total = 1
            while (offset < total) {
                val response = api.getAllDrivers(limit = 100, offset = offset)
                total = response.mrData.total?.toIntOrNull() ?: all.size
                val page = response.mrData.driverTable.drivers
                if (page.isEmpty()) break
                all.addAll(page.map { it.toDomain() })
                offset += 100
                if (offset < total) delay(280)
            }
            all.sortedBy { it.familyName.lowercase() }
        }

    override suspend fun getCurrentConstructorsList(): Result<List<Constructor>> =
        ApiCallHandler.safeCall {
            api.getCurrentConstructors().mrData.constructorTable.constructors.map { it.toDomain() }
        }

    override suspend fun getAllConstructors(): Result<List<Constructor>> =
        ApiCallHandler.safeCall {
            val all = mutableListOf<Constructor>()
            var offset = 0
            var total = 1
            while (offset < total) {
                val response = api.getAllConstructors(limit = 100, offset = offset)
                total = response.mrData.total?.toIntOrNull() ?: all.size
                val page = response.mrData.constructorTable.constructors
                if (page.isEmpty()) break
                all.addAll(page.map { it.toDomain() })
                offset += 100
                if (offset < total) delay(280)
            }
            all.sortedBy { it.name.lowercase() }
        }

    /** История победителей на трассе [circuitId] (новые сверху). */
    override suspend fun getCircuitWinners(circuitId: String): Result<List<CircuitRaceWin>> =
        ApiCallHandler.safeCall {
            api.getCircuitWinners(circuitId).mrData.raceTable.races.mapNotNull { race ->
                val winner = race.results?.firstOrNull() ?: return@mapNotNull null
                CircuitRaceWin(
                    season = race.season,
                    round = race.round,
                    raceName = race.raceName,
                    driver = winner.driver.toDomain(),
                    constructor = winner.constructor.toDomain(),
                )
            }.reversed()
        }

    /** Текущие команды пилота из кэша standings (Home). */
    override suspend fun currentConstructorsForDriver(driverId: String): List<Constructor> =
        peekCurrentDriversCache()?.first?.find { it.driver.driverId == driverId }?.constructors.orEmpty()

    /** Текущие пилоты конструктора из кэша standings (Home). */
    override suspend fun currentDriversForConstructor(constructorId: String): List<Driver> =
        peekCurrentDriversCache()?.first?.filter { standing ->
            standing.constructors.any { it.constructorId == constructorId }
        }?.map { it.driver }.orEmpty()

    private suspend fun resolveDriverNames(driverIds: List<String>): Map<String, String> = coroutineScope {
        val semaphore = Semaphore(MAX_DRIVER_FETCH_PARALLEL)
        driverIds.map { driverId ->
            async {
                driverNamesMutex.withLock { driverNames[driverId] }?.let { return@async driverId to it }
                semaphore.withPermit {
                    val name = runCatching {
                        api.getDriver(driverId).mrData.driverTable.drivers.firstOrNull()?.fullName
                    }.getOrNull()
                    if (name != null) {
                        driverNamesMutex.withLock { driverNames[driverId] = name }
                    }
                    driverId to name
                }
            }
        }.awaitAll().mapNotNull { (id, name) -> name?.let { id to it } }.toMap()
    }

    private suspend inline fun <reified T> saveCache(key: String, value: T) {
        val json = cacheJsonMapper.toJson(value)
        cacheDao.insert(CacheEntry(key, json))
    }

    private suspend inline fun <reified T> saveCacheList(key: String, items: List<T>) {
        val json = cacheJsonMapper.toJsonList(items)
        cacheDao.insert(CacheEntry(key, json))
    }

    private suspend inline fun <reified T> loadCache(key: String): T? =
        cacheDao.get(key)?.json?.let { cacheJsonMapper.fromJson(it) }

    private suspend inline fun <reified T> loadCacheList(key: String): List<T>? =
        cacheDao.get(key)?.json?.let { cacheJsonMapper.fromJsonList(it) }

    private companion object {
        const val MAX_DRIVER_FETCH_PARALLEL = 8
    }
}
