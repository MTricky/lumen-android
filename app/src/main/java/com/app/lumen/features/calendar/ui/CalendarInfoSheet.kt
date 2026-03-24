package com.app.lumen.features.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.lumen.R
import com.app.lumen.ui.theme.*

private val CardBg = Color.White.copy(alpha = 0.05f)
private val SecondaryText = Color.White.copy(alpha = 0.55f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarInfoSheet(
    regionName: String,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NearBlack)
    ) {
        // Header with title and Done button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.calendar_info_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Text(
                    text = stringResource(R.string.done),
                    color = SoftGold,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Liturgical Region
            InfoSectionCard(
                icon = "\uD83C\uDF10", // globe emoji
                title = stringResource(R.string.calendar_info_region_title),
                description = stringResource(R.string.calendar_info_region_description, regionName)
            )

            // 2. Liturgical Colors
            InfoCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "\uD83C\uDFA8  " + stringResource(R.string.calendar_info_colors_title),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.calendar_info_colors_description),
                        fontSize = 14.sp,
                        color = SecondaryText
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    ColorRow(LiturgicalGreen, stringResource(R.string.calendar_info_color_green_name), stringResource(R.string.calendar_info_color_green_desc))
                    ColorRow(LiturgicalPurple, stringResource(R.string.calendar_info_color_violet_name), stringResource(R.string.calendar_info_color_violet_desc))
                    ColorRow(LiturgicalWhite, stringResource(R.string.calendar_info_color_white_name), stringResource(R.string.calendar_info_color_white_desc))
                    ColorRow(LiturgicalRed, stringResource(R.string.calendar_info_color_red_name), stringResource(R.string.calendar_info_color_red_desc))
                    ColorRow(LiturgicalRose, stringResource(R.string.calendar_info_color_rose_name), stringResource(R.string.calendar_info_color_rose_desc))
                }
            }

            // 3. Celebration Ranks
            InfoCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "\u2606  " + stringResource(R.string.calendar_info_ranks_title),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.calendar_info_ranks_description),
                        fontSize = 14.sp,
                        color = SecondaryText
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    RankRow(stringResource(R.string.calendar_info_rank_solemnity_name), stringResource(R.string.calendar_info_rank_solemnity))
                    RankRow(stringResource(R.string.calendar_info_rank_feast_name), stringResource(R.string.calendar_info_rank_feast))
                    RankRow(stringResource(R.string.calendar_info_rank_memorial_name), stringResource(R.string.calendar_info_rank_memorial))
                    RankRow(stringResource(R.string.calendar_info_rank_weekday_name), stringResource(R.string.calendar_info_rank_weekday))
                }
            }

            // 4. Holy Days of Obligation
            InfoSectionCard(
                icon = "\u2605",
                title = stringResource(R.string.calendar_info_holydays_title),
                description = stringResource(R.string.calendar_info_holydays_description)
            )

            // 5. How to Use
            InfoSectionCard(
                icon = "\uD83D\uDC46", // hand pointing emoji
                title = stringResource(R.string.calendar_info_howto_title),
                description = stringResource(R.string.calendar_info_howto_description)
            )
        }
    }
}

@Composable
private fun InfoCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
    ) {
        content()
    }
}

@Composable
private fun InfoSectionCard(
    icon: String,
    title: String,
    description: String
) {
    InfoCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "$icon  $title",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                fontSize = 14.sp,
                color = SecondaryText,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun ColorRow(color: Color, name: String, meaning: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "\u2014",
            fontSize = 14.sp,
            color = SecondaryText
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = meaning,
            fontSize = 14.sp,
            color = SecondaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RankRow(name: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "\u2022",
            fontSize = 14.sp,
            color = SoftGold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "\u2014",
            fontSize = 14.sp,
            color = SecondaryText
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = description,
            fontSize = 14.sp,
            color = SecondaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
