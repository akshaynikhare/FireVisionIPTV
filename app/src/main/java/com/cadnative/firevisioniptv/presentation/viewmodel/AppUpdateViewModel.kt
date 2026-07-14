package com.cadnative.firevisioniptv.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadnative.firevisioniptv.presentation.model.UpdateInfo
import com.cadnative.firevisioniptv.update.AppUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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
 * [dismissed] is session-only (not persisted) — ignoring the update hides the
 * screen until the next app launch, when the check runs again.
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
    private val appUpdater: AppUpdater
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUpdateUiState())
    val uiState: StateFlow<AppUpdateUiState> = _uiState.asStateFlow()

    private var checked = false

    /** Checks once per process. Safe to call repeatedly. */
    fun checkForUpdate() {
        if (checked) return
        checked = true
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { appUpdater.check() }
            if (result != null) _uiState.update { it.copy(updateInfo = result) }
        }
    }

    fun dismiss() {
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

    override fun onCleared() {
        super.onCleared()
        appUpdater.cleanup()
    }
}
