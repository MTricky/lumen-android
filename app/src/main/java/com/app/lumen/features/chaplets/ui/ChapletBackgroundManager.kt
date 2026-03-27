package com.app.lumen.features.chaplets.ui

import androidx.annotation.DrawableRes
import com.app.lumen.R
import com.app.lumen.features.chaplets.model.*
import com.app.lumen.features.rosary.ui.RosaryVisualMode

object ChapletBackgroundManager {

    // --- Divine Mercy ---

    @DrawableRes
    fun background(step: DivineMercyPrayerStep, visualMode: RosaryVisualMode): Int? {
        if (visualMode == RosaryVisualMode.SIMPLE) return null
        return when (step) {
            is DivineMercyPrayerStep.Intro,
            is DivineMercyPrayerStep.SignOfTheCross,
            is DivineMercyPrayerStep.OpeningPrayer,
            is DivineMercyPrayerStep.OurFather,
            is DivineMercyPrayerStep.HailMary,
            is DivineMercyPrayerStep.ApostlesCreed -> R.drawable.chaplet_divine_mercy_opening

            is DivineMercyPrayerStep.DecadeAnnouncement -> divineMercyDecadeImage(step.decade)
            is DivineMercyPrayerStep.EternalFather -> divineMercyDecadeImage(step.decade)
            is DivineMercyPrayerStep.ForTheSake -> divineMercyDecadeImage(step.decade)

            is DivineMercyPrayerStep.HolyGod,
            is DivineMercyPrayerStep.ClosingPrayer,
            is DivineMercyPrayerStep.ClosingSignOfTheCross -> R.drawable.chaplet_divine_mercy_closing
        }
    }

    @DrawableRes
    private fun divineMercyDecadeImage(decade: Int): Int = when (decade) {
        1 -> R.drawable.chaplet_divine_mercy_decade1
        2 -> R.drawable.chaplet_divine_mercy_decade2
        3 -> R.drawable.chaplet_divine_mercy_decade3
        4 -> R.drawable.chaplet_divine_mercy_decade4
        5 -> R.drawable.chaplet_divine_mercy_decade5
        else -> R.drawable.chaplet_divine_mercy_opening
    }

    // --- St. Michael ---

    @DrawableRes
    fun background(step: StMichaelPrayerStep, visualMode: RosaryVisualMode): Int? {
        if (visualMode == RosaryVisualMode.SIMPLE) return null
        return R.drawable.chaplet_bg_st_michael
    }

    // --- Seven Sorrows ---

    @DrawableRes
    fun background(step: SevenSorrowsPrayerStep, visualMode: RosaryVisualMode): Int? {
        if (visualMode == RosaryVisualMode.SIMPLE) return null
        return R.drawable.chaplet_bg_seven_sorrows
    }
}
