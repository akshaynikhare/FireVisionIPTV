package com.cadnative.firevisioniptv.presentation.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cadnative.firevisioniptv.presentation.navigation.Screen
import com.cadnative.firevisioniptv.presentation.ui.theme.Amber
import com.cadnative.firevisioniptv.presentation.ui.theme.BackgroundDark
import com.cadnative.firevisioniptv.presentation.ui.theme.BackgroundMedium
import com.cadnative.firevisioniptv.presentation.ui.theme.FocusGlow
import com.cadnative.firevisioniptv.presentation.ui.theme.TextDim
import com.cadnative.firevisioniptv.presentation.ui.theme.TextPrimary
import com.cadnative.firevisioniptv.presentation.ui.theme.TextSecondary

private data class NavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String
)

private val topNavItems = listOf(
    NavItem(Screen.Home, Icons.Default.Home, "Home"),
    NavItem(Screen.Search, Icons.Default.Search, "Search"),
    NavItem(Screen.Channels, Icons.Default.LiveTv, "Channels"),
    NavItem(Screen.Categories, Icons.Default.Category, "Categories"),
    NavItem(Screen.Favorites, Icons.Default.Favorite, "Favorites"),
)

private val bottomNavItem = NavItem(Screen.Settings, Icons.Default.Settings, "Settings")

@Composable
fun SideNavRail(
    currentRoute: String?,
    onScreenSelected: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    val railWidth by animateDpAsState(
        targetValue = if (isExpanded) 220.dp else 72.dp,
        animationSpec = tween(durationMillis = 200),
        label = "railWidth"
    )

    Column(
        modifier = modifier
            .width(railWidth)
            .fillMaxHeight()
            .background(
                Brush.verticalGradient(
                    colors = listOf(BackgroundMedium, BackgroundDark)
                )
            )
            .onFocusChanged { focusState ->
                isExpanded = focusState.hasFocus
            }
            .focusGroup()
            .padding(vertical = 24.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Brand mark
        Text(
            text = if (isExpanded) "FireVision" else "FV",
            color = Amber,
            fontSize = if (isExpanded) 20.sp else 14.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = if (isExpanded) (-0.5).sp else 1.sp,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Top nav items
        topNavItems.forEach { item ->
            NavRailItem(
                icon = item.icon,
                label = item.label,
                isSelected = currentRoute == item.screen.route,
                isExpanded = isExpanded,
                onClick = { onScreenSelected(item.screen) }
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        // Settings at bottom
        NavRailItem(
            icon = bottomNavItem.icon,
            label = bottomNavItem.label,
            isSelected = currentRoute == bottomNavItem.screen.route,
            isExpanded = isExpanded,
            onClick = { onScreenSelected(bottomNavItem.screen) }
        )
    }
}

@Composable
private fun NavRailItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    val contentColor by animateColorAsState(
        targetValue = when {
            isFocused -> TextPrimary
            isSelected -> Amber
            else -> TextSecondary
        },
        animationSpec = tween(durationMillis = 150),
        label = "navItemContent"
    )

    val glowAlpha by animateColorAsState(
        targetValue = when {
            isFocused -> FocusGlow
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 150),
        label = "navItemGlow"
    )

    val shape = RoundedCornerShape(10.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .drawBehind {
                drawRoundRect(
                    color = glowAlpha,
                    cornerRadius = CornerRadius(10.dp.toPx())
                )
            }
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center
    ) {
        // Accent bar for selected item
        if (isSelected && !isFocused) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(20.dp)
                    .background(Amber, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(if (isExpanded) 11.dp else 8.dp))
        }

        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )

        if (isExpanded) {
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = label,
                color = contentColor,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                letterSpacing = 0.15.sp
            )
        }
    }
}
