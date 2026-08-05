package com.example.f1_kmp.ui.screens.predictor

import f1_kmp.composeapp.generated.resources.Res
import f1_kmp.composeapp.generated.resources.nav_profile
import f1_kmp.composeapp.generated.resources.profile_predictor_requires_auth
import f1_kmp.composeapp.generated.resources.profile_predictor_requires_verification
import f1_kmp.composeapp.generated.resources.profile_sign_in
import com.example.f1_kmp.domain.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.f1_kmp.ui.components.BlackButton
import com.example.f1_kmp.ui.theme.AppDimens
import com.example.f1_kmp.ui.theme.AppStyles
import com.example.f1_kmp.ui.theme.appColors
import com.example.f1_kmp.viewmodel.PredictorAuthGateViewModel
import org.koin.compose.viewmodel.koinViewModel

/** Гейт вкладки Predictor: контент только после sign-in + verified email. */
@Composable
fun PredictorAuthGate(
    asTabRoot: Boolean,
    onGoSignIn: () -> Unit,
    onGoProfile: () -> Unit,
    onBlockedNested: () -> Unit = {},
    authViewModel: PredictorAuthGateViewModel = koinViewModel(),
    content: @Composable () -> Unit,
) {
    val canUse by authViewModel.canUsePredictor.collectAsState()
    val signedIn by authViewModel.isSignedIn.collectAsState()

    when {
        canUse -> content()
        asTabRoot && !signedIn -> PredictorGateMessage(
            message = stringResource(Res.string.profile_predictor_requires_auth),
            actionLabel = stringResource(Res.string.profile_sign_in),
            onAction = onGoSignIn,
        )
        asTabRoot -> PredictorGateMessage(
            message = stringResource(Res.string.profile_predictor_requires_verification),
            actionLabel = stringResource(Res.string.nav_profile),
            onAction = onGoProfile,
        )
        else -> {
            LaunchedEffect(canUse, signedIn) {
                onBlockedNested()
            }
            Box(Modifier.fillMaxSize()) {}
        }
    }
}

@Composable
private fun PredictorGateMessage(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors().white)
            .padding(horizontal = AppDimens.horizontalPadding.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = AppStyles.body.copy(color = appColors().black),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        BlackButton(text = actionLabel, onClick = onAction)
    }
}
