package com.example.f1_kmp.data.mapper

import com.example.f1_kmp.data.model.AverageSpeedModel
import com.example.f1_kmp.data.model.CircuitLocationModel
import com.example.f1_kmp.data.model.CircuitModel
import com.example.f1_kmp.data.model.ConstructorModel
import com.example.f1_kmp.data.model.ConstructorStandingsModel
import com.example.f1_kmp.data.model.DriverModel
import com.example.f1_kmp.data.model.DriverStandingsModel
import com.example.f1_kmp.data.model.FastestLapModel
import com.example.f1_kmp.data.model.PitStopModel
import com.example.f1_kmp.data.model.QualifyingResultModel
import com.example.f1_kmp.data.model.RaceDateModel
import com.example.f1_kmp.data.model.RaceModel
import com.example.f1_kmp.data.model.RaceResultModel
import com.example.f1_kmp.data.model.StandingsListsModel
import com.example.f1_kmp.data.model.TimeModel
import com.example.f1_kmp.domain.model.AverageSpeed
import com.example.f1_kmp.domain.model.Circuit
import com.example.f1_kmp.domain.model.CircuitLocation
import com.example.f1_kmp.domain.model.Constructor
import com.example.f1_kmp.domain.model.ConstructorStanding
import com.example.f1_kmp.domain.model.Driver
import com.example.f1_kmp.domain.model.DriverStanding
import com.example.f1_kmp.domain.model.FastestLap
import com.example.f1_kmp.domain.model.PitStop
import com.example.f1_kmp.domain.model.QualifyingResult
import com.example.f1_kmp.domain.model.Race
import com.example.f1_kmp.domain.model.RaceResult
import com.example.f1_kmp.domain.model.RaceSession
import com.example.f1_kmp.domain.model.RaceTime
import com.example.f1_kmp.domain.model.StandingsMeta

/**
 * Jolpica API-модели (`data.model`, kotlinx DTO) → domain (`domain.model`).
 * HTTP/JSON парсятся в DTO; сюда — маппинг в domain для ViewModel/UI.
 */

fun DriverModel.toDomain() = Driver(
    driverId = driverId,
    url = url,
    givenName = givenName,
    familyName = familyName,
    dateOfBirth = dateOfBirth,
    nationality = nationality,
    code = code,
    permanentNumber = permanentNumber,
)

fun ConstructorModel.toDomain() = Constructor(
    constructorId = constructorId,
    url = url,
    name = name,
    nationality = nationality,
)

fun DriverStandingsModel.toDomain() = DriverStanding(
    position = position,
    positionText = positionText,
    points = points,
    wins = wins,
    driver = driver.toDomain(),
    constructors = constructors.map { it.toDomain() },
)

fun ConstructorStandingsModel.toDomain() = ConstructorStanding(
    position = position,
    positionText = positionText,
    points = points,
    wins = wins,
    constructor = constructor.toDomain(),
)

fun StandingsListsModel.toMeta() = StandingsMeta(season = season, round = round)

fun CircuitLocationModel.toDomain() = CircuitLocation(
    lat = lat,
    longitude = longitude,
    locality = locality,
    country = country,
)

fun CircuitModel.toDomain() = Circuit(
    circuitId = circuitId,
    url = url,
    circuitName = circuitName,
    location = location.toDomain(),
)

fun RaceDateModel.toDomain() = RaceSession(date = date, time = time)

fun TimeModel.toDomain() = RaceTime(millis = millis, time = time)

fun AverageSpeedModel.toDomain() = AverageSpeed(units = units, speed = speed)

fun FastestLapModel.toDomain() = FastestLap(
    rank = rank,
    lap = lap,
    time = time.toDomain(),
    averageSpeed = averageSpeed?.toDomain(),
)

fun RaceResultModel.toDomain() = RaceResult(
    number = number,
    position = position,
    positionText = positionText,
    points = points,
    driver = driver.toDomain(),
    constructor = constructor.toDomain(),
    grid = grid,
    laps = laps,
    status = status,
    time = time?.toDomain(),
    fastestLap = fastestLap?.toDomain(),
)

fun QualifyingResultModel.toDomain() = QualifyingResult(
    number = number,
    position = position,
    positionText = positionText,
    driver = driver.toDomain(),
    constructor = constructor.toDomain(),
    q1 = Q1,
    q2 = Q2,
    q3 = Q3,
)

fun PitStopModel.toDomain() = PitStop(
    driverId = driverId,
    lap = lap,
    stop = stop,
    time = time,
    duration = duration,
)

fun RaceModel.toDomain() = Race(
    season = season,
    round = round,
    url = url,
    raceName = raceName,
    circuit = circuit.toDomain(),
    date = date,
    time = time,
    firstPractice = firstPractice?.toDomain(),
    secondPractice = secondPractice?.toDomain(),
    thirdPractice = thirdPractice?.toDomain(),
    sprintQualifying = sprintQualifying?.toDomain(),
    qualifying = qualifying?.toDomain(),
    sprint = sprint?.toDomain(),
    results = results?.map { it.toDomain() },
    sprintResults = sprintResults?.map { it.toDomain() },
    qualifyingResults = qualifyingResults?.map { it.toDomain() },
    pitStops = pitStops?.map { it.toDomain() },
)

fun List<DriverStandingsModel>.toDriverStandingDomain() = map { it.toDomain() }
fun List<ConstructorStandingsModel>.toConstructorStandingDomain() = map { it.toDomain() }
fun List<RaceModel>.toRaceDomain() = map { it.toDomain() }
