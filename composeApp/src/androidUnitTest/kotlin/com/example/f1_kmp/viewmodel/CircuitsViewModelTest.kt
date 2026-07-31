package com.example.f1_kmp.viewmodel

import com.example.f1_kmp.data.repository.IF1Repository
import com.example.f1_kmp.domain.AppDataRefresh
import com.example.f1_kmp.domain.AsyncValue
import com.example.f1_kmp.domain.model.Circuit
import com.example.f1_kmp.domain.model.CircuitLocation
import io.mockk.coEvery
import io.mockk.coVerify
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
class CircuitsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: IF1Repository
    private lateinit var refresh: AppDataRefresh

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = mockk()
        refresh = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun load_success_andChangePage() = runTest {
        val circuits = listOf(
            Circuit("monaco", "", "Monaco", CircuitLocation("0", "0", "Monte Carlo", "Monaco")),
        )
        coEvery { repository.peekCircuitsCache() } returns null
        coEvery { repository.getCircuits() } returns Result.success(circuits)

        val vm = CircuitsViewModel(repository, refresh)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.circuits is AsyncValue.Value)
        assertEquals(1, (vm.uiState.value.circuits as AsyncValue.Value).value.size)
        vm.changeActivePage(1)
        assertEquals(1, vm.uiState.value.activePage)

        vm.refreshAll()
        advanceUntilIdle()
        coVerify { refresh.clearAll() }
    }
}
