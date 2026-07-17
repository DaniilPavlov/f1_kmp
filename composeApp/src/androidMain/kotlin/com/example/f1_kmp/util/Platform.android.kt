package com.example.f1_kmp.util

import android.content.Intent
import android.net.Uri
import com.example.f1_kmp.BuildConfig
import com.example.f1_kmp.platform.AndroidContextHolder

actual fun isDebugBuild(): Boolean = BuildConfig.DEBUG

/** Android: открываем ссылку через [Intent.ACTION_VIEW] (внешний браузер). */
actual fun openUrl(url: String) {
    val context = AndroidContextHolder.applicationContext
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
