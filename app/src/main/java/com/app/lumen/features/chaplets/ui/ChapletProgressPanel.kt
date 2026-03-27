package com.app.lumen.features.chaplets.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.lumen.R
import com.app.lumen.features.chaplets.model.*
import com.app.lumen.ui.theme.SoftGold

// Sacred Art mode
private val PanelBg = Color(0xFF2A2A2E).copy(alpha = 0.86f)
private val PanelBorder = SoftGold.copy(alpha = 0.20f)
// Simple mode
private val SimplePanelBg = Color(0xFF1E1E32).copy(alpha = 0.85f)
private val SimplePanelBorder = Color.White.copy(alpha = 0.10f)
private val ProgressTrack = Color.White.copy(alpha = 0.15f)

private val PanelShape = RoundedCornerShape(16.dp)

// --- Divine Mercy Progress Panel ---

@Composable
fun DivineMercyProgressPanel(
    progress: DivineMercyProgress?,
    isSimple: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (progress == null) return
    val step = progress.currentStep
    val decade = step.getDecade()
    val forTheSakeCount = step.getForTheSakeCount()
    val holyGodCount = step.getHolyGodCount()

    ProgressPanelLayout(
        progress = progress.progress,
        totalSteps = progress.totalSteps,
        currentStepIndex = progress.currentStepIndex,
        isSimple = isSimple,
        modifier = modifier,
    ) {
        if (decade != null) {
            Text(
                text = stringResource(R.string.chaplet_decade_of, decade, 5),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
            )
        }
        if (forTheSakeCount != null) {
            Text(
                text = stringResource(R.string.chaplet_for_the_sake_of, forTheSakeCount, 10),
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.8f),
            )
        } else if (holyGodCount != null) {
            Text(
                text = stringResource(R.string.chaplet_holy_god_of, holyGodCount, 3),
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.8f),
            )
        } else {
            Text(
                text = divineMercySectionLabel(step),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f),
            )
        }
    }
}

// --- St. Michael Progress Panel ---

@Composable
fun StMichaelProgressPanel(
    progress: StMichaelProgress?,
    isSimple: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (progress == null) return
    val step = progress.currentStep
    val salutation = step.getSalutation()
    val archangel = step.getArchangel()
    val hailMaryCount = step.getHailMaryCount()

    ProgressPanelLayout(
        progress = progress.progress,
        totalSteps = progress.totalSteps,
        currentStepIndex = progress.currentStepIndex,
        isSimple = isSimple,
        modifier = modifier,
    ) {
        if (salutation != null) {
            Text(
                text = stringResource(R.string.chaplet_salutation_of, salutation, 9),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
            )
        } else if (archangel != null) {
            Text(
                text = stringResource(R.string.chaplet_archangel_prayer_of, archangel, 4),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
            )
        }
        if (hailMaryCount != null) {
            Text(
                text = stringResource(R.string.chaplet_hail_mary_of, hailMaryCount, 3),
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.8f),
            )
        } else {
            Text(
                text = stMichaelSectionLabel(step),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f),
            )
        }
    }
}

// --- Seven Sorrows Progress Panel ---

@Composable
fun SevenSorrowsProgressPanel(
    progress: SevenSorrowsProgress?,
    isSimple: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (progress == null) return
    val step = progress.currentStep
    val sorrow = step.getSorrow()
    val hailMaryCount = step.getHailMaryCount()
    val invocationCount = step.getFinalInvocationCount()

    ProgressPanelLayout(
        progress = progress.progress,
        totalSteps = progress.totalSteps,
        currentStepIndex = progress.currentStepIndex,
        isSimple = isSimple,
        modifier = modifier,
    ) {
        if (sorrow != null) {
            Text(
                text = stringResource(R.string.chaplet_sorrow_of, sorrow, 7),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
            )
        }
        if (hailMaryCount != null) {
            Text(
                text = stringResource(R.string.chaplet_hail_mary_of, hailMaryCount, 7),
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.8f),
            )
        } else if (invocationCount != null) {
            Text(
                text = stringResource(R.string.chaplet_invocation_of, invocationCount, 3),
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.8f),
            )
        } else {
            Text(
                text = sevenSorrowsSectionLabel(step),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f),
            )
        }
    }
}

// --- Shared Layout ---

@Composable
private fun ProgressPanelLayout(
    progress: Float,
    totalSteps: Int,
    currentStepIndex: Int,
    isSimple: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val bg = if (isSimple) SimplePanelBg else PanelBg
    val border = if (isSimple) SimplePanelBorder else PanelBorder

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(PanelShape)
            .background(bg)
            .border(0.5.dp, border, PanelShape)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        content()

        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = SoftGold,
            trackColor = ProgressTrack,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )

        val remaining = totalSteps - currentStepIndex - 1
        val pct = (progress * 100).toInt()
        Text(
            text = if (remaining == 0) {
                stringResource(R.string.rosary_almost_done)
            } else {
                stringResource(R.string.rosary_progress_remaining, remaining, pct)
            },
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}

