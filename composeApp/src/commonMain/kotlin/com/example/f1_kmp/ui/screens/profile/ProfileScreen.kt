package com.example.f1_kmp.ui.screens.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.f1_kmp.data.analytics.AnalyticsEvent
import com.example.f1_kmp.data.analytics.AnalyticsGateway
import com.example.f1_kmp.domain.AppThemePreference
import com.example.f1_kmp.domain.LocaleController
import com.example.f1_kmp.domain.ThemeController
import com.example.f1_kmp.domain.auth.AuthUser
import com.example.f1_kmp.domain.auth.authErrorStringRes
import com.example.f1_kmp.domain.stringResource
import com.example.f1_kmp.ui.components.BlackButton
import com.example.f1_kmp.ui.theme.AppDimens
import com.example.f1_kmp.ui.theme.AppStyles
import com.example.f1_kmp.ui.theme.F1Red
import com.example.f1_kmp.ui.theme.appColors
import com.example.f1_kmp.util.showPlatformToast
import com.example.f1_kmp.viewmodel.ProfileUiState
import com.example.f1_kmp.viewmodel.ProfileViewModel
import f1_kmp.composeapp.generated.resources.Res
import f1_kmp.composeapp.generated.resources.auth_error_generic
import f1_kmp.composeapp.generated.resources.profile_account_section
import f1_kmp.composeapp.generated.resources.profile_appearance_section
import f1_kmp.composeapp.generated.resources.profile_email_not_verified
import f1_kmp.composeapp.generated.resources.profile_email_verified
import f1_kmp.composeapp.generated.resources.profile_language
import f1_kmp.composeapp.generated.resources.profile_not_signed_in
import f1_kmp.composeapp.generated.resources.profile_notifications_section
import f1_kmp.composeapp.generated.resources.profile_practice_reminders
import f1_kmp.composeapp.generated.resources.profile_practice_reminders_subtitle
import f1_kmp.composeapp.generated.resources.profile_race_reminders
import f1_kmp.composeapp.generated.resources.profile_race_reminders_subtitle
import f1_kmp.composeapp.generated.resources.profile_refresh_verification
import f1_kmp.composeapp.generated.resources.profile_resend_verification
import f1_kmp.composeapp.generated.resources.profile_sign_in
import f1_kmp.composeapp.generated.resources.profile_sign_out
import f1_kmp.composeapp.generated.resources.profile_signed_in_as
import f1_kmp.composeapp.generated.resources.profile_still_not_verified
import f1_kmp.composeapp.generated.resources.profile_theme
import f1_kmp.composeapp.generated.resources.profile_verification_sent
import org.koin.compose.koinInject

/** Экран профиля: auth-блок, тема/локаль, напоминания. */
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onSignIn: () -> Unit,
) {
    val user by viewModel.user.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val raceReminders by viewModel.raceRemindersEnabled.collectAsState()
    val practiceReminders by viewModel.practiceRemindersEnabled.collectAsState()

    ProfileScreenContent(
        user = user,
        uiState = uiState,
        raceRemindersEnabled = raceReminders,
        practiceRemindersEnabled = raceReminders && practiceReminders,
        canTogglePractice = raceReminders,
        onClearToast = viewModel::clearToast,
        onSignIn = onSignIn,
        onSignOut = viewModel::signOut,
        onResendVerification = viewModel::resendVerification,
        onRefreshVerification = viewModel::refreshVerification,
        onRaceRemindersChange = viewModel::setRaceRemindersEnabled,
        onPracticeRemindersChange = viewModel::setPracticeRemindersEnabled,
        onLocaleChanged = viewModel::onLocaleChanged,
    )
}

