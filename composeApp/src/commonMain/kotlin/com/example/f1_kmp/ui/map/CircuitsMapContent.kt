package com.example.f1_kmp.ui.map

import androidx.compose.runtime.Composable
import com.example.f1_kmp.data.model.CircuitModel

/**
 * Карта трасс: expect/actual из‑за разных SDK карт.
 * Android — OSMDroid с кластерами; iOS — заглушка (список доступен на обеих платформах).
 */
@Composable
expect fun CircuitsMapContent(
    circuits: List<CircuitModel>,
    onCircuitClick: (String) -> Unit,
)