// --- Section Labels ---

@Composable
private fun divineMercySectionLabel(step: DivineMercyPrayerStep): String = when (step) {
    is DivineMercyPrayerStep.SignOfTheCross,
    is DivineMercyPrayerStep.ClosingSignOfTheCross -> stringResource(R.string.rosary_section_sign_of_cross)
    is DivineMercyPrayerStep.OpeningPrayer -> stringResource(R.string.chaplet_section_opening_prayer)
    is DivineMercyPrayerStep.OurFather -> stringResource(R.string.rosary_section_our_father)
    is DivineMercyPrayerStep.HailMary -> stringResource(R.string.rosary_section_hail_mary)
    is DivineMercyPrayerStep.ApostlesCreed -> stringResource(R.string.rosary_section_apostles_creed)
    is DivineMercyPrayerStep.EternalFather -> stringResource(R.string.chaplet_section_eternal_father)
    is DivineMercyPrayerStep.ForTheSake -> stringResource(R.string.chaplet_section_for_the_sake)
    is DivineMercyPrayerStep.HolyGod -> stringResource(R.string.chaplet_section_holy_god)
    is DivineMercyPrayerStep.ClosingPrayer -> stringResource(R.string.chaplet_section_closing_prayer)
    is DivineMercyPrayerStep.DecadeAnnouncement -> stringResource(R.string.chaplet_section_decade)
    else -> ""
}

@Composable
private fun stMichaelSectionLabel(step: StMichaelPrayerStep): String = when (step) {
    is StMichaelPrayerStep.SignOfTheCross,
    is StMichaelPrayerStep.ClosingSignOfTheCross -> stringResource(R.string.rosary_section_sign_of_cross)
    is StMichaelPrayerStep.OpeningPrayer -> stringResource(R.string.chaplet_section_opening_prayer)
    is StMichaelPrayerStep.SalutationOurFather,
    is StMichaelPrayerStep.ArchangelOurFather -> stringResource(R.string.rosary_section_our_father)
    is StMichaelPrayerStep.SalutationHailMary -> stringResource(R.string.rosary_section_hail_mary)
    is StMichaelPrayerStep.SalutationGloryBe -> stringResource(R.string.rosary_section_glory_be)
    is StMichaelPrayerStep.ClosingPrayer -> stringResource(R.string.chaplet_section_closing_prayer)
    is StMichaelPrayerStep.FinalPrayer -> stringResource(R.string.rosary_section_final_prayer)
    is StMichaelPrayerStep.SalutationAnnouncement -> stringResource(R.string.chaplet_section_salutation)
    is StMichaelPrayerStep.ArchangelAnnouncement -> stringResource(R.string.chaplet_section_archangel_prayer)
    else -> ""
}

@Composable
private fun sevenSorrowsSectionLabel(step: SevenSorrowsPrayerStep): String = when (step) {
    is SevenSorrowsPrayerStep.SignOfTheCross,
    is SevenSorrowsPrayerStep.ClosingSignOfTheCross -> stringResource(R.string.rosary_section_sign_of_cross)
    is SevenSorrowsPrayerStep.IntroductoryPrayer -> stringResource(R.string.chaplet_section_introductory_prayer)
    is SevenSorrowsPrayerStep.ActOfContrition -> stringResource(R.string.chaplet_section_act_of_contrition)
    is SevenSorrowsPrayerStep.SorrowOurFather -> stringResource(R.string.rosary_section_our_father)
    is SevenSorrowsPrayerStep.SorrowHailMary -> stringResource(R.string.rosary_section_hail_mary)
    is SevenSorrowsPrayerStep.SorrowfulMotherPrayer -> stringResource(R.string.chaplet_section_sorrowful_mother)
    is SevenSorrowsPrayerStep.ClosingPrayer -> stringResource(R.string.chaplet_section_closing_prayer)
    is SevenSorrowsPrayerStep.FinalInvocation -> stringResource(R.string.chaplet_section_final_invocation)
    is SevenSorrowsPrayerStep.Versicle -> stringResource(R.string.chaplet_section_versicle)
    is SevenSorrowsPrayerStep.Response -> stringResource(R.string.chaplet_section_response)
    is SevenSorrowsPrayerStep.ConcludingPrayer -> stringResource(R.string.chaplet_section_concluding_prayer)
    is SevenSorrowsPrayerStep.SorrowAnnouncement -> stringResource(R.string.chaplet_section_sorrow)
    else -> ""
}
