package com.example.f1_kmp.viewmodel

import com.example.f1_kmp.data.model.H2hStats
import com.example.f1_kmp.data.repository.IF1Repository
import com.example.f1_kmp.domain.AsyncValue
import com.example.f1_kmp.domain.model.Constructor
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
class H2hConstructorsViewModelTest {
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
    fun compare_success_setsComparison() = runTest {
        val a = Constructor("red_bull", "", "Red Bull", "Austrian")
        val b = Constructor("mercedes", "", "Mercedes", "German")
        coEvery { repository.getSeasonYears() } returns Result.success(listOf("2026"))
        coEvery { repository.getConstructorH2hStats("red_bull", null) } returns
            Result.success(H2hStats(100, 50, 80, 40))
        coEvery { repository.getConstructorH2hStats("mercedes", null) } returns
            Result.success(H2hStats(100, 40, 70, 30))
        coEvery { repository.getConstructorH2hRoundScores(any(), any()) } returns Result.success(emptyList())

        val vm = H2hConstructorsViewModel(repository)
        advanceUntilIdle()
        vm.setConstructorA(a)
        vm.setConstructorB(b)
        vm.compare()
        advanceUntilIdle()

        val state = vm.comparison.value
        assertTrue(state is AsyncValue.Value)
        assertEquals("red_bull", (state as AsyncValue.Value).value?.constructorA?.constructorId)
    }
}
