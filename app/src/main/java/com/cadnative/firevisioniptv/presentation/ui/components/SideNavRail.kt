package com.cadnative.firevisioniptv.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.cadnative.firevisioniptv.R
import com.cadnative.firevisioniptv.presentation.navigation.Screen
import com.cadnative.firevisioniptv.presentation.ui.LocalPerfProfile
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_FAST
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_NORMAL
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import com.cadnative.firevisioniptv.presentation.ui.animation.FOCUS_SCALE_SUBTLE
import com.cadnative.firevisioniptv.presentation.ui.theme.Dimens
import com.cadnative.firevisioniptv.presentation.ui.theme.Elevation
import com.cadnative.firevisioniptv.presentation.ui.theme.Void900

private data class NavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String
)

private val topNavItems = listOf(
    NavItem(Screen.Home, Icons.Default.Home, "Home"),
    NavItem(Screen.Favorites, Icons.Default.Favorite, "Favorites"),
    NavItem(Screen.Search, Icons.Default.Search, "Search"),
    NavItem(Screen.Categories, Icons.Default.Category, "Categories"),
    NavItem(Screen.Guide, Icons.Default.GridView, "Guide"),
)

private val bottomNavItem = NavItem(Screen.Settings, Icons.Default.Settings, "Settings")

private fun NavItem.isSelectedFor(currentRoute: String?): Boolean =
    currentRoute == screen.route ||
        (screen == Screen.Channels && currentRoute == Screen.ChannelsByCategory.route)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SideNavRail(
    currentRoute: String?,
    onScreenSelected: (Screen) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    var isExpanded by remember { mutableStateOf(false) }
    val reduceMotion = LocalPerfProfile.current.reduceMotion

    val collapsedWidth = if (compact) Dimens.NavRailCollapsedMobile else Dimens.NavRailCollapsedTv
    val expandedWidth = if (compact) Dimens.NavRailExpandedMobile else Dimens.NavRailExpandedTv

    val railWidth by animateDpAsState(
        targetValue = if (isExpanded) expandedWidth else collapsedWidth,
        animationSpec = if (reduceMotion) snap() else tween(DURATION_NORMAL, easing = EaseOutQuart),
        label = "railWidth"
    )

    val labelEnter = if (reduceMotion) {
        EnterTransition.None
    } else {
        fadeIn(tween(DURATION_NORMAL, easing = EaseOutQuart)) +
            expandHorizontally(tween(DURATION_NORMAL, easing = EaseOutQuart))
    }
    val labelExit = if (reduceMotion) {
        ExitTransition.None
    } else {
        fadeOut(tween(DURATION_FAST, easing = EaseOutQuart)) +
            shrinkHorizontally(tween(DURATION_FAST, easing = EaseOutQuart))
    }

    // Restore focus to the current screen's item when the rail regains focus.
    val restoreFocusRequester = remember { FocusRequester() }
    val selectedTopIndex = topNavItems.indexOfFirst { it.isSelectedFor(currentRoute) }
    val settingsHoldsRestore = selectedTopIndex < 0 && bottomNavItem.isSelectedFor(currentRoute)
    val restoreIndex = if (selectedTopIndex >= 0) selectedTopIndex else 0

    Column(
        modifier = modifier
            .width(railWidth)
            .fillMaxHeight()
            .background(Void900)
            .onFocusChanged { isExpanded = it.hasFocus }
            .focusRestorer { restoreFocusRequester }
            .focusProperties {
                // DPAD_RIGHT always leaves the rail into content; left edge is a wall.
                exit = { direction ->
                    if (direction == FocusDirection.Left) FocusRequester.Cancel
                    else FocusRequester.Default
                }
            }
            .focusGroup()
            .padding(
                vertical = if (compact) Dimens.Space2 else Dimens.Space5,
                horizontal = if (compact) Dimens.Space1 else Dimens.Space2
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Brand mark
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = if (compact) Dimens.Space1 else Dimens.Space2)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "FireVision",
                modifier = Modifier.size(if (compact) Dimens.NavRailBrandIconMobile else Dimens.NavRailBrandIconTv)
            )
            AnimatedVisibility(visible = isExpanded, enter = labelEnter, exit = labelExit) {
                Text(
                    text = "FireVision",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    modifier = Modifier.padding(start = Dimens.NavBrandLabelGap)
                )
            }
        }

        Spacer(modifier = Modifier.height(if (compact) Dimens.Space2 else Dimens.Space4))

        topNavItems.forEachIndexed { index, item ->
            NavRailItem(
                icon = item.icon,
                label = item.label,
                isSelected = index == selectedTopIndex,
                isExpanded = isExpanded,
                labelEnter = labelEnter,
                labelExit = labelExit,
                reduceMotion = reduceMotion,
                compact = compact,
                onClick = { onScreenSelected(item.screen) },
                modifier = if (!settingsHoldsRestore && index == restoreIndex) {
                    Modifier.focusRequester(restoreFocusRequester)
                } else {
                    Modifier
                }
            )
            Spacer(modifier = Modifier.height(if (compact) Dimens.NavItemGapMobile else Dimens.NavItemGapTv))
        }

        Spacer(modifier = Modifier.weight(1f))

        NavRailItem(
            icon = bottomNavItem.icon,
            label = bottomNavItem.label,
            isSelected = settingsHoldsRestore,
            isExpanded = isExpanded,
            labelEnter = labelEnter,
            labelExit = labelExit,
            reduceMotion = reduceMotion,
            compact = compact,
            onClick = { onScreenSelected(bottomNavItem.screen) },
            modifier = if (settingsHoldsRestore) {
                Modifier.focusRequester(restoreFocusRequester)
            } else {
                Modifier
            }
        )
    }
}

