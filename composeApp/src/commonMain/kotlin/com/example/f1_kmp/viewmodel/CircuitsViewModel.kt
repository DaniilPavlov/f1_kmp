package com.example.f1_kmp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.f1_kmp.domain.model.Circuit
import com.example.f1_kmp.data.repository.IF1Repository
import com.example.f1_kmp.domain.AppDataRefresh
import com.example.f1_kmp.domain.AsyncValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CircuitsUiState(
    val circuits: AsyncValue<List<Circuit>> = AsyncValue.Loading,
    val activePage: Int = 0,
    val isRefreshing: Boolean = false,
)

/**
 * ViewModel вкладки «Трассы».
 *
 * Список всех трасс F1: peek-файл кэша → сеть.
 * [activePage] — 0 = карта, 1 = список (Android OSMDroid / iOS MapKit).
 */
class CircuitsViewModel(
    private val repository: IF1Repository,
    private val appDataRefresh: AppDataRefresh,
) : ViewModel() {
    private val loadJob = LoadJobHolder()

    private val _uiState = MutableStateFlow(CircuitsUiState())
    val uiState: StateFlow<CircuitsUiState> = _uiState.asStateFlow()

    init {
        loadCircuits()
    }

    /** Переключение «На карте / Списком» на экране трасс. */
    fun changeActivePage(index: Int) {
        _uiState.update { it.copy(activePage = index) }
    }

    /** Peek → сеть. Ошибку сети не показываем, если на экране уже есть кэш. */
    fun loadCircuits() {
        loadJob.launch(viewModelScope) {
            loadInternal(clearCaches = false)
        }
    }

    /** ErrorBody / pull-to-refresh: сброс кэшей, затем сеть. */
    fun refreshAll() {
        loadJob.launch(viewModelScope) {
            loadInternal(clearCaches = true)
        }
    }

    private suspend fun loadInternal(clearCaches: Boolean) {
        try {
            if (clearCaches) {
                appDataRefresh.clearAll()
                _uiState.update {
                    it.copy(
                        isRefreshing = true,
                        circuits = if (it.circuits is AsyncValue.Value) it.circuits else AsyncValue.Loading,
                    )
                }
            } else {
                repository.peekCircuitsCache()?.let { cached ->
                    _uiState.update { it.copy(circuits = AsyncValue.Value(cached)) }
                } ?: run {
                    _uiState.update { it.copy(circuits = AsyncValue.Loading) }
                }
            }

            repository.getCircuits().applyUnlessCached(
                current = _uiState.value.circuits,
                onSuccess = { list ->
                    _uiState.update { it.copy(circuits = AsyncValue.Value(list)) }
                },
                onFailure = { err ->
                    _uiState.update { it.copy(circuits = err.toAsyncError()) }
                },
            )
        } finally {
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }
}
