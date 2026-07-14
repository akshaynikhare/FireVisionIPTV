package com.cadnative.firevisioniptv.presentation.ui.screens.player

import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.cadnative.firevisioniptv.presentation.ui.components.tvFocusVisuals
import com.cadnative.firevisioniptv.presentation.ui.theme.Amber
import com.cadnative.firevisioniptv.presentation.ui.theme.Dimens
import com.cadnative.firevisioniptv.presentation.ui.theme.OnVideo
import com.cadnative.firevisioniptv.presentation.ui.theme.ScrimHeavy
import com.cadnative.firevisioniptv.presentation.ui.theme.ShapeLarge
import com.cadnative.firevisioniptv.presentation.ui.theme.ShapeSmall

/**
 * Audio-track and subtitle selection panel — the TiviMate-parity controls the player
 * previously lacked. Reads the live [ExoPlayer] track groups and applies overrides via
 * [ExoPlayer.trackSelectionParameters]. Subtitles render automatically in PlayerView's
 * SubtitleView once a text track is selected. D-pad friendly: rows are focusable, back
 * dismisses.
 */
@OptIn(UnstableApi::class)
@Composable
internal fun PlayerTracksPanel(
    exoPlayer: ExoPlayer,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Bump to re-read tracks after a selection change.
    var tick by remember { mutableIntStateOf(0) }
    val tracks = remember(tick) { exoPlayer.currentTracks }
    val audioGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO && it.isSupported }
    val textGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT && it.isSupported }
    val subtitlesOff = textGroups.none { group ->
        (0 until group.length).any { group.isTrackSelected(it) }
    }

    fun selectTrack(group: Tracks.Group, index: Int) {
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon()
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, index))
            .setTrackTypeDisabled(group.type, false)
            .build()
        tick++
    }

    fun disableSubtitles() {
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()
        tick++
    }

    BackHandler { onDismiss() }

    // Move focus into the panel when it opens so the D-pad drives it.
    val firstRowFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstRowFocus.requestFocus() } }
    val hasAudio = audioGroups.isNotEmpty()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                // Cap for TV/tablet; fraction keeps it inside narrow portrait phones
                .fillMaxWidth(0.92f)
                .widthIn(max = Dimens.TracksPanelMaxWidth)
                .clip(ShapeLarge)
                .background(ScrimHeavy)
                .padding(Dimens.Space4)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space2)
        ) {
            SectionHeader("Audio")
            if (!hasAudio) {
                EmptyRow("No alternate audio tracks")
            } else {
                var firstAudio = true
                audioGroups.forEach { group ->
                    (0 until group.length).forEach { i ->
                        TrackRow(
                            label = trackLabel(group, i, "Audio"),
                            selected = group.isTrackSelected(i),
                            focusRequester = if (firstAudio) firstRowFocus else null,
                            onClick = { selectTrack(group, i) }
                        )
                        firstAudio = false
                    }
                }
            }

            SectionHeader("Subtitles")
            TrackRow(
                label = "Off",
                selected = subtitlesOff,
                focusRequester = if (!hasAudio) firstRowFocus else null,
                onClick = { disableSubtitles() }
            )
            textGroups.forEach { group ->
                (0 until group.length).forEach { i ->
                    TrackRow(
                        label = trackLabel(group, i, "Subtitle"),
                        selected = group.isTrackSelected(i),
                        onClick = { selectTrack(group, i) }
                    )
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
private fun trackLabel(group: Tracks.Group, index: Int, fallbackPrefix: String): String {
    val format = group.getTrackFormat(index)
    return format.label
        ?: format.language?.uppercase()
        ?: "$fallbackPrefix ${index + 1}"
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = OnVideo.copy(alpha = 0.7f),
        modifier = Modifier.padding(top = Dimens.Space1)
    )
}

@Composable
private fun EmptyRow(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = OnVideo.copy(alpha = 0.5f),
        modifier = Modifier.padding(vertical = Dimens.Space2)
    )
}

@Composable
private fun TrackRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { isFocused = it.isFocused }
            // No elevation shadow: rows have transparent fills over the panel
            // scrim, so the shadow body reads as a dark box (see QuickActions).
            .tvFocusVisuals(
                focused = isFocused,
                shape = ShapeSmall,
                restingElevation = 0.dp,
                focusedElevation = 0.dp
            )
            .clip(ShapeSmall)
            .background(if (isFocused) Amber.copy(alpha = 0.18f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.Space3, vertical = Dimens.Space2)
    ) {
        Text(
            text = if (selected) "✓  $label" else label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) Amber else OnVideo
        )
    }
}
