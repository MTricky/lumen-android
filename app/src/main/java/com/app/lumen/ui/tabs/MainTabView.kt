package com.app.lumen.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.lumen.features.liturgy.ui.LiturgyScreen
import com.app.lumen.ui.theme.NearBlack
import com.app.lumen.ui.theme.Slate
import com.app.lumen.ui.theme.SoftGold

enum class Tab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    LITURGY("Liturgy", Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook),
    BIBLE("Bible", Icons.Filled.Book, Icons.Outlined.Book),
    PRAYERS("Prayers", Icons.Filled.GridView, Icons.Outlined.GridView),
    CALENDAR("Calendar", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
}

@Composable
fun MainTabView() {
    var selectedTab by remember { mutableStateOf(Tab.LITURGY) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = NearBlack,
                contentColor = Slate,
                tonalElevation = 0.dp,
            ) {
                Tab.entries.forEach { tab ->
                    val isSelected = tab == selectedTab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.label,
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                fontSize = 10.sp,
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SoftGold,
                            selectedTextColor = SoftGold,
                            unselectedIconColor = Slate,
                            unselectedTextColor = Slate,
                            indicatorColor = SoftGold.copy(alpha = 0.12f),
                        ),
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            Tab.LITURGY -> LiturgyScreen(bottomPadding = innerPadding.calculateBottomPadding())
            else -> Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                PlaceholderScreen(tab = selectedTab)
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(tab: Tab) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NearBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = tab.selectedIcon,
                contentDescription = null,
                tint = SoftGold.copy(alpha = 0.3f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = tab.label,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Coming soon",
                fontSize = 14.sp,
                color = Slate,
            )
        }
    }
}
