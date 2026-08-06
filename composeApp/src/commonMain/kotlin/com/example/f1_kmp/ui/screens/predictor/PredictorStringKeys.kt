package com.example.f1_kmp.ui.screens.predictor

import f1_kmp.composeapp.generated.resources.Res
import f1_kmp.composeapp.generated.resources.predictor_leaderboard_error_generic
import f1_kmp.composeapp.generated.resources.predictor_leaderboard_opt_in_required
import f1_kmp.composeapp.generated.resources.predictor_nickname_error_chars
import f1_kmp.composeapp.generated.resources.predictor_nickname_error_length
import f1_kmp.composeapp.generated.resources.predictor_nickname_error_taken
import org.jetbrains.compose.resources.StringResource

/** Ключ формы лидерборда/ника → `@StringRes` или null. */
fun predictorFormErrorRes(key: String?): StringResource? = when (key) {
    "predictorNicknameErrorLength" -> Res.string.predictor_nickname_error_length
    "predictorNicknameErrorChars" -> Res.string.predictor_nickname_error_chars
    "predictorNicknameErrorTaken" -> Res.string.predictor_nickname_error_taken
    "predictorLeaderboardErrorGeneric" -> Res.string.predictor_leaderboard_error_generic
    "predictorLeaderboardOptInRequired" -> Res.string.predictor_leaderboard_opt_in_required
    else -> null
}
