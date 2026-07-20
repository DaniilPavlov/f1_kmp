package com.example.f1_kmp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.f1_kmp.data.model.CircuitModel
import com.example.f1_kmp.data.model.PitStopModel
import com.example.f1_kmp.data.model.QualifyingResultModel
import com.example.f1_kmp.data.model.RaceModel
import com.example.f1_kmp.data.model.RaceResultModel
import com.example.f1_kmp.data.repository.F1Repository
import com.example.f1_kmp.domain.AppException
import com.example.f1_kmp.domain.ErrorStrings
import com.example.f1_kmp.domain.AsyncValue
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel экрана деталей гонки.
 *
 * [season] и [round] приходят из NavHost (маршрут `race_info/{season}/{round}`),
 * Koin создаёт VM с этими параметрами.
 *
 * Порядок загрузки:
 * 1) результаты гонки — без них экран бессмысленен;
 * 2) параллельно квалификация и пит-стопы (доп. секции; ошибка одной не обязана валить всё).
 * Кэша на этом экране нет: каждый вход — свежий запрос.
 */
class RaceInfoScreenViewModel(
    private val season: String,
    private val round: String,
    private val repository: F1Repository,
) : ViewModel() {
    private val loadJob = LoadJobHolder()

    private val _race = MutableStateFlow<AsyncValue<RaceModel>>(AsyncValue.Loading)
    val race: StateFlow<AsyncValue<RaceModel>> = _race.asStateFlow()

    private val _qualifying = MutableStateFlow<AsyncValue<List<QualifyingResultModel>>>(AsyncValue.Loading)
    val qualifying: StateFlow<AsyncValue<List<QualifyingResultModel>>> = _qualifying.asStateFlow()

    private val _pitStops = MutableStateFlow<AsyncValue<List<PitStopModel>>>(AsyncValue.Loading)
    val pitStops: StateFlow<AsyncValue<List<PitStopModel>>> = _pitStops.asStateFlow()

    private val _sprint = MutableStateFlow<AsyncValue<List<RaceResultModel>>>(AsyncValue.Loading)
    val sprint: StateFlow<AsyncValue<List<RaceResultModel>>> = _sprint.asStateFlow()

    private val _allDataLoaded = MutableStateFlow(false)
    val allDataLoaded: StateFlow<Boolean> = _allDataLoaded.asStateFlow()

    private val _error = MutableStateFlow<AppException?>(null)
    val error: StateFlow<AppException?> = _error.asStateFlow()

    init {
        loadAllData()
    }

    /** Повторный вызов — retry с экрана ошибки. */
    fun loadAllData() {
        loadJob.launch(viewModelScope) {
            _error.value = null
            _allDataLoaded.value = false
            _race.value = AsyncValue.Loading
            _qualifying.value = AsyncValue.Loading
            _pitStops.value = AsyncValue.Loading
            _sprint.value = AsyncValue.Loading

            val raceResult = repository.getRaceResults(season, round)
            raceResult.onFailure { e ->
                val ex = e as AppException
                _race.value = AsyncValue.Error(ex.title, ex.subtitle)
                _error.value = ex
            }
            if (raceResult.isFailure) return@launch

            val loadedRace = raceResult.getOrNull()
            if (loadedRace == null) {
                val ex = AppException(ErrorStrings.raceNotFoundTitle)
                _race.value = AsyncValue.Error(ex.title, ex.subtitle)
                _error.value = ex
                return@launch
            }

            _race.value = AsyncValue.Value(loadedRace)
            _allDataLoaded.value = true
            loadExtraSections(loadedRace)
        }
    }

    private suspend fun loadExtraSections(race: RaceModel) {
        coroutineScope {
            val qualifyingDeferred = async { repository.getQualifyingResults(race.season, race.round) }
            val pitStopsDeferred = async { repository.getPitStopsWithDriverNames(race.season, race.round) }
            val sprintDeferred = async { repository.getSprintResults(race.season, race.round) }

            qualifyingDeferred.await().applyUnlessCached(
                current = _qualifying.value,
                onSuccess = { _qualifying.value = AsyncValue.Value(it) },
                onFailure = { ex ->
                    _qualifying.value = AsyncValue.Error(ex.title, ex.subtitle)
                    _error.value = ex
                },
            )

            pitStopsDeferred.await().applyUnlessCached(
                current = _pitStops.value,
                onSuccess = { _pitStops.value = AsyncValue.Value(it) },
                onFailure = { ex ->
                    _pitStops.value = AsyncValue.Error(ex.title, ex.subtitle)
                    _error.value = ex
                },
            )

            sprintDeferred.await().applyUnlessCached(
                current = _sprint.value,
                onSuccess = { _sprint.value = AsyncValue.Value(it) },
                onFailure = {
                    // Нет спринта в уик-энде — не ошибка экрана, просто пустая секция.
                    _sprint.value = AsyncValue.Value(emptyList())
                },
            )
        }
    }
}

