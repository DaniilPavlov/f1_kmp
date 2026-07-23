package com.example.f1_kmp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.f1_kmp.domain.model.Constructor
import com.example.f1_kmp.data.repository.IF1Repository
import com.example.f1_kmp.domain.toAppError
import com.example.f1_kmp.domain.AsyncValue
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** ViewModel экрана сравнения конструкторов head-to-head. */
class H2hConstructorsViewModel(
    private val repository: IF1Repository,
) : ViewModel() {
    private val loadJob = LoadJobHolder()

    private val _scopeMode = MutableStateFlow(0)
    val scopeMode: StateFlow<Int> = _scopeMode.asStateFlow()

    private val _useCurrentSeason = MutableStateFlow(true)
    val useCurrentSeason: StateFlow<Boolean> = _useCurrentSeason.asStateFlow()

    private val _currentOnly = MutableStateFlow(true)
    val currentOnly: StateFlow<Boolean> = _currentOnly.asStateFlow()

    private val _latestSeason = MutableStateFlow("")
    val latestSeason: StateFlow<String> = _latestSeason.asStateFlow()

    private val _pickedSeason = MutableStateFlow("")
    val pickedSeason: StateFlow<String> = _pickedSeason.asStateFlow()

    private val _constructorA = MutableStateFlow<Constructor?>(null)
    val constructorA: StateFlow<Constructor?> = _constructorA.asStateFlow()

    private val _constructorB = MutableStateFlow<Constructor?>(null)
    val constructorB: StateFlow<Constructor?> = _constructorB.asStateFlow()

    private val _comparison =
        MutableStateFlow<AsyncValue<H2hConstructorCompareResult?>>(AsyncValue.Value(null))
    val comparison: StateFlow<AsyncValue<H2hConstructorCompareResult?>> = _comparison.asStateFlow()

    val isSeasonScope: Boolean get() = _scopeMode.value == 1
    val showYearPicker: Boolean get() = isSeasonScope && !_useCurrentSeason.value

    val selectedSeason: String?
        get() {
            if (!isSeasonScope) return null
            return if (_useCurrentSeason.value) {
                _latestSeason.value.takeIf { it.isNotEmpty() }
            } else {
                _pickedSeason.value.takeIf { it.length == 4 }
            }
        }

    val canCompare: Boolean
        get() {
            val a = _constructorA.value
            val b = _constructorB.value
            return a != null && b != null && a.constructorId != b.constructorId &&
                (!isSeasonScope || selectedSeason != null)
        }

    init {
        viewModelScope.launch {
            repository.getSeasonYears().onSuccess { years ->
                if (years.isNotEmpty()) {
                    _latestSeason.value = years.first()
                    _pickedSeason.value = years.first()
                }
            }
        }
    }

    fun setScopeMode(mode: Int) {
        if (_scopeMode.value == mode) return
        _scopeMode.value = mode
        resetComparison()
    }

    fun setUseCurrentSeason(value: Boolean) {
        if (_useCurrentSeason.value == value) return
        _useCurrentSeason.value = value
        resetComparison()
    }

    fun setCurrentOnly(value: Boolean) {
        if (_currentOnly.value == value) return
        _currentOnly.value = value
        _constructorA.value = null
        _constructorB.value = null
        resetComparison()
    }

    fun onSeasonPicked(year: String) {
        _pickedSeason.value = year
        resetComparison()
    }

    fun setConstructorA(constructor: Constructor) {
        _constructorA.value = constructor
        resetComparison()
    }

    fun setConstructorB(constructor: Constructor) {
        _constructorB.value = constructor
        resetComparison()
    }

    suspend fun loadSeasonYears(): Result<List<String>> = repository.getSeasonYears()

    suspend fun loadConstructorsForPicker(): Result<List<Constructor>> =
        if (_currentOnly.value) {
            repository.getCurrentConstructorsList()
        } else {
            repository.getAllConstructors()
        }

    fun compare() {
        if (!canCompare) return
        val a = _constructorA.value ?: return
        val b = _constructorB.value ?: return
        val season = selectedSeason
        loadJob.launch(viewModelScope) {
            _comparison.value = AsyncValue.Loading
            coroutineScope {
                val statsADeferred = async { repository.getConstructorH2hStats(a.constructorId, season) }
                val statsBDeferred = async { repository.getConstructorH2hStats(b.constructorId, season) }
                val statsA = statsADeferred.await()
                val statsB = statsBDeferred.await()
                val leftEx = statsA.exceptionOrNull()?.toAppError()?.asException()
                if (leftEx != null) {
                    _comparison.value = AsyncValue.Error(leftEx.title, leftEx.subtitle)
                    return@coroutineScope
                }
                val rightEx = statsB.exceptionOrNull()?.toAppError()?.asException()
                if (rightEx != null) {
                    _comparison.value = AsyncValue.Error(rightEx.title, rightEx.subtitle)
                    return@coroutineScope
                }
                _comparison.value = AsyncValue.Value(
                    H2hConstructorCompareResult(a, b, statsA.getOrThrow(), statsB.getOrThrow(), season),
                )
            }
        }
    }

    private fun resetComparison() {
        _comparison.value = AsyncValue.Value(null)
    }
}
