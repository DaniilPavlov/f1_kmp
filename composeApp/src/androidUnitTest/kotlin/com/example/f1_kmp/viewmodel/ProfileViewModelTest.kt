package com.example.f1_kmp.viewmodel

import com.example.f1_kmp.data.repository.IAuthRepository
import com.example.f1_kmp.data.repository.IPredictorLeaderboardRepository
import com.example.f1_kmp.data.repository.IPredictorRepository
import com.example.f1_kmp.domain.NotificationsPreference
import com.example.f1_kmp.domain.auth.AuthResult
import com.example.f1_kmp.domain.auth.AuthUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var authRepository: IAuthRepository
    private lateinit var notifications: NotificationsPreference
    private lateinit var predictorRepository: IPredictorRepository
    private lateinit var leaderboardRepository: IPredictorLeaderboardRepository
    private lateinit var users: MutableStateFlow<AuthUser?>

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        users = MutableStateFlow(null)
        authRepository = mockk(relaxed = true)
        every { authRepository.userChanges } returns users
        every { authRepository.currentUser } answers { users.value }
        notifications = mockk(relaxed = true)
        every { notifications.raceRemindersEnabled } returns MutableStateFlow(true)
        every { notifications.practiceRemindersEnabled } returns MutableStateFlow(true)
        predictorRepository = mockk(relaxed = true)
        leaderboardRepository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun signOut_clearsPredictorCaches() = runTest {
        val vm = ProfileViewModel(authRepository, notifications, predictorRepository, leaderboardRepository)
        vm.signOut()
        advanceUntilIdle()
        coVerify { authRepository.signOut() }
        verify { predictorRepository.clearMemoryCache() }
        verify { leaderboardRepository.clearMemoryCache() }
    }

    @Test
    fun resendVerification_success_setsToast() = runTest {
        coEvery { authRepository.sendEmailVerification() } returns AuthResult.Ok
        val vm = ProfileViewModel(authRepository, notifications, predictorRepository, leaderboardRepository)
        vm.resendVerification()
        advanceUntilIdle()
        assertEquals(ProfileViewModel.TOAST_VERIFICATION_SENT, vm.uiState.value.toastMessageKey)
        vm.clearToast()
        assertNull(vm.uiState.value.toastMessageKey)
    }

    @Test
    fun refreshVerification_verified_setsToast() = runTest {
        coEvery { authRepository.refreshEmailVerification() } returns true
        val vm = ProfileViewModel(authRepository, notifications, predictorRepository, leaderboardRepository)
        vm.refreshVerification()
        advanceUntilIdle()
        assertEquals(ProfileViewModel.TOAST_EMAIL_VERIFIED, vm.uiState.value.toastMessageKey)
    }

    @Test
    fun setRaceReminders_delegates() = runTest {
        val vm = ProfileViewModel(authRepository, notifications, predictorRepository, leaderboardRepository)
        vm.setRaceRemindersEnabled(false)
        verify { notifications.setRaceRemindersEnabled(false) }
    }
}
