package com.example.f1_kmp.ui.screens.schedule

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.f1_kmp.data.circuits.CircuitLayoutAssets
import com.example.f1_kmp.domain.model.RaceSession
import com.example.f1_kmp.domain.model.Race
import com.example.f1_kmp.domain.LocaleController
import com.example.f1_kmp.ui.components.BlackButton
import com.example.f1_kmp.ui.components.ScheduleSessionCard
import com.example.f1_kmp.ui.components.circuits.CircuitLayoutImage
import com.example.f1_kmp.ui.theme.AppDimens
import com.example.f1_kmp.ui.theme.AppStyles
import com.example.f1_kmp.ui.theme.F1Red
import com.example.f1_kmp.ui.theme.F1TextGray
import com.example.f1_kmp.util.CountdownParts
import com.example.f1_kmp.util.DateUtils
import com.example.f1_kmp.util.RaceDateTimeHelper
import f1_kmp.composeapp.generated.resources.Res
import f1_kmp.composeapp.generated.resources.first_practice
import f1_kmp.composeapp.generated.resources.qualifying
import f1_kmp.composeapp.generated.resources.race
import f1_kmp.composeapp.generated.resources.schedule_countdown_title
import f1_kmp.composeapp.generated.resources.schedule_days
import f1_kmp.composeapp.generated.resources.schedule_hours
import f1_kmp.composeapp.generated.resources.schedule_minutes
import f1_kmp.composeapp.generated.resources.schedule_round
import f1_kmp.composeapp.generated.resources.schedule_view_sessions
import f1_kmp.composeapp.generated.resources.second_practice
import f1_kmp.composeapp.generated.resources.sprint
import f1_kmp.composeapp.generated.resources.sprint_qualifying
import f1_kmp.composeapp.generated.resources.third_practice
import com.example.f1_kmp.domain.stringResource
import kotlinx.coroutines.delay
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource

/** Крупная карточка ближайшей гонки со схемой и countdown. */
@Composable
fun ScheduleRaceFeaturedCard(
    race: Race,
    onViewSessions: () -> Unit,
    modifier: Modifier = Modifier,
    showCountdown: Boolean = true,
) {
    val language by LocaleController.language.collectAsState()
    var now by remember { mutableStateOf(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        }
    }
    val countdown = remember(now, race) {
        CountdownParts.until(RaceDateTimeHelper.countdownTarget(race), now)
    }

    val start = RaceDateTimeHelper.weekendStart(race)
    val end = RaceDateTimeHelper.raceLocal(race)
    val dateRange = if (start.date == end.date) {
        DateUtils.formatLongDate(end.date, language)
    } else {
        "${start.day} – ${DateUtils.formatLongDate(end.date, language)}"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, F1Red, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(Res.string.schedule_round, race.round),
            style = AppStyles.caption.copy(color = F1TextGray),
        )
        Spacer(Modifier.height(4.dp))
        Text(race.raceName, style = AppStyles.h2)
        Spacer(Modifier.height(4.dp))
        Text(race.circuit.circuitName, style = AppStyles.body.copy(color = F1Red))
        Spacer(Modifier.height(4.dp))
        Text(dateRange, style = AppStyles.body)
        if (CircuitLayoutAssets.hasLayout(race.circuit.circuitId)) {
            Spacer(Modifier.height(12.dp))
            CircuitLayoutImage(
                circuitId = race.circuit.circuitId,
                height = 140.dp,
                padding = 0.dp,
            )
        }
        if (showCountdown) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(Res.string.schedule_countdown_title),
                style = AppStyles.caption.copy(color = F1TextGray),
            )
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                CountdownCell(
                    value = countdown.days.toString(),
                    label = stringResource(Res.string.schedule_days),
                    modifier = Modifier.weight(1f),
                )
                CountdownCell(
                    value = countdown.hours.toString(),
                    label = stringResource(Res.string.schedule_hours),
                    modifier = Modifier.weight(1f),
                )
                CountdownCell(
                    value = countdown.minutes.toString(),
                    label = stringResource(Res.string.schedule_minutes),
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        BlackButton(
            text = stringResource(Res.string.schedule_view_sessions),
            onClick = onViewSessions,
        )
    }
}

@Composable
private fun CountdownCell(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = AppStyles.h3)
        Spacer(Modifier.height(2.dp))
        Text(label, style = AppStyles.caption.copy(color = F1TextGray))
    }
}

/** Нижний лист со всеми сессиями выбранного ГП. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleRaceSessionsSheet(
    race: Race,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sessions = remember(race) { raceWeekendSessions(race) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(race.raceName, style = AppStyles.h2)
            Spacer(Modifier.height(4.dp))
            Text(race.circuit.circuitName, style = AppStyles.body)
            Spacer(Modifier.height(AppDimens.verticalPadding.dp))
            sessions.forEach { (titleRes, date) ->
                ScheduleSessionCard(stringResource(titleRes), date.date, date.time)
            }
        }
    }
}

/** Все сессии уикенда для bottom sheet / «View sessions». */
fun raceWeekendSessions(race: Race): List<Pair<StringResource, RaceSession>> = buildList {
    race.firstPractice?.let { add(Res.string.first_practice to it) }
    race.secondPractice?.let { add(Res.string.second_practice to it) }
    race.thirdPractice?.let { add(Res.string.third_practice to it) }
    race.sprintQualifying?.let { add(Res.string.sprint_qualifying to it) }
    race.sprint?.let { add(Res.string.sprint to it) }
    race.qualifying?.let { add(Res.string.qualifying to it) }
    add(Res.string.race to RaceSession(date = race.date, time = race.time))
}
