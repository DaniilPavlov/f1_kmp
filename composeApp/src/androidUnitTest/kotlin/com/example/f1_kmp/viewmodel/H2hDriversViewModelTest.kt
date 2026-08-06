package com.example.f1_kmp.viewmodel

import com.example.f1_kmp.data.model.H2hEntityCompareData
import com.example.f1_kmp.data.model.H2hStats
import com.example.f1_kmp.data.repository.IF1Repository
import com.example.f1_kmp.domain.AppError
import com.example.f1_kmp.domain.AsyncValue
import com.example.f1_kmp.domain.model.Constructor
import com.example.f1_kmp.domain.model.Driver
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
    private lateinit var repository: IF1Repository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = mockk()
        coEvery { repository.getSeasonYears() } returns Result.success(listOf("2026"))
        coEvery { repository.currentConstructorsForDriver(any()) } returns emptyList()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Успешный [IF1Repository.getDriverH2hCompareData] для обоих пилотов → [AsyncValue.Value]. */
    @Test
    fun compare_success_setsComparison() = runTest {
        val driverA = Driver("hamilton", "", "Lewis", "Hamilton", "", "British")
        val driverB = Driver("russell", "", "George", "Russell", "", "British")
        val statsA = H2hStats(races = 100, wins = 50, podiums = 80, poles = 40)
        val statsB = H2hStats(races = 80, wins = 5, podiums = 30, poles = 2)

        coEvery { repository.getDriverH2hCompareData("hamilton", null) } returns Result.success(
            H2hEntityCompareData(statsA, emptyList()),
        )
        coEvery { repository.getDriverH2hCompareData("russell", null) } returns Result.success(
            H2hEntityCompareData(statsB, emptyList()),
        )
        coEvery { repository.currentConstructorsForDriver("hamilton") } returns
            listOf(Constructor("mercedes", "", "Mercedes", "German"))
        coEvery { repository.currentConstructorsForDriver("russell") } returns
            listOf(Constructor("mercedes", "", "Mercedes", "German"))

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
        assertEquals("mercedes", result?.constructorIdA)
        assertEquals("mercedes", result?.constructorIdB)
    }

    @Test
    fun compare_failure_setsComparisonError() = runTest {
        coEvery { repository.getDriverH2hCompareData("hamilton", null) } returns Result.failure(
            AppError("Соединение отсутствует").asException(),
        )
        coEvery { repository.getDriverH2hCompareData("russell", null) } returns Result.success(
            H2hEntityCompareData(H2hStats(1, 0, 0, 0), emptyList()),
        )

        val viewModel = H2hDriversViewModel(repository)
        advanceUntilIdle()
        viewModel.setDriverA(Driver("hamilton", "", "Lewis", "Hamilton", "", "British"))
        viewModel.setDriverB(Driver("russell", "", "George", "Russell", "", "British"))
        viewModel.compare()
        advanceUntilIdle()

        val state = viewModel.comparison.value
        assertTrue(state is AsyncValue.Error)
        assertEquals("Соединение отсутствует", (state as AsyncValue.Error).message)
    }
}
