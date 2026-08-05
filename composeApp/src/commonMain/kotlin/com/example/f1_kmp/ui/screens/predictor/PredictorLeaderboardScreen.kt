package com.example.f1_kmp.ui.screens.predictor

import f1_kmp.composeapp.generated.resources.Res
import f1_kmp.composeapp.generated.resources.predictor_leaderboard_empty
import f1_kmp.composeapp.generated.resources.predictor_leaderboard_join
import f1_kmp.composeapp.generated.resources.predictor_leaderboard_join_hint
import f1_kmp.composeapp.generated.resources.predictor_leaderboard_leave
import f1_kmp.composeapp.generated.resources.predictor_leaderboard_list_title
import f1_kmp.composeapp.generated.resources.predictor_leaderboard_opt_in_label
import f1_kmp.composeapp.generated.resources.predictor_leaderboard_your_rank
import f1_kmp.composeapp.generated.resources.predictor_nickname_hint
import f1_kmp.composeapp.generated.resources.predictor_nickname_label
import f1_kmp.composeapp.generated.resources.predictor_nickname_save
import com.example.f1_kmp.domain.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.f1_kmp.domain.predictor.PredictorLeaderboardEntry
import com.example.f1_kmp.ui.components.BlackButton
import com.example.f1_kmp.ui.components.ErrorBody
import com.example.f1_kmp.ui.components.LoadingIndicator
import com.example.f1_kmp.ui.theme.AppDimens
import com.example.f1_kmp.ui.theme.AppStyles
import com.example.f1_kmp.ui.theme.F1Red
import com.example.f1_kmp.ui.theme.appColors
import com.example.f1_kmp.viewmodel.PredictorLeaderboardViewModel

/** UI лидерборда предиктора: форма join + таблица. */
@Suppress("LongMethod")
@Composable
fun PredictorLeaderboardScreen(
    viewModel: PredictorLeaderboardViewModel,
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
            uiState.error != null && uiState.isLoading -> ErrorBody(
                title = uiState.error?.title,
                subtitle = uiState.error?.subtitle,
                onRetry = viewModel::load,
                modifier = Modifier.fillMaxSize(),
            )
            uiState.isLoading -> LoadingIndicator(Modifier.fillMaxSize())
            else -> {
                val formErrorRes = predictorFormErrorRes(uiState.formErrorKey)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = AppDimens.horizontalPadding.dp)
                        .padding(vertical = AppDimens.verticalPadding.dp),
                ) {
                    if (viewModel.showJoinForm(uiState)) {
                        Text(
                            text = stringResource(Res.string.predictor_leaderboard_join_hint),
                            style = AppStyles.body.copy(color = appColors().black),
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = uiState.nicknameDraft,
                            onValueChange = viewModel::setNicknameDraft,
                            label = { Text(stringResource(Res.string.predictor_nickname_label)) },
                            placeholder = { Text(stringResource(Res.string.predictor_nickname_hint)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isSaving,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = uiState.optInAgreed,
                                onCheckedChange = viewModel::setOptInAgreed,
                                enabled = !uiState.isSaving,
                                colors = CheckboxDefaults.colors(checkedColor = F1Red),
                            )
                            Text(
                                text = stringResource(Res.string.predictor_leaderboard_opt_in_label),
                                style = AppStyles.body.copy(color = appColors().black),
                            )
                        }
                        formErrorRes?.let {
                            Text(
                                text = stringResource(it),
                                style = AppStyles.caption.copy(color = F1Red),
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        BlackButton(
                            text = stringResource(Res.string.predictor_leaderboard_join),
                            onClick = viewModel::join,
                            enabled = !uiState.isSaving,
                        )
                    } else {
                        viewModel.myEntry(uiState)?.let { mine ->
                            Text(
                                text = stringResource(
                                    Res.string.predictor_leaderboard_your_rank,
                                    mine.rank,
                                    mine.totalPoints,
                                ),
                                style = AppStyles.h3.copy(color = appColors().black),
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                        OutlinedTextField(
                            value = uiState.nicknameDraft,
                            onValueChange = viewModel::setNicknameDraft,
                            label = { Text(stringResource(Res.string.predictor_nickname_label)) },
                            placeholder = { Text(stringResource(Res.string.predictor_nickname_hint)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isSaving,
                        )
                        Spacer(Modifier.height(8.dp))
                        formErrorRes?.let {
                            Text(
                                text = stringResource(it),
                                style = AppStyles.caption.copy(color = F1Red),
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        BlackButton(
                            text = stringResource(Res.string.predictor_nickname_save),
                            onClick = viewModel::saveNickname,
                            enabled = !uiState.isSaving,
                        )
                        Spacer(Modifier.height(8.dp))
                        BlackButton(
                            text = stringResource(Res.string.predictor_leaderboard_leave),
                            onClick = { viewModel.leave() },
                            enabled = !uiState.isSaving,
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = stringResource(Res.string.predictor_leaderboard_list_title),
                        style = AppStyles.h3.copy(color = appColors().black),
                    )
                    Spacer(Modifier.height(8.dp))
                    if (uiState.entries.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.predictor_leaderboard_empty),
                            style = AppStyles.body.copy(color = appColors().black),
                        )
                    } else {
                        val myUid = viewModel.myEntry(uiState)?.uid
                        uiState.entries.forEach { entry ->
                            LeaderboardRow(entry = entry, highlight = entry.uid == myUid)
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(
    entry: PredictorLeaderboardEntry,
    highlight: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (highlight) F1Red.copy(alpha = 0.18f) else appColors().grayBg)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "#${entry.rank}",
            style = AppStyles.body.copy(color = appColors().black),
            modifier = Modifier.padding(end = 12.dp),
        )
        Text(
            text = entry.nickname,
            style = AppStyles.body.copy(color = appColors().black),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${entry.totalPoints}",
            style = AppStyles.body.copy(color = appColors().black),
        )
    }
}
