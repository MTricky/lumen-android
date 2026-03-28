package com.app.lumen.features.onboarding.ui.phases

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.lumen.R
import com.app.lumen.features.onboarding.OnboardingViewModel
import com.app.lumen.ui.HapticManager
import com.app.lumen.ui.theme.NearBlack
import com.app.lumen.ui.theme.SoftGold
import kotlinx.coroutines.delay

private val CardBg = Color(0xFF1A1A29)
private val CardBorder = Color.White.copy(alpha = 0.10f)

private val loadingStepTextRes = listOf(
    R.string.onboarding_loading_step_1,
    R.string.onboarding_loading_step_2,
    R.string.onboarding_loading_step_3,
    R.string.onboarding_loading_step_4
)

private data class BibleQuote(val quoteRes: Int, val sourceRes: Int)

private val quotes = listOf(
    BibleQuote(R.string.onboarding_quote_isaiah_60_1, R.string.onboarding_quote_isaiah_60_1_source),
    BibleQuote(R.string.onboarding_quote_matthew_5_14, R.string.onboarding_quote_matthew_5_14_source),
    BibleQuote(R.string.onboarding_quote_ephesians_5_8, R.string.onboarding_quote_ephesians_5_8_source),
    BibleQuote(R.string.onboarding_quote_psalm_119_105, R.string.onboarding_quote_psalm_119_105_source),
    BibleQuote(R.string.onboarding_quote_psalm_27_1, R.string.onboarding_quote_psalm_27_1_source),
    BibleQuote(R.string.onboarding_quote_john_8_12, R.string.onboarding_quote_john_8_12_source),
)

@Composable
fun LoadingPhase(viewModel: OnboardingViewModel, onContinue: () -> Unit) {
    val view = LocalView.current
    var currentQuoteIndex by remember { mutableIntStateOf(0) }
    var timerProgress by remember { mutableFloatStateOf(0f) }
    val quoteDuration = 8000L

    // Flame pulse animation (matching iOS: easeInOut 1.8s)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    // Glow opacity pulse (iOS: 0.4 to 0.9)
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    // Haptic on each loading step change
    LaunchedEffect(viewModel.loadingCurrentStep) {
        if (viewModel.loadingCurrentStep > 0) {
            HapticManager.selection(view)
        }
    }

    // Haptic on loading complete (wait for fill animation to finish visually)
    LaunchedEffect(viewModel.isLoadingComplete) {
        if (viewModel.isLoadingComplete) {
            delay(300)
            HapticManager.success(view)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.startLoadingSequence()
    }

    // Quote timer
    LaunchedEffect(Unit) {
        while (true) {
            delay(50)
            timerProgress += 50f / quoteDuration
            if (timerProgress >= 1f) {
                currentQuoteIndex = (currentQuoteIndex + 1) % quotes.size
                timerProgress = 0f
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background
        Box(modifier = Modifier.fillMaxSize().background(NearBlack))

        // Header image
        Box(modifier = Modifier.fillMaxWidth().height(400.dp)) {
            Image(
                painter = painterResource(id = R.drawable.onboarding_olive_garden),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                NearBlack.copy(alpha = 0f),
                                NearBlack.copy(alpha = 0.3f),
                                NearBlack.copy(alpha = 0.7f),
                                NearBlack
                            )
                        )
                    )
            )
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 15.dp)
                .padding(top = 150.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Animated icon with glow + glass circle background
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(220.dp)
            ) {
                // Radial glow behind the circle (iOS: RadialGradient softGold 0.25 → clear)
                if (!viewModel.isLoadingComplete) {
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .scale(pulseScale)
                            .drawBehind {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            SoftGold.copy(alpha = 0.25f * glowAlpha),
                                            SoftGold.copy(alpha = 0.08f * glowAlpha),
                                            Color.Transparent
                                        ),
                                        center = Offset(size.width / 2, size.height / 2),
                                        radius = size.width / 2
                                    )
                                )
                            }
                    )
                }

                // Glass circle background
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(CardBg)
                        .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
                )

                if (!viewModel.isLoadingComplete) {
                    Icon(
                        imageVector = Icons.Filled.LocalFireDepartment,
                        contentDescription = null,
                        tint = SoftGold,
                        modifier = Modifier
                            .size(48.dp)
                            .scale(pulseScale)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Rotating quotes
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBg)
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AnimatedContent(
                        targetState = currentQuoteIndex,
                        transitionSpec = {
                            fadeIn(tween(800)) togetherWith fadeOut(tween(800))
                        },
                        label = "loadingQuote"
                    ) { index ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(quotes[index].quoteRes),
                                fontSize = 14.sp,
                                fontStyle = FontStyle.Italic,
                                color = Color.White.copy(alpha = 0.85f),
                                textAlign = TextAlign.Center,
                                maxLines = 3
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(quotes[index].sourceRes),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = SoftGold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Status text (fade only, matching iOS easeInOut 0.5s)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBg)
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = if (viewModel.isLoadingComplete) -1 else viewModel.loadingCurrentStep,
                    transitionSpec = {
                        fadeIn(tween(500, easing = LinearEasing)) togetherWith
                                fadeOut(tween(500, easing = LinearEasing))
                    },
                    label = "loadingText"
                ) { step ->
                    Text(
                        text = if (step == -1) stringResource(R.string.onboarding_loading_ready)
                            else loadingStepTextRes.getOrNull(step)?.let { stringResource(it) } ?: "",
                        fontSize = 14.sp,
                        fontWeight = if (step == -1) FontWeight.SemiBold else FontWeight.Medium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Step dots
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(4) { step ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (step <= viewModel.loadingCurrentStep) SoftGold
                                else Color.White.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Progress fill button (capsule that fills with progress)
            ProgressFillButton(
                progress = viewModel.loadingButtonProgress,
                onClick = { HapticManager.lightImpact(view); onContinue() }
            )
        }
    }
}

@Composable
private fun ProgressFillButton(
    progress: Float,
    onClick: () -> Unit
) {
    val isComplete = progress >= 1f
    var showCompleteTitle by remember { mutableStateOf(false) }

    // Delay title change until after the fill animation (300ms) completes + 100ms buffer
    LaunchedEffect(isComplete) {
        if (isComplete) {
            delay(400)
            showCompleteTitle = true
        } else {
            showCompleteTitle = false
        }
    }

    // Smooth animation of the fill width
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300, easing = LinearEasing),
        label = "progressFill"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(SoftGold.copy(alpha = 0.3f))
            .clickable(enabled = isComplete) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Progress fill
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(30.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = animatedProgress)
                    .height(60.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(SoftGold, SoftGold.copy(alpha = 0.8f))
                        )
                    )
            )
        }

        // Content - animated title transition
        AnimatedContent(
            targetState = showCompleteTitle,
            transitionSpec = {
                fadeIn(tween(400)) togetherWith fadeOut(tween(300))
            },
            label = "buttonTitle"
        ) { isComplete ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isComplete) stringResource(R.string.onboarding_loading_begin)
                        else stringResource(R.string.onboarding_loading_preparing),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    color = Color.White
                )
                if (isComplete) {
                    Icon(
                        imageVector = Icons.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
