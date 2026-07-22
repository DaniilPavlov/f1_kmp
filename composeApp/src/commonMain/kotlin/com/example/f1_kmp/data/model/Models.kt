package com.example.f1_kmp.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data-модели ответов Ergast/Jolpica API.
 *
 * В JSON поля часто в PascalCase (`MRData`, `DriverStandings`),
 * в Kotlin — camelCase. [@SerialName] связывает их при парсинге.
 */

/** Обёртка всех ответов API: `{ "MRData": { ... } }`. */
@Serializable
data class MrDataResponse<T>(
    @SerialName("MRData") val mrData: T,
)

/** Ответ standings: таблица чемпионата. */
@Serializable
data class StandingsModel(
    @SerialName("StandingsTable") val standingsTable: StandingsTableModel,
)

/** Таблица standings со списком срезов (обычно один — текущий/запрошенный сезон). */
@Serializable
data class StandingsTableModel(
    @SerialName("StandingsLists") val standingsLists: List<StandingsListsModel>,
)

/** Один «срез» чемпионата: сезон, раунд и списки пилотов или конструкторов. */
@Serializable
data class StandingsListsModel(
    val season: String,
    val round: String,
    @SerialName("DriverStandings") val driverStandings: List<DriverStandingsModel>? = null,
    @SerialName("ConstructorStandings") val constructorStandings: List<ConstructorStandingsModel>? = null,
)

/** Строка турнирной таблицы пилотов. */
@Serializable
data class DriverStandingsModel(
    val position: String,
    val positionText: String,
    val points: String,
    val wins: String,
    @SerialName("Driver") val driver: DriverModel,
    @SerialName("Constructors") val constructors: List<ConstructorModel>,
)

/** Строка турнирной таблицы конструкторов. */
@Serializable
data class ConstructorStandingsModel(
    val position: String,
    val positionText: String,
    val points: String,
    val wins: String,
    @SerialName("Constructor") val constructor: ConstructorModel,
)

/** Пилот F1 (паспортные данные из Jolpica). */
@Serializable
data class DriverModel(
    val driverId: String,
    val url: String,
    val givenName: String,
    val familyName: String,
    val dateOfBirth: String,
    val nationality: String,
    val code: String? = null,
    val permanentNumber: String? = null,
) {
    /** Удобное поле для UI вместо склейки имени в каждой таблице. */
    val fullName: String get() = "$givenName $familyName"
}

/** Команда (конструктор) F1. */
@Serializable
data class ConstructorModel(
    val constructorId: String,
    val url: String,
    val name: String,
    val nationality: String,
)

/** Ответ расписания / результатов гонок: таблица [RaceTableModel]. */
@Serializable
data class ScheduleModel(
    @SerialName("RaceTable") val raceTable: RaceTableModel,
)

/** Таблица гонок сезона (или одной гонки — в зависимости от эндпоинта). */
@Serializable
data class RaceTableModel(
    val season: String? = null,
    val round: String? = null,
    @SerialName("Races") val races: List<RaceModel>,
)

/**
 * Гонка — центральная сущность приложения.
 * Может содержать расписание сессий, результаты, квалификацию и пит-стопы
 * в зависимости от того, какой эндпоинт её вернул.
 */
@Serializable
data class RaceModel(
    val season: String,
    val round: String,
    val url: String,
    val raceName: String,
    @SerialName("Circuit") val circuit: CircuitModel,
    val date: String,
    val time: String? = null,
    @SerialName("FirstPractice") val firstPractice: RaceDateModel? = null,
    @SerialName("SecondPractice") val secondPractice: RaceDateModel? = null,
    @SerialName("ThirdPractice") val thirdPractice: RaceDateModel? = null,
    @SerialName("SprintQualifying") val sprintQualifying: RaceDateModel? = null,
    @SerialName("Qualifying") val qualifying: RaceDateModel? = null,
    @SerialName("Sprint") val sprint: RaceDateModel? = null,
    @SerialName("Results") val results: List<RaceResultModel>? = null,
    @SerialName("SprintResults") val sprintResults: List<RaceResultModel>? = null,
    @SerialName("QualifyingResults") val qualifyingResults: List<QualifyingResultModel>? = null,
    @SerialName("PitStops") val pitStops: List<PitStopModel>? = null,
) {
    /**
     * Лучшее время круга среди финишировавших.
     * Строки сравниваются лексикографически — для формата Ergast `M:SS.mmm` это работает.
     * `999999` — sentinel «круга ещё не было».
     */
    val fastestLapTime: String
        get() {
            var fastest = "999999"
            results?.forEach { result ->
                val lap = result.fastestLap?.time?.time
                if (lap != null && fastest > lap) {
                    fastest = lap
                }
            }
            return fastest
        }
}

