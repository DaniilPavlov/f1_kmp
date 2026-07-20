package com.example.f1_kmp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.f1_kmp.data.model.ConstructorStandingsModel
import com.example.f1_kmp.data.model.DriverModel
import com.example.f1_kmp.data.model.DriverStandingsModel
import com.example.f1_kmp.data.model.PitStopModel
import com.example.f1_kmp.data.model.QualifyingResultModel
import com.example.f1_kmp.data.model.RaceModel
import com.example.f1_kmp.data.model.RaceResultModel
import com.example.f1_kmp.ui.theme.AppDimens
import com.example.f1_kmp.ui.theme.AppStyles
import com.example.f1_kmp.ui.theme.F1Red
import com.example.f1_kmp.ui.theme.F1White
import com.example.f1_kmp.util.DateUtils
import f1_kmp.composeapp.generated.resources.Res
import f1_kmp.composeapp.generated.resources.constructor
import f1_kmp.composeapp.generated.resources.country
import f1_kmp.composeapp.generated.resources.detailed_info
import f1_kmp.composeapp.generated.resources.driver
import f1_kmp.composeapp.generated.resources.duration
import f1_kmp.composeapp.generated.resources.fastest_suffix
import f1_kmp.composeapp.generated.resources.lap
import f1_kmp.composeapp.generated.resources.laps
import f1_kmp.composeapp.generated.resources.points
import f1_kmp.composeapp.generated.resources.stop
import f1_kmp.composeapp.generated.resources.time
import f1_kmp.composeapp.generated.resources.wins
import kotlinx.datetime.number
import com.example.f1_kmp.domain.stringResource

/** Таблица чемпионата пилотов (вкладки Главная и Зал славы). */
@Composable
fun TournamentDriversTable(
    drivers: List<DriverStandingsModel>,
    onDriverClick: ((DriverModel) -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TableHeaderRow(
            listOf(
                "#",
                stringResource(Res.string.driver),
                stringResource(Res.string.country),
                stringResource(Res.string.points),
                stringResource(Res.string.wins),
                stringResource(Res.string.constructor),
            ),
        )
        drivers.forEachIndexed { index, item ->
            TableDataRow(
                cells = listOf(
                    "${index + 1}",
                    item.driver.fullName,
                    item.driver.nationality,
                    item.points,
                    item.wins,
                    item.constructors.firstOrNull()?.name.orEmpty(),
                ),
                index = index,
                onClick = onDriverClick?.let { { it(item.driver) } },
            )
        }
    }
}

/** Таблица чемпионата конструкторов. */
@Composable
fun TournamentConstructorsTable(
    constructors: List<ConstructorStandingsModel>,
    onConstructorClick: ((com.example.f1_kmp.data.model.ConstructorModel) -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TableHeaderRow(
            listOf(
                "#",
                stringResource(Res.string.constructor),
                stringResource(Res.string.country),
                stringResource(Res.string.points),
                stringResource(Res.string.wins),
            ),
        )
        constructors.forEachIndexed { index, item ->
            TableDataRow(
                cells = listOf(
                    "${index + 1}",
                    item.constructor.name,
                    item.constructor.nationality,
                    item.points,
                    item.wins,
                ),
                index = index,
                onClick = onConstructorClick?.let { { it(item.constructor) } },
            )
        }
    }
}

/**
 * Таблица результатов гонки.
 *
 * @param maxRows ограничить число строк (на главной результатов — топ-3)
 * @param showHeader показывать ли красную шапку с колонками
 * @param onDetailsClick если задан — рисуем строку «Подробная информация» внизу таблицы
 */
@Composable
fun RaceResultsTable(
    race: RaceModel,
    maxRows: Int? = null,
    showHeader: Boolean = true,
    onDetailsClick: (() -> Unit)? = null,
    onDriverClick: ((DriverModel) -> Unit)? = null,
) {
    val results = race.results.orEmpty()
    val rows = maxRows?.let { results.take(it) } ?: results
    val fastest = race.fastestLapTime

    Column(modifier = Modifier.fillMaxWidth()) {
        if (showHeader) {
            TableHeaderRow(
                listOf(
                    "#",
                    stringResource(Res.string.driver),
                    stringResource(Res.string.constructor),
                    stringResource(Res.string.points),
                    stringResource(Res.string.laps),
                    stringResource(Res.string.time),
                ),
            )
        }
        rows.forEachIndexed { index, result ->
            val isFastest = result.fastestLap?.time?.time == fastest && fastest != "999999"
            RaceResultRow(result, index + 1, isFastest, onDriverClick)
        }
        if (onDetailsClick != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(com.example.f1_kmp.ui.theme.F1GrayBg)
                    .clickable(onClick = onDetailsClick)
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(Res.string.detailed_info), style = AppStyles.caption)
                Icon(Icons.Default.Flag, contentDescription = null)
            }
        }
    }
}

