package com.example.f1_kmp.ui.screens.predictor

import f1_kmp.composeapp.generated.resources.Res
import f1_kmp.composeapp.generated.resources.predictor_history_empty
import f1_kmp.composeapp.generated.resources.predictor_pending_points
import f1_kmp.composeapp.generated.resources.predictor_season_points
import f1_kmp.composeapp.generated.resources.predictor_weekend_points
import com.example.f1_kmp.domain.stringResource
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.f1_kmp.domain.predictor.PredictorWeekendPrediction
import com.example.f1_kmp.ui.components.ErrorBody
import com.example.f1_kmp.ui.components.LoadingIndicator
import com.example.f1_kmp.ui.theme.AppDimens
import com.example.f1_kmp.ui.theme.AppStyles
import com.example.f1_kmp.ui.theme.F1Red
import com.example.f1_kmp.ui.theme.appColors
import com.example.f1_kmp.viewmodel.PredictorSeasonHistoryViewModel

/** Список уикендов сезона с очками. */
@Composable
fun PredictorSeasonHistoryScreen(
    viewModel: PredictorSeasonHistoryViewModel,
    onGoSignIn: () -> Unit,
    onBlocked: () -> Unit,
    onOpenWeekend: (season: String, weekend: PredictorWeekendPrediction) -> Unit,
) {
    PredictorAuthGate(
        asTabRoot = false,
        onGoSignIn = onGoSignIn,
        onGoProfile = onBlocked,
        onBlockedNested = onBlocked,
    ) {
        val uiState by viewModel.uiState.collectAsState()
        when {
            uiState.error != null && uiState.season == null -> ErrorBody(
                title = uiState.error?.title,
                subtitle = uiState.error?.subtitle,
                onRetry = viewModel::load,
                modifier = Modifier.fillMaxSize(),
            )
            uiState.isLoading -> LoadingIndicator(Modifier.fillMaxSize())
            else -> {
                val season = uiState.season
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = AppDimens.horizontalPadding.dp)
                        .padding(vertical = AppDimens.verticalPadding.dp),
                ) {
                    Text(
                        text = stringResource(
                            Res.string.predictor_season_points,
                            viewModel.year,
                            season?.totalPoints ?: 0,
                        ),
                        style = AppStyles.h3.copy(color = appColors().black),
                    )
                    Spacer(Modifier.height(16.dp))
                    val weekends = season?.weekendsSorted?.asReversed().orEmpty()
                    if (weekends.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.predictor_history_empty),
                            style = AppStyles.body.copy(color = appColors().black),
                        )
                    } else {
                        weekends.forEach { weekend ->
                            SeasonHistoryTile(
                                weekend = weekend,
                                onClick = { onOpenWeekend(viewModel.year, weekend) },
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeasonHistoryTile(
    weekend: PredictorWeekendPrediction,
    onClick: () -> Unit,
) {
    val pending = stringResource(Res.string.predictor_pending_points)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, F1Red, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Text(
            text = weekend.raceName.ifBlank { "R${weekend.round}" },
            style = AppStyles.body.copy(color = appColors().black),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(
                Res.string.predictor_weekend_points,
                weekend.qualiPoints?.toString() ?: pending,
                weekend.racePoints?.toString() ?: pending,
                weekend.totalPoints,
            ),
            style = AppStyles.caption.copy(color = appColors().black),
        )
    }
}
