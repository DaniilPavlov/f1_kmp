package com.example.f1_kmp.domain.live

import com.example.f1_kmp.data.model.EspnScoreboardEvent
import com.example.f1_kmp.data.repository.IEspnRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * App-scoped ESPN scoreboard + 30s poll while a session is live and app is foregrounded.
 */
class LiveWeekendController(
    private val espnRepository: IEspnRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pollJob: Job? = null
    private var appInForeground = true

    private val _scoreboard = MutableStateFlow<EspnScoreboardEvent?>(null)
    val scoreboard: StateFlow<EspnScoreboardEvent?> = _scoreboard.asStateFlow()

    val isLive: Boolean
        get() = _scoreboard.value?.isLive == true

    val liveSessionAbbreviation: String?
        get() {
            val event = _scoreboard.value ?: return null
            if (!event.isLive) return null
            return event.highlightedSession?.abbreviation?.takeIf { it.isNotBlank() }
        }

    fun loadScoreboard(forceRefresh: Boolean = false) {
        scope.launch {
            espnRepository.getScoreboardEvent(forceRefresh = forceRefresh)
                .onSuccess { event ->
                    _scoreboard.value = event
                    syncLivePolling()
                }
                .onFailure {
                    if (_scoreboard.value == null) {
                        _scoreboard.value = null
                    }
                    syncLivePolling()
                }
        }
    }

    fun onAppForeground() {
        appInForeground = true
        loadScoreboard(forceRefresh = true)
    }

    fun onAppBackground() {
        appInForeground = false
        stopLivePolling()
    }

    private fun syncLivePolling() {
        if (isLive && appInForeground) {
            startLivePolling()
        } else {
            stopLivePolling()
        }
    }

    private fun startLivePolling() {
        if (pollJob != null) return
        pollJob = scope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                if (!isLive || !appInForeground) {
                    stopLivePolling()
                    return@launch
                }
                espnRepository.getScoreboardEvent(forceRefresh = true)
                    .onSuccess { _scoreboard.value = it }
            }
        }
    }

    fun stopLivePolling() {
        pollJob?.cancel()
        pollJob = null
    }

    private companion object {
        const val POLL_INTERVAL_MS = 30_000L
    }
}
