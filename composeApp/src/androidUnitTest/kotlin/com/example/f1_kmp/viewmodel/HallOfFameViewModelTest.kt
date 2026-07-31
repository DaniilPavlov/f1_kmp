package com.example.f1_kmp.viewmodel

import com.example.f1_kmp.data.repository.IF1Repository
import com.example.f1_kmp.domain.AsyncValue
import com.example.f1_kmp.domain.model.Constructor
import com.example.f1_kmp.domain.model.ConstructorStanding
import com.example.f1_kmp.domain.model.Driver
import com.example.f1_kmp.domain.model.DriverStanding
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
class HallOfFameViewModelTest {
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
    fun init_loadsHistoricalStandingsForLatestYear() = runTest {
        val drivers = listOf(
            DriverStanding(
                "1", "1", "100", "5",
                Driver("verstappen", "", "Max", "Verstappen", "", "Dutch"),
                listOf(Constructor("red_bull", "", "Red Bull", "Austrian")),
            ),
        )
        val constructors = listOf(
            ConstructorStanding(
                "1", "1", "200", "6",
                Constructor("red_bull", "", "Red Bull", "Austrian"),
            ),
        )
        coEvery { repository.getSeasonYears() } returns Result.success(listOf("2024", "2023"))
        coEvery { repository.peekHistoricalStandingsCache("2024") } returns null
        coEvery { repository.getHistoricalStandings("2024") } returns Result.success(drivers to constructors)

        val vm = HallOfFameViewModel(repository)
        advanceUntilIdle()

        assertEquals("2024", vm.uiState.value.year)
        assertTrue(vm.uiState.value.drivers is AsyncValue.Value)
        assertEquals(1, (vm.uiState.value.drivers as AsyncValue.Value).value.size)
        assertEquals(1, (vm.uiState.value.constructors as AsyncValue.Value).value.size)
    }
}
