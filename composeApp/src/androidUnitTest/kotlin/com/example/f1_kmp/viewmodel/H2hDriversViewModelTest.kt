package com.example.f1_kmp.viewmodel

import com.example.f1_kmp.data.model.DriverModel
import com.example.f1_kmp.data.model.H2hStats
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit-тесты [H2hDriversViewModel] — сравнение пилотов head-to-head.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class H2hDriversViewModelTest {
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

    /** Успешный [F1Repository.getDriverH2hStats] для обоих пилотов → [AsyncValue.Value]. */
    @Test
    fun compare_success_setsComparison() = runTest {
        val driverA = DriverModel("hamilton", "", "Lewis", "Hamilton", "", "British")
        val driverB = DriverModel("russell", "", "George", "Russell", "", "British")
        val statsA = H2hStats(races = 100, wins = 50, podiums = 80, poles = 40)
        val statsB = H2hStats(races = 80, wins = 5, podiums = 30, poles = 2)

        coEvery { repository.getSeasonYears() } returns Result.success(listOf("2026"))
        coEvery { repository.getDriverH2hStats("hamilton", null) } returns Result.success(statsA)
        coEvery { repository.getDriverH2hStats("russell", null) } returns Result.success(statsB)

        val viewModel = H2hDriversViewModel(repository)
        advanceUntilIdle()

        viewModel.setDriverA(driverA)
        viewModel.setDriverB(driverB)
        viewModel.compare()
        advanceUntilIdle()

        val state = viewModel.comparison.value
        assertTrue(state is AsyncValue.Value)
        val result = (state as AsyncValue.Value).value
        assertEquals("hamilton", result?.driverA?.driverId)
        assertEquals("russell", result?.driverB?.driverId)
        assertEquals(50, result?.statsA?.wins)
        assertEquals(5, result?.statsB?.wins)
    }
}
