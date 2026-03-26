package com.app.lumen.features.subscription

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.lumen.R
import com.app.lumen.ui.theme.NearBlack
import com.app.lumen.ui.theme.SoftGold
import com.revenuecat.purchases.Package

private val GlassBg = Color(0xFF2A2A2E).copy(alpha = 0.86f)
private val SelectedBorder = SoftGold
private val UnselectedBorder = Color.White.copy(alpha = 0.2f)

// Translucent glass style matching Rosary prayer buttons
private val CloseButtonBg = Color.White.copy(alpha = 0.12f)
private val CloseButtonBorder = Color.White.copy(alpha = 0.20f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallSheet(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var selectedPackageId by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val packages by SubscriptionManager.availablePackages.collectAsState()
    val monthlyPkg = SubscriptionManager.monthlyPackage
    val yearlyPkg = SubscriptionManager.yearlyPackage

    LaunchedEffect(packages) {
        if (packages.isNotEmpty() && selectedPackageId == null) {
            selectedPackageId = yearlyPkg?.identifier ?: monthlyPkg?.identifier
        }
    }

    LaunchedEffect(Unit) {
        SubscriptionManager.fetchOfferings()
    }

    val selectedPkg = packages.firstOrNull { it.identifier == selectedPackageId }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.Transparent,
        dragHandle = null,
        sheetMaxWidth = Int.MAX_VALUE.dp,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        modifier = Modifier.statusBarsPadding().padding(top = 24.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        ) {
            // Background — fills everything including behind nav bar
            Image(
                painter = painterResource(R.drawable.paywall_bg),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(4.dp),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to NearBlack.copy(alpha = 0.7f),
                                0.4f to NearBlack.copy(alpha = 0.5f),
                                0.7f to NearBlack.copy(alpha = 0.7f),
                                1.0f to NearBlack.copy(alpha = 0.9f),
                            ),
                        ),
                    ),
            )

            // Content
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(16.dp))

                HeaderSection()

                Spacer(Modifier.weight(0.5f))

                // Features — edge to edge
                FeatureCarousel()

                Spacer(Modifier.weight(1f))

                // Plan cards
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    if (packages.isEmpty()) {
                        CircularProgressIndicator(
                            color = SoftGold,
                            modifier = Modifier
                                .padding(24.dp)
                                .align(Alignment.CenterHorizontally),
                        )
                    } else {
                        monthlyPkg?.let { pkg ->
                            val trialText = pkg.product.subscriptionOptions
                                ?.freeTrial?.freePhase?.billingPeriod
                            val title = if (trialText != null) {
                                "${formatTrialPeriod(trialText)} free trial"
                            } else {
                                "Monthly"
                            }
                            val priceStr = pkg.product.price.formatted
                            val subtitle = if (trialText != null) "then $priceStr/month" else "$priceStr/month"
                            val weeklyPrice = SubscriptionManager.formatWeeklyPrice(pkg, 4)
                            PlanCard(
                                title = title,
                                subtitle = subtitle,
                                weeklyPrice = weeklyPrice?.let { "$it/week" },
                                isSelected = selectedPackageId == pkg.identifier,
                                onClick = { selectedPackageId = pkg.identifier },
                            )
                            Spacer(Modifier.height(12.dp))
                        }

                        yearlyPkg?.let { pkg ->
                            val trialText = pkg.product.subscriptionOptions
                                ?.freeTrial?.freePhase?.billingPeriod
                            val title = if (trialText != null) {
                                "${formatTrialPeriod(trialText)} free trial"
                            } else {
                                "Annual"
                            }
                            val priceStr = pkg.product.price.formatted
                            val subtitle = if (trialText != null) "then $priceStr/year" else "$priceStr/year"
                            val weeklyPrice = SubscriptionManager.formatWeeklyPrice(pkg, 52)
                            PlanCard(
                                title = title,
                                subtitle = subtitle,
                                weeklyPrice = weeklyPrice?.let { "$it/week" },
                                isSelected = selectedPackageId == pkg.identifier,
                                onClick = { selectedPackageId = pkg.identifier },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                // CTA Button
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    val hasFreeTrial = selectedPkg?.product?.subscriptionOptions
                        ?.freeTrial?.freePhase != null
                    Button(
                        onClick = {
                            val pkg = selectedPkg ?: return@Button
                            val act = activity ?: return@Button
                            isLoading = true
                            SubscriptionManager.purchase(
                                activity = act,
                                pkg = pkg,
                                onSuccess = {
                                    isLoading = false
                                    onDismiss()
                                },
                                onError = { msg ->
                                    isLoading = false
                                    errorMessage = msg
                                    showError = true
                                },
                                onCancelled = {
                                    isLoading = false
                                },
                            )
                        },
                        enabled = !isLoading && selectedPkg != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SoftGold,
                            contentColor = Color.White,
                            disabledContainerColor = SoftGold.copy(alpha = 0.5f),
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.Black,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp),
                            )
                        } else {
                            Text(
                                text = if (hasFreeTrial) "Start Free Trial" else "Subscribe Now",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Legal links
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/intl/en_us/about/play-terms/")),
                        )
                    }) {
                        Text("Terms", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
                    }
                    Text("•", fontSize = 12.sp, color = Color.White.copy(alpha = 0.3f))
                    TextButton(onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://maranatha.app/privacy")),
                        )
                    }) {
                        Text("Privacy", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
                    }
                    Text("•", fontSize = 12.sp, color = Color.White.copy(alpha = 0.3f))
                    TextButton(onClick = {
                        isLoading = true
                        SubscriptionManager.restorePurchases(
                            onSuccess = { hasAccess ->
                                isLoading = false
                                if (hasAccess) {
                                    onDismiss()
                                } else {
                                    errorMessage = "No active subscription found."
                                    showError = true
                                }
                            },
                            onError = { msg ->
                                isLoading = false
                                errorMessage = msg
                                showError = true
                            },
                        )
                    }) {
                        Text("Restore", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
                    }
                }

                Spacer(Modifier.navigationBarsPadding().height(8.dp))
            }

            // Close button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 14.dp, end = 14.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CloseButtonBg)
                    .border(0.5.dp, CloseButtonBorder, CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }

    if (showError) {
        AlertDialog(
            onDismissRequest = { showError = false },
            title = { Text("Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { showError = false }) {
                    Text("OK", color = SoftGold)
                }
            },
            containerColor = Color(0xFF1E1E32),
            titleContentColor = Color.White,
            textContentColor = Color.White,
        )
    }
}

