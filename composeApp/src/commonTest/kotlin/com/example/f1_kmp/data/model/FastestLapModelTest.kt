package com.example.f1_kmp.data.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json

class FastestLapModelTest {
    @Test
    fun decode_sprintFastestLap_withoutRank() {
        val json = Json { ignoreUnknownKeys = true }
        val model = json.decodeFromString(
            FastestLapModel.serializer(),
            """{"lap":"17","Time":{"time":"1:31.773"}}""",
        )
        assertEquals("", model.rank)
        assertEquals("17", model.lap)
        assertEquals("1:31.773", model.time.time)
    }
}
