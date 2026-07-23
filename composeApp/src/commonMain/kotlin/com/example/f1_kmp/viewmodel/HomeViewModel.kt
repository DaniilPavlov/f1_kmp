package com.example.f1_kmp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.f1_kmp.domain.model.ConstructorStanding
import com.example.f1_kmp.domain.model.DriverStanding
import com.example.f1_kmp.data.repository.IF1Repository
import com.example.f1_kmp.domain.AppDataRefresh
import com.example.f1_kmp.domain.AppError
import com.example.f1_kmp.domain.AsyncValue
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel вкладки «Главная».
 *
 * Показывает текущие чемпионаты пилотов и конструкторов.
 * Поток данных: сначала [IF1Repository.peekCurrentDriversCache] / peek конструкторов
 * (мгновенный UI офлайн), потом параллельный refresh с сети.
 * [refreshAll] чистит кэши через [AppDataRefresh] и грузит заново (ErrorBody retry).
 *
 * [activeTable] — какая таблица открыта (0 = пилоты, 1 = конструкторы).
 * [season]/[round] — метаданные из кэша/сети для подписи «сезон · этап».
 */
class HomeViewModel(
    private val repository: IF1Repository,
    private val appDataRefresh: AppDataRefresh,
) : ViewModel() {
    private val loadJob = LoadJobHolder()

    private val _drivers = MutableStateFlow<AsyncValue<List<DriverStanding>>>(AsyncValue.Loading)
    val drivers: StateFlow<AsyncValue<List<DriverStanding>>> = _drivers.asStateFlow()

    private val _constructors = MutableStateFlow<AsyncValue<List<ConstructorStanding>>>(AsyncValue.Loading)
    val constructors: StateFlow<AsyncValue<List<ConstructorStanding>>> = _constructors.asStateFlow()

    private val _season = MutableStateFlow("")
    val season: StateFlow<String> = _season.asStateFlow()

    private val _round = MutableStateFlow("")
    val round: StateFlow<String> = _round.asStateFlow()

    private val _activeTable = MutableStateFlow(0)
    val activeTable: StateFlow<Int> = _activeTable.asStateFlow()

    private val _error = MutableStateFlow<AppError?>(null)
    val error: StateFlow<AppError?> = _error.asStateFlow()

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
            loadInternal(clearCaches = false)
        }
    }

    /** ErrorBody / forced reload: сброс кэшей, затем сеть. */
    fun refreshAll() {
        loadJob.launch(viewModelScope) {
            loadInternal(clearCaches = true)
        }
    }

    private suspend fun loadInternal(clearCaches: Boolean) = coroutineScope {
        _error.value = null
        if (clearCaches) {
            appDataRefresh.clearAll()
            _drivers.value = AsyncValue.Loading
            _constructors.value = AsyncValue.Loading
        } else {
            repository.peekCurrentDriversCache()?.let { (list, meta) ->
                _drivers.value = AsyncValue.Value(list)
                _season.value = meta.season
                _round.value = meta.round
            } ?: run { _drivers.value = AsyncValue.Loading }

            repository.peekCurrentConstructorsCache()?.let {
                _constructors.value = AsyncValue.Value(it)
            } ?: run { _constructors.value = AsyncValue.Loading }
        }

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
