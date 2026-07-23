package com.example.f1_kmp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.f1_kmp.data.model.CareerStats
import com.example.f1_kmp.domain.model.Constructor
import com.example.f1_kmp.domain.model.Driver
import com.example.f1_kmp.data.model.EspnDriverCardData
import com.example.f1_kmp.data.repository.IEspnRepository
import com.example.f1_kmp.data.repository.IF1Repository
import com.example.f1_kmp.domain.AppError
import com.example.f1_kmp.domain.toAppError
import com.example.f1_kmp.domain.ErrorStrings
import com.example.f1_kmp.domain.AsyncValue
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel карточки пилота.
 *
 * [driverId] — аргумент маршрута `driver_detail/{driverId}`.
 * Загружает профиль и карьерную статистику через [IF1Repository].
 */
class DriverDetailViewModel(
    private val driverId: String,
    private val repository: IF1Repository,
    private val espnRepository: IEspnRepository,
) : ViewModel() {
    private val loadJob = LoadJobHolder()

    private val _driver = MutableStateFlow<AsyncValue<Driver>>(AsyncValue.Loading)
    val driver: StateFlow<AsyncValue<Driver>> = _driver.asStateFlow()

    private val _careerStats = MutableStateFlow<AsyncValue<CareerStats<Constructor>>>(AsyncValue.Loading)
    val careerStats: StateFlow<AsyncValue<CareerStats<Constructor>>> = _careerStats.asStateFlow()

    private val _espnCard = MutableStateFlow(EspnDriverCardData())
    val espnCard: StateFlow<EspnDriverCardData> = _espnCard.asStateFlow()

    private val _error = MutableStateFlow<AppError?>(null)
    val error: StateFlow<AppError?> = _error.asStateFlow()

    init {
        loadAllData()
    }

    /** Повторный вызов — retry с экрана ошибки. */
    fun loadAllData() {
        loadJob.launch(viewModelScope) {
            _error.value = null
            _driver.value = AsyncValue.Loading
            _careerStats.value = AsyncValue.Loading
            _espnCard.value = EspnDriverCardData()

            val currentConstructors = repository.currentConstructorsForDriver(driverId)
            val driverResult = repository.getDriver(driverId)
            driverResult.onFailure { e ->
                val ex = e.toAppError()
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

            coroutineScope {
                val careerDeferred = async {
                    repository.getDriverCareerStats(driverId, currentConstructors)
                }
                val espnDeferred = async {
                    espnRepository.driverCardData(loadedDriver.givenName, loadedDriver.familyName)
                }

                careerDeferred.await().applyUnlessCached(
                    current = _careerStats.value,
                    onSuccess = { _careerStats.value = AsyncValue.Value(it) },
                    onFailure = { ex ->
                        _careerStats.value = AsyncValue.Error(ex.title, ex.subtitle)
                        _error.value = ex
                    },
                )
                _espnCard.value = espnDeferred.await()
            }
        }
    }
}
