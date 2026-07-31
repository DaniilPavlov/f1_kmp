package com.example.f1_kmp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.f1_kmp.domain.model.Driver
import com.example.f1_kmp.data.repository.IF1Repository
import com.example.f1_kmp.domain.toAppError
import com.example.f1_kmp.domain.AsyncValue
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** ViewModel экрана сравнения пилотов head-to-head. */
class H2hDriversViewModel(
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

    private val _driverA = MutableStateFlow<Driver?>(null)
    val driverA: StateFlow<Driver?> = _driverA.asStateFlow()

    private val _driverB = MutableStateFlow<Driver?>(null)
    val driverB: StateFlow<Driver?> = _driverB.asStateFlow()

    private val _comparison = MutableStateFlow<AsyncValue<H2hDriverCompareResult?>>(AsyncValue.Value(null))
    val comparison: StateFlow<AsyncValue<H2hDriverCompareResult?>> = _comparison.asStateFlow()

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
            val a = _driverA.value
            val b = _driverB.value
            return a != null && b != null && a.driverId != b.driverId &&
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
        _driverA.value = null
        _driverB.value = null
        resetComparison()
    }

    fun onSeasonPicked(year: String) {
        _pickedSeason.value = year
        resetComparison()
    }

    fun setDriverA(driver: Driver) {
        _driverA.value = driver
        resetComparison()
    }

    fun setDriverB(driver: Driver) {
        _driverB.value = driver
        resetComparison()
    }

    suspend fun loadSeasonYears(): Result<List<String>> = repository.getSeasonYears()

    suspend fun loadDriversForPicker(): Result<List<Driver>> =
        if (_currentOnly.value) repository.getCurrentDrivers() else repository.getAllDrivers()

    fun compare() {
        if (!canCompare) return
        val a = _driverA.value ?: return
        val b = _driverB.value ?: return
        val season = selectedSeason
        loadJob.launch(viewModelScope) {
            _comparison.value = AsyncValue.Loading
            coroutineScope {
                val statsADeferred = async { repository.getDriverH2hStats(a.driverId, season) }
                val statsBDeferred = async { repository.getDriverH2hStats(b.driverId, season) }
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
                val scoresA = repository.getDriverH2hRoundScores(a.driverId, season).getOrElse { emptyList() }
                val scoresB = repository.getDriverH2hRoundScores(b.driverId, season).getOrElse { emptyList() }
                val timeline = H2hPointsTimeline.fromScores(scoresA, scoresB, season)
                _comparison.value = AsyncValue.Value(
                    H2hDriverCompareResult(
                        a,
                        b,
                        statsA.getOrThrow(),
                        statsB.getOrThrow(),
                        season,
                        timeline,
                    ),
                )
            }
        }
    }

    private fun resetComparison() {
        _comparison.value = AsyncValue.Value(null)
    }
}
