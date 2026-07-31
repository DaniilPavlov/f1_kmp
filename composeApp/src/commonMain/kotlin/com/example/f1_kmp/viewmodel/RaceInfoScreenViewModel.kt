package com.example.f1_kmp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.f1_kmp.domain.model.PitStop
import com.example.f1_kmp.domain.model.QualifyingResult
import com.example.f1_kmp.domain.model.Race
import com.example.f1_kmp.domain.model.RaceResult
import com.example.f1_kmp.data.repository.IF1Repository
import com.example.f1_kmp.domain.AppError
import com.example.f1_kmp.domain.toAppError
import com.example.f1_kmp.domain.ErrorStrings
import com.example.f1_kmp.domain.AsyncValue
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class RaceInfoUiState(
    val race: AsyncValue<Race> = AsyncValue.Loading,
    val qualifying: AsyncValue<List<QualifyingResult>> = AsyncValue.Loading,
    val pitStops: AsyncValue<List<PitStop>> = AsyncValue.Loading,
    val sprint: AsyncValue<List<RaceResult>> = AsyncValue.Loading,
    val error: AppError? = null,
    val isRefreshing: Boolean = false,
)

/**
 * ViewModel экрана деталей гонки.
 *
 * [season] и [round] приходят из NavHost ([com.example.f1_kmp.ui.navigation.RaceInfo]),
 * Koin создаёт VM с этими параметрами.
 *
 * Порядок загрузки:
 * 1) результаты гонки — без них экран бессмысленен;
 * 2) параллельно квалификация и пит-стопы (доп. секции; ошибка одной не обязана валить всё).
 * Кэша на этом экране нет: каждый вход — свежий запрос.
 */
class RaceInfoScreenViewModel(
    private val season: String,
    private val round: String,
    private val repository: IF1Repository,
) : ViewModel() {
    private val loadJob = LoadJobHolder()

    private val _uiState = MutableStateFlow(RaceInfoUiState())
    val uiState: StateFlow<RaceInfoUiState> = _uiState.asStateFlow()

    init {
        loadAllData()
    }

    /** Повторный вызов — retry с экрана ошибки. */
    fun loadAllData() {
        loadJob.launch(viewModelScope) {
            loadInternal(softRefresh = false)
        }
    }

    /** Pull-to-refresh: keep Values while reloading. */
    fun refreshAll() {
        loadJob.launch(viewModelScope) {
            loadInternal(softRefresh = true)
        }
    }

    private suspend fun loadInternal(softRefresh: Boolean) {
        try {
            if (softRefresh) {
                _uiState.update {
                    it.copy(
                        isRefreshing = true,
                        error = null,
                        race = if (it.race is AsyncValue.Value) it.race else AsyncValue.Loading,
                        qualifying = if (it.qualifying is AsyncValue.Value) {
                            it.qualifying
                        } else {
                            AsyncValue.Loading
                        },
                        pitStops = if (it.pitStops is AsyncValue.Value) {
                            it.pitStops
                        } else {
                            AsyncValue.Loading
                        },
                        sprint = if (it.sprint is AsyncValue.Value) it.sprint else AsyncValue.Loading,
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        error = null,
                        race = AsyncValue.Loading,
                        qualifying = AsyncValue.Loading,
                        pitStops = AsyncValue.Loading,
                        sprint = AsyncValue.Loading,
                    )
                }
            }

            val raceResult = repository.getRaceResults(season, round)
            raceResult.onFailure { e ->
                val err = e.toAppError()
                if (_uiState.value.race !is AsyncValue.Value) {
                    _uiState.update {
                        it.copy(
                            race = err.toAsyncError(),
                            error = err,
                        )
                    }
                }
            }
            if (raceResult.isFailure) return

            val loadedRace = raceResult.getOrNull()
            if (loadedRace == null) {
                if (_uiState.value.race !is AsyncValue.Value) {
                    val err = AppError(ErrorStrings.raceNotFoundTitle)
                    _uiState.update {
                        it.copy(
                            race = err.toAsyncError(),
                            error = err,
                        )
                    }
                }
                return
            }

            _uiState.update { it.copy(race = AsyncValue.Value(loadedRace)) }
            loadExtraSections(loadedRace)
        } finally {
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private suspend fun loadExtraSections(race: Race) {
        coroutineScope {
            val qualifyingDeferred = async { repository.getQualifyingResults(race.season, race.round) }
            val pitStopsDeferred = async { repository.getPitStopsWithDriverNames(race.season, race.round) }
            val sprintDeferred = async { repository.getSprintResults(race.season, race.round) }

            qualifyingDeferred.await().applyUnlessCached(
                current = _uiState.value.qualifying,
                onSuccess = { _uiState.update { state -> state.copy(qualifying = AsyncValue.Value(it)) } },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            qualifying = err.toAsyncError(),
                            error = err,
                        )
                    }
                },
            )

            pitStopsDeferred.await().applyUnlessCached(
                current = _uiState.value.pitStops,
                onSuccess = { _uiState.update { state -> state.copy(pitStops = AsyncValue.Value(it)) } },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            pitStops = err.toAsyncError(),
                            error = err,
                        )
                    }
                },
            )

            sprintDeferred.await().applyUnlessCached(
                current = _uiState.value.sprint,
                onSuccess = { _uiState.update { state -> state.copy(sprint = AsyncValue.Value(it)) } },
                onFailure = {
                    // Нет спринта в уик-энде — не ошибка экрана, просто пустая секция.
                    _uiState.update { state -> state.copy(sprint = AsyncValue.Value(emptyList())) }
                },
            )
        }
    }
}
