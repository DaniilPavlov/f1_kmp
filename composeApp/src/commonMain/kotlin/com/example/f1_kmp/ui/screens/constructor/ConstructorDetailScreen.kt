package com.example.f1_kmp.ui.screens.constructor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.f1_kmp.data.model.CareerStats
import com.example.f1_kmp.domain.model.Circuit
import com.example.f1_kmp.domain.model.Driver
import com.example.f1_kmp.data.model.NewsArticle
import com.example.f1_kmp.domain.AsyncValue
import com.example.f1_kmp.ui.components.CareerDetailSections
import com.example.f1_kmp.ui.components.CareerInfoRow
import com.example.f1_kmp.ui.components.CountryFlag
import com.example.f1_kmp.ui.components.ErrorBody
import com.example.f1_kmp.ui.components.NewsArticleTile
import com.example.f1_kmp.ui.components.WikipediaLink
import com.example.f1_kmp.ui.components.shimmer.CareerScreenShimmer
import com.example.f1_kmp.ui.theme.AppDimens
import com.example.f1_kmp.ui.theme.AppStyles
import com.example.f1_kmp.util.RegisterShareAction
import com.example.f1_kmp.util.openUrl
import com.example.f1_kmp.util.rememberShareCareerAction
import com.example.f1_kmp.viewmodel.ConstructorDetailViewModel
import f1_kmp.composeapp.generated.resources.Res
import f1_kmp.composeapp.generated.resources.constructor_drivers_title
import f1_kmp.composeapp.generated.resources.current_drivers
import f1_kmp.composeapp.generated.resources.driver_news_title
import f1_kmp.composeapp.generated.resources.nationality
import com.example.f1_kmp.domain.stringResource

/** Экран карточки конструктора: профиль, карьера, пилоты и ESPN-новости. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConstructorDetailScreen(
    viewModel: ConstructorDetailViewModel,
    onDriverClick: (Driver) -> Unit,
    onCircuitClick: (Circuit) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val constructor = uiState.constructor
    val career = uiState.careerStats

    when {
        !uiState.isRefreshing && (constructor.isLoading || career.isLoading) -> CareerScreenShimmer(
            showPhoto = false,
            modifier = Modifier.fillMaxSize(),
        )
        constructor is AsyncValue.Value && career is AsyncValue.Value -> {
            val model = constructor.value
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::refreshAll,
                modifier = Modifier.fillMaxSize(),
            ) {
                ConstructorContent(
                    name = model.name,
                    nationality = model.nationality,
                    url = model.url,
                    stats = career.value,
                    news = uiState.news,
                    onDriverClick = onDriverClick,
                    onCircuitClick = onCircuitClick,
                    onWikipediaClick = { openUrl(model.url) },
                )
            }
        }
        else -> {
            val asyncError = (constructor as? AsyncValue.Error) ?: (career as? AsyncValue.Error)
            ErrorBody(
                uiState.error?.title ?: asyncError?.message,
                uiState.error?.subtitle ?: asyncError?.subtitle,
                onRetry = viewModel::loadAllData,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ConstructorContent(
    name: String,
    nationality: String,
    url: String,
    stats: CareerStats<Driver>,
    news: List<NewsArticle>,
    onDriverClick: (Driver) -> Unit,
    onCircuitClick: (Circuit) -> Unit,
    onWikipediaClick: () -> Unit,
) {
    RegisterShareAction(
        rememberShareCareerAction(
            title = name,
            races = stats.races,
            wins = stats.wins,
            podiums = stats.podiums,
            poles = stats.poles,
        ),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AppDimens.horizontalPadding.dp, vertical = AppDimens.verticalPadding.dp),
    ) {
        Text(name, style = AppStyles.h1)
        Spacer(Modifier.height(16.dp))
        CareerInfoRow(stringResource(Res.string.nationality)) {
            CountryFlag(countryOrNationality = nationality, fontSize = 28.sp)
        }
        if (stats.current.isNotEmpty()) {
            CareerInfoRow(
                stringResource(Res.string.current_drivers),
                stats.current.joinToString(", ") { it.fullName },
            )
        }
        if (url.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            WikipediaLink(onWikipediaClick)
        }
        CareerDetailSections(
            stats = stats,
            relatedSectionTitle = stringResource(Res.string.constructor_drivers_title),
            relatedItemTitle = { it.fullName },
            onRelatedItemClick = onDriverClick,
            onCircuitClick = onCircuitClick,
            relatedItemTrailing = { CountryFlag(countryOrNationality = it.nationality) },
        )
        if (news.isNotEmpty()) {
            Spacer(Modifier.height(28.dp))
            Text(stringResource(Res.string.driver_news_title), style = AppStyles.h2)
            Spacer(Modifier.height(12.dp))
            news.forEach { article ->
                NewsArticleTile(article)
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
