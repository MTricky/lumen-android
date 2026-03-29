package com.app.lumen.features.chaplets.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.app.lumen.features.chaplets.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import java.util.Locale

class DivineMercyViewModel(application: Application) : AndroidViewModel(application) {

    private val json = Json { ignoreUnknownKeys = true }

    private val _prayerData = MutableStateFlow<DivineMercyPrayerData?>(null)
    val prayerData: StateFlow<DivineMercyPrayerData?> = _prayerData

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _currentStepIndex = MutableStateFlow(0)
    val currentStepIndex: StateFlow<Int> = _currentStepIndex

    private val _isComplete = MutableStateFlow(false)
    val isComplete: StateFlow<Boolean> = _isComplete

    private var allSteps: List<DivineMercyPrayerStep> = emptyList()

    val currentStep: DivineMercyPrayerStep?
        get() {
            val idx = _currentStepIndex.value
            return if (idx < allSteps.size) allSteps[idx] else null
        }

    val currentPrayer: Prayer?
        get() {
            val step = currentStep ?: return null
            val prayers = _prayerData.value?.prayers ?: return null
            return when (step) {
                is DivineMercyPrayerStep.SignOfTheCross,
                is DivineMercyPrayerStep.ClosingSignOfTheCross -> prayers.signOfTheCross
                is DivineMercyPrayerStep.OpeningPrayer -> prayers.openingPrayer
                is DivineMercyPrayerStep.OurFather -> prayers.ourFather
                is DivineMercyPrayerStep.HailMary -> prayers.hailMary
                is DivineMercyPrayerStep.ApostlesCreed -> prayers.apostlesCreed
                is DivineMercyPrayerStep.EternalFather -> prayers.eternalFather
                is DivineMercyPrayerStep.ForTheSake -> prayers.forTheSake
                is DivineMercyPrayerStep.HolyGod -> prayers.holyGod
                is DivineMercyPrayerStep.ClosingPrayer -> prayers.closingPrayer
                is DivineMercyPrayerStep.Intro,
                is DivineMercyPrayerStep.DecadeAnnouncement -> null
            }
        }

    val currentDecade: DivineMercyDecade?
        get() {
            val step = currentStep ?: return null
            val decades = _prayerData.value?.decades ?: return null
            val decadeNum = step.getDecade() ?: return null
            return if (decadeNum in 1..decades.size) decades[decadeNum - 1] else null
        }

    val progress: DivineMercyProgress?
        get() {
            val step = currentStep ?: return null
            return DivineMercyProgress(
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
            val path = "prayers/chaplets/divinemercy/divinemercy_$lang.json"
            val jsonString = try {
                getApplication<Application>().assets.open(path)
            } catch (_: Exception) {
                getApplication<Application>().assets.open("prayers/chaplets/divinemercy/divinemercy_en.json")
            }.bufferedReader().use { it.readText() }
            _prayerData.value = json.decodeFromString<DivineMercyPrayerData>(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _isLoading.value = false
    }

    private fun prayerLanguageCode(): String {
        val lang = Locale.getDefault().language
        return when {
            lang.startsWith("pl") -> "pl"
            else -> "en"
        }
    }

    fun startChaplet() {
        _currentStepIndex.value = 0
        _isComplete.value = false
        allSteps = buildAllSteps()
    }

    fun advanceToNextStep() {
        if (_currentStepIndex.value >= allSteps.size - 1) {
            _isComplete.value = true
            return
        }
        _currentStepIndex.value += 1
    }

    fun peekNextStep(): DivineMercyPrayerStep? {
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

    private fun buildAllSteps(): List<DivineMercyPrayerStep> = buildList {
        add(DivineMercyPrayerStep.Intro)

        // Opening prayers
        add(DivineMercyPrayerStep.SignOfTheCross)
        add(DivineMercyPrayerStep.OpeningPrayer)

        // Opening prayers on the rosary beads
        add(DivineMercyPrayerStep.OurFather)
        add(DivineMercyPrayerStep.HailMary)
        add(DivineMercyPrayerStep.ApostlesCreed)

        // 5 Decades
        for (decade in 1..5) {
            add(DivineMercyPrayerStep.DecadeAnnouncement(decade))
            add(DivineMercyPrayerStep.EternalFather(decade))
            for (count in 1..10) {
                add(DivineMercyPrayerStep.ForTheSake(decade, count))
            }
        }

        // Closing — Holy God x3
        for (count in 1..3) {
            add(DivineMercyPrayerStep.HolyGod(count))
        }

        // Final prayers
        add(DivineMercyPrayerStep.ClosingPrayer)
        add(DivineMercyPrayerStep.ClosingSignOfTheCross)
    }
}
