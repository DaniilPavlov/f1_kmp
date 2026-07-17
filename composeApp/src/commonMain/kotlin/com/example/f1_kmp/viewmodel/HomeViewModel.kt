package com.example.f1_kmp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.f1_kmp.data.model.ConstructorStandingsModel
import com.example.f1_kmp.data.model.DriverStandingsModel
import com.example.f1_kmp.data.repository.F1Repository
import com.example.f1_kmp.domain.AppException
import com.example.f1_kmp.domain.AsyncValue
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel вкладки «Главная».
 *
 * Показывает текущие чемпионаты пилотов и конструкторов.
 * Поток данных: сначала [F1Repository.peekCurrentDriversCache] / peek конструкторов
 * (мгновенный UI офлайн), потом параллельный refresh с сети.
 * Повторный вызов [loadAllData] отменяет предыдущий Job через [LoadJobHolder].
 *
 * [activeTable] — какая таблица открыта (0 = пилоты, 1 = конструкторы).
 * [season]/[round] — метаданные из кэша/сети для подписи «сезон · этап».
 */
class HomeViewModel(
    private val repository: F1Repository,
) : ViewModel() {
    private val loadJob = LoadJobHolder()

    private val _drivers = MutableStateFlow<AsyncValue<List<DriverStandingsModel>>>(AsyncValue.Loading)
    val drivers: StateFlow<AsyncValue<List<DriverStandingsModel>>> = _drivers.asStateFlow()

    private val _constructors = MutableStateFlow<AsyncValue<List<ConstructorStandingsModel>>>(AsyncValue.Loading)
    val constructors: StateFlow<AsyncValue<List<ConstructorStandingsModel>>> = _constructors.asStateFlow()

    private val _season = MutableStateFlow("")
    val season: StateFlow<String> = _season.asStateFlow()

    private val _round = MutableStateFlow("")
    val round: StateFlow<String> = _round.asStateFlow()

    private val _activeTable = MutableStateFlow(0)
    val activeTable: StateFlow<Int> = _activeTable.asStateFlow()

    private val _error = MutableStateFlow<AppException?>(null)
    val error: StateFlow<AppException?> = _error.asStateFlow()

    init {
        loadAllData()
    }

    /** Переключение SegmentedControl: пилоты / конструкторы. */
    fun changeActiveTable(index: Int) {
        _activeTable.value = index
    }

    /** Peek → сеть. Ошибку сети не показываем, если на экране уже есть кэш. */
    fun loadAllData() {
        loadJob.launch(viewModelScope) {
            _error.value = null

            repository.peekCurrentDriversCache()?.let { (list, meta) ->
                _drivers.value = AsyncValue.Value(list)
                _season.value = meta.season
                _round.value = meta.round
            } ?: run { _drivers.value = AsyncValue.Loading }

            repository.peekCurrentConstructorsCache()?.let {
                _constructors.value = AsyncValue.Value(it)
            } ?: run { _constructors.value = AsyncValue.Loading }

            val driversDeferred = async { repository.getCurrentDriverStandings() }
            val constructorsDeferred = async { repository.getCurrentConstructorStandings() }

            driversDeferred.await().applyUnlessCached(
                current = _drivers.value,
                onSuccess = { (list, meta) ->
                    _drivers.value = AsyncValue.Value(list)
                    _season.value = meta.season
                    _round.value = meta.round
                },
                onFailure = { ex ->
                    _drivers.value = AsyncValue.Error(ex.title, ex.subtitle)
                    _error.value = ex
                },
            )

            constructorsDeferred.await().applyUnlessCached(
                current = _constructors.value,
                onSuccess = { _constructors.value = AsyncValue.Value(it) },
                onFailure = { ex ->
                    _constructors.value = AsyncValue.Error(ex.title, ex.subtitle)
                    _error.value = ex
                },
            )
        }
    }
}
