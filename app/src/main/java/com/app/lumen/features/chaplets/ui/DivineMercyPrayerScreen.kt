package com.app.lumen.features.chaplets.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.app.lumen.R
import com.app.lumen.features.chaplets.model.DivineMercyPrayerStep
import com.app.lumen.features.chaplets.service.ChapletAudioPlayer
import com.app.lumen.features.chaplets.viewmodel.DivineMercyViewModel
import com.app.lumen.features.rosary.ui.RosaryVisualMode

@Composable
fun DivineMercyPrayerScreen(
    viewModel: DivineMercyViewModel,
    onBack: () -> Unit,
    onComplete: () -> Unit,
) {
    val currentStepIndex by viewModel.currentStepIndex.collectAsState()
    val isComplete by viewModel.isComplete.collectAsState()

    val step = viewModel.currentStep
    val prayer = viewModel.currentPrayer
    val decade = viewModel.currentDecade
    val progress = viewModel.progress

    val context = LocalContext.current
    val visualMode = remember { RosaryVisualMode.current(context) }
    val audioPlayer = remember { ChapletAudioPlayer.getInstance(context) }

    val isIntro = step is DivineMercyPrayerStep.Intro
    val isAnnouncement = step?.isDecadeAnnouncement == true

    val backgroundRes: Int? = if (step != null) {
        ChapletBackgroundManager.background(step, visualMode)
    } else null

    val titleLabel = stringResource(R.string.chaplet_divine_mercy_title)

    val ordinalSuffix = when (decade?.let { step?.getDecade() }) {
        1 -> stringResource(R.string.rosary_ordinal_1)
        2 -> stringResource(R.string.rosary_ordinal_2)
        3 -> stringResource(R.string.rosary_ordinal_3)
        else -> stringResource(R.string.rosary_ordinal_other)
    }

    val state = ChapletPrayerState(
        currentStepIndex = currentStepIndex,
        isIntro = isIntro,
        isAnnouncement = isAnnouncement,
        prayer = prayer,
        backgroundRes = backgroundRes,
        titleLabel = titleLabel,
        chapletType = "divineMercy",
        chapletDisplayName = titleLabel,
        progressContent = {
            DivineMercyProgressPanel(
                progress = progress,
                isSimple = visualMode == RosaryVisualMode.SIMPLE,
            )
        },
        announcementTitle = decade?.title,
        announcementNumber = step?.getDecade(),
        announcementNumberLabel = step?.getDecade()?.let { d ->
            stringResource(R.string.chaplet_decade_number, d, ordinalSuffix)
        },
    )

    ChapletPrayerScreen(
        state = state,
        onAdvance = { viewModel.advanceToNextStep() },
        onGoBack = { viewModel.goToPreviousStep() },
        onNavigateBack = onBack,
        onComplete = onComplete,
        isComplete = isComplete,
        peekNextIsAnnouncement = viewModel.peekNextStep()?.isDecadeAnnouncement == true,
        playAudioForStep = { onFinished ->
            viewModel.currentStep?.let { currentStep ->
                audioPlayer.playAudio(currentStep, onFinished)
            }
        },
    )
}
