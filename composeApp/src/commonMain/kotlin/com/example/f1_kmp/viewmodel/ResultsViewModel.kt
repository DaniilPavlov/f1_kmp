package com.example.f1_kmp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.f1_kmp.data.api.EspnApiService
import com.example.f1_kmp.data.model.EspnScoreboardEvent
import com.example.f1_kmp.data.model.RaceModel
import com.example.f1_kmp.data.repository.EspnRepository
import com.example.f1_kmp.data.repository.F1Repository
import com.example.f1_kmp.domain.AsyncValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ViewModel «Результаты» — последняя гонка + ESPN weekend scoreboard.
 *
 * Сначала [F1Repository.peekLastRaceCache] (если файл кэша есть), затем [getLastRace].
 * [applyUnlessCached] не затирает уже показанные данные ошибкой сети.
 * Scoreboard: ошибка ESPN не ломает экран — секция скрывается.
 */
class ResultsViewModel(
    private val repository: F1Repository,
    private val espnRepository: EspnRepository,
) : ViewModel() {
    private val loadJob = LoadJobHolder()
    private var pollJob: Job? = null

    private val _lastRace = MutableStateFlow<AsyncValue<RaceModel>>(AsyncValue.Loading)
    val lastRace: StateFlow<AsyncValue<RaceModel>> = _lastRace.asStateFlow()

    private val _scoreboard = MutableStateFlow<AsyncValue<EspnScoreboardEvent?>>(AsyncValue.Loading)
    val scoreboard: StateFlow<AsyncValue<EspnScoreboardEvent?>> = _scoreboard.asStateFlow()

    init {
        loadAllData()
    }

    /** Повторный вызов — pull-to-refresh / retry с экрана ошибки. */
    fun loadAllData() {
        loadJob.launch(viewModelScope) {
            val raceDeferred = async { loadLastRaceInternal() }
            val scoreboardDeferred = async { loadScoreboardInternal(forceRefresh = false) }
            raceDeferred.await()
            scoreboardDeferred.await()
        }
    }

    private suspend fun loadLastRaceInternal() {
        repository.peekLastRaceCache()?.let { _lastRace.value = AsyncValue.Value(it) }
            ?: run { _lastRace.value = AsyncValue.Loading }

        repository.getLastRace().applyUnlessCached(
            current = _lastRace.value,
            onSuccess = { _lastRace.value = AsyncValue.Value(it) },
            onFailure = { ex -> _lastRace.value = AsyncValue.Error(ex.title, ex.subtitle) },
        )
    }

    /** ESPN scoreboard: cache first; network failure never breaks Results (hide silently). */
    private suspend fun loadScoreboardInternal(forceRefresh: Boolean) {
        if (!forceRefresh) {
            if (espnRepository.isScoreboardFresh) {
                _scoreboard.value = AsyncValue.Value(espnRepository.peekScoreboard)
                syncLivePolling()
                return
            }
            espnRepository.peekScoreboard?.let {
                _scoreboard.value = AsyncValue.Value(it)
            } ?: run {
                if (_scoreboard.value !is AsyncValue.Value) {
                    _scoreboard.value = AsyncValue.Loading
                }
            }
        } else if (_scoreboard.value !is AsyncValue.Value) {
            _scoreboard.value = AsyncValue.Loading
        }

        espnRepository.getScoreboardEvent(forceRefresh = forceRefresh).fold(
            onSuccess = { event -> _scoreboard.value = AsyncValue.Value(event) },
            onFailure = {
                if (_scoreboard.value !is AsyncValue.Value) {
                    _scoreboard.value = AsyncValue.Value(null)
                }
            },
        )
        syncLivePolling()
    }

    private fun syncLivePolling() {
        val live = _scoreboard.value.getOrNull()?.isLive == true
        if (live) startLivePolling() else stopLivePolling()
    }

    private fun startLivePolling() {
        if (pollJob?.isActive == true) return
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(EspnApiService.SCOREBOARD_POLL_INTERVAL_MS)
                if (_scoreboard.value.getOrNull()?.isLive != true) {
                    stopLivePolling()
                    return@launch
                }
                loadScoreboardInternal(forceRefresh = true)
            }
        }
    }

    private fun stopLivePolling() {
        pollJob?.cancel()
        pollJob = null
    }

    override fun onCleared() {
        stopLivePolling()
        super.onCleared()
    }
}
