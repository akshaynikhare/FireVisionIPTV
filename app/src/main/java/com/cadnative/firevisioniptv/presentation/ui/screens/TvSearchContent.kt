package com.cadnative.firevisioniptv.presentation.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.cadnative.firevisioniptv.presentation.model.SearchUiState
import com.cadnative.firevisioniptv.presentation.ui.components.ScreenHeaderTitle
import com.cadnative.firevisioniptv.presentation.ui.components.TvSearchKeyboard
import com.cadnative.firevisioniptv.presentation.ui.components.VoiceSearchButton
import com.cadnative.firevisioniptv.presentation.ui.theme.Dimens

/**
 * TV search layout, modeled on Google TV / YouTube: a read-only query bar and
 * voice button on top, then an on-screen QWERTY keyboard on the left with a
 * live-updating results grid on the right. All input flows through the keyboard
 * callbacks, so there is no system IME on TV.
 */
@Composable
internal fun TvSearchContent(
    query: String,
    uiState: SearchUiState,
    voiceStatus: String?,
    onKey: (Char) -> Unit,
    onBackspace: () -> Unit,
    onSpace: () -> Unit,
    onClear: () -> Unit,
    onVoiceResult: (String) -> Unit,
    onVoiceStatusChange: (String?) -> Unit,
    onChannelClick: (String) -> Unit,
    onFavoriteClick: (String) -> Unit,
    onMultiviewClick: (String) -> Unit,
    onRetry: () -> Unit,
    onRecentSearchClick: (String) -> Unit,
    onClearHistory: () -> Unit,
    firstKeyFocus: FocusRequester,
    modifier: Modifier = Modifier
) {
    // Netflix-style two-column layout: the "Search" title sits above the on-screen
    // keyboard on the left, while the query bar + voice button ride above the
    // results on the right — so the search box spans only the results column, not
    // the keyboard. A shared header band keeps the keyboard and results tops aligned.
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.KeyboardColumnGap)
    ) {
        // LEFT — title + keyboard
        Column {
            Box(
                modifier = Modifier.height(Dimens.HeaderBandHeightTv),
                contentAlignment = Alignment.CenterStart
            ) {
                ScreenHeaderTitle(text = "Search")
            }
            Spacer(modifier = Modifier.height(Dimens.Space5))
            TvSearchKeyboard(
                onKey = onKey,
                onBackspace = onBackspace,
                onSpace = onSpace,
                onClear = onClear,
                firstKeyFocus = firstKeyFocus
            )
        }

        // RIGHT — query bar + voice above the results
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.HeaderBandHeightTv),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.Space3)
            ) {
                TvSearchQueryDisplay(query = query, modifier = Modifier.weight(1f))
                VoiceSearchButton(
                    onResult = onVoiceResult,
                    onStatusChange = onVoiceStatusChange,
                    // Match the compact query bar height (caller size overrides the
                    // button's default 56dp).
                    modifier = Modifier.size(Dimens.SearchBarHeightTv)
                )
            }

            voiceStatus?.let { status ->
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = Dimens.Space2)
                )
            }

            Spacer(modifier = Modifier.height(Dimens.Space5))

            SearchResultsArea(
                uiState = uiState,
                searchQuery = query,
                isMobile = false,
                onChannelClick = onChannelClick,
                onFavoriteClick = onFavoriteClick,
                onMultiviewClick = onMultiviewClick,
                onRetry = onRetry,
                onRecentSearchClick = onRecentSearchClick,
                onClearHistory = onClearHistory,
                keyboardController = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}
