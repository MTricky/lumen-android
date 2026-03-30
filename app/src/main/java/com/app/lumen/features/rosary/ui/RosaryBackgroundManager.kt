package com.app.lumen.features.rosary.ui

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.app.lumen.R
import com.app.lumen.features.rosary.model.MysteryType
import com.app.lumen.features.rosary.model.RosaryPrayerStep

enum class RosaryVisualMode(val key: String, @StringRes val displayNameRes: Int) {
    SACRED_ART("Sacred Art", R.string.settings_prayer_visual_sacred_art),
    SIMPLE("Simple", R.string.settings_prayer_visual_simple);

    companion object {
        private const val PREFS_NAME = "rosary_prefs"
        private const val KEY = "visual_style"

        fun current(context: Context): RosaryVisualMode {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val stored = prefs.getString(KEY, SACRED_ART.key) ?: SACRED_ART.key
            return entries.firstOrNull { it.key == stored } ?: SACRED_ART
        }
    }
}

object RosaryBackgroundManager {

    /**
     * Returns the background drawable for the current prayer step, or null if visual mode is Simple.
     */
    @DrawableRes
    fun background(step: RosaryPrayerStep, mysteryType: MysteryType?, visualMode: RosaryVisualMode = RosaryVisualMode.SACRED_ART): Int? {
        if (visualMode == RosaryVisualMode.SIMPLE) return null
        val type = mysteryType ?: return introImage(MysteryType.JOYFUL)

        return when (step) {
            is RosaryPrayerStep.Intro,
            is RosaryPrayerStep.SignOfTheCross,
            is RosaryPrayerStep.ApostlesCreed,
            is RosaryPrayerStep.IntroOurFather,
            is RosaryPrayerStep.IntroHailMary,
            is RosaryPrayerStep.IntroGloryBe -> introImage(type)

            is RosaryPrayerStep.MysteryAnnouncement -> decadeImage(type, step.decade)
            is RosaryPrayerStep.DecadeOurFather -> decadeImage(type, step.decade)
            is RosaryPrayerStep.DecadeHailMary -> decadeImage(type, step.decade)
            is RosaryPrayerStep.DecadeGloryBe -> decadeImage(type, step.decade)
            is RosaryPrayerStep.DecadeFatimaPrayer -> decadeImage(type, step.decade)

            is RosaryPrayerStep.HailHolyQueen,
            is RosaryPrayerStep.FinalPrayer,
            is RosaryPrayerStep.ClosingSignOfTheCross -> introImage(type)
        }
    }

    @DrawableRes
    private fun introImage(type: MysteryType): Int = when (type) {
        MysteryType.JOYFUL -> R.drawable.rosary_joyful_intro
        MysteryType.SORROWFUL -> R.drawable.rosary_sorrowful_intro
        MysteryType.GLORIOUS -> R.drawable.rosary_glorious_intro
        MysteryType.LUMINOUS -> R.drawable.rosary_luminous_intro
    }

    @DrawableRes
    private fun decadeImage(type: MysteryType, decade: Int): Int {
        val index = (decade - 1).coerceIn(0, 4)
        return when (type) {
            MysteryType.JOYFUL -> joyfulImages[index]
            MysteryType.SORROWFUL -> sorrowfulImages[index]
            MysteryType.GLORIOUS -> gloriousImages[index]
            MysteryType.LUMINOUS -> luminousImages[index]
        }
    }

    private val joyfulImages = intArrayOf(
        R.drawable.rosary_joyful_annunciation,
        R.drawable.rosary_joyful_visitation,
        R.drawable.rosary_joyful_nativity,
        R.drawable.rosary_joyful_presentation,
        R.drawable.rosary_joyful_finding,
    )

    private val sorrowfulImages = intArrayOf(
        R.drawable.rosary_sorrowful_agony,
        R.drawable.rosary_sorrowful_scourging,
        R.drawable.rosary_sorrowful_crowning,
        R.drawable.rosary_sorrowful_carrying,
        R.drawable.rosary_sorrowful_crucifixion,
    )

    private val gloriousImages = intArrayOf(
        R.drawable.rosary_glorious_resurrection,
        R.drawable.rosary_glorious_ascension,
        R.drawable.rosary_glorious_descent,
        R.drawable.rosary_glorious_assumption,
        R.drawable.rosary_glorious_coronation,
    )

    private val luminousImages = intArrayOf(
        R.drawable.rosary_luminous_baptism,
        R.drawable.rosary_luminous_wedding,
        R.drawable.rosary_luminous_proclamation,
        R.drawable.rosary_luminous_transfiguration,
        R.drawable.rosary_luminous_eucharist,
    )
}
