package com.example.f1_kmp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.f1_kmp.data.model.NewsArticle
import com.example.f1_kmp.data.repository.IEspnRepository
import com.example.f1_kmp.domain.AppDataRefresh
import com.example.f1_kmp.domain.AsyncValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** ViewModel вкладки «Новости» (ESPN). */
class NewsViewModel(
    private val espnRepository: IEspnRepository,
    private val appDataRefresh: AppDataRefresh,
) : ViewModel() {
    private val loadJob = LoadJobHolder()

    private val _articles = MutableStateFlow<AsyncValue<List<NewsArticle>>>(AsyncValue.Loading)
    val articles: StateFlow<AsyncValue<List<NewsArticle>>> = _articles.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadArticles()
    }

    fun loadArticles(forceRefresh: Boolean = false) {
        loadJob.launch(viewModelScope) {
            if (forceRefresh) {
                _isRefreshing.value = true
                appDataRefresh.clearAll()
            }
            try {
                if (!forceRefresh) {
                    espnRepository.peekNews?.let { cached ->
                        _articles.value = AsyncValue.Value(cached)
                        if (espnRepository.isNewsFresh) return@launch
                    } ?: run { _articles.value = AsyncValue.Loading }
                } else if (_articles.value !is AsyncValue.Value) {
                    _articles.value = AsyncValue.Loading
                }

                espnRepository.getNews(forceRefresh = forceRefresh).applyUnlessCached(
                    current = _articles.value,
                    onSuccess = { _articles.value = AsyncValue.Value(it) },
                    onFailure = { ex -> _articles.value = AsyncValue.Error(ex.title, ex.subtitle) },
                )
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun refreshAll() = loadArticles(forceRefresh = true)
}
