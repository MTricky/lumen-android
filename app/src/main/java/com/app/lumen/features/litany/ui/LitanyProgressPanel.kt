package com.app.lumen.features.litany.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.lumen.R
import com.app.lumen.features.litany.model.LitanyPrayerStep
import com.app.lumen.features.litany.model.LitanyProgress
import com.app.lumen.ui.theme.SoftGold

@Composable
fun LitanyProgressPanel(
    progress: LitanyProgress?,
    isSimple: Boolean = false,
) {
    val bg = if (isSimple) Color(0xFF1E1E32).copy(alpha = 0.85f) else Color(0xFF2A2A2E).copy(alpha = 0.86f)
    val border = if (isSimple) Color.White.copy(alpha = 0.10f) else SoftGold.copy(alpha = 0.20f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(0.5.dp, border, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (progress != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Section title
                    if (progress.sectionTitle != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MenuBook,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp),
                            )
                            Text(
                                text = progress.sectionTitle,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                            )
                        }
                    }

                    // Step label
                    Text(
                        text = stepLabel(progress.currentStep),
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }
            }

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.15f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress.progress.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(2.dp))
                        .background(SoftGold),
                )
            }

            // Remaining label
            val remaining = progress.remaining
            Text(
                text = if (remaining == 0) {
                    stringResource(R.string.litany_progress_almost_done)
                } else {
                    stringResource(
                        R.string.litany_progress_remaining,
                        remaining,
                    )
                },
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun stepLabel(step: LitanyPrayerStep): String = when (step) {
    is LitanyPrayerStep.Intro -> ""
    is LitanyPrayerStep.SignOfTheCross,
    is LitanyPrayerStep.ClosingSignOfTheCross -> stringResource(R.string.litany_step_sign_of_cross)
    is LitanyPrayerStep.ClosingPrayer -> stringResource(R.string.litany_step_closing_prayer)
    is LitanyPrayerStep.Invocation -> stringResource(R.string.litany_step_invocation)
}
