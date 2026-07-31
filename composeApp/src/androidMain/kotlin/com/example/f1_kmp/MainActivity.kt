package com.example.f1_kmp

import android.Manifest
import android.content.Intent
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
import androidx.lifecycle.lifecycleScope
import com.example.f1_kmp.data.deeplink.DeepLinkBus
import com.example.f1_kmp.domain.ForceUpdateGate
import com.example.f1_kmp.domain.live.LiveWeekendController
import com.example.f1_kmp.notifications.RaceReminderScheduler
import com.example.f1_kmp.ui.theme.F1Chrome
import com.example.f1_kmp.util.initShareHelper
import com.example.f1_kmp.widgets.AppWidgetSyncService
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * Единственная Activity.
 *
 * Splash → edge-to-edge → статус-бар F1-чёрный → [App].
 * На resume: Remote Config + force-update gate; напоминания — если обновление не требуется.
 */
class MainActivity : ComponentActivity() {
    private val reminderScheduler: RaceReminderScheduler by inject()
    private val forceUpdateGate: ForceUpdateGate by inject()
    private val deepLinkBus: DeepLinkBus by inject()
    private val liveWeekendController: LiveWeekendController by inject()
    private val appWidgetSyncService: AppWidgetSyncService by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
        }
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.argb(
            (F1Chrome.alpha * 255).toInt(),
            (F1Chrome.red * 255).toInt(),
            (F1Chrome.green * 255).toInt(),
            (F1Chrome.blue * 255).toInt(),
        )
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                liveWeekendController.onAppForeground()
                lifecycleScope.launch {
                    forceUpdateGate.onResume()
                    if (!forceUpdateGate.required.value) {
                        reminderScheduler.sync()
                        runCatching { appWidgetSyncService.sync() }
                    }
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                liveWeekendController.onAppBackground()
            }
        })
        initShareHelper(this, lifecycleScope)
        deepLinkBus.offer(intent?.data?.toString())
        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkBus.offer(intent.data?.toString())
    }
}
