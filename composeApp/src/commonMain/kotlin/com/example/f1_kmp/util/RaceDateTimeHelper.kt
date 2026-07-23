package com.example.f1_kmp.util

import com.example.f1_kmp.domain.model.RaceSession
import com.example.f1_kmp.domain.model.Race
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * Конвертация UTC-дат сессий Ergast/Jolpica в локальное время устройства.
 */
object RaceDateTimeHelper {
    /**
     * Парсит [RaceSession] (дата + время в UTC) в локальный [LocalDateTime].
     * Пустое время трактуется как полночь UTC указанного дня.
     */
    fun toLocal(date: RaceSession): LocalDateTime =
        DateUtils.parseUtcSession(date.date, date.time)
            .toLocalDateTime(TimeZone.currentSystemDefault())

    /** Локальное время старта основной гонки. */
    fun raceLocal(race: Race): LocalDateTime =
        toLocal(RaceSession(date = race.date, time = race.time))

    /** Цель countdown: FP1, иначе первая доступная сессия, иначе гонка. */
    fun countdownTarget(race: Race): LocalDateTime {
        for (session in orderedSessions(race)) {
            if (session != null) return toLocal(session)
        }
        return raceLocal(race)
    }

    /** Гонка ещё не стартовала (по времени race). */
    fun isUpcoming(
        race: Race,
        now: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
    ): Boolean = raceLocal(race) > now

    /** Начало уикенда (первая сессия или гонка). */
    fun weekendStart(race: Race): LocalDateTime = countdownTarget(race)

    private fun orderedSessions(race: Race): List<RaceSession?> = listOf(
        race.firstPractice,
        race.secondPractice,
        race.thirdPractice,
        race.sprintQualifying,
        race.sprint,
        race.qualifying,
    )
}

/** Разбивка оставшегося времени до события. */
data class CountdownParts(
    val days: Int,
    val hours: Int,
    val minutes: Int,
    val seconds: Int,
) {
    val isZero: Boolean get() = days == 0 && hours == 0 && minutes == 0 && seconds == 0

    companion object {
        val ZERO = CountdownParts(0, 0, 0, 0)

        fun until(target: Instant, now: Instant = Clock.System.now()): CountdownParts {
            if (target <= now) return ZERO
            var diffSeconds = (target - now).inWholeSeconds
            val days = (diffSeconds / 86_400).toInt()
            diffSeconds -= days * 86_400L
            val hours = (diffSeconds / 3_600).toInt()
            diffSeconds -= hours * 3_600L
            val minutes = (diffSeconds / 60).toInt()
            diffSeconds -= minutes * 60L
            return CountdownParts(days, hours, minutes, diffSeconds.toInt())
        }

        fun until(
            target: LocalDateTime,
            now: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
        ): CountdownParts = until(
            target.toInstant(TimeZone.currentSystemDefault()),
            now.toInstant(TimeZone.currentSystemDefault()),
        )
    }
}
