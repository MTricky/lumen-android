package com.app.lumen.features.onboarding.ui.steps

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.lumen.R
import com.app.lumen.features.onboarding.OnboardingStep
import com.app.lumen.features.onboarding.OnboardingViewModel
import com.app.lumen.features.onboarding.ui.components.OnboardingGlassCard
import com.app.lumen.features.onboarding.ui.components.OnboardingGlassProminentButton
import com.app.lumen.features.onboarding.ui.components.OnboardingPageContainer
import com.app.lumen.ui.HapticManager
import com.app.lumen.ui.theme.SoftGold
import kotlinx.coroutines.delay

private data class BibleQuote(val quoteRes: Int, val sourceRes: Int)

private val quotes = listOf(
    BibleQuote(R.string.onboarding_quote_john_8_12, R.string.onboarding_quote_john_8_12_source),
    BibleQuote(R.string.onboarding_quote_psalm_27_1, R.string.onboarding_quote_psalm_27_1_source),
    BibleQuote(R.string.onboarding_quote_psalm_119_105, R.string.onboarding_quote_psalm_119_105_source),
    BibleQuote(R.string.onboarding_quote_ephesians_5_8, R.string.onboarding_quote_ephesians_5_8_source),
    BibleQuote(R.string.onboarding_quote_matthew_5_14, R.string.onboarding_quote_matthew_5_14_source),
    BibleQuote(R.string.onboarding_quote_isaiah_60_1, R.string.onboarding_quote_isaiah_60_1_source),
)

@Composable
fun WelcomeStep(viewModel: OnboardingViewModel) {
    val view = LocalView.current
    val quoteDuration = 8000L
    var currentQuoteIndex by remember { mutableIntStateOf(0) }
    var timerProgress by remember { mutableFloatStateOf(0f) }

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

    OnboardingPageContainer(backgroundRes = OnboardingStep.WELCOME.backgroundRes) {
        Spacer(modifier = Modifier.weight(1f))

        // App intro card
        OnboardingGlassCard {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontSize = 41.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Icon(
                        imageVector = Icons.Filled.LocalFireDepartment,
                        contentDescription = null,
                        tint = SoftGold,
                        modifier = Modifier.size(30.dp)
                    )
                }

                AutoSizeText(
                    text = stringResource(R.string.onboarding_welcome_subtitle),
                    maxFontSize = 19.sp,
                    minFontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = SoftGold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(R.string.onboarding_welcome_body),
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Rotating quotes
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1A1A29))
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
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
                    label = "quote"
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

            // Circular timer
            Canvas(
                modifier = Modifier
                    .size(14.dp)
                    .align(Alignment.BottomEnd)
            ) {
                drawArc(
                    color = Color.White.copy(alpha = 0.2f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 2.dp.toPx())
                )
                drawArc(
                    color = SoftGold,
                    startAngle = -90f,
                    sweepAngle = 360f * timerProgress,
                    useCenter = false,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        OnboardingGlassProminentButton(title = stringResource(R.string.onboarding_get_started)) {
            HapticManager.selection(view)
            viewModel.goToNextStep()
        }
    }
}

@Composable
private fun AutoSizeText(
    text: String,
    maxFontSize: androidx.compose.ui.unit.TextUnit,
    minFontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight,
    color: Color,
    textAlign: TextAlign,
    modifier: Modifier = Modifier
) {
    var fontSize by remember { mutableFloatStateOf(maxFontSize.value) }
    Text(
        text = text,
        fontSize = fontSize.sp,
        fontWeight = fontWeight,
        color = color,
        textAlign = textAlign,
        maxLines = 1,
        modifier = modifier,
        onTextLayout = { result ->
            if (result.hasVisualOverflow && fontSize > minFontSize.value) {
                fontSize = (fontSize - 0.5f).coerceAtLeast(minFontSize.value)
            }
        }
    )
}
