package com.app.lumen.features.onboarding.ui.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WbSunny
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
import com.app.lumen.features.onboarding.ui.components.OnboardingGlassCard
import com.app.lumen.features.onboarding.ui.components.OnboardingGlassProminentButton
import com.app.lumen.features.onboarding.ui.components.OnboardingPageContainer
import com.app.lumen.ui.HapticManager
import com.app.lumen.ui.theme.SoftGold

@Composable
fun RoutineIntroStep(viewModel: OnboardingViewModel) {
    val view = LocalView.current
    OnboardingPageContainer(backgroundRes = OnboardingStep.ROUTINE_INTRO.backgroundRes) {
        Spacer(modifier = Modifier.weight(1f))

        // Title Card
        OnboardingGlassCard {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.WbSunny,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.onboarding_routine_intro_title),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.onboarding_routine_intro_subtitle),
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Benefits
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BenefitRow(
                icon = Icons.Filled.AccessTime,
                color = Color(0xFF2196F3),
                text = stringResource(R.string.onboarding_routine_intro_benefit_reminders)
            )
            BenefitRow(
                icon = Icons.Filled.TrendingUp,
                color = Color(0xFF4CAF50),
                text = stringResource(R.string.onboarding_routine_intro_benefit_progress)
            )
            BenefitRow(
                icon = Icons.Filled.Favorite,
                color = Color(0xFFF44336),
                text = stringResource(R.string.onboarding_routine_intro_benefit_habits)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        OnboardingGlassProminentButton(title = stringResource(R.string.onboarding_routine_intro_button)) {
            HapticManager.selection(view)
            viewModel.goToNextStep()
        }
    }
}

@Composable
private fun BenefitRow(icon: ImageVector, color: Color, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1A1A29))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}
