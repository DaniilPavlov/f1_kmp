package com.example.f1_kmp.ui.screens.halloffame

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.f1_kmp.data.model.DriverModel
import com.example.f1_kmp.domain.AsyncValue
import com.example.f1_kmp.ui.components.BlackButton
import com.example.f1_kmp.ui.components.CustomSwitcher
import com.example.f1_kmp.ui.components.DriverInfoBottomSheet
import com.example.f1_kmp.ui.components.ErrorBody
import com.example.f1_kmp.ui.components.LoadingIndicator
import com.example.f1_kmp.ui.components.TournamentConstructorsTable
import com.example.f1_kmp.ui.components.TournamentDriversTable
import com.example.f1_kmp.ui.theme.AppDimens
import com.example.f1_kmp.ui.theme.AppStyles
import com.example.f1_kmp.viewmodel.HallOfFameViewModel
import f1_kmp.composeapp.generated.resources.Res
import f1_kmp.composeapp.generated.resources.constructors
import f1_kmp.composeapp.generated.resources.drivers
import f1_kmp.composeapp.generated.resources.hall_of_fame_title
import f1_kmp.composeapp.generated.resources.search
import f1_kmp.composeapp.generated.resources.season
import f1_kmp.composeapp.generated.resources.year_hint
import com.example.f1_kmp.domain.stringResource

/**
 * Экран «Зал славы» — итоговые таблицы за выбранный год.
 */
@Composable
fun HallOfFameScreen(viewModel: HallOfFameViewModel) {
    val selectedDriver = remember { mutableStateOf<DriverModel?>(null) }
    val drivers by viewModel.drivers.collectAsState()
    val constructors by viewModel.constructors.collectAsState()
    val year by viewModel.year.collectAsState()
    val fieldsInputted by viewModel.fieldsInputted.collectAsState()
    val activeTable by viewModel.activeTable.collectAsState()
    val error by viewModel.error.collectAsState()

    when {
        drivers.isLoading || constructors.isLoading -> LoadingIndicator(Modifier.fillMaxSize())
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
                    Text(stringResource(Res.string.hall_of_fame_title), style = AppStyles.h1)
                    Spacer(Modifier.height(16.dp))
                    Row {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(Res.string.season), style = AppStyles.caption)
                            OutlinedTextField(
                                value = year,
                                onValueChange = viewModel::onYearChanged,
                                placeholder = { Text(stringResource(Res.string.year_hint)) },
                                modifier = Modifier.fillMaxSize(),
                                singleLine = true,
                            )
                        }
                        Spacer(Modifier.height(0.dp).weight(0.05f))
                        Column(modifier = Modifier.weight(1f).padding(start = 20.dp)) {
                            Spacer(Modifier.height(18.dp))
                            BlackButton(
                                text = stringResource(Res.string.search),
                                enabled = fieldsInputted,
                                onClick = viewModel::loadAllData,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                CustomSwitcher(
                    stringResource(Res.string.drivers),
                    stringResource(Res.string.constructors),
                    activeTable,
                    viewModel::changeActiveTable,
                )
                Spacer(Modifier.height(8.dp))
                if (activeTable == 0) {
                    TournamentDriversTable(driversList) { selectedDriver.value = it }
                } else {
                    TournamentConstructorsTable(constructorsList)
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
    selectedDriver.value?.let { DriverInfoBottomSheet(it) { selectedDriver.value = null } }
}
