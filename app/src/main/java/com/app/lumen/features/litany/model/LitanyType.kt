package com.app.lumen.features.litany.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.app.lumen.R

enum class LitanyType(
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    @StringRes val durationRes: Int,
    @DrawableRes val previewImageRes: Int,
    @DrawableRes val backgroundImageRes: Int,
    val audioChapletType: String,
    val jsonPath: String,
) {
    BLESSED_VIRGIN_MARY(
        titleRes = R.string.litany_blessed_virgin_mary_title,
        subtitleRes = R.string.litany_blessed_virgin_mary_subtitle,
        durationRes = R.string.litany_blessed_virgin_mary_duration,
        previewImageRes = R.drawable.litany_preview_blessed_virgin_mary,
        backgroundImageRes = R.drawable.litany_bg_blessed_virgin_mary,
        audioChapletType = "litanyBlessedVirginMary",
        jsonPath = "prayers/litanies/blessedvirginmary/litany_blessed_virgin_mary_en.json",
    ),
    SACRED_HEART(
        titleRes = R.string.litany_sacred_heart_title,
        subtitleRes = R.string.litany_sacred_heart_subtitle,
        durationRes = R.string.litany_sacred_heart_duration,
        previewImageRes = R.drawable.litany_preview_sacred_heart,
        backgroundImageRes = R.drawable.litany_bg_sacred_heart,
        audioChapletType = "litanySacredHeartOfJesus",
        jsonPath = "prayers/litanies/sacredheart/litany_sacred_heart_en.json",
    ),
    ST_JOSEPH(
        titleRes = R.string.litany_st_joseph_title,
        subtitleRes = R.string.litany_st_joseph_subtitle,
        durationRes = R.string.litany_st_joseph_duration,
        previewImageRes = R.drawable.litany_preview_st_joseph,
        backgroundImageRes = R.drawable.litany_bg_st_joseph,
        audioChapletType = "litanyStJoseph",
        jsonPath = "prayers/litanies/stjoseph/litany_st_joseph_en.json",
    ),
    SAINTS(
        titleRes = R.string.litany_saints_title,
        subtitleRes = R.string.litany_saints_subtitle,
        durationRes = R.string.litany_saints_duration,
        previewImageRes = R.drawable.litany_preview_saints,
        backgroundImageRes = R.drawable.litany_bg_saints,
        audioChapletType = "litanySaints",
        jsonPath = "prayers/litanies/saints/litany_saints_en.json",
    ),
    HOLY_NAME(
        titleRes = R.string.litany_holy_name_title,
        subtitleRes = R.string.litany_holy_name_subtitle,
        durationRes = R.string.litany_holy_name_duration,
        previewImageRes = R.drawable.litany_preview_holy_name,
        backgroundImageRes = R.drawable.litany_bg_holy_name,
        audioChapletType = "litanyHolyName",
        jsonPath = "prayers/litanies/holyname/litany_holy_name_en.json",
    ),
    PRECIOUS_BLOOD(
        titleRes = R.string.litany_precious_blood_title,
        subtitleRes = R.string.litany_precious_blood_subtitle,
        durationRes = R.string.litany_precious_blood_duration,
        previewImageRes = R.drawable.litany_preview_precious_blood,
        backgroundImageRes = R.drawable.litany_bg_precious_blood,
        audioChapletType = "litanyPreciousBlood",
        jsonPath = "prayers/litanies/preciousblood/litany_precious_blood_en.json",
    ),
}
