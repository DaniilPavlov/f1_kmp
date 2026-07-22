package com.example.f1_kmp.data.api

import com.example.f1_kmp.data.model.EspnAthleteResponseDto
import com.example.f1_kmp.data.model.EspnNewsResponseDto
import com.example.f1_kmp.data.model.EspnOverviewResponseDto
import com.example.f1_kmp.data.model.EspnScoreboardResponseDto
import com.example.f1_kmp.data.model.EspnSearchResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * ESPN Site API (F1 news / scoreboard / athlete media).
 *
 * Docs: https://github.com/pseudo-r/Public-ESPN-API/blob/main/docs/sports/racing.md
 * Base URL: https://site.api.espn.com/
 *
 * Абсолютные URL — для search / athlete / overview (другие хосты).
 */
class EspnApiService(private val client: HttpClient) {

    /** F1 news feed. Без [limit] ESPN возвращает ~6 статей; максимум 50. */
    suspend fun getNews(limit: Int = NEWS_LIMIT, team: String? = null): EspnNewsResponseDto =
        client.get("apis/site/v2/sports/racing/f1/news") {
            parameter("limit", limit)
            team?.let { parameter("team", it) }
        }.body()

    /** Текущий / ближайший F1 уик-энд (scoreboard). */
    suspend fun getScoreboard(): EspnScoreboardResponseDto =
        client.get("apis/site/v2/sports/racing/f1/scoreboard").body()

    /** Поиск пилотов (site.web.api). */
    suspend fun searchPlayers(
        region: String = "us",
        lang: String = "en",
        query: String,
        limit: Int = 10,
        type: String = "player",
    ): EspnSearchResponseDto =
        client.get("https://site.web.api.espn.com/apis/common/v3/search") {
            parameter("region", region)
            parameter("lang", lang)
            parameter("query", query)
            parameter("limit", limit)
            parameter("type", type)
        }.body()

    /** Карточка пилота (core API) — headshot. */
    suspend fun getAthlete(espnAthleteId: String): EspnAthleteResponseDto =
        client.get("https://sports.core.api.espn.com/v2/sports/racing/leagues/f1/athletes/$espnAthleteId").body()

    /** Overview пилота — новости. */
    suspend fun getAthleteOverview(
        espnAthleteId: String,
        region: String = "us",
        lang: String = "en",
    ): EspnOverviewResponseDto =
        client.get("https://site.web.api.espn.com/apis/common/v3/sports/racing/f1/athletes/$espnAthleteId/overview") {
            parameter("region", region)
            parameter("lang", lang)
        }.body()

    companion object {
        const val NEWS_LIMIT = 50
        const val BASE_URL = "https://site.api.espn.com/"
        const val NEWS_CACHE_TTL_MS = 15 * 60 * 1000L
        const val SCOREBOARD_CACHE_TTL_MS = 5 * 60 * 1000L
        const val SCOREBOARD_POLL_INTERVAL_MS = 30_000L
        const val DRIVER_PHOTO_BASE = "https://a.espncdn.com/combiner/i?img="
    }
}