// ── Header ──────────────────────────────────────────────────

@Composable
private fun HeaderSection() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row {
            Text(
                text = "Lumen",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = SoftGold,
            )
            Text(
                text = " Pro",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Unlock the Full\nSpiritual Experience",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            lineHeight = 28.sp,
        )
    }
}

// ── Feature Carousel ────────────────────────────────────────
// Uses LazyRow with virtual infinite items (Int.MAX_VALUE / 2 centered)
// and frame-based auto-scrolling via scrollBy — the standard Android pattern.

private data class PaywallFeature(
    val icon: ImageVector,
    val title: String,
    val description: String,
)

private val features = listOf(
    PaywallFeature(Icons.AutoMirrored.Filled.VolumeUp, "Audio", "Listen to readings and prayers"),
    PaywallFeature(Icons.Filled.FormatQuote, "Daily Reflections", "Spiritual insights for each day"),
    PaywallFeature(Icons.Filled.Book, "Full Bible Access", "All books of the Old & New Testament"),
    PaywallFeature(Icons.Filled.FrontHand, "All Prayers", "Rosary, chaplets & litanies"),
    PaywallFeature(Icons.Filled.Widgets, "Widgets", "Daily verse on your home screen"),
    PaywallFeature(Icons.Filled.Lightbulb, "Shape the App", "Suggest new features & ideas"),
)

