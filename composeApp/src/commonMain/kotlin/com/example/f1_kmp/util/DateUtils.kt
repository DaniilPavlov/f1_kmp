package com.example.f1_kmp.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * Простой аналог YearMonth для календаря расписания (без java.time — для KMP).
 */
data class YearMonth(val year: Int, val month: Int) {
    init {
        require(month in 1..12) { "month must be 1..12, was $month" }
    }

    fun atDay(day: Int): LocalDate = LocalDate(year, month, day)

    fun lengthOfMonth(): Int = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (isLeapYear(year)) 29 else 28
        else -> error("Invalid month $month")
    }

    fun plusMonths(months: Int): YearMonth {
        val total = year * 12 + (month - 1) + months
        val newYear = if (total >= 0) total / 12 else (total - 11) / 12
        val newMonth = ((total % 12) + 12) % 12 + 1
        return YearMonth(newYear, newMonth)
    }
}

private fun isLeapYear(year: Int): Boolean =
    year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)

/**
 * Утилиты дат/времени F1-сессий.
 *
 * API отдаёт дату и время в UTC (например `"14:00:00Z"`-подобное).
 * Перед показом переводим в локальный часовой пояс через [toLocalDateTime].
 */
object DateUtils {
    private val russianMonths = listOf(
        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь",
    )

    /** Сравнивает две даты без учёта времени. */
    fun isSameDay(first: LocalDate, second: LocalDate): Boolean = first == second

    /** Название месяца по-русски: `3` → «Март». */
    fun monthName(month: Int): String = russianMonths[month - 1]

    /** Форматирует время как `HH:mm`. */
    fun formatHourMinute(dateTime: LocalDateTime): String =
        "${dateTime.hour.toString().padStart(2, '0')}:${dateTime.minute.toString().padStart(2, '0')}"

    /**
     * Собирает [LocalDateTime] из даты и времени сессии и переводит UTC → локальное время.
     * Если время в API пустое — возвращает null.
     */
    fun toLocalDateTime(date: String, time: String?): LocalDateTime? {
        if (time.isNullOrBlank()) return null
        val parts = time.split(":")
        if (parts.size < 2) return null
        val utc = LocalDateTime(
            date = LocalDate.parse(date),
            time = LocalTime(parts[0].toInt(), parts[1].toInt()),
        )
        val instant = utc.toInstant(TimeZone.UTC)
        return instant.toLocalDateTime(TimeZone.currentSystemDefault())
    }
}
