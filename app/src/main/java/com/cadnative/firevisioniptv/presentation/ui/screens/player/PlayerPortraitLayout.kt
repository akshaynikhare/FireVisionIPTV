package com.cadnative.firevisioniptv.presentation.ui.screens.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cadnative.firevisioniptv.presentation.model.ChannelUiModel
import com.cadnative.firevisioniptv.presentation.model.PlayerUiState
import com.cadnative.firevisioniptv.presentation.ui.components.EpgProgressBar
import com.cadnative.firevisioniptv.presentation.ui.components.formatEpgTimeRange
import com.cadnative.firevisioniptv.presentation.ui.theme.Amber
import com.cadnative.firevisioniptv.presentation.ui.theme.BodyOverlay
import com.cadnative.firevisioniptv.presentation.ui.theme.Dimens
import com.cadnative.firevisioniptv.presentation.ui.theme.EmphasisMedium
import com.cadnative.firevisioniptv.presentation.ui.theme.LabelBadge
import com.cadnative.firevisioniptv.presentation.ui.theme.LabelToast
import com.cadnative.firevisioniptv.presentation.ui.theme.OnVideo
import com.cadnative.firevisioniptv.presentation.ui.theme.ShapeSmall
import com.cadnative.firevisioniptv.presentation.ui.theme.SurfaceElevated
import com.cadnative.firevisioniptv.presentation.ui.theme.categoryColor

/** Callbacks for the portrait sections below the docked video. */
internal class PortraitSectionActions(
    val onToggleFavorite: () -> Unit,
    val onShowTracks: () -> Unit,
    val onCycleAspect: () -> Unit,
    val onCycleSleepTimer: (Int?) -> Unit,
    val onEnterPip: () -> Unit,
    val onZapTo: (String) -> Unit
)

/**
 * Portrait 3-section body under the docked 16:9 player: channel detail
 * (logo, name, category, favorite, Now/Next + progress, action row) and the
 * Channels/Schedule tabs. Always visible — no auto-hide in portrait.
 */
@Composable
internal fun PlayerPortraitSections(
    uiState: PlayerUiState,
    zapChannels: List<ChannelUiModel>,
    aspectLabel: String,
    actions: PortraitSectionActions,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        PortraitDetailSection(
            uiState = uiState,
            aspectLabel = aspectLabel,
            actions = actions
        )
        PortraitTabs(
            uiState = uiState,
            zapChannels = zapChannels,
            onZapTo = actions.onZapTo,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PortraitDetailSection(
    uiState: PlayerUiState,
    aspectLabel: String,
    actions: PortraitSectionActions
) {
    val channel = uiState.channel

    Column(
        verticalArrangement = Arrangement.spacedBy(Dimens.Space2),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.Space4, vertical = Dimens.Space3)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Space3)
        ) {
            Card(
                shape = ShapeSmall,
                colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                modifier = Modifier.size(Dimens.PlayerPortraitDetailLogo)
            ) {
                AsyncImage(
                    model = channel?.logoUrl,
                    contentDescription = channel?.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Dimens.Space1)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel?.name.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = OnVideo,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (channel != null && channel.category.isNotBlank()) {
                    val catColor = categoryColor(channel.category)
                    Surface(
                        shape = ShapeSmall,
                        color = catColor.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, catColor.copy(alpha = 0.4f)),
                        modifier = Modifier.padding(top = Dimens.Space1)
                    ) {
                        Text(
                            text = channel.category,
                            style = LabelBadge,
                            color = catColor,
                            modifier = Modifier.padding(horizontal = Dimens.Space2, vertical = Dimens.BadgePaddingV)
                        )
                    }
                }
            }
            IconButton(onClick = actions.onToggleFavorite) {
                Icon(
                    imageVector = if (channel?.isFavorite == true) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (channel?.isFavorite == true) MaterialTheme.colorScheme.error else OnVideo
                )
            }
        }

        uiState.nowPlaying?.let { now ->
            Text(
                text = "Now: ${now.title}  ${formatEpgTimeRange(now)}",
                style = BodyOverlay,
                color = OnVideo,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            EpgProgressBar(now)
        }
        uiState.nextProgram?.let { next ->
            Text(
                text = "Next: ${next.title}  ${formatEpgTimeRange(next)}",
                style = LabelToast,
                color = OnVideo.copy(alpha = EmphasisMedium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space4)) {
            PortraitActionButton(
                icon = Icons.Filled.ClosedCaption,
                label = "Audio/Subs",
                onClick = actions.onShowTracks
            )
            val sleepLabel = uiState.sleepTimerMinutes?.let { "Sleep ${it}m" } ?: "Sleep off"
            PortraitActionButton(
                icon = Icons.Filled.Bedtime,
                label = sleepLabel,
                tint = if (uiState.sleepTimerMinutes != null) Amber else OnVideo,
                onClick = { actions.onCycleSleepTimer(nextSleepTimerStep(uiState.sleepTimerMinutes)) }
            )
            PortraitActionButton(
                icon = Icons.Filled.AspectRatio,
                label = aspectLabel,
                onClick = actions.onCycleAspect
            )
            PortraitActionButton(
                icon = Icons.Filled.PictureInPictureAlt,
                label = "PiP",
                onClick = actions.onEnterPip
            )
        }
    }
}

@Composable
private fun PortraitActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = OnVideo
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.Space1),
        modifier = Modifier
            .clip(ShapeSmall)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.Space2, vertical = Dimens.Space1)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(Dimens.IconMedium)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = OnVideo.copy(alpha = EmphasisMedium)
        )
    }
}
