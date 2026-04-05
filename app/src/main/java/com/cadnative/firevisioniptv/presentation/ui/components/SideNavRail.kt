package com.cadnative.firevisioniptv.presentation.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_FAST
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_NORMAL
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cadnative.firevisioniptv.R
import androidx.compose.material3.MaterialTheme
import com.cadnative.firevisioniptv.presentation.navigation.Screen
import com.cadnative.firevisioniptv.presentation.ui.theme.FocusGlow

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
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    var isExpanded by remember { mutableStateOf(false) }

    val collapsedWidth = if (compact) 52.dp else 72.dp
    val expandedWidth = if (compact) 180.dp else 220.dp

    val railWidth by animateDpAsState(
        targetValue = if (isExpanded) expandedWidth else collapsedWidth,
        animationSpec = tween(durationMillis = DURATION_NORMAL, easing = EaseOutQuart),
        label = "railWidth"
    )

    val surfaceColor = MaterialTheme.colorScheme.surface
    val bgColor = MaterialTheme.colorScheme.background

    Column(
        modifier = modifier
            .width(railWidth)
            .fillMaxHeight()
            .background(
                Brush.verticalGradient(
                    colors = listOf(surfaceColor, bgColor)
                )
            )
            .onFocusChanged { focusState ->
                isExpanded = focusState.hasFocus
            }
            .focusGroup()
            .padding(
                vertical = if (compact) 8.dp else 24.dp,
                horizontal = if (compact) 4.dp else 8.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Brand mark — app flame icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = if (compact) 4.dp else 12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "FireVision",
                modifier = Modifier.size(if (compact) 40.dp else 64.dp)
            )
            if (isExpanded) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "FireVision",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = if (compact) 16.sp else 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                )
            }
        }

        Spacer(modifier = Modifier.height(if (compact) 8.dp else 32.dp))

        // Top nav items
        topNavItems.forEach { item ->
            NavRailItem(
                icon = item.icon,
                label = item.label,
                isSelected = currentRoute == item.screen.route ||
                    (item.screen == Screen.Channels && currentRoute == Screen.ChannelsByCategory.route),
                isExpanded = isExpanded,
                compact = compact,
                onClick = { onScreenSelected(item.screen) }
            )
            Spacer(modifier = Modifier.height(if (compact) 2.dp else 6.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        // Settings at bottom
        NavRailItem(
            icon = bottomNavItem.icon,
            label = bottomNavItem.label,
            isSelected = currentRoute == bottomNavItem.screen.route,
            isExpanded = isExpanded,
            compact = compact,
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
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val contentColor by animateColorAsState(
        targetValue = when {
            isFocused -> onSurface
            isSelected -> primaryColor
            else -> onSurfaceVariant
        },
        animationSpec = tween(durationMillis = DURATION_FAST, easing = EaseOutQuart),
        label = "navItemContent"
    )

    val glowAlpha by animateColorAsState(
        targetValue = when {
            isFocused -> FocusGlow
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = DURATION_FAST, easing = EaseOutQuart),
        label = "navItemGlow"
    )

    val shape = RoundedCornerShape(14.dp)

    val selectedBg by animateColorAsState(
        targetValue = when {
            isFocused -> Color.Transparent
            isSelected -> primaryColor.copy(alpha = 0.12f)
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = DURATION_FAST, easing = EaseOutQuart),
        label = "navItemBg"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .drawBehind {
                drawRoundRect(
                    color = glowAlpha,
                    cornerRadius = CornerRadius(14.dp.toPx())
                )
                drawRoundRect(
                    color = selectedBg,
                    cornerRadius = CornerRadius(14.dp.toPx())
                )
            }
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(
                horizontal = if (compact) 10.dp else 14.dp,
                vertical = if (compact) 8.dp else 14.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(if (compact) 22.dp else 28.dp)
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