/** Дата и время одной сессии (практика, квалификация, гонка). */
@Serializable
data class RaceDateModel(
    val date: String,
    val time: String? = null,
)

/** Результат гонки или спринта для одного пилота. */
@Serializable
data class RaceResultModel(
    val number: String,
    val position: String,
    val positionText: String,
    val points: String,
    @SerialName("Driver") val driver: DriverModel,
    @SerialName("Constructor") val constructor: ConstructorModel,
    val grid: String,
    val laps: String,
    val status: String,
    @SerialName("Time") val time: TimeModel? = null,
    @SerialName("FastestLap") val fastestLap: FastestLapModel? = null,
)

/** Время круга / финиша в формате Ergast (`M:SS.mmm` или относительное). */
@Serializable
data class TimeModel(
    val millis: String? = null,
    val time: String,
)

/** Лучший круг пилота в гонке. */
@Serializable
data class FastestLapModel(
    val rank: String,
    val lap: String,
    @SerialName("Time") val time: TimeModel,
    @SerialName("AverageSpeed") val averageSpeed: AverageSpeedModel? = null,
)

/** Средняя скорость на лучшем круге. */
@Serializable
data class AverageSpeedModel(
    val units: String,
    val speed: String,
)

/** Результат квалификации: Q1/Q2/Q3. Пустые поля = пилот выбыл раньше. */
@Serializable
data class QualifyingResultModel(
    val number: String,
    val position: String,
    val positionText: String? = null,
    @SerialName("Driver") val driver: DriverModel,
    @SerialName("Constructor") val constructor: ConstructorModel,
    val Q1: String? = null,
    val Q2: String? = null,
    val Q3: String? = null,
)

/**
 * Пит-стоп. В сыром ответе API [driverId] — id пилота;
 * после [com.example.f1_kmp.data.repository.F1Repository.getPitStopsWithDriverNames] — полное имя.
 */
@Serializable
data class PitStopModel(
    val driverId: String,
    val lap: String,
    val stop: String,
    val time: String,
    val duration: String,
)

/** Ответ списка трасс. */
@Serializable
data class CircuitsModel(
    @SerialName("CircuitTable") val circuitTable: CircuitTableModel,
)

/** Таблица трасс. */
@Serializable
data class CircuitTableModel(
    @SerialName("Circuits") val circuits: List<CircuitModel>,
)

/** Трасса: id, Wikipedia URL, название и геолокация. */
@Serializable
data class CircuitModel(
    val circuitId: String,
    val url: String,
    val circuitName: String,
    @SerialName("Location") val location: CircuitLocationModel,
)

/** Координаты и адрес трассы (`long` в JSON → [longitude]). */
@Serializable
data class CircuitLocationModel(
    val lat: String,
    @SerialName("long") val longitude: String,
    val locality: String,
    val country: String,
)

/** Ответ эндпоинта пилота(ов): обёртка [DriverTableModel]. */
@Serializable
data class DriverFetchingModel(
    val total: String? = null,
    @SerialName("DriverTable") val driverTable: DriverTableModel,
)

/** Таблица пилотов. */
@Serializable
data class DriverTableModel(
    @SerialName("Drivers") val drivers: List<DriverModel>,
)

/** Ответ эндпоинта конструктора(ов): обёртка [ConstructorTableModel]. */
@Serializable
data class ConstructorFetchingModel(
    val total: String? = null,
    @SerialName("ConstructorTable") val constructorTable: ConstructorTableModel,
)

