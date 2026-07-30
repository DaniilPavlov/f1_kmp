package com.example.f1_kmp.domain

import com.example.f1_kmp.data.firebase.IRemoteConfigService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ForceUpdateGateTest {
    private lateinit var remoteConfig: IRemoteConfigService
    private lateinit var gate: ForceUpdateGate

    @Before
    fun setUp() {
        remoteConfig = mockk()
        gate = ForceUpdateGate(remoteConfig)
    }

    @Test
    fun check_setsRequiredFromRemoteConfig() {
        every { remoteConfig.isUpdateRequired() } returns true
        gate.check()
        assertTrue(gate.required.value)

        every { remoteConfig.isUpdateRequired() } returns false
        gate.check()
        assertFalse(gate.required.value)
    }

    @Test
    fun onResume_refreshesThenChecks() = runTest {
        coEvery { remoteConfig.refresh() } returns Unit
        every { remoteConfig.isUpdateRequired() } returns true
        gate.onResume()
        assertTrue(gate.required.value)
    }
}
