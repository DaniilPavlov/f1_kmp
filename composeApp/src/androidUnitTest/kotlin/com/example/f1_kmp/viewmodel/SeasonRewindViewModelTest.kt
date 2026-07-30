package com.example.f1_kmp.viewmodel

import com.example.f1_kmp.data.analytics.AnalyticsGateway
import com.example.f1_kmp.data.repository.IF1Repository
import com.example.f1_kmp.domain.AsyncValue
import com.example.f1_kmp.domain.model.Circuit
import com.example.f1_kmp.domain.model.CircuitLocation
import com.example.f1_kmp.domain.model.Constructor
import com.example.f1_kmp.domain.model.ConstructorStanding
import com.example.f1_kmp.domain.model.Driver
import com.example.f1_kmp.domain.model.DriverStanding
import com.example.f1_kmp.domain.model.Race
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SeasonRewindViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: IF1Repository
    private lateinit var analytics: AnalyticsGateway

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = mockk()
        analytics = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_loadsCompletedRacesAndStandings() = runTest {
        val race = Race(
            season = "2024",
            round = "1",
            url = "",
            raceName = "Bahrain Grand Prix",
            circuit = Circuit(
                "bahrain", "", "Bahrain",
                CircuitLocation("0", "0", "Sakhir", "Bahrain"),
            ),
            date = "2024-03-02",
        )
        coEvery { repository.getSeasonYears() } returns Result.success(listOf("2024"))
        coEvery { repository.getSeasonRaces("2024") } returns Result.success(listOf(race))
        coEvery { repository.getStandingsAfterRound("2024", "1") } returns Result.success(
            listOf(
                DriverStanding(
                    "1", "1", "25", "1",
                    Driver("verstappen", "", "Max", "Verstappen", "", "Dutch", "VER"),
                    emptyList(),
                ),
            ) to listOf(
                ConstructorStanding(
                    "1", "1", "25", "1",
                    Constructor("red_bull", "", "Red Bull", "Austrian"),
                ),
            ),
        )

        val vm = SeasonRewindViewModel(repository, analytics)
        advanceUntilIdle()

        verify { analytics.log(any()) }
        assertEquals("2024", vm.uiState.value.year)
        assertTrue(vm.uiState.value.races is AsyncValue.Value)
        assertEquals(1, (vm.uiState.value.races as AsyncValue.Value).value.size)
        assertEquals(1, vm.uiState.value.driverBars.size)
        assertEquals("Verstappen", vm.uiState.value.driverBars.single().label)
    }
}
