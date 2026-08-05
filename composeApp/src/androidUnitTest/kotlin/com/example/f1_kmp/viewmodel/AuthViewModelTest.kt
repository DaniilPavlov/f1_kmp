package com.example.f1_kmp.viewmodel

import com.example.f1_kmp.data.repository.AuthErrorKeys
import com.example.f1_kmp.data.repository.IAuthRepository
import com.example.f1_kmp.domain.auth.AuthResult
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var authRepository: IAuthRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        authRepository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun signIn_emptyFields_setsErrorWithoutNetwork() = runTest {
        val vm = AuthViewModel(authRepository)
        var success = false
        vm.signIn { success = true }
        advanceUntilIdle()

        assertEquals(AuthErrorKeys.EMPTY_FIELDS, vm.uiState.value.errorKey)
        assertFalse(success)
        coVerify(exactly = 0) { authRepository.signIn(any(), any()) }
    }

    @Test
    fun register_disposableEmail_blocked() = runTest {
        val vm = AuthViewModel(authRepository)
        vm.setEmail("x@mailinator.com")
        vm.setPassword("Password1")
        var success = false
        vm.register { success = true }
        advanceUntilIdle()

        assertEquals(AuthErrorKeys.DISPOSABLE_EMAIL, vm.uiState.value.errorKey)
        assertFalse(success)
        coVerify(exactly = 0) { authRepository.register(any(), any()) }
    }

    @Test
    fun signIn_success_invokesCallback() = runTest {
        coEvery { authRepository.signIn("a@b.com", "Password1") } returns AuthResult.Ok
        val vm = AuthViewModel(authRepository)
        vm.setEmail("a@b.com")
        vm.setPassword("Password1")
        var success = false
        vm.signIn { success = true }
        advanceUntilIdle()

        assertTrue(success)
        assertNull(vm.uiState.value.errorKey)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun signIn_failure_setsErrorKey() = runTest {
        coEvery { authRepository.signIn(any(), any()) } returns AuthResult.Fail(AuthErrorKeys.WRONG_PASSWORD)
        val vm = AuthViewModel(authRepository)
        vm.setEmail("a@b.com")
        vm.setPassword("Password1")
        vm.signIn { }
        advanceUntilIdle()

        assertEquals(AuthErrorKeys.WRONG_PASSWORD, vm.uiState.value.errorKey)
    }

    @Test
    fun register_weakPassword_blocked() = runTest {
        val vm = AuthViewModel(authRepository)
        vm.setEmail("a@b.com")
        vm.setPassword("short")
        vm.register { }
        advanceUntilIdle()
        assertEquals(AuthErrorKeys.WEAK_PASSWORD, vm.uiState.value.errorKey)
        coVerify(exactly = 0) { authRepository.register(any(), any()) }
    }

    @Test
    fun register_success_invokesCallback() = runTest {
        coEvery { authRepository.register("a@b.com", "Password1") } returns AuthResult.Ok
        val vm = AuthViewModel(authRepository)
        vm.setEmail("a@b.com")
        vm.setPassword("Password1")
        var success = false
        vm.register { success = true }
        advanceUntilIdle()
        assertTrue(success)
    }

    @Test
    fun sendPasswordReset_emptyEmail_setsError() = runTest {
        val vm = AuthViewModel(authRepository)
        vm.sendPasswordReset()
        advanceUntilIdle()
        assertEquals(AuthErrorKeys.EMPTY_EMAIL, vm.uiState.value.errorKey)
    }

    @Test
    fun sendPasswordReset_success_setsFlag() = runTest {
        coEvery { authRepository.sendPasswordResetEmail("a@b.com") } returns AuthResult.Ok
        val vm = AuthViewModel(authRepository)
        vm.setEmail("a@b.com")
        vm.sendPasswordReset()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.passwordResetSent)
        vm.clearTransientFlags()
        assertFalse(vm.uiState.value.passwordResetSent)
    }

    @Test
    fun signIn_invalidEmail_setsError() = runTest {
        val vm = AuthViewModel(authRepository)
        vm.setEmail("not-an-email")
        vm.setPassword("Password1")
        vm.signIn { }
        advanceUntilIdle()
        assertEquals(AuthErrorKeys.INVALID_EMAIL, vm.uiState.value.errorKey)
    }
}
