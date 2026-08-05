package com.example.f1_kmp.util

import android.widget.Toast
import com.example.f1_kmp.platform.AndroidContextHolder

actual fun showPlatformToast(message: String) {
    Toast.makeText(AndroidContextHolder.applicationContext, message, Toast.LENGTH_SHORT).show()
}
