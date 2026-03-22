package com.app.lumen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.lumen.ui.theme.Slate
import com.app.lumen.ui.theme.SoftGold

private val CardBorder = Color.White.copy(alpha = 0.10f)
private val CardBg = Color(0xFF1A1A29)
private val ReferenceBlue = Color(0xFF5BA8D9)

// Verse category colors matching iOS
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
    "faith" -> Icons.Filled.Add // cross substitute
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
fun ReadingCard(
    icon: ImageVector,
    label: String,
    reference: String,
    previewText: String,
    prominent: Boolean = false,
    audioUrl: String? = null,
    isPlayingThis: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onPlayClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (prominent) SoftGold else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (prominent) SoftGold else Color.White,
            )
            if (audioUrl != null) {
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = onPlayClick,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = if (isPlayingThis) Icons.Filled.PauseCircle else Icons.Filled.PlayCircle,
                        contentDescription = if (isPlayingThis) "Pause" else "Play",
                        tint = if (prominent) SoftGold else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = reference,
            fontSize = 14.sp,
            color = if (prominent) SoftGold else ReferenceBlue,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = previewText,
            fontSize = 15.sp,
            color = Color.White.copy(alpha = 0.8f),
            lineHeight = 22.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Tap to read more",
            fontSize = 12.sp,
            color = Slate,
        )
    }
}

@Composable
fun SaintCard(
    name: String,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = SoftGold,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Saint of the Day",
                fontSize = 13.sp,
                color = Slate,
            )
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Slate,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = name,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = description,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.65f),
            lineHeight = 20.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Tap to read more...",
            fontSize = 12.sp,
            color = Slate,
        )
    }
}

@Composable
fun ReflectionCard(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.FormatListBulleted,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Reflection",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = text,
            fontSize = 15.sp,
            color = Color.White.copy(alpha = 0.8f),
            lineHeight = 22.sp,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Tap to read more...",
            fontSize = 12.sp,
            color = Slate,
        )
    }
}

@Composable
fun GlassButton(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.08f))
            .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
    }
}

@Composable
fun VerseCard(
    text: String,
    reference: String,
    category: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val catColor = categoryColor(category)
    val catIcon = categoryIcon(category)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.FormatQuote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Verse of the Day",
                fontSize = 13.sp,
                color = Slate,
            )
            Spacer(Modifier.weight(1f))
            if (category.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(catColor.copy(alpha = 0.8f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = catIcon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = category.replaceFirstChar { it.uppercase() },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "\u201C$text\u201D",
            fontSize = 15.sp,
            fontStyle = FontStyle.Italic,
            color = Color.White.copy(alpha = 0.9f),
            lineHeight = 22.sp
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "\u2014 $reference",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = catColor,
            modifier = Modifier.align(Alignment.End)
        )
    }
}
