package com.example.f1_kmp.domain.predictor

import com.example.f1_kmp.domain.model.Driver
import com.example.f1_kmp.domain.model.QualifyingResult
import com.example.f1_kmp.domain.model.Race
import com.example.f1_kmp.domain.model.RaceResult
import com.example.f1_kmp.util.RaceDateTimeHelper
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/** Дедлайн прогноза: за 1 ч до квалификации; после — сетка только на чтение. */
object PredictorLock {
    private val lead = 1.hours

    fun lockAt(race: Race): LocalDateTime? {
        val quali = race.qualifying ?: return null
        val tz = TimeZone.currentSystemDefault()
        return (RaceDateTimeHelper.toLocal(quali).toInstant(tz) - lead).toLocalDateTime(tz)
    }

    fun isLocked(
        race: Race,
        now: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
    ): Boolean {
        val at = lockAt(race) ?: return false
        return now >= at
    }
}

/** Подсчёт очков 1:1 по позиции + применение actual order к уикенду. */
object PredictorScoreService {
    /** +1 за каждое совпадение driverId на той же позиции. */
    fun scoreOrders(predicted: List<String>, actualByPosition: List<String>): Int {
        val length = minOf(predicted.size, actualByPosition.size)
        var points = 0
        for (i in 0 until length) {
            if (predicted[i] == actualByPosition[i]) points++
        }
        return points
    }

    fun qualifyingActualOrder(results: List<QualifyingResult>): List<String> =
        results.sortedBy { it.position.toIntOrNull() ?: 999 }
            .map { it.driver.driverId }

    fun raceActualOrder(results: List<RaceResult>): List<String> =
        results.filter { it.positionText.toIntOrNull() != null }
            .sortedBy { it.position.toIntOrNull() ?: 999 }
            .map { it.driver.driverId }

    /** Мержит actual order/очки в prediction, не затирая уже scored. */
    fun applyResults(
        weekend: PredictorWeekendPrediction,
        qualifyingResults: List<QualifyingResult>? = null,
        raceResults: List<RaceResult>? = null,
        actualQualifyingOrder: List<String>? = null,
        actualRaceOrder: List<String>? = null,
        now: Instant = Clock.System.now(),
    ): PredictorWeekendPrediction {
        val qualiActual = actualQualifyingOrder
            ?: qualifyingResults?.let { qualifyingActualOrder(it) }
        val raceActual = actualRaceOrder
            ?: raceResults?.let { raceActualOrder(it) }

        val qualiPts = qualiActual?.let { scoreOrders(weekend.qualifyingOrder, it) }
        val racePts = raceActual?.let { scoreOrders(weekend.raceOrder, it) }

        return weekend.copy(
            qualiPoints = qualiPts ?: weekend.qualiPoints,
            racePoints = racePts ?: weekend.racePoints,
            actualQualifyingOrder = qualiActual ?: weekend.actualQualifyingOrder,
            actualRaceOrder = raceActual ?: weekend.actualRaceOrder,
            scoredAt = if (qualiPts != null || racePts != null) now else weekend.scoredAt,
        )
    }
}

/** Сборка/синк драфта сетки с ростером чемпионата. */
object PredictorOrder {
    fun hasUsableDriverCode(driver: Driver): Boolean {
        val code = driver.code?.trim().orEmpty()
        return code.isNotEmpty() && !code.equals("none", ignoreCase = true)
    }

    /** Стартовый драфт: чемпионат ∩ roster, затем хвост ростера. */
    fun defaultPredictorOrder(rosterIds: List<String>, championshipOrder: List<String>): List<String> {
        val roster = rosterIds.toSet()
        val fromStandings = championshipOrder.filter { it in roster }
        val rest = rosterIds.filter { it !in fromStandings.toSet() }
        return fromStandings + rest
    }

    /** Сохраняет порядок пользователя, дописывает новых пилотов в конец. */
    fun syncOrderToRoster(saved: List<String>, rosterIds: List<String>): List<String> {
        val roster = rosterIds.toSet()
        val kept = saved.filter { it in roster }
        val missing = rosterIds.filter { it !in kept.toSet() }
        return kept + missing
    }
}

/** Правила ника лидерборда: длина, charset, нормализация для уникальности. */
object PredictorNickname {
    const val MIN_LENGTH = 3
    const val MAX_LENGTH = 16
    private val allowed = Regex("^[a-zA-Z0-9_]+$")

    fun normalize(raw: String): String = raw.trim().lowercase()

    /** null = ok, иначе ключ ошибки. */
    fun validate(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.length !in MIN_LENGTH..MAX_LENGTH) {
            return "predictorNicknameErrorLength"
        }
        if (!allowed.matches(trimmed)) {
            return "predictorNicknameErrorChars"
        }
        return null
    }
}

/** Подпись пилота в сетке: `CODE · family` или fullName / id. */
fun predictorDriverLabel(driver: Driver?, fallbackId: String?): String {
    if (driver != null) {
        val code = driver.code?.trim().orEmpty()
        if (code.isNotEmpty() && !code.equals("none", ignoreCase = true)) {
            return "$code · ${driver.familyName}"
        }
        return driver.fullName
    }
    return fallbackId?.takeIf { it.isNotBlank() } ?: "—"
}
