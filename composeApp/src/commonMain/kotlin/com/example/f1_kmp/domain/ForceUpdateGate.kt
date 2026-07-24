package com.example.f1_kmp.domain

import com.example.f1_kmp.data.firebase.IRemoteConfigService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Гейт принудительного обновления по Remote Config [IRemoteConfigService.minAppVersion].
 *
 * GoF Creational Singleton — один экземпляр в Koin; UI и Application слушают [required].
 */
class ForceUpdateGate(
    private val remoteConfig: IRemoteConfigService,
) {
    private val _required = MutableStateFlow(false)
    val required: StateFlow<Boolean> = _required.asStateFlow()

    fun check() {
        _required.value = remoteConfig.isUpdateRequired()
    }

    suspend fun onResume() {
        remoteConfig.refresh()
        check()
    }
}
