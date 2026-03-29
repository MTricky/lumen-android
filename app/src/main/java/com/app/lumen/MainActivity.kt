package com.app.lumen

import android.app.NotificationManager
import android.content.Context
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
import com.app.lumen.features.rosary.service.RosaryAudioService
import com.app.lumen.features.subscription.SubscriptionManager
import com.app.lumen.ui.tabs.MainTabView
import com.app.lumen.ui.theme.LumenTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val shouldOpenPaywall = mutableStateOf(false)
    private val shouldOpenVerseDetail = mutableStateOf(false)
    private val shouldOpenCalendarRoutine = mutableStateOf(false)
    private val shouldOpenRosary = mutableStateOf(false)
    private val shouldOpenChaplets = mutableStateOf(false)
    private var showOnboarding = mutableStateOf(!OnboardingManager.shared.hasCompletedOnboarding)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        checkLanguageChangeAndReDownloadAudio()
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
                        openCalendarRoutine = shouldOpenCalendarRoutine.value,
                        onCalendarRoutineConsumed = { shouldOpenCalendarRoutine.value = false },
                        openRosary = shouldOpenRosary.value,
                        onRosaryConsumed = { shouldOpenRosary.value = false },
                        openChaplets = shouldOpenChaplets.value,
                        onChapletsConsumed = { shouldOpenChaplets.value = false },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Clear notification badge by dismissing all displayed notifications
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun checkLanguageChangeAndReDownloadAudio() {
        val prefs = getSharedPreferences("cache_prefs", Context.MODE_PRIVATE)
        val currentLang = Locale.getDefault().language
        val audioLang = when {
            currentLang.startsWith("pl") -> "pl"
            currentLang.startsWith("fr") -> "fr"
            currentLang.startsWith("es") -> "es"
            currentLang.startsWith("pt") -> "pt"
            currentLang.startsWith("it") -> "it"
            currentLang.startsWith("de") -> "de"
            else -> "en"
        }
        val previousAudioLang = prefs.getString("audio_language", null)
        prefs.edit().putString("audio_language", audioLang).apply()

        val audioService = RosaryAudioService.getInstance(this)
        val rosaryPrefs = getSharedPreferences("rosary_prefs", Context.MODE_PRIVATE)
        val audioEnabled = rosaryPrefs.getBoolean("audio_enabled", false)

        // Re-download if: language changed explicitly, OR audio is enabled but not
        // downloaded for the current language (covers first migration and language switch)
        if (!audioService.isAudioDownloaded(audioLang) && audioEnabled) {
            MainScope().launch {
                withContext(Dispatchers.IO) {
                    audioService.downloadAudio(audioLang)
                }
            }
        } else if (previousAudioLang != null && previousAudioLang != audioLang
            && audioService.isAudioDownloaded(previousAudioLang)
            && !audioService.isAudioDownloaded(audioLang)
        ) {
            MainScope().launch {
                withContext(Dispatchers.IO) {
                    audioService.downloadAudio(audioLang)
                }
            }
        }
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
        if (intent?.getBooleanExtra("open_calendar_routine", false) == true) {
            shouldOpenCalendarRoutine.value = true
            intent.removeExtra("open_calendar_routine")
        }
        if (intent?.getBooleanExtra("open_rosary", false) == true) {
            shouldOpenRosary.value = true
            intent.removeExtra("open_rosary")
        }
        if (intent?.getBooleanExtra("open_chaplets", false) == true) {
            shouldOpenChaplets.value = true
            intent.removeExtra("open_chaplets")
        }
    }
}
