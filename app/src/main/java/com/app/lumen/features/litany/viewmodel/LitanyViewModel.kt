package com.app.lumen.features.litany.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.app.lumen.features.chaplets.model.Prayer
import com.app.lumen.features.litany.model.*
import com.app.lumen.services.AnalyticsEvent
import com.app.lumen.services.AnalyticsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json

class LitanyViewModel(application: Application) : AndroidViewModel(application) {

    private val json = Json { ignoreUnknownKeys = true }

    private val _prayerData = MutableStateFlow<LitanyPrayerData?>(null)
    val prayerData: StateFlow<LitanyPrayerData?> = _prayerData

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _currentStepIndex = MutableStateFlow(0)
    val currentStepIndex: StateFlow<Int> = _currentStepIndex

    private val _isComplete = MutableStateFlow(false)
    val isComplete: StateFlow<Boolean> = _isComplete

    private var allSteps: List<LitanyPrayerStep> = emptyList()
    private var currentLitanyType: LitanyType? = null

    val currentStep: LitanyPrayerStep?
        get() {
            val idx = _currentStepIndex.value
            return if (idx < allSteps.size) allSteps[idx] else null
        }

    val currentPrayer: Prayer?
        get() {
            val step = currentStep ?: return null
            val prayers = _prayerData.value?.prayers ?: return null
            return when (step) {
                is LitanyPrayerStep.SignOfTheCross,
                is LitanyPrayerStep.ClosingSignOfTheCross -> prayers.signOfTheCross
                is LitanyPrayerStep.ClosingPrayer -> prayers.closingPrayer
                is LitanyPrayerStep.Intro,
                is LitanyPrayerStep.Invocation -> null
            }
        }

    val currentInvocation: LitanyInvocation?
        get() {
            val step = currentStep ?: return null
            val sections = _prayerData.value?.sections ?: return null
            if (step is LitanyPrayerStep.Invocation) {
                val section = sections.getOrNull(step.sectionIndex) ?: return null
                return section.invocations.getOrNull(step.invocationIndex)
            }
            return null
        }

    val currentSection: LitanySection?
        get() {
            val step = currentStep ?: return null
            val sections = _prayerData.value?.sections ?: return null
            if (step is LitanyPrayerStep.Invocation) {
                return sections.getOrNull(step.sectionIndex)
            }
            return null
        }

    val currentResponseText: String?
        get() {
            val invocation = currentInvocation ?: return null
            val section = currentSection ?: return null
            // Per-invocation response takes priority (kyrie, lambOfGod, versicle)
            invocation.response?.let { return it }
            // Shared section response (petition sections)
            return section.response
        }

    val currentSectionTitle: String?
        get() = currentSection?.title

    val currentInvocationIndexInSection: Int?
        get() {
            val step = currentStep ?: return null
            if (step is LitanyPrayerStep.Invocation) return step.invocationIndex
            return null
        }

    val currentSectionInvocationCount: Int?
        get() = currentSection?.invocations?.size

    val currentSectionType: LitanySectionType?
        get() {
            val section = currentSection ?: return null
            return LitanySectionType.from(section.type)
        }

    val progress: LitanyProgress?
        get() {
            val step = currentStep ?: return null
            return LitanyProgress(
                currentStep = step,
                totalSteps = allSteps.size,
                currentStepIndex = _currentStepIndex.value,
                sectionTitle = currentSectionTitle,
                sectionInvocationIndex = currentInvocationIndexInSection,
                sectionInvocationCount = currentSectionInvocationCount,
                sectionType = currentSectionType,
                sectionResponse = currentSection?.response,
            )
        }

    private var loadedLanguage: String? = null

    fun loadPrayers(litanyType: LitanyType) {
        val currentLang = LitanyType.prayerLanguageCode()
        if (_prayerData.value != null && currentLitanyType == litanyType && loadedLanguage == currentLang) return
        if (_isLoading.value) return
        _isLoading.value = true
        currentLitanyType = litanyType
        try {
            val jsonString = try {
                getApplication<Application>().assets.open(litanyType.jsonPath)
            } catch (_: Exception) {
                getApplication<Application>().assets.open(litanyType.jsonPathFallback)
            }.bufferedReader().use { it.readText() }
            _prayerData.value = json.decodeFromString<LitanyPrayerData>(jsonString)
            loadedLanguage = currentLang
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _isLoading.value = false
    }

    fun startLitany() {
        _currentStepIndex.value = 0
        _isComplete.value = false
        allSteps = buildAllSteps()

        // Track prayer started (matching iOS) — dynamic prayer_type based on litany type
        val litanyTypeName = currentLitanyType?.name?.lowercase() ?: "unknown"
        AnalyticsManager.trackEvent(
            AnalyticsEvent.PRAYER_STARTED,
            mapOf("prayer_type" to "litany_$litanyTypeName")
        )
    }

    fun advanceToNextStep() {
        if (_isComplete.value) return // Prevent duplicate tracking
        if (_currentStepIndex.value >= allSteps.size - 1) {
            _isComplete.value = true

            // Track prayer completed (matching iOS)
            val litanyTypeName = currentLitanyType?.name?.lowercase() ?: "unknown"
            AnalyticsManager.trackEvent(
                AnalyticsEvent.PRAYER_COMPLETED,
                mapOf(
                    "prayer_type" to "litany_$litanyTypeName",
                    "audio_enabled" to false // Litanies don't have audio
                )
            )
            return
        }
        _currentStepIndex.value += 1
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
        _prayerData.value = null
        currentLitanyType = null
        loadedLanguage = null
    }

    private fun buildAllSteps(): List<LitanyPrayerStep> = buildList {
        val sections = _prayerData.value?.sections ?: return@buildList

        add(LitanyPrayerStep.Intro)
        add(LitanyPrayerStep.SignOfTheCross)

        for ((sectionIndex, section) in sections.withIndex()) {
            for (invocationIndex in section.invocations.indices) {
                add(LitanyPrayerStep.Invocation(sectionIndex, invocationIndex))
            }
        }

        add(LitanyPrayerStep.ClosingPrayer)
        add(LitanyPrayerStep.ClosingSignOfTheCross)
    }
}
