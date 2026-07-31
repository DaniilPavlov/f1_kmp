package com.example.f1_kmp.viewmodel

import androidx.lifecycle.ViewModel
import com.example.f1_kmp.domain.model.CircuitLocation
import com.example.f1_kmp.domain.model.Circuit
import com.example.f1_kmp.data.model.EspnScoreboardEvent
import com.example.f1_kmp.domain.model.Race
import com.example.f1_kmp.data.repository.IEspnRepository
import com.example.f1_kmp.data.repository.IF1Repository
import com.example.f1_kmp.domain.AppException
import com.example.f1_kmp.domain.AsyncValue
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

/**
 * Unit-тесты [ResultsViewModel] — экран «Последняя гонка».
 *
 * ViewModel в [init] сразу вызывает [ResultsViewModel.loadAllData], поэтому достаточно
 * создать mock Repository, затем ViewModel — загрузка стартует сама.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ResultsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: IF1Repository
    private lateinit var espnRepository: IEspnRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = mockk()
        espnRepository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Успешный [IF1Repository.getLastRace] → [AsyncValue.Value] с названием гонки. */
    @Test
    fun loadAllData_success_updatesLastRace() = runTest {
        val race = sampleRace()
        coEvery { repository.peekLastRaceCache() } returns null
        coEvery { repository.getLastRace() } returns Result.success(race)
        coEvery { espnRepository.isScoreboardFresh } returns false
        coEvery { espnRepository.peekScoreboard } returns null
        coEvery { espnRepository.getScoreboardEvent(forceRefresh = false) } returns Result.success(null)

        val viewModel = ResultsViewModel(repository, espnRepository, mockk(relaxed = true))
        advanceUntilIdle()

        val state = viewModel.uiState.value.lastRace
        assertTrue(state is AsyncValue.Value)
        assertEquals("Monaco Grand Prix", (state as AsyncValue.Value).value.raceName)
    }

    /** Ошибка сети/сервера → [AsyncValue.Error], UI покажет ErrorBody. */
    @Test
    fun loadAllData_failure_setsError() = runTest {
        coEvery { repository.peekLastRaceCache() } returns null
        coEvery { repository.getLastRace() } returns Result.failure(
            AppException("Соединение отсутствует"),
        )
        coEvery { espnRepository.isScoreboardFresh } returns false
        coEvery { espnRepository.peekScoreboard } returns null
        coEvery { espnRepository.getScoreboardEvent(forceRefresh = false) } returns Result.success(null)

        val viewModel = ResultsViewModel(repository, espnRepository, mockk(relaxed = true))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.lastRace is AsyncValue.Error)
    }

    /** Успешный ESPN scoreboard → [ResultsViewModel.scoreboard] как [AsyncValue.Value]. */
    @Test
    fun loadAllData_scoreboardSuccess_setsScoreboardValue() = runTest {
        val race = sampleRace()
        val scoreboard = sampleScoreboard()
        coEvery { repository.peekLastRaceCache() } returns null
        coEvery { repository.getLastRace() } returns Result.success(race)
        coEvery { espnRepository.isScoreboardFresh } returns false
        coEvery { espnRepository.peekScoreboard } returns null
        coEvery { espnRepository.getScoreboardEvent(forceRefresh = false) } returns Result.success(scoreboard)

        val viewModel = ResultsViewModel(repository, espnRepository, mockk(relaxed = true))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.scoreboard is AsyncValue.Value)
        assertEquals("Monaco Grand Prix", (viewModel.uiState.value.scoreboard as AsyncValue.Value).value?.name)
    }

    /** [ViewModel.onCleared] останавливает pollJob без падения (без live-loop в тесте). */
    @Test
    fun onCleared_stopsWithoutCrash() = runTest {
        val race = sampleRace()
        coEvery { repository.peekLastRaceCache() } returns null
        coEvery { repository.getLastRace() } returns Result.success(race)
        coEvery { espnRepository.isScoreboardFresh } returns false
        coEvery { espnRepository.peekScoreboard } returns null
        coEvery { espnRepository.getScoreboardEvent(forceRefresh = false) } returns Result.success(sampleScoreboard())

        val viewModel = ResultsViewModel(repository, espnRepository, mockk(relaxed = true))
        advanceUntilIdle()

        invokeOnCleared(viewModel)
        assertTrue(viewModel.uiState.value.scoreboard is AsyncValue.Value)
    }

    /** Минимальная заготовка [Race] — не тянем полный JSON из API. */
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

    private fun sampleScoreboard() = EspnScoreboardEvent(
        name = "Monaco Grand Prix",
        shortName = "Monaco GP",
        statusState = "post",
        statusDetail = "Finished",
    )

    private fun invokeOnCleared(viewModel: ViewModel) {
        val method = ViewModel::class.java.getDeclaredMethod("onCleared")
        method.isAccessible = true
        method.invoke(viewModel)
    }
}
