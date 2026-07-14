package com.cadnative.firevisioniptv.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import com.cadnative.firevisioniptv.presentation.ui.theme.Dimens

/**
 * Single decision point for the app-wide page-title treatment: a neutral
 * ([MaterialTheme.colorScheme.onBackground]) title with a leading amber accent
 * bar, echoing [SectionHeader]. Amber itself stays reserved for focus rings and
 * active nav states; the bar carries the brand (or a category identity when a
 * screen passes its own accent).
 */
object ScreenHeaderDefaults {
    val titleColor: Color
        @Composable get() = MaterialTheme.colorScheme.onBackground
    val accentColor: Color
        @Composable get() = MaterialTheme.colorScheme.primary
    const val showAccentBar = true
}

/**
 * The harmonized page title: accent bar + [MaterialTheme.typography.headlineMedium]
 * text. Non-focusable. Screens with bespoke header layouts (Search TV's
 * two-column band) reuse this so title rendering stays single-sourced.
 */
@Composable
fun ScreenHeaderTitle(
    text: String,
    modifier: Modifier = Modifier,
    accentColor: Color? = null
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (ScreenHeaderDefaults.showAccentBar) {
            Box(
                modifier = Modifier
                    .width(Dimens.HeaderAccentBarWidth)
                    .height(Dimens.HeaderAccentBarHeight)
                    .background(
                        accentColor ?: ScreenHeaderDefaults.accentColor,
                        MaterialTheme.shapes.extraSmall
                    )
            )
            Spacer(modifier = Modifier.width(Dimens.HeaderAccentBarGap))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.headlineMedium,
            color = ScreenHeaderDefaults.titleColor,
            maxLines = 1
        )
    }
}

/**
 * Fixed-height title band shared by every sidebar screen: top padding
 * [Dimens.ScreenPaddingVertical], band height [Dimens.HeaderBandHeightTv]
 * (mobile variants on compact), title left-aligned at
 * [Dimens.ScreenPaddingHorizontalTv]. Optional focusable back button and a
 * trailing slot for screen-specific chrome (notices, actions).
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    accentColor: Color? = null,
    applyHorizontalPadding: Boolean = true,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    val isCompact = LocalConfiguration.current.screenWidthDp < 600
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = if (isCompact) Dimens.ScreenPaddingVerticalMobile
                      else Dimens.ScreenPaddingVertical
            )
            .height(if (isCompact) Dimens.HeaderBandHeightMobile else Dimens.HeaderBandHeightTv)
            .then(
                if (applyHorizontalPadding) Modifier.padding(
                    horizontal = if (isCompact) Dimens.ScreenPaddingHorizontalMobile
                                 else Dimens.ScreenPaddingHorizontalTv
                ) else Modifier
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(Dimens.Space3))
        }
        ScreenHeaderTitle(text = title, accentColor = accentColor)
        if (trailing != null) {
            Spacer(modifier = Modifier.weight(1f))
            trailing()
        }
    }
}

/**
 * Screen template: [ScreenHeader] band, an optional full-width [belowHeader]
 * strip (filter chips, detail panels — these pad themselves), then the content
 * in a weighted Box separated by [Dimens.HeaderContentGap]. Content handles its
 * own horizontal padding (grids keep edge-to-edge scrolling with contentPadding).
 */
@Composable
fun ScreenScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    accentColor: Color? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    belowHeader: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val isCompact = LocalConfiguration.current.screenWidthDp < 600
    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = title,
            onBack = onBack,
            accentColor = accentColor,
            trailing = trailing
        )
        Spacer(
            modifier = Modifier.height(
                if (isCompact) Dimens.HeaderContentGapMobile else Dimens.HeaderContentGap
            )
        )
        if (belowHeader != null) {
            belowHeader()
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            content = content
        )
    }
}
