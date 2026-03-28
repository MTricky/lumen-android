package com.app.lumen

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.app.lumen.features.onboarding.OnboardingManager
import com.app.lumen.features.onboarding.ui.OnboardingView
import com.app.lumen.features.subscription.SubscriptionManager
import com.app.lumen.ui.tabs.MainTabView
import com.app.lumen.ui.theme.LumenTheme
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val shouldOpenPaywall = mutableStateOf(false)
    private val shouldOpenVerseDetail = mutableStateOf(false)
    private var showOnboarding = mutableStateOf(!OnboardingManager.shared.hasCompletedOnboarding)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            LumenTheme {
                if (showOnboarding.value) {
                    OnboardingView(
                        onComplete = {
                            showOnboarding.value = false
                            // Show paywall 1.5s after onboarding if user is not premium (matching iOS)
                            if (!SubscriptionManager.hasProAccess.value) {
                                MainScope().launch {
                                    delay(1500)
                                    shouldOpenPaywall.value = true
                                }
                            }
                        }
                    )
                } else {
                    MainTabView(
                        openPaywall = shouldOpenPaywall.value,
                        onPaywallConsumed = { shouldOpenPaywall.value = false },
                        openVerseDetail = shouldOpenVerseDetail.value,
                        onVerseDetailConsumed = { shouldOpenVerseDetail.value = false },
                    )
                }
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
