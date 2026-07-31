package com.example.f1_kmp.ui.screens.finishstatus

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.f1_kmp.data.model.FinishStatusItem
import com.example.f1_kmp.domain.AsyncValue
import com.example.f1_kmp.ui.components.ErrorBody
import com.example.f1_kmp.ui.components.SeasonPickerField
import com.example.f1_kmp.ui.components.shimmer.ListRowsShimmer
import com.example.f1_kmp.ui.theme.AppDimens
import com.example.f1_kmp.ui.theme.AppStyles
import com.example.f1_kmp.ui.theme.F1Red
import com.example.f1_kmp.ui.theme.F1StrokeGray
import com.example.f1_kmp.ui.theme.F1TextGray
import com.example.f1_kmp.viewmodel.FinishStatusViewModel
import f1_kmp.composeapp.generated.resources.Res
import f1_kmp.composeapp.generated.resources.finish_status_empty
import f1_kmp.composeapp.generated.resources.finish_status_subtitle
import f1_kmp.composeapp.generated.resources.season
import f1_kmp.composeapp.generated.resources.select_season
import com.example.f1_kmp.domain.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinishStatusScreen(viewModel: FinishStatusViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    PullToRefreshBox(
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
            Text(stringResource(Res.string.finish_status_subtitle), style = AppStyles.body)
            Spacer(Modifier.height(16.dp))
            SeasonPickerField(
                value = uiState.year,
                label = stringResource(Res.string.season),
                hint = stringResource(Res.string.select_season),
                onSeasonSelected = viewModel::onYearChanged,
                loadSeasons = viewModel::loadSeasonYears,
            )
            Spacer(Modifier.height(20.dp))
            when (val state = uiState.statuses) {
                is AsyncValue.Loading -> if (!uiState.isRefreshing) ListRowsShimmer(rowCount = 8)
                is AsyncValue.Error -> ErrorBody(
                    state.message,
                    state.subtitle,
                    onRetry = viewModel::loadAllData,
                )
                is AsyncValue.Value -> StatusList(state.value)
            }
        }
    }
}

@Composable
private fun StatusList(items: List<FinishStatusItem>) {
    if (items.isEmpty()) {
        Text(
            stringResource(Res.string.finish_status_empty),
            style = AppStyles.body,
            modifier = Modifier.padding(vertical = 24.dp),
        )
        return
    }
    val total = items.sumOf { it.count }
    items.forEach { item ->
        val color = if (item.isHighlight) F1Red else Color.Black
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
        ) {
            Text(
                item.status,
                style = AppStyles.body.copy(color = color),
                modifier = Modifier.weight(1f),
            )
            Text("${item.count}", style = AppStyles.h3.copy(color = color))
            if (total > 0) {
                Spacer(Modifier.width(8.dp))
                Text(
                    "${((item.count.toDouble() / total) * 100).toInt()}%",
                    style = AppStyles.caption.copy(color = F1TextGray),
                    textAlign = TextAlign.Right,
                    modifier = Modifier.width(48.dp),
                )
            }
        }
        Divider(color = F1StrokeGray)
    }
}
