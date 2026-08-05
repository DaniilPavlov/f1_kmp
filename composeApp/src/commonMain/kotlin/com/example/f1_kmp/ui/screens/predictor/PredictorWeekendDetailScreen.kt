package com.example.f1_kmp.ui.screens.predictor

import f1_kmp.composeapp.generated.resources.Res
import f1_kmp.composeapp.generated.resources.predictor_actual
import f1_kmp.composeapp.generated.resources.predictor_compare_empty
import f1_kmp.composeapp.generated.resources.predictor_predicted
import f1_kmp.composeapp.generated.resources.predictor_session_points
import f1_kmp.composeapp.generated.resources.qualifying
import f1_kmp.composeapp.generated.resources.race
import com.example.f1_kmp.domain.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.f1_kmp.domain.predictor.PredictorComparisonRow
import com.example.f1_kmp.domain.predictor.PredictorGridKind
import com.example.f1_kmp.domain.predictor.predictorDriverLabel
import com.example.f1_kmp.ui.components.CustomSwitcher
import com.example.f1_kmp.ui.components.ErrorBody
import com.example.f1_kmp.ui.components.LoadingIndicator
import com.example.f1_kmp.ui.theme.AppDimens
import com.example.f1_kmp.ui.theme.AppStyles
import com.example.f1_kmp.ui.theme.F1Red
import com.example.f1_kmp.ui.theme.appColors
import com.example.f1_kmp.viewmodel.PredictorWeekendDetailViewModel

/** Сравнение прогноза и факта по quali/race. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PredictorWeekendDetailScreen(
    viewModel: PredictorWeekendDetailViewModel,
    onGoSignIn: () -> Unit,
    onBlocked: () -> Unit,
) {
    PredictorAuthGate(
        asTabRoot = false,
        onGoSignIn = onGoSignIn,
        onGoProfile = onBlocked,
        onBlockedNested = onBlocked,
    ) {
        val uiState by viewModel.uiState.collectAsState()
        when {
            uiState.error != null && uiState.weekend == null -> ErrorBody(
                title = uiState.error?.title,
                subtitle = uiState.error?.subtitle,
                onRetry = viewModel::refreshAll,
                modifier = Modifier.fillMaxSize(),
            )
            uiState.isLoading -> LoadingIndicator(Modifier.fillMaxSize())
            else -> {
                val compare = viewModel.activeCompare(uiState)
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = viewModel::refreshAll,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = AppDimens.horizontalPadding.dp)
                            .padding(vertical = AppDimens.verticalPadding.dp),
                    ) {
                        compare?.let {
                            Text(
                                text = stringResource(Res.string.predictor_session_points, it.points),
                                style = AppStyles.h3.copy(color = appColors().black),
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                        CustomSwitcher(
                            firstTitle = stringResource(Res.string.qualifying),
                            secondTitle = stringResource(Res.string.race),
                            activeValue = if (uiState.selectedSession == PredictorGridKind.Qualifying) 0 else 1,
                            onChanged = {
                                viewModel.selectSession(
                                    if (it == 0) PredictorGridKind.Qualifying else PredictorGridKind.Race,
                                )
                            },
                        )
                        Spacer(Modifier.height(12.dp))
                        if (compare == null || compare.rows.isEmpty()) {
                            Text(
                                text = stringResource(Res.string.predictor_compare_empty),
                                style = AppStyles.body.copy(color = appColors().black),
                            )
                        } else {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = stringResource(Res.string.predictor_predicted),
                                    style = AppStyles.caption.copy(color = appColors().black),
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = stringResource(Res.string.predictor_actual),
                                    style = AppStyles.caption.copy(color = appColors().black),
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            compare.rows.forEach { row ->
                                ComparisonRow(
                                    row = row,
                                    predictedLabel = predictorDriverLabel(
                                        uiState.driversById[row.predictedDriverId],
                                        row.predictedDriverId,
                                    ),
                                    actualLabel = predictorDriverLabel(
                                        uiState.driversById[row.actualDriverId],
                                        row.actualDriverId,
                                    ),
                                )
                                Spacer(Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparisonRow(
    row: PredictorComparisonRow,
    predictedLabel: String,
    actualLabel: String,
) {
    val bg = if (row.isCorrect) F1Red.copy(alpha = 0.18f) else appColors().grayBg
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "P${row.position}",
            style = AppStyles.caption.copy(color = appColors().black),
            modifier = Modifier.width(36.dp),
        )
        Text(
            text = predictedLabel,
            style = AppStyles.body.copy(color = appColors().black),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = actualLabel,
            style = AppStyles.body.copy(color = appColors().black),
            modifier = Modifier.weight(1f),
        )
    }
}
