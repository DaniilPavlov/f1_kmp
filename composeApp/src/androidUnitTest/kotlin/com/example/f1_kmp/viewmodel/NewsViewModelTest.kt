package com.example.f1_kmp.viewmodel

import com.example.f1_kmp.data.model.NewsArticle
import com.example.f1_kmp.data.repository.EspnRepository
import com.example.f1_kmp.domain.AsyncValue
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit-тесты [NewsViewModel] — вкладка «Новости» (ESPN).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NewsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var espnRepository: EspnRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        espnRepository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Свежий in-memory кэш → сразу [AsyncValue.Value], сеть не дергаем. */
    @Test
    fun loadArticles_cacheHitFresh_skipsNetwork() = runTest {
        val cached = listOf(sampleArticle())
        every { espnRepository.peekNews } returns cached
        every { espnRepository.isNewsFresh } returns true

        val viewModel = NewsViewModel(espnRepository)
        advanceUntilIdle()

        assertTrue(viewModel.articles.value is AsyncValue.Value)
        assertEquals("Test Headline", (viewModel.articles.value as AsyncValue.Value).value.first().headline)
        coVerify(exactly = 0) { espnRepository.getNews(any()) }
    }

    /** Нет кэша → [getNews] успешно заполняет [NewsViewModel.articles]. */
    @Test
    fun loadArticles_networkSuccess_setsArticles() = runTest {
        val articles = listOf(sampleArticle(id = 2, headline = "Network Headline"))
        every { espnRepository.peekNews } returns null
        coEvery { espnRepository.getNews(forceRefresh = false) } returns Result.success(articles)

        val viewModel = NewsViewModel(espnRepository)
        advanceUntilIdle()

        assertTrue(viewModel.articles.value is AsyncValue.Value)
        assertEquals("Network Headline", (viewModel.articles.value as AsyncValue.Value).value.first().headline)
    }

    /** Pull-to-refresh → [getNews] с forceRefresh и обновлённый список. */
    @Test
    fun refreshAll_fetchesFromNetwork() = runTest {
        val initial = listOf(sampleArticle())
        val refreshed = listOf(sampleArticle(id = 3, headline = "Refreshed"))
        every { espnRepository.peekNews } returns initial
        every { espnRepository.isNewsFresh } returns true
        coEvery { espnRepository.getNews(forceRefresh = true) } returns Result.success(refreshed)

        val viewModel = NewsViewModel(espnRepository)
        advanceUntilIdle()

        viewModel.refreshAll()
        advanceUntilIdle()

        assertTrue(viewModel.articles.value is AsyncValue.Value)
        assertEquals("Refreshed", (viewModel.articles.value as AsyncValue.Value).value.first().headline)
        assertFalse(viewModel.isRefreshing.value)
        coVerify { espnRepository.getNews(forceRefresh = true) }
    }

    private fun sampleArticle(id: Int = 1, headline: String = "Test Headline") = NewsArticle(
        id = id,
        headline = headline,
        description = "Description",
        webUrl = "https://example.com",
    )
}
