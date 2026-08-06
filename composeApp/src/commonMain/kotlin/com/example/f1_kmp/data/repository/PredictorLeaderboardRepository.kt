package com.example.f1_kmp.data.repository

import com.example.f1_kmp.data.model.PredictorLeaderboardEntryWriteDto
import com.example.f1_kmp.data.model.PredictorLeaderboardJoinUserDto
import com.example.f1_kmp.data.model.PredictorLeaderboardOptOutDto
import com.example.f1_kmp.data.model.PredictorNicknameDocDto
import com.example.f1_kmp.data.model.PredictorNicknameUpdateUserDto
import com.example.f1_kmp.domain.predictor.PredictorLeaderboardEntry
import com.example.f1_kmp.domain.predictor.PredictorLeaderboardProfile
import com.example.f1_kmp.domain.predictor.PredictorLeaderboardResult
import com.example.f1_kmp.domain.predictor.PredictorNickname
import com.example.f1_kmp.util.AppLogger
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.firestore

/**
 * Join/leave/никнейм/синк очков лидерборда через Firestore-транзакции
 * (уникальность ника в `nicknames/{normalized}`).
 */
class PredictorLeaderboardRepository(
    private val authRepository: IAuthRepository,
) : IPredictorLeaderboardRepository {
    private val firestore get() = Firebase.firestore

    override val currentUid: String?
        get() = authRepository.currentUser?.uid

    private fun userDoc(uid: String) = firestore.collection("users").document(uid)
    private fun nicknameDoc(normalized: String) = firestore.collection("nicknames").document(normalized)
    private fun entryDoc(year: String, uid: String) =
        firestore.collection("leaderboards").document(year).collection("entries").document(uid)

    override suspend fun loadProfile(): PredictorLeaderboardProfile {
        val uid = currentUid ?: return PredictorLeaderboardProfile()
        return try {
            userDoc(uid).get().toLeaderboardProfile()
        } catch (e: Exception) {
            AppLogger.e(TAG, "loadProfile failed", e)
            PredictorLeaderboardProfile()
        }
    }

    override suspend fun loadLeaderboard(year: String): List<PredictorLeaderboardEntry> {
        return try {
            val snap = firestore.collection("leaderboards").document(year)
                .collection("entries")
                .orderBy("totalPoints", Direction.DESCENDING)
                .get()
            val list = snap.documents.map { doc ->
                doc.toLeaderboardEntry()
            }.sortedWith(
                compareByDescending<PredictorLeaderboardEntry> { it.totalPoints }
                    .thenBy { it.nickname.lowercase() },
            )
            list.mapIndexed { index, entry -> entry.withRank(index + 1) }
        } catch (e: Exception) {
            AppLogger.e(TAG, "loadLeaderboard failed", e)
            emptyList()
        }
    }

    override suspend fun join(nickname: String, year: String, totalPoints: Int): PredictorLeaderboardResult {
        PredictorNickname.validate(nickname)?.let { return PredictorLeaderboardResult.Fail(it) }
        val uid = currentUid ?: return PredictorLeaderboardResult.Fail(ERROR_GENERIC)
        val trimmed = nickname.trim()
        val normalized = PredictorNickname.normalize(trimmed)
        return try {
            firestore.runTransaction {
                val userRef = userDoc(uid)
                val nickRef = nicknameDoc(normalized)
                val entryRef = entryDoc(year, uid)
                val profile = get(userRef).toLeaderboardProfile()
                val previousNormalized = profile.nickname?.let(PredictorNickname::normalize)

                val nickSnap = get(nickRef)
                if (nickSnap.exists) {
                    val owner = nickSnap.get<String?>("uid")
                    if (owner != null && owner != uid) {
                        throw NicknameTakenException()
                    }
                }
                if (previousNormalized != null && previousNormalized != normalized) {
                    delete(nicknameDoc(previousNormalized))
                }
                set(nickRef, PredictorNicknameDocDto(uid = uid, nickname = trimmed))
                set(
                    userRef,
                    PredictorLeaderboardJoinUserDto.join(
                        nickname = trimmed,
                        nicknameNormalized = normalized,
                    ),
                    merge = true,
                )
                set(
                    entryRef,
                    PredictorLeaderboardEntryWriteDto.of(nickname = trimmed, totalPoints = totalPoints),
                )
                null
            }
            PredictorLeaderboardResult.Ok
        } catch (_: NicknameTakenException) {
            PredictorLeaderboardResult.Fail(ERROR_TAKEN)
        } catch (e: Exception) {
            AppLogger.e(TAG, "join failed", e)
            PredictorLeaderboardResult.Fail(ERROR_GENERIC)
        }
    }

    override suspend fun leave(year: String): PredictorLeaderboardResult {
        val uid = currentUid ?: return PredictorLeaderboardResult.Fail(ERROR_GENERIC)
        return try {
            val batch = firestore.batch()
            batch.set(userDoc(uid), PredictorLeaderboardOptOutDto.optOut(), merge = true)
            batch.delete(entryDoc(year, uid))
            batch.commit()
            PredictorLeaderboardResult.Ok
        } catch (e: Exception) {
            AppLogger.e(TAG, "leave failed", e)
            PredictorLeaderboardResult.Fail(ERROR_GENERIC)
        }
    }

    override suspend fun updateNickname(nickname: String, year: String): PredictorLeaderboardResult {
        PredictorNickname.validate(nickname)?.let { return PredictorLeaderboardResult.Fail(it) }
        val uid = currentUid ?: return PredictorLeaderboardResult.Fail(ERROR_GENERIC)
        val trimmed = nickname.trim()
        val normalized = PredictorNickname.normalize(trimmed)
        return try {
            firestore.runTransaction {
                val userRef = userDoc(uid)
                val nickRef = nicknameDoc(normalized)
                val entryRef = entryDoc(year, uid)
                val profile = get(userRef).toLeaderboardProfile()
                val previousNormalized = profile.nickname?.let(PredictorNickname::normalize)

                val nickSnap = get(nickRef)
                if (nickSnap.exists) {
                    val owner = nickSnap.get<String?>("uid")
                    if (owner != null && owner != uid) {
                        throw NicknameTakenException()
                    }
                }
                if (previousNormalized != null && previousNormalized != normalized) {
                    delete(nicknameDoc(previousNormalized))
                }
                set(nickRef, PredictorNicknameDocDto(uid = uid, nickname = trimmed))
                set(
                    userRef,
                    PredictorNicknameUpdateUserDto(
                        nickname = trimmed,
                        nicknameNormalized = normalized,
                    ),
                    merge = true,
                )
                if (profile.leaderboardOptIn) {
                    val entrySnap = get(entryRef)
                    val points = entrySnap.get<Int?>("totalPoints")
                        ?: entrySnap.get<Long?>("totalPoints")?.toInt()
                        ?: 0
                    set(
                        entryRef,
                        PredictorLeaderboardEntryWriteDto.of(nickname = trimmed, totalPoints = points),
                        merge = true,
                    )
                }
                null
            }
            PredictorLeaderboardResult.Ok
        } catch (_: NicknameTakenException) {
            PredictorLeaderboardResult.Fail(ERROR_TAKEN)
        } catch (e: Exception) {
            AppLogger.e(TAG, "updateNickname failed", e)
            PredictorLeaderboardResult.Fail(ERROR_GENERIC)
        }
    }

    override suspend fun syncPoints(year: String, totalPoints: Int) {
        val uid = currentUid ?: return
        try {
            firestore.runTransaction {
                val userRef = userDoc(uid)
                val profile = get(userRef).toLeaderboardProfile()
                if (!profile.canShowOnLeaderboard) return@runTransaction null
                val nick = profile.nickname ?: return@runTransaction null
                set(
                    entryDoc(year, uid),
                    PredictorLeaderboardEntryWriteDto.of(nickname = nick, totalPoints = totalPoints),
                    merge = true,
                )
                null
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "syncPoints failed", e)
        }
    }

    override fun clearMemoryCache() = Unit

    private class NicknameTakenException : RuntimeException()

    companion object {
        private const val TAG = "PredictorLeaderboardRepo"
        const val ERROR_GENERIC = "predictorLeaderboardErrorGeneric"
        const val ERROR_TAKEN = "predictorNicknameErrorTaken"
    }
}

private fun DocumentSnapshot.toLeaderboardProfile(): PredictorLeaderboardProfile {
    if (!exists) return PredictorLeaderboardProfile()
    // Field gets — user docs also hold email/timestamps; full data() decode breaks on Timestamp/Any.
    return PredictorLeaderboardProfile(
        nickname = runCatching { get<String?>("nickname") }.getOrNull(),
        leaderboardOptIn = runCatching { get<Boolean?>("leaderboardOptIn") }.getOrNull() ?: false,
    )
}

private fun DocumentSnapshot.toLeaderboardEntry(): PredictorLeaderboardEntry {
    if (!exists) return PredictorLeaderboardEntry(uid = id, nickname = "", totalPoints = 0)
    val points = runCatching { get<Int?>("totalPoints") }.getOrNull()
        ?: runCatching { get<Long?>("totalPoints")?.toInt() }.getOrNull()
        ?: 0
    return PredictorLeaderboardEntry(
        uid = id,
        nickname = runCatching { get<String?>("nickname") }.getOrNull().orEmpty(),
        totalPoints = points,
    )
}
