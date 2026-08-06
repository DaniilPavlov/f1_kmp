package com.example.f1_kmp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.f1_kmp.data.repository.IAuthRepository
import com.example.f1_kmp.data.repository.IPredictorLeaderboardRepository
import com.example.f1_kmp.data.repository.IPredictorRepository
import com.example.f1_kmp.domain.NotificationsPreference
import com.example.f1_kmp.domain.auth.AuthUser
import com.example.f1_kmp.util.onLocaleChanged
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Состояние экрана профиля: auth + reminder toggles. */
data class ProfileUiState(
    val toastMessageKey: String? = null,
    val isBusy: Boolean = false,
)

/** Профиль: сессия Auth, верификация email, тогглы напоминаний + reschedule. */
class ProfileViewModel(
    private val authRepository: IAuthRepository,
    private val notificationsPreference: NotificationsPreference,
    private val predictorRepository: IPredictorRepository,
    private val leaderboardRepository: IPredictorLeaderboardRepository,
) : ViewModel() {
    val user: StateFlow<AuthUser?> = authRepository.userChanges
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), authRepository.currentUser)

    val raceRemindersEnabled = notificationsPreference.raceRemindersEnabled
    val practiceRemindersEnabled = notificationsPreference.practiceRemindersEnabled

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun clearToast() {
        _uiState.update { it.copy(toastMessageKey = null) }
    }

    /** Выход + сброс in-memory predictor/leaderboard кэшей. */
    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            predictorRepository.clearMemoryCache()
            leaderboardRepository.clearMemoryCache()
        }
    }

    fun resendVerification() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            try {
                val result = authRepository.sendEmailVerification()
                _uiState.update {
                    it.copy(
                        toastMessageKey = if (result.isSuccess) {
                            TOAST_VERIFICATION_SENT
                        } else {
                            result.errorKeyOrNull ?: TOAST_GENERIC
                        },
                    )
                }
            } finally {
                _uiState.update { it.copy(isBusy = false) }
            }
        }
    }

    /** После «я подтвердил»: reload токена и toast-ключ. */
    fun refreshVerification() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            try {
                val ok = authRepository.refreshEmailVerification()
                _uiState.update {
                    it.copy(
                        toastMessageKey = if (ok) TOAST_EMAIL_VERIFIED else TOAST_STILL_NOT_VERIFIED,
                    )
                }
            } finally {
                _uiState.update { it.copy(isBusy = false) }
            }
        }
    }

    fun setRaceRemindersEnabled(enabled: Boolean) {
        notificationsPreference.setRaceRemindersEnabled(enabled)
        onLocaleChanged()
    }

    fun setPracticeRemindersEnabled(enabled: Boolean) {
        notificationsPreference.setPracticeRemindersEnabled(enabled)
        onLocaleChanged()
    }

    fun onLocaleChanged() {
        com.example.f1_kmp.util.onLocaleChanged()
    }

    companion object {
        const val TOAST_VERIFICATION_SENT = "profileVerificationSent"
        const val TOAST_EMAIL_VERIFIED = "profileEmailVerified"
        const val TOAST_STILL_NOT_VERIFIED = "profileStillNotVerified"
        const val TOAST_GENERIC = "authErrorGeneric"
    }
}
