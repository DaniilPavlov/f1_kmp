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

    private val _circuits = MutableStateFlow<AsyncValue<List<Circuit>>>(AsyncValue.Loading)
    val circuits: StateFlow<AsyncValue<List<Circuit>>> = _circuits.asStateFlow()

    private val _activePage = MutableStateFlow(0)
    val activePage: StateFlow<Int> = _activePage.asStateFlow()

    init {
        loadCircuits()
    }

    /** Переключение «На карте / Списком» на экране трасс. */
    fun changeActivePage(index: Int) {
        _activePage.value = index
    }

    /** Peek → сеть. Ошибку сети не показываем, если на экране уже есть кэш. */
    fun loadCircuits() {
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

    private suspend fun loadInternal(clearCaches: Boolean) {
        if (clearCaches) {
            appDataRefresh.clearAll()
            _circuits.value = AsyncValue.Loading
        } else {
            repository.peekCircuitsCache()?.let { _circuits.value = AsyncValue.Value(it) }
                ?: run { _circuits.value = AsyncValue.Loading }
        }

        repository.getCircuits().applyUnlessCached(
            current = _circuits.value,
            onSuccess = { _circuits.value = AsyncValue.Value(it) },
            onFailure = { ex -> _circuits.value = AsyncValue.Error(ex.title, ex.subtitle) },
        )
    }
}
