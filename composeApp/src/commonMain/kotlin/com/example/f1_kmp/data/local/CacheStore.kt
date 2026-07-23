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

    /** Ключ кэша standings за исторический сезон [year]. */
    fun historicalStandings(year: String) = "historical_standings_$year"
}

/**
 * Интерфейс чтения/записи кэша.
 * Реализация — [FileCacheDao] (файлы на диске через [PlatformCacheStore]).
 */
interface CacheDao {
    /** Читает запись по ключу; null — файла нет. */
    suspend fun get(key: String): CacheEntry?

    /** Записывает или перезаписывает JSON по ключу. */
    suspend fun insert(entry: CacheEntry)

    /** Удаляет все файлы кэша (pull-to-refresh / [AppDataRefresh.clearAll]). */
    suspend fun clearAll()
}

/**
 * Платформенный доступ к файлам (expect/actual).
 * Android пишет в filesDir, iOS — в Documents.
 */
expect class PlatformCacheStore() {
    /** Читает содержимое файла кэша; null — файла нет. */
    fun readText(fileName: String): String?

    /** Записывает JSON в файл кэша. */
    fun writeText(fileName: String, content: String)

    /** Удаляет все файлы в каталоге кэша. */
    fun clearAll()
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

    override suspend fun clearAll() = withContext(Dispatchers.IO) {
        mutex.withLock {
            store.clearAll()
        }
    }

    private fun fileName(key: String): String =
        "cache_${key.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.json"
}
