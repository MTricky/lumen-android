package com.app.lumen.features.chaplets.ui

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
import com.app.lumen.features.chaplets.model.ChapletType
import com.app.lumen.ui.theme.NearBlack
import com.app.lumen.ui.theme.Slate
import com.app.lumen.ui.theme.SoftGold

private val CardBorder = Color.White.copy(alpha = 0.10f)
private val CardBg = Color(0xFF1E1E2B)
private val HeaderHeight = 460.dp

@Composable
fun ChapletsScreen(
    bottomPadding: Dp = 100.dp,
    onChapletSelected: (ChapletType) -> Unit = {},
    onMenuClick: () -> Unit = {},
) {
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

            // Chaplet cards
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .offset(y = (-60).dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ChapletType.entries.forEach { chapletType ->
                    ChapletCard(chapletType, onChapletSelected)
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
            painter = painterResource(R.drawable.chaplet_header_bg),
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
                text = stringResource(R.string.chaplets_header_subtitle),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = SoftGold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.chaplets_header_title),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.chaplets_header_description),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun ChapletCard(chapletType: ChapletType, onChapletSelected: (ChapletType) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .clickable { onChapletSelected(chapletType) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Preview image
        Image(
            painter = painterResource(chapletType.previewImageRes),
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
                    imageVector = chapletIcon(chapletType),
                    contentDescription = null,
                    tint = SoftGold,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = stringResource(chapletType.subtitleRes),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SoftGold,
                    maxLines = 2,
                )
            }

            Text(
                text = stringResource(chapletType.titleRes),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )

            Text(
                text = "•• " + stringResource(chapletType.durationRes),
                fontSize = 12.sp,
                color = Slate,
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = SoftGold,
            modifier = Modifier.size(24.dp),
        )
    }
}

private fun chapletIcon(chapletType: ChapletType): ImageVector = when (chapletType) {
    ChapletType.DIVINE_MERCY -> Icons.Filled.Favorite
    ChapletType.ST_MICHAEL -> Icons.Filled.Shield
    ChapletType.SEVEN_SORROWS -> Icons.Filled.WaterDrop
}
