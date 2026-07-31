package com.example.f1_kmp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.f1_kmp.data.model.CareerStats
import com.example.f1_kmp.domain.model.Constructor
import com.example.f1_kmp.domain.model.Driver
import com.example.f1_kmp.data.model.NewsArticle
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

data class ConstructorDetailUiState(
    val constructor: AsyncValue<Constructor> = AsyncValue.Loading,
    val careerStats: AsyncValue<CareerStats<Driver>> = AsyncValue.Loading,
    val news: List<NewsArticle> = emptyList(),
    val error: AppError? = null,
    val isRefreshing: Boolean = false,
)

/**
 * ViewModel карточки конструктора.
 *
 * [constructorId] — аргумент маршрута `constructor_detail/{constructorId}`.
 * Загружает профиль и карьерную статистику через [IF1Repository].
 */
class ConstructorDetailViewModel(
    private val constructorId: String,
    private val repository: IF1Repository,
    private val espnRepository: IEspnRepository,
) : ViewModel() {
    private val loadJob = LoadJobHolder()

    private val _uiState = MutableStateFlow(ConstructorDetailUiState())
    val uiState: StateFlow<ConstructorDetailUiState> = _uiState.asStateFlow()

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
                        constructor = if (it.constructor is AsyncValue.Value) {
                            it.constructor
                        } else {
                            AsyncValue.Loading
                        },
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
                        constructor = AsyncValue.Loading,
                        careerStats = AsyncValue.Loading,
                        news = emptyList(),
                    )
                }
            }

            val currentDrivers = repository.currentDriversForConstructor(constructorId)
            val constructorResult = repository.getConstructor(constructorId)
            constructorResult.onFailure { e ->
                val err = e.toAppError()
                if (_uiState.value.constructor !is AsyncValue.Value) {
                    _uiState.update {
                        it.copy(
                            constructor = err.toAsyncError(),
                            error = err,
                        )
                    }
                }
                return
            }
            val loaded = constructorResult.getOrNull()
            if (loaded == null) {
                if (_uiState.value.constructor !is AsyncValue.Value) {
                    _uiState.update {
                        it.copy(constructor = AsyncValue.Error(ErrorStrings.constructorNotFound))
                    }
                }
                return
            }
            _uiState.update { it.copy(constructor = AsyncValue.Value(loaded)) }

            coroutineScope {
                val careerDeferred = async {
                    repository.getConstructorCareerStats(constructorId, currentDrivers)
                }
                val newsDeferred = async {
                    espnRepository.constructorNews(loaded.constructorId, loaded.name)
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
                _uiState.update { it.copy(news = newsDeferred.await()) }
            }
        } finally {
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }
}
