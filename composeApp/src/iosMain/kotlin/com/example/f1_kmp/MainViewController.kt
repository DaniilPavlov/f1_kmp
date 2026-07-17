package com.example.f1_kmp

import androidx.compose.ui.window.ComposeUIViewController
import com.example.f1_kmp.di.appModule
import org.koin.core.context.startKoin
import platform.UIKit.UIColor
import platform.UIKit.UIViewController

private var koinStarted = false

/**
 * Точка входа iOS: UIViewController со shared Compose UI.
 * Вызывается из Swift (`MainViewControllerKt.MainViewController()`).
 *
 * Koin стартуем один раз — при повторном создании контроллера модули не дублируем.
 */
fun MainViewController(): UIViewController {
    if (!koinStarted) {
        startKoin {
            modules(appModule)
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
