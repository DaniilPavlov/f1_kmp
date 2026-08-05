package com.example.f1_kmp.data.repository

import com.example.f1_kmp.domain.auth.AuthResult
import com.example.f1_kmp.domain.auth.AuthUser
import com.example.f1_kmp.util.AppLogger
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuthException
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.auth.code
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

/**
 * Реализация [IAuthRepository]: Firebase Auth + merge `users/{uid}` в Firestore.
 * GoF Structural Adapter — чужой Firebase Auth/Firestore API → доменные [AuthUser]/[AuthResult].
 * Ошибки мапятся в стабильные ключи [AuthErrorKeys] для l10n.
 */
class AuthRepository : IAuthRepository {
    private val auth get() = Firebase.auth
    private val firestore get() = Firebase.firestore

    override val currentUser: AuthUser?
        get() = auth.currentUser?.toAuthUser()

    override val userChanges: Flow<AuthUser?> =
        merge(auth.authStateChanged, auth.idTokenChanged)
            .map { it?.toAuthUser() }
            .distinctUntilChanged()

    override val isSignedIn: Boolean
        get() = auth.currentUser != null

    override val isEmailVerified: Boolean
        get() = auth.currentUser?.isEmailVerified == true

    override val canUsePredictor: Boolean
        get() = isSignedIn && isEmailVerified

    override suspend fun signIn(email: String, password: String): AuthResult =
        runAuth {
            auth.signInWithEmailAndPassword(email.trim(), password)
            ensureUserDocument()
            AuthResult.Ok
        }

    override suspend fun register(email: String, password: String): AuthResult =
        runAuth {
            auth.createUserWithEmailAndPassword(email.trim(), password)
            ensureUserDocument()
            sendEmailVerification()
            AuthResult.Ok
        }

    override suspend fun sendPasswordResetEmail(email: String): AuthResult {
        val trimmed = email.trim()
        if (trimmed.isEmpty()) return AuthResult.Fail(AuthErrorKeys.EMPTY_EMAIL)
        return runAuth {
            auth.sendPasswordResetEmail(trimmed)
            AuthResult.Ok
        }
    }

    override suspend fun sendEmailVerification(): AuthResult {
        val user = auth.currentUser ?: return AuthResult.Fail(AuthErrorKeys.GENERIC)
        if (user.isEmailVerified) return AuthResult.Ok
        return runAuth {
            user.sendEmailVerification()
            AuthResult.Ok
        }
    }

    override suspend fun refreshEmailVerification(): Boolean {
        val user = auth.currentUser ?: return false
        return try {
            user.reload()
            val refreshed = auth.currentUser ?: run {
                signOut()
                return false
            }
            refreshed.getIdToken(true)
            if (refreshed.isEmailVerified) {
                ensureUserDocument()
            }
            refreshed.isEmailVerified
        } catch (e: FirebaseAuthException) {
            signOutIfSessionDead(e)
            false
        } catch (e: Exception) {
            AppLogger.e(TAG, "refreshEmailVerification failed", e)
            false
        }
    }

    override suspend fun refreshIdToken(): Boolean {
        val user = auth.currentUser ?: return false
        return try {
            user.getIdToken(true)
            true
        } catch (e: Exception) {
            AppLogger.w(TAG, "refreshIdToken failed", e)
            false
        }
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    override suspend fun ensureUserDocument() {
        val user = auth.currentUser ?: return
        try {
            val ref = firestore.collection("users").document(user.uid)
            val existing = ref.get()
            if (existing.exists) {
                ref.set(
                    mapOf(
                        "email" to user.email,
                        "emailVerified" to user.isEmailVerified,
                    ),
                    merge = true,
                )
            } else {
                ref.set(
                    mapOf(
                        "email" to user.email,
                        "emailVerified" to user.isEmailVerified,
                        "createdAt" to FieldValue.serverTimestamp,
                    ),
                )
            }
        } catch (e: Exception) {
            signOutIfSessionDead(e)
            throw e
        }
    }

    private suspend fun runAuth(block: suspend () -> AuthResult): AuthResult =
        try {
            block()
        } catch (e: FirebaseAuthException) {
            AuthResult.Fail(mapErrorCode(e.code.orEmpty()))
        } catch (e: Exception) {
            AppLogger.e(TAG, "auth call failed", e)
            AuthResult.Fail(AuthErrorKeys.GENERIC)
        }

    private suspend fun signOutIfSessionDead(error: Exception) {
        if (error is FirebaseAuthException && error.code in DEAD_SESSION_CODES) {
            signOut()
        }
    }

    private fun FirebaseUser.toAuthUser() = AuthUser(
        uid = uid,
        email = email,
        emailVerified = isEmailVerified,
    )

    companion object {
        private const val TAG = "AuthRepository"

        private val DEAD_SESSION_CODES = setOf(
            "ERROR_USER_NOT_FOUND",
            "ERROR_USER_DISABLED",
            "user-not-found",
            "user-disabled",
            "user-token-expired",
            "invalid-user-token",
        )

        fun mapErrorCode(code: String): String = when (code) {
            "ERROR_INVALID_EMAIL", "invalid-email" -> AuthErrorKeys.INVALID_EMAIL
            "ERROR_USER_DISABLED", "user-disabled" -> AuthErrorKeys.USER_DISABLED
            "ERROR_USER_NOT_FOUND", "user-not-found" -> AuthErrorKeys.USER_NOT_FOUND
            "ERROR_WRONG_PASSWORD", "wrong-password" -> AuthErrorKeys.WRONG_PASSWORD
            "ERROR_INVALID_CREDENTIAL", "invalid-credential" -> AuthErrorKeys.INVALID_CREDENTIAL
            "ERROR_EMAIL_ALREADY_IN_USE", "email-already-in-use" -> AuthErrorKeys.EMAIL_IN_USE
            "ERROR_WEAK_PASSWORD", "weak-password" -> AuthErrorKeys.WEAK_PASSWORD
            "ERROR_TOO_MANY_REQUESTS", "too-many-requests" -> AuthErrorKeys.TOO_MANY_REQUESTS
            "ERROR_NETWORK_REQUEST_FAILED", "network-request-failed" -> AuthErrorKeys.NETWORK
            else -> AuthErrorKeys.GENERIC
        }
    }
}

/** Стабильные ключи ошибок Auth для l10n (не UI-строки). */
object AuthErrorKeys {
    const val EMPTY_FIELDS = "authErrorEmptyFields"
    const val EMPTY_EMAIL = "authErrorEmptyEmail"
    const val INVALID_EMAIL = "authErrorInvalidEmail"
    const val USER_DISABLED = "authErrorUserDisabled"
    const val USER_NOT_FOUND = "authErrorUserNotFound"
    const val WRONG_PASSWORD = "authErrorWrongPassword"
    const val INVALID_CREDENTIAL = "authErrorInvalidCredential"
    const val EMAIL_IN_USE = "authErrorEmailInUse"
    const val WEAK_PASSWORD = "authErrorWeakPassword"
    const val DISPOSABLE_EMAIL = "authErrorDisposableEmail"
    const val TOO_MANY_REQUESTS = "authErrorTooManyRequests"
    const val NETWORK = "authErrorNetwork"
    const val GENERIC = "authErrorGeneric"
}
