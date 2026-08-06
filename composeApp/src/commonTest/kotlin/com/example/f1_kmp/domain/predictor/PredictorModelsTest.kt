package com.example.f1_kmp.domain.predictor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PredictorModelsTest {
    @Test
    fun weekend_totalPoints_andFirestoreRoundTrip() {
        val weekend = PredictorWeekendPrediction(
            round = "3",
            raceName = "Australia",
            qualifyingOrder = listOf("a", "b"),
            raceOrder = listOf("b", "a"),
            qualiPoints = 2,
            racePoints = 1,
        )
        assertEquals(3, weekend.totalPoints)
        assertTrue(weekend.hasAnyPoints)

        val restored = PredictorWeekendPrediction.fromFirestoreMap(weekend.toFirestoreMap())
        assertEquals(weekend.round, restored.round)
        assertEquals(weekend.qualifyingOrder, restored.qualifyingOrder)
        assertEquals(weekend.raceOrder, restored.raceOrder)
        assertEquals(2, restored.qualiPoints)
        assertEquals(1, restored.racePoints)
    }

    @Test
    fun season_upsertAndSummary() {
        val season = PredictorSeason(year = "2026")
            .upsertWeekend(PredictorWeekendPrediction(round = "1", qualiPoints = 1, racePoints = 2))
            .upsertWeekend(PredictorWeekendPrediction(round = "2", qualiPoints = 0, racePoints = 3))
        assertEquals(6, season.totalPoints)
        val summary = PredictorSeasonSummary.fromSeason(season)
        assertEquals("2026", summary.year)
        assertEquals(2, summary.weekendCount)
        assertEquals(6, summary.totalPoints)
    }

    @Test
    fun store_weekendLookup() {
        val store = PredictorStore.empty()
            .upsertWeekend("2026", PredictorWeekendPrediction(round = "5", raceName = "Japan"))
        assertEquals("Japan", store.weekend("2026", "5")?.raceName)
        assertEquals(null, store.weekend("2025", "5"))
    }

    @Test
    fun sessionCompare_marksCorrectRows() {
        val compare = PredictorSessionCompare.fromOrders(listOf("a", "b", "c"), listOf("a", "x", "c"))
        assertEquals(2, compare.points)
        assertTrue(compare.rows[0].isCorrect)
        assertFalse(compare.rows[1].isCorrect)
        assertTrue(compare.rows[2].isCorrect)
    }

    @Test
    fun leaderboardProfile_optInRequiresNickname() {
        assertFalse(PredictorLeaderboardProfile(nickname = "Ace", leaderboardOptIn = false).canShowOnLeaderboard)
        assertFalse(PredictorLeaderboardProfile(nickname = null, leaderboardOptIn = true).canShowOnLeaderboard)
        assertTrue(PredictorLeaderboardProfile(nickname = "Ace", leaderboardOptIn = true).canShowOnLeaderboard)
    }

    @Test
    fun season_fromFirestoreMap_roundTrips() {
        val season = PredictorSeason(year = "2026")
            .upsertWeekend(
                PredictorWeekendPrediction(
                    round = "1",
                    raceName = "Bahrain",
                    qualifyingOrder = listOf("a"),
                    raceOrder = listOf("a"),
                    qualiPoints = 1,
                    racePoints = 1,
                ),
            )
        val restored = PredictorSeason.fromFirestoreMap("2026", season.toFirestoreMap())
        assertEquals(1, restored.weekends.size)
        assertEquals("Bahrain", restored.weekends["1"]?.raceName)
        assertEquals(2, restored.totalPoints)
        assertEquals(listOf("1"), restored.weekendsSorted.map { it.round })
    }

    @Test
    fun store_emptyAndUpsert() {
        val empty = PredictorStore.empty()
        assertEquals(null, empty.season("2026"))
        val next = empty.upsertWeekend("2026", PredictorWeekendPrediction(round = "2", raceName = "Jeddah"))
        assertEquals("Jeddah", next.weekend("2026", "2")?.raceName)
        assertEquals(null, next.weekend("2026", "9"))
    }

    @Test
    fun leaderboardEntry_firestoreRoundTrip() {
        val entry = PredictorLeaderboardEntry(uid = "u1", nickname = "Ace", totalPoints = 12)
        val restored = PredictorLeaderboardEntry.fromFirestoreMap("u1", entry.toFirestoreMap())
        assertEquals("Ace", restored.nickname)
        assertEquals(12, restored.totalPoints)
    }

    @Test
    fun profile_fromFirestoreMap_defaults() {
        val empty = PredictorLeaderboardProfile.fromFirestoreMap(null)
        assertEquals(null, empty.nickname)
        assertFalse(empty.leaderboardOptIn)
        val filled = PredictorLeaderboardProfile.fromFirestoreMap(
            mapOf(
                "nickname" to "Ace",
                "nicknameNormalized" to "ace",
                "leaderboardOptIn" to true,
            ),
        )
        assertEquals("Ace", filled.nickname)
        assertTrue(filled.canShowOnLeaderboard)
    }
}
