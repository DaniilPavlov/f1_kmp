package com.example.f1_kmp.data.deeplink

/**
 * Deep links `f1pet://` — parity with Flutter / f1_kotlin.
 * Parse from URL string (no android.net.Uri in commonMain).
 */
object F1PetDeepLinks {
    const val SCHEME = "f1pet"

    fun driver(driverId: String): String = "$SCHEME://driver/$driverId"
    fun constructor(constructorId: String): String = "$SCHEME://constructor/$constructorId"
    fun circuit(circuitId: String): String = "$SCHEME://circuit/$circuitId"
    fun raceLive(): String = "$SCHEME://race/live"
    fun race(season: String, round: String): String = "$SCHEME://race/$season/$round"
}

sealed class DeepLinkTarget {
    data class Driver(val driverId: String) : DeepLinkTarget()
    data class Constructor(val constructorId: String) : DeepLinkTarget()
    data class Circuit(val circuitId: String) : DeepLinkTarget()
    data object RaceLive : DeepLinkTarget()
    data class Race(val season: String, val round: String) : DeepLinkTarget()
}

fun String.toDeepLinkTarget(): DeepLinkTarget? {
    val trimmed = trim()
    if (!trimmed.startsWith("${F1PetDeepLinks.SCHEME}://", ignoreCase = true)) return null
    val withoutScheme = trimmed.substringAfter("://")
    val host = withoutScheme.substringBefore('/').lowercase()
    val path = withoutScheme.substringAfter('/', missingDelimiterValue = "")
    val segments = path.split('/').filter { it.isNotBlank() }
    return when (host) {
        "driver" -> segments.firstOrNull()?.takeIf { it.isNotBlank() }?.let { DeepLinkTarget.Driver(it) }
        "constructor" -> segments.firstOrNull()?.takeIf { it.isNotBlank() }
            ?.let { DeepLinkTarget.Constructor(it) }
        "circuit" -> segments.firstOrNull()?.takeIf { it.isNotBlank() }?.let { DeepLinkTarget.Circuit(it) }
        "race" -> when {
            segments.firstOrNull() == "live" -> DeepLinkTarget.RaceLive
            segments.size >= 2 -> DeepLinkTarget.Race(segments[0], segments[1])
            else -> null
        }
        else -> null
    }
}
