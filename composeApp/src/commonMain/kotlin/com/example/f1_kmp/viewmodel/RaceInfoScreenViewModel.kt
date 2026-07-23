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

    private val _race = MutableStateFlow<AsyncValue<Race>>(AsyncValue.Loading)
    val race: StateFlow<AsyncValue<Race>> = _race.asStateFlow()

    private val _qualifying = MutableStateFlow<AsyncValue<List<QualifyingResult>>>(AsyncValue.Loading)
    val qualifying: StateFlow<AsyncValue<List<QualifyingResult>>> = _qualifying.asStateFlow()

    private val _pitStops = MutableStateFlow<AsyncValue<List<PitStop>>>(AsyncValue.Loading)
    val pitStops: StateFlow<AsyncValue<List<PitStop>>> = _pitStops.asStateFlow()

    private val _sprint = MutableStateFlow<AsyncValue<List<RaceResult>>>(AsyncValue.Loading)
    val sprint: StateFlow<AsyncValue<List<RaceResult>>> = _sprint.asStateFlow()

    private val _error = MutableStateFlow<AppError?>(null)
    val error: StateFlow<AppError?> = _error.asStateFlow()

    init {
        loadAllData()
    }

    /** Повторный вызов — retry с экрана ошибки. */
    fun loadAllData() {
        loadJob.launch(viewModelScope) {
            _error.value = null
            _race.value = AsyncValue.Loading
            _qualifying.value = AsyncValue.Loading
            _pitStops.value = AsyncValue.Loading
            _sprint.value = AsyncValue.Loading

            val raceResult = repository.getRaceResults(season, round)
            raceResult.onFailure { e ->
                val ex = e.toAppError()
                _race.value = AsyncValue.Error(ex.title, ex.subtitle)
                _error.value = ex
            }
            if (raceResult.isFailure) return@launch

            val loadedRace = raceResult.getOrNull()
            if (loadedRace == null) {
                val ex = AppError(ErrorStrings.raceNotFoundTitle)
                _race.value = AsyncValue.Error(ex.title, ex.subtitle)
                _error.value = ex
                return@launch
            }

            _race.value = AsyncValue.Value(loadedRace)
            loadExtraSections(loadedRace)
        }
    }

    private suspend fun loadExtraSections(race: Race) {
        coroutineScope {
            val qualifyingDeferred = async { repository.getQualifyingResults(race.season, race.round) }
            val pitStopsDeferred = async { repository.getPitStopsWithDriverNames(race.season, race.round) }
            val sprintDeferred = async { repository.getSprintResults(race.season, race.round) }

            qualifyingDeferred.await().applyUnlessCached(
                current = _qualifying.value,
                onSuccess = { _qualifying.value = AsyncValue.Value(it) },
                onFailure = { ex ->
                    _qualifying.value = AsyncValue.Error(ex.title, ex.subtitle)
                    _error.value = ex
                },
            )

            pitStopsDeferred.await().applyUnlessCached(
                current = _pitStops.value,
                onSuccess = { _pitStops.value = AsyncValue.Value(it) },
                onFailure = { ex ->
                    _pitStops.value = AsyncValue.Error(ex.title, ex.subtitle)
                    _error.value = ex
                },
            )

            sprintDeferred.await().applyUnlessCached(
                current = _sprint.value,
                onSuccess = { _sprint.value = AsyncValue.Value(it) },
                onFailure = {
                    // Нет спринта в уик-энде — не ошибка экрана, просто пустая секция.
                    _sprint.value = AsyncValue.Value(emptyList())
                },
            )
        }
    }
}
