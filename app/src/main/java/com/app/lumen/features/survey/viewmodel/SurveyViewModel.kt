package com.app.lumen.features.survey.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.app.lumen.features.survey.model.AgeRange
import com.app.lumen.features.survey.model.Gender
import com.app.lumen.features.survey.model.SatisfactionLevel
import com.app.lumen.features.survey.model.SurveyStep
import com.app.lumen.services.AnalyticsEvent
import com.app.lumen.services.AnalyticsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SurveyViewModel(application: Application) : AndroidViewModel(application) {

    private val _currentStep = MutableStateFlow(SurveyStep.WELCOME)
    val currentStep: StateFlow<SurveyStep> = _currentStep.asStateFlow()

    private val _selectedAgeRange = MutableStateFlow<AgeRange?>(null)
    val selectedAgeRange: StateFlow<AgeRange?> = _selectedAgeRange.asStateFlow()

    private val _selectedGender = MutableStateFlow<Gender?>(null)
    val selectedGender: StateFlow<Gender?> = _selectedGender.asStateFlow()

    private val _featureRequest = MutableStateFlow("")
    val featureRequest: StateFlow<String> = _featureRequest.asStateFlow()

    private val _selectedSatisfaction = MutableStateFlow<SatisfactionLevel?>(null)
    val selectedSatisfaction: StateFlow<SatisfactionLevel?> = _selectedSatisfaction.asStateFlow()

    val canProceedFromAboutYou: Boolean
        get() = _selectedAgeRange.value != null && _selectedGender.value != null

    val canProceedFromSatisfaction: Boolean
        get() = _selectedSatisfaction.value != null

    val showReviewPrompt: Boolean
        get() = _selectedSatisfaction.value == SatisfactionLevel.FULLY_SATISFIED

    fun selectAgeRange(age: AgeRange) { _selectedAgeRange.value = age }
    fun selectGender(gender: Gender) { _selectedGender.value = gender }
    fun updateFeatureRequest(text: String) { _featureRequest.value = text }
    fun selectSatisfaction(level: SatisfactionLevel) { _selectedSatisfaction.value = level }

    fun nextStep() {
        _currentStep.value.next()?.let { _currentStep.value = it }
    }

    fun completeSurvey() {
        val prefs = getApplication<Application>().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SURVEY_COMPLETED, true).apply()

        val properties = mutableMapOf<String, Any>()
        _selectedAgeRange.value?.let { properties["age_range"] = it.value }
        _selectedGender.value?.let { properties["gender"] = it.value }
        _selectedSatisfaction.value?.let { properties["satisfaction"] = it.value }
        val trimmed = _featureRequest.value.trim()
        if (trimmed.isNotEmpty()) {
            properties["feature_request"] = trimmed
        }
        AnalyticsManager.trackEvent(AnalyticsEvent.SURVEY_COMPLETED, properties)
    }

    fun trackReviewClicked() {
        AnalyticsManager.trackEvent(AnalyticsEvent.REVIEW_BUTTON_CLICKED)
    }

    companion object {
        private const val PREFS_NAME = "survey_prefs"
        private const val KEY_ONBOARDING_COMPLETED_DATE = "onboardingCompletedDate"
        private const val KEY_SURVEY_COMPLETED = "surveyCompleted"
        private const val KEY_SURVEY_DISMISSED = "surveyDismissed"
        private const val REQUIRED_DAYS_BEFORE_SURVEY = 3
        fun setOnboardingCompletedDateIfNeeded(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (!prefs.contains(KEY_ONBOARDING_COMPLETED_DATE)) {
                prefs.edit().putLong(KEY_ONBOARDING_COMPLETED_DATE, System.currentTimeMillis()).apply()
            }
        }

        fun shouldShowSurvey(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (prefs.getBoolean(KEY_SURVEY_COMPLETED, false)) return false
            if (prefs.getBoolean(KEY_SURVEY_DISMISSED, false)) return false
            val onboardingCompleted = prefs.getLong(KEY_ONBOARDING_COMPLETED_DATE, 0L)
            if (onboardingCompleted == 0L) return false
            val daysSince = (System.currentTimeMillis() - onboardingCompleted) / (1000 * 60 * 60 * 24)
            return daysSince >= REQUIRED_DAYS_BEFORE_SURVEY
        }

        fun dismissSurvey(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_SURVEY_DISMISSED, true).apply()
        }
    }
}
