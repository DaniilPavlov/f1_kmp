package com.example.f1_kmp.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.f1_kmp.data.model.CareerRaceResult
import com.example.f1_kmp.data.model.CareerStats
import com.example.f1_kmp.data.model.CircuitModel
import com.example.f1_kmp.ui.theme.AppStyles
import f1_kmp.composeapp.generated.resources.Res
import f1_kmp.composeapp.generated.resources.career_stat_podiums
import f1_kmp.composeapp.generated.resources.career_stat_poles
import f1_kmp.composeapp.generated.resources.career_title
import f1_kmp.composeapp.generated.resources.wins
import com.example.f1_kmp.domain.stringResource

/**
 * Карьерная статистика, bottom sheet с результатами гонок и список связанных сущностей
 * (команды для пилота / пилоты для конструктора).
 */
@Composable
fun <T> CareerDetailSections(
    stats: CareerStats<T>,
    relatedSectionTitle: String,
    relatedItemTitle: (T) -> String,
    onRelatedItemClick: (T) -> Unit,
    onCircuitClick: (CircuitModel) -> Unit,
    relatedItemTrailing: @Composable (T) -> Unit = {},
) {
    var sheetTitle by remember { mutableStateOf<String?>(null) }
    var sheetRaces by remember { mutableStateOf<List<CareerRaceResult>>(emptyList()) }
    var sheetShowPosition by remember { mutableStateOf(false) }
    val winsTitle = stringResource(Res.string.wins)
    val podiumsTitle = stringResource(Res.string.career_stat_podiums)
    val polesTitle = stringResource(Res.string.career_stat_poles)

    Spacer(Modifier.height(28.dp))
    Text(stringResource(Res.string.career_title), style = AppStyles.h2)
    Spacer(Modifier.height(16.dp))
    CareerStatsGrid(
        races = stats.races,
        wins = stats.wins,
        podiums = stats.podiums,
        poles = stats.poles,
        onWinsTap = {
            sheetTitle = winsTitle
            sheetRaces = stats.winRaces
            sheetShowPosition = false
        },
        onPodiumsTap = {
            sheetTitle = podiumsTitle
            sheetRaces = stats.podiumRaces
            sheetShowPosition = true
        },
        onPolesTap = {
            sheetTitle = polesTitle
            sheetRaces = stats.poleRaces
            sheetShowPosition = false
        },
    )
    Spacer(Modifier.height(28.dp))
    Text(relatedSectionTitle, style = AppStyles.h2)
    Spacer(Modifier.height(12.dp))
    stats.related.forEach { item ->
        CareerListTile(
            title = relatedItemTitle(item),
            subtitle = "",
            onClick = { onRelatedItemClick(item) },
            trailing = { relatedItemTrailing(item) },
        )
    }

    sheetTitle?.let { title ->
        CareerRaceResultsSheet(
            title = title,
            races = sheetRaces,
            showPosition = sheetShowPosition,
            onDismiss = { sheetTitle = null },
            onCircuitClick = onCircuitClick,
        )
    }
}
