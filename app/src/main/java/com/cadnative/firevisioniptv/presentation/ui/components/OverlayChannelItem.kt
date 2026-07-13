package com.cadnative.firevisioniptv.presentation.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cadnative.firevisioniptv.presentation.model.ChannelUiModel
import com.cadnative.firevisioniptv.presentation.ui.LocalPerfProfile
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_NORMAL
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import com.cadnative.firevisioniptv.presentation.ui.theme.Amber
import com.cadnative.firevisioniptv.presentation.ui.theme.BackgroundDark
import com.cadnative.firevisioniptv.presentation.ui.theme.FocusBorder
import com.cadnative.firevisioniptv.presentation.ui.theme.LabelBadge
import com.cadnative.firevisioniptv.presentation.ui.theme.ShapeSmall
import com.cadnative.firevisioniptv.presentation.ui.theme.TextSecondary
import com.cadnative.firevisioniptv.presentation.ui.theme.categoryColor
import com.cadnative.firevisioniptv.presentation.ui.theme.subtleBorder

internal val OverlayChannelCardWidth = 160.dp
internal val OverlayChannelCardHeight = 96.dp

private val BadgeShape = RoundedCornerShape(bottomEnd = 8.dp, topStart = 8.dp)

@Composable
internal fun OverlayChannelItem(
    channel: ChannelUiModel,
    isCurrentChannel: Boolean,
    isLastChannel: Boolean = false,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.width(OverlayChannelCardWidth).height(OverlayChannelCardHeight)) {
        ChannelCard(
            channel = channel,
            onClick = onClick,
            onFavoriteClick = onFavoriteClick,
            modifier = Modifier.fillMaxSize()
        )

        // "NOW" badge for the currently playing channel, "LAST" for the pinned recall card
        if (isCurrentChannel || isLastChannel) {
            Surface(
                shape = BadgeShape,
                color = if (isCurrentChannel) Amber else TextSecondary,
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(
                    text = if (isCurrentChannel) "NOW" else "LAST",
                    style = LabelBadge,
                    fontWeight = FontWeight.Bold,
                    color = BackgroundDark,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
internal fun OverlayCategoryChips(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Auto-scroll to the selected chip; index 0 = "All", categories start at 1
    val selectedIndex = if (selectedCategory == null) 0
    else categories.indexOf(selectedCategory).let { if (it >= 0) it + 1 else 0 }
    LaunchedEffect(selectedCategory) {
        listState.scrollToItem(maxOf(0, selectedIndex - 1))
    }

    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        item(key = "all") {
            OverlayFilterChip(
                label = "All",
                isSelected = selectedCategory == null,
                selectedColor = Amber,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                onClick = { onCategorySelected(null) }
            )
        }
        items(categories, key = { it }) { category ->
            OverlayFilterChip(
                label = category,
                isSelected = selectedCategory == category,
                selectedColor = categoryColor(category),
                selectedLabelColor = MaterialTheme.colorScheme.background,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

@Composable
private fun OverlayFilterChip(
    label: String,
    isSelected: Boolean,
    selectedColor: Color,
    selectedLabelColor: Color,
    onClick: () -> Unit
) {
    val reduceMotion = LocalPerfProfile.current.reduceMotion
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused && !reduceMotion) 1.12f else 1f,
        animationSpec = tween(DURATION_NORMAL, easing = EaseOutQuart),
        label = "chipScale"
    )
    val borderStroke: BorderStroke? = when {
        isFocused -> BorderStroke(2.5.dp, FocusBorder)
        !isSelected -> BorderStroke(1.dp, subtleBorder)
        else -> null
    }

    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontWeight = if (isSelected || isFocused) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isFocused && !isSelected) Amber else Color.Unspecified
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = selectedColor,
            selectedLabelColor = selectedLabelColor,
            containerColor = if (isFocused) Amber.copy(alpha = 0.15f) else Color.Transparent
        ),
        border = borderStroke,
        shape = ShapeSmall,
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged { isFocused = it.isFocused }
    )
}
