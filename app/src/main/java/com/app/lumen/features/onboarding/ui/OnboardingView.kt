package com.app.lumen.features.onboarding.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.lumen.features.onboarding.OnboardingPhase
import com.app.lumen.features.onboarding.OnboardingStep
import com.app.lumen.features.onboarding.OnboardingViewModel
import com.app.lumen.features.onboarding.ui.components.OnboardingFirstFridayEditSheet
import com.app.lumen.features.onboarding.ui.components.OnboardingRoutineEditSheet
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
import com.app.lumen.ui.theme.NearBlack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingView(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = viewModel()
) {
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