/**
 * ViewModel карточки трассы.
 *
 * [circuitId] — аргумент маршрута `circuit_detail/{circuitId}`.
 * Сначала ищем трассу в peek-списке (если пользователь уже открывал вкладку «Трассы»),
 * затем [F1Repository.getCircuitById] на случай, если кэша ещё нет.
 */
class CircuitDetailViewModel(
    private val circuitId: String,
    private val repository: F1Repository,
) : ViewModel() {
    private val loadJob = LoadJobHolder()

    private val _circuit = MutableStateFlow<AsyncValue<CircuitModel>>(AsyncValue.Loading)
    val circuit: StateFlow<AsyncValue<CircuitModel>> = _circuit.asStateFlow()

    private val _winners = MutableStateFlow<AsyncValue<List<com.example.f1_kmp.data.model.CircuitRaceWin>>>(AsyncValue.Loading)
    val winners: StateFlow<AsyncValue<List<com.example.f1_kmp.data.model.CircuitRaceWin>>> = _winners.asStateFlow()

    private val _error = MutableStateFlow<AppException?>(null)
    val error: StateFlow<AppException?> = _error.asStateFlow()

    init {
        loadAllData()
    }

    /** Повторный вызов — retry с экрана ошибки. */
    fun loadAllData() {
        loadJob.launch(viewModelScope) {
            _error.value = null
            loadCircuit()
            loadWinners()
        }
    }

    private suspend fun loadCircuit() {
        repository.peekCircuitsCache()?.find { it.circuitId == circuitId }?.let {
            _circuit.value = AsyncValue.Value(it)
        } ?: run { _circuit.value = AsyncValue.Loading }

        repository.getCircuitById(circuitId).applyUnlessCached(
            current = _circuit.value,
            onSuccess = { found ->
                if (found != null) {
                    _circuit.value = AsyncValue.Value(found)
                } else {
                    _circuit.value = AsyncValue.Error(ErrorStrings.circuitNotFound)
                }
            },
            onFailure = { ex -> _circuit.value = AsyncValue.Error(ex.title, ex.subtitle) },
        )
    }

    private suspend fun loadWinners() {
        _winners.value = AsyncValue.Loading
        repository.getCircuitWinners(circuitId).applyUnlessCached(
            current = _winners.value,
            onSuccess = { _winners.value = AsyncValue.Value(it) },
            onFailure = { ex ->
                _winners.value = AsyncValue.Error(ex.title, ex.subtitle)
                _error.value = ex
            },
        )
    }
}

/**
 * ViewModel карточки пилота.
 *
 * [driverId] — аргумент маршрута `driver_detail/{driverId}`.
 * Загружает профиль и карьерную статистику через [F1Repository].
 */
