package com.example.f1_kmp.domain.auth

import com.example.f1_kmp.data.repository.AuthErrorKeys
import f1_kmp.composeapp.generated.resources.Res
import f1_kmp.composeapp.generated.resources.auth_error_disposable_email
import f1_kmp.composeapp.generated.resources.auth_error_email_in_use
import f1_kmp.composeapp.generated.resources.auth_error_empty_email
import f1_kmp.composeapp.generated.resources.auth_error_empty_fields
import f1_kmp.composeapp.generated.resources.auth_error_generic
import f1_kmp.composeapp.generated.resources.auth_error_invalid_credential
import f1_kmp.composeapp.generated.resources.auth_error_invalid_email
import f1_kmp.composeapp.generated.resources.auth_error_network
import f1_kmp.composeapp.generated.resources.auth_error_too_many_requests
import f1_kmp.composeapp.generated.resources.auth_error_user_disabled
import f1_kmp.composeapp.generated.resources.auth_error_user_not_found
import f1_kmp.composeapp.generated.resources.auth_error_weak_password
import f1_kmp.composeapp.generated.resources.auth_error_wrong_password
import org.jetbrains.compose.resources.StringResource

/**
 * Имена composeResources / string KEY для ошибок Auth (не Android R).
 * Неизвестный/null → generic / null.
 */
object AuthErrorStringKeys {
    const val EMPTY_FIELDS = "auth_error_empty_fields"
    const val EMPTY_EMAIL = "auth_error_empty_email"
    const val INVALID_EMAIL = "auth_error_invalid_email"
    const val USER_DISABLED = "auth_error_user_disabled"
    const val USER_NOT_FOUND = "auth_error_user_not_found"
    const val WRONG_PASSWORD = "auth_error_wrong_password"
    const val INVALID_CREDENTIAL = "auth_error_invalid_credential"
    const val EMAIL_IN_USE = "auth_error_email_in_use"
    const val WEAK_PASSWORD = "auth_error_weak_password"
    const val DISPOSABLE_EMAIL = "auth_error_disposable_email"
    const val TOO_MANY_REQUESTS = "auth_error_too_many_requests"
    const val NETWORK = "auth_error_network"
    const val GENERIC = "auth_error_generic"
}

/** Ключ AuthRepository → имя string resource; неизвестный/null → generic/null. */
fun authErrorStringKey(key: String?): String? = when (key) {
    AuthErrorKeys.EMPTY_FIELDS -> AuthErrorStringKeys.EMPTY_FIELDS
    AuthErrorKeys.EMPTY_EMAIL -> AuthErrorStringKeys.EMPTY_EMAIL
    AuthErrorKeys.INVALID_EMAIL -> AuthErrorStringKeys.INVALID_EMAIL
    AuthErrorKeys.USER_DISABLED -> AuthErrorStringKeys.USER_DISABLED
    AuthErrorKeys.USER_NOT_FOUND -> AuthErrorStringKeys.USER_NOT_FOUND
    AuthErrorKeys.WRONG_PASSWORD -> AuthErrorStringKeys.WRONG_PASSWORD
    AuthErrorKeys.INVALID_CREDENTIAL -> AuthErrorStringKeys.INVALID_CREDENTIAL
    AuthErrorKeys.EMAIL_IN_USE -> AuthErrorStringKeys.EMAIL_IN_USE
    AuthErrorKeys.WEAK_PASSWORD -> AuthErrorStringKeys.WEAK_PASSWORD
    AuthErrorKeys.DISPOSABLE_EMAIL -> AuthErrorStringKeys.DISPOSABLE_EMAIL
    AuthErrorKeys.TOO_MANY_REQUESTS -> AuthErrorStringKeys.TOO_MANY_REQUESTS
    AuthErrorKeys.NETWORK -> AuthErrorStringKeys.NETWORK
    AuthErrorKeys.GENERIC -> AuthErrorStringKeys.GENERIC
    null -> null
    else -> AuthErrorStringKeys.GENERIC
}

/** Ключ Auth → compose [StringResource]; неизвестный/null → generic/null. */
fun authErrorStringRes(key: String?): StringResource? = when (key) {
    AuthErrorKeys.EMPTY_FIELDS, AuthErrorStringKeys.EMPTY_FIELDS -> Res.string.auth_error_empty_fields
    AuthErrorKeys.EMPTY_EMAIL, AuthErrorStringKeys.EMPTY_EMAIL -> Res.string.auth_error_empty_email
    AuthErrorKeys.INVALID_EMAIL, AuthErrorStringKeys.INVALID_EMAIL -> Res.string.auth_error_invalid_email
    AuthErrorKeys.USER_DISABLED, AuthErrorStringKeys.USER_DISABLED -> Res.string.auth_error_user_disabled
    AuthErrorKeys.USER_NOT_FOUND, AuthErrorStringKeys.USER_NOT_FOUND -> Res.string.auth_error_user_not_found
    AuthErrorKeys.WRONG_PASSWORD, AuthErrorStringKeys.WRONG_PASSWORD -> Res.string.auth_error_wrong_password
    AuthErrorKeys.INVALID_CREDENTIAL, AuthErrorStringKeys.INVALID_CREDENTIAL ->
        Res.string.auth_error_invalid_credential
    AuthErrorKeys.EMAIL_IN_USE, AuthErrorStringKeys.EMAIL_IN_USE -> Res.string.auth_error_email_in_use
    AuthErrorKeys.WEAK_PASSWORD, AuthErrorStringKeys.WEAK_PASSWORD -> Res.string.auth_error_weak_password
    AuthErrorKeys.DISPOSABLE_EMAIL, AuthErrorStringKeys.DISPOSABLE_EMAIL ->
        Res.string.auth_error_disposable_email
    AuthErrorKeys.TOO_MANY_REQUESTS, AuthErrorStringKeys.TOO_MANY_REQUESTS ->
        Res.string.auth_error_too_many_requests
    AuthErrorKeys.NETWORK, AuthErrorStringKeys.NETWORK -> Res.string.auth_error_network
    AuthErrorKeys.GENERIC, AuthErrorStringKeys.GENERIC -> Res.string.auth_error_generic
    null -> null
    else -> Res.string.auth_error_generic
}
