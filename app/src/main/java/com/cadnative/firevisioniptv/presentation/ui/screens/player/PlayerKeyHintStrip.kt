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

/** Short hint label for a remappable key action (mirrors Settings wording). */
private fun keyActionHintLabel(action: String): String = when (action) {
    PlayerKeyAction.ZAP -> "Zap"
    PlayerKeyAction.LAST_CHANNEL -> "Last Ch"
    PlayerKeyAction.FAVORITE -> "Favorite"
    PlayerKeyAction.PLAY_PAUSE -> "Play/Pause"
    else -> "Controls" // MENU (and any unknown) summons the quick-actions bar
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
        add("OK  Channels")
        if (keyUpDownAction == keyLeftRightAction) {
            add("D-pad  ${keyActionHintLabel(keyUpDownAction)}")
        } else {
            add("▲▼  ${keyActionHintLabel(keyUpDownAction)}")
            add("◀▶  ${keyActionHintLabel(keyLeftRightAction)}")
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
