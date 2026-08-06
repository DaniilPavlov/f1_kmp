package com.example.f1_kmp.viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class H2hPointsTimelineTest {

    @Test
    fun fromScores_empty_isEmpty() {
        val timeline = H2hPointsTimeline.fromScores(emptyList(), emptyList())
        assertTrue(timeline.isEmpty)
        assertEquals(0.0, timeline.maxCumulative)
    }

    @Test
    fun fromScores_accumulatesAndLabelsAllTime() {
        val a = listOf(
            H2hRoundScore("2024", "1", "Bahrain", 25.0),
            H2hRoundScore("2024", "2", "Saudi", 18.0),
        )
        val b = listOf(
            H2hRoundScore("2024", "1", "Bahrain", 18.0),
            H2hRoundScore("2024", "3", "Australia", 25.0),
        )

        val timeline = H2hPointsTimeline.fromScores(a, b)
        assertEquals(3, timeline.points.size)
        assertEquals("2024", timeline.points[0].label)
        assertEquals(25.0, timeline.points[0].cumulativeA)
        assertEquals(18.0, timeline.points[0].cumulativeB)
        assertEquals(43.0, timeline.points[1].cumulativeA)
        assertEquals(18.0, timeline.points[1].cumulativeB)
        assertEquals(43.0, timeline.points[2].cumulativeA)
        assertEquals(43.0, timeline.points[2].cumulativeB)
        assertEquals(43.0, timeline.maxCumulative)
    }

    @Test
    fun fromScores_seasonScope_usesRoundLabel() {
        val a = listOf(H2hRoundScore("2026", "5", "Monaco", 25.0))
        val b = listOf(H2hRoundScore("2026", "5", "Monaco", 12.0))
        val timeline = H2hPointsTimeline.fromScores(a, b, seasonScope = "2026")
        assertEquals("5", timeline.points.single().label)
        assertEquals(25.0, timeline.points.single().roundPointsA)
        assertEquals(12.0, timeline.points.single().roundPointsB)
    }

    @Test
    fun fromScores_careerScope_usesYearOnlyLabels() {
        val a = listOf(H2hRoundScore("2026", "3", "Japan", 12.0))
        val timeline = H2hPointsTimeline.fromScores(a, emptyList(), seasonScope = null)
        assertEquals("2026", timeline.points.single().label)
    }

    @Test
    fun roundScore_keyAndNumber() {
        val score = H2hRoundScore("2025", "10", "X", 1.0)
        assertEquals("2025-10", score.key)
        assertEquals(10, score.roundNumber)
        assertEquals(0, H2hRoundScore("2025", "x", "X", 1.0).roundNumber)
    }
}