private const val CARD_WIDTH_DP = 280f
private const val CARD_SPACING_DP = 10f
private const val SCROLL_SPEED_DP_PER_SEC = 30f

@Composable
private fun FeatureCarousel() {
    val configuration = LocalContext.current.resources.configuration
    val screenHeightDp = configuration.screenHeightDp
    val singleRow = screenHeightDp < 750

    if (singleRow) {
        // Small devices: all 6 features in one row
        AutoScrollRow(items = features, scrollRight = true)
    } else {
        // Normal devices: two rows scrolling in opposite directions
        val row1 = features.take(3)
        val row2 = features.drop(3)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AutoScrollRow(items = row1, scrollRight = true)
            AutoScrollRow(items = row2, scrollRight = false)
        }
    }
}

@Composable
private fun AutoScrollRow(
    items: List<PaywallFeature>,
    scrollRight: Boolean,
) {
    val density = LocalDensity.current
    val speedPx = with(density) { SCROLL_SPEED_DP_PER_SEC.dp.toPx() }

    // Start in the middle of virtual range to allow scrolling in both directions
    val startIndex = (Int.MAX_VALUE / 2) - ((Int.MAX_VALUE / 2) % items.size)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)

    LaunchedEffect(Unit) {
        var lastFrameMs = 0L
        while (true) {
            var deltaMs = 0L
            withInfiniteAnimationFrameMillis { frameMs ->
                if (lastFrameMs != 0L) {
                    deltaMs = frameMs - lastFrameMs
                }
                lastFrameMs = frameMs
            }
            if (deltaMs > 0L) {
                // Time-based scrolling: same speed regardless of frame rate
                val px = speedPx * (deltaMs / 1000f)
                listState.scrollBy(if (scrollRight) -px else px)
            }
        }
    }

    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(CARD_SPACING_DP.dp),
        userScrollEnabled = false,
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp),
    ) {
        items(Int.MAX_VALUE) { index ->
            val item = items[index.mod(items.size)]
            FeatureCard(item, modifier = Modifier.width(CARD_WIDTH_DP.dp))
        }
    }
}

@Composable
private fun FeatureCard(
    feature: PaywallFeature,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(82.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(GlassBg)
            .border(1.dp, SoftGold.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(SoftGold.copy(alpha = 0.3f), SoftGold.copy(alpha = 0.1f)),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = feature.icon,
                contentDescription = null,
                tint = SoftGold,
                modifier = Modifier.size(20.dp),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = feature.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
            )
            Text(
                text = feature.description,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 2,
                lineHeight = 16.sp,
            )
        }

        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = SoftGold,
            modifier = Modifier.size(20.dp),
        )
    }
}

// ── Plan Card ───────────────────────────────────────────────

@Composable
private fun PlanCard(
    title: String,
    subtitle: String,
    weeklyPrice: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) SelectedBorder else UnselectedBorder,
        animationSpec = tween(200),
        label = "plan_border",
    )
    val bgAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.10f else 0.05f,
        animationSpec = tween(200),
        label = "plan_bg",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = bgAlpha))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .border(2.dp, if (isSelected) SoftGold else Color.White.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(SoftGold),
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.6f),
            )
        }

        if (weeklyPrice != null) {
            Text(
                text = weeklyPrice,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.6f),
            )
        }
    }
}

// ── Helpers ─────────────────────────────────────────────────

private fun formatTrialPeriod(period: com.revenuecat.purchases.models.Period?): String {
    if (period == null) return ""
    val value = period.value
    val unit = period.unit
    return when (unit) {
        com.revenuecat.purchases.models.Period.Unit.DAY -> "$value days"
        com.revenuecat.purchases.models.Period.Unit.WEEK -> "${value * 7} days"
        com.revenuecat.purchases.models.Period.Unit.MONTH -> "$value months"
        com.revenuecat.purchases.models.Period.Unit.YEAR -> "$value years"
        else -> ""
    }
}
