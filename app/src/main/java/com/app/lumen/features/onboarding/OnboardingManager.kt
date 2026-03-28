package com.app.lumen.features.onboarding

import android.content.Context
import android.content.SharedPreferences

enum class OnboardingFeedbackResult(val value: String) {
    POSITIVE("positive"),
    NEUTRAL("neutral"),
    NEGATIVE("negative"),
    NOT_DISPLAYED("notDisplayed"),
    FEEDBACK_DISABLED("feedbackDisabled");

    companion object {
        fun fromValue(value: String?): OnboardingFeedbackResult? =
            entries.firstOrNull { it.value == value }
    }
}

class OnboardingManager private constructor(private val prefs: SharedPreferences) {

    companion object {
        private const val PREFS_NAME = "onboarding_prefs"
        private const val KEY_HAS_COMPLETED = "onboarding.hasCompleted"
        private const val KEY_HAS_ASKED_FEEDBACK = "onboarding.hasAskedForFeedback"
        private const val KEY_FEEDBACK_RESULT = "onboarding.feedbackResult"
        private const val KEY_HAS_SHOWN_COMPLETION_REVIEW = "onboarding.hasShownCompletionReview"

        @Volatile
        private var instance: OnboardingManager? = null

        fun initialize(context: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = OnboardingManager(
                            context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        )
                    }
                }
            }
        }

        val shared: OnboardingManager
            get() = instance ?: throw IllegalStateException("OnboardingManager not initialized. Call initialize() first.")
    }

    val hasCompletedOnboarding: Boolean
        get() = prefs.getBoolean(KEY_HAS_COMPLETED, false)

    val hasAskedForFeedback: Boolean
        get() = prefs.getBoolean(KEY_HAS_ASKED_FEEDBACK, false)

    val onboardingFeedbackResult: OnboardingFeedbackResult?
        get() = OnboardingFeedbackResult.fromValue(prefs.getString(KEY_FEEDBACK_RESULT, null))

    val hasShownCompletionReview: Boolean
        get() = prefs.getBoolean(KEY_HAS_SHOWN_COMPLETION_REVIEW, false)

    val shouldShowCompletionReview: Boolean
        get() {
            if (hasShownCompletionReview) return false
            val result = onboardingFeedbackResult ?: return false
            return when (result) {
                OnboardingFeedbackResult.POSITIVE -> false
                else -> true
            }
        }

    fun completeOnboarding() {
        prefs.edit().putBoolean(KEY_HAS_COMPLETED, true).apply()
    }

    fun markFeedbackAsked() {
        prefs.edit().putBoolean(KEY_HAS_ASKED_FEEDBACK, true).apply()
    }

    fun saveFeedbackResult(result: OnboardingFeedbackResult) {
        prefs.edit().putString(KEY_FEEDBACK_RESULT, result.value).apply()
    }

    fun markCompletionReviewShown() {
        prefs.edit().putBoolean(KEY_HAS_SHOWN_COMPLETION_REVIEW, true).apply()
    }

    fun resetOnboarding() {
        prefs.edit()
            .putBoolean(KEY_HAS_COMPLETED, false)
            .putBoolean(KEY_HAS_ASKED_FEEDBACK, false)
            .remove(KEY_FEEDBACK_RESULT)
            .putBoolean(KEY_HAS_SHOWN_COMPLETION_REVIEW, false)
            .apply()
    }
}
