package com.app.lumen.features.survey.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.lumen.R
import com.app.lumen.features.survey.viewmodel.SurveyViewModel
import com.app.lumen.ui.HapticManager
import com.app.lumen.ui.theme.NearBlack
import com.app.lumen.ui.theme.SoftGold

private const val MAX_CHARACTERS = 500
private const val MIN_CHARACTERS_FOR_SUBMIT = 5

@Composable
fun SurveyFeatureRequestStep(viewModel: SurveyViewModel) {
    val view = LocalView.current
    val focusManager = LocalFocusManager.current
    val featureRequest by viewModel.featureRequest.collectAsState()

    val trimmedInput = featureRequest.trim()
    val hasInput = trimmedInput.isNotEmpty()
    val canSubmit = trimmedInput.length >= MIN_CHARACTERS_FOR_SUBMIT

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { focusManager.clearFocus() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 16.dp),
        ) {
            // Flexible top spacer
            Spacer(Modifier.weight(1f))

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
                    imageVector = Icons.Filled.Lightbulb,
                    contentDescription = null,
                    tint = SoftGold,
                    modifier = Modifier.size(32.dp),
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.survey_feature_request_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.survey_feature_request_subtitle),
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(16.dp))

            // Text field
            BasicTextField(
                value = featureRequest,
                onValueChange = { newValue ->
                    if (newValue.length <= MAX_CHARACTERS) {
                        viewModel.updateFeatureRequest(newValue)
                    }
                },
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 15.sp,
                ),
                cursorBrush = SolidColor(SoftGold),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                    .padding(16.dp),
                decorationBox = { innerTextField ->
                    Box {
                        if (featureRequest.isEmpty()) {
                            Text(
                                text = stringResource(R.string.survey_feature_request_placeholder),
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 15.sp,
                            )
                        }
                        innerTextField()
                    }
                },
            )

            // Character count
            if (hasInput) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Text(
                        text = "${featureRequest.length}/$MAX_CHARACTERS",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.5f),
                    )
                }
            }

            // Flexible bottom spacer
            Spacer(Modifier.weight(1f))

            // Bottom button
            Button(
                onClick = {
                    HapticManager.lightImpact(view)
                    focusManager.clearFocus()
                    if (!hasInput) {
                        viewModel.updateFeatureRequest("")
                    }
                    viewModel.nextStep()
                },
                enabled = !hasInput || canSubmit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (hasInput) SoftGold else Color.White.copy(alpha = 0.3f),
                    disabledContainerColor = SoftGold.copy(alpha = 0.4f),
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text(
                    text = stringResource(
                        if (hasInput) R.string.survey_feature_request_submit
                        else R.string.survey_feature_request_skip
                    ),
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    fontSize = 16.sp,
                )
            }
        }

        // Skip button in top-right when user has input
        if (hasInput) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 20.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
                    .clickable {
                        HapticManager.lightImpact(view)
                        focusManager.clearFocus()
                        viewModel.updateFeatureRequest("")
                        viewModel.nextStep()
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.survey_feature_request_skip),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }
        }
    }
}
