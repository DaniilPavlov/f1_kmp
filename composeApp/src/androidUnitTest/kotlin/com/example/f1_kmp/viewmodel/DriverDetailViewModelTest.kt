package com.example.f1_kmp.viewmodel

import com.example.f1_kmp.data.model.CareerStats
import com.example.f1_kmp.data.model.EspnDriverCardData
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
class DriverDetailViewModelTest {
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
    fun load_success_setsDriverAndCareer() = runTest {
        val driver = Driver("verstappen", "", "Max", "Verstappen", "", "Dutch")
        val career = CareerStats(
            races = 200,
            wins = 60,
            podiums = 100,
            poles = 40,
            current = emptyList<Constructor>(),
            related = emptyList(),
            winRaces = emptyList(),
            poleRaces = emptyList(),
        )
        coEvery { repository.currentConstructorsForDriver("verstappen") } returns emptyList()
        coEvery { repository.getDriver("verstappen") } returns Result.success(driver)
        coEvery { repository.getDriverCareerStats("verstappen", emptyList()) } returns Result.success(career)
        coEvery { espn.driverCardData("Max", "Verstappen") } returns EspnDriverCardData()

        val vm = DriverDetailViewModel("verstappen", repository, espn)
        advanceUntilIdle()

        assertTrue(vm.driver.value is AsyncValue.Value)
        assertEquals("verstappen", (vm.driver.value as AsyncValue.Value).value.driverId)
        assertTrue(vm.careerStats.value is AsyncValue.Value)
        assertEquals(60, (vm.careerStats.value as AsyncValue.Value).value.wins)
    }
}
