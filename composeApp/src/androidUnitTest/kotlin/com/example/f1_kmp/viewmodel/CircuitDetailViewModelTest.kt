package com.example.f1_kmp.viewmodel

import com.example.f1_kmp.data.circuits.CircuitStats
import com.example.f1_kmp.data.circuits.CircuitStatsRepository
import com.example.f1_kmp.data.repository.IF1Repository
import com.example.f1_kmp.domain.AppError
import com.example.f1_kmp.domain.AsyncValue
import com.example.f1_kmp.domain.model.Circuit
import com.example.f1_kmp.domain.model.CircuitLocation
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CircuitDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: IF1Repository
    private lateinit var circuitStatsRepository: CircuitStatsRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = mockk()
        circuitStatsRepository = mockk()
        coEvery { repository.peekCircuitsCache() } returns null
        coEvery { circuitStatsRepository.of("monaco") } returns CircuitStats(
            lengthKm = 3.337,
            laps = 78,
            turns = 19,
            topSpeedKmh = 290.0,
            elevationM = 42.0,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadAllData_success_setsCircuitAndWinners() = runTest {
        val circuit = sampleCircuit()
        coEvery { repository.getCircuitById("monaco") } returns Result.success(circuit)
        coEvery { repository.getCircuitWinners("monaco") } returns Result.success(emptyList())

        val viewModel = CircuitDetailViewModel("monaco", repository, circuitStatsRepository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.circuit is AsyncValue.Value)
        assertEquals("Monaco", (viewModel.uiState.value.circuit as AsyncValue.Value).value.circuitName)
        assertTrue(viewModel.uiState.value.winners is AsyncValue.Value)
        assertEquals(0, (viewModel.uiState.value.winners as AsyncValue.Value).value.size)
    }

    @Test
    fun getCircuitById_failure_setsCircuitError() = runTest {
        coEvery { repository.getCircuitById("monaco") } returns Result.failure(
            AppError("Соединение отсутствует").asException(),
        )
        coEvery { repository.getCircuitWinners("monaco") } returns Result.success(emptyList())

        val viewModel = CircuitDetailViewModel("monaco", repository, circuitStatsRepository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.circuit is AsyncValue.Error)
    }

    @Test
    fun getCircuitById_null_setsCircuitNotFoundError() = runTest {
        coEvery { repository.getCircuitById("monaco") } returns Result.success(null)
        coEvery { repository.getCircuitWinners("monaco") } returns Result.success(emptyList())

        val viewModel = CircuitDetailViewModel("monaco", repository, circuitStatsRepository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.circuit is AsyncValue.Error)
    }

    private fun sampleCircuit() = Circuit(
        circuitId = "monaco",
        url = "",
        circuitName = "Monaco",
        location = CircuitLocation("43.7", "7.4", "Monte Carlo", "Monaco"),
    )
}
