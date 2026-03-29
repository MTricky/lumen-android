package com.app.lumen.features.onboarding.ui.phases

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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

    // Shared completion state — drives checkmark, glow fade, and button title simultaneously
    var showComplete by remember { mutableStateOf(false) }

    // Flame animations with organic easing
    val infiniteTransition = rememberInfiniteTransition(label = "flame")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.93f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    // Secondary outer glow ring
    val outerGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "outerGlow"
    )

    // Glow fades out gold, fades in accent on complete
    val glowFade by animateFloatAsState(
        targetValue = if (showComplete) 0f else 1f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "glowFade"
    )
    val accentGlow by animateFloatAsState(
        targetValue = if (showComplete) 1f else 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "accentGlow"
    )

    // Glass circle border transitions to gray on completion
    val circleBorderColor by animateColorAsState(
        targetValue = if (showComplete) Color.White.copy(alpha = 0.25f)
        else Color.White.copy(alpha = 0.10f),
        animationSpec = tween(500),
        label = "circleBorder"
    )

    // Haptic on each loading step change
    LaunchedEffect(viewModel.loadingCurrentStep) {
        if (viewModel.loadingCurrentStep > 0) {
            HapticManager.selection(view)
        }
    }

    // Synchronized completion — wait for button fill animation, then trigger everything together
    LaunchedEffect(viewModel.isLoadingComplete) {
        if (viewModel.isLoadingComplete) {
            delay(400) // wait for fill animation (300ms) + buffer
            showComplete = true
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

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isCompact = maxHeight < 680.dp
        val headerHeight = (maxHeight * 0.5f).coerceAtMost(400.dp)
        val topPadding = (maxHeight * 0.22f).coerceAtMost(150.dp)
        val iconContainerSize = if (isCompact) 160.dp else 220.dp
        val circleSize = if (isCompact) 90.dp else 110.dp

        // Background
        Box(modifier = Modifier.fillMaxSize().background(NearBlack))

        // Header image
        Box(modifier = Modifier.fillMaxWidth().height(headerHeight)) {
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
                .padding(top = topPadding)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Animated icon with multi-layer glow + glass circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(iconContainerSize)
            ) {
                // Outer glow ring (fades with glowFade on completion)
                Box(
                    modifier = Modifier
                        .size(iconContainerSize)
                        .scale(pulseScale * 1.1f)
                        .drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        SoftGold.copy(alpha = outerGlowAlpha * glowFade),
                                        Color.Transparent
                                    ),
                                    center = Offset(size.width / 2, size.height / 2),
                                    radius = size.width / 2
                                ),
                            )
                        }
                )

                // Inner glow ring
                Box(
                    modifier = Modifier
                        .size(iconContainerSize)
                        .scale(pulseScale)
                        .drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        SoftGold.copy(alpha = 0.25f * glowAlpha * glowFade),
                                        SoftGold.copy(alpha = 0.08f * glowAlpha * glowFade),
                                        Color.Transparent
                                    ),
                                    center = Offset(size.width / 2, size.height / 2),
                                    radius = size.width / 2
                                )
                            )
                        }
                )

                // Accent color blur behind circle on completion
                if (accentGlow > 0f) {
                    Box(
                        modifier = Modifier
                            .size(circleSize * 1.6f)
                            .drawBehind {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            SoftGold.copy(alpha = 0.4f * accentGlow),
                                            SoftGold.copy(alpha = 0.15f * accentGlow),
                                            Color.Transparent
                                        ),
                                        center = Offset(size.width / 2, size.height / 2),
                                        radius = size.width / 2
                                    )
                                )
                            }
                    )
                }

                // Glass circle background with animated border
                Box(
                    modifier = Modifier
                        .size(circleSize)
                        .clip(CircleShape)
                        .background(CardBg)
                        .border(1.dp, circleBorderColor, CircleShape)
                )

                // Icon transition: flame → checkmark with scale + fade
                AnimatedContent(
                    targetState = showComplete,
                    transitionSpec = {
                        (scaleIn(
                            initialScale = 0.3f,
                            animationSpec = tween(500, easing = FastOutSlowInEasing)
                        ) + fadeIn(tween(400))).togetherWith(
                            scaleOut(
                                targetScale = 1.5f,
                                animationSpec = tween(350)
                            ) + fadeOut(tween(300))
                        )
                    },
                    label = "iconTransition"
                ) { complete ->
                    if (complete) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(if (isCompact) 52.dp else 62.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.LocalFireDepartment,
                            contentDescription = null,
                            tint = SoftGold,
                            modifier = Modifier
                                .size(if (isCompact) 40.dp else 48.dp)
                                .scale(pulseScale)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Rotating quotes - hidden on compact screens
            if (!isCompact) {
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
            }

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

            Spacer(modifier = Modifier.height(if (isCompact) 16.dp else 28.dp))

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

            Spacer(modifier = Modifier.height(if (isCompact) 16.dp else 28.dp))

            // Progress fill button
            ProgressFillButton(
                progress = viewModel.loadingButtonProgress,
                isReady = showComplete,
                onClick = { HapticManager.lightImpact(view); onContinue() }
            )
        }
    }
}

@Composable
private fun ProgressFillButton(
    progress: Float,
    isReady: Boolean,
    onClick: () -> Unit
) {
    val isComplete = progress >= 1f

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

        // Content - title transitions in sync with showComplete
        AnimatedContent(
            targetState = isReady,
            transitionSpec = {
                fadeIn(tween(400)) togetherWith fadeOut(tween(300))
            },
            label = "buttonTitle"
        ) { ready ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (ready) stringResource(R.string.onboarding_loading_begin)
                        else stringResource(R.string.onboarding_loading_preparing),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    color = Color.White
                )
                if (ready) {
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
