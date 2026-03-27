package com.app.lumen.features.rosary.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Equalizer bars indicating audio state, similar to the Music app.
 * Bars are vertically centered — idle shows short bars at center,
 * active shows bars growing both up and down from center.
 */
@Composable
fun AudioBarsView(
    isAnimating: Boolean,
    color: Color = Color.White,
    modifier: Modifier = Modifier,
) {
    val barWidth = 3.dp
    val barSpacing = 2.dp
    val maxHeight = 16f
    val minHeight = 3f

    val barConfigs = remember {
        listOf(
            BarConfig(heightFraction = 0.6f, durationMs = 450, delayMs = 0),
            BarConfig(heightFraction = 1.0f, durationMs = 550, delayMs = 100),
            BarConfig(heightFraction = 0.75f, durationMs = 400, delayMs = 200),
            BarConfig(heightFraction = 0.9f, durationMs = 500, delayMs = 300),
        )
    }

    var active by remember { mutableStateOf(isAnimating) }
    LaunchedEffect(isAnimating) { active = isAnimating }

    Row(
        modifier = modifier.height(maxHeight.dp),
        horizontalArrangement = Arrangement.spacedBy(barSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        barConfigs.forEachIndexed { index, config ->
            val targetHeight = if (active) maxHeight * config.heightFraction else minHeight

            val animatedHeight by animateFloatAsState(
                targetValue = targetHeight,
                animationSpec = if (active) {
                    infiniteRepeatable(
                        animation = tween(
                            durationMillis = config.durationMs,
                            easing = FastOutSlowInEasing,
                        ),
                        repeatMode = RepeatMode.Reverse,
                        initialStartOffset = StartOffset(config.delayMs),
                    )
                } else {
                    tween(durationMillis = 350, easing = FastOutSlowInEasing)
                },
                label = "bar_$index",
            )

            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(animatedHeight.dp)
                    .clip(RoundedCornerShape(barWidth / 2))
                    .background(color),
            )
        }
    }
}

private data class BarConfig(
    val heightFraction: Float,
    val durationMs: Int,
    val delayMs: Int,
)
