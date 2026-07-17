package com.example.f1_kmp.viewmodel

import com.example.f1_kmp.data.model.CircuitLocationModel
import com.example.f1_kmp.data.model.CircuitModel
import com.example.f1_kmp.data.model.RaceDateModel
import com.example.f1_kmp.data.model.RaceModel
import com.example.f1_kmp.data.repository.F1Repository
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
    private lateinit var repository: F1Repository

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

        val viewModel = ScheduleViewModel(repository)
        advanceUntilIdle()

        assertEquals(DayLogo.Finish, viewModel.logoForDay(LocalDate.parse("2026-05-10")))
    }

    @Test
    fun logoForDay_practiceDay_returnsCar() = runTest {
        val race = sampleRace(
            date = "2026-05-11",
            firstPractice = RaceDateModel("2026-05-10", "12:00:00Z"),
        )
        stubSchedule(listOf(race))

        val viewModel = ScheduleViewModel(repository)
        advanceUntilIdle()

        assertEquals(DayLogo.Car, viewModel.logoForDay(LocalDate.parse("2026-05-10")))
    }

    @Test
    fun onSelectDay_buildsScheduleItemsForPractice() = runTest {
        val race = sampleRace(
            date = "2026-05-11",
            firstPractice = RaceDateModel("2026-05-10", "12:00:00Z"),
        )
        stubSchedule(listOf(race))

        val viewModel = ScheduleViewModel(repository)
        advanceUntilIdle()

        viewModel.onSelectDay(LocalDate.parse("2026-05-10"))
        advanceUntilIdle()

        val items = viewModel.scheduleItems.value
        assertTrue(items.isNotEmpty())
        assertEquals("Monaco Grand Prix", items.first().raceName)
        assertEquals("Первая практика", items[1].title)
    }

    @Test
    fun loadAllData_success_setsRacesValue() = runTest {
        val race = sampleRace(date = "2026-05-10")
        stubSchedule(listOf(race))

        val viewModel = ScheduleViewModel(repository)
        advanceUntilIdle()

        assertTrue(viewModel.races.value is AsyncValue.Value)
        assertEquals(1, (viewModel.races.value as AsyncValue.Value).value.size)
    }

    private fun stubSchedule(races: List<RaceModel>) {
        coEvery { repository.peekScheduleCache() } returns null
        coEvery { repository.getCurrentSchedule() } returns Result.success(races)
    }

    private fun sampleRace(
        date: String,
        firstPractice: RaceDateModel? = null,
    ) = RaceModel(
        season = "2026",
        round = "7",
        url = "",
        raceName = "Monaco Grand Prix",
        circuit = CircuitModel(
            circuitId = "monaco",
            url = "",
            circuitName = "Monaco",
            location = CircuitLocationModel("43.7", "7.4", "Monte Carlo", "Monaco"),
        ),
        date = date,
        time = "13:00:00Z",
        firstPractice = firstPractice,
    )
}
