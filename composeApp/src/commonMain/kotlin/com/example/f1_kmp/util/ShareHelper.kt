package com.example.f1_kmp.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.f1_kmp.data.model.RaceModel

/** Экраны регистрируют share-действие для app bar; сбрасывается при dispose. */
val LocalShareActionSetter = staticCompositionLocalOf<(((() -> Unit)?) -> Unit)> {
    { _ -> }
}

/** Регистрирует [onShare] в app bar, пока composition активна. */
@Composable
fun RegisterShareAction(onShare: (() -> Unit)?) {
    val setShare = LocalShareActionSetter.current
    DisposableEffect(onShare) {
        setShare(onShare)
        onDispose { setShare(null) }
    }
}

@Composable
fun rememberShareCareerAction(title: String, races: Int, wins: Int, podiums: Int, poles: Int): () -> Unit =
    remember(title, races, wins, podiums, poles) {
        { shareCareerCard(title, races, wins, podiums, poles) }
    }

@Composable
fun rememberShareRaceAction(race: RaceModel): () -> Unit =
    remember(race.season, race.round, race.raceName, race.results) {
        { shareRaceResultsCard(race) }
    }

/** Платформенная реализация: рендер карточки и системный share sheet. */
expect fun shareCareerCard(title: String, races: Int, wins: Int, podiums: Int, poles: Int)

expect fun shareRaceResultsCard(race: RaceModel)
