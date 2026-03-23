package com.app.lumen.features.rosary.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.app.lumen.R
import java.util.Calendar

enum class MysteryType(
    @StringRes val labelRes: Int,
    @StringRes val daysRes: Int,
    @DrawableRes val imageRes: Int,
    val daysOfWeek: Set<Int>,
) {
    JOYFUL(
        labelRes = R.string.rosary_joyful,
        daysRes = R.string.rosary_days_joyful,
        imageRes = R.drawable.mystery_joyful,
        daysOfWeek = setOf(Calendar.MONDAY, Calendar.SATURDAY),
    ),
    SORROWFUL(
        labelRes = R.string.rosary_sorrowful,
        daysRes = R.string.rosary_days_sorrowful,
        imageRes = R.drawable.mystery_sorrowful,
        daysOfWeek = setOf(Calendar.TUESDAY, Calendar.FRIDAY),
    ),
    GLORIOUS(
        labelRes = R.string.rosary_glorious,
        daysRes = R.string.rosary_days_glorious,
        imageRes = R.drawable.mystery_glorious,
        daysOfWeek = setOf(Calendar.WEDNESDAY, Calendar.SUNDAY),
    ),
    LUMINOUS(
        labelRes = R.string.rosary_luminous,
        daysRes = R.string.rosary_days_luminous,
        imageRes = R.drawable.mystery_luminous,
        daysOfWeek = setOf(Calendar.THURSDAY),
    );

    val isTodaysMystery: Boolean
        get() = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) in daysOfWeek

    companion object {
        fun forToday(): MysteryType {
            val weekday = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            return entries.firstOrNull { weekday in it.daysOfWeek } ?: JOYFUL
        }
    }
}
