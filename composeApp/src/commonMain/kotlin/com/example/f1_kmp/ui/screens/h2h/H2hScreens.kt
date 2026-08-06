package com.example.f1_kmp.ui.screens.h2h

import androidx.compose.foundation.border
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.f1_kmp.domain.AsyncValue
import com.example.f1_kmp.domain.stringResource
import com.example.f1_kmp.ui.components.BlackButton
import com.example.f1_kmp.ui.components.ConstructorPickerField
import com.example.f1_kmp.ui.components.DriverPickerField
import com.example.f1_kmp.ui.components.ErrorBody
import com.example.f1_kmp.ui.components.H2hCompareTable
import com.example.f1_kmp.ui.components.H2hFilterToggle
import com.example.f1_kmp.ui.components.SeasonPickerField
import com.example.f1_kmp.ui.components.shimmer.H2hCompareShimmer
import com.example.f1_kmp.ui.theme.AppDimens
import com.example.f1_kmp.ui.theme.AppStyles
import com.example.f1_kmp.ui.theme.appColors
import com.example.f1_kmp.viewmodel.H2hConstructorsViewModel
import com.example.f1_kmp.viewmodel.H2hDriversViewModel
import f1_kmp.composeapp.generated.resources.Res
import f1_kmp.composeapp.generated.resources.career_title
import f1_kmp.composeapp.generated.resources.h2h_all_constructors
import f1_kmp.composeapp.generated.resources.h2h_all_drivers
import f1_kmp.composeapp.generated.resources.h2h_compare
import f1_kmp.composeapp.generated.resources.h2h_constructor_a
import f1_kmp.composeapp.generated.resources.h2h_constructor_b
import f1_kmp.composeapp.generated.resources.h2h_constructors_filter
import f1_kmp.composeapp.generated.resources.h2h_constructors_subtitle
import f1_kmp.composeapp.generated.resources.h2h_current_constructors
import f1_kmp.composeapp.generated.resources.h2h_current_drivers
import f1_kmp.composeapp.generated.resources.h2h_current_season
import f1_kmp.composeapp.generated.resources.h2h_driver_a
import f1_kmp.composeapp.generated.resources.h2h_driver_b
import f1_kmp.composeapp.generated.resources.h2h_drivers_filter
import f1_kmp.composeapp.generated.resources.h2h_filters_title
import f1_kmp.composeapp.generated.resources.h2h_mode_constructors
import f1_kmp.composeapp.generated.resources.h2h_mode_drivers
import f1_kmp.composeapp.generated.resources.h2h_period_filter
import f1_kmp.composeapp.generated.resources.h2h_pick_year
import f1_kmp.composeapp.generated.resources.h2h_season_filter
import f1_kmp.composeapp.generated.resources.h2h_subtitle
import f1_kmp.composeapp.generated.resources.season
import f1_kmp.composeapp.generated.resources.season_label
import f1_kmp.composeapp.generated.resources.select_season

/** Режим единого H2H-экрана: пилоты или конструкторы. */
enum class H2hMode { Drivers, Constructors }

/**
 * Единый экран head-to-head с переключателем Drivers / Constructors.
 * Маршруты H2hDrivers / H2hConstructors хостят этот экран с разным [initialMode].
 */
