package com.example.f1_kmp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.f1_kmp.data.model.CircuitModel
import com.example.f1_kmp.data.repository.F1Repository
import com.example.f1_kmp.domain.AsyncValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel вкладки «Трассы».
 *
 * Список всех трасс F1: peek-файл кэша → сеть.
 * [activePage] — 0 = карта, 1 = список (на iOS карта — заглушка).
 */
class CircuitsViewModel(
    private val repository: F1Repository,
) : ViewModel() {
    private val loadJob = LoadJobHolder()

    private val _circuits = MutableStateFlow<AsyncValue<List<CircuitModel>>>(AsyncValue.Loading)
    val circuits: StateFlow<AsyncValue<List<CircuitModel>>> = _circuits.asStateFlow()

    private val _activePage = MutableStateFlow(0)
    val activePage: StateFlow<Int> = _activePage.asStateFlow()

    init {
        loadCircuits()
    }

    /** Переключение «На карте / Списком» на экране трасс. */
    fun changeActivePage(index: Int) {
        _activePage.value = index
    }

    fun loadCircuits() {
        loadJob.launch(viewModelScope) {
            repository.peekCircuitsCache()?.let { _circuits.value = AsyncValue.Value(it) }
                ?: run { _circuits.value = AsyncValue.Loading }

            repository.getCircuits().applyUnlessCached(
                current = _circuits.value,
                onSuccess = { _circuits.value = AsyncValue.Value(it) },
                onFailure = { ex -> _circuits.value = AsyncValue.Error(ex.title, ex.subtitle) },
            )
        }
    }
}
