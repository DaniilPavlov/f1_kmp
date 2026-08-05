package com.example.f1_kmp.domain.predictor

import com.example.f1_kmp.data.repository.IF1Repository
import com.example.f1_kmp.domain.model.QualifyingResult
import com.example.f1_kmp.domain.model.RaceResult
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * GoF Structural Facade — один вход [scoreAllPending]/[scoreWeekend] закрывает
 * подтягивание результатов Jolpica и дописывание очков во все unscored уикенды сезона.
 */
class PredictorScoringCoordinator(
    private val f1Repository: IF1Repository,
) {
    /** Проходит уикенды сезона; новый store только если что-то изменилось. */
    suspend fun scoreAllPending(
        store: PredictorStore,
        year: String,
        now: Instant = Clock.System.now(),
    ): PredictorStore? {
        val season = store.season(year) ?: return null
        var next = store
        var changed = false
        for (weekend in season.weekendsSorted) {
            val scored = scoreWeekend(year, weekend, now) ?: continue
            if (weekendScoreChanged(weekend, scored)) {
                next = next.upsertWeekend(year, scored)
                changed = true
            }
        }
        return next.takeIf { changed }
    }

    /** Тянет quali/race results при отсутствии actual и считает очки. */
    suspend fun scoreWeekend(
        year: String,
        weekend: PredictorWeekendPrediction,
        now: Instant = Clock.System.now(),
    ): PredictorWeekendPrediction? {
        if (weekend.qualifyingOrder.isEmpty() && weekend.raceOrder.isEmpty()) return null

        var qualiActual = weekend.actualQualifyingOrder
        var raceActual = weekend.actualRaceOrder
        var qualiResults = emptyList<QualifyingResult>()
        var raceResults = emptyList<RaceResult>()

        if (qualiActual == null) {
            f1Repository.getQualifyingResults(year, weekend.round).getOrNull()?.let {
                qualiResults = it
                qualiActual = PredictorScoreService.qualifyingActualOrder(it)
            }
        }
        if (raceActual == null) {
            f1Repository.getRaceResults(year, weekend.round).getOrNull()?.results?.let {
                raceResults = it
                raceActual = PredictorScoreService.raceActualOrder(it)
            }
        }

        if (qualiActual == null && raceActual == null) return null

        return PredictorScoreService.applyResults(
            weekend = weekend,
            qualifyingResults = qualiResults.takeIf { it.isNotEmpty() },
            raceResults = raceResults.takeIf { it.isNotEmpty() },
            actualQualifyingOrder = qualiActual,
            actualRaceOrder = raceActual,
            now = now,
        )
    }

    fun weekendScoreChanged(before: PredictorWeekendPrediction, after: PredictorWeekendPrediction): Boolean =
        before.qualiPoints != after.qualiPoints ||
            before.racePoints != after.racePoints ||
            before.actualQualifyingOrder != after.actualQualifyingOrder ||
            before.actualRaceOrder != after.actualRaceOrder
}
