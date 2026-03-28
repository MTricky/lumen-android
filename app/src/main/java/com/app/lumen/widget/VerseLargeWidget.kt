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
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
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

class VerseLargeWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val verseData = VerseWidgetData.load(context)
        val isPremium = VerseWidgetData.loadPremiumStatus(context)
        val backgroundImage = VerseWidgetData.loadBackgroundImage(context)
        val blurredImage = if (!isPremium) {
            VerseWidgetData.loadBlurredBackgroundImage(context)
        } else null

        provideContent {
            GlanceTheme {
                VerseLargeContent(
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
private fun VerseLargeContent(
    context: Context,
    verseData: VerseWidgetData?,
    isPremium: Boolean,
    backgroundImage: Bitmap?,
    blurredImage: Bitmap? = null,
) {
    val data = verseData ?: VerseWidgetData.placeholder
    val isLocked = !isPremium

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
                    .padding(16.dp),
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
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Skeleton icon block
                            Box(
                                modifier = GlanceModifier
                                    .size(16.dp)
                                    .background(ImageProvider(R.drawable.widget_skeleton_line)),
                            ) {}
                            Spacer(modifier = GlanceModifier.width(6.dp))
                            // Skeleton text block
                            Box(
                                modifier = GlanceModifier
                                    .width(50.dp)
                                    .height(12.dp)
                                    .background(ImageProvider(R.drawable.widget_skeleton_line)),
                            ) {}
                        }
                    }

                    Spacer(modifier = GlanceModifier.defaultWeight())

                    // Quote icon skeleton
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_quote),
                        contentDescription = null,
                        modifier = GlanceModifier.size(32.dp),
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
                                .height(20.dp)
                                .background(ImageProvider(R.drawable.widget_skeleton_line)),
                        ) {}
                        Spacer(modifier = GlanceModifier.height(10.dp))
                        // Line 2 — full width
                        Box(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .height(20.dp)
                                .background(ImageProvider(R.drawable.widget_skeleton_line)),
                        ) {}
                        Spacer(modifier = GlanceModifier.height(10.dp))
                        // Line 3 — 85% width
                        Box(
                            modifier = GlanceModifier
                                .width(240.dp)
                                .height(20.dp)
                                .background(ImageProvider(R.drawable.widget_skeleton_line)),
                        ) {}
                        Spacer(modifier = GlanceModifier.height(10.dp))
                        // Line 4 — 60% width
                        Box(
                            modifier = GlanceModifier
                                .width(170.dp)
                                .height(20.dp)
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
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .width(60.dp)
                                .height(12.dp)
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
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_widget_lock),
                            contentDescription = null,
                            modifier = GlanceModifier.size(18.dp),
                        )
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        Text(
                            text = context.getString(R.string.widget_unlock),
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 17.sp,
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
                    .padding(16.dp),
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = GlanceModifier
                            .background(ImageProvider(R.drawable.widget_badge_bg))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                provider = ImageProvider(categoryIconRes(data.category)),
                                contentDescription = null,
                                modifier = GlanceModifier.size(16.dp),
                            )
                            Spacer(modifier = GlanceModifier.width(6.dp))
                            Text(
                                text = categoryName(data.category),
                                style = TextStyle(
                                    color = ColorProvider(Color.White),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.defaultWeight())

                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_quote),
                        contentDescription = null,
                        modifier = GlanceModifier.size(32.dp),
                        colorFilter = androidx.glance.ColorFilter.tint(
                            ColorProvider(Color.White.copy(alpha = 0.5f)),
                        ),
                    )
                }

                Box(
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                        .padding(horizontal = 2.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = data.text,
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        maxLines = 8,
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
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = data.reference,
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }
            }
        }
    }
}

class VerseLargeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = VerseLargeWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        VerseWidgetWorker.enqueuePeriodicWork(context)
    }
}
