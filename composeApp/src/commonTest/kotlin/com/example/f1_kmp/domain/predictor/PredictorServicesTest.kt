package com.example.f1_kmp.domain.predictor

import com.example.f1_kmp.domain.model.Circuit
import com.example.f1_kmp.domain.model.CircuitLocation
import com.example.f1_kmp.domain.model.Constructor
import com.example.f1_kmp.domain.model.Driver
import com.example.f1_kmp.domain.model.QualifyingResult
import com.example.f1_kmp.domain.model.Race
import com.example.f1_kmp.domain.model.RaceResult
import com.example.f1_kmp.domain.model.RaceSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

class PredictorServicesTest {
    @Test
    fun scoreOrders_countsExactMatchesOnly() {
        assertEquals(2, PredictorScoreService.scoreOrders(listOf("a", "b", "c"), listOf("a", "x", "c")))
        assertEquals(0, PredictorScoreService.scoreOrders(listOf("a"), listOf("b")))
        assertEquals(0, PredictorScoreService.scoreOrders(emptyList(), listOf("a")))
    }

    @Test
    fun defaultPredictorOrder_championshipFirstThenRest() {
        val order = PredictorOrder.defaultPredictorOrder(
            rosterIds = listOf("c", "a", "b"),
            championshipOrder = listOf("a", "b", "z"),
        )
        assertEquals(listOf("a", "b", "c"), order)
    }

    @Test
    fun syncOrderToRoster_keepsSavedAndAppendsMissing() {
        val synced = PredictorOrder.syncOrderToRoster(
            saved = listOf("a", "gone", "b"),
            rosterIds = listOf("b", "a", "c"),
        )
        assertEquals(listOf("a", "b", "c"), synced)
    }

    @Test
    fun nickname_validate() {
        assertNull(PredictorNickname.validate("Ace_1"))
        assertEquals("predictorNicknameErrorLength", PredictorNickname.validate("ab"))
        assertEquals("predictorNicknameErrorChars", PredictorNickname.validate("bad nick"))
        assertEquals("ace_1", PredictorNickname.normalize(" Ace_1 "))
    }

    @Test
    fun hasUsableDriverCode() {
        assertTrue(PredictorOrder.hasUsableDriverCode(driver(code = "VER")))
        assertFalse(PredictorOrder.hasUsableDriverCode(driver(code = "none")))
        assertFalse(PredictorOrder.hasUsableDriverCode(driver(code = null)))
    }

    @Test
    fun lock_oneHourBeforeQualifying() {
        val race = raceWithQuali(date = "2026-05-24", time = "14:00:00Z")
        val lockAt = PredictorLock.lockAt(race)!!
        val tz = TimeZone.currentSystemDefault()
        val qualiLocal = LocalDateTime(2026, 5, 24, 14, 0, 0)
            .toInstant(TimeZone.UTC)
            .toLocalDateTime(tz)
        val lockInstant = lockAt.toInstant(tz)
        val expectedInstant = qualiLocal.toInstant(tz).minus(kotlin.time.Duration.parse("1h"))
        assertEquals(expectedInstant, lockInstant)
        assertFalse(
            PredictorLock.isLocked(
                race,
                lockInstant.minus(kotlin.time.Duration.parse("1m")).toLocalDateTime(tz),
            ),
        )
        assertTrue(
            PredictorLock.isLocked(
                race,
                lockInstant.plus(kotlin.time.Duration.parse("1s")).toLocalDateTime(tz),
            ),
        )
    }

    @Test
    fun applyResults_scoresBothSessions() {
        val weekend = PredictorWeekendPrediction(
            round = "1",
            qualifyingOrder = listOf("a", "b"),
            raceOrder = listOf("b", "a"),
        )
        val scored = PredictorScoreService.applyResults(
            weekend = weekend,
            actualQualifyingOrder = listOf("a", "x"),
            actualRaceOrder = listOf("b", "a"),
        )
        assertEquals(1, scored.qualiPoints)
        assertEquals(2, scored.racePoints)
    }

    @Test
    fun qualifyingAndRaceActualOrder_sortByPosition() {
        val ctor = Constructor(constructorId = "m", url = "", name = "M", nationality = "")
        val q = PredictorScoreService.qualifyingActualOrder(
            listOf(
                QualifyingResult(number = "1", position = "2", driver = driver(id = "b"), constructor = ctor),
                QualifyingResult(number = "2", position = "1", driver = driver(id = "a"), constructor = ctor),
            ),
        )
        assertEquals(listOf("a", "b"), q)

        val r = PredictorScoreService.raceActualOrder(
            listOf(
                RaceResult(
                    number = "1",
                    position = "2",
                    positionText = "2",
                    points = "0",
                    driver = driver(id = "b"),
                    constructor = ctor,
                    grid = "1",
                    laps = "50",
                    status = "Finished",
                ),
                RaceResult(
                    number = "2",
                    position = "1",
                    positionText = "1",
                    points = "25",
                    driver = driver(id = "a"),
                    constructor = ctor,
                    grid = "2",
                    laps = "50",
                    status = "Finished",
                ),
            ),
        )
        assertEquals(listOf("a", "b"), r)
    }

    private fun driver(id: String = "x", code: String? = "XXX") = Driver(
        driverId = id,
        url = "",
        givenName = "A",
        familyName = "B",
        dateOfBirth = "",
        nationality = "",
        code = code,
    )

    private fun raceWithQuali(date: String, time: String) = Race(
        season = "2026",
        round = "1",
        url = "",
        raceName = "Test",
        circuit = Circuit(
            circuitId = "c",
            url = "",
            circuitName = "C",
            location = CircuitLocation(lat = "0", longitude = "0", locality = "", country = ""),
        ),
        date = date,
        time = time,
        qualifying = RaceSession(date = date, time = time),
    )
}