/** Таблица конструкторов. */
@Serializable
data class ConstructorTableModel(
    @SerialName("Constructors") val constructors: List<ConstructorModel>,
)

/**
 * Универсальный кусок [MrData] для карьерных запросов:
 * поле [total] + опциональные таблицы (гонки / конструкторы / пилоты / сезоны).
 */
@Serializable
data class MrDataTotalModel(
    val total: String? = null,
    @SerialName("RaceTable") val raceTable: RaceTableModel? = null,
    @SerialName("ConstructorTable") val constructorTable: ConstructorTableModel? = null,
    @SerialName("DriverTable") val driverTable: DriverTableModel? = null,
    @SerialName("SeasonTable") val seasonTable: SeasonTableModel? = null,
    @SerialName("StatusTable") val statusTable: StatusTableModel? = null,
)

@Serializable
data class StatusTableModel(
    @SerialName("Status") val status: List<FinishStatusDto> = emptyList(),
)

@Serializable
data class FinishStatusDto(
    val statusId: String? = null,
    val status: String? = null,
    val count: String? = null,
)

/** Статус финиша сезона из Jolpica `/{year}/status`. */
data class FinishStatusItem(
    val statusId: String,
    val status: String,
    val count: Int,
) {
    /** Retired / DNF-подобные и дисквалификации. */
    val isHighlight: Boolean
        get() {
            val lower = status.lowercase()
            return lower.contains("retir") ||
                lower.contains("disqual") ||
                lower.contains("accident") ||
                lower.contains("collision") ||
                lower.contains("did not start") ||
                lower.contains("dns") ||
                lower.contains("dnf") ||
                lower.startsWith("+") ||
                lower.contains("lapped") ||
                lower.contains("not classified")
        }
}

/** Метрики для сравнения H2H (пилот или конструктор). */
data class H2hStats(
    val races: Int,
    val wins: Int,
    val podiums: Int,
    val poles: Int,
)

/** Таблица сезонов чемпионата. */
@Serializable
data class SeasonTableModel(
    @SerialName("Seasons") val seasons: List<SeasonModel>,
)

/** Один сезон (год) в списке Jolpica. */
@Serializable
data class SeasonModel(
    val season: String,
    val url: String,
)

/** Финиш в конкретной гонке (победа / подиум / поул). */
data class CareerRaceResult(
    val season: String,
    val round: String,
    val raceName: String,
    val position: Int,
    val constructor: ConstructorModel,
    val circuit: CircuitModel,
    val driver: DriverModel? = null,
) {
    /** Подзаголовок строки: пилот (если есть) или конструктор. */
    val entityName: String
        get() = driver?.fullName?.trim()?.takeIf { it.isNotEmpty() } ?: constructor.name
}

/** Карьерная статистика пилота или конструктора. */
data class CareerStats<T>(
    val races: Int,
    val wins: Int,
    val podiums: Int,
    val poles: Int,
    val current: List<T>,
    val related: List<T>,
    val winRaces: List<CareerRaceResult> = emptyList(),
    val podiumRaces: List<CareerRaceResult> = emptyList(),
    val poleRaces: List<CareerRaceResult> = emptyList(),
)

/** Победа на трассе (история ГП). */
data class CircuitRaceWin(
    val season: String,
    val round: String,
    val raceName: String,
    val driver: DriverModel,
    val constructor: ConstructorModel,
)

/** Кэш списка сезонов (обновляется раз в сутки). */
@Serializable
data class SeasonsCache(
    val dayKey: String,
    val years: List<String>,
)

/**
 * DTO кэша таблицы пилотов: список + сезон/раунд.
 * Нужен экрану «Главная» для строк «Сезон / Раунд» в offline-режиме.
 */
@Serializable
data class DriverStandingsCache(
    val drivers: List<DriverStandingsModel>,
    val season: String,
    val round: String,
)

/** DTO кэша «Зал славы» — таблицы пилотов и конструкторов за год. */
@Serializable
data class HistoricalStandingsCache(
    val drivers: List<DriverStandingsModel>,
    val constructors: List<ConstructorStandingsModel>,
)
