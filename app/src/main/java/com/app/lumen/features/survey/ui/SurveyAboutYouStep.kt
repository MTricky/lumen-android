package com.app.lumen.features.survey.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.lumen.R
import com.app.lumen.features.survey.model.AgeRange
import com.app.lumen.features.survey.model.Gender
import com.app.lumen.features.survey.viewmodel.SurveyViewModel
import com.app.lumen.ui.HapticManager
import com.app.lumen.ui.theme.NearBlack
import com.app.lumen.ui.theme.SoftGold

@Composable
fun SurveyAboutYouStep(viewModel: SurveyViewModel) {
    val view = LocalView.current
    val selectedAge by viewModel.selectedAgeRange.collectAsState()
    val selectedGender by viewModel.selectedGender.collectAsState()
    val canProceed = selectedAge != null && selectedGender != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(bottom = 16.dp),
    ) {
        // Flexible top spacer — pushes content below header image
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
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = SoftGold,
                modifier = Modifier.size(32.dp),
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.survey_about_you_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.survey_about_you_subtitle),
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(20.dp))

        // Age Range
        Text(
            text = stringResource(R.string.survey_age_range_label),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.6f),
        )

        Spacer(Modifier.height(10.dp))

        WrappingRow(spacing = 8.dp, centerHorizontally = true) {
            AgeRange.entries.forEach { age ->
                PillButton(
                    title = stringResource(age.displayNameRes),
                    isSelected = selectedAge == age,
                    onClick = {
                        HapticManager.selection(view)
                        viewModel.selectAgeRange(age)
                    },
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Gender
        Text(
            text = stringResource(R.string.survey_gender_label),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.6f),
        )

        Spacer(Modifier.height(10.dp))

        WrappingRow(spacing = 8.dp, centerHorizontally = true) {
            Gender.entries.forEach { gender ->
                PillButton(
                    title = stringResource(gender.displayNameRes),
                    isSelected = selectedGender == gender,
                    onClick = {
                        HapticManager.selection(view)
                        viewModel.selectGender(gender)
                    },
                )
            }
        }

        // Flexible bottom spacer
        Spacer(Modifier.weight(1f))

        // Continue button
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
                text = stringResource(R.string.survey_continue),
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                fontSize = 16.sp,
            )
        }
    }
}

@Composable
private fun PillButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isSelected) SoftGold else Color.White.copy(alpha = 0.1f))
            .then(
                if (!isSelected) Modifier.border(0.5.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) NearBlack else Color.White,
        )
    }
}

/** Simple flow layout that wraps children to the next line when they exceed available width. */
@Composable
private fun WrappingRow(
    spacing: Dp = 8.dp,
    centerHorizontally: Boolean = false,
    content: @Composable () -> Unit,
) {
    Layout(content = content) { measurables, constraints ->
        val spacingPx = spacing.roundToPx()
        val placeables = measurables.map { it.measure(constraints) }
        val maxWidth = constraints.maxWidth

        // First pass: compute rows
        data class RowInfo(val indices: MutableList<Int> = mutableListOf(), var width: Int = 0, var height: Int = 0)
        val rows = mutableListOf(RowInfo())
        var currentRowWidth = 0

        placeables.forEachIndexed { i, placeable ->
            if (currentRowWidth + placeable.width > maxWidth && currentRowWidth > 0) {
                rows.add(RowInfo())
                currentRowWidth = 0
            }
            val row = rows.last()
            if (row.indices.isNotEmpty()) currentRowWidth += spacingPx
            row.indices.add(i)
            currentRowWidth += placeable.width
            row.width = currentRowWidth
            row.height = maxOf(row.height, placeable.height)
        }

        // Second pass: place with optional centering
        val positions = Array(placeables.size) { 0 to 0 }
        var y = 0
        rows.forEach { row ->
            var x = if (centerHorizontally) (maxWidth - row.width) / 2 else 0
            row.indices.forEachIndexed { idx, i ->
                if (idx > 0) x += spacingPx
                positions[i] = x to y
                x += placeables[i].width
            }
            y += row.height + spacingPx
        }

        val totalHeight = if (rows.isEmpty()) 0 else y - spacingPx
        layout(maxWidth, totalHeight) {
            placeables.forEachIndexed { i, placeable ->
                placeable.placeRelative(positions[i].first, positions[i].second)
            }
        }
    }
}
