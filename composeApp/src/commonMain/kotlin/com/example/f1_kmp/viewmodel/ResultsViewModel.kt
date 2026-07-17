package com.example.f1_kmp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.f1_kmp.data.model.RaceModel
import com.example.f1_kmp.data.repository.F1Repository
import com.example.f1_kmp.domain.AsyncValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel вкладки «Результаты».
 *
 * Показывает последнюю завершённую гонку сезона.
 * Сначала [F1Repository.peekLastRaceCache] (если файл кэша есть), затем [getLastRace].
 * [applyUnlessCached] не затирает уже показанные данные ошибкой сети.
 */
class ResultsViewModel(
    private val repository: F1Repository,
) : ViewModel() {
    private val loadJob = LoadJobHolder()

    private val _lastRace = MutableStateFlow<AsyncValue<RaceModel>>(AsyncValue.Loading)
    val lastRace: StateFlow<AsyncValue<RaceModel>> = _lastRace.asStateFlow()

    init {
        loadAllData()
    }

    /** Повторный вызов — pull-to-refresh / retry с экрана ошибки. */
    fun loadAllData() {
        loadJob.launch(viewModelScope) {
            repository.peekLastRaceCache()?.let { _lastRace.value = AsyncValue.Value(it) }
                ?: run { _lastRace.value = AsyncValue.Loading }

            repository.getLastRace().applyUnlessCached(
                current = _lastRace.value,
                onSuccess = { _lastRace.value = AsyncValue.Value(it) },
                onFailure = { ex -> _lastRace.value = AsyncValue.Error(ex.title, ex.subtitle) },
            )
        }
    }
}
