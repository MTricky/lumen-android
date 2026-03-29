package com.app.lumen.features.onboarding.ui.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.lumen.R
import com.app.lumen.features.onboarding.OnboardingViewModel
import com.app.lumen.features.onboarding.RoutineSuggestion
import com.app.lumen.features.onboarding.ui.components.OnboardingGlassCard
import com.app.lumen.features.onboarding.ui.components.OnboardingGlassProminentButton
import com.app.lumen.features.onboarding.ui.components.OnboardingSecondaryButton
import com.app.lumen.ui.theme.NearBlack
import com.app.lumen.ui.HapticManager
import com.app.lumen.ui.theme.SoftGold
import java.util.Locale

private val CardBg = Color(0xFF1A1A29)
private val CardBorder = Color.White.copy(alpha = 0.10f)

@Composable
fun RoutineSetupStep(viewModel: OnboardingViewModel) {
    val view = LocalView.current
    var showAddDialog by remember { mutableStateOf(false) }
    var customRoutineName by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NearBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 140.dp)
        ) {
            // Title Card
            OnboardingGlassCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.onboarding_routine_setup_title),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.onboarding_routine_setup_subtitle),
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Suggested section label
            Text(
                text = stringResource(R.string.onboarding_routine_setup_suggested),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
            )

            // Routine grid (2 columns)
            val suggestions = RoutineSuggestion.all
            val rows = suggestions.chunked(2)
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { suggestion ->
                        RoutineCard(
                            title = stringResource(suggestion.titleRes),
                            icon = suggestion.type.icon,
                            isSelected = viewModel.isRoutineSelected(suggestion),
                            defaultHour = suggestion.defaultHour,
                            defaultMinute = suggestion.defaultMinute,
                            modifier = Modifier.weight(1f),
                            onToggle = { HapticManager.softImpact(view); viewModel.toggleRoutineSelection(suggestion) },
                            onEdit = {
                                viewModel.selectedRoutines
                                    .firstOrNull { it.type == suggestion.type && !it.isCustom }
                                    ?.let { viewModel.editRoutine(it) }
                            }
                        )
                    }
                    // Fill remaining space if odd number
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Angelus + First Friday row (side by side)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Angelus card
                val angelus = RoutineSuggestion.angelus
                RoutineCard(
                    title = stringResource(angelus.titleRes),
                    icon = angelus.type.icon,
                    isSelected = viewModel.isRoutineSelected(angelus),
                    defaultHour = angelus.defaultHour,
                    defaultMinute = angelus.defaultMinute,
                    modifier = Modifier.weight(1f),
                    onToggle = { HapticManager.softImpact(view); viewModel.toggleRoutineSelection(angelus) },
                    onEdit = {
                        viewModel.selectedRoutines
                            .firstOrNull { it.type == angelus.type && !it.isCustom }
                            ?.let { viewModel.editRoutine(it) }
                    }
                )

                // First Friday card
                FirstFridayCard(
                    isSelected = viewModel.isFirstFridaySelected,
                    modifier = Modifier.weight(1f),
                    onToggle = { HapticManager.selection(view); viewModel.toggleFirstFridaySelection() },
                    onEdit = { viewModel.showFirstFridayEditSheet() }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Custom routines
            val customRoutines = viewModel.selectedRoutines.filter { it.isCustom }
            if (customRoutines.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.onboarding_routine_setup_custom),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
                )
                customRoutines.forEach { routine ->
                    CustomRoutineRow(
                        routine = routine,
                        onEdit = { viewModel.editRoutine(routine) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Add custom button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CardBg)
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                    .clickable { showAddDialog = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = SoftGold,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.onboarding_routine_setup_add_custom),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Selected count
            if (viewModel.hasSelectedRoutines) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardBg)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = SoftGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.onboarding_routine_setup_count, viewModel.totalRoutineCount),
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Floating button at bottom
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
                if (viewModel.hasSelectedRoutines) {
                    OnboardingGlassProminentButton(title = stringResource(R.string.onboarding_continue)) {
                        HapticManager.selection(view)
                        viewModel.goToNextStep()
                    }
                } else {
                    OnboardingSecondaryButton(title = stringResource(R.string.onboarding_routine_setup_skip)) {
                        HapticManager.selection(view)
                        viewModel.goToNextStep()
                    }
                }
            }
        }
    }

    // Add custom routine dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                customRoutineName = ""
            },
            title = { Text(stringResource(R.string.onboarding_routine_setup_dialog_title)) },
            text = {
                TextField(
                    value = customRoutineName,
                    onValueChange = { customRoutineName = it },
                    placeholder = { Text(stringResource(R.string.onboarding_routine_setup_dialog_placeholder)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (customRoutineName.isNotBlank()) {
                            HapticManager.softImpact(view)
                            viewModel.addCustomRoutine(customRoutineName.trim())
                            customRoutineName = ""
                            showAddDialog = false
                        }
                    }
                ) { Text(stringResource(R.string.onboarding_routine_setup_dialog_add)) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddDialog = false
                        customRoutineName = ""
                    }
                ) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun RoutineCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    defaultHour: Int,
    defaultMinute: Int,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit,
    onEdit: () -> Unit
) {
    val timeText = String.format(Locale.getDefault(), "%d:%02d %s",
        if (defaultHour % 12 == 0) 12 else defaultHour % 12,
        defaultMinute,
        if (defaultHour < 12) "AM" else "PM"
    )

    Column(
        modifier = modifier
            .height(110.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .then(
                if (isSelected)
                    Modifier.border(2.dp, SoftGold, RoundedCornerShape(16.dp))
                else
                    Modifier.border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            )
            .clickable { onToggle() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SoftGold,
                modifier = Modifier.size(22.dp)
            )
            Icon(
                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.Circle,
                contentDescription = null,
                tint = if (isSelected) SoftGold else Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.AccessTime,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = timeText,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit",
                    tint = SoftGold,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onEdit() }
                )
            }
        }
    }
}

@Composable
private fun FirstFridayCard(
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit,
    onEdit: () -> Unit
) {
    Column(
        modifier = modifier
            .height(110.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .then(
                if (isSelected)
                    Modifier.border(2.dp, SoftGold, RoundedCornerShape(16.dp))
                else
                    Modifier.border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            )
            .clickable { onToggle() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Circle with "1" icon (matching RoutineTab)
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(SoftGold),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "1",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
            Icon(
                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.Circle,
                contentDescription = null,
                tint = if (isSelected) SoftGold else Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.onboarding_routine_setup_first_fridays),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.onboarding_routine_setup_yearly_tracking),
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit",
                    tint = SoftGold,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onEdit() }
                )
            }
        }
    }
}

@Composable
private fun CustomRoutineRow(
    routine: com.app.lumen.features.onboarding.OnboardingRoutineSelection,
    onEdit: () -> Unit
) {
    val timeText = String.format(Locale.getDefault(), "%d:%02d %s",
        if (routine.selectedHour % 12 == 0) 12 else routine.selectedHour % 12,
        routine.selectedMinute,
        if (routine.selectedHour < 12) "AM" else "PM"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .border(2.dp, SoftGold, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = SoftGold,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = routine.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = timeText,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
        IconButton(onClick = onEdit) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = "Edit",
                tint = SoftGold,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
