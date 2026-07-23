package com.example.f1_kmp.ui.map

import androidx.compose.runtime.Composable
import com.example.f1_kmp.domain.model.Circuit

/**
 * Карта трасс: expect/actual из‑за разных SDK карт.
 *
 * GoF Structural Bridge — абстракция [CircuitsMapContent] отделена от реализации:
 * Android — OSMDroid с кластерами; iOS — MapKit pins (без кластеров).
 */
@Composable
expect fun CircuitsMapContent(
    circuits: List<Circuit>,
    onCircuitClick: (String) -> Unit,
)
