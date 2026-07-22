package com.example.f1_kmp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.f1_kmp.data.model.FinishStatusItem
import com.example.f1_kmp.data.repository.F1Repository
import com.example.f1_kmp.domain.AppException
import com.example.f1_kmp.domain.AsyncValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** ViewModel экрана «Статусы финиша» за сезон. */
class FinishStatusViewModel(
    private val repository: F1Repository,
) : ViewModel() {
    private val loadJob = LoadJobHolder()

    private val _year = MutableStateFlow("")
    val year: StateFlow<String> = _year.asStateFlow()

    private val _statuses = MutableStateFlow<AsyncValue<List<FinishStatusItem>>>(AsyncValue.Loading)
    val statuses: StateFlow<AsyncValue<List<FinishStatusItem>>> = _statuses.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getSeasonYears().onSuccess { years ->
                if (_year.value.isEmpty() && years.isNotEmpty()) {
                    _year.value = years.first()
                    loadAllData()
                }
            }
        }
    }

    fun onYearChanged(value: String) {
        _year.value = value
        if (value.length == 4) loadAllData()
    }

    suspend fun loadSeasonYears(): Result<List<String>> = repository.getSeasonYears()

    fun loadAllData() {
        if (_year.value.length != 4) return
        loadJob.launch(viewModelScope) {
            _statuses.value = AsyncValue.Loading
            repository.getSeasonFinishStatuses(_year.value).fold(
                onSuccess = { _statuses.value = AsyncValue.Value(it) },
                onFailure = { e ->
                    val ex = e as AppException
                    _statuses.value = AsyncValue.Error(ex.title, ex.subtitle)
                },
            )
        }
    }
}
