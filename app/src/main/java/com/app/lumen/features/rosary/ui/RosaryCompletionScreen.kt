package com.app.lumen.features.rosary.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.lumen.R
import com.app.lumen.features.rosary.viewmodel.RosaryViewModel
import com.app.lumen.ui.theme.NearBlack
import com.app.lumen.ui.theme.SoftGold

private val CardBg = Color.Black.copy(alpha = 0.40f)
private val CardBorder = Color.White.copy(alpha = 0.12f)

@Composable
fun RosaryCompletionScreen(
    viewModel: RosaryViewModel,
    onDone: () -> Unit,
) {
    val mysteryType by viewModel.selectedMysteryType.collectAsState()

    // Dedicated rosary completion background (same as iOS)
    val backgroundRes = R.drawable.rosary_completion_bg

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NearBlack),
    ) {
        // Background image
        Image(
            painter = painterResource(backgroundRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // Dark overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            Spacer(Modifier.weight(1f))

            // Checkmark icon
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(80.dp),
            )

            // Completion card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardBg)
                    .border(0.5.dp, CardBorder, RoundedCornerShape(20.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.rosary_completed),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )

                if (mysteryType != null) {
                    Text(
                        text = stringResource(mysteryType!!.labelRes),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SoftGold,
                    )
                }

                Text(
                    text = stringResource(R.string.rosary_completed_message),
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                )
            }

            Spacer(Modifier.weight(1f))

            Spacer(Modifier.height(16.dp))

            // Done button — same glass bg as progress/text panels
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xFF2E2A24).copy(alpha = 0.88f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(26.dp))
                    .clickable { onDone() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.rosary_done),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
        }
    }
}
