package com.example.f1_kmp.util

import com.example.f1_kmp.domain.LocaleController
import kotlinx.datetime.Instant
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

    /** Дата [day]-го числа этого месяца. */
    fun atDay(day: Int): LocalDate = LocalDate(year, month, day)

    /** Число дней в месяце (учитывает високосный февраль). */
    fun lengthOfMonth(): Int = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (isLeapYear(year)) 29 else 28
        else -> error("Invalid month $month")
    }

    /** Сдвиг на [months] месяцев (может быть отрицательным). */
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
    private val englishMonths = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December",
    )

    private val russianWeekdays = listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс")
    private val englishWeekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    /** Сравнивает две даты без учёта времени. */
    fun isSameDay(first: LocalDate, second: LocalDate): Boolean = first == second

    /** Название месяца: `3` → «Март» / «March». */
    fun monthName(month: Int, language: String = LocaleController.language.value): String {
        val months = if (language == "en") englishMonths else russianMonths
        return months[month - 1]
    }

    /** Короткие подписи дней недели (ISO, с понедельника). */
    fun weekdayLabels(language: String = LocaleController.language.value): List<String> =
        if (language == "en") englishWeekdays else russianWeekdays

    /** Форматирует время как `HH:mm`. */
    fun formatHourMinute(dateTime: LocalDateTime): String =
        "${dateTime.hour.toString().padStart(2, '0')}:${dateTime.minute.toString().padStart(2, '0')}"

    /** Дата в формате «22 июля 2026» / «July 22, 2026». */
    fun formatLongDate(date: LocalDate, language: String = LocaleController.language.value): String {
        val month = monthName(date.monthNumber, language)
        return if (language == "en") {
            "$month ${date.dayOfMonth}, ${date.year}"
        } else {
            "${date.dayOfMonth} $month ${date.year}"
        }
    }

    /** Дата без года: «22 июля» / «July 22». */
    fun formatMediumDate(dateTime: LocalDateTime, language: String = LocaleController.language.value): String {
        val month = monthName(dateTime.monthNumber, language)
        return if (language == "en") {
            "$month ${dateTime.dayOfMonth}, ${dateTime.year}"
        } else {
            "${dateTime.dayOfMonth} $month ${dateTime.year}"
        }
    }

    /** Дата и время: «22 июля 2026, 14:30». */
    fun formatMediumDateTime(dateTime: LocalDateTime, language: String = LocaleController.language.value): String =
        "${formatMediumDate(dateTime, language)}, ${formatHourMinute(dateTime)}"

    /**
     * Собирает [LocalDateTime] из даты и времени сессии и переводит UTC → локальное время.
     * Если время в API пустое — возвращает null.
     */
    fun toLocalDateTime(date: String, time: String?): LocalDateTime? {
        if (time.isNullOrBlank()) return null
        return parseUtcSession(date, time).toLocalDateTime(TimeZone.currentSystemDefault())
    }

    /**
     * Парсит UTC-дату/время сессии. Пустое [time] → полночь UTC.
     */
    fun parseUtcSession(date: String, time: String?): Instant {
        val raw = time?.trim().orEmpty().removeSuffix("Z")
        val localTime = if (raw.isEmpty()) {
            LocalTime(0, 0, 0)
        } else {
            val parts = raw.split(":")
            require(parts.size >= 2) { "Invalid session time: $time" }
            LocalTime(
                hour = parts[0].toInt(),
                minute = parts[1].toInt(),
                second = parts.getOrNull(2)?.toInt() ?: 0,
            )
        }
        return LocalDateTime(LocalDate.parse(date), localTime).toInstant(TimeZone.UTC)
    }
}
