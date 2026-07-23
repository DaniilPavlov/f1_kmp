package com.example.f1_kmp.viewmodel

import com.example.f1_kmp.data.repository.IF1Repository
import com.example.f1_kmp.domain.AppError
import com.example.f1_kmp.domain.AsyncValue
import com.example.f1_kmp.domain.model.Circuit
import com.example.f1_kmp.domain.model.CircuitLocation
import com.example.f1_kmp.domain.model.Race
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RaceInfoScreenViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: IF1Repository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadAllData_success_setsRaceAndExtraSections() = runTest {
        val race = sampleRace()
        coEvery { repository.getRaceResults("2026", "5") } returns Result.success(race)
        coEvery { repository.getQualifyingResults("2026", "5") } returns Result.success(emptyList())
        coEvery { repository.getPitStopsWithDriverNames("2026", "5") } returns Result.success(emptyList())
        coEvery { repository.getSprintResults("2026", "5") } returns Result.success(emptyList())

        val viewModel = RaceInfoScreenViewModel("2026", "5", repository)
        advanceUntilIdle()

        assertTrue(viewModel.race.value is AsyncValue.Value)
        assertEquals("Monaco Grand Prix", (viewModel.race.value as AsyncValue.Value).value.raceName)
        assertTrue(viewModel.qualifying.value is AsyncValue.Value)
        assertTrue(viewModel.pitStops.value is AsyncValue.Value)
        assertTrue(viewModel.sprint.value is AsyncValue.Value)
    }

    @Test
    fun getRaceResults_failure_setsRaceError() = runTest {
        coEvery { repository.getRaceResults("2026", "5") } returns Result.failure(
            AppError("Соединение отсутствует").asException(),
        )

        val viewModel = RaceInfoScreenViewModel("2026", "5", repository)
        advanceUntilIdle()

        assertTrue(viewModel.race.value is AsyncValue.Error)
        assertNotNull(viewModel.error.value)
    }

    @Test
    fun getRaceResults_null_setsRaceNotFoundError() = runTest {
        coEvery { repository.getRaceResults("2026", "5") } returns Result.success(null)

        val viewModel = RaceInfoScreenViewModel("2026", "5", repository)
        advanceUntilIdle()

        assertTrue(viewModel.race.value is AsyncValue.Error)
        assertNotNull(viewModel.error.value)
    }

    private fun sampleRace() = Race(
        season = "2026",
        round = "5",
        url = "",
        raceName = "Monaco Grand Prix",
        circuit = Circuit(
            circuitId = "monaco",
            url = "",
            circuitName = "Monaco",
            location = CircuitLocation("43.7", "7.4", "Monte Carlo", "Monaco"),
        ),
        date = "2026-05-25",
        results = emptyList(),
    )
}
