package com.app.lumen.features.onboarding.ui.phases

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.lumen.R
import com.app.lumen.features.onboarding.OnboardingViewModel
import com.app.lumen.ui.HapticManager
import com.app.lumen.features.onboarding.ui.components.OnboardingGlassProminentButton
import com.app.lumen.ui.theme.NearBlack
import com.app.lumen.ui.theme.SoftGold

private val CardBg = Color(0xFF1A1A29)
private val CardBorder = Color.White.copy(alpha = 0.10f)

@Composable
fun CompletionPhase(viewModel: OnboardingViewModel, onDone: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(NearBlack)
    ) {
        val isCompact = maxHeight < 680.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isCompact) Modifier.verticalScroll(rememberScrollState())
                    else Modifier
                )
                .padding(horizontal = 24.dp)
                .padding(top = 48.dp, bottom = 140.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isCompact) {
                Spacer(modifier = Modifier.height(24.dp))
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            // Main content - no animation, just show everything
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (isCompact) 20.dp else 32.dp)
            ) {
                // Success checkmark
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .border(3.dp, Color(0xFF4CAF50).copy(alpha = 0.2f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                // Title and subtitle
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.onboarding_completion_title),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.onboarding_completion_subtitle),
                        fontSize = 18.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }

                // Summary cards
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Routines
                    if (viewModel.totalRoutineCount > 0) {
                        SummaryCard(
                            icon = Icons.Filled.Repeat,
                            iconColor = Color(0xFFFF9800),
                            title = stringResource(R.string.onboarding_completion_routines, viewModel.totalRoutineCount),
                            subtitle = stringResource(R.string.onboarding_completion_routines_subtitle)
                        )
                    }

                    // Bible language
                    SummaryCard(
                        icon = Icons.Filled.Book,
                        iconColor = SoftGold,
                        title = stringResource(viewModel.selectedBibleLanguage.displayNameRes),
                        subtitle = stringResource(R.string.onboarding_completion_bible_subtitle)
                    )

                    // Region
                    SummaryCard(
                        icon = Icons.Filled.Public,
                        iconColor = Color.Cyan,
                        title = stringResource(viewModel.selectedRegion.displayNameRes),
                        subtitle = stringResource(R.string.onboarding_completion_region_subtitle)
                    )

                    // Notifications
                    if (viewModel.notificationsAuthorized) {
                        SummaryCard(
                            icon = Icons.Filled.Notifications,
                            iconColor = Color.Yellow,
                            title = stringResource(R.string.onboarding_completion_notifications_title),
                            subtitle = stringResource(R.string.onboarding_completion_notifications_subtitle)
                        )
                    }
                }
            }

            if (!isCompact) {
                Spacer(modifier = Modifier.weight(1f))
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
                    .height(60.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                NearBlack.copy(alpha = 0f),
                                NearBlack.copy(alpha = 0.8f),
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
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp)
            ) {
                OnboardingGlassProminentButton(
                    title = stringResource(R.string.onboarding_completion_done),
                    isLoading = viewModel.isCompleting
                ) {
                    HapticManager.success(view)
                    viewModel.completeOnboarding()
                    onDone()
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1
            )
        }

        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF4CAF50).copy(alpha = 0.8f),
            modifier = Modifier.size(22.dp)
        )
    }
}
