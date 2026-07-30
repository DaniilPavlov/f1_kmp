package com.example.f1_kmp.ui.screens.driver

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.f1_kmp.data.model.CareerRaceResult
import com.example.f1_kmp.ui.components.CareerListTile
import com.example.f1_kmp.ui.components.CareerRaceResultsSheet
import com.example.f1_kmp.ui.components.CareerStatsGrid
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.f1_kmp.data.model.CareerStats
import com.example.f1_kmp.domain.model.Circuit
import com.example.f1_kmp.domain.model.Constructor
import com.example.f1_kmp.domain.model.Driver
import com.example.f1_kmp.data.model.EspnDriverCardData
import com.example.f1_kmp.domain.AsyncValue
import com.example.f1_kmp.domain.LocaleController
import com.example.f1_kmp.ui.components.CareerDetailSections
import com.example.f1_kmp.ui.components.CareerInfoRow
import com.example.f1_kmp.ui.components.CountryFlag
import com.example.f1_kmp.ui.components.ErrorBody
import com.example.f1_kmp.ui.components.NewsArticleTile
import com.example.f1_kmp.ui.components.WikipediaLink
import com.example.f1_kmp.ui.components.displayValue
import com.example.f1_kmp.ui.components.shimmer.CareerScreenShimmer
import com.example.f1_kmp.ui.theme.AppDimens
import com.example.f1_kmp.ui.theme.AppStyles
import com.example.f1_kmp.ui.theme.F1GrayBg
import com.example.f1_kmp.util.DateUtils
import com.example.f1_kmp.util.RegisterShareAction
import com.example.f1_kmp.util.TrustedUrl
import com.example.f1_kmp.util.openUrl
import com.example.f1_kmp.util.rememberShareCareerAction
import com.example.f1_kmp.viewmodel.DriverDetailViewModel
import f1_kmp.composeapp.generated.resources.Res
import f1_kmp.composeapp.generated.resources.current_team
import f1_kmp.composeapp.generated.resources.date_of_birth
import f1_kmp.composeapp.generated.resources.driver_code
import f1_kmp.composeapp.generated.resources.driver_news_title
import f1_kmp.composeapp.generated.resources.driver_number
import f1_kmp.composeapp.generated.resources.driver_teams_title
import f1_kmp.composeapp.generated.resources.nationality
import f1_kmp.composeapp.generated.resources.wins
import com.example.f1_kmp.domain.stringResource
import kotlinx.datetime.number
import kotlinx.datetime.LocalDate

/** Экран карточки пилота: профиль, карьера, команды и ESPN-новости. */
@Composable
fun DriverDetailScreen(
    viewModel: DriverDetailViewModel,
    onConstructorClick: (Constructor) -> Unit,
    onCircuitClick: (Circuit) -> Unit,
) {
    val driver by viewModel.driver.collectAsState()
    val career by viewModel.careerStats.collectAsState()
    val espnCard by viewModel.espnCard.collectAsState()
    val error by viewModel.error.collectAsState()

    when {
        driver.isLoading || career.isLoading -> CareerScreenShimmer(modifier = Modifier.fillMaxSize())
        error != null && driver !is AsyncValue.Value -> ErrorBody(
            error?.title,
            error?.subtitle,
            onRetry = viewModel::loadAllData,
            modifier = Modifier.fillMaxSize(),
        )
        driver is AsyncValue.Value && career is AsyncValue.Value -> {
            DriverContent(
                driver = (driver as AsyncValue.Value).value,
                stats = (career as AsyncValue.Value).value,
                espnCard = espnCard,
                onConstructorClick = onConstructorClick,
                onCircuitClick = onCircuitClick,
                onWikipediaClick = { openUrl((driver as AsyncValue.Value).value.url) },
            )
        }
    }
}

@Composable
private fun DriverContent(
    driver: Driver,
    stats: CareerStats<Constructor>,
    espnCard: EspnDriverCardData,
    onConstructorClick: (Constructor) -> Unit,
    onCircuitClick: (Circuit) -> Unit,
    onWikipediaClick: () -> Unit,
) {
    RegisterShareAction(
        rememberShareCareerAction(
            title = driver.fullName,
            races = stats.races,
            wins = stats.wins,
            podiums = stats.podiums,
            poles = stats.poles,
        ),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AppDimens.horizontalPadding.dp, vertical = AppDimens.verticalPadding.dp),
    ) {
        espnCard.photoUrl?.let { url ->
            AsyncImage(
                model = TrustedUrl.preferHttps(url),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 2f)
                    .background(F1GrayBg),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.height(16.dp))
        }
        Text(driver.fullName, style = AppStyles.h1)
        Spacer(Modifier.height(16.dp))
        CareerInfoRow(stringResource(Res.string.driver_code), displayValue(driver.code))
        CareerInfoRow(stringResource(Res.string.driver_number), displayValue(driver.permanentNumber))
        CareerInfoRow(stringResource(Res.string.nationality)) {
            CountryFlag(countryOrNationality = driver.nationality, fontSize = 28.sp)
        }
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
        CareerDetailSections(
            stats = stats,
            relatedSectionTitle = stringResource(Res.string.driver_teams_title),
            relatedItemTitle = { it.name },
            onRelatedItemClick = onConstructorClick,
            onCircuitClick = onCircuitClick,
            relatedItemTrailing = { CountryFlag(countryOrNationality = it.nationality) },
        )
        if (espnCard.news.isNotEmpty()) {
            Spacer(Modifier.height(28.dp))
            Text(stringResource(Res.string.driver_news_title), style = AppStyles.h2)
            Spacer(Modifier.height(12.dp))
            espnCard.news.forEach { article ->
                NewsArticleTile(article)
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

private fun formatBirthDate(value: String): String = runCatching {
    val date = LocalDate.parse(value)
    val language = LocaleController.language.value
    "${date.day} ${DateUtils.monthName(date.month.number, language)} ${date.year}"
}.getOrElse { displayValue(value) }