@Composable
private fun NavRailItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    isExpanded: Boolean,
    labelEnter: EnterTransition,
    labelExit: ExitTransition,
    reduceMotion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }

    val colorSpec: AnimationSpec<Color> =
        if (reduceMotion) snap() else tween(DURATION_FAST, easing = EaseOutQuart)

    val contentColor by animateColorAsState(
        targetValue = when {
            isFocused -> MaterialTheme.colorScheme.onPrimary
            isSelected -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = colorSpec,
        label = "navItemContent"
    )

    // Solid brand fill when focused (YouTube-TV flat); a subtle primary tint
    // marks the selected-but-unfocused item so the current screen stays legible
    // even when focus has moved into content.
    val pillColor by animateColorAsState(
        targetValue = when {
            isFocused -> MaterialTheme.colorScheme.primary
            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
            else -> Color.Transparent
        },
        animationSpec = colorSpec,
        label = "navItemPill"
    )

    val pillShape = MaterialTheme.shapes.medium

    Row(
        modifier = modifier
            .fillMaxWidth()
            // Unified TV focus treatment: subtle scale + brand glow, like every
            // other surface. Glow only reads on the focused pill (no resting glow
            // needed for a flat rail), so keep resting elevation flush.
            .tvFocusVisuals(
                focused = isFocused,
                shape = pillShape,
                focusedScale = FOCUS_SCALE_SUBTLE,
                restingElevation = Elevation.Level0
            )
            .clip(pillShape)
            .background(pillColor)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(
                horizontal = if (compact) Dimens.NavItemPaddingHMobile else Dimens.NavItemPaddingHTv,
                vertical = if (compact) Dimens.NavItemPaddingVMobile else Dimens.NavItemPaddingVTv
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center
    ) {
        // Leading accent bar marks the selected item when the rail is expanded.
        // Shown for the whole selected state (not gated on focus) so the icon
        // never shifts as focus moves in and out. On the focused item it reads
        // against the solid fill; otherwise against the subtle tint.
        if (isSelected && isExpanded) {
            Box(
                modifier = Modifier
                    .padding(end = Dimens.Space2)
                    .width(Dimens.NavItemIndicatorWidth)
                    .height(Dimens.NavItemIndicatorHeight)
                    .clip(CircleShape)
                    .background(
                        if (isFocused) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.primary
                    )
            )
        }

        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(if (compact) Dimens.IconMedium else Dimens.IconLarge)
        )

        AnimatedVisibility(visible = isExpanded, enter = labelEnter, exit = labelExit) {
            Text(
                text = label,
                color = contentColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                modifier = Modifier.padding(start = Dimens.NavItemLabelGap)
            )
        }
    }
}
