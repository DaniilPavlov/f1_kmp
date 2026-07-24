package com.example.f1_kmp

import androidx.compose.ui.window.ComposeUIViewController
import com.example.f1_kmp.data.appmetrica.AppMetricaBootstrap
import com.example.f1_kmp.data.firebase.FirebaseBootstrap
import com.example.f1_kmp.data.firebase.IRemoteConfigService
import com.example.f1_kmp.di.appModule
import com.example.f1_kmp.di.iosModule
import com.example.f1_kmp.domain.ForceUpdateGate
import com.example.f1_kmp.domain.LocaleController
import com.example.f1_kmp.domain.LocalePreferences
import com.example.f1_kmp.notifications.RaceReminderBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform.getKoin
import platform.UIKit.UIColor
import platform.UIKit.UIViewController

private var koinStarted = false
private val bootstrapScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

/**
 * Точка входа iOS: UIViewController со shared Compose UI.
 * Вызывается из Swift (`MainViewControllerKt.MainViewController()`).
 *
 * Koin стартуем один раз — при повторном создании контроллера модули не дублируем.
 * Firebase core уже поднят в Swift; здесь — AppMetrica + RC check + reminders.
 */
fun MainViewController(): UIViewController {
    if (!koinStarted) {
        LocaleController.init(LocalePreferences())
        startKoin {
            modules(appModule, iosModule)
        }
        runCatching { FirebaseBootstrap.initializeSync() }
        AppMetricaBootstrap.bootstrap()
        bootstrapScope.launch {
            val remoteConfig = getKoin().get<IRemoteConfigService>()
            val forceUpdateGate = getKoin().get<ForceUpdateGate>()
            runCatching { FirebaseBootstrap.fetchRemoteConfig(remoteConfig) }
            forceUpdateGate.check()
            if (!forceUpdateGate.required.value) {
                runCatching { RaceReminderBridge.sync() }
            }
        }
        koinStarted = true
    }
    val controller = ComposeUIViewController { App() }

    // Если Compose по каким-то причинам не рисует до краёв safe-area (iOS),
    // то фон UIViewController не должен быть белым.
    // F1GrayBg = 0xFFF6F6F6: R=G=B=0xF6.
    controller.view.backgroundColor = UIColor.colorWithRed(0.9647, 0.9647, 0.9647, 1.0)

    return controller
}
