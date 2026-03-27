package com.app.lumen.features.chaplets.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.app.lumen.R
import com.app.lumen.features.chaplets.model.StMichaelPrayerStep
import com.app.lumen.features.chaplets.service.ChapletAudioPlayer
import com.app.lumen.features.chaplets.viewmodel.StMichaelViewModel
import com.app.lumen.features.rosary.ui.RosaryVisualMode

@Composable
fun StMichaelPrayerScreen(
    viewModel: StMichaelViewModel,
    onBack: () -> Unit,
    onComplete: () -> Unit,
) {
    val currentStepIndex by viewModel.currentStepIndex.collectAsState()
    val isComplete by viewModel.isComplete.collectAsState()

    val step = viewModel.currentStep
    val prayer = viewModel.currentPrayer
    val salutation = viewModel.currentSalutation
    val archangelPrayer = viewModel.currentArchangelPrayer
    val progress = viewModel.progress

    val context = LocalContext.current
    val visualMode = remember { RosaryVisualMode.current(context) }
    val audioPlayer = remember { ChapletAudioPlayer.getInstance(context) }

    val isIntro = step is StMichaelPrayerStep.Intro
    val isAnnouncement = step?.isSalutationAnnouncement == true || step?.isArchangelAnnouncement == true

    val backgroundRes: Int? = if (step != null) {
        ChapletBackgroundManager.background(step, visualMode)
    } else null

    val titleLabel = stringResource(R.string.chaplet_st_michael_title)

    val state = ChapletPrayerState(
        currentStepIndex = currentStepIndex,
        isIntro = isIntro,
        isAnnouncement = isAnnouncement,
        prayer = prayer,
        backgroundRes = backgroundRes,
        titleLabel = titleLabel,
        chapletType = "stMichael",
        chapletDisplayName = titleLabel,
        progressContent = {
            StMichaelProgressPanel(
                progress = progress,
                isSimple = visualMode == RosaryVisualMode.SIMPLE,
            )
        },
        announcementTitle = when {
            salutation != null -> salutation.title
            archangelPrayer != null -> archangelPrayer.title
            else -> null
        },
        announcementSubtitle = when {
            salutation != null -> stringResource(R.string.chaplet_choir_of, salutation.choir)
            archangelPrayer != null -> archangelPrayer.instruction
            else -> null
        },
        announcementBody = salutation?.salutation,
        announcementNumber = step?.getSalutation() ?: step?.getArchangel(),
        announcementNumberLabel = when {
            step?.isSalutationAnnouncement == true -> step.getSalutation()?.let { s ->
                val suffix = ordinalSuffix(s)
                stringResource(R.string.chaplet_salutation_number, s, suffix)
            }
            step?.isArchangelAnnouncement == true -> step.getArchangel()?.let { a ->
                val suffix = ordinalSuffix(a)
                stringResource(R.string.chaplet_archangel_number, a, suffix)
            }
            else -> null
        },
    )

    ChapletPrayerScreen(
        state = state,
        onAdvance = { viewModel.advanceToNextStep() },
        onGoBack = { viewModel.goToPreviousStep() },
        onNavigateBack = onBack,
        onComplete = onComplete,
        isComplete = isComplete,
        peekNextIsAnnouncement = viewModel.peekNextStep()?.let {
            it.isSalutationAnnouncement || it.isArchangelAnnouncement
        } == true,
        playAudioForStep = { onFinished ->
            viewModel.currentStep?.let { currentStep ->
                audioPlayer.playAudio(currentStep, onFinished)
            }
        },
    )
}

@Composable
private fun ordinalSuffix(n: Int): String = when (n) {
    1 -> stringResource(R.string.rosary_ordinal_1)
    2 -> stringResource(R.string.rosary_ordinal_2)
    3 -> stringResource(R.string.rosary_ordinal_3)
    else -> stringResource(R.string.rosary_ordinal_other)
}
