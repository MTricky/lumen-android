package com.app.lumen.features.chaplets.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.app.lumen.features.chaplets.model.*
import com.app.lumen.services.AnalyticsEvent
import com.app.lumen.services.AnalyticsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import java.util.Locale

class StMichaelViewModel(application: Application) : AndroidViewModel(application) {

    private val json = Json { ignoreUnknownKeys = true }

    private val _prayerData = MutableStateFlow<StMichaelPrayerData?>(null)
    val prayerData: StateFlow<StMichaelPrayerData?> = _prayerData

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _currentStepIndex = MutableStateFlow(0)
    val currentStepIndex: StateFlow<Int> = _currentStepIndex

    private val _isComplete = MutableStateFlow(false)
    val isComplete: StateFlow<Boolean> = _isComplete

    private var allSteps: List<StMichaelPrayerStep> = emptyList()

    val currentStep: StMichaelPrayerStep?
        get() {
            val idx = _currentStepIndex.value
            return if (idx < allSteps.size) allSteps[idx] else null
        }

    val currentPrayer: Prayer?
        get() {
            val step = currentStep ?: return null
            val prayers = _prayerData.value?.prayers ?: return null
            return when (step) {
                is StMichaelPrayerStep.SignOfTheCross,
                is StMichaelPrayerStep.ClosingSignOfTheCross -> prayers.signOfTheCross
                is StMichaelPrayerStep.OpeningPrayer -> prayers.openingPrayer
                is StMichaelPrayerStep.SalutationOurFather,
                is StMichaelPrayerStep.ArchangelOurFather -> prayers.ourFather
                is StMichaelPrayerStep.SalutationHailMary -> prayers.hailMary
                is StMichaelPrayerStep.SalutationGloryBe -> prayers.gloryBe
                is StMichaelPrayerStep.ClosingPrayer -> prayers.closingPrayer
                is StMichaelPrayerStep.FinalPrayer -> prayers.finalPrayer
                is StMichaelPrayerStep.Intro,
                is StMichaelPrayerStep.SalutationAnnouncement,
                is StMichaelPrayerStep.ArchangelAnnouncement -> null
            }
        }

    val currentSalutation: StMichaelSalutation?
        get() {
            val step = currentStep ?: return null
            val salutations = _prayerData.value?.salutations ?: return null
            val salutationNum = step.getSalutation() ?: return null
            return if (salutationNum in 1..salutations.size) salutations[salutationNum - 1] else null
        }

    val currentArchangelPrayer: StMichaelArchangelPrayer?
        get() {
            val step = currentStep ?: return null
            val archangelPrayers = _prayerData.value?.archangelPrayers ?: return null
            val archangelNum = step.getArchangel() ?: return null
            return if (archangelNum in 1..archangelPrayers.size) archangelPrayers[archangelNum - 1] else null
        }

    val progress: StMichaelProgress?
        get() {
            val step = currentStep ?: return null
            return StMichaelProgress(
                currentStep = step,
                totalSteps = allSteps.size,
                currentStepIndex = _currentStepIndex.value,
            )
        }

    fun loadPrayers() {
        if (_prayerData.value != null || _isLoading.value) return
        _isLoading.value = true
        try {
            val lang = prayerLanguageCode()
            val path = "prayers/chaplets/stmichael/stmichael_$lang.json"
            val jsonString = try {
                getApplication<Application>().assets.open(path)
            } catch (_: Exception) {
                getApplication<Application>().assets.open("prayers/chaplets/stmichael/stmichael_en.json")
            }.bufferedReader().use { it.readText() }
            _prayerData.value = json.decodeFromString<StMichaelPrayerData>(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _isLoading.value = false
    }

    private fun prayerLanguageCode(): String {
        val lang = Locale.getDefault().language
        return when {
            lang.startsWith("pl") -> "pl"
            lang.startsWith("fr") -> "fr"
            lang.startsWith("es") -> "es"
            lang.startsWith("pt") -> "pt"
            lang.startsWith("it") -> "it"
            lang.startsWith("de") -> "de"
            else -> "en"
        }
    }

    fun startChaplet() {
        _currentStepIndex.value = 0
        _isComplete.value = false
        allSteps = buildAllSteps()

        // Track prayer started (matching iOS)
        AnalyticsManager.trackEvent(
            AnalyticsEvent.PRAYER_STARTED,
            mapOf("prayer_type" to "st_michael")
        )
    }

    fun advanceToNextStep() {
        if (_isComplete.value) return // Prevent duplicate tracking
        if (_currentStepIndex.value >= allSteps.size - 1) {
            _isComplete.value = true

            // Track prayer completed (matching iOS)
            val app = getApplication<Application>()
            val prefs = app.getSharedPreferences("rosary_prefs", Context.MODE_PRIVATE)
            val audioToggle = prefs.getBoolean("audio_enabled", false)
            val lang = prayerLanguageCode()
            val audioService = com.app.lumen.features.rosary.service.RosaryAudioService.getInstance(app)
            val audioEffective = audioToggle &&
                audioService.isAudioDownloaded(lang) &&
                audioService.isChapletAudioDownloaded(lang, "st_michael")
            AnalyticsManager.trackEvent(
                AnalyticsEvent.PRAYER_COMPLETED,
                mapOf(
                    "prayer_type" to "st_michael",
                    "audio_enabled" to audioEffective
                )
            )
            return
        }
        _currentStepIndex.value += 1
    }

    fun peekNextStep(): StMichaelPrayerStep? {
        val nextIndex = _currentStepIndex.value + 1
        return if (nextIndex < allSteps.size) allSteps[nextIndex] else null
    }

    fun goToPreviousStep() {
        if (_currentStepIndex.value > 0) {
            _currentStepIndex.value -= 1
            _isComplete.value = false
        }
    }

    fun reset() {
        _currentStepIndex.value = 0
        _isComplete.value = false
        allSteps = emptyList()
    }

    private fun buildAllSteps(): List<StMichaelPrayerStep> = buildList {
        add(StMichaelPrayerStep.Intro)

        // Opening prayers
        add(StMichaelPrayerStep.SignOfTheCross)
        add(StMichaelPrayerStep.OpeningPrayer)

        // 9 Salutations
        for (salutation in 1..9) {
            add(StMichaelPrayerStep.SalutationAnnouncement(salutation))
            add(StMichaelPrayerStep.SalutationOurFather(salutation))
            for (count in 1..3) {
                add(StMichaelPrayerStep.SalutationHailMary(salutation, count))
            }
            add(StMichaelPrayerStep.SalutationGloryBe(salutation))
        }

        // 4 Archangel prayers
        for (archangel in 1..4) {
            add(StMichaelPrayerStep.ArchangelAnnouncement(archangel))
            add(StMichaelPrayerStep.ArchangelOurFather(archangel))
        }

        // Closing prayers
        add(StMichaelPrayerStep.ClosingPrayer)
        add(StMichaelPrayerStep.FinalPrayer)
        add(StMichaelPrayerStep.ClosingSignOfTheCross)
    }
}
