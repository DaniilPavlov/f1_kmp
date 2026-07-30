package com.example.f1_kmp.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit-тесты [DateUtils] и [YearMonth] — чистая логика дат без сети и UI.
 */
class DateUtilsTest {

    @Test
    fun isSameDay_equalDates_returnsTrue() {
        val date = LocalDate(2026, 5, 10)
        assertTrue(DateUtils.isSameDay(date, date))
    }

    @Test
    fun isSameDay_differentDates_returnsFalse() {
        assertFalse(
            DateUtils.isSameDay(
                LocalDate(2026, 5, 10),
                LocalDate(2026, 5, 11),
            ),
        )
    }

    @Test
    fun monthName_returnsRussianName() {
        assertEquals("Март", DateUtils.monthName(3))
    }

    @Test
    fun formatHourMinute_padsSingleDigits() {
        val dateTime = LocalDateTime(2026, 5, 10, 9, 5)
        assertEquals("09:05", DateUtils.formatHourMinute(dateTime))
    }

    @Test
    fun toLocalDateTime_blankTime_returnsNull() {
        assertNull(DateUtils.toLocalDateTime("2026-05-10", null))
        assertNull(DateUtils.toLocalDateTime("2026-05-10", "   "))
    }

    @Test
    fun toLocalDateTime_validTime_returnsLocalDateTime() {
        val result = DateUtils.toLocalDateTime("2026-05-10", "14:00:00Z")
        assertEquals(2026, result!!.year)
        assertEquals(5, result.monthNumber)
        assertEquals(10, result.dayOfMonth)
    }

    @Test
    fun yearMonth_lengthOfMonth_februaryNonLeap() {
        assertEquals(28, YearMonth(2025, 2).lengthOfMonth())
    }

    @Test
    fun yearMonth_lengthOfMonth_februaryLeap() {
        assertEquals(29, YearMonth(2024, 2).lengthOfMonth())
    }

    @Test
    fun yearMonth_plusMonths_wrapsYear() {
        assertEquals(YearMonth(2026, 3), YearMonth(2025, 12).plusMonths(3))
    }

    @Test
    fun yearMonth_atDay_createsLocalDate() {
        assertEquals(LocalDate(2026, 7, 17), YearMonth(2026, 7).atDay(17))
    }

    @Test
    fun formatLongAndMedium_en() {
        val date = LocalDate(2026, 3, 15)
        assertTrue(DateUtils.formatLongDate(date, "en").contains("2026"))
        val dt = LocalDateTime(2026, 3, 15, 14, 30)
        assertTrue(DateUtils.formatMediumDate(dt, "en").isNotBlank())
        assertTrue(DateUtils.formatMediumDateTime(dt, "en").contains("14"))
        assertEquals(7, DateUtils.weekdayLabels("en").size)
        assertEquals(7, DateUtils.weekdayLabels("ru").size)
    }

    @Test
    fun parseUtcSession_blankTime_isMidnightUtc() {
        val instant = DateUtils.parseUtcSession("2026-05-10", null)
        assertEquals(2026, instant.toLocalDateTime(TimeZone.UTC).year)
    }
}
