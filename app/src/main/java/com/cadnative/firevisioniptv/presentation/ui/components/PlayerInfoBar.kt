package com.cadnative.firevisioniptv.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cadnative.firevisioniptv.domain.model.EpgProgram
import com.cadnative.firevisioniptv.presentation.model.ChannelUiModel
import com.cadnative.firevisioniptv.presentation.ui.theme.BodyOverlay
import com.cadnative.firevisioniptv.presentation.ui.theme.EmphasisMedium
import com.cadnative.firevisioniptv.presentation.ui.theme.LabelBadge
import com.cadnative.firevisioniptv.presentation.ui.theme.LabelToast
import com.cadnative.firevisioniptv.presentation.ui.theme.OnVideo
import com.cadnative.firevisioniptv.presentation.ui.theme.ScrimLight
import com.cadnative.firevisioniptv.presentation.ui.theme.Elevation
import com.cadnative.firevisioniptv.presentation.ui.theme.ShapeSmall
import com.cadnative.firevisioniptv.presentation.ui.theme.SurfaceElevated
import com.cadnative.firevisioniptv.presentation.ui.theme.categoryColor
import com.cadnative.firevisioniptv.presentation.ui.theme.softShadow

// Flat vertical scrim — no blur/shadow, cheap on low-end boxes
private val InfoBarScrim = Brush.verticalGradient(
    0f to Color.Transparent,
    0.4f to ScrimLight,
    1f to Color.Black.copy(alpha = 0.85f)
)

private val LogoSize = 40.dp

/**
 * Bottom-anchored now-playing bar content for the player. Renders the bar
 * only — the caller owns reveal/auto-hide logic and positioning.
 *
 * When [compact] is true it renders a slim, single-line strip suitable for the
 * always-pinned live-EPG row; otherwise the full channel + now/next card.
 */
@Composable
fun PlayerInfoBar(
    channel: ChannelUiModel?,
    nowPlaying: EpgProgram?,
    nextProgram: EpgProgram?,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    if (channel == null) return
    if (compact) {
        CompactInfoBar(channel, nowPlaying, nextProgram, modifier)
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(InfoBarScrim)
            .padding(horizontal = 24.dp)
            .padding(top = 32.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ChannelInfoRow(channel)

        if (nowPlaying != null) {
            Text(
                text = "Now: ${nowPlaying.title}  ${formatEpgTimeRange(nowPlaying)}",
                style = BodyOverlay,
                color = OnVideo,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            EpgProgressBar(nowPlaying)
        }
        if (nextProgram != null) {
            Text(
                text = "Next: ${nextProgram.title}  ${formatEpgTimeRange(nextProgram)}",
                style = LabelToast,
                color = OnVideo.copy(alpha = EmphasisMedium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Slim always-on strip: channel name · Now title/time · thin progress · Next.
 * Kept to a single logical row so it stays out of the way during viewing.
 */
@Composable
private fun CompactInfoBar(
    channel: ChannelUiModel,
    nowPlaying: EpgProgram?,
    nextProgram: EpgProgram?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(InfoBarScrim)
            .padding(horizontal = 24.dp)
            .padding(top = 20.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = channel.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = OnVideo,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (nowPlaying != null) {
                Text(
                    text = "• ${nowPlaying.title}",
                    style = BodyOverlay,
                    color = OnVideo,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatEpgTimeRange(nowPlaying),
                    style = LabelToast,
                    color = OnVideo.copy(alpha = EmphasisMedium),
                    maxLines = 1
                )
            }
        }
        if (nowPlaying != null) {
            EpgProgressBar(nowPlaying)
        }
        if (nextProgram != null) {
            Text(
                text = "Next: ${nextProgram.title}",
                style = LabelToast,
                color = OnVideo.copy(alpha = EmphasisMedium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ChannelInfoRow(channel: ChannelUiModel) {
    val catColor = categoryColor(channel.category)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            shape = ShapeSmall,
            colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
            modifier = Modifier
                .softShadow(Elevation.Level2, ShapeSmall)
                .size(LogoSize)
        ) {
            AsyncImage(
                model = channel.logoUrl,
                contentDescription = channel.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Text(
            text = channel.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = OnVideo,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )

        Surface(
            shape = ShapeSmall,
            color = catColor.copy(alpha = 0.2f),
            border = BorderStroke(1.dp, catColor.copy(alpha = 0.4f))
        ) {
            Text(
                text = channel.category,
                style = LabelBadge,
                color = catColor,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

