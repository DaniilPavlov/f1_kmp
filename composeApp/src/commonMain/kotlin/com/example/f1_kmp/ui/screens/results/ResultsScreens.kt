package com.example.f1_kmp.ui.screens.results

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.f1_kmp.domain.model.Driver
import com.example.f1_kmp.domain.model.PitStop
import com.example.f1_kmp.domain.model.QualifyingResult
import com.example.f1_kmp.domain.model.Race
import com.example.f1_kmp.domain.model.RaceResult
import com.example.f1_kmp.domain.AsyncValue
import com.example.f1_kmp.ui.components.BlackButton
import com.example.f1_kmp.ui.components.ErrorBody
import com.example.f1_kmp.ui.components.LoadingIndicator
import com.example.f1_kmp.ui.components.PitStopsTable
import com.example.f1_kmp.ui.components.QualifyingTable
import com.example.f1_kmp.ui.components.RacePickerField
import com.example.f1_kmp.ui.components.RaceResultsTable
import com.example.f1_kmp.ui.components.SeasonPickerField
import com.example.f1_kmp.ui.components.SectionHeader
import com.example.f1_kmp.ui.components.TableHeaderRow
import com.example.f1_kmp.ui.theme.AppDimens
import com.example.f1_kmp.ui.theme.AppStyles
import com.example.f1_kmp.ui.theme.F1Red
import com.example.f1_kmp.ui.theme.appColors
import com.example.f1_kmp.viewmodel.RaceInfoScreenViewModel
import com.example.f1_kmp.viewmodel.RaceInfoUiState
import com.example.f1_kmp.viewmodel.RaceSearchViewModel
import com.example.f1_kmp.viewmodel.ResultsViewModel
import f1_kmp.composeapp.generated.resources.Res
import f1_kmp.composeapp.generated.resources.best_lap
import f1_kmp.composeapp.generated.resources.choose_specific_race
import f1_kmp.composeapp.generated.resources.constructor
import f1_kmp.composeapp.generated.resources.driver
import f1_kmp.composeapp.generated.resources.finish_status_title
import f1_kmp.composeapp.generated.resources.h2h_constructors_title
import f1_kmp.composeapp.generated.resources.h2h_title
import f1_kmp.composeapp.generated.resources.hall_of_fame_title
import f1_kmp.composeapp.generated.resources.lap
import f1_kmp.composeapp.generated.resources.last_race
import f1_kmp.composeapp.generated.resources.pit_stops
import f1_kmp.composeapp.generated.resources.points
import f1_kmp.composeapp.generated.resources.qualifying
import f1_kmp.composeapp.generated.resources.race
import f1_kmp.composeapp.generated.resources.race_search_info
import f1_kmp.composeapp.generated.resources.race_time
import f1_kmp.composeapp.generated.resources.round_label
import f1_kmp.composeapp.generated.resources.search
import f1_kmp.composeapp.generated.resources.season
import f1_kmp.composeapp.generated.resources.season_label
import f1_kmp.composeapp.generated.resources.season_rewind_title
import f1_kmp.composeapp.generated.resources.select_race
import f1_kmp.composeapp.generated.resources.select_season
import f1_kmp.composeapp.generated.resources.select_season_first
import f1_kmp.composeapp.generated.resources.sprint
import f1_kmp.composeapp.generated.resources.stop_number
import f1_kmp.composeapp.generated.resources.stop_time
import f1_kmp.composeapp.generated.resources.time
import com.example.f1_kmp.ui.components.shimmer.LastRaceSectionShimmer
import com.example.f1_kmp.ui.components.shimmer.ListRowsShimmer
import com.example.f1_kmp.ui.components.shimmer.RaceInfoShimmer
import com.example.f1_kmp.ui.screens.results.WeekendScoreboardSection
import com.example.f1_kmp.util.RegisterShareAction
import com.example.f1_kmp.util.rememberShareRaceAction
import com.example.f1_kmp.domain.stringResource

