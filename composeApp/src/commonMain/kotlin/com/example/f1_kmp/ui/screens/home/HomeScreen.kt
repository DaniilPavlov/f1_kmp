package com.example.f1_kmp.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.dp
import com.example.f1_kmp.domain.AsyncValue
import com.example.f1_kmp.domain.model.Constructor
import com.example.f1_kmp.domain.model.Driver
import com.example.f1_kmp.domain.stringResource
import com.example.f1_kmp.ui.components.CachedDataBanner
import com.example.f1_kmp.ui.components.CustomSwitcher
import com.example.f1_kmp.ui.components.ErrorBody
import com.example.f1_kmp.ui.components.OnAppResumed
import com.example.f1_kmp.ui.components.TournamentConstructorsTable
import com.example.f1_kmp.ui.components.TournamentDriversTable
import com.example.f1_kmp.ui.components.shimmer.TournamentTablesShimmer
import com.example.f1_kmp.ui.theme.AppDimens
import com.example.f1_kmp.ui.theme.AppStyles
import com.example.f1_kmp.ui.theme.F1Red
import com.example.f1_kmp.viewmodel.HomeUiState
import com.example.f1_kmp.viewmodel.HomeViewModel
import com.example.f1_kmp.viewmodel.NewsUiState
import com.example.f1_kmp.viewmodel.NewsViewModel
import f1_kmp.composeapp.generated.resources.Res
import f1_kmp.composeapp.generated.resources.constructors
import f1_kmp.composeapp.generated.resources.drivers
import f1_kmp.composeapp.generated.resources.home_scroll_to_news
import f1_kmp.composeapp.generated.resources.home_standings_title
import f1_kmp.composeapp.generated.resources.round_label
import f1_kmp.composeapp.generated.resources.season_label
import kotlinx.coroutines.launch

private const val FabExtraOffsetPx = 500f
private const val RevealMoreThresholdPx = 480

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    newsViewModel: NewsViewModel,
    onDriverClick: (Driver) -> Unit,
    onConstructorClick: (Constructor) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val newsState by newsViewModel.uiState.collectAsState()
    HomeScreenContent(
        uiState = uiState,
        newsState = newsState,
        onRetry = {
            viewModel.refreshAll()
            newsViewModel.refreshAll()
        },
        onRefresh = {
            viewModel.refreshAll()
            newsViewModel.refreshAll()
        },
        onChangeTable = viewModel::changeActiveTable,
        onRevealMoreNews = newsViewModel::revealMore,
        onDismissOfflineBanner = viewModel::dismissOfflineBannerIfOnline,
        onDriverClick = onDriverClick,
        onConstructorClick = onConstructorClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Suppress("LongMethod")
@Composable
fun HomeScreenContent(
    uiState: HomeUiState,
    newsState: NewsUiState = NewsUiState(),
    onRetry: () -> Unit,
    onChangeTable: (Int) -> Unit,
    onDriverClick: (Driver) -> Unit = {},
    onConstructorClick: (Constructor) -> Unit = {},
    onRefresh: () -> Unit = {},
    onRevealMoreNews: () -> Unit = {},
    onDismissOfflineBanner: () -> Unit = {},
) {
    OnAppResumed(onResumed = onDismissOfflineBanner)

    val drivers = uiState.drivers
    val constructors = uiState.constructors

    when {
        !uiState.isRefreshing && (drivers.isLoading || constructors.isLoading) -> {
            TournamentTablesShimmer(modifier = Modifier.fillMaxSize())
        }
        uiState.error != null -> ErrorBody(
            uiState.error?.title,
            uiState.error?.subtitle,
            onRetry = onRetry,
            modifier = Modifier.fillMaxSize(),
        )
        drivers is AsyncValue.Value && constructors is AsyncValue.Value -> {
            val scrollState = rememberScrollState()
            val scope = rememberCoroutineScope()
            val newsTitleRequester = remember { BringIntoViewRequester() }
            var titleBottomInWindow by remember { mutableStateOf<Float?>(null) }
            var viewportTopInWindow by remember { mutableStateOf<Float?>(null) }

            val showScrollToNews by remember {
                derivedStateOf {
                    scrollState.value
                    val titleBottom = titleBottomInWindow ?: return@derivedStateOf false
                    val viewportTop = viewportTopInWindow ?: return@derivedStateOf false
                    viewportTop - titleBottom >= FabExtraOffsetPx
                }
            }

            LaunchedEffect(scrollState.value, newsState.canRevealMore) {
                val remaining = scrollState.maxValue - scrollState.value
                if (newsState.canRevealMore && remaining < RevealMoreThresholdPx) {
                    onRevealMoreNews()
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing || newsState.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { coords ->
                            viewportTopInWindow = coords.positionInWindow().y
                        },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(vertical = AppDimens.verticalPadding.dp),
                    ) {
                        if (uiState.showingCachedData) {
                            CachedDataBanner(modifier = Modifier.padding(bottom = 8.dp))
                        }
                        Column(modifier = Modifier.padding(horizontal = AppDimens.horizontalPadding.dp)) {
                            Text(stringResource(Res.string.home_standings_title), style = AppStyles.h1)
                            Spacer(Modifier.height(32.dp))
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    stringResource(Res.string.season_label, uiState.season),
                                    style = AppStyles.h2,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(stringResource(Res.string.round_label, uiState.round), style = AppStyles.h2)
                            }
                        }
                        Spacer(Modifier.height(32.dp))
                        CustomSwitcher(
                            stringResource(Res.string.drivers),
                            stringResource(Res.string.constructors),
                            uiState.activeTable,
                            onChangeTable,
                        )
                        Spacer(Modifier.height(8.dp))
                        if (uiState.activeTable == 0) {
                            TournamentDriversTable(drivers.value, onDriverClick = onDriverClick)
                        } else {
                            TournamentConstructorsTable(constructors.value, onConstructorClick = onConstructorClick)
                        }
                        Spacer(Modifier.height(32.dp))
                        HomeHeadlinesSection(
                            articles = newsState.articles,
                            visibleArticles = newsState.visibleArticles,
                            titleModifier = Modifier
                                .bringIntoViewRequester(newsTitleRequester)
                                .onGloballyPositioned { coords ->
                                    titleBottomInWindow =
                                        coords.positionInWindow().y + coords.size.height
                                },
                        )
                    }
                }

                if (showScrollToNews) {
                    FloatingActionButton(
                        onClick = { scope.launch { newsTitleRequester.bringIntoView() } },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        containerColor = F1Red,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowUp,
                            contentDescription = stringResource(Res.string.home_scroll_to_news),
                        )
                    }
                }
            }
        }
    }
}
