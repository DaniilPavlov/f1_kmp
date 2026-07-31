package com.example.f1_kmp.data.local

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CacheJsonMapperTest {
    private val mapper = CacheJsonMapper(Json { ignoreUnknownKeys = true })

    @Serializable
    private data class Sample(val id: String, val n: Int)

    @Test
    fun roundTrip_objectAndList() {
        val json = mapper.toJson(Sample("a", 1))
        assertEquals(Sample("a", 1), mapper.fromJson<Sample>(json))

        val listJson = mapper.toJsonList(listOf(Sample("a", 1), Sample("b", 2)))
        assertEquals(2, mapper.fromJsonList<Sample>(listJson)!!.size)
    }

    @Test
    fun fromJson_invalid_returnsNull() {
        assertNull(mapper.fromJson<Sample>("{not-json"))
        assertNull(mapper.fromJsonList<Sample>("{not-json"))
    }

    @Test
    fun cacheKeys_historical() {
        assertEquals("historical_standings_2024", CacheKeys.historicalStandings("2024"))
    }
}
