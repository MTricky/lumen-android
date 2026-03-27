package com.app.lumen.features.chaplets.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.app.lumen.features.chaplets.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json

class SevenSorrowsViewModel(application: Application) : AndroidViewModel(application) {

    private val json = Json { ignoreUnknownKeys = true }

    private val _prayerData = MutableStateFlow<SevenSorrowsPrayerData?>(null)
    val prayerData: StateFlow<SevenSorrowsPrayerData?> = _prayerData

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _currentStepIndex = MutableStateFlow(0)
    val currentStepIndex: StateFlow<Int> = _currentStepIndex

    private val _isComplete = MutableStateFlow(false)
    val isComplete: StateFlow<Boolean> = _isComplete

    private var allSteps: List<SevenSorrowsPrayerStep> = emptyList()

    val currentStep: SevenSorrowsPrayerStep?
        get() {
            val idx = _currentStepIndex.value
            return if (idx < allSteps.size) allSteps[idx] else null
        }

    val currentPrayer: Prayer?
        get() {
            val step = currentStep ?: return null
            val prayers = _prayerData.value?.prayers ?: return null
            return when (step) {
                is SevenSorrowsPrayerStep.SignOfTheCross,
                is SevenSorrowsPrayerStep.ClosingSignOfTheCross -> prayers.signOfTheCross
                is SevenSorrowsPrayerStep.IntroductoryPrayer -> prayers.introductoryPrayer
                is SevenSorrowsPrayerStep.ActOfContrition -> prayers.actOfContrition
                is SevenSorrowsPrayerStep.SorrowOurFather -> prayers.ourFather
                is SevenSorrowsPrayerStep.SorrowHailMary -> prayers.hailMary
                is SevenSorrowsPrayerStep.SorrowfulMotherPrayer -> prayers.sorrowfulMotherPrayer
                is SevenSorrowsPrayerStep.ClosingPrayer -> prayers.closingPrayer
                is SevenSorrowsPrayerStep.FinalInvocation -> prayers.finalInvocation
                is SevenSorrowsPrayerStep.Versicle -> prayers.versicle
                is SevenSorrowsPrayerStep.Response -> prayers.response
                is SevenSorrowsPrayerStep.ConcludingPrayer -> prayers.concludingPrayer
                is SevenSorrowsPrayerStep.Intro,
                is SevenSorrowsPrayerStep.SorrowAnnouncement -> null
            }
        }

    val currentSorrow: SevenSorrowsSorrow?
        get() {
            val step = currentStep ?: return null
            val sorrows = _prayerData.value?.sorrows ?: return null
            val sorrowNum = step.getSorrow() ?: return null
            return if (sorrowNum in 1..sorrows.size) sorrows[sorrowNum - 1] else null
        }

    val progress: SevenSorrowsProgress?
        get() {
            val step = currentStep ?: return null
            return SevenSorrowsProgress(
                currentStep = step,
                totalSteps = allSteps.size,
                currentStepIndex = _currentStepIndex.value,
            )
        }

    fun loadPrayers() {
        if (_prayerData.value != null || _isLoading.value) return
        _isLoading.value = true
        try {
            val jsonString = getApplication<Application>().assets
                .open("prayers/chaplets/sevensorrows/sevensorrows_en.json")
                .bufferedReader()
                .use { it.readText() }
            _prayerData.value = json.decodeFromString<SevenSorrowsPrayerData>(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _isLoading.value = false
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

    fun peekNextStep(): SevenSorrowsPrayerStep? {
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

    private fun buildAllSteps(): List<SevenSorrowsPrayerStep> = buildList {
        add(SevenSorrowsPrayerStep.Intro)

        // Opening prayers
        add(SevenSorrowsPrayerStep.SignOfTheCross)
        add(SevenSorrowsPrayerStep.IntroductoryPrayer)
        add(SevenSorrowsPrayerStep.ActOfContrition)

        // 7 Sorrows
        for (sorrow in 1..7) {
            add(SevenSorrowsPrayerStep.SorrowAnnouncement(sorrow))
            add(SevenSorrowsPrayerStep.SorrowOurFather(sorrow))
            for (count in 1..7) {
                add(SevenSorrowsPrayerStep.SorrowHailMary(sorrow, count))
            }
            add(SevenSorrowsPrayerStep.SorrowfulMotherPrayer(sorrow))
        }

        // Closing prayers
        add(SevenSorrowsPrayerStep.ClosingPrayer)
        for (count in 1..3) {
            add(SevenSorrowsPrayerStep.FinalInvocation(count))
        }
        add(SevenSorrowsPrayerStep.Versicle)
        add(SevenSorrowsPrayerStep.Response)
        add(SevenSorrowsPrayerStep.ConcludingPrayer)
        add(SevenSorrowsPrayerStep.ClosingSignOfTheCross)
    }
}
