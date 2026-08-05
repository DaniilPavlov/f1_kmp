package com.example.f1_kmp.util

/** iOS: silent no-op (toasts are not a system pattern; Profile can later use snackbar). */
actual fun showPlatformToast(message: String) {
    // no-op
}
