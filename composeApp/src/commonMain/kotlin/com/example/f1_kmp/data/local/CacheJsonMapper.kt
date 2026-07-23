package com.example.f1_kmp.data.local

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

/**
 * Преобразует Kotlin-объекты в JSON для файлового кэша и обратно.
 *
 * [reified T] нужен, чтобы kotlinx.serialization знал конкретный тип
 * (в том числе для `List<Race>`).
 */
class CacheJsonMapper(val json: Json) {

    /** Сериализует объект в JSON-строку для файлового кэша. */
    inline fun <reified T> toJson(value: T): String =
        json.encodeToString(serializer<T>(), value)

    /** Десериализует JSON; при ошибке парсинга возвращает null. */
    inline fun <reified T> fromJson(raw: String): T? =
        runCatching { json.decodeFromString(serializer<T>(), raw) }.getOrNull()

    /** Сериализует список объектов в JSON-массив. */
    inline fun <reified T> toJsonList(items: List<T>): String =
        json.encodeToString(ListSerializer(serializer<T>()), items)

    /** Десериализует JSON-массив; при ошибке парсинга возвращает null. */
    inline fun <reified T> fromJsonList(raw: String): List<T>? =
        runCatching { json.decodeFromString(ListSerializer(serializer<T>()), raw) }.getOrNull()
}
