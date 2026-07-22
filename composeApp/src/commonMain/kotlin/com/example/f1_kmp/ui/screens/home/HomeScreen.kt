package com.example.f1_kmp.ui.screens.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.f1_kmp.data.model.ConstructorModel
import com.example.f1_kmp.data.model.DriverModel
import com.example.f1_kmp.domain.AsyncValue
import com.example.f1_kmp.ui.components.CustomSwitcher
import com.example.f1_kmp.ui.components.ErrorBody
import com.example.f1_kmp.ui.components.shimmer.TournamentTablesShimmer
import com.example.f1_kmp.ui.components.TournamentConstructorsTable
import com.example.f1_kmp.ui.components.TournamentDriversTable
import com.example.f1_kmp.ui.theme.AppDimens
import com.example.f1_kmp.ui.theme.AppStyles
import com.example.f1_kmp.viewmodel.HomeViewModel
import f1_kmp.composeapp.generated.resources.Res
import f1_kmp.composeapp.generated.resources.constructors
import f1_kmp.composeapp.generated.resources.drivers
import f1_kmp.composeapp.generated.resources.home_standings_title
import f1_kmp.composeapp.generated.resources.round_label
import f1_kmp.composeapp.generated.resources.season_label
import com.example.f1_kmp.domain.stringResource

/** Главная вкладка: текущие standings пилотов и конструкторов. */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onDriverClick: (DriverModel) -> Unit,
    onConstructorClick: (ConstructorModel) -> Unit,
) {
    val drivers by viewModel.drivers.collectAsState()
    val constructors by viewModel.constructors.collectAsState()
    val season by viewModel.season.collectAsState()
    val round by viewModel.round.collectAsState()
    val activeTable by viewModel.activeTable.collectAsState()
    val error by viewModel.error.collectAsState()

    when {
        drivers.isLoading || constructors.isLoading -> TournamentTablesShimmer(modifier = Modifier.fillMaxSize())
        error != null -> ErrorBody(error?.title, error?.subtitle, onRetry = viewModel::loadAllData, modifier = Modifier.fillMaxSize())
        drivers is AsyncValue.Value && constructors is AsyncValue.Value -> {
            val driversList = (drivers as AsyncValue.Value).value
            val constructorsList = (constructors as AsyncValue.Value).value
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = AppDimens.verticalPadding.dp),
            ) {
                Column(modifier = Modifier.padding(horizontal = AppDimens.horizontalPadding.dp)) {
                    Text(stringResource(Res.string.home_standings_title), style = AppStyles.h1)
                    Spacer(Modifier.height(32.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(Res.string.season_label, season), style = AppStyles.h2, modifier = Modifier.weight(1f))
                        Text(stringResource(Res.string.round_label, round), style = AppStyles.h2)
                    }
                }
                Spacer(Modifier.height(32.dp))
                CustomSwitcher(
                    stringResource(Res.string.drivers),
                    stringResource(Res.string.constructors),
                    activeTable,
                    viewModel::changeActiveTable,
                )
                Spacer(Modifier.height(8.dp))
                if (activeTable == 0) {
                    TournamentDriversTable(driversList, onDriverClick = onDriverClick)
                } else {
                    TournamentConstructorsTable(constructorsList, onConstructorClick = onConstructorClick)
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
