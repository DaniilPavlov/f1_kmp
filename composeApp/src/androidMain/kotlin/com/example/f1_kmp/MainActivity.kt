package com.example.f1_kmp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.f1_kmp.notifications.RaceReminderScheduler
import com.example.f1_kmp.ui.theme.F1Black
import org.koin.android.ext.android.inject

/**
 * Единственная Activity.
 *
 * Splash → edge-to-edge → статус-бар F1-чёрный → [App].
 * На resume синхронизируем напоминания о сессиях.
 */
class MainActivity : ComponentActivity() {
    private val reminderScheduler: RaceReminderScheduler by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
        }
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.argb(
            (F1Black.alpha * 255).toInt(),
            (F1Black.red * 255).toInt(),
            (F1Black.green * 255).toInt(),
            (F1Black.blue * 255).toInt(),
        )
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                reminderScheduler.sync()
            }
        })
        setContent {
            App()
        }
    }
}
