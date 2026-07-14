package com.cadnative.firevisioniptv.presentation.ui.screens.settings

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cadnative.firevisioniptv.domain.service.ScanProgress
import com.cadnative.firevisioniptv.presentation.model.SettingsUiState
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_FAST
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import com.cadnative.firevisioniptv.presentation.ui.animation.animateItemEntrance
import com.cadnative.firevisioniptv.presentation.ui.theme.Dimens
import com.cadnative.firevisioniptv.presentation.ui.theme.FocusGlow
import com.cadnative.firevisioniptv.presentation.ui.theme.ShapeSmall

/** Callbacks for the settings screen, grouped to keep composable signatures small. */
internal class SettingsActions(
    val onResetPairing: () -> Unit,
    val onPairDevice: () -> Unit,
    val onNavigateToSelfHost: () -> Unit,
    val onCheckLiveliness: () -> Unit,
    val onClearCache: () -> Unit,
    val onBackExitProtectionChange: (Boolean) -> Unit,
    val onKeyUpDownChange: (String) -> Unit,
    val onKeyLeftRightChange: (String) -> Unit,
    val onLongOkChange: (String) -> Unit,
    val onSleepTimerDefaultChange: (Int) -> Unit,
    val onAlwaysShowProgramBarChange: (Boolean) -> Unit,
    val onInfoBarTimeoutChange: (Int) -> Unit,
    val onThemeChange: (String) -> Unit,
    val onCheckForUpdate: () -> Unit,
    val onUpdateNow: () -> Unit
)

internal enum class SettingsSection(val label: String) {
    Connection("Connection"),
    Channels("Channels"),
    Controls("Controls"),
    Appearance("Appearance"),
    About("About")
}

private val SectionListWidth = 280.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScaffold(
    uiState: SettingsUiState,
    scanProgress: ScanProgress,
    actions: SettingsActions,
    modifier: Modifier = Modifier
) {
    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        if (isPortrait) {
            StackedSettings(
                uiState = uiState,
                scanProgress = scanProgress,
                actions = actions,
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            TwoPaneSettings(
                uiState = uiState,
                scanProgress = scanProgress,
                actions = actions,
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }
}

// ── TV: fixed section list + content pane ───────────────────────────────────

@Composable
private fun TwoPaneSettings(
    uiState: SettingsUiState,
    scanProgress: ScanProgress,
    actions: SettingsActions,
    modifier: Modifier = Modifier
) {
    var selectedSection by remember { mutableStateOf(SettingsSection.Connection) }
    val contentFocusRequester = remember { FocusRequester() }
    val listFocusRequester = remember { FocusRequester() }

    Row(modifier = modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
        Column(
            modifier = Modifier
                .width(SectionListWidth)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SettingsSection.entries.forEach { section ->
                SectionListItem(
                    section = section,
                    isSelected = section == selectedSection,
                    onSelect = { selectedSection = section },
                    onEnterContent = {
                        try {
                            contentFocusRequester.requestFocus()
                        } catch (_: Exception) {
                        }
                    },
                    modifier = Modifier
                        .then(
                            // LEFT from content returns to the selected item
                            if (section == selectedSection) Modifier.focusRequester(listFocusRequester)
                            else Modifier
                        )
                        .focusProperties { right = contentFocusRequester }
                )
            }
        }

        Spacer(modifier = Modifier.width(24.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .focusRequester(contentFocusRequester)
                .focusProperties { left = listFocusRequester }
                .focusGroup(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionContent(
                section = selectedSection,
                uiState = uiState,
                scanProgress = scanProgress,
                actions = actions
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SectionListItem(
    section: SettingsSection,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEnterContent: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isFocused -> FocusGlow
            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            else -> Color.Transparent
        },
        animationSpec = tween(DURATION_FAST, easing = EaseOutQuart),
        label = "sectionItemBg"
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            isFocused -> MaterialTheme.colorScheme.onSurface
            isSelected -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(DURATION_FAST, easing = EaseOutQuart),
        label = "sectionItemContent"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(ShapeSmall)
            .background(backgroundColor)
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) onSelect()
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onEnterContent
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = section.label,
            color = contentColor,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected || isFocused) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

// ── Mobile/portrait: stacked full-width sections ────────────────────────────

@Composable
private fun StackedSettings(
    uiState: SettingsUiState,
    scanProgress: ScanProgress,
    actions: SettingsActions,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = Dimens.ScreenPaddingHorizontalMobile,
                vertical = Dimens.ScreenPaddingVerticalMobile
            ),
        verticalArrangement = Arrangement.spacedBy(Dimens.Space3)
    ) {
        SettingsSection.entries.forEachIndexed { index, section ->
            SectionContent(
                section = section,
                uiState = uiState,
                scanProgress = scanProgress,
                actions = actions,
                modifier = Modifier.animateItemEntrance(index = index)
            )
        }
        Spacer(modifier = Modifier.height(Dimens.Space3))
    }
}

// ── Shared section content ──────────────────────────────────────────────────

@Composable
private fun SectionContent(
    section: SettingsSection,
    uiState: SettingsUiState,
    scanProgress: ScanProgress,
    actions: SettingsActions,
    modifier: Modifier = Modifier
) {
    when (section) {
        SettingsSection.Connection -> ConnectionSection(
            uiState = uiState,
            onResetPairing = actions.onResetPairing,
            onPairDevice = actions.onPairDevice,
            onNavigateToSelfHost = actions.onNavigateToSelfHost,
            modifier = modifier
        )
        SettingsSection.Channels -> ChannelsSection(
            scanProgress = scanProgress,
            onCheckLiveliness = actions.onCheckLiveliness,
            isClearingCache = uiState.isClearingCache,
            cacheCleared = uiState.cacheCleared,
            onClearCache = actions.onClearCache,
            modifier = modifier
        )
        SettingsSection.Controls -> ControlsSection(
            backExitProtection = uiState.backExitProtection,
            keyUpDownAction = uiState.keyUpDownAction,
            keyLeftRightAction = uiState.keyLeftRightAction,
            longOkAction = uiState.longOkAction,
            sleepTimerDefaultMinutes = uiState.sleepTimerDefaultMinutes,
            alwaysShowProgramBar = uiState.alwaysShowProgramBar,
            infoBarTimeoutSeconds = uiState.infoBarTimeoutSeconds,
            onBackExitProtectionChange = actions.onBackExitProtectionChange,
            onKeyUpDownChange = actions.onKeyUpDownChange,
            onKeyLeftRightChange = actions.onKeyLeftRightChange,
            onLongOkChange = actions.onLongOkChange,
            onSleepTimerDefaultChange = actions.onSleepTimerDefaultChange,
            onAlwaysShowProgramBarChange = actions.onAlwaysShowProgramBarChange,
            onInfoBarTimeoutChange = actions.onInfoBarTimeoutChange,
            modifier = modifier
        )
        SettingsSection.Appearance -> AppearanceSection(
            currentTheme = uiState.theme,
            onThemeChange = actions.onThemeChange,
            modifier = modifier
        )
        SettingsSection.About -> AboutSection(
            appVersion = uiState.appVersion,
            isChecking = uiState.isCheckingForUpdate,
            updateInfo = uiState.updateInfo,
            updateChecked = uiState.updateChecked,
            isDownloading = uiState.isDownloadingUpdate,
            downloadError = uiState.downloadError,
            onCheckForUpdate = actions.onCheckForUpdate,
            onUpdateNow = actions.onUpdateNow,
            modifier = modifier
        )
    }
}
