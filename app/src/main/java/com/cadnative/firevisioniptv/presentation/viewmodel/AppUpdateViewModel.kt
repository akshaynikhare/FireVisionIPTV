package com.cadnative.firevisioniptv.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadnative.firevisioniptv.data.AppPreferences
import com.cadnative.firevisioniptv.di.IoDispatcher
import com.cadnative.firevisioniptv.presentation.model.UpdateInfo
import com.cadnative.firevisioniptv.update.AppUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * UI state for the app-root update overlay.
 *
 * [dismissed] hides the overlay for this session; dismissing also records a
 * snooze so the same version stops reappearing on every cold launch.
 */
data class AppUpdateUiState(
    val updateInfo: UpdateInfo? = null,
    val isDownloading: Boolean = false,
    val downloadError: String? = null,
    val dismissed: Boolean = false
)

/**
 * Drives the full-screen "Update Available" overlay shown once at launch (after
 * the splash) when a newer app version is published. All version-check and
 * download/install logic lives in the shared [AppUpdater]; this VM only holds
 * the overlay's UI state.
 */
@HiltViewModel
class AppUpdateViewModel @Inject constructor(
    private val appUpdater: AppUpdater,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUpdateUiState())
    val uiState: StateFlow<AppUpdateUiState> = _uiState.asStateFlow()

    private var checked = false

    /**
     * Checks once per process. Safe to call repeatedly.
     *
     * The snooze is applied here rather than inside [AppUpdater.check], because
     * Settings' explicit "Check for Updates" shares that call and has to surface a
     * snoozed version — the user asking directly is not the user being nagged.
     */
    fun checkForUpdate() {
        if (checked) return
        checked = true
        viewModelScope.launch {
            val result = withContext(ioDispatcher) { appUpdater.check() } ?: return@launch
            val snoozed = AppPreferences.getSnoozedUpdateVersion(context)
            // A mandatory update is never snoozed.
            if (!result.isMandatory && result.versionName == snoozed) return@launch
            _uiState.update { it.copy(updateInfo = result) }
        }
    }

    fun dismiss() {
        _uiState.value.updateInfo?.let { update ->
            AppPreferences.setSnoozedUpdateVersion(context, update.versionName)
        }
        _uiState.update { it.copy(dismissed = true) }
    }

    fun downloadAndInstallUpdate() {
        val update = _uiState.value.updateInfo ?: return
        if (_uiState.value.isDownloading) return
        _uiState.update { it.copy(isDownloading = true, downloadError = null) }
        appUpdater.downloadAndInstall(update) { state ->
            _uiState.update {
                when (state) {
                    AppUpdater.DownloadState.Started -> it.copy(isDownloading = true)
                    AppUpdater.DownloadState.InstallLaunched -> it.copy(isDownloading = false)
                    is AppUpdater.DownloadState.Failed ->
                        it.copy(isDownloading = false, downloadError = state.message)
                }
            }
        }
    }

}
