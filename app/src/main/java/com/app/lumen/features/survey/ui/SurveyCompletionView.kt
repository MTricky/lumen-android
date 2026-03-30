package com.app.lumen.features.survey.ui

import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import com.google.android.play.core.review.ReviewManagerFactory
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.lumen.R
import com.app.lumen.features.survey.viewmodel.SurveyViewModel
import com.app.lumen.ui.HapticManager
import com.app.lumen.ui.theme.NearBlack
import com.app.lumen.ui.theme.SoftGold

@Composable
fun SurveyCompletionView(
    viewModel: SurveyViewModel,
    onDismiss: () -> Unit,
    activity: android.app.Activity? = null,
) {
    val view = LocalView.current
    val context = LocalContext.current
    val showReviewPrompt = viewModel.showReviewPrompt

    // Entrance animation
    var showContent by remember { mutableStateOf(false) }
    val iconScale = remember { Animatable(0.5f) }
    val iconRotation = remember { Animatable(-30f) }

    // Heartbeat animation
    val infiniteTransition = rememberInfiniteTransition(label = "heartbeat")
    val heartBeatScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "heartbeat_scale",
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow_alpha",
    )

    LaunchedEffect(Unit) {
        iconScale.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow))
    }
    LaunchedEffect(Unit) {
        iconRotation.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow))
    }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        showContent = true
    }

    val contentAlpha by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = tween(400),
        label = "content_alpha",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))

        // Glass card with thank you content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Icon with heartbeat animation
            Box(contentAlignment = Alignment.Center) {
                // Outer glow
                Box(
                    modifier = Modifier
                        .size(105.dp)
                        .scale(heartBeatScale)
                        .clip(CircleShape)
                        .background(SoftGold.copy(alpha = glowAlpha * 0.3f))
                )

                Box(
                    modifier = Modifier
                        .size(85.dp)
                        .scale(iconScale.value * heartBeatScale)
                        .clip(CircleShape)
                        .background(SoftGold.copy(alpha = 0.15f))
                )

                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = SoftGold,
                    modifier = Modifier
                        .size(40.dp)
                        .scale(iconScale.value * heartBeatScale)
                        .graphicsLayer { rotationZ = iconRotation.value },
                )
            }

            Spacer(Modifier.height(20.dp))

            // Thank you message
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { alpha = contentAlpha },
            ) {
                Text(
                    text = stringResource(R.string.survey_completion_title),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = stringResource(
                        if (showReviewPrompt) R.string.survey_completion_subtitle_positive
                        else R.string.survey_completion_subtitle_neutral
                    ),
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        // Review request card (only for fully satisfied)
        if (showReviewPrompt) {
            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = contentAlpha }
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.survey_completion_review_request),
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SoftGold.copy(alpha = 0.15f))
                        .border(0.5.dp, SoftGold.copy(alpha = 0.4f), CircleShape)
                        .clickable {
                            HapticManager.lightImpact(view)
                            viewModel.trackReviewClicked()
                            if (activity != null) {
                                val reviewManager = ReviewManagerFactory.create(context)
                                reviewManager.requestReviewFlow().addOnSuccessListener { reviewInfo ->
                                    reviewManager.launchReviewFlow(activity, reviewInfo)
                                }
                            }
                        }
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = SoftGold,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(R.string.survey_completion_write_review),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SoftGold,
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Done button
        Button(
            onClick = {
                HapticManager.lightImpact(view)
                onDismiss()
            },
            colors = ButtonDefaults.buttonColors(containerColor = SoftGold),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .graphicsLayer { alpha = contentAlpha },
        ) {
            Text(
                text = stringResource(R.string.survey_completion_done),
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                fontSize = 16.sp,
            )
        }
    }
}