/** Вкладка «Результаты»: scoreboard, последняя гонка и переходы. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    viewModel: ResultsViewModel,
    onSearchRace: () -> Unit,
    onHallOfFame: () -> Unit,
    onSeasonRewind: () -> Unit,
    onH2hDrivers: () -> Unit,
    onH2hConstructors: () -> Unit,
    onFinishStatus: () -> Unit,
    onRaceDetails: (Race) -> Unit,
    onDriverClick: (Driver) -> Unit,
) {
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
                .padding(bottom = AppDimens.verticalPadding.dp),
        ) {
            WeekendScoreboardSection(uiState.scoreboard)

            when (val state = uiState.lastRace) {
                is AsyncValue.Loading -> if (!uiState.isRefreshing) LastRaceSectionShimmer()
                is AsyncValue.Error -> ErrorBody(
                    state.message,
                    state.subtitle,
                    onRetry = viewModel::refreshAll,
                    modifier = Modifier.padding(horizontal = AppDimens.horizontalPadding.dp),
                )
                is AsyncValue.Value -> Column(
                    modifier = Modifier.padding(vertical = AppDimens.verticalPadding.dp),
                ) {
                    Column(modifier = Modifier.padding(horizontal = AppDimens.horizontalPadding.dp)) {
                        Text(stringResource(Res.string.last_race), style = AppStyles.h2)
                        Spacer(Modifier.height(AppDimens.verticalPadding.dp))
                        Text(state.value.raceName, style = AppStyles.h2)
                        Spacer(Modifier.height(AppDimens.verticalPadding.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                stringResource(Res.string.season_label, state.value.season),
                                style = AppStyles.h2,
                                modifier = Modifier.weight(1f),
                            )
                            Text(stringResource(Res.string.round_label, state.value.round), style = AppStyles.h2)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    RaceResultsTable(
                        race = state.value,
                        maxRows = 3,
                        onDetailsClick = { onRaceDetails(state.value) },
                        onDriverClick = onDriverClick,
                    )
                }
            }

            Spacer(Modifier.height(AppDimens.verticalPadding.dp))
            BoxedAction(title = stringResource(Res.string.choose_specific_race), onClick = onSearchRace)
            Spacer(Modifier.height(12.dp))
            BoxedAction(title = stringResource(Res.string.hall_of_fame_title), onClick = onHallOfFame)
            Spacer(Modifier.height(12.dp))
            BoxedAction(title = stringResource(Res.string.season_rewind_title), onClick = onSeasonRewind)
            Spacer(Modifier.height(12.dp))
            BoxedAction(title = stringResource(Res.string.h2h_title), onClick = onH2hDrivers)
            Spacer(Modifier.height(12.dp))
            BoxedAction(title = stringResource(Res.string.h2h_constructors_title), onClick = onH2hConstructors)
            Spacer(Modifier.height(12.dp))
            BoxedAction(title = stringResource(Res.string.finish_status_title), onClick = onFinishStatus)
        }
    }
}

/** Экран поиска гонки по сезону и этапу. */
@Composable
fun RaceSearchScreen(
    viewModel: RaceSearchViewModel,
    onRaceDetails: (Race) -> Unit,
    onDriverClick: (Driver) -> Unit,
) {
    val year by viewModel.year.collectAsState()
    val raceDisplay by viewModel.raceDisplay.collectAsState()
    val fieldsInputted by viewModel.fieldsInputted.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val searchedRace by viewModel.searchedRace.collectAsState()
    val dataLoaded by viewModel.dataLoaded.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AppDimens.horizontalPadding.dp),
    ) {
        Text(stringResource(Res.string.race_search_info), style = AppStyles.body, modifier = Modifier.padding(vertical = 16.dp))
        SeasonPickerField(
            value = year,
            label = stringResource(Res.string.season),
            hint = stringResource(Res.string.select_season),
            onSeasonSelected = viewModel::onYearChanged,
            loadSeasons = viewModel::loadSeasonYears,
        )
        Spacer(Modifier.height(12.dp))
        RacePickerField(
            displayValue = raceDisplay,
            seasonYear = year,
            label = stringResource(Res.string.race),
            hint = stringResource(Res.string.select_race),
            disabledHint = stringResource(Res.string.select_season_first),
            onRacePicked = { viewModel.onRacePicked(it.round, it.title) },
            loadRaces = viewModel::loadSeasonRaces,
        )
        Spacer(Modifier.height(16.dp))
        BlackButton(
            text = stringResource(Res.string.search),
            enabled = fieldsInputted,
            onClick = viewModel::loadRaceResults,
        )
        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, style = AppStyles.body, modifier = Modifier.padding(top = 16.dp))
        }
        if (!dataLoaded) {
            LoadingIndicator(Modifier.padding(top = 24.dp))
        }
        when (val state = searchedRace) {
            is AsyncValue.Value -> state.value?.let { race ->
                Spacer(Modifier.height(24.dp))
                Text(race.raceName, style = AppStyles.h2)
                RaceResultsTable(
                    race = race,
                    maxRows = 3,
                    onDetailsClick = { onRaceDetails(race) },
                    onDriverClick = onDriverClick,
                )
            }
            else -> Unit
        }
    }
}

