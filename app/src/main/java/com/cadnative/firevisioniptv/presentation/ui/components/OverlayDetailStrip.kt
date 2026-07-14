package com.cadnative.firevisioniptv.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.cadnative.firevisioniptv.domain.model.EpgProgram
import com.cadnative.firevisioniptv.presentation.model.ChannelUiModel
import com.cadnative.firevisioniptv.presentation.ui.theme.Dimens
import com.cadnative.firevisioniptv.presentation.ui.theme.EmphasisMedium
import com.cadnative.firevisioniptv.presentation.ui.theme.LabelToast
import com.cadnative.firevisioniptv.presentation.ui.theme.OnVideo
import com.cadnative.firevisioniptv.presentation.ui.theme.TextSecondary
import com.cadnative.firevisioniptv.presentation.ui.theme.categoryColor

/** Lookup key into [com.cadnative.firevisioniptv.presentation.model.PlayerUiState.overlayEpg]. */
internal fun overlayEpgKey(tvgId: String?): String? =
    tvgId?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }

/**
 * Focused-channel detail header for the channel overlay: what's airing on the
 * focused card, with progress, description, and what's next. Fixed height so
 * the panel never reflows (and focus never jumps) as D-pad focus moves.
 */
@Composable
internal fun OverlayDetailStrip(
    channel: ChannelUiModel?,
    epg: Pair<EpgProgram?, EpgProgram?>?,
    modifier: Modifier = Modifier
) {
    val now = epg?.first
    val next = epg?.second

    Column(
        verticalArrangement = Arrangement.spacedBy(Dimens.Space1),
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.OverlayDetailStripHeight)
            .padding(horizontal = Dimens.Space5)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = channel?.name.orEmpty(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (now != null) TextSecondary else OnVideo,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (channel != null && channel.category.isNotBlank()) {
                Spacer(modifier = Modifier.width(Dimens.Space2))
                Text(
                    text = channel.category,
                    style = LabelToast,
                    color = categoryColor(channel.category),
                    maxLines = 1
                )
            }
        }
        if (now != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = now.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = OnVideo,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(Dimens.Space2))
                Text(
                    text = formatEpgTimeRange(now),
                    style = LabelToast,
                    color = OnVideo.copy(alpha = EmphasisMedium),
                    maxLines = 1
                )
            }
            EpgProgressBar(now)
            val description = now.description
            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = OnVideo.copy(alpha = EmphasisMedium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (next != null) {
                Text(
                    text = "Next: ${next.title}  ${formatEpgTimeRange(next)}",
                    style = LabelToast,
                    color = OnVideo.copy(alpha = EmphasisMedium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            Text(
                text = "No program info",
                style = LabelToast,
                color = TextSecondary,
                maxLines = 1
            )
        }
    }
}
