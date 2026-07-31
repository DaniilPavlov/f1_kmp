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
import kotlinx.coroutines.flow.update

data class DriverDetailUiState(
    val driver: AsyncValue<Driver> = AsyncValue.Loading,
    val careerStats: AsyncValue<CareerStats<Constructor>> = AsyncValue.Loading,
    val espnCard: EspnDriverCardData = EspnDriverCardData(),
    val error: AppError? = null,
    val isRefreshing: Boolean = false,
)

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

    private val _uiState = MutableStateFlow(DriverDetailUiState())
    val uiState: StateFlow<DriverDetailUiState> = _uiState.asStateFlow()

    init {
        loadAllData()
    }

    /** Повторный вызов — retry с экрана ошибки. */
    fun loadAllData() {
        loadJob.launch(viewModelScope) {
            loadInternal(softRefresh = false)
        }
    }

    /** Pull-to-refresh: keep Values while reloading. */
    fun refreshAll() {
        loadJob.launch(viewModelScope) {
            loadInternal(softRefresh = true)
        }
    }

    private suspend fun loadInternal(softRefresh: Boolean) {
        try {
            if (softRefresh) {
                _uiState.update {
                    it.copy(
                        isRefreshing = true,
                        error = null,
                        driver = if (it.driver is AsyncValue.Value) it.driver else AsyncValue.Loading,
                        careerStats = if (it.careerStats is AsyncValue.Value) {
                            it.careerStats
                        } else {
                            AsyncValue.Loading
                        },
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        error = null,
                        driver = AsyncValue.Loading,
                        careerStats = AsyncValue.Loading,
                        espnCard = EspnDriverCardData(),
                    )
                }
            }

            val currentConstructors = repository.currentConstructorsForDriver(driverId)
            val driverResult = repository.getDriver(driverId)
            driverResult.onFailure { e ->
                val err = e.toAppError()
                if (_uiState.value.driver !is AsyncValue.Value) {
                    _uiState.update {
                        it.copy(
                            driver = err.toAsyncError(),
                            error = err,
                        )
                    }
                }
                return
            }
            val loadedDriver = driverResult.getOrNull()
            if (loadedDriver == null) {
                if (_uiState.value.driver !is AsyncValue.Value) {
                    _uiState.update { it.copy(driver = AsyncValue.Error(ErrorStrings.driverNotFound)) }
                }
                return
            }
            _uiState.update { it.copy(driver = AsyncValue.Value(loadedDriver)) }

            coroutineScope {
                val careerDeferred = async {
                    repository.getDriverCareerStats(driverId, currentConstructors)
                }
                val espnDeferred = async {
                    espnRepository.driverCardData(loadedDriver.givenName, loadedDriver.familyName)
                }

                careerDeferred.await().applyUnlessCached(
                    current = _uiState.value.careerStats,
                    onSuccess = { stats ->
                        _uiState.update { it.copy(careerStats = AsyncValue.Value(stats)) }
                    },
                    onFailure = { err ->
                        _uiState.update {
                            it.copy(
                                careerStats = err.toAsyncError(),
                                error = err,
                            )
                        }
                    },
                )
                _uiState.update { it.copy(espnCard = espnDeferred.await()) }
            }
        } finally {
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }
}
