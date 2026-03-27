package com.app.lumen.features.chaplets.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.app.lumen.R

enum class ChapletType(
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    @StringRes val durationRes: Int,
    @DrawableRes val previewImageRes: Int,
) {
    DIVINE_MERCY(
        titleRes = R.string.chaplet_divine_mercy_title,
        subtitleRes = R.string.chaplet_divine_mercy_subtitle,
        durationRes = R.string.chaplet_divine_mercy_duration,
        previewImageRes = R.drawable.chaplet_preview_divine_mercy,
    ),
    ST_MICHAEL(
        titleRes = R.string.chaplet_st_michael_title,
        subtitleRes = R.string.chaplet_st_michael_subtitle,
        durationRes = R.string.chaplet_st_michael_duration,
        previewImageRes = R.drawable.chaplet_preview_st_michael,
    ),
    SEVEN_SORROWS(
        titleRes = R.string.chaplet_seven_sorrows_title,
        subtitleRes = R.string.chaplet_seven_sorrows_subtitle,
        durationRes = R.string.chaplet_seven_sorrows_duration,
        previewImageRes = R.drawable.chaplet_preview_seven_sorrows,
    ),
}
