package com.app.lumen.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.app.lumen.MainActivity
import com.app.lumen.R

class VerseMediumWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val verseData = VerseWidgetData.load(context)
        val isPremium = VerseWidgetData.loadPremiumStatus(context)
        val backgroundImage = VerseWidgetData.loadBackgroundImage(context)
        val blurredImage = if (!isPremium) {
            VerseWidgetData.loadBlurredBackgroundImage(context)
        } else null

        provideContent {
            GlanceTheme {
                VerseMediumContent(
                    context = context,
                    verseData = verseData,
                    isPremium = isPremium,
                    backgroundImage = backgroundImage,
                    blurredImage = blurredImage,
                )
            }
        }
    }
}

@Composable
private fun VerseMediumContent(
    context: Context,
    verseData: VerseWidgetData?,
    isPremium: Boolean,
    backgroundImage: Bitmap?,
    blurredImage: Bitmap? = null,
) {
    val data = verseData ?: VerseWidgetData.placeholder
    val isLocked = !isPremium

    // Responsive sizing based on actual widget dimensions
    val size = LocalSize.current
    val isCompact = size.width < 280.dp
    val isNarrow = size.width < 220.dp

    val contentPadding = if (isNarrow) 8.dp else if (isCompact) 10.dp else 12.dp
    val verseFontSize = if (isNarrow) 14.sp else if (isCompact) 16.sp else 20.sp
    val badgeFontSize = if (isNarrow) 10.sp else 12.sp
    val refFontSize = if (isNarrow) 9.sp else 11.sp
    val quoteIconSize = if (isNarrow) 20.dp else 26.dp
    val badgeIconSize = if (isNarrow) 12.dp else 14.dp
    val badgePaddingH = if (isNarrow) 7.dp else 10.dp
    val badgePaddingV = if (isNarrow) 4.dp else 5.dp
    val unlockFontSize = if (isNarrow) 13.sp else 15.sp
    val unlockIconSize = if (isNarrow) 14.dp else 16.dp
    val unlockPaddingH = if (isNarrow) 12.dp else 16.dp
    val unlockPaddingV = if (isNarrow) 8.dp else 10.dp
    val skeletonLineHeight = if (isNarrow) 12.dp else 16.dp
    val skeletonLine2Width = if (isNarrow) 140.dp else 200.dp

    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        if (isLocked) {
            putExtra("open_paywall", true)
        } else {
            putExtra("open_verse_detail", true)
        }
    }

    // Use blurred background when locked, normal when unlocked
    val displayBackground = if (isLocked) (blurredImage ?: backgroundImage) else backgroundImage

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity(intent)),
    ) {
        // Background image (blurred when locked, normal when unlocked)
        if (displayBackground != null) {
            Image(
                provider = ImageProvider(displayBackground),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        // Dark overlay
        val overlayAlpha = if (displayBackground != null) 0.45f else 0.86f
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFF0F0F19).copy(alpha = overlayAlpha))),
        ) {}

        if (isLocked) {
            // LOCKED: skeleton content + unlock button
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(contentPadding),
            ) {
                // Top: skeleton badge + skeleton quote icon
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Badge skeleton in category color + shape
                    Box(
                        modifier = GlanceModifier
                            .background(ImageProvider(categorySkeletonBadgeRes(data.category)))
                            .padding(horizontal = badgePaddingH, vertical = badgePaddingV),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = GlanceModifier
                                    .size(badgeIconSize)
                                    .background(ImageProvider(R.drawable.widget_skeleton_line)),
                            ) {}
                            Spacer(modifier = GlanceModifier.width(5.dp))
                            Box(
                                modifier = GlanceModifier
                                    .width(40.dp)
                                    .height(10.dp)
                                    .background(ImageProvider(R.drawable.widget_skeleton_line)),
                            ) {}
                        }
                    }

                    Spacer(modifier = GlanceModifier.defaultWeight())

                    // Quote icon skeleton
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_quote),
                        contentDescription = null,
                        modifier = GlanceModifier.size(quoteIconSize),
                        colorFilter = androidx.glance.ColorFilter.tint(
                            ColorProvider(Color.White.copy(alpha = 0.15f)),
                        ),
                    )
                }

                // Center: skeleton text lines
                Box(
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                        .padding(horizontal = 2.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Column {
                        // Line 1 — full width
                        Box(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .height(skeletonLineHeight)
                                .background(ImageProvider(R.drawable.widget_skeleton_line)),
                        ) {}
                        Spacer(modifier = GlanceModifier.height(8.dp))
                        // Line 2 — partial width
                        Box(
                            modifier = GlanceModifier
                                .width(skeletonLine2Width)
                                .height(skeletonLineHeight)
                                .background(ImageProvider(R.drawable.widget_skeleton_line)),
                        ) {}
                    }
                }

                // Bottom: skeleton reference badge
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End,
                ) {
                    Box(
                        modifier = GlanceModifier
                            .background(ImageProvider(R.drawable.widget_ref_bg))
                            .padding(horizontal = badgePaddingH, vertical = 4.dp),
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .width(45.dp)
                                .height(10.dp)
                                .background(ImageProvider(R.drawable.widget_skeleton_short)),
                        ) {}
                    }
                }
            }

            // Unlock button
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = GlanceModifier
                        .background(ImageProvider(R.drawable.widget_unlock_bg))
                        .padding(horizontal = unlockPaddingH, vertical = unlockPaddingV),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_widget_lock),
                            contentDescription = null,
                            modifier = GlanceModifier.size(unlockIconSize),
                        )
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        Text(
                            text = context.getString(R.string.widget_unlock),
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = unlockFontSize,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }
            }
        } else {
            // UNLOCKED: full content
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(contentPadding),
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = GlanceModifier
                            .background(ImageProvider(R.drawable.widget_badge_bg))
                            .padding(horizontal = badgePaddingH, vertical = badgePaddingV),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                provider = ImageProvider(categoryIconRes(data.category)),
                                contentDescription = null,
                                modifier = GlanceModifier.size(badgeIconSize),
                            )
                            Spacer(modifier = GlanceModifier.width(5.dp))
                            Text(
                                text = categoryName(data.category),
                                style = TextStyle(
                                    color = ColorProvider(Color.White),
                                    fontSize = badgeFontSize,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.defaultWeight())

                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_quote),
                        contentDescription = null,
                        modifier = GlanceModifier.size(quoteIconSize),
                        colorFilter = androidx.glance.ColorFilter.tint(
                            ColorProvider(Color.White.copy(alpha = 0.6f)),
                        ),
                    )
                }

                Box(
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                        .padding(horizontal = 2.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = data.mediumText,
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = verseFontSize,
                            fontWeight = FontWeight.Medium,
                        ),
                        maxLines = if (isNarrow) 4 else 3,
                        modifier = GlanceModifier.fillMaxWidth(),
                    )
                }

                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End,
                ) {
                    Box(
                        modifier = GlanceModifier
                            .background(ImageProvider(R.drawable.widget_ref_bg))
                            .padding(horizontal = badgePaddingH, vertical = 4.dp),
                    ) {
                        Text(
                            text = data.shortReference,
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = refFontSize,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }
            }
        }
    }
}

class VerseMediumWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = VerseMediumWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        VerseWidgetWorker.enqueuePeriodicWork(context)
    }
}
