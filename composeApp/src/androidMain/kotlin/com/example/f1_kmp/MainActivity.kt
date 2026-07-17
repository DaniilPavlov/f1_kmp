package com.example.f1_kmp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat 
import com.example.f1_kmp.ui.theme.F1Black

/**
 * Единственная Activity.
 *
 * Splash → edge-to-edge → статус-бар F1-чёрный → [App].
 * XML-layout не используется: весь UI в Compose.
 */
class MainActivity : ComponentActivity() {
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
        setContent {
            App()
        }
    }
}
