package com.example.f1_kmp.data.repository

import com.example.f1_kmp.data.api.F1ApiService
import com.example.f1_kmp.data.local.CacheDao
import com.example.f1_kmp.data.local.CacheEntry
import com.example.f1_kmp.data.local.CacheJsonMapper
import com.example.f1_kmp.data.local.CacheKeys
import com.example.f1_kmp.data.model.CircuitModel
import com.example.f1_kmp.data.model.ConstructorStandingsModel
import com.example.f1_kmp.data.model.DriverStandingsCache
import com.example.f1_kmp.data.model.DriverStandingsModel
import com.example.f1_kmp.data.model.HistoricalStandingsCache
import com.example.f1_kmp.data.model.PitStopModel
import com.example.f1_kmp.data.model.QualifyingResultModel
import com.example.f1_kmp.data.model.RaceModel
import com.example.f1_kmp.data.model.StandingsListsModel
import com.example.f1_kmp.domain.ApiCallHandler
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

/**
 * Repository — единая точка доступа к данным для всех экранов.
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
) {
    private var circuitsMemoryCache: List<CircuitModel>? = null

    private val driverNames = mutableMapOf<String, String>()
    private val driverNamesMutex = Mutex()

    // region peek — мгновенный UI без ожидания сети

    suspend fun peekCurrentDriversCache(): Pair<List<DriverStandingsModel>, StandingsListsModel>? =
        loadCache<DriverStandingsCache>(CacheKeys.CURRENT_DRIVERS)?.let { cached ->
            Pair(cached.drivers, StandingsListsModel(cached.season, cached.round, cached.drivers, null))
        }

    suspend fun peekCurrentConstructorsCache(): List<ConstructorStandingsModel>? =
        loadCacheList(CacheKeys.CURRENT_CONSTRUCTORS)

    suspend fun peekLastRaceCache(): RaceModel? =
        loadCache(CacheKeys.LAST_RACE)

    suspend fun peekScheduleCache(): List<RaceModel>? =
        loadCacheList(CacheKeys.SCHEDULE)

    suspend fun peekCircuitsCache(): List<CircuitModel>? {
        circuitsMemoryCache?.let { return it }
        return loadCacheList<CircuitModel>(CacheKeys.CIRCUITS)?.also { circuitsMemoryCache = it }
    }

    suspend fun peekHistoricalStandingsCache(
        year: String,
    ): Pair<List<DriverStandingsModel>, List<ConstructorStandingsModel>>? =
        loadCache<HistoricalStandingsCache>(CacheKeys.historicalStandings(year))?.let {
            Pair(it.drivers, it.constructors)
        }

    suspend fun getCurrentDriverStandings(): Result<Pair<List<DriverStandingsModel>, StandingsListsModel>> {
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
            return network
        }
        return peekCurrentDriversCache()?.let { Result.success(it) } ?: network
    }

    suspend fun getCurrentConstructorStandings(): Result<List<ConstructorStandingsModel>> {
        val network = ApiCallHandler.safeCall {
            api.getCurrentConstructorStandings()
                .mrData.standingsTable.standingsLists.first()
                .constructorStandings.orEmpty()
        }
        if (network.isSuccess) {
            network.getOrNull()?.let { saveCacheList(CacheKeys.CURRENT_CONSTRUCTORS, it) }
            return network
        }
        return peekCurrentConstructorsCache()?.let { Result.success(it) } ?: network
    }

    suspend fun getLastRace(): Result<RaceModel> {
        val network = ApiCallHandler.safeCall {
            api.getLastRaceResults().mrData.raceTable.races.first()
        }
        if (network.isSuccess) {
            network.getOrNull()?.let { saveCache(CacheKeys.LAST_RACE, it) }
            return network
        }
        return peekLastRaceCache()?.let { Result.success(it) } ?: network
    }

    suspend fun getRaceResults(year: String, round: String): Result<RaceModel?> =
        ApiCallHandler.safeCall {
            api.getRaceResults(year, round).mrData.raceTable.races.firstOrNull()
        }

    suspend fun getQualifyingResults(year: String, round: String): Result<List<QualifyingResultModel>> =
        ApiCallHandler.safeCall {
            api.getQualifyingResults(year, round).mrData.raceTable.races
                .firstOrNull()?.qualifyingResults.orEmpty()
        }

    /**
     * Пит-стопы: один запрос pitstops + уникальные driverId (не каждая остановка).
     * Имена кэшируются в памяти; параллельных getDriver не больше [MAX_DRIVER_FETCH_PARALLEL].
     */
    suspend fun getPitStopsWithDriverNames(year: String, round: String): Result<List<PitStopModel>> =
        ApiCallHandler.safeCall {
            val stops = api.getPitStops(year, round).mrData.raceTable.races
                .firstOrNull()?.pitStops.orEmpty()
            if (stops.isEmpty()) return@safeCall emptyList()

            val uniqueDriverIds = stops.map { it.driverId }.distinct()
            val namesById = resolveDriverNames(uniqueDriverIds)
            stops.map { stop ->
                namesById[stop.driverId]?.let { stop.copy(driverId = it) } ?: stop
            }
        }

    suspend fun getCurrentSchedule(): Result<List<RaceModel>> {
        val network = ApiCallHandler.safeCall {
            api.getCurrentSchedule().mrData.raceTable.races
        }
        if (network.isSuccess) {
            network.getOrNull()?.let { saveCacheList(CacheKeys.SCHEDULE, it) }
            return network
        }
        return peekScheduleCache()?.let { Result.success(it) } ?: network
    }

    suspend fun getHistoricalStandings(
        year: String,
    ): Result<Pair<List<DriverStandingsModel>, List<ConstructorStandingsModel>>> {
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
            return network
        }
        return peekHistoricalStandingsCache(year)?.let { Result.success(it) } ?: network
    }

    suspend fun getCircuits(): Result<List<CircuitModel>> {
        val network = ApiCallHandler.safeCall {
            api.getCircuits().mrData.circuitTable.circuits
        }
        if (network.isSuccess) {
            network.getOrNull()?.let {
                circuitsMemoryCache = it
                saveCacheList(CacheKeys.CIRCUITS, it)
            }
            return network
        }
        return peekCircuitsCache()?.let { Result.success(it) } ?: network
    }

    suspend fun getCircuitById(circuitId: String): Result<CircuitModel?> {
        circuitsMemoryCache?.find { it.circuitId == circuitId }?.let { return Result.success(it) }
        peekCircuitsCache()?.find { it.circuitId == circuitId }?.let { return Result.success(it) }
        return getCircuits().map { circuits -> circuits.find { it.circuitId == circuitId } }
    }

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
