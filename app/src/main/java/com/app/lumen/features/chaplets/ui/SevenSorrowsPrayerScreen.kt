package com.app.lumen.features.chaplets.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.app.lumen.R
import com.app.lumen.features.chaplets.model.SevenSorrowsPrayerStep
import com.app.lumen.features.chaplets.service.ChapletAudioPlayer
import com.app.lumen.features.chaplets.viewmodel.SevenSorrowsViewModel
import com.app.lumen.features.rosary.ui.RosaryVisualMode

@Composable
fun SevenSorrowsPrayerScreen(
    viewModel: SevenSorrowsViewModel,
    onBack: () -> Unit,
    onComplete: () -> Unit,
) {
    val currentStepIndex by viewModel.currentStepIndex.collectAsState()
    val isComplete by viewModel.isComplete.collectAsState()

    val step = viewModel.currentStep
    val prayer = viewModel.currentPrayer
    val sorrow = viewModel.currentSorrow
    val progress = viewModel.progress

    val context = LocalContext.current
    val visualMode = remember { RosaryVisualMode.current(context) }
    val audioPlayer = remember { ChapletAudioPlayer.getInstance(context) }

    val isIntro = step is SevenSorrowsPrayerStep.Intro
    val isAnnouncement = step?.isSorrowAnnouncement == true

    val backgroundRes: Int? = if (step != null) {
        ChapletBackgroundManager.background(step, visualMode)
    } else null

    val titleLabel = stringResource(R.string.chaplet_seven_sorrows_title)

    val state = ChapletPrayerState(
        currentStepIndex = currentStepIndex,
        isIntro = isIntro,
        isAnnouncement = isAnnouncement,
        prayer = prayer,
        backgroundRes = backgroundRes,
        titleLabel = titleLabel,
        chapletType = "sevenSorrows",
        chapletDisplayName = titleLabel,
        progressContent = {
            SevenSorrowsProgressPanel(
                progress = progress,
                isSimple = visualMode == RosaryVisualMode.SIMPLE,
            )
        },
        announcementTitle = sorrow?.name,
        announcementSubtitle = sorrow?.title,
        announcementBody = sorrow?.meditation,
        announcementFruit = sorrow?.fruit,
        announcementScripture = sorrow?.scripture,
        announcementNumber = step?.getSorrow(),
        announcementNumberLabel = step?.getSorrow()?.let { s ->
            val suffix = ordinalSuffix(s)
            stringResource(R.string.chaplet_sorrow_number, s, suffix)
        },
    )

    ChapletPrayerScreen(
        state = state,
        onAdvance = { viewModel.advanceToNextStep() },
        onGoBack = { viewModel.goToPreviousStep() },
        onNavigateBack = onBack,
        onComplete = onComplete,
        isCompleteProvider = { viewModel.isComplete.value },
        peekNextIsAnnouncement = viewModel.peekNextStep()?.isSorrowAnnouncement == true,
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
