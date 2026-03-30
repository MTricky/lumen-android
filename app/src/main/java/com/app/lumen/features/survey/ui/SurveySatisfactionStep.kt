package com.app.lumen.features.survey.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.lumen.R
import com.app.lumen.features.survey.model.SatisfactionLevel
import com.app.lumen.features.survey.viewmodel.SurveyViewModel
import com.app.lumen.ui.HapticManager
import com.app.lumen.ui.theme.NearBlack
import com.app.lumen.ui.theme.SoftGold

@Composable
fun SurveySatisfactionStep(viewModel: SurveyViewModel) {
    val view = LocalView.current
    val selectedSatisfaction by viewModel.selectedSatisfaction.collectAsState()
    val canProceed = selectedSatisfaction != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(bottom = 16.dp),
    ) {
        // Flexible top spacer
        Spacer(Modifier.weight(1.8f))

        // Header glass card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Filled.SentimentSatisfied,
                contentDescription = null,
                tint = SoftGold,
                modifier = Modifier.size(32.dp),
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.survey_satisfaction_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.survey_satisfaction_subtitle),
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(16.dp))

        // Satisfaction options
        SatisfactionLevel.entries.forEach { level ->
            val isSelected = selectedSatisfaction == level
            SatisfactionCard(
                level = level,
                isSelected = isSelected,
                onClick = {
                    HapticManager.selection(view)
                    viewModel.selectSatisfaction(level)
                },
            )
            Spacer(Modifier.height(10.dp))
        }

        // Flexible bottom spacer
        Spacer(Modifier.weight(1f))

        // Finish button
        Button(
            onClick = {
                HapticManager.lightImpact(view)
                viewModel.nextStep()
            },
            enabled = canProceed,
            colors = ButtonDefaults.buttonColors(
                containerColor = SoftGold,
                disabledContainerColor = SoftGold.copy(alpha = 0.4f),
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text(
                text = stringResource(R.string.survey_finish),
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                fontSize = 16.sp,
            )
        }
    }
}

@Composable
private fun SatisfactionCard(
    level: SatisfactionLevel,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) SoftGold.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.08f))
            .border(
                0.5.dp,
                if (isSelected) SoftGold else Color.White.copy(alpha = 0.15f),
                RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Stars
        Row(
            modifier = Modifier.width(72.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            repeat(level.starCount) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = SoftGold,
                    modifier = Modifier.size(18.dp),
                )
            }
            repeat(3 - level.starCount) {
                Icon(
                    imageVector = Icons.Outlined.StarOutline,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // Text
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(level.displayNameRes),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            Text(
                text = stringResource(level.descriptionRes),
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.5f),
            )
        }

        // Selection indicator
        Icon(
            imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
            contentDescription = null,
            tint = if (isSelected) SoftGold else Color.White.copy(alpha = 0.3f),
            modifier = Modifier.size(22.dp),
        )
    }
}