/** Детали гонки: результаты, спринт, квалификация и пит-стопы. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaceInfoScreen(
    viewModel: RaceInfoScreenViewModel,
    onDriverClick: (Driver) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val race = uiState.race
    val error = uiState.error

    when {
        error != null && race.isError -> ErrorBody(
            error.title,
            error.subtitle,
            onRetry = viewModel::loadAllData,
            modifier = Modifier.fillMaxSize(),
        )
        !uiState.isRefreshing && race.isLoading -> RaceInfoShimmer(modifier = Modifier.fillMaxSize())
        race is AsyncValue.Value -> {
            val raceData = race.value
            RegisterShareAction(rememberShareRaceAction(raceData))
            RaceInfoScreenContent(
                uiState = uiState,
                race = raceData,
                onRefresh = viewModel::refreshAll,
                onDriverClick = onDriverClick,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun RaceInfoScreenContent(
    uiState: RaceInfoUiState,
    race: Race,
    onRefresh: () -> Unit,
    onDriverClick: (Driver) -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            raceInfoTitle(race)
            raceInfoRaceResults(race, onDriverClick)
            raceInfoSprintResults(race, uiState.sprint, onDriverClick)
            raceInfoQualifying(uiState.qualifying, onDriverClick)
            raceInfoPitStops(uiState.pitStops)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.raceInfoTitle(race: Race) {
    item {
        Column(
            modifier = Modifier.padding(horizontal = AppDimens.horizontalPadding.dp),
        ) {
            Spacer(Modifier.height(AppDimens.verticalPadding.dp))
            Text(race.raceName, style = AppStyles.h2)
            RowInfo(
                stringResource(Res.string.season_label, race.season),
                stringResource(Res.string.round_label, race.round),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.raceInfoRaceResults(
    race: Race,
    onDriverClick: (Driver) -> Unit,
) {
    stickyHeader {
        RaceInfoPinnedHeader(
            title = stringResource(Res.string.race),
            headerCells = listOf(
                stringResource(Res.string.driver),
                stringResource(Res.string.constructor),
                stringResource(Res.string.time),
                stringResource(Res.string.points),
                stringResource(Res.string.best_lap),
            ),
            weights = RaceResultsStickyWeights,
        )
    }
    item {
        RaceResultsTable(
            race = race,
            showHeader = false,
            timeHeaderRes = Res.string.time,
            onDriverClick = onDriverClick,
        )
        Spacer(Modifier.height(AppDimens.verticalPadding.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.raceInfoSprintResults(
    race: Race,
    sprint: AsyncValue<List<RaceResult>>,
    onDriverClick: (Driver) -> Unit,
) {
    if (sprint !is AsyncValue.Value || sprint.value.isEmpty()) return
    stickyHeader {
        RaceInfoPinnedHeader(
            title = stringResource(Res.string.sprint),
            headerCells = listOf(
                stringResource(Res.string.driver),
                stringResource(Res.string.constructor),
                stringResource(Res.string.time),
                stringResource(Res.string.points),
                stringResource(Res.string.best_lap),
            ),
            weights = RaceResultsStickyWeights,
        )
    }
    item {
        RaceResultsTable(
            race = race.copy(results = sprint.value),
            showHeader = false,
            timeHeaderRes = Res.string.time,
            onDriverClick = onDriverClick,
        )
        Spacer(Modifier.height(AppDimens.verticalPadding.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.raceInfoQualifying(
    qualifying: AsyncValue<List<QualifyingResult>>,
    onDriverClick: (Driver) -> Unit,
) {
    stickyHeader {
        RaceInfoPinnedHeader(
            title = stringResource(Res.string.qualifying),
            headerCells = listOf(
                stringResource(Res.string.driver),
                stringResource(Res.string.constructor),
                "Q1",
                "Q2",
                "Q3",
            ),
        )
    }
    item {
        when (qualifying) {
            is AsyncValue.Loading -> ListRowsShimmer(rowCount = 8)
            is AsyncValue.Error -> Text(qualifying.message, style = AppStyles.body)
            is AsyncValue.Value -> QualifyingTable(
                results = qualifying.value,
                showHeader = false,
                onDriverClick = onDriverClick,
            )
        }
        Spacer(Modifier.height(AppDimens.verticalPadding.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.raceInfoPitStops(
    pitStops: AsyncValue<List<PitStop>>,
) {
    stickyHeader {
        RaceInfoPinnedHeader(
            title = stringResource(Res.string.pit_stops),
            headerCells = listOf(
                stringResource(Res.string.driver),
                stringResource(Res.string.lap),
                stringResource(Res.string.stop_number),
                stringResource(Res.string.stop_time),
                stringResource(Res.string.race_time),
            ),
        )
    }
    item {
        when (pitStops) {
            is AsyncValue.Loading -> ListRowsShimmer(rowCount = 6)
            is AsyncValue.Error -> Text(pitStops.message, style = AppStyles.body)
            is AsyncValue.Value -> PitStopsTable(stops = pitStops.value, showHeader = false)
        }
        Spacer(Modifier.height(32.dp))
    }
}

private val RaceResultsStickyWeights = listOf(1.15f, 1.35f, 1.1f, 0.55f, 0.9f)

@Composable
private fun RaceInfoPinnedHeader(
    title: String,
    headerCells: List<String>,
    weights: List<Float>? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(appColors().white),
    ) {
        SectionHeader(title)
        TableHeaderRow(cells = headerCells, weights = weights)
    }
}

@Composable
private fun RowInfo(left: String, right: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppDimens.verticalPadding.dp),
    ) {
        Text(left, style = AppStyles.h2, modifier = Modifier.weight(1f))
        Text(right, style = AppStyles.h2)
    }
}

@Composable
private fun BoxedAction(title: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.horizontalPadding.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, F1Red, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Text(title, style = AppStyles.h3)
    }
}
