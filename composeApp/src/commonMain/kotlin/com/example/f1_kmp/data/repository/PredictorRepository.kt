package com.example.f1_kmp.data.repository

import com.example.f1_kmp.data.model.PredictorSeasonFirestoreDto
import com.example.f1_kmp.data.model.PredictorSeasonWriteDto
import com.example.f1_kmp.domain.predictor.PredictorSeason
import com.example.f1_kmp.domain.predictor.PredictorStore
import com.example.f1_kmp.domain.predictor.PredictorWeekendPrediction
import com.example.f1_kmp.util.AppLogger
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Firestore-хранилище прогнозов сезона (`users/{uid}/seasons`).
 * Mutex + memory по uid; UI offline-кэш не трогает.
 */
class PredictorRepository(
    private val authRepository: IAuthRepository,
) : IPredictorRepository {
    private val firestore get() = Firebase.firestore
    private val mutex = Mutex()

    @Volatile private var memory: PredictorStore? = null
    @Volatile private var memoryUid: String? = null

    private fun seasonsCol(uid: String) =
        firestore.collection("users").document(uid).collection("seasons")

    override suspend fun load(): PredictorStore = mutex.withLock {
        val uid = authRepository.currentUser?.uid
        val cached = memory
        if (cached != null && memoryUid == uid && uid != null) return cached
        if (uid == null) {
            clearMemoryCacheUnlocked()
            return PredictorStore.empty()
        }
        return try {
            val snap = seasonsCol(uid).get()
            val seasons = snap.documents.associate { doc ->
                doc.id to doc.toSeason(doc.id)
            }
            val store = PredictorStore(seasons = seasons)
            memory = store
            memoryUid = uid
            store
        } catch (e: Exception) {
            AppLogger.e(TAG, "load failed", e)
            clearMemoryCacheUnlocked()
            throw e
        }
    }

    override suspend fun saveWeekend(year: String, weekend: PredictorWeekendPrediction): PredictorStore =
        mutex.withLock {
            val uid = authRepository.currentUser?.uid
                ?: error("PredictorRepository.saveWeekend requires signed-in user")
            val current = memory?.takeIf { memoryUid == uid } ?: loadUnlocked(uid)
            val next = current.upsertWeekend(year, weekend)
            persistSeason(uid, year, next.season(year)!!)
            memory = next
            memoryUid = uid
            next
        }

    override suspend fun replace(store: PredictorStore): PredictorStore = mutex.withLock {
        val uid = authRepository.currentUser?.uid
            ?: error("PredictorRepository.replace requires signed-in user")
        for ((year, season) in store.seasons) {
            persistSeason(uid, year, season)
        }
        memory = store
        memoryUid = uid
        store
    }

    override fun clearMemoryCache() {
        memory = null
        memoryUid = null
    }

    private fun clearMemoryCacheUnlocked() {
        memory = null
        memoryUid = null
    }

    private suspend fun loadUnlocked(uid: String): PredictorStore {
        val snap = seasonsCol(uid).get()
        val seasons = snap.documents.associate { doc ->
            doc.id to doc.toSeason(doc.id)
        }
        return PredictorStore(seasons = seasons)
    }

    private suspend fun persistSeason(uid: String, year: String, season: PredictorSeason) {
        seasonsCol(uid).document(year).set(
            PredictorSeasonWriteDto.fromDomain(season),
            merge = true,
        )
    }

    companion object {
        private const val TAG = "PredictorRepository"
    }
}

private fun DocumentSnapshot.toSeason(year: String): PredictorSeason {
    if (!exists) return PredictorSeason(year = year)
    return data(PredictorSeasonFirestoreDto.serializer()).toDomain(year)
}
