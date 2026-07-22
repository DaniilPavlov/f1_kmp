package com.example.f1_kmp.data.circuits

import f1_kmp.composeapp.generated.resources.Res
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** Загружает curated stats трасс из `files/data/circuit_stats.json` один раз. */
class CircuitStatsRepository(
    private val json: Json,
) {
    private val mutex = Mutex()
    private var cache: Map<String, CircuitStats>? = null

    /** Stats по Jolpica [circuitId], либо `null`. */
    suspend fun of(circuitId: String): CircuitStats? {
        val map = ensureLoaded()
        return map[circuitId.trim().lowercase()]
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun ensureLoaded(): Map<String, CircuitStats> {
        cache?.let { return it }
        return mutex.withLock {
            cache?.let { return it }
            val text = Res.readBytes("files/data/circuit_stats.json").decodeToString()
            val decoded = json.decodeFromString<Map<String, CircuitStats>>(text)
            decoded.mapKeys { it.key.lowercase() }.also { cache = it }
        }
    }
}
