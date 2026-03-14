package com.cadnative.firevisioniptv.presentation.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cadnative.firevisioniptv.presentation.navigation.Screen
import com.cadnative.firevisioniptv.presentation.ui.theme.BackgroundMedium
import com.cadnative.firevisioniptv.presentation.ui.theme.FireOrange
import com.cadnative.firevisioniptv.presentation.ui.theme.TextSecondary

/**
 * Data class for navigation rail items.
 */
private data class NavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String
)

/** Top section nav items (Home through Favorites). */
private val topNavItems = listOf(
    NavItem(Screen.Home, Icons.Default.Home, "Home"),
    NavItem(Screen.Search, Icons.Default.Search, "Search"),
    NavItem(Screen.Channels, Icons.Default.LiveTv, "Channels"),
    NavItem(Screen.Favorites, Icons.Default.Favorite, "Favorites"),
)

/** Bottom section nav item. */
private val bottomNavItem = NavItem(Screen.Settings, Icons.Default.Settings, "Settings")

/**
 * Left rail sidebar for TV navigation.
 *
 * Collapsed (~80dp) by default showing icons only.
 * Expands (~220dp) when any child receives D-pad focus, revealing labels.
 *
 * @param currentRoute The currently active route for selection highlighting.
 * @param onScreenSelected Called when a sidebar item is clicked/selected.
 */
@Composable
fun SideNavRail(
    currentRoute: String?,
    onScreenSelected: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    val railWidth by animateDpAsState(
        targetValue = if (isExpanded) 220.dp else 80.dp,
        animationSpec = tween(durationMillis = 250),
        label = "railWidth"
    )

    Column(
        modifier = modifier
            .width(railWidth)
            .fillMaxHeight()
            .background(BackgroundMedium)
            .onFocusChanged { focusState ->
                isExpanded = focusState.hasFocus
            }
            .focusGroup()
            .padding(vertical = 20.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Brand mark
        Text(
            text = if (isExpanded) "FireVision" else "FV",
            color = FireOrange,
            fontSize = if (isExpanded) 20.sp else 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Top nav items
        topNavItems.forEach { item ->
            NavRailItem(
                icon = item.icon,
                label = item.label,
                isSelected = currentRoute == item.screen.route,
                isExpanded = isExpanded,
                onClick = { onScreenSelected(item.screen) }
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Push settings to bottom
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

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isFocused -> FireOrange.copy(alpha = 0.25f)
            isSelected -> FireOrange.copy(alpha = 0.12f)
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 150),
        label = "navItemBg"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            isFocused -> Color.White
            isSelected -> FireOrange
            else -> TextSecondary
        },
        animationSpec = tween(durationMillis = 150),
        label = "navItemContent"
    )

    val shape = RoundedCornerShape(12.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isFocused) Modifier.border(2.dp, Color.White, shape)
                else Modifier
            )
            .clip(shape)
            .background(backgroundColor)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )

        if (isExpanded) {
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                color = contentColor,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}
