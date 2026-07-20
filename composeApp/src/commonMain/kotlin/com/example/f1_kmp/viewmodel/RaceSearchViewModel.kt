package com.example.f1_kmp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.f1_kmp.data.model.RaceModel
import com.example.f1_kmp.data.repository.F1Repository
import com.example.f1_kmp.domain.AppException
import com.example.f1_kmp.domain.ErrorStrings
import com.example.f1_kmp.domain.AsyncValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel экрана «Поиск гонки».
 *
 * Пользователь выбирает сезон и гонку из picker; по кнопке «Найти» — запрос без кэша.
 */
class RaceSearchViewModel(
    private val repository: F1Repository,
) : ViewModel() {
    private val loadJob = LoadJobHolder()

    private val _year = MutableStateFlow("")
    val year: StateFlow<String> = _year.asStateFlow()

    private val _round = MutableStateFlow("")
    val round: StateFlow<String> = _round.asStateFlow()

    private val _raceDisplay = MutableStateFlow("")
    val raceDisplay: StateFlow<String> = _raceDisplay.asStateFlow()

    private val _searchedRace = MutableStateFlow<AsyncValue<RaceModel?>>(AsyncValue.Value(null))
    val searchedRace: StateFlow<AsyncValue<RaceModel?>> = _searchedRace.asStateFlow()

    private val _fieldsInputted = MutableStateFlow(false)
    val fieldsInputted: StateFlow<Boolean> = _fieldsInputted.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    private val _dataLoaded = MutableStateFlow(true)
    val dataLoaded: StateFlow<Boolean> = _dataLoaded.asStateFlow()

    fun onYearChanged(value: String) {
        _year.value = value
        _round.value = ""
        _raceDisplay.value = ""
        checkFields()
    }

    fun onRacePicked(round: String, display: String) {
        _round.value = round
        _raceDisplay.value = display
        checkFields()
    }

    fun checkFields() {
        _fieldsInputted.value = _year.value.length == 4 && _round.value.isNotEmpty()
    }

    suspend fun loadSeasonYears(): Result<List<String>> = repository.getSeasonYears()

    suspend fun loadSeasonRaces(year: String): Result<List<RaceModel>> = repository.getSeasonRaces(year)

    fun loadRaceResults() {
        loadJob.launch(viewModelScope) {
            _dataLoaded.value = false
            _errorMessage.value = ""
            _searchedRace.value = AsyncValue.Loading
            repository.getRaceResults(_year.value, _round.value)
                .onSuccess { race ->
                    if (race != null) {
                        _searchedRace.value = AsyncValue.Value(race)
                    } else {
                        _searchedRace.value = AsyncValue.Value(null)
                        _errorMessage.value = ErrorStrings.raceNotFound
                    }
                }
                .onFailure { e ->
                    val ex = e as AppException
                    _searchedRace.value = AsyncValue.Error(ex.title, ex.subtitle)
                    _errorMessage.value = ex.title
                }
            _dataLoaded.value = true
        }
    }
}
