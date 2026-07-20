package com.example.f1_kmp.ui.screens.results

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.f1_kmp.data.model.DriverModel
import com.example.f1_kmp.data.model.RaceModel
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
import com.example.f1_kmp.ui.theme.AppDimens
import com.example.f1_kmp.ui.theme.AppStyles
import com.example.f1_kmp.ui.theme.F1Red
import com.example.f1_kmp.viewmodel.RaceInfoScreenViewModel
import com.example.f1_kmp.viewmodel.RaceSearchViewModel
import com.example.f1_kmp.viewmodel.ResultsViewModel
import f1_kmp.composeapp.generated.resources.Res
import f1_kmp.composeapp.generated.resources.choose_specific_race
import f1_kmp.composeapp.generated.resources.last_race
import f1_kmp.composeapp.generated.resources.pit_stops
import f1_kmp.composeapp.generated.resources.qualifying
import f1_kmp.composeapp.generated.resources.race
import f1_kmp.composeapp.generated.resources.race_search_info
import f1_kmp.composeapp.generated.resources.round_label
import f1_kmp.composeapp.generated.resources.search
import f1_kmp.composeapp.generated.resources.season
import f1_kmp.composeapp.generated.resources.season_label
import f1_kmp.composeapp.generated.resources.select_race
import f1_kmp.composeapp.generated.resources.select_season
import f1_kmp.composeapp.generated.resources.select_season_first
import f1_kmp.composeapp.generated.resources.sprint
import com.example.f1_kmp.domain.stringResource

@Composable
fun ResultsScreen(
    viewModel: ResultsViewModel,
    onSearchRace: () -> Unit,
    onRaceDetails: (RaceModel) -> Unit,
    onDriverClick: (DriverModel) -> Unit,
) {
    val lastRace by viewModel.lastRace.collectAsState()

    when (val state = lastRace) {
        is AsyncValue.Loading -> LoadingIndicator(Modifier.fillMaxSize())
        is AsyncValue.Error -> ErrorBody(state.message, state.subtitle, onRetry = viewModel::loadAllData, modifier = Modifier.fillMaxSize())
        is AsyncValue.Value -> Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = AppDimens.verticalPadding.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = AppDimens.horizontalPadding.dp)) {
                Text(stringResource(Res.string.last_race), style = AppStyles.h2)
                Spacer(Modifier.height(AppDimens.verticalPadding.dp))
                Text(state.value.raceName, style = AppStyles.h2)
                Spacer(Modifier.height(AppDimens.verticalPadding.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.season_label, state.value.season), style = AppStyles.h2, modifier = Modifier.weight(1f))
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
            Spacer(Modifier.height(AppDimens.verticalPadding.dp))
            BoxedAction(title = stringResource(Res.string.choose_specific_race), onClick = onSearchRace)
        }
    }
}

@Composable
fun RaceSearchScreen(
    viewModel: RaceSearchViewModel,
    onRaceDetails: (RaceModel) -> Unit,
    onDriverClick: (DriverModel) -> Unit,
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

@Composable
fun RaceInfoScreen(
    viewModel: RaceInfoScreenViewModel,
    onDriverClick: (DriverModel) -> Unit,
) {
    val race by viewModel.race.collectAsState()
    val qualifying by viewModel.qualifying.collectAsState()
    val pitStops by viewModel.pitStops.collectAsState()
    val sprint by viewModel.sprint.collectAsState()
    val error by viewModel.error.collectAsState()

    when {
        error != null && race.isError -> ErrorBody(
            error?.title,
            error?.subtitle,
            onRetry = viewModel::loadAllData,
            modifier = Modifier.fillMaxSize(),
        )
        race.isLoading -> LoadingIndicator(Modifier.fillMaxSize())
        race is AsyncValue.Value -> {
            val raceData = (race as AsyncValue.Value).value
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AppDimens.horizontalPadding.dp),
            ) {
                Spacer(Modifier.height(AppDimens.verticalPadding.dp))
                Text(raceData.raceName, style = AppStyles.h2)
                RowInfo(
                    stringResource(Res.string.season_label, raceData.season),
                    stringResource(Res.string.round_label, raceData.round),
                )
                SectionHeader(stringResource(Res.string.race))
                RaceResultsTable(
                    race = raceData,
                    showHeader = false,
                    onDriverClick = onDriverClick,
                )
                Spacer(Modifier.height(AppDimens.verticalPadding.dp))
                if (sprint is AsyncValue.Value && (sprint as AsyncValue.Value).value.isNotEmpty()) {
                    SectionHeader(stringResource(Res.string.sprint))
                    RaceResultsTable(
                        race = raceData.copy(results = (sprint as AsyncValue.Value).value),
                        showHeader = false,
                        onDriverClick = onDriverClick,
                    )
                    Spacer(Modifier.height(AppDimens.verticalPadding.dp))
                }
                SectionHeader(stringResource(Res.string.qualifying))
                when (val q = qualifying) {
                    is AsyncValue.Loading -> LoadingIndicator(Modifier.padding(vertical = 16.dp))
                    is AsyncValue.Error -> Text(q.message, style = AppStyles.body)
                    is AsyncValue.Value -> QualifyingTable(q.value, onDriverClick = onDriverClick)
                }
                Spacer(Modifier.height(AppDimens.verticalPadding.dp))
                SectionHeader(stringResource(Res.string.pit_stops))
                when (val p = pitStops) {
                    is AsyncValue.Loading -> LoadingIndicator(Modifier.padding(vertical = 16.dp))
                    is AsyncValue.Error -> Text(p.message, style = AppStyles.body)
                    is AsyncValue.Value -> PitStopsTable(p.value)
                }
                Spacer(Modifier.height(32.dp))
            }
        }
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
