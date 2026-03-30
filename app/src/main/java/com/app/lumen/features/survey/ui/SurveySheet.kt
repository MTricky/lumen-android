package com.app.lumen.features.survey.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.lumen.R
import com.app.lumen.features.survey.model.SurveyStep
import com.app.lumen.features.survey.viewmodel.SurveyViewModel
import com.app.lumen.ui.theme.NearBlack

private val HEADER_HEIGHT = 280.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurveySheet(
    onDismiss: () -> Unit,
    surveyViewModel: SurveyViewModel = viewModel(),
) {
    val currentStep by surveyViewModel.currentStep.collectAsState()
    // Capture activity before entering ModalBottomSheet (sheet has different window context)
    val activity = LocalContext.current as? android.app.Activity

    // Complete survey when reaching completion step
    LaunchedEffect(currentStep) {
        if (currentStep == SurveyStep.COMPLETION) {
            surveyViewModel.completeSurvey()
        }
    }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden },
    )

    ModalBottomSheet(
        onDismissRequest = { /* blocked by confirmValueChange */ },
        sheetState = sheetState,
        containerColor = Color.Transparent,
        dragHandle = null,
        sheetMaxWidth = Int.MAX_VALUE.dp,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        modifier = Modifier.statusBarsPadding().padding(top = 24.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(NearBlack)
        ) {
            // Header image at top
            Image(
                painter = painterResource(R.drawable.survey_header),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HEADER_HEIGHT)
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    NearBlack.copy(alpha = 0.0f),
                                    NearBlack.copy(alpha = 0.3f),
                                    NearBlack.copy(alpha = 0.7f),
                                    NearBlack,
                                ),
                                startY = 0f,
                                endY = size.height,
                            )
                        )
                    }
            )

            // Content
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                },
                label = "survey_step",
                modifier = Modifier.fillMaxSize()
            ) { step ->
                when (step) {
                    SurveyStep.WELCOME -> SurveyWelcomeStep(surveyViewModel)
                    SurveyStep.ABOUT_YOU -> SurveyAboutYouStep(surveyViewModel)
                    SurveyStep.FEATURE_REQUEST -> SurveyFeatureRequestStep(surveyViewModel)
                    SurveyStep.SATISFACTION -> SurveySatisfactionStep(surveyViewModel)
                    SurveyStep.COMPLETION -> SurveyCompletionView(surveyViewModel, onDismiss, activity)
                }
            }
        }
    }
}
