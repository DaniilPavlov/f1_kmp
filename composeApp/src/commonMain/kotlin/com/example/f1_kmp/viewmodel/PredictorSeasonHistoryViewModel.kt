package com.example.f1_kmp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.f1_kmp.data.repository.IPredictorRepository
import com.example.f1_kmp.domain.AppError
import com.example.f1_kmp.domain.predictor.PredictorSeason
import com.example.f1_kmp.domain.toAppError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** История предиктов за выбранный сезон. */
data class PredictorSeasonHistoryUiState(
    val isLoading: Boolean = true,
    val error: AppError? = null,
    val season: PredictorSeason? = null,
)

/** История одного сезона предиктора из Firestore store. */
class PredictorSeasonHistoryViewModel(
    val year: String,
    private val predictorRepository: IPredictorRepository,
) : ViewModel() {

    private val loadJob = LoadJobHolder()
    private val _uiState = MutableStateFlow(PredictorSeasonHistoryUiState())
    val uiState: StateFlow<PredictorSeasonHistoryUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        loadJob.launch(viewModelScope) {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val store = predictorRepository.load()
                val season = store.season(year) ?: PredictorSeason(year = year)
                _uiState.update { it.copy(isLoading = false, season = season, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.toAppError()) }
            }
        }
    }
}
