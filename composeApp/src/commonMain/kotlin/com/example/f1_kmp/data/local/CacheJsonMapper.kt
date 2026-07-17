package com.example.f1_kmp.data.local

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

/**
 * Преобразует Kotlin-объекты в JSON для файлового кэша и обратно.
 *
 * [reified T] нужен, чтобы kotlinx.serialization знал конкретный тип
 * (в том числе для `List<RaceModel>`).
 */
class CacheJsonMapper(val json: Json) {

    inline fun <reified T> toJson(value: T): String =
        json.encodeToString(serializer<T>(), value)

    inline fun <reified T> fromJson(raw: String): T? =
        runCatching { json.decodeFromString(serializer<T>(), raw) }.getOrNull()

    inline fun <reified T> toJsonList(items: List<T>): String =
        json.encodeToString(ListSerializer(serializer<T>()), items)

    inline fun <reified T> fromJsonList(raw: String): List<T>? =
        runCatching { json.decodeFromString(ListSerializer(serializer<T>()), raw) }.getOrNull()
}
