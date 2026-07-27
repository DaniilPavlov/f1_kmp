package com.example.f1_kmp.util

import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url

/**
 * Проверка и нормализация внешних ссылок перед открытием в браузере.
 *
 * Парсинг через [io.ktor.http.Url], чтобы логика работала в commonMain / unit-тестах.
 */
object TrustedUrl {
    private const val TAG = "TrustedUrl"

    private val allowedHostSuffixes = listOf(
        "wikipedia.org",
        "wikimedia.org",
        "espn.com",
        "espn.co.uk",
        "github.com",
        "formula1.com",
        "jolpi.ca",
        "ergast.com",
    )

    /**
     * Возвращает нормализованный https-URL или `null`, если ссылка не доверенная.
     */
    fun parse(rawUrl: String): String? {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return null

        val url = runCatching { Url(trimmed) }.getOrNull() ?: return null
        val host = url.host.takeIf { it.isNotEmpty() } ?: return null
        val httpsUrl = when (url.protocol.name.lowercase()) {
            "https" -> url
            "http" -> URLBuilder(url).apply {
                this.protocol = URLProtocol.HTTPS
                // Ktor keeps :80 from http; drop so https uses default 443.
                port = 0
            }.build()
            else -> null
        }
        return httpsUrl?.takeIf { isAllowedHost(host) }?.toString()
    }

    /** Для загрузки изображений: http → https без проверки allowlist. */
    fun preferHttps(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        val url = runCatching { Url(trimmed) }.getOrNull() ?: return rawUrl
        if (!url.protocol.name.equals("http", ignoreCase = true)) return rawUrl
        return URLBuilder(url).apply {
            protocol = URLProtocol.HTTPS
            port = 0
        }.buildString()
    }

    fun isWikipediaHost(host: String): Boolean {
        val lower = host.lowercase()
        return lower == "wikipedia.org" || lower.endsWith(".wikipedia.org")
    }

    private fun isAllowedHost(host: String): Boolean {
        val lower = host.lowercase()
        return allowedHostSuffixes.any { suffix ->
            lower == suffix || lower.endsWith(".$suffix")
        }
    }

    internal fun logOpenFailure(rawUrl: String) {
        AppLogger.w(TAG, "Rejected or failed to open URL (len=${rawUrl.length})")
    }
}
