package com.example.f1_kmp.domain

import com.example.f1_kmp.data.local.CacheDao
import com.example.f1_kmp.data.repository.IEspnRepository

/**
 * Единый контракт pull-to-refresh: сбрасывает слои кэша приложения.
 *
 * GoF Structural Facade — один метод [clearAll] закрывает подсистему из
 * ESPN TTL-кэша и файлового Jolpica-кэша; UI / ViewModel не знают,
 * какие репозитории и в каком порядке чистить.
 */
class AppDataRefresh(
    private val espnRepository: IEspnRepository,
    private val cacheDao: CacheDao,
) {
    /** Очищает все кэши перед принудительной перезагрузкой экрана. */
    suspend fun clearAll() {
        espnRepository.clearCaches()
        cacheDao.clearAll()
    }
}
