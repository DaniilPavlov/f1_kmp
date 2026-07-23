package com.example.f1_kmp.viewmodel

import com.example.f1_kmp.data.model.FinishStatusItem
import com.example.f1_kmp.data.repository.IF1Repository
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
 * Unit-тесты [FinishStatusViewModel] — экран «Статусы финиша».
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FinishStatusViewModelTest {
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

    /** Смена года (4 цифры) перезагружает статусы для выбранного сезона. */
    @Test
    fun onYearChanged_loadsStatusesForYear() = runTest {
        coEvery { repository.getSeasonYears() } returns Result.success(listOf("2026", "2025"))
        coEvery { repository.getSeasonFinishStatuses("2026") } returns Result.success(
            listOf(FinishStatusItem("1", "Finished", 100)),
        )
        coEvery { repository.getSeasonFinishStatuses("2025") } returns Result.success(
            listOf(FinishStatusItem("1", "Finished", 80)),
        )

        val viewModel = FinishStatusViewModel(repository)
        advanceUntilIdle()

        assertEquals("2026", viewModel.year.value)
        assertTrue(viewModel.statuses.value is AsyncValue.Value)
        assertEquals(100, (viewModel.statuses.value as AsyncValue.Value).value.first().count)

        viewModel.onYearChanged("2025")
        advanceUntilIdle()

        assertEquals("2025", viewModel.year.value)
        assertTrue(viewModel.statuses.value is AsyncValue.Value)
        assertEquals(80, (viewModel.statuses.value as AsyncValue.Value).value.first().count)
    }
}
