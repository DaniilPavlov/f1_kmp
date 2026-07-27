package com.example.f1_kmp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.f1_kmp.domain.model.Race
import com.example.f1_kmp.data.repository.IF1Repository
import com.example.f1_kmp.domain.toAppError
import com.example.f1_kmp.domain.ErrorStrings
import com.example.f1_kmp.domain.AsyncValue
import com.example.f1_kmp.util.F1InputValidation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel экрана «Поиск гонки».
 *
 * Пользователь выбирает сезон и гонку из picker; по кнопке «Найти» — запрос без кэша.
 */
class RaceSearchViewModel(
    private val repository: IF1Repository,
) : ViewModel() {
    private val loadJob = LoadJobHolder()

    private val _year = MutableStateFlow("")
    val year: StateFlow<String> = _year.asStateFlow()

    private val _round = MutableStateFlow("")
    val round: StateFlow<String> = _round.asStateFlow()

    private val _raceDisplay = MutableStateFlow("")
    val raceDisplay: StateFlow<String> = _raceDisplay.asStateFlow()

    private val _searchedRace = MutableStateFlow<AsyncValue<Race?>>(AsyncValue.Value(null))
    val searchedRace: StateFlow<AsyncValue<Race?>> = _searchedRace.asStateFlow()

    private val _fieldsInputted = MutableStateFlow(false)
    val fieldsInputted: StateFlow<Boolean> = _fieldsInputted.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    private val _dataLoaded = MutableStateFlow(true)
    val dataLoaded: StateFlow<Boolean> = _dataLoaded.asStateFlow()

    /** Смена сезона; сбрасывает выбранную гонку. */
    fun onYearChanged(value: String) {
        _year.value = value
        _round.value = ""
        _raceDisplay.value = ""
        checkFields()
    }

    /** Выбор гонки из [RacePickerField]. */
    fun onRacePicked(round: String, display: String) {
        _round.value = round
        _raceDisplay.value = display
        checkFields()
    }

    /** Проверяет, что сезон и этап валидны — кнопка «Найти» активна. */
    fun checkFields() {
        _fieldsInputted.value =
            F1InputValidation.isValidYear(_year.value) && F1InputValidation.isValidRound(_round.value)
    }

    /** Список сезонов для [SeasonPickerField]. */
    suspend fun loadSeasonYears(): Result<List<String>> = repository.getSeasonYears()

    /** Список гонок сезона для [RacePickerField]. */
    suspend fun loadSeasonRaces(year: String): Result<List<Race>> = repository.getSeasonRaces(year)

    /** Запрос результатов выбранной гонки (без кэша). */
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
                    val ex = e.toAppError()
                    _searchedRace.value = AsyncValue.Error(ex.title, ex.subtitle)
                    _errorMessage.value = ex.title
                }
            _dataLoaded.value = true
        }
    }
}
