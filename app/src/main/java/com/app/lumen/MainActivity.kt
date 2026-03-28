package com.app.lumen

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.app.lumen.ui.tabs.MainTabView
import com.app.lumen.ui.theme.LumenTheme

class MainActivity : ComponentActivity() {

    private val shouldOpenPaywall = mutableStateOf(false)
    private val shouldOpenVerseDetail = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            LumenTheme {
                MainTabView(
                    openPaywall = shouldOpenPaywall.value,
                    onPaywallConsumed = { shouldOpenPaywall.value = false },
                    openVerseDetail = shouldOpenVerseDetail.value,
                    onVerseDetailConsumed = { shouldOpenVerseDetail.value = false },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("open_paywall", false) == true) {
            shouldOpenPaywall.value = true
            intent.removeExtra("open_paywall")
        }
        if (intent?.getBooleanExtra("open_verse_detail", false) == true) {
            shouldOpenVerseDetail.value = true
            intent.removeExtra("open_verse_detail")
        }
    }
}
