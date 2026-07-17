package com.example.f1_kmp.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.f1_kmp.data.model.CircuitModel
import com.example.f1_kmp.ui.theme.AppStyles

/**
 * iOS-заглушка карты: OSMDroid только на Android.
 * Список трасс доступен на вкладке «Списком».
 */
@Composable
actual fun CircuitsMapContent(
    circuits: List<CircuitModel>,
    onCircuitClick: (String) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Карта трасс в разработке. Используйте вкладку «Списком».",
            style = AppStyles.body,
            textAlign = TextAlign.Center,
        )
    }
}
