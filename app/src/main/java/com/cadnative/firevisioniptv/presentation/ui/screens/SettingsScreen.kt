package com.cadnative.firevisioniptv.presentation.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cadnative.firevisioniptv.presentation.ui.screens.settings.SettingsActions
import com.cadnative.firevisioniptv.presentation.ui.screens.settings.SettingsScaffold
import com.cadnative.firevisioniptv.presentation.viewmodel.SettingsViewModel

/**
 * Settings entry point (NavGraph route). Thin root that wires the ViewModel
 * into the two-pane [SettingsScaffold] in `screens/settings/`.
 */
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onPairDevice: () -> Unit,
    onResetPairing: () -> Unit = {},
    onNavigateToSelfHost: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scanProgress by viewModel.scanProgress.collectAsStateWithLifecycle()

    // Re-read the source config each time Settings resumes so a change made on the
    // AddSource screen (self-hosted connect, M3U/Xtream load) shows on return.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshSource() }

    val actions = remember(viewModel, onPairDevice, onResetPairing, onNavigateToSelfHost) {
        SettingsActions(
            onResetPairing = {
                viewModel.resetPairing()
                onResetPairing()
            },
            onPairDevice = onPairDevice,
            onNavigateToSelfHost = onNavigateToSelfHost,
            onCheckLiveliness = viewModel::triggerLivelinessCheck,
            onClearCache = viewModel::clearCache,
            onResetGuide = viewModel::resetGuideData,
            onResetAppData = viewModel::resetAppData,
            onBackExitProtectionChange = viewModel::setBackExitProtection,
            onKeyUpDownChange = viewModel::setKeyUpDownAction,
            onKeyLeftRightChange = viewModel::setKeyLeftRightAction,
            onLongOkChange = viewModel::setLongOkAction,
            onSleepTimerDefaultChange = viewModel::setSleepTimerDefaultMinutes,
            onAlwaysShowProgramBarChange = viewModel::setAlwaysShowProgramBar,
            onInfoBarTimeoutChange = viewModel::setInfoBarTimeoutSeconds,
            onThemeChange = viewModel::setTheme,
            onCheckForUpdate = viewModel::checkForUpdate,
            onUpdateNow = viewModel::downloadAndInstallUpdate
        )
    }

    SettingsScaffold(
        uiState = uiState,
        scanProgress = scanProgress,
        actions = actions,
        modifier = modifier
    )
}