@Composable
private fun RaceResultRow(
    result: RaceResultModel,
    position: Int,
    isFastest: Boolean,
    onDriverClick: ((DriverModel) -> Unit)? = null,
) {
    val fastestSuffix = stringResource(Res.string.fastest_suffix)
    val timeText = buildString {
        append(result.time?.time ?: result.status)
        if (isFastest) append(fastestSuffix)
    }
    TableDataRow(
        cells = listOf(
            "$position",
            result.driver.fullName,
            result.constructor.name,
            result.points,
            result.laps,
            timeText,
        ),
        index = position,
        highlight = isFastest,
        onClick = onDriverClick?.let { { it(result.driver) } },
    )
}

/**
 * Таблица квалификации Q1/Q2/Q3.
 * Прочерк «-» ставится, если пилот не прошёл в следующий сегмент (позиция 16+ / 11+).
 */
@Composable
fun QualifyingTable(
    results: List<QualifyingResultModel>,
    onDriverClick: ((DriverModel) -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TableHeaderRow(
            listOf(
                "#",
                stringResource(Res.string.driver),
                stringResource(Res.string.constructor),
                "Q1",
                "Q2",
                "Q3",
            ),
        )
        results.forEachIndexed { index, item ->
            val position = item.position.toIntOrNull() ?: (index + 1)
            TableDataRow(
                cells = listOf(
                    "${index + 1}",
                    item.driver.fullName,
                    item.constructor.name,
                    item.Q1 ?: if (position >= 16) "-" else "",
                    item.Q2 ?: if (position >= 11) "-" else "",
                    item.Q3 ?: "-",
                ),
                index = index,
                onClick = onDriverClick?.let { { it(item.driver) } },
            )
        }
    }
}

/** Таблица пит-стопов. В [PitStopModel.driverId] уже подставлено ФИО из Repository. */
@Composable
fun PitStopsTable(stops: List<PitStopModel>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TableHeaderRow(
            listOf(
                stringResource(Res.string.driver),
                stringResource(Res.string.lap),
                stringResource(Res.string.stop),
                stringResource(Res.string.time),
                stringResource(Res.string.duration),
            ),
        )
        stops.forEachIndexed { index, stop ->
            TableDataRow(
                cells = listOf(stop.driverId, stop.lap, stop.stop, stop.time, stop.duration),
                index = index,
            )
        }
    }
}

/** Красный заголовок секции на экране детальной гонки. */
@Composable
fun SectionHeader(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(F1Red)
            .padding(vertical = 10.dp, horizontal = AppDimens.horizontalPadding.dp),
    ) {
        Text(text = title, style = AppStyles.body.copy(color = F1White))
    }
}

/**
 * Карточка одной сессии в календаре (практика, квалификация, гонка).
 * Время конвертируется из UTC в локальное через [DateUtils.toLocalDateTime].
 */
@Composable
fun ScheduleSessionCard(
    title: String,
    date: String,
    time: String?,
) {
    val localDateTime = DateUtils.toLocalDateTime(date, time)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = AppDimens.horizontalPadding.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, F1Red, RoundedCornerShape(20.dp))
            .padding(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(title, style = AppStyles.h3)
                if (localDateTime != null) {
                    Text(
                        "${localDateTime.dayOfMonth} ${DateUtils.monthName(localDateTime.month.number)} ${localDateTime.year}",
                        style = AppStyles.body,
                        modifier = Modifier.padding(vertical = 5.dp),
                    )
                    Text(DateUtils.formatHourMinute(localDateTime), style = AppStyles.body)
                }
            }
            Icon(Icons.Default.Flag, contentDescription = null, tint = F1Red)
        }
    }
}
