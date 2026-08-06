package com.example.f1_kmp.ui.screens.profile

import f1_kmp.composeapp.generated.resources.Res
import f1_kmp.composeapp.generated.resources.auth_email_label
import f1_kmp.composeapp.generated.resources.auth_forgot_password
import f1_kmp.composeapp.generated.resources.auth_have_account
import f1_kmp.composeapp.generated.resources.auth_no_account
import f1_kmp.composeapp.generated.resources.auth_password_label
import f1_kmp.composeapp.generated.resources.auth_password_reset_sent
import f1_kmp.composeapp.generated.resources.profile_register
import f1_kmp.composeapp.generated.resources.profile_sign_in
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.f1_kmp.domain.auth.authErrorStringRes
import com.example.f1_kmp.domain.stringResource
import com.example.f1_kmp.util.showPlatformToast
import com.example.f1_kmp.ui.components.BlackButton
import com.example.f1_kmp.ui.theme.AppDimens
import com.example.f1_kmp.ui.theme.AppStyles
import com.example.f1_kmp.ui.theme.F1Red
import com.example.f1_kmp.ui.theme.appColors
import com.example.f1_kmp.viewmodel.AuthFormUiState
import com.example.f1_kmp.viewmodel.AuthViewModel

/** Экран входа поверх [AuthViewModel]. */
@Composable
fun AuthSignInScreen(
    viewModel: AuthViewModel,
    onSuccess: () -> Unit,
    onGoRegister: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    AuthFormContent(
        isRegister = false,
        uiState = uiState,
        onEmailChange = viewModel::setEmail,
        onPasswordChange = viewModel::setPassword,
        onSubmit = { viewModel.signIn(onSuccess) },
        onForgotPassword = viewModel::sendPasswordReset,
        onSwitchMode = onGoRegister,
        onConsumePasswordResetSent = viewModel::clearTransientFlags,
    )
}

/** Экран регистрации поверх [AuthViewModel]. */
@Composable
fun AuthRegisterScreen(
    viewModel: AuthViewModel,
    onSuccess: () -> Unit,
    onGoSignIn: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    AuthFormContent(
        isRegister = true,
        uiState = uiState,
        onEmailChange = viewModel::setEmail,
        onPasswordChange = viewModel::setPassword,
        onSubmit = { viewModel.register(onSuccess) },
        onForgotPassword = {},
        onSwitchMode = onGoSignIn,
        onConsumePasswordResetSent = viewModel::clearTransientFlags,
    )
}

/** Общая форма sign-in/register (тестируемый content без nav). */
@Suppress("LongMethod")
@Composable
fun AuthFormContent(
    isRegister: Boolean,
    uiState: AuthFormUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onForgotPassword: () -> Unit,
    onSwitchMode: () -> Unit,
    onConsumePasswordResetSent: () -> Unit,
) {
    val colors = appColors()
    val focusManager = LocalFocusManager.current
    val errorRes = authErrorStringRes(uiState.errorKey)
    val passwordResetSentMessage = stringResource(Res.string.auth_password_reset_sent)

    LaunchedEffect(uiState.passwordResetSent) {
        if (uiState.passwordResetSent) {
            showPlatformToast(passwordResetSentMessage)
            onConsumePasswordResetSent()
        }
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
        OutlinedTextField(
            value = uiState.email,
            onValueChange = onEmailChange,
            enabled = !uiState.isLoading,
            label = { Text(stringResource(Res.string.auth_email_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = uiState.password,
            onValueChange = onPasswordChange,
            enabled = !uiState.isLoading,
            label = { Text(stringResource(Res.string.auth_password_label)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    if (!uiState.isLoading) onSubmit()
                },
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        if (errorRes != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(errorRes),
                style = AppStyles.caption.copy(color = F1Red),
            )
        }
        if (!isRegister) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = stringResource(Res.string.auth_forgot_password),
                    style = AppStyles.caption.copy(color = colors.textGray),
                    modifier = Modifier
                        .clickable(enabled = !uiState.isLoading) {
                            focusManager.clearFocus()
                            onForgotPassword()
                        }
                        .padding(4.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 8.dp),
                color = F1Red,
            )
        } else {
            BlackButton(
                text = stringResource(
                    if (isRegister) Res.string.profile_register else Res.string.profile_sign_in,
                ),
                onClick = {
                    focusManager.clearFocus()
                    onSubmit()
                },
            )
        }
        Spacer(Modifier.height(16.dp))
        TextButton(
            onClick = { if (!uiState.isLoading) onSwitchMode() },
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(
                    if (isRegister) Res.string.auth_have_account else Res.string.auth_no_account,
                ),
                style = AppStyles.body.copy(color = colors.black),
            )
        }
    }
}
