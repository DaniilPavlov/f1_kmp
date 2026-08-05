package com.example.f1_kmp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.f1_kmp.data.repository.IAuthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Только auth-флаги для PredictorAuthGate (без загрузки расписания/store). */
class PredictorAuthGateViewModel (
    authRepository: IAuthRepository,
) : ViewModel() {
    val canUsePredictor: StateFlow<Boolean> = authRepository.userChanges
        .map { authRepository.canUsePredictor }
        .stateIn(viewModelScope, SharingStarted.Eagerly, authRepository.canUsePredictor)

    val isSignedIn: StateFlow<Boolean> = authRepository.userChanges
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, authRepository.isSignedIn)
}
