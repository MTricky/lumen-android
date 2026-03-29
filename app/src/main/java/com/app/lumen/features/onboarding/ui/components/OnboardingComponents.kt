package com.app.lumen.features.onboarding.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.lumen.ui.theme.NearBlack
import com.app.lumen.ui.theme.SoftGold

// Card colors matching the liturgy tab cards
private val CardBg = Color(0xFF1A1A29)
private val CardBorder = Color.White.copy(alpha = 0.10f)

val LocalCompactOnboarding = staticCompositionLocalOf { false }

@Composable
fun ColumnScope.OnboardingTopSpacer() {
    if (LocalCompactOnboarding.current) {
        Spacer(modifier = Modifier.height(8.dp))
    } else {
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun ColumnScope.OnboardingBottomSpacer() {
    if (!LocalCompactOnboarding.current) {
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun OnboardingPageContainer(
    @DrawableRes backgroundRes: Int,
    modifier: Modifier = Modifier,
    button: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isCompact = maxHeight < 680.dp
        val headerHeight = (maxHeight * 0.5f).coerceAtMost(400.dp)
        val topPadding = (maxHeight * 0.22f).coerceAtMost(150.dp)

        // Background color
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NearBlack)
        )

        // Header image at top with gradient overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
        ) {
            Image(
                painter = painterResource(id = backgroundRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Gradient overlay
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

        // Content area - scrollable on compact, weighted layout on large
        CompositionLocalProvider(LocalCompactOnboarding provides isCompact) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (isCompact) Modifier.verticalScroll(rememberScrollState())
                        else Modifier
                    )
                    .padding(horizontal = 20.dp)
                    .padding(top = topPadding, bottom = 140.dp)
            ) {
                content()
            }
        }

        // Floating button at bottom with gradient transition
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                NearBlack.copy(alpha = 0f),
                                NearBlack
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NearBlack)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp)
            ) {
                button()
            }
        }
    }
}

@Composable
fun OnboardingGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .border(
                width = 1.dp,
                color = CardBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun GlassRow(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 14,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(CardBg)
            .border(
                width = 1.dp,
                color = CardBorder,
                shape = RoundedCornerShape(cornerRadius.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        content()
    }
}

@Composable
fun OnboardingGlassProminentButton(
    title: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                if (enabled && !isLoading) SoftGold
                else SoftGold.copy(alpha = 0.5f)
            )
            .then(
                if (enabled && !isLoading) Modifier.clickable { onClick() }
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                color = Color.White
            )
        }
    }
}

@Composable
fun OnboardingSecondaryButton(
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Medium,
            fontSize = 17.sp,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}