@Composable
fun H2hScreen(
    driversViewModel: H2hDriversViewModel,
    constructorsViewModel: H2hConstructorsViewModel,
    initialMode: H2hMode = H2hMode.Drivers,
) {
    var mode by rememberSaveable { mutableStateOf(initialMode.name) }
    val isDrivers = mode == H2hMode.Drivers.name

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AppDimens.horizontalPadding.dp, vertical = AppDimens.verticalPadding.dp),
    ) {
        H2hFilterToggle(
            label = "",
            firstTitle = stringResource(Res.string.h2h_mode_drivers),
            secondTitle = stringResource(Res.string.h2h_mode_constructors),
            activeIndex = if (isDrivers) 0 else 1,
            onChanged = { mode = if (it == 0) H2hMode.Drivers.name else H2hMode.Constructors.name },
        )
        Spacer(Modifier.height(16.dp))
        if (isDrivers) {
            H2hDriversContent(viewModel = driversViewModel)
        } else {
            H2hConstructorsContent(viewModel = constructorsViewModel)
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun H2hDriversContent(viewModel: H2hDriversViewModel) {
    val scopeMode by viewModel.scopeMode.collectAsState()
    val useCurrentSeason by viewModel.useCurrentSeason.collectAsState()
    val currentOnly by viewModel.currentOnly.collectAsState()
    val latestSeason by viewModel.latestSeason.collectAsState()
    val pickedSeason by viewModel.pickedSeason.collectAsState()
    val driverA by viewModel.driverA.collectAsState()
    val driverB by viewModel.driverB.collectAsState()
    val comparison by viewModel.comparison.collectAsState()

    val isSeasonScope = scopeMode == 1
    val showYearPicker = isSeasonScope && !useCurrentSeason

    Text(stringResource(Res.string.h2h_subtitle), style = AppStyles.body)
    Spacer(Modifier.height(16.dp))
    H2hFiltersPanel(
        scopeMode = scopeMode,
        useCurrentSeason = useCurrentSeason,
        currentOnly = currentOnly,
        latestSeason = latestSeason,
        pickedSeason = pickedSeason,
        isSeasonScope = isSeasonScope,
        showYearPicker = showYearPicker,
        entityFilterLabel = stringResource(Res.string.h2h_drivers_filter),
        entityCurrentTitle = stringResource(Res.string.h2h_current_drivers),
        entityAllTitle = stringResource(Res.string.h2h_all_drivers),
        onScopeModeChanged = viewModel::setScopeMode,
        onUseCurrentSeasonChanged = viewModel::setUseCurrentSeason,
        onSeasonPicked = viewModel::onSeasonPicked,
        loadSeasons = viewModel::loadSeasonYears,
        onCurrentOnlyChanged = viewModel::setCurrentOnly,
    )
    Spacer(Modifier.height(20.dp))
    DriverPickerField(
        label = stringResource(Res.string.h2h_driver_a),
        driver = driverA,
        enableSearch = !currentOnly,
        onChanged = viewModel::setDriverA,
        loadDrivers = viewModel::loadDriversForPicker,
    )
    Spacer(Modifier.height(12.dp))
    DriverPickerField(
        label = stringResource(Res.string.h2h_driver_b),
        driver = driverB,
        enableSearch = !currentOnly,
        onChanged = viewModel::setDriverB,
        loadDrivers = viewModel::loadDriversForPicker,
    )
    Spacer(Modifier.height(20.dp))
    BlackButton(
        text = stringResource(Res.string.h2h_compare),
        enabled = driverA != null &&
            driverB != null &&
            driverA!!.driverId != driverB!!.driverId &&
            (!isSeasonScope || (if (useCurrentSeason) latestSeason.isNotEmpty() else pickedSeason.length == 4)) &&
            comparison !is AsyncValue.Loading,
        onClick = viewModel::compare,
    )
    Spacer(Modifier.height(24.dp))
    when (val state = comparison) {
        is AsyncValue.Loading -> H2hCompareShimmer()
        is AsyncValue.Error -> ErrorBody(
            state.message,
            state.subtitle,
            onRetry = viewModel::compare,
        )
        is AsyncValue.Value -> state.value?.let { result ->
            H2hCompareTable(
                nameA = result.driverA.fullName,
                nameB = result.driverB.fullName,
                statsA = result.statsA,
                statsB = result.statsB,
                season = result.season,
                timeline = result.timeline,
                constructorIdA = result.constructorIdA,
                constructorIdB = result.constructorIdB,
            )
        }
    }
}

@Composable
private fun H2hConstructorsContent(viewModel: H2hConstructorsViewModel) {
    val scopeMode by viewModel.scopeMode.collectAsState()
    val useCurrentSeason by viewModel.useCurrentSeason.collectAsState()
    val currentOnly by viewModel.currentOnly.collectAsState()
    val latestSeason by viewModel.latestSeason.collectAsState()
    val pickedSeason by viewModel.pickedSeason.collectAsState()
    val constructorA by viewModel.constructorA.collectAsState()
    val constructorB by viewModel.constructorB.collectAsState()
    val comparison by viewModel.comparison.collectAsState()

    val isSeasonScope = scopeMode == 1
    val showYearPicker = isSeasonScope && !useCurrentSeason

    Text(stringResource(Res.string.h2h_constructors_subtitle), style = AppStyles.body)
    Spacer(Modifier.height(16.dp))
    H2hFiltersPanel(
        scopeMode = scopeMode,
        useCurrentSeason = useCurrentSeason,
        currentOnly = currentOnly,
        latestSeason = latestSeason,
        pickedSeason = pickedSeason,
        isSeasonScope = isSeasonScope,
        showYearPicker = showYearPicker,
        entityFilterLabel = stringResource(Res.string.h2h_constructors_filter),
        entityCurrentTitle = stringResource(Res.string.h2h_current_constructors),
        entityAllTitle = stringResource(Res.string.h2h_all_constructors),
        onScopeModeChanged = viewModel::setScopeMode,
        onUseCurrentSeasonChanged = viewModel::setUseCurrentSeason,
        onSeasonPicked = viewModel::onSeasonPicked,
        loadSeasons = viewModel::loadSeasonYears,
        onCurrentOnlyChanged = viewModel::setCurrentOnly,
    )
    Spacer(Modifier.height(20.dp))
    ConstructorPickerField(
        label = stringResource(Res.string.h2h_constructor_a),
        constructor = constructorA,
        enableSearch = !currentOnly,
        onChanged = viewModel::setConstructorA,
        loadConstructors = viewModel::loadConstructorsForPicker,
    )
    Spacer(Modifier.height(12.dp))
    ConstructorPickerField(
        label = stringResource(Res.string.h2h_constructor_b),
        constructor = constructorB,
        enableSearch = !currentOnly,
        onChanged = viewModel::setConstructorB,
        loadConstructors = viewModel::loadConstructorsForPicker,
    )
    Spacer(Modifier.height(20.dp))
    BlackButton(
        text = stringResource(Res.string.h2h_compare),
        enabled = constructorA != null &&
            constructorB != null &&
            constructorA!!.constructorId != constructorB!!.constructorId &&
            (!isSeasonScope || (if (useCurrentSeason) latestSeason.isNotEmpty() else pickedSeason.length == 4)) &&
            comparison !is AsyncValue.Loading,
        onClick = viewModel::compare,
    )
    Spacer(Modifier.height(24.dp))
    when (val state = comparison) {
        is AsyncValue.Loading -> H2hCompareShimmer()
        is AsyncValue.Error -> ErrorBody(
            state.message,
            state.subtitle,
            onRetry = viewModel::compare,
        )
        is AsyncValue.Value -> state.value?.let { result ->
            H2hCompareTable(
                nameA = result.constructorA.name,
                nameB = result.constructorB.name,
                statsA = result.statsA,
                statsB = result.statsB,
                season = result.season,
                timeline = result.timeline,
                constructorIdA = result.constructorIdA,
                constructorIdB = result.constructorIdB,
            )
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun H2hFiltersPanel(
    scopeMode: Int,
    useCurrentSeason: Boolean,
    currentOnly: Boolean,
    latestSeason: String,
    pickedSeason: String,
    isSeasonScope: Boolean,
    showYearPicker: Boolean,
    entityFilterLabel: String,
    entityCurrentTitle: String,
    entityAllTitle: String,
    onScopeModeChanged: (Int) -> Unit,
    onUseCurrentSeasonChanged: (Boolean) -> Unit,
    onSeasonPicked: (String) -> Unit,
    loadSeasons: suspend () -> Result<List<String>>,
    onCurrentOnlyChanged: (Boolean) -> Unit,
) {
    val colors = appColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.strokeGray, RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        Text(stringResource(Res.string.h2h_filters_title), style = AppStyles.body)
        Spacer(Modifier.height(16.dp))
        H2hFilterToggle(
            label = stringResource(Res.string.h2h_period_filter),
            firstTitle = stringResource(Res.string.career_title),
            secondTitle = stringResource(Res.string.season),
            activeIndex = scopeMode,
            onChanged = onScopeModeChanged,
        )
        if (isSeasonScope) {
            Spacer(Modifier.height(14.dp))
            H2hFilterToggle(
                label = stringResource(Res.string.h2h_season_filter),
                firstTitle = stringResource(Res.string.h2h_current_season),
                secondTitle = stringResource(Res.string.h2h_pick_year),
                activeIndex = if (useCurrentSeason) 0 else 1,
                onChanged = { onUseCurrentSeasonChanged(it == 0) },
            )
            if (showYearPicker) {
                Spacer(Modifier.height(12.dp))
                SeasonPickerField(
                    value = pickedSeason,
                    label = stringResource(Res.string.season),
                    hint = stringResource(Res.string.select_season),
                    onSeasonSelected = onSeasonPicked,
                    loadSeasons = loadSeasons,
                )
            } else if (latestSeason.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(Res.string.season_label, latestSeason),
                    style = AppStyles.caption.copy(color = colors.textGray),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        H2hFilterToggle(
            label = entityFilterLabel,
            firstTitle = entityCurrentTitle,
            secondTitle = entityAllTitle,
            activeIndex = if (currentOnly) 0 else 1,
            onChanged = { onCurrentOnlyChanged(it == 0) },
        )
    }
}
