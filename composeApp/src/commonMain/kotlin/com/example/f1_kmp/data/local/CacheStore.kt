package com.example.f1_kmp.data.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Одна запись локального кэша.
 *
 * Вместо отдельных таблиц под каждый тип данных храним JSON-строку по ключу —
 * проще для pet-проекта. Сериализация через [CacheJsonMapper].
 */
data class CacheEntry(
    val key: String,
    val json: String,
)

/** Константы ключей кэша — одна строка на весь проект, без опечаток в разных местах. */
object CacheKeys {
    const val CURRENT_DRIVERS = "current_drivers"
    const val CURRENT_CONSTRUCTORS = "current_constructors"
    const val SCHEDULE = "schedule"
    const val CIRCUITS = "circuits"
    const val LAST_RACE = "last_race"
    const val SEASONS = "seasons"

    fun historicalStandings(year: String) = "historical_standings_$year"
}

/**
 * Интерфейс чтения/записи кэша.
 * Реализация — [FileCacheDao] (файлы на диске через [PlatformCacheStore]).
 */
interface CacheDao {
    suspend fun get(key: String): CacheEntry?
    suspend fun insert(entry: CacheEntry)
}

/**
 * Платформенный доступ к файлам (expect/actual).
 * Android пишет в filesDir, iOS — в Documents.
 */
expect class PlatformCacheStore() {
    fun readText(fileName: String): String?
    fun writeText(fileName: String, content: String)
}

/**
 * Кэш на файлах: один JSON-файл на ключ.
 * [Mutex] не даёт двум корутинам одновременно писать/читать один и тот же файл.
 * IO идёт на [Dispatchers.IO], UI-поток не блокируется.
 */
class FileCacheDao(
    private val store: PlatformCacheStore = PlatformCacheStore(),
) : CacheDao {
    private val mutex = Mutex()

    override suspend fun get(key: String): CacheEntry? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val json = store.readText(fileName(key)) ?: return@withLock null
            CacheEntry(key = key, json = json)
        }
    }

    override suspend fun insert(entry: CacheEntry) = withContext(Dispatchers.IO) {
        mutex.withLock {
            store.writeText(fileName(entry.key), entry.json)
        }
    }

    private fun fileName(key: String): String =
        "cache_${key.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.json"
}
