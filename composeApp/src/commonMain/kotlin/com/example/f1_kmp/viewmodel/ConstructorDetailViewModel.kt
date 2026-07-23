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

    private val _constructor = MutableStateFlow<AsyncValue<Constructor>>(AsyncValue.Loading)
    val constructor: StateFlow<AsyncValue<Constructor>> = _constructor.asStateFlow()

    private val _careerStats = MutableStateFlow<AsyncValue<CareerStats<Driver>>>(AsyncValue.Loading)
    val careerStats: StateFlow<AsyncValue<CareerStats<Driver>>> = _careerStats.asStateFlow()

    private val _news = MutableStateFlow<List<NewsArticle>>(emptyList())
    val news: StateFlow<List<NewsArticle>> = _news.asStateFlow()

    private val _error = MutableStateFlow<AppError?>(null)
    val error: StateFlow<AppError?> = _error.asStateFlow()

    init {
        loadAllData()
    }

    /** Повторный вызов — retry с экрана ошибки. */
    fun loadAllData() {
        loadJob.launch(viewModelScope) {
            _error.value = null
            _constructor.value = AsyncValue.Loading
            _careerStats.value = AsyncValue.Loading
            _news.value = emptyList()

            val currentDrivers = repository.currentDriversForConstructor(constructorId)
            val constructorResult = repository.getConstructor(constructorId)
            constructorResult.onFailure { e ->
                val ex = e.toAppError()
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

            coroutineScope {
                val careerDeferred = async {
                    repository.getConstructorCareerStats(constructorId, currentDrivers)
                }
                val newsDeferred = async {
                    espnRepository.constructorNews(loaded.constructorId, loaded.name)
                }

                careerDeferred.await().applyUnlessCached(
                    current = _careerStats.value,
                    onSuccess = { _careerStats.value = AsyncValue.Value(it) },
                    onFailure = { ex ->
                        _careerStats.value = AsyncValue.Error(ex.title, ex.subtitle)
                        _error.value = ex
                    },
                )
                _news.value = newsDeferred.await()
            }
        }
    }
}
