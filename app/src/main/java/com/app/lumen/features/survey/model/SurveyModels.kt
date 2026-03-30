package com.app.lumen.features.survey.model

import androidx.annotation.StringRes
import com.app.lumen.R

enum class AgeRange(val value: String, @StringRes val displayNameRes: Int) {
    UNDER_18("under_18", R.string.survey_age_under_18),
    AGE_18_24("18_24", R.string.survey_age_18_24),
    AGE_25_34("25_34", R.string.survey_age_25_34),
    AGE_35_44("35_44", R.string.survey_age_35_44),
    AGE_45_54("45_54", R.string.survey_age_45_54),
    AGE_55_64("55_64", R.string.survey_age_55_64),
    AGE_65_PLUS("65_plus", R.string.survey_age_65_plus),
}

enum class Gender(val value: String, @StringRes val displayNameRes: Int) {
    MALE("male", R.string.survey_gender_male),
    FEMALE("female", R.string.survey_gender_female),
}

enum class SatisfactionLevel(
    val value: String,
    @StringRes val displayNameRes: Int,
    @StringRes val descriptionRes: Int,
    val starCount: Int,
) {
    FULLY_SATISFIED("fully_satisfied", R.string.survey_satisfaction_fully, R.string.survey_satisfaction_fully_desc, 3),
    SOMEWHAT_SATISFIED("somewhat_satisfied", R.string.survey_satisfaction_somewhat, R.string.survey_satisfaction_somewhat_desc, 2),
    NOT_SATISFIED("not_satisfied", R.string.survey_satisfaction_not, R.string.survey_satisfaction_not_desc, 1),
}

enum class SurveyStep(val index: Int) {
    WELCOME(0),
    ABOUT_YOU(1),
    FEATURE_REQUEST(2),
    SATISFACTION(3),
    COMPLETION(4);

    fun next(): SurveyStep? = entries.find { it.index == index + 1 }
    fun previous(): SurveyStep? = entries.find { it.index == index - 1 }
}
