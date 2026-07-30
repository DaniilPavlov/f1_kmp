package com.example.f1_kmp.viewmodel

import com.example.f1_kmp.data.model.CareerStats
import com.example.f1_kmp.data.repository.IEspnRepository
import com.example.f1_kmp.data.repository.IF1Repository
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

@OptIn(ExperimentalCoroutinesApi::class)
class ConstructorDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: IF1Repository
    private lateinit var espn: IEspnRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = mockk()
        espn = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun load_success_setsConstructorAndCareer() = runTest {
        val constructor = Constructor("red_bull", "", "Red Bull", "Austrian")
        val career = CareerStats(
            races = 300,
            wins = 100,
            podiums = 200,
            poles = 80,
            current = emptyList<Driver>(),
            related = emptyList(),
            winRaces = emptyList(),
            poleRaces = emptyList(),
        )
        coEvery { repository.currentDriversForConstructor("red_bull") } returns emptyList()
        coEvery { repository.getConstructor("red_bull") } returns Result.success(constructor)
        coEvery { repository.getConstructorCareerStats("red_bull", emptyList()) } returns Result.success(career)
        coEvery { espn.constructorNews("red_bull", "Red Bull") } returns emptyList()

        val vm = ConstructorDetailViewModel("red_bull", repository, espn)
        advanceUntilIdle()

        assertTrue(vm.constructor.value is AsyncValue.Value)
        assertEquals("red_bull", (vm.constructor.value as AsyncValue.Value).value.constructorId)
        assertTrue(vm.careerStats.value is AsyncValue.Value)
        assertEquals(100, (vm.careerStats.value as AsyncValue.Value).value.wins)
    }
}
