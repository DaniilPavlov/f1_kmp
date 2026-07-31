package com.example.f1_kmp.domain.live

import com.example.f1_kmp.data.model.EspnScoreboardEvent
import com.example.f1_kmp.data.model.EspnScoreboardSession
import com.example.f1_kmp.data.repository.IEspnRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LiveWeekendControllerTest {
    private lateinit var espn: IEspnRepository
    private lateinit var controller: LiveWeekendController

    @Before
    fun setUp() {
        espn = mockk()
        controller = LiveWeekendController(espn)
    }

    @Test
    fun loadScoreboard_live_updatesStateAndAbbreviation() = runBlocking {
        val session = EspnScoreboardSession(
            abbreviation = "R",
            statusState = "in",
            statusDetail = "Lap 12",
        )
        val event = EspnScoreboardEvent(
            name = "Monaco",
            shortName = "MON",
            statusState = "in",
            statusDetail = "Live",
            sessions = listOf(session),
        )
        coEvery { espn.getScoreboardEvent(any()) } returns Result.success(event)

        controller.loadScoreboard()
        withTimeout(3_000) {
            while (controller.scoreboard.value == null) delay(20)
        }

        assertEquals(event, controller.scoreboard.value)
        assertTrue(controller.isLive)
        assertEquals("R", controller.liveSessionAbbreviation)

        controller.onAppBackground()
        controller.stopLivePolling()
    }

    @Test
    fun loadScoreboard_failure_keepsNullWhenEmpty() = runBlocking {
        coEvery { espn.getScoreboardEvent(any()) } returns Result.failure(RuntimeException("x"))
        controller.loadScoreboard()
        delay(100)
        assertNull(controller.scoreboard.value)
        assertFalse(controller.isLive)
    }
}
