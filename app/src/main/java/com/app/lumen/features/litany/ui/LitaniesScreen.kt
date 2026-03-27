package com.app.lumen.features.litany.ui

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
import com.app.lumen.features.chaplets.model.PrayerType
import com.app.lumen.features.subscription.SubscriptionManager
import com.app.lumen.features.chaplets.ui.PrayerTypeMenuButton
import com.app.lumen.features.litany.model.LitanyType
import com.app.lumen.ui.theme.NearBlack
import com.app.lumen.ui.theme.Slate
import com.app.lumen.ui.theme.SoftGold

private val CardBorder = Color.White.copy(alpha = 0.10f)
private val CardBg = Color(0xFF1E1E2B)
private val HeaderHeight = 460.dp

@Composable
fun LitaniesScreen(
    bottomPadding: Dp = 100.dp,
    onLitanySelected: (LitanyType) -> Unit = {},
    onMenuClick: () -> Unit = {},
) {
    val isPremium by SubscriptionManager.hasProAccess.collectAsState()

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
            // Header with background image
            HeaderSection(onMenuClick = onMenuClick)

            // Litany cards
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .offset(y = (-60).dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                LitanyType.entries.forEach { litanyType ->
                    LitanyCard(litanyType, onLitanySelected, isPremium)
                }

                Spacer(Modifier.height(bottomPadding - 40.dp))
            }
        }
    }
}

@Composable
private fun HeaderSection(onMenuClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(HeaderHeight)
    ) {
        // Background image
        Image(
            painter = painterResource(R.drawable.litany_header_bg),
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
            currentPrayerType = PrayerType.LITANIES,
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
                text = stringResource(R.string.litanies_header_subtitle),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = SoftGold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.litanies_header_title),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.litanies_header_description),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun LitanyCard(litanyType: LitanyType, onLitanySelected: (LitanyType) -> Unit, isPremium: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .clickable { onLitanySelected(litanyType) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Preview image
        Image(
            painter = painterResource(litanyType.previewImageRes),
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
            // Icon + subtitle row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = litanyIcon(litanyType),
                    contentDescription = null,
                    tint = SoftGold,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = stringResource(litanyType.subtitleRes),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SoftGold,
                    maxLines = 2,
                )
            }

            Text(
                text = stringResource(litanyType.titleRes),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
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

private fun litanyIcon(litanyType: LitanyType): ImageVector = when (litanyType) {
    LitanyType.BLESSED_VIRGIN_MARY -> Icons.Filled.Star
    LitanyType.SACRED_HEART -> Icons.Filled.Favorite
    LitanyType.ST_JOSEPH -> Icons.Filled.Person
    LitanyType.SAINTS -> Icons.Filled.Groups
    LitanyType.HOLY_NAME -> Icons.Filled.MenuBook
    LitanyType.PRECIOUS_BLOOD -> Icons.Filled.WaterDrop
}
