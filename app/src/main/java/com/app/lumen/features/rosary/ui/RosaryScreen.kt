package com.app.lumen.features.rosary.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.lumen.R
import com.app.lumen.features.chaplets.ui.PrayerTypeMenuButton
import com.app.lumen.features.rosary.model.MysteryType
import com.app.lumen.features.subscription.SubscriptionManager
import com.app.lumen.ui.theme.NearBlack
import com.app.lumen.ui.theme.Slate
import com.app.lumen.ui.theme.SoftGold

private val CardBorder = Color.White.copy(alpha = 0.10f)
private val CardBg = Color(0xFF1E1E2B)
private val HeaderHeight = 460.dp

@Composable
fun RosaryScreen(
    bottomPadding: Dp = 100.dp,
    onMysterySelected: (MysteryType) -> Unit = {},
    onMenuClick: () -> Unit = {},
) {
    val isPremium by SubscriptionManager.hasProAccess.collectAsState()
    val todaysMystery = remember { MysteryType.forToday() }
    val otherMysteries = remember {
        MysteryType.entries.filter { !it.isTodaysMystery }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NearBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header with hero image
            HeaderSection(todaysMystery, onMenuClick)

            // Mystery cards
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .offset(y = (-60).dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Today's mystery - prominent card
                TodaysMysteryCard(todaysMystery, onMysterySelected, isPremium)

                // Other mysteries
                otherMysteries.forEach { mysteryType ->
                    MysteryCard(mysteryType, onMysterySelected, isPremium)
                }

                Spacer(Modifier.height(bottomPadding - 40.dp))
            }
        }
    }
}

@Composable
private fun HeaderSection(todaysMystery: MysteryType, onMenuClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(HeaderHeight)
    ) {
        // Background image
        Image(
            painter = painterResource(todaysMystery.imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to NearBlack.copy(alpha = 0.0f),
                            0.3f to NearBlack.copy(alpha = 0.15f),
                            0.6f to NearBlack.copy(alpha = 0.5f),
                            1.0f to NearBlack,
                        )
                    )
                )
        )

        // Top-trailing prayer type menu button
        PrayerTypeMenuButton(
            onClick = onMenuClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 16.dp, top = 8.dp),
        )

        // Header content
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 76.dp)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.rosary_todays_mysteries),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = SoftGold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(todaysMystery.labelRes),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = mysteryIcon(todaysMystery),
                    contentDescription = null,
                    tint = SoftGold,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = stringResource(todaysMystery.daysRes),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.8f),
                )
            }
        }
    }
}

@Composable
private fun TodaysMysteryCard(mysteryType: MysteryType, onMysterySelected: (MysteryType) -> Unit, isPremium: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .clickable { onMysterySelected(mysteryType) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Thumbnail
        Image(
            painter = painterResource(mysteryType.imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(12.dp))
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Badge row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = mysteryIcon(mysteryType),
                    contentDescription = null,
                    tint = SoftGold,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = stringResource(R.string.rosary_todays_mysteries),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SoftGold,
                )
            }

            Text(
                text = stringResource(mysteryType.labelRes),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )

            Text(
                text = stringResource(R.string.rosary_tap_to_start),
                fontSize = 12.sp,
                color = Slate,
            )
        }

        Icon(
            imageVector = if (isPremium) Icons.AutoMirrored.Filled.KeyboardArrowRight else Icons.Filled.Lock,
            contentDescription = null,
            tint = SoftGold,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun MysteryCard(mysteryType: MysteryType, onMysterySelected: (MysteryType) -> Unit, isPremium: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .clickable { onMysterySelected(mysteryType) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Thumbnail
        Image(
            painter = painterResource(mysteryType.imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(10.dp))
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = mysteryIcon(mysteryType),
                    contentDescription = null,
                    tint = Slate,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = stringResource(mysteryType.labelRes),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
                if (!isPremium) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = SoftGold.copy(alpha = 0.6f),
                        modifier = Modifier.size(10.dp),
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    tint = Slate,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = stringResource(mysteryType.daysRes),
                    fontSize = 12.sp,
                    color = Slate,
                )
            }
        }

        Icon(
            imageVector = if (isPremium) Icons.AutoMirrored.Filled.KeyboardArrowRight else Icons.Filled.Lock,
            contentDescription = null,
            tint = if (isPremium) Slate else SoftGold.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp),
        )
    }
}

private fun mysteryIcon(mysteryType: MysteryType): ImageVector = when (mysteryType) {
    MysteryType.JOYFUL -> Icons.Filled.WbSunny
    MysteryType.SORROWFUL -> Icons.Filled.WaterDrop
    MysteryType.GLORIOUS -> Icons.Filled.EmojiEvents
    MysteryType.LUMINOUS -> Icons.Filled.LightMode
}
