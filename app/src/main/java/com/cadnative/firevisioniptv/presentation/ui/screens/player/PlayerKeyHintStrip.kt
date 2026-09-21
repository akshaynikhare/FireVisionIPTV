package com.cadnative.firevisioniptv.presentation.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.cadnative.firevisioniptv.domain.repository.PlayerKeyAction
import com.cadnative.firevisioniptv.presentation.ui.theme.Dimens
import com.cadnative.firevisioniptv.presentation.ui.theme.LabelToast
import com.cadnative.firevisioniptv.presentation.ui.theme.OnVideo
import com.cadnative.firevisioniptv.presentation.ui.theme.ScrimHeavy
import com.cadnative.firevisioniptv.presentation.ui.theme.ShapePill
import com.cadnative.firevisioniptv.R
import androidx.compose.ui.res.stringResource
import androidx.annotation.StringRes

/** Label for a remappable key action — the same resources Settings offers. */
@StringRes
private fun keyActionHintLabel(action: String): Int = when (action) {
    PlayerKeyAction.ZAP -> R.string.player_key_action_zap
    PlayerKeyAction.LAST_CHANNEL -> R.string.player_key_action_last_channel
    PlayerKeyAction.FAVORITE -> R.string.player_key_action_favorite
    PlayerKeyAction.PLAY_PAUSE -> R.string.player_key_action_play_pause
    // MENU, and anything unrecognised, summons the quick-actions bar
    else -> R.string.player_key_action_controls
}

/**
 * One-line key-hint pill shown with the full info bar (TV only) so the remote
 * never has to be guessed: what OK and the D-pad do right now. Remap-aware —
 * labels follow the Settings key mappings; when both axes share an action the
 * segments merge into a single "D-pad" hint.
 */
@Composable
internal fun PlayerKeyHintStrip(
    keyUpDownAction: String,
    keyLeftRightAction: String,
    modifier: Modifier = Modifier
) {
    val segments = buildList {
        add(stringResource(R.string.player_hint_ok_channels))
        if (keyUpDownAction == keyLeftRightAction) {
            add(stringResource(R.string.player_hint_dpad, stringResource(keyActionHintLabel(keyUpDownAction))))
        } else {
            add(stringResource(R.string.player_hint_up_down, stringResource(keyActionHintLabel(keyUpDownAction))))
            add(stringResource(R.string.player_hint_left_right, stringResource(keyActionHintLabel(keyLeftRightAction))))
        }
    }
    Text(
        text = segments.joinToString("   ·   "),
        style = LabelToast,
        color = OnVideo,
        modifier = modifier
            .clip(ShapePill)
            .background(ScrimHeavy)
            .padding(horizontal = Dimens.Space4, vertical = Dimens.Space2)
    )
}