/** Тестируемый контент профиля без ViewModel. */
@Suppress("LongMethod", "LongParameterList")
@Composable
fun ProfileScreenContent(
    user: AuthUser?,
    uiState: ProfileUiState,
    raceRemindersEnabled: Boolean,
    practiceRemindersEnabled: Boolean,
    canTogglePractice: Boolean,
    onClearToast: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onResendVerification: () -> Unit,
    onRefreshVerification: () -> Unit,
    onRaceRemindersChange: (Boolean) -> Unit,
    onPracticeRemindersChange: (Boolean) -> Unit,
    onLocaleChanged: () -> Unit,
) {
    val colors = appColors()
    val analytics = koinInject<AnalyticsGateway>()
    val language by LocaleController.language.collectAsState()
    val themePreference by ThemeController.preference.collectAsState()

    val toastVerificationSent = stringResource(Res.string.profile_verification_sent)
    val toastEmailVerified = stringResource(Res.string.profile_email_verified)
    val toastStillNotVerified = stringResource(Res.string.profile_still_not_verified)
    val toastGeneric = stringResource(Res.string.auth_error_generic)
    val toastErrorRes = authErrorStringRes(uiState.toastMessageKey)
    val toastError = toastErrorRes?.let { stringResource(it) }

    LaunchedEffect(uiState.toastMessageKey) {
        val key = uiState.toastMessageKey ?: return@LaunchedEffect
        val message = when (key) {
            ProfileViewModel.TOAST_VERIFICATION_SENT -> toastVerificationSent
            ProfileViewModel.TOAST_EMAIL_VERIFIED -> toastEmailVerified
            ProfileViewModel.TOAST_STILL_NOT_VERIFIED -> toastStillNotVerified
            else -> toastError ?: toastGeneric
        }
        showPlatformToast(message)
        onClearToast()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = AppDimens.horizontalPadding.dp,
                vertical = 16.dp,
            ),
    ) {
        Text(
            text = stringResource(Res.string.profile_account_section),
            style = AppStyles.h3.copy(fontSize = 18.sp, lineHeight = 22.sp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = user?.email?.let { stringResource(Res.string.profile_signed_in_as, it) }
                ?: stringResource(Res.string.profile_not_signed_in),
            style = AppStyles.body,
        )
        if (user != null && !user.emailVerified) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.profile_email_not_verified),
                style = AppStyles.caption.copy(color = colors.textGray),
            )
            Spacer(Modifier.height(12.dp))
            BlackButton(
                text = stringResource(Res.string.profile_resend_verification),
                onClick = onResendVerification,
                enabled = !uiState.isBusy,
            )
            Spacer(Modifier.height(12.dp))
            BlackButton(
                text = stringResource(Res.string.profile_refresh_verification),
                onClick = onRefreshVerification,
                enabled = !uiState.isBusy,
            )
        }
        Spacer(Modifier.height(12.dp))
        if (user == null) {
            BlackButton(
                text = stringResource(Res.string.profile_sign_in),
                onClick = onSignIn,
            )
        } else {
            BlackButton(
                text = stringResource(Res.string.profile_sign_out),
                onClick = onSignOut,
            )
        }

        Spacer(Modifier.height(28.dp))
        Text(
            text = stringResource(Res.string.profile_appearance_section),
            style = AppStyles.h3.copy(fontSize = 18.sp, lineHeight = 22.sp),
        )
        Spacer(Modifier.height(8.dp))
        ProfileSettingsRow(
            title = stringResource(Res.string.profile_theme),
            trailing = {
                Icon(
                    imageVector = when (themePreference) {
                        AppThemePreference.System -> Icons.Filled.BrightnessAuto
                        AppThemePreference.Light -> Icons.Filled.LightMode
                        AppThemePreference.Dark -> Icons.Filled.DarkMode
                    },
                    contentDescription = null,
                    tint = F1Red,
                )
            },
            onClick = {
                val next = ThemeController.cycle()
                analytics.log(
                    AnalyticsEvent.ThemeChanged(
                        when (next) {
                            AppThemePreference.System -> "system"
                            AppThemePreference.Light -> "light"
                            AppThemePreference.Dark -> "dark"
                        },
                    ),
                )
            },
        )
        Spacer(Modifier.height(8.dp))
        ProfileSettingsRow(
            title = stringResource(Res.string.profile_language),
            trailing = {
                Text(
                    text = language.uppercase(),
                    style = AppStyles.body.copy(color = F1Red),
                )
            },
            onClick = {
                val next = LocaleController.toggle()
                com.example.f1_kmp.util.onLocaleChanged()
                onLocaleChanged()
                analytics.log(AnalyticsEvent.LocaleChanged(next))
            },
        )

        Spacer(Modifier.height(28.dp))
        Text(
            text = stringResource(Res.string.profile_notifications_section),
            style = AppStyles.h3.copy(fontSize = 18.sp, lineHeight = 22.sp),
        )
        Spacer(Modifier.height(8.dp))
        ProfileSwitchRow(
            title = stringResource(Res.string.profile_race_reminders),
            subtitle = stringResource(Res.string.profile_race_reminders_subtitle),
            checked = raceRemindersEnabled,
            enabled = true,
            onCheckedChange = onRaceRemindersChange,
        )
        ProfileSwitchRow(
            title = stringResource(Res.string.profile_practice_reminders),
            subtitle = stringResource(Res.string.profile_practice_reminders_subtitle),
            checked = practiceRemindersEnabled,
            enabled = canTogglePractice,
            onCheckedChange = onPracticeRemindersChange,
        )
    }
}

@Composable
private fun ProfileSettingsRow(
    title: String,
    trailing: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = AppStyles.body, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
private fun ProfileSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = appColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = AppStyles.body.copy(
                    color = if (enabled) colors.black else colors.textGray,
                ),
            )
            Text(
                text = subtitle,
                style = AppStyles.caption.copy(color = colors.textGray),
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = F1Red),
        )
    }
}
