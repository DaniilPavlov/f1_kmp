package com.example.f1_kmp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.f1_kmp.data.api.EspnApiService
import com.example.f1_kmp.data.model.EspnScoreboardEvent
import com.example.f1_kmp.domain.model.Race
import com.example.f1_kmp.data.repository.IEspnRepository
import com.example.f1_kmp.data.repository.IF1Repository
import com.example.f1_kmp.domain.AppDataRefresh
import com.example.f1_kmp.domain.AsyncValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ViewModel «Результаты» — последняя гонка + ESPN weekend scoreboard.
 *
 * Сначала [IF1Repository.peekLastRaceCache] (если файл кэша есть), затем [getLastRace].
 * [applyUnlessCached] не затирает уже показанные данные ошибкой сети.
 * Scoreboard: ошибка ESPN не ломает экран — секция скрывается.
 */
class ResultsViewModel(
    private val repository: IF1Repository,
    private val espnRepository: IEspnRepository,
    private val appDataRefresh: AppDataRefresh,
) : ViewModel() {
    private val loadJob = LoadJobHolder()
    private var pollJob: Job? = null

    private val _lastRace = MutableStateFlow<AsyncValue<Race>>(AsyncValue.Loading)
    val lastRace: StateFlow<AsyncValue<Race>> = _lastRace.asStateFlow()

    private val _scoreboard = MutableStateFlow<AsyncValue<EspnScoreboardEvent?>>(AsyncValue.Loading)
    val scoreboard: StateFlow<AsyncValue<EspnScoreboardEvent?>> = _scoreboard.asStateFlow()

    init {
        loadAllData()
    }

    /** Первичная загрузка / soft retry: peek → сеть. */
    fun loadAllData() {
        loadJob.launch(viewModelScope) {
            loadInternal(clearCaches = false)
        }
    }

    /** ErrorBody: сброс кэшей, затем сеть. */
    fun refreshAll() {
        loadJob.launch(viewModelScope) {
            loadInternal(clearCaches = true)
        }
    }

    private suspend fun loadInternal(clearCaches: Boolean) = coroutineScope {
        if (clearCaches) {
            appDataRefresh.clearAll()
            _lastRace.value = AsyncValue.Loading
            _scoreboard.value = AsyncValue.Loading
        }
        val raceDeferred = async { loadLastRaceInternal(skipPeek = clearCaches) }
        val scoreboardDeferred = async { loadScoreboardInternal(forceRefresh = clearCaches) }
        raceDeferred.await()
        scoreboardDeferred.await()
    }

    private suspend fun loadLastRaceInternal(skipPeek: Boolean = false) {
        if (!skipPeek) {
            repository.peekLastRaceCache()?.let { _lastRace.value = AsyncValue.Value(it) }
                ?: run { _lastRace.value = AsyncValue.Loading }
        }

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