class DriverDetailViewModel(
    private val driverId: String,
    private val repository: F1Repository,
) : ViewModel() {
    private val loadJob = LoadJobHolder()

    private val _driver = MutableStateFlow<AsyncValue<com.example.f1_kmp.data.model.DriverModel>>(AsyncValue.Loading)
    val driver: StateFlow<AsyncValue<com.example.f1_kmp.data.model.DriverModel>> = _driver.asStateFlow()

    private val _careerStats = MutableStateFlow<AsyncValue<com.example.f1_kmp.data.model.CareerStats<com.example.f1_kmp.data.model.ConstructorModel>>>(AsyncValue.Loading)
    val careerStats: StateFlow<AsyncValue<com.example.f1_kmp.data.model.CareerStats<com.example.f1_kmp.data.model.ConstructorModel>>> = _careerStats.asStateFlow()

    private val _error = MutableStateFlow<AppException?>(null)
    val error: StateFlow<AppException?> = _error.asStateFlow()

    init {
        loadAllData()
    }

    /** Повторный вызов — retry с экрана ошибки. */
    fun loadAllData() {
        loadJob.launch(viewModelScope) {
            _error.value = null
            _driver.value = AsyncValue.Loading
            _careerStats.value = AsyncValue.Loading

            val currentConstructors = repository.currentConstructorsForDriver(driverId)
            val driverResult = repository.getDriver(driverId)
            driverResult.onFailure { e ->
                val ex = e as AppException
                _driver.value = AsyncValue.Error(ex.title, ex.subtitle)
                _error.value = ex
                return@launch
            }
            val loadedDriver = driverResult.getOrNull()
            if (loadedDriver == null) {
                _driver.value = AsyncValue.Error(ErrorStrings.driverNotFound)
                return@launch
            }
            _driver.value = AsyncValue.Value(loadedDriver)

            repository.getDriverCareerStats(driverId, currentConstructors).applyUnlessCached(
                current = _careerStats.value,
                onSuccess = { _careerStats.value = AsyncValue.Value(it) },
                onFailure = { ex ->
                    _careerStats.value = AsyncValue.Error(ex.title, ex.subtitle)
                    _error.value = ex
                },
            )
        }
    }
}

/**
 * ViewModel карточки конструктора.
 *
 * [constructorId] — аргумент маршрута `constructor_detail/{constructorId}`.
 * Загружает профиль и карьерную статистику через [F1Repository].
 */
class ConstructorDetailViewModel(
    private val constructorId: String,
    private val repository: F1Repository,
) : ViewModel() {
    private val loadJob = LoadJobHolder()

    private val _constructor = MutableStateFlow<AsyncValue<com.example.f1_kmp.data.model.ConstructorModel>>(AsyncValue.Loading)
    val constructor: StateFlow<AsyncValue<com.example.f1_kmp.data.model.ConstructorModel>> = _constructor.asStateFlow()

    private val _careerStats = MutableStateFlow<AsyncValue<com.example.f1_kmp.data.model.CareerStats<com.example.f1_kmp.data.model.DriverModel>>>(AsyncValue.Loading)
    val careerStats: StateFlow<AsyncValue<com.example.f1_kmp.data.model.CareerStats<com.example.f1_kmp.data.model.DriverModel>>> = _careerStats.asStateFlow()

    private val _error = MutableStateFlow<AppException?>(null)
    val error: StateFlow<AppException?> = _error.asStateFlow()

    init {
        loadAllData()
    }

    /** Повторный вызов — retry с экрана ошибки. */
    fun loadAllData() {
        loadJob.launch(viewModelScope) {
            _error.value = null
            _constructor.value = AsyncValue.Loading
            _careerStats.value = AsyncValue.Loading

            val currentDrivers = repository.currentDriversForConstructor(constructorId)
            val constructorResult = repository.getConstructor(constructorId)
            constructorResult.onFailure { e ->
                val ex = e as AppException
                _constructor.value = AsyncValue.Error(ex.title, ex.subtitle)
                _error.value = ex
                return@launch
            }
            val loaded = constructorResult.getOrNull()
            if (loaded == null) {
                _constructor.value = AsyncValue.Error(ErrorStrings.constructorNotFound)
                return@launch
            }
            _constructor.value = AsyncValue.Value(loaded)

            repository.getConstructorCareerStats(constructorId, currentDrivers).applyUnlessCached(
                current = _careerStats.value,
                onSuccess = { _careerStats.value = AsyncValue.Value(it) },
                onFailure = { ex ->
                    _careerStats.value = AsyncValue.Error(ex.title, ex.subtitle)
                    _error.value = ex
                },
            )
        }
    }
}
