package com.app.lumen.features.onboarding.ui

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.lumen.R
import com.app.lumen.features.onboarding.OnboardingFeedbackResult
import com.app.lumen.features.onboarding.OnboardingManager
import com.app.lumen.features.onboarding.OnboardingPhase
import com.app.lumen.features.onboarding.OnboardingStep
import com.app.lumen.features.onboarding.OnboardingViewModel
import com.app.lumen.features.onboarding.ui.components.OnboardingFirstFridayEditSheet
import com.app.lumen.features.onboarding.ui.components.OnboardingRoutineEditSheet
import com.app.lumen.features.onboarding.ui.components.OnboardingSecondaryButton
import com.app.lumen.features.onboarding.ui.phases.CompletionPhase
import com.app.lumen.features.onboarding.ui.phases.LoadingPhase
import com.app.lumen.features.onboarding.ui.phases.WidgetsPhase
import com.app.lumen.features.onboarding.ui.steps.BibleStep
import com.app.lumen.features.onboarding.ui.steps.FeaturesStep
import com.app.lumen.features.onboarding.ui.steps.NotificationsStep
import com.app.lumen.features.onboarding.ui.steps.RegionStep
import com.app.lumen.features.onboarding.ui.steps.RosaryStep
import com.app.lumen.features.onboarding.ui.steps.RoutineIntroStep
import com.app.lumen.features.onboarding.ui.steps.RoutineSetupStep
import com.app.lumen.features.onboarding.ui.steps.WelcomeStep
import com.app.lumen.services.RemoteConfigManager
import com.app.lumen.ui.HapticManager
import com.app.lumen.ui.theme.NearBlack
import com.app.lumen.ui.theme.SoftGold
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingView(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = viewModel()
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = context as? Activity
    var showFeedbackDialog by remember { mutableStateOf(false) }

    // Show feedback dialog after loading completes (matching iOS behavior)
    LaunchedEffect(viewModel.isLoadingComplete) {
        if (viewModel.isLoadingComplete && !OnboardingManager.shared.hasAskedForFeedback) {
            if (RemoteConfigManager.reviewEnabled) {
                delay(1000)
                showFeedbackDialog = true
            } else {
                OnboardingManager.shared.markFeedbackAsked()
                OnboardingManager.shared.saveFeedbackResult(OnboardingFeedbackResult.FEEDBACK_DISABLED)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = viewModel.currentPhase to viewModel.currentStep,
            transitionSpec = {
                fadeIn(tween(500)) togetherWith fadeOut(tween(300))
            },
            modifier = Modifier
                .fillMaxSize()
                .background(NearBlack),
            label = "onboardingTransition"
        ) { (phase, step) ->
            when (phase) {
                OnboardingPhase.STEPS -> {
                    when (step) {
                        OnboardingStep.WELCOME -> WelcomeStep(viewModel)
                        OnboardingStep.FEATURES -> FeaturesStep(viewModel)
                        OnboardingStep.REGION -> RegionStep(viewModel)
                        OnboardingStep.NOTIFICATIONS -> NotificationsStep(viewModel)
                        OnboardingStep.BIBLE -> BibleStep(viewModel)
                        OnboardingStep.ROSARY -> RosaryStep(viewModel)
                        OnboardingStep.ROUTINE_INTRO -> RoutineIntroStep(viewModel)
                        OnboardingStep.ROUTINE_SETUP -> RoutineSetupStep(viewModel)
                    }
                }

                OnboardingPhase.LOADING -> {
                    LoadingPhase(viewModel = viewModel) {
                        viewModel.goToWidgetsPhase()
                    }
                }

                OnboardingPhase.WIDGETS -> {
                    WidgetsPhase(viewModel = viewModel) {
                        viewModel.goToCompletionPhase()
                    }
                }

                OnboardingPhase.COMPLETION -> {
                    CompletionPhase(viewModel = viewModel) {
                        onComplete()
                    }
                }
            }
        }

        // Feedback dialog overlay
        AnimatedVisibility(
            visible = showFeedbackDialog,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(200))
        ) {
            OnboardingFeedbackDialog(
                onDismiss = {
                    showFeedbackDialog = false
                },
                onFeedback = { result ->
                    HapticManager.selection(view)
                    OnboardingManager.shared.markFeedbackAsked()
                    OnboardingManager.shared.saveFeedbackResult(result)
                    showFeedbackDialog = false

                    // Trigger Google Play In-App Review on positive feedback
                    if (result == OnboardingFeedbackResult.POSITIVE && activity != null) {
                        val reviewManager = ReviewManagerFactory.create(context)
                        reviewManager.requestReviewFlow().addOnSuccessListener { reviewInfo ->
                            reviewManager.launchReviewFlow(activity, reviewInfo)
                        }
                    }
                }
            )
        }
    }

    // Edit routine bottom sheet
    viewModel.routineBeingEdited?.let { routine ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.cancelRoutineEdit() },
            sheetState = sheetState,
            containerColor = NearBlack,
            scrimColor = Color.Black.copy(alpha = 0.5f)
        ) {
            OnboardingRoutineEditSheet(
                routine = routine,
                onDismiss = { viewModel.cancelRoutineEdit() },
                onSave = { updated -> viewModel.updateRoutine(updated) },
                onDelete = if (routine.isCustom) {
                    {
                        viewModel.removeRoutine(routine)
                        viewModel.cancelRoutineEdit()
                    }
                } else null
            )
        }
    }

    // First Friday edit sheet
    if (viewModel.isFirstFridayEditSheetShown) {
        val ffSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissFirstFridayEditSheet() },
            sheetState = ffSheetState,
            containerColor = NearBlack,
            scrimColor = Color.Black.copy(alpha = 0.5f)
        ) {
            OnboardingFirstFridayEditSheet(
                initialCount = viewModel.firstFridayInitialCount,
                onDismiss = { viewModel.dismissFirstFridayEditSheet() },
                onSave = { count ->
                    viewModel.firstFridayInitialCount = count
                    viewModel.dismissFirstFridayEditSheet()
                }
            )
        }
    }
}

private val FeedbackCardBg = Color(0xFF1A1A29)
private val FeedbackCardBorder = Color.White.copy(alpha = 0.15f)

@Composable
private fun OnboardingFeedbackDialog(
    onDismiss: () -> Unit,
    onFeedback: (OnboardingFeedbackResult) -> Unit
) {
    // Card scale animation
    val cardScale = remember { Animatable(0.85f) }
    LaunchedEffect(Unit) {
        cardScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.75f,
                stiffness = 400f
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .graphicsLayer {
                    scaleX = cardScale.value
                    scaleY = cardScale.value
                }
                .clip(RoundedCornerShape(20.dp))
                .background(FeedbackCardBg)
                .border(1.dp, FeedbackCardBorder, RoundedCornerShape(20.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* consume taps on the card so they don't dismiss */ }
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.onboarding_feedback_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.onboarding_feedback_subtitle),
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Start,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            OnboardingSecondaryButton(
                title = stringResource(R.string.onboarding_feedback_positive)
            ) {
                onFeedback(OnboardingFeedbackResult.POSITIVE)
            }

            Spacer(modifier = Modifier.height(10.dp))

            OnboardingSecondaryButton(
                title = stringResource(R.string.onboarding_feedback_neutral)
            ) {
                onFeedback(OnboardingFeedbackResult.NEUTRAL)
            }

            Spacer(modifier = Modifier.height(10.dp))

            OnboardingSecondaryButton(
                title = stringResource(R.string.onboarding_feedback_negative)
            ) {
                onFeedback(OnboardingFeedbackResult.NEGATIVE)
            }
        }
    }
}
