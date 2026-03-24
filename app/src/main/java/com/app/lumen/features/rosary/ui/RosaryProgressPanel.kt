package com.app.lumen.features.rosary.ui

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
import com.app.lumen.features.rosary.model.RosaryPrayerStep
import com.app.lumen.features.rosary.model.RosaryProgress
import com.app.lumen.ui.theme.SoftGold

// Sacred Art mode: subtle warm glass with gold border
private val PanelBg = Color(0xFF2A2A2E).copy(alpha = 0.86f)
private val PanelBorder = SoftGold.copy(alpha = 0.20f)
// Simple mode: cool dark glass matching NearBlack
private val SimplePanelBg = Color(0xFF1E1E32).copy(alpha = 0.85f)
private val SimplePanelBorder = Color.White.copy(alpha = 0.10f)
private val ProgressTrack = Color.White.copy(alpha = 0.15f)

private val PanelShape = RoundedCornerShape(16.dp)

@Composable
fun RosaryProgressPanel(
    progress: RosaryProgress?,
    isSimple: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (progress == null) return

    val step = progress.currentStep
    val decade = step.getDecade()
    val hailMaryCount = step.getHailMaryCount()

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
        // Decade info
        if (decade != null) {
            Text(
                text = stringResource(R.string.rosary_decade_of, decade, 5),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
            )
        }

        // Hail Mary count or prayer section name
        if (hailMaryCount != null) {
            Text(
                text = stringResource(R.string.rosary_hail_mary_of, hailMaryCount, 10),
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.8f),
            )
        } else {
            Text(
                text = sectionLabel(step),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f),
            )
        }

        // Progress bar
        LinearProgressIndicator(
            progress = { progress.progress.coerceIn(0f, 1f) },
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

        // Remaining label
        val remaining = progress.totalSteps - progress.currentStepIndex - 1
        val pct = (progress.progress * 100).toInt()

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

@Composable
private fun sectionLabel(step: RosaryPrayerStep): String {
    return when (step) {
        is RosaryPrayerStep.SignOfTheCross,
        is RosaryPrayerStep.ClosingSignOfTheCross -> stringResource(R.string.rosary_section_sign_of_cross)
        is RosaryPrayerStep.ApostlesCreed -> stringResource(R.string.rosary_section_apostles_creed)
        is RosaryPrayerStep.IntroOurFather,
        is RosaryPrayerStep.DecadeOurFather -> stringResource(R.string.rosary_section_our_father)
        is RosaryPrayerStep.IntroHailMary,
        is RosaryPrayerStep.DecadeHailMary -> stringResource(R.string.rosary_section_hail_mary)
        is RosaryPrayerStep.IntroGloryBe,
        is RosaryPrayerStep.DecadeGloryBe -> stringResource(R.string.rosary_section_glory_be)
        is RosaryPrayerStep.DecadeFatimaPrayer -> stringResource(R.string.rosary_section_fatima_prayer)
        is RosaryPrayerStep.MysteryAnnouncement -> stringResource(R.string.rosary_section_mystery)
        is RosaryPrayerStep.HailHolyQueen -> stringResource(R.string.rosary_section_hail_holy_queen)
        is RosaryPrayerStep.FinalPrayer -> stringResource(R.string.rosary_section_final_prayer)
        else -> ""
    }
}
