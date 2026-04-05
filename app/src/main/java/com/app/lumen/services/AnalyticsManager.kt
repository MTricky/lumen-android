package com.app.lumen.services

import android.content.Context
import com.mixpanel.android.mpmetrics.MixpanelAPI
import com.revenuecat.purchases.Purchases
import org.json.JSONObject

/**
 * Centralized analytics manager wrapping Mixpanel.
 * Mirrors the iOS AnalyticsManager 1:1.
 *
 * Only tracks in production (non-debug) builds.
 */
object AnalyticsManager {

    private const val MIXPANEL_TOKEN = "1b3b8fc7c3bf05d9afbd4527e34c5010"

    private var mixpanel: MixpanelAPI? = null
    private var isProduction = false

    /**
     * Initialize Mixpanel. Call from Application.onCreate().
     */
    fun configure(context: Context) {
        isProduction = !com.app.lumen.BuildConfig.DEBUG
        if (isProduction) {
            mixpanel = MixpanelAPI.getInstance(context, MIXPANEL_TOKEN, false)
            identifyUser(context)
        }
    }

    private fun identifyUser(context: Context) {
        if (!isProduction) return
        val deviceId = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )
        if (deviceId != null) {
            mixpanel?.identify(deviceId)
            // Sync Mixpanel distinct ID with RevenueCat for attribution (matching iOS)
            try {
                Purchases.sharedInstance.setMixpanelDistinctID(deviceId)
            } catch (_: Exception) {
                // RevenueCat may not be configured yet
            }
        }
    }

    /**
     * Set the $premium people property (mirrors iOS).
     */
    fun setUserPremiumProperty(isPremium: Boolean) {
        if (!isProduction) return
        mixpanel?.people?.set("premium", isPremium)
    }

    /**
     * Track an analytics event with optional properties.
     */
    fun trackEvent(event: AnalyticsEvent, properties: Map<String, Any>? = null) {
        if (!isProduction) return
        val props = properties?.let {
            JSONObject().apply {
                it.forEach { (key, value) -> put(key, value) }
            }
        }
        mixpanel?.track(event.eventName, props)
    }
}

/**
 * All analytics events — matches iOS AnalyticsEvent enum exactly.
 */
enum class AnalyticsEvent(val eventName: String) {
    // Onboarding
    STARTED_ONBOARDING("started_onboarding"),
    FINISHED_ONBOARDING("finished_onboarding"),
    FEEDBACK_POSITIVE("feedback_positive"),
    FEEDBACK_NEUTRAL("feedback_neutral"),
    FEEDBACK_NEGATIVE("feedback_negative"),

    // Routines
    ROUTINE_CREATED("routine_created"),
    ROUTINE_CREATED_ONBOARDING("routine_created_onboarding"),
    ROUTINE_COMPLETED("routine_completed"),

    // Notes
    NOTE_CREATED("note_created"),

    // Reminders
    REMINDER_CREATED("reminder_created"),

    // Survey
    SURVEY_COMPLETED("survey_completed"),
    SURVEY_ALERT_INTERACTED("survey_alert_interacted"),
    REVIEW_BUTTON_CLICKED("review_button_clicked"),

    // Promo
    PROMO_OFFER_SHOWN("promo_offer_shown"),

    // Completion Review (post-prayer)
    COMPLETION_REVIEW_POSITIVE("completion_review_positive"),
    COMPLETION_REVIEW_NEUTRAL("completion_review_neutral"),
    COMPLETION_REVIEW_NEGATIVE("completion_review_negative"),

    // Prayers
    PRAYER_STARTED("prayer_started"),
    PRAYER_COMPLETED("prayer_completed"),

    // Rosary
    ROSARY_ONBOARDING_COMPLETED("rosary_onboarding_completed"),
    ROSARY_AUDIO_DOWNLOADED("rosary_audio_downloaded"),
}
