package com.example.f1_kmp.ui.screens.circuits

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.f1_kmp.data.circuits.CircuitLayoutAssets
import com.example.f1_kmp.domain.model.Circuit
import com.example.f1_kmp.domain.model.Driver
import com.example.f1_kmp.domain.AsyncValue
import com.example.f1_kmp.ui.components.CachedDataBanner
import com.example.f1_kmp.ui.components.CareerListTile
import com.example.f1_kmp.ui.components.CountryFlag
import com.example.f1_kmp.ui.components.CustomSwitcher
import com.example.f1_kmp.ui.components.ErrorBody
import com.example.f1_kmp.ui.components.LinkText
import com.example.f1_kmp.ui.components.OnAppResumed
import com.example.f1_kmp.ui.components.circuits.CircuitLayoutImage
import com.example.f1_kmp.ui.components.circuits.CircuitStatsGrid
import com.example.f1_kmp.ui.components.shimmer.CircuitScreenShimmer
import com.example.f1_kmp.ui.components.shimmer.CircuitsShimmer
import com.example.f1_kmp.ui.components.shimmer.ListRowsShimmer
import com.example.f1_kmp.ui.map.CircuitsMapContent
import com.example.f1_kmp.ui.theme.AppDimens
import com.example.f1_kmp.ui.theme.AppStyles
import com.example.f1_kmp.ui.theme.F1Red
import com.example.f1_kmp.util.RegisterShareAction
import com.example.f1_kmp.util.openUrl
import com.example.f1_kmp.util.shareCircuitDeepLink
import com.example.f1_kmp.viewmodel.CircuitDetailViewModel
import com.example.f1_kmp.viewmodel.CircuitsViewModel
import f1_kmp.composeapp.generated.resources.Res
import f1_kmp.composeapp.generated.resources.as_list
import f1_kmp.composeapp.generated.resources.circuit_winners_empty
import f1_kmp.composeapp.generated.resources.circuit_winners_title
import f1_kmp.composeapp.generated.resources.city_label
import f1_kmp.composeapp.generated.resources.country
import f1_kmp.composeapp.generated.resources.on_map
import f1_kmp.composeapp.generated.resources.read_on_wikipedia
import com.example.f1_kmp.domain.stringResource

/**
 * Экран «Трассы»: переключатель закреплён сверху, контент — в [Box] с [Modifier.weight].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircuitsScreen(
    viewModel: CircuitsViewModel,
    onCircuitClick: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    OnAppResumed(onResumed = viewModel::dismissOfflineBannerIfOnline)

    when (val state = uiState.circuits) {
        is AsyncValue.Loading -> if (!uiState.isRefreshing) {
            CircuitsShimmer(modifier = Modifier.fillMaxSize())
        }
        is AsyncValue.Error -> ErrorBody(
            state.message,
            state.subtitle,
            onRetry = viewModel::refreshAll,
            modifier = Modifier.fillMaxSize(),
        )
        is AsyncValue.Value -> Column(modifier = Modifier.fillMaxSize()) {
            if (uiState.showingCachedData) {
                CachedDataBanner()
            }
            Spacer(Modifier.height(12.dp))
            CustomSwitcher(
                stringResource(Res.string.on_map),
                stringResource(Res.string.as_list),
                uiState.activePage,
                viewModel::changeActivePage,
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                when (uiState.activePage) {
                    0 -> CircuitsMapContent(state.value, onCircuitClick)
                    else -> PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = viewModel::refreshAll,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        CircuitsList(state.value, onCircuitClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun CircuitsList(circuits: List<Circuit>, onCircuitClick: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = AppDimens.horizontalPadding.dp)) {
        items(circuits) { circuit ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, F1Red, RoundedCornerShape(20.dp))
                    .clickable { onCircuitClick(circuit.circuitId) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = circuit.circuitName,
                    style = AppStyles.h3,
                    modifier = Modifier.weight(1f),
                )
                if (CircuitLayoutAssets.hasLayout(circuit.circuitId)) {
                    Box(modifier = Modifier.width(72.dp).height(48.dp)) {
                        CircuitLayoutImage(
                            circuitId = circuit.circuitId,
                            height = 48.dp,
                            padding = 0.dp,
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = F1Red,
                    )
                }
            }
        }
    }
}

/** Карточка трассы: схема, stats, Wikipedia, флаг и история победителей. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircuitDetailScreen(
    viewModel: CircuitDetailViewModel,
    onDriverClick: (Driver) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val circuit = (uiState.circuit as? AsyncValue.Value)?.value
    val shareAction = if (circuit != null) {
        remember(circuit.circuitId, circuit.circuitName) {
            { shareCircuitDeepLink(circuit.circuitId, circuit.circuitName) }
        }
    } else {
        null
    }
    RegisterShareAction(shareAction)

    when (val state = uiState.circuit) {
        is AsyncValue.Loading -> if (!uiState.isRefreshing) {
            CircuitScreenShimmer(modifier = Modifier.fillMaxSize())
        }
        is AsyncValue.Error -> ErrorBody(
            state.message,
            state.subtitle,
            onRetry = viewModel::loadAllData,
            modifier = Modifier.fillMaxSize(),
        )
        is AsyncValue.Value -> PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = viewModel::refreshAll,
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AppDimens.horizontalPadding.dp, vertical = AppDimens.verticalPadding.dp),
            ) {
                if (CircuitLayoutAssets.hasLayout(state.value.circuitId)) {
                    CircuitLayoutImage(circuitId = state.value.circuitId, height = 220.dp)
                    Spacer(Modifier.height(16.dp))
                }
                Text(state.value.circuitName, style = AppStyles.h1)
                uiState.stats?.let {
                    Spacer(Modifier.height(16.dp))
                    CircuitStatsGrid(stats = it)
                }
                Spacer(Modifier.height(16.dp))
                LinkText(stringResource(Res.string.read_on_wikipedia)) { openUrl(state.value.url) }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${stringResource(Res.string.country)}: ", style = AppStyles.h3)
                    CountryFlag(
                        countryOrNationality = state.value.location.country,
                        fontSize = 28.sp,
                        fallbackStyle = AppStyles.h3,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(stringResource(Res.string.city_label, state.value.location.locality), style = AppStyles.h3)
                Spacer(Modifier.height(28.dp))
                Text(stringResource(Res.string.circuit_winners_title), style = AppStyles.h2)
                Spacer(Modifier.height(12.dp))
                when (val winners = uiState.winners) {
                    is AsyncValue.Loading -> if (!uiState.isRefreshing) {
                        ListRowsShimmer(rowCount = 4)
                    }
                    is AsyncValue.Error -> Text(winners.message, style = AppStyles.body)
                    is AsyncValue.Value -> {
                        if (winners.value.isEmpty()) {
                            Text(stringResource(Res.string.circuit_winners_empty), style = AppStyles.body)
                        } else {
                            winners.value.forEach { win ->
                                CareerListTile(
                                    title = "${win.season} · ${win.raceName}",
                                    subtitle = "${win.driver.fullName} · ${win.constructor.name}",
                                    onClick = { onDriverClick(win.driver) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
