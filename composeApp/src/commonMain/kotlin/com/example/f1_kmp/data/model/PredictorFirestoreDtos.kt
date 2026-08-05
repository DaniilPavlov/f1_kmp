package com.example.f1_kmp.data.model

import com.example.f1_kmp.domain.predictor.PredictorLeaderboardEntry
import com.example.f1_kmp.domain.predictor.PredictorLeaderboardProfile
import com.example.f1_kmp.domain.predictor.PredictorSeason
import com.example.f1_kmp.domain.predictor.PredictorWeekendPrediction
import dev.gitlive.firebase.firestore.FieldValue
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/** Firestore DTO for `users/{uid}/seasons/{year}.weekends/{round}`. */
@Serializable
data class PredictorWeekendFirestoreDto(
    val round: String = "",
    val raceName: String = "",
    val qualifyingOrder: List<String> = emptyList(),
    val raceOrder: List<String> = emptyList(),
    val lockedAt: String? = null,
    val qualiPoints: Int? = null,
    val racePoints: Int? = null,
    val scoredAt: String? = null,
    val actualQualifyingOrder: List<String>? = null,
    val actualRaceOrder: List<String>? = null,
) {
    fun toDomain(): PredictorWeekendPrediction =
        PredictorWeekendPrediction(
            round = round,
            raceName = raceName,
            qualifyingOrder = qualifyingOrder,
            raceOrder = raceOrder,
            lockedAt = lockedAt?.let { runCatching { Instant.parse(it) }.getOrNull() },
            qualiPoints = qualiPoints,
            racePoints = racePoints,
            scoredAt = scoredAt?.let { runCatching { Instant.parse(it) }.getOrNull() },
            actualQualifyingOrder = actualQualifyingOrder,
            actualRaceOrder = actualRaceOrder,
        )

    companion object {
        fun fromDomain(weekend: PredictorWeekendPrediction): PredictorWeekendFirestoreDto =
            PredictorWeekendFirestoreDto(
                round = weekend.round,
                raceName = weekend.raceName,
                qualifyingOrder = weekend.qualifyingOrder,
                raceOrder = weekend.raceOrder,
                lockedAt = weekend.lockedAt?.toString(),
                qualiPoints = weekend.qualiPoints,
                racePoints = weekend.racePoints,
                scoredAt = weekend.scoredAt?.toString(),
                actualQualifyingOrder = weekend.actualQualifyingOrder,
                actualRaceOrder = weekend.actualRaceOrder,
            )
    }
}

/** Read DTO for `users/{uid}/seasons/{year}` (ignores server timestamps). */
@Serializable
data class PredictorSeasonFirestoreDto(
    val weekends: Map<String, PredictorWeekendFirestoreDto> = emptyMap(),
) {
    fun toDomain(year: String): PredictorSeason =
        PredictorSeason(
            year = year,
            weekends = weekends.mapValues { (_, weekend) -> weekend.toDomain() },
        )
}

/** Write DTO for season docs — includes server timestamp sentinel. */
@Serializable
data class PredictorSeasonWriteDto(
    val weekends: Map<String, PredictorWeekendFirestoreDto>,
    val updatedAt: FieldValue,
) {
    companion object {
        fun fromDomain(season: PredictorSeason): PredictorSeasonWriteDto =
            PredictorSeasonWriteDto(
                weekends = season.weekends.mapValues { (_, weekend) ->
                    PredictorWeekendFirestoreDto.fromDomain(weekend)
                },
                updatedAt = FieldValue.serverTimestamp,
            )
    }
}

/** Profile fields on `users/{uid}` used by the leaderboard. */
@Serializable
data class PredictorLeaderboardProfileDto(
    val nickname: String? = null,
    val leaderboardOptIn: Boolean = false,
) {
    fun toDomain(): PredictorLeaderboardProfile =
        PredictorLeaderboardProfile(nickname = nickname, leaderboardOptIn = leaderboardOptIn)
}

@Serializable
data class PredictorNicknameDocDto(
    val uid: String,
    val nickname: String,
)

/** Read DTO for public leaderboard rows. */
@Serializable
data class PredictorLeaderboardEntryDto(
    val nickname: String = "",
    val totalPoints: Int = 0,
) {
    fun toDomain(uid: String): PredictorLeaderboardEntry =
        PredictorLeaderboardEntry(uid = uid, nickname = nickname, totalPoints = totalPoints)
}

@Serializable
data class PredictorLeaderboardEntryWriteDto(
    val nickname: String,
    val totalPoints: Int,
    val updatedAt: FieldValue,
) {
    companion object {
        fun of(nickname: String, totalPoints: Int) = PredictorLeaderboardEntryWriteDto(
            nickname = nickname,
            totalPoints = totalPoints,
            updatedAt = FieldValue.serverTimestamp,
        )
    }
}

@Serializable
data class PredictorLeaderboardJoinUserDto(
    val nickname: String,
    val nicknameNormalized: String,
    val leaderboardOptIn: Boolean,
    val leaderboardOptInAt: FieldValue,
) {
    companion object {
        fun join(nickname: String, nicknameNormalized: String) = PredictorLeaderboardJoinUserDto(
            nickname = nickname,
            nicknameNormalized = nicknameNormalized,
            leaderboardOptIn = true,
            leaderboardOptInAt = FieldValue.serverTimestamp,
        )
    }
}

@Serializable
data class PredictorNicknameUpdateUserDto(
    val nickname: String,
    val nicknameNormalized: String,
)

@Serializable
data class PredictorLeaderboardOptOutDto(
    val leaderboardOptIn: Boolean,
) {
    companion object {
        fun optOut() = PredictorLeaderboardOptOutDto(leaderboardOptIn = false)
    }
}
