package com.example.f1_kmp.data.api

import com.example.f1_kmp.data.model.CircuitsModel
import com.example.f1_kmp.data.model.ConstructorFetchingModel
import com.example.f1_kmp.data.model.DriverFetchingModel
import com.example.f1_kmp.data.model.MrDataResponse
import com.example.f1_kmp.data.model.MrDataTotalModel
import com.example.f1_kmp.data.model.ScheduleModel
import com.example.f1_kmp.data.model.StandingsModel
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * HTTP-клиент Jolpica/Ergast API на Ktor.
 *
 * Каждый метод — один GET. Ktor:
 * - подставляет path/query в URL (base URL задаётся в [com.example.f1_kmp.di.appModule]);
 * - suspend-функции не блокируют UI-поток;
 * - парсит JSON в data class через kotlinx.serialization.
 *
 * Все ответы обёрнуты в `{ "MRData": { ... } }` → [MrDataResponse].
 */
class F1ApiService(private val client: HttpClient) {

    /** Текущий чемпионат пилотов (вкладка «Главная»). */
    suspend fun getCurrentDriverStandings(limit: Int = 100): MrDataResponse<StandingsModel> =
        client.get("current/driverStandings.json") { parameter("limit", limit) }.body()

    /** Текущий чемпионат конструкторов. */
    suspend fun getCurrentConstructorStandings(limit: Int = 100): MrDataResponse<StandingsModel> =
        client.get("current/constructorStandings.json") { parameter("limit", limit) }.body()

    /** Результаты последней завершённой гонки. */
    suspend fun getLastRaceResults(limit: Int = 100): MrDataResponse<ScheduleModel> =
        client.get("current/last/results.json") { parameter("limit", limit) }.body()

    /** Результаты конкретной гонки по году и номеру раунда. */
    suspend fun getRaceResults(year: String, round: String, limit: Int = 100): MrDataResponse<ScheduleModel> =
        client.get("$year/$round/results.json") { parameter("limit", limit) }.body()

    /** Результаты спринта конкретной гонки. */
    suspend fun getSprintResults(year: String, round: String, limit: Int = 100): MrDataResponse<ScheduleModel> =
        client.get("$year/$round/sprint.json") { parameter("limit", limit) }.body()

    /** Квалификация гонки (Q1/Q2/Q3). */
    suspend fun getQualifyingResults(year: String, round: String, limit: Int = 100): MrDataResponse<ScheduleModel> =
        client.get("$year/$round/qualifying.json") { parameter("limit", limit) }.body()

    /** Пит-стопы. В ответе только driverId — имя подтягивается отдельным запросом. */
    suspend fun getPitStops(year: String, round: String, limit: Int = 100): MrDataResponse<ScheduleModel> =
        client.get("$year/$round/pitstops.json") { parameter("limit", limit) }.body()

    /** Карточка пилота по ID (для расшифровки пит-стопов). */
    suspend fun getDriver(driverId: String, limit: Int = 100): MrDataResponse<DriverFetchingModel> =
        client.get("drivers/$driverId.json") { parameter("limit", limit) }.body()

    /** Календарь текущего сезона со всеми сессиями. */
    suspend fun getCurrentSchedule(limit: Int = 100): MrDataResponse<ScheduleModel> =
        client.get("current.json") { parameter("limit", limit) }.body()

    /** Итоговая таблица пилотов за год («Зал славы»). */
    suspend fun getDriverStandings(year: String, limit: Int = 100): MrDataResponse<StandingsModel> =
        client.get("$year/driverStandings.json") { parameter("limit", limit) }.body()

    /** Итоговая таблица конструкторов за год. */
    suspend fun getConstructorStandings(year: String, limit: Int = 100): MrDataResponse<StandingsModel> =
        client.get("$year/constructorStandings.json") { parameter("limit", limit) }.body()

    /** Список всех F1-трасс с координатами. */
    suspend fun getCircuits(limit: Int = 100): MrDataResponse<CircuitsModel> =
        client.get("circuits.json") { parameter("limit", limit) }.body()

    /** Список сезонов F1 (для picker). */
    suspend fun getSeasons(limit: Int = 100): MrDataResponse<MrDataTotalModel> =
        client.get("seasons.json") { parameter("limit", limit) }.body()

    /** Календарь конкретного сезона (для picker раундов). */
    suspend fun getSeasonSchedule(year: String, limit: Int = 100): MrDataResponse<ScheduleModel> =
        client.get("$year.json") { parameter("limit", limit) }.body()

    /** Карточка конструктора по ID. */
    suspend fun getConstructor(constructorId: String, limit: Int = 100): MrDataResponse<ConstructorFetchingModel> =
        client.get("constructors/$constructorId.json") { parameter("limit", limit) }.body()

    /** История побед на трассе (все ГП с position=1). */
    suspend fun getCircuitWinners(circuitId: String, limit: Int = 100): MrDataResponse<ScheduleModel> =
        client.get("circuits/$circuitId/results/1.json") { parameter("limit", limit) }.body()

    /** Totals и таблицы для карьерной статистики (поле total в MRData). */
    suspend fun getMrDataTotal(
        path: String,
        limit: Int = 100,
        offset: Int = 0,
    ): MrDataResponse<MrDataTotalModel> =
        client.get("$path.json") {
            parameter("limit", limit)
            parameter("offset", offset)
        }.body()

    /** Статусы финиша сезона (`Finished`, `Retired`, …). */
    suspend fun getSeasonStatus(year: String, limit: Int = 100): MrDataResponse<MrDataTotalModel> =
        client.get("$year/status.json") { parameter("limit", limit) }.body()

    /** Пилоты текущего сезона. */
    suspend fun getCurrentDrivers(limit: Int = 100): MrDataResponse<DriverFetchingModel> =
        client.get("current/drivers.json") { parameter("limit", limit) }.body()

    /** Все пилоты (пагинация). */
    suspend fun getAllDrivers(limit: Int = 100, offset: Int = 0): MrDataResponse<DriverFetchingModel> =
        client.get("drivers.json") {
            parameter("limit", limit)
            parameter("offset", offset)
        }.body()

    /** Конструкторы текущего сезона. */
    suspend fun getCurrentConstructors(limit: Int = 100): MrDataResponse<ConstructorFetchingModel> =
        client.get("current/constructors.json") { parameter("limit", limit) }.body()

    /** Все конструкторы (пагинация). */
    suspend fun getAllConstructors(limit: Int = 100, offset: Int = 0): MrDataResponse<ConstructorFetchingModel> =
        client.get("constructors.json") {
            parameter("limit", limit)
            parameter("offset", offset)
        }.body()
}
