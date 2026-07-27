package com.example.f1_kmp.util

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import com.example.f1_kmp.BuildConfig
import com.example.f1_kmp.notifications.RaceReminderScheduler
import com.example.f1_kmp.platform.AndroidContextHolder
import org.koin.core.context.GlobalContext

actual fun isDebugBuild(): Boolean = BuildConfig.DEBUG

actual fun onLocaleChanged() {
    runCatching {
        GlobalContext.get().get<RaceReminderScheduler>().sync()
    }
}

/** Android: открываем только доверенную https-ссылку через [Intent.ACTION_VIEW]. */
actual fun openUrl(url: String) {
    val normalized = TrustedUrl.parse(url)
    if (normalized == null) {
        TrustedUrl.logOpenFailure(url)
        return
    }
    try {
        val context = AndroidContextHolder.applicationContext
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalized))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        TrustedUrl.logOpenFailure(url)
    }
}
