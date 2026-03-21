package com.app.lumen.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.app.lumen.ui.theme.NearBlack

private val ShimmerBase = Color.White.copy(alpha = 0.06f)
private val ShimmerHighlight = Color.White.copy(alpha = 0.15f)
private val CardBorder = Color.White.copy(alpha = 0.10f)
private val CardBg = Color(0xFF1A1A29)

@Composable
fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_translate",
    )

    return Brush.linearGradient(
        colors = listOf(ShimmerBase, ShimmerHighlight, ShimmerBase),
        start = Offset(translateAnim.value - 200f, 0f),
        end = Offset(translateAnim.value + 200f, 0f),
    )
}

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(shimmerBrush())
    )
}

@Composable
fun LiturgyLoadingSkeleton(modifier: Modifier = Modifier) {
    val brush = shimmerBrush()

    Column(modifier = modifier.fillMaxWidth()) {
        // Header shimmer (matches the real header area)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
        ) {
            // Shimmer background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush)
            )
            // Gradient overlay like real header
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                NearBlack.copy(alpha = 0.4f),
                                Color.Transparent,
                                Color.Transparent,
                                NearBlack.copy(alpha = 0.5f),
                                NearBlack,
                            ),
                            startY = 0f,
                        )
                    )
            )

            // Placeholder text at bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Date placeholder
                Box(
                    modifier = Modifier
                        .width(130.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                )
                Spacer(Modifier.height(10.dp))
                // Celebration title placeholder
                Box(
                    modifier = Modifier
                        .width(220.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                )
                Spacer(Modifier.height(10.dp))
                // Season badge placeholder
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                    )
                }
            }
        }

        // Skeleton cards
        SkeletonCard(
            titleWidth = 120.dp,
            contentLines = 2,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )

        SkeletonCard(
            titleWidth = 100.dp,
            contentLines = 3,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )

        SkeletonCard(
            titleWidth = 150.dp,
            contentLines = 2,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun SkeletonCard(
    titleWidth: androidx.compose.ui.unit.Dp,
    contentLines: Int,
    modifier: Modifier = Modifier,
) {
    val brush = shimmerBrush()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(16.dp)
    ) {
        // Header row: icon + title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .width(titleWidth)
                    .height(15.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(brush)
            )
        }

        Spacer(Modifier.height(12.dp))

        // Reference placeholder
        Box(
            modifier = Modifier
                .width(140.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(brush)
        )

        Spacer(Modifier.height(8.dp))

        // Content lines
        repeat(contentLines) { i ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (i == contentLines - 1) 0.7f else 1f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
            if (i < contentLines - 1) {
                Spacer(Modifier.height(6.dp))
            }
        }

        Spacer(Modifier.height(8.dp))

        // "Tap to read more" placeholder
        Box(
            modifier = Modifier
                .width(90.dp)
                .height(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(brush)
        )
    }
}
