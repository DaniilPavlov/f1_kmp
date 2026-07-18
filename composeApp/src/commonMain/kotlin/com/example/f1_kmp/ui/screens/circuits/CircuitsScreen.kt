package com.example.f1_kmp.ui.screens.circuits

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.f1_kmp.data.model.CircuitModel
import com.example.f1_kmp.domain.AsyncValue
import com.example.f1_kmp.ui.components.CustomSwitcher
import com.example.f1_kmp.ui.components.ErrorBody
import com.example.f1_kmp.ui.components.LinkText
import com.example.f1_kmp.ui.components.LoadingIndicator
import com.example.f1_kmp.ui.map.CircuitsMapContent
import com.example.f1_kmp.ui.theme.AppDimens
import com.example.f1_kmp.ui.theme.AppStyles
import com.example.f1_kmp.ui.theme.F1Red
import com.example.f1_kmp.util.openUrl
import com.example.f1_kmp.viewmodel.CircuitDetailViewModel
import com.example.f1_kmp.viewmodel.CircuitsViewModel
import f1_kmp.composeapp.generated.resources.Res
import f1_kmp.composeapp.generated.resources.as_list
import f1_kmp.composeapp.generated.resources.city_label
import f1_kmp.composeapp.generated.resources.country_label
import f1_kmp.composeapp.generated.resources.on_map
import f1_kmp.composeapp.generated.resources.read_on_wikipedia
import com.example.f1_kmp.domain.stringResource

/**
 * Экран «Трассы»: переключатель закреплён сверху, контент — в [Box] с [Modifier.weight].
 */
@Composable
fun CircuitsScreen(
    viewModel: CircuitsViewModel,
    onCircuitClick: (String) -> Unit,
) {
    val circuits by viewModel.circuits.collectAsState()
    val activePage by viewModel.activePage.collectAsState()

    when (val state = circuits) {
        is AsyncValue.Loading -> LoadingIndicator(Modifier.fillMaxSize())
        is AsyncValue.Error -> ErrorBody(state.message, state.subtitle, onRetry = viewModel::loadCircuits, modifier = Modifier.fillMaxSize())
        is AsyncValue.Value -> Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.height(12.dp))
            CustomSwitcher(
                stringResource(Res.string.on_map),
                stringResource(Res.string.as_list),
                activePage,
                viewModel::changeActivePage,
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                when (activePage) {
                    0 -> CircuitsMapContent(state.value, onCircuitClick)
                    else -> CircuitsList(state.value, onCircuitClick)
                }
            }
        }
    }
}

@Composable
private fun CircuitsList(circuits: List<CircuitModel>, onCircuitClick: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = AppDimens.horizontalPadding.dp)) {
        items(circuits) { circuit ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, F1Red, RoundedCornerShape(20.dp))
                    .clickable { onCircuitClick(circuit.circuitId) }
                    .padding(16.dp),
            ) {
                Text(circuit.circuitName, style = AppStyles.h3)
            }
        }
    }
}

/** Карточка трассы: название, ссылка на Wikipedia, страна и город. */
@Composable
fun CircuitDetailScreen(viewModel: CircuitDetailViewModel) {
    val circuitState by viewModel.circuit.collectAsState()

    when (val state = circuitState) {
        is AsyncValue.Loading -> LoadingIndicator(Modifier.fillMaxSize())
        is AsyncValue.Error -> ErrorBody(state.message, state.subtitle, onRetry = viewModel::loadCircuit, modifier = Modifier.fillMaxSize())
        is AsyncValue.Value -> Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppDimens.horizontalPadding.dp, vertical = AppDimens.verticalPadding.dp),
        ) {
            Text(state.value.circuitName, style = AppStyles.h1)
            Spacer(Modifier.height(20.dp))
            LinkText(stringResource(Res.string.read_on_wikipedia)) { openUrl(state.value.url) }
            Spacer(Modifier.height(20.dp))
            Text(stringResource(Res.string.country_label, state.value.location.country), style = AppStyles.h3)
            Spacer(Modifier.height(10.dp))
            Text(stringResource(Res.string.city_label, state.value.location.locality), style = AppStyles.h3)
        }
    }
}
