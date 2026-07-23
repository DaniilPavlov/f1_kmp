package com.example.f1_kmp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.f1_kmp.data.circuits.CircuitStats
import com.example.f1_kmp.data.circuits.CircuitStatsRepository
import com.example.f1_kmp.domain.model.Circuit
import com.example.f1_kmp.data.model.CircuitRaceWin
import com.example.f1_kmp.data.repository.IF1Repository
import com.example.f1_kmp.domain.AppError
import com.example.f1_kmp.domain.ErrorStrings
import com.example.f1_kmp.domain.AsyncValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel карточки трассы.
 *
 * [circuitId] — аргумент маршрута `circuit_detail/{circuitId}`.
 * Сначала ищем трассу в peek-списке (если пользователь уже открывал вкладку «Трассы»),
 * затем [IF1Repository.getCircuitById] на случай, если кэша ещё нет.
 */
class CircuitDetailViewModel(
    private val circuitId: String,
    private val repository: IF1Repository,
    private val circuitStatsRepository: CircuitStatsRepository,
) : ViewModel() {
    private val loadJob = LoadJobHolder()

    private val _circuit = MutableStateFlow<AsyncValue<Circuit>>(AsyncValue.Loading)
    val circuit: StateFlow<AsyncValue<Circuit>> = _circuit.asStateFlow()

    private val _winners = MutableStateFlow<AsyncValue<List<CircuitRaceWin>>>(AsyncValue.Loading)
    val winners: StateFlow<AsyncValue<List<CircuitRaceWin>>> = _winners.asStateFlow()

    private val _stats = MutableStateFlow<CircuitStats?>(null)
    val stats: StateFlow<CircuitStats?> = _stats.asStateFlow()

    private val _error = MutableStateFlow<AppError?>(null)
    val error: StateFlow<AppError?> = _error.asStateFlow()

    init {
        loadAllData()
    }

    /** Повторный вызов — retry с экрана ошибки. */
    fun loadAllData() {
        loadJob.launch(viewModelScope) {
            _error.value = null
            loadCircuit()
            loadStats()
            loadWinners()
        }
    }

    private suspend fun loadStats() {
        _stats.value = runCatching { circuitStatsRepository.of(circuitId) }.getOrNull()
    }

    private suspend fun loadCircuit() {
        repository.peekCircuitsCache()?.find { it.circuitId == circuitId }?.let {
            _circuit.value = AsyncValue.Value(it)
        } ?: run { _circuit.value = AsyncValue.Loading }

        repository.getCircuitById(circuitId).applyUnlessCached(
            current = _circuit.value,
            onSuccess = { found ->
                if (found != null) {
                    _circuit.value = AsyncValue.Value(found)
                } else {
                    _circuit.value = AsyncValue.Error(ErrorStrings.circuitNotFound)
                }
            },
            onFailure = { ex -> _circuit.value = AsyncValue.Error(ex.title, ex.subtitle) },
        )
    }

    private suspend fun loadWinners() {
        _winners.value = AsyncValue.Loading
        repository.getCircuitWinners(circuitId).applyUnlessCached(
            current = _winners.value,
            onSuccess = { _winners.value = AsyncValue.Value(it) },
            onFailure = { ex ->
                _winners.value = AsyncValue.Error(ex.title, ex.subtitle)
                _error.value = ex
            },
        )
    }
}
