package com.app.lumen.features.liturgy.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.lumen.R
import com.app.lumen.features.liturgy.model.DailyVerse
import com.app.lumen.ui.theme.NearBlack
import com.app.lumen.ui.theme.Slate
import com.app.lumen.ui.theme.SoftGold

private val DividerColor = Color.White.copy(alpha = 0.12f)

// Category helpers (same as ContentCard)
private fun categoryColor(category: String): Color = when (category.lowercase()) {
    "faith" -> Color(0xFF6A1B9A)
    "love" -> Color(0xFFC62828)
    "hope" -> Color(0xFFF9A825)
    "strength" -> Color(0xFF2E7D32)
    "peace" -> Color(0xFF0277BD)
    "guidance" -> Color(0xFF4527A0)
    "healing" -> Color(0xFF00838F)
    "family" -> Color(0xFFD84315)
    "gratitude" -> Color(0xFF558B2F)
    "forgiveness" -> Color(0xFF6D4C41)
    else -> Color(0xFF6A1B9A)
}

private fun categoryIcon(category: String): ImageVector = when (category.lowercase()) {
    "faith" -> Icons.Filled.Add
    "love" -> Icons.Filled.Favorite
    "hope" -> Icons.Filled.WbSunny
    "strength" -> Icons.Filled.FitnessCenter
    "peace" -> Icons.Filled.Spa
    "guidance" -> Icons.Filled.Signpost
    "healing" -> Icons.Filled.Healing
    "family" -> Icons.Filled.FamilyRestroom
    "gratitude" -> Icons.Filled.AutoAwesome
    "forgiveness" -> Icons.Filled.BackHand
    else -> Icons.Filled.FormatQuote
}

@Composable
fun VerseDetailScreen(
    verse: DailyVerse,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val catColor = categoryColor(verse.category)
    val catIcon = categoryIcon(verse.category)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NearBlack)
            .statusBarsPadding(),
    ) {
        // Toolbar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 16.dp),
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF191927))
                    .border(0.5.dp, Color.White.copy(alpha = 0.18f), CircleShape),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }

            Text(
                text = stringResource(R.string.verse_of_the_day),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 40.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // ── Header ──────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Quote icon + title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.FormatQuote,
                        contentDescription = null,
                        tint = SoftGold,
                        modifier = Modifier.size(28.dp),
                    )
                    Text(
                        text = stringResource(R.string.verse_of_the_day),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SoftGold,
                    )
                }

                // Reference
                Text(
                    text = verse.reference,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )

                // Category badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(catColor.copy(alpha = 0.8f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        imageVector = catIcon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = verse.category.replaceFirstChar { it.uppercase() },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                    )
                }
            }

            // ── Divider ─────────────────────────────────────────────
            HorizontalDivider(color = DividerColor)

            // ── Full verse text ──────────────────────────────────────
            Text(
                text = verse.text,
                fontSize = 16.sp,
                color = Color.White,
                lineHeight = 26.sp,
            )

            // ── Divider ─────────────────────────────────────────────
            HorizontalDivider(color = DividerColor)

            // ── Reflection section ──────────────────────────────────
            if (!verse.reflection.isNullOrEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FormatQuote,
                            contentDescription = null,
                            tint = Slate,
                            modifier = Modifier.size(28.dp),
                        )
                        Text(
                            text = stringResource(R.string.verse_reflection),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate,
                        )
                    }

                    Text(
                        text = verse.reflection,
                        fontSize = 16.sp,
                        color = Color.White,
                        lineHeight = 26.sp,
                    )
                }
            }

            // ── Liturgical Connection section ─────────────────────
            if (verse.liturgicalConnectionType != null &&
                verse.liturgicalConnectionType != "none" &&
                !verse.liturgicalConnectionDescription.isNullOrEmpty()
            ) {
                HorizontalDivider(color = DividerColor)

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = connectionIcon(verse.liturgicalConnectionType),
                            contentDescription = null,
                            tint = Slate,
                            modifier = Modifier.size(28.dp),
                        )
                        Text(
                            text = stringResource(R.string.verse_liturgical_connection),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate,
                        )
                    }

                    // Connection type badge
                    Text(
                        text = connectionLabel(verse.liturgicalConnectionType),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0277BD).copy(alpha = 0.7f))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    )

                    Text(
                        text = verse.liturgicalConnectionDescription,
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        lineHeight = 24.sp,
                    )
                }
            }

            // ── Share button ────────────────────────────────────────
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(
                        0.5.dp,
                        Color.White.copy(alpha = 0.15f),
                        RoundedCornerShape(14.dp),
                    )
                    .clickable {
                        val shareText = "\u201C${verse.text}\u201D\n\n\u2014 ${verse.reference}"
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        context.startActivity(
                            Intent.createChooser(sendIntent, null)
                        )
                    }
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.verse_share),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun connectionLabel(type: String): String = when (type) {
    "gospel" -> stringResource(R.string.verse_connection_gospel)
    "first_reading" -> stringResource(R.string.verse_connection_first_reading)
    "psalm" -> stringResource(R.string.verse_connection_psalm)
    "saint" -> stringResource(R.string.verse_connection_saint)
    "season" -> stringResource(R.string.verse_connection_season)
    else -> ""
}

private fun connectionIcon(type: String): ImageVector = when (type) {
    "gospel" -> Icons.Filled.Book
    "first_reading" -> Icons.Filled.Description
    "psalm" -> Icons.Filled.MusicNote
    "saint" -> Icons.Filled.Person
    "season" -> Icons.Filled.CalendarMonth
    else -> Icons.Filled.AutoAwesome
}
