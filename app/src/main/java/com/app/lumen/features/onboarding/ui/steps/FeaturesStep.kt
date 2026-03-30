package com.app.lumen.features.onboarding.ui.steps

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
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.lumen.R
import com.app.lumen.features.onboarding.OnboardingStep
import com.app.lumen.features.onboarding.OnboardingViewModel
import com.app.lumen.features.onboarding.ui.components.LocalCompactOnboarding
import com.app.lumen.features.onboarding.ui.components.OnboardingBottomSpacer
import com.app.lumen.features.onboarding.ui.components.OnboardingGlassCard
import com.app.lumen.features.onboarding.ui.components.OnboardingGlassProminentButton
import com.app.lumen.features.onboarding.ui.components.OnboardingPageContainer
import com.app.lumen.features.onboarding.ui.components.OnboardingTopSpacer
import com.app.lumen.ui.HapticManager
import com.app.lumen.ui.theme.SoftGold

private data class Feature(val icon: ImageVector, val color: Color, val titleRes: Int)

private val features = listOf(
    Feature(Icons.Filled.MenuBook, Color(0xFFD4AF37), R.string.onboarding_features_bible),
    Feature(Icons.Filled.AutoStories, Color(0xFF2196F3), R.string.onboarding_features_readings),
    Feature(Icons.Filled.Workspaces, Color(0xFF9C27B0), R.string.onboarding_features_prayers),
    Feature(Icons.Filled.CalendarMonth, Color(0xFF4CAF50), R.string.onboarding_features_calendar),
    Feature(Icons.Filled.Repeat, Color(0xFFFF9800), R.string.onboarding_features_routines),
)

@Composable
fun FeaturesStep(viewModel: OnboardingViewModel) {
    val view = LocalView.current
    OnboardingPageContainer(
        backgroundRes = OnboardingStep.FEATURES.backgroundRes,
        button = {
            OnboardingGlassProminentButton(title = stringResource(R.string.onboarding_continue)) {
                HapticManager.selection(view)
                viewModel.goToNextStep()
            }
        }
    ) {
        OnboardingTopSpacer()

        OnboardingGlassCard {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.onboarding_features_title),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.onboarding_features_subtitle),
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        features.forEachIndexed { index, feature ->
            FeatureRow(icon = feature.icon, color = feature.color, title = stringResource(feature.titleRes))
            if (index < features.lastIndex) {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        Text(
            text = stringResource(R.string.onboarding_features_and_more),
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp),
            textAlign = TextAlign.Center
        )

        OnboardingBottomSpacer()
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, color: Color, title: String) {
    val isCompact = LocalCompactOnboarding.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1A1A29))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = if (isCompact) 10.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(if (isCompact) 32.dp else 36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }

        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = SoftGold,
            modifier = Modifier.size(18.dp)
        )
    }
}
