package com.example.f1_kmp.viewmodel

import com.example.f1_kmp.domain.model.CircuitLocation
import com.example.f1_kmp.domain.model.Circuit
import com.example.f1_kmp.domain.model.RaceSession
import com.example.f1_kmp.domain.model.Race
import com.example.f1_kmp.data.repository.IF1Repository
import com.example.f1_kmp.domain.AppDataRefresh
import com.example.f1_kmp.domain.AsyncValue
import com.example.f1_kmp.onlineReachability
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit-тесты [ScheduleViewModel]: иконки дней и построение списка сессий.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModelTest {
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
    fun logoForDay_raceDay_returnsFinish() = runTest {
        val race = sampleRace(date = "2026-05-10")
        stubSchedule(listOf(race))

        val viewModel = ScheduleViewModel(repository, mockk<AppDataRefresh>(relaxed = true), onlineReachability())
        advanceUntilIdle()

        assertEquals(DayLogo.Finish, viewModel.logoForDay(LocalDate.parse("2026-05-10")))
    }

    @Test
    fun logoForDay_practiceDay_returnsCar() = runTest {
        val race = sampleRace(
            date = "2026-05-11",
            firstPractice = RaceSession("2026-05-10", "12:00:00Z"),
        )
        stubSchedule(listOf(race))

        val viewModel = ScheduleViewModel(repository, mockk<AppDataRefresh>(relaxed = true), onlineReachability())
        advanceUntilIdle()

        assertEquals(DayLogo.Car, viewModel.logoForDay(LocalDate.parse("2026-05-10")))
    }

    @Test
    fun onSelectDay_buildsScheduleItemsForPractice() = runTest {
        val race = sampleRace(
            date = "2026-05-11",
            firstPractice = RaceSession("2026-05-10", "12:00:00Z"),
        )
        stubSchedule(listOf(race))

        val viewModel = ScheduleViewModel(repository, mockk<AppDataRefresh>(relaxed = true), onlineReachability())
        advanceUntilIdle()

        viewModel.onSelectDay(LocalDate.parse("2026-05-10"))
        advanceUntilIdle()

        val items = viewModel.uiState.value.scheduleItems
        assertTrue(items.isNotEmpty())
        assertEquals("Monaco Grand Prix", items.first().raceName)
        assertEquals("Первая практика", items[1].title)
    }

    @Test
    fun loadAllData_success_setsRacesValue() = runTest {
        val race = sampleRace(date = "2026-05-10")
        stubSchedule(listOf(race))

        val viewModel = ScheduleViewModel(repository, mockk<AppDataRefresh>(relaxed = true), onlineReachability())
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.races is AsyncValue.Value)
        assertEquals(1, (viewModel.uiState.value.races as AsyncValue.Value).value.size)
    }

    private fun stubSchedule(races: List<Race>) {
        coEvery { repository.peekScheduleCache() } returns null
        coEvery { repository.getCurrentSchedule() } returns Result.success(races)
    }

    private fun sampleRace(
        date: String,
        firstPractice: RaceSession? = null,
    ) = Race(
        season = "2026",
        round = "7",
        url = "",
        raceName = "Monaco Grand Prix",
        circuit = Circuit(
            circuitId = "monaco",
            url = "",
            circuitName = "Monaco",
            location = CircuitLocation("43.7", "7.4", "Monte Carlo", "Monaco"),
        ),
        date = date,
        time = "13:00:00Z",
        firstPractice = firstPractice,
    )
}
