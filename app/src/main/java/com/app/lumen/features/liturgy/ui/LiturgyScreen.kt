package com.app.lumen.features.liturgy.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.app.lumen.features.liturgy.model.liturgicalColor
import com.app.lumen.features.liturgy.viewmodel.DaySelection
import com.app.lumen.features.liturgy.viewmodel.LiturgyViewModel
import com.app.lumen.ui.components.ReadingCard
import com.app.lumen.ui.components.SaintCard
import com.app.lumen.ui.components.VerseCard
import com.app.lumen.ui.theme.*

private val HEADER_HEIGHT = 380.dp

@Composable
fun LiturgyScreen(bottomPadding: Dp = 0.dp, viewModel: LiturgyViewModel = viewModel()) {
    val liturgy by viewModel.liturgy.collectAsStateWithLifecycle()
    val verse by viewModel.verse.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val daySelection by viewModel.daySelection.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize().background(NearBlack)) {
        AnimatedContent(
            targetState = daySelection,
            transitionSpec = {
                fadeIn(tween(300)) togetherWith fadeOut(tween(300))
            },
            label = "day_switch"
        ) { currentDay ->
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // Header with image + overlay content
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(HEADER_HEIGHT)
                    ) {
                        // Background image
                        if (liturgy?.imageUrl != null) {
                            AsyncImage(
                                model = liturgy?.imageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                alignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .drawWithContent {
                                        drawContent()
                                        // Top fade for status bar
                                        drawRect(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    NearBlack.copy(alpha = 0.4f),
                                                    Color.Transparent,
                                                ),
                                                startY = 0f,
                                                endY = size.height * 0.15f,
                                            )
                                        )
                                        // Bottom fade into content
                                        drawRect(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    NearBlack.copy(alpha = 0.5f),
                                                    NearBlack,
                                                ),
                                                startY = size.height * 0.45f,
                                                endY = size.height,
                                            )
                                        )
                                    }
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                MarianBlue.copy(alpha = 0.6f),
                                                NearBlack,
                                            )
                                        )
                                    )
                            )
                        }

                        // Day picker at top center
                        DayPicker(
                            selected = currentDay,
                            onSelect = viewModel::selectDay,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                                .padding(top = 8.dp)
                        )

                        // Header text centered at bottom
                        if (liturgy != null) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(horizontal = 24.dp)
                                    .padding(bottom = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = viewModel.getDisplayDate(),
                                    fontSize = 15.sp,
                                    color = Color.White.copy(alpha = 0.85f),
                                )

                                Spacer(Modifier.height(6.dp))

                                Text(
                                    text = liturgy!!.celebration,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    lineHeight = 30.sp,
                                    textAlign = TextAlign.Center,
                                )

                                Spacer(Modifier.height(8.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(liturgicalColor(liturgy!!.liturgicalColor))
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = liturgy!!.season,
                                        fontSize = 14.sp,
                                        color = Color.White.copy(alpha = 0.7f),
                                    )
                                }
                            }
                        }
                    }
                }

                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = SoftGold,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                } else if (error != null) {
                    item {
                        Text(
                            text = error!!,
                            color = Slate,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 60.dp, start = 20.dp, end = 20.dp)
                        )
                    }
                } else if (liturgy != null) {
                    val lit = liturgy!!

                    // Saint of the Day
                    if (lit.saintOfDay != null) {
                        item {
                            SaintCard(
                                name = lit.saintOfDay.name,
                                description = lit.saintOfDay.description,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // First Reading
                    item {
                        ReadingCard(
                            icon = Icons.Filled.LooksOne,
                            label = "First Reading",
                            reference = lit.readings.firstReading.reference,
                            previewText = lit.readings.firstReading.text,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }

                    // Psalm
                    item {
                        ReadingCard(
                            icon = Icons.Filled.MusicNote,
                            label = "Responsorial Psalm",
                            reference = lit.readings.psalm.reference,
                            previewText = lit.readings.psalm.response,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }

                    // Second Reading
                    if (lit.readings.secondReading != null) {
                        item {
                            ReadingCard(
                                icon = Icons.Filled.LooksTwo,
                                label = "Second Reading",
                                reference = lit.readings.secondReading.reference,
                                previewText = lit.readings.secondReading.text,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Gospel (prominent)
                    item {
                        ReadingCard(
                            icon = Icons.Filled.AutoStories,
                            label = "Gospel",
                            reference = lit.readings.gospel.reference,
                            previewText = lit.readings.gospel.text,
                            prominent = true,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }

                    // Verse of the Day
                    if (verse != null) {
                        item {
                            Spacer(Modifier.height(4.dp))
                            VerseCard(
                                text = verse!!.text,
                                reference = verse!!.reference,
                                category = verse!!.category,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Bottom spacer for tab bar
                    item {
                        Spacer(Modifier.height(bottomPadding + 16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DayPicker(
    selected: DaySelection,
    onSelect: (DaySelection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .width(220.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .border(
                width = 0.5.dp,
                color = Color.White.copy(alpha = 0.25f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(4.dp),
    ) {
        DaySelection.entries.forEach { day ->
            val isSelected = day == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isSelected) Color.White.copy(alpha = 0.2f)
                        else Color.Transparent
                    )
                    .clickable { onSelect(day) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (day == DaySelection.TODAY) "Today" else "Tomorrow",
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = Color.White.copy(alpha = if (isSelected) 1f else 0.7f),
                )
            }
        }
    }
}
