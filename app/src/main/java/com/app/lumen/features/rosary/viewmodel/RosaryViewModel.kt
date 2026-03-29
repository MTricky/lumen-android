package com.app.lumen.features.rosary.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.app.lumen.features.rosary.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import java.util.Locale

class RosaryViewModel(application: Application) : AndroidViewModel(application) {

    private val json = Json { ignoreUnknownKeys = true }

    private val _prayerData = MutableStateFlow<PrayerData?>(null)
    val prayerData: StateFlow<PrayerData?> = _prayerData

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _selectedMysteryType = MutableStateFlow<MysteryType?>(null)
    val selectedMysteryType: StateFlow<MysteryType?> = _selectedMysteryType

    private val _currentStepIndex = MutableStateFlow(0)
    val currentStepIndex: StateFlow<Int> = _currentStepIndex

    private val _isComplete = MutableStateFlow(false)
    val isComplete: StateFlow<Boolean> = _isComplete

    private var allSteps: List<RosaryPrayerStep> = emptyList()

    val currentStep: RosaryPrayerStep?
        get() {
            val idx = _currentStepIndex.value
            return if (idx < allSteps.size) allSteps[idx] else null
        }

    val currentPrayer: Prayer?
        get() {
            val step = currentStep ?: return null
            val prayers = _prayerData.value?.prayers ?: return null
            return when (step) {
                is RosaryPrayerStep.SignOfTheCross, is RosaryPrayerStep.ClosingSignOfTheCross -> prayers.signOfTheCross
                is RosaryPrayerStep.ApostlesCreed -> prayers.apostlesCreed
                is RosaryPrayerStep.IntroOurFather, is RosaryPrayerStep.DecadeOurFather -> prayers.ourFather
                is RosaryPrayerStep.IntroHailMary -> when (step.virtue) {
                    IntroVirtue.FAITH -> prayers.introHailMaryFaith
                    IntroVirtue.HOPE -> prayers.introHailMaryHope
                    IntroVirtue.CHARITY -> prayers.introHailMaryCharity
                }
                is RosaryPrayerStep.DecadeHailMary -> prayers.hailMary
                is RosaryPrayerStep.IntroGloryBe, is RosaryPrayerStep.DecadeGloryBe -> prayers.gloryBe
                is RosaryPrayerStep.DecadeFatimaPrayer -> prayers.fatimaPrayer
                is RosaryPrayerStep.HailHolyQueen -> prayers.hailHolyQueen
                is RosaryPrayerStep.FinalPrayer -> prayers.finalPrayer
                is RosaryPrayerStep.Intro, is RosaryPrayerStep.MysteryAnnouncement -> null
            }
        }

    val currentMystery: Mystery?
        get() {
            val step = currentStep ?: return null
            val mysteryType = _selectedMysteryType.value ?: return null
            val mysteries = _prayerData.value?.mysteries ?: return null
            val decade = step.getDecade() ?: return null
            val mysteryList = mysteries.forType(mysteryType)
            return if (decade in 1..mysteryList.size) mysteryList[decade - 1] else null
        }

    val progress: RosaryProgress?
        get() {
            val step = currentStep ?: return null
            return RosaryProgress(
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
            val path = "prayers/rosary/prayers_$lang.json"
            val jsonString = try {
                getApplication<Application>().assets.open(path)
            } catch (_: Exception) {
                getApplication<Application>().assets.open("prayers/rosary/prayers_en.json")
            }.bufferedReader().use { it.readText() }
            _prayerData.value = json.decodeFromString<PrayerData>(jsonString)
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
            else -> "en"
        }
    }

    fun startRosary(mysteryType: MysteryType) {
        _selectedMysteryType.value = mysteryType
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

    fun peekNextStep(): RosaryPrayerStep? {
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
        _selectedMysteryType.value = null
        _currentStepIndex.value = 0
        _isComplete.value = false
        allSteps = emptyList()
    }

    private fun buildAllSteps(): List<RosaryPrayerStep> = buildList {
        add(RosaryPrayerStep.Intro)
        add(RosaryPrayerStep.SignOfTheCross)
        add(RosaryPrayerStep.ApostlesCreed)
        add(RosaryPrayerStep.IntroOurFather)

        IntroVirtue.entries.forEach { virtue ->
            add(RosaryPrayerStep.IntroHailMary(virtue))
        }

        add(RosaryPrayerStep.IntroGloryBe)

        for (decade in 1..5) {
            add(RosaryPrayerStep.MysteryAnnouncement(decade))
            add(RosaryPrayerStep.DecadeOurFather(decade))
            for (hailMary in 1..10) {
                add(RosaryPrayerStep.DecadeHailMary(decade, hailMary))
            }
            add(RosaryPrayerStep.DecadeGloryBe(decade))
            add(RosaryPrayerStep.DecadeFatimaPrayer(decade))
        }

        add(RosaryPrayerStep.HailHolyQueen)
        add(RosaryPrayerStep.FinalPrayer)
        add(RosaryPrayerStep.ClosingSignOfTheCross)
    }
}
