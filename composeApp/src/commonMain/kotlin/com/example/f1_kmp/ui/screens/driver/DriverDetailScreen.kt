package com.example.f1_kmp.ui.screens.driver

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import com.example.f1_kmp.data.model.CareerStats
import com.example.f1_kmp.data.model.ConstructorModel
import com.example.f1_kmp.data.model.DriverModel
import com.example.f1_kmp.domain.AsyncValue
import com.example.f1_kmp.domain.LocaleController
import com.example.f1_kmp.ui.components.CareerInfoRow
import com.example.f1_kmp.ui.components.CareerListTile
import com.example.f1_kmp.ui.components.CareerStatsGrid
import com.example.f1_kmp.ui.components.ErrorBody
import com.example.f1_kmp.ui.components.LoadingIndicator
import com.example.f1_kmp.ui.components.WikipediaLink
import com.example.f1_kmp.ui.components.displayValue
import com.example.f1_kmp.ui.theme.AppDimens
import com.example.f1_kmp.ui.theme.AppStyles
import com.example.f1_kmp.util.DateUtils
import com.example.f1_kmp.util.openUrl
import com.example.f1_kmp.viewmodel.DriverDetailViewModel
import f1_kmp.composeapp.generated.resources.Res
import f1_kmp.composeapp.generated.resources.career_title
import f1_kmp.composeapp.generated.resources.current_team
import f1_kmp.composeapp.generated.resources.date_of_birth
import f1_kmp.composeapp.generated.resources.driver_code
import f1_kmp.composeapp.generated.resources.driver_number
import f1_kmp.composeapp.generated.resources.driver_teams_title
import f1_kmp.composeapp.generated.resources.nationality
import com.example.f1_kmp.domain.stringResource
import kotlinx.datetime.LocalDate

@Composable
fun DriverDetailScreen(
    viewModel: DriverDetailViewModel,
    onConstructorClick: (ConstructorModel) -> Unit,
) {
    val driver by viewModel.driver.collectAsState()
    val career by viewModel.careerStats.collectAsState()
    val error by viewModel.error.collectAsState()

    when {
        driver.isLoading || career.isLoading -> LoadingIndicator(Modifier.fillMaxSize())
        error != null -> ErrorBody(error?.title, error?.subtitle, onRetry = viewModel::loadAllData, modifier = Modifier.fillMaxSize())
        driver is AsyncValue.Value && career is AsyncValue.Value -> {
            DriverContent(
                driver = (driver as AsyncValue.Value).value,
                stats = (career as AsyncValue.Value).value,
                onConstructorClick = onConstructorClick,
                onWikipediaClick = { openUrl((driver as AsyncValue.Value).value.url) },
            )
        }
    }
}

@Composable
private fun DriverContent(
    driver: DriverModel,
    stats: CareerStats<ConstructorModel>,
    onConstructorClick: (ConstructorModel) -> Unit,
    onWikipediaClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AppDimens.horizontalPadding.dp, vertical = AppDimens.verticalPadding.dp),
    ) {
        Text(driver.fullName, style = AppStyles.h1)
        Spacer(Modifier.height(16.dp))
        CareerInfoRow(stringResource(Res.string.driver_code), displayValue(driver.code))
        CareerInfoRow(stringResource(Res.string.driver_number), displayValue(driver.permanentNumber))
        CareerInfoRow(stringResource(Res.string.nationality), displayValue(driver.nationality))
        CareerInfoRow(stringResource(Res.string.date_of_birth), formatBirthDate(driver.dateOfBirth))
        if (stats.current.isNotEmpty()) {
            CareerInfoRow(
                stringResource(Res.string.current_team),
                stats.current.joinToString(", ") { it.name },
            )
        }
        if (driver.url.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            WikipediaLink(onWikipediaClick)
        }
        Spacer(Modifier.height(28.dp))
        Text(stringResource(Res.string.career_title), style = AppStyles.h2)
        Spacer(Modifier.height(16.dp))
        CareerStatsGrid(stats.races, stats.wins, stats.podiums, stats.poles)
        Spacer(Modifier.height(28.dp))
        Text(stringResource(Res.string.driver_teams_title), style = AppStyles.h2)
        Spacer(Modifier.height(12.dp))
        stats.related.forEach { constructor ->
            CareerListTile(
                title = constructor.name,
                subtitle = displayValue(constructor.nationality),
                onClick = { onConstructorClick(constructor) },
            )
        }
    }
}

private fun formatBirthDate(value: String): String = runCatching {
    val date = LocalDate.parse(value)
    val language = LocaleController.language.value
    "${date.dayOfMonth} ${DateUtils.monthName(date.monthNumber, language)} ${date.year}"
}.getOrElse { displayValue(value) }
