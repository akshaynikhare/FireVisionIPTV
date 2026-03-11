package com.cadnative.firevisioniptv;

import android.app.Application;
import android.content.res.AssetManager;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

/**
 * Activity-scoped ViewModel that owns the channel list.
 *
 * Responsibilities:
 *  - Fetch channels via MovieList / ApiClient exactly once per session
 *  - Cache the result so fragment transitions (Home → Categories → Home)
 *    never trigger a duplicate network request
 *  - Expose a single LiveData<ChannelUiState> that all channel-displaying
 *    fragments can observe; UI thread safety is handled by LiveData.postValue()
 *
 * Replaces the raw Thread + runOnUiThread pattern and the static MovieList.list field.
 */
public class ChannelViewModel extends AndroidViewModel {

    // ---------------------------------------------------------------------------
    // UI state model
    // ---------------------------------------------------------------------------

    public static final class ChannelUiState {

        public enum Status { LOADING, SUCCESS, ERROR }

        public final Status status;
        public final List<Movie> channels;
        public final String errorMessage;
        public final boolean isWarning;

        private ChannelUiState(Status status, List<Movie> channels,
                               String errorMessage, boolean isWarning) {
            this.status = status;
            this.channels = channels;
            this.errorMessage = errorMessage;
            this.isWarning = isWarning;
        }

        public static ChannelUiState loading() {
            return new ChannelUiState(Status.LOADING, null, null, false);
        }

        public static ChannelUiState success(List<Movie> channels) {
            return new ChannelUiState(Status.SUCCESS, channels, null, false);
        }

        public static ChannelUiState error(String message) {
            return new ChannelUiState(Status.ERROR, null, message, false);
        }

        public static ChannelUiState warning(String message) {
            return new ChannelUiState(Status.ERROR, null, message, true);
        }
    }

    // ---------------------------------------------------------------------------
    // State
    // ---------------------------------------------------------------------------

    private final MutableLiveData<ChannelUiState> mUiState = new MutableLiveData<>();
    public final LiveData<ChannelUiState> uiState = mUiState;

    private List<Movie> mCachedChannels = null;
    private boolean mIsLoading = false;

    // ---------------------------------------------------------------------------
    // Constructor
    // ---------------------------------------------------------------------------

    public ChannelViewModel(@NonNull Application application) {
        super(application);
    }

    // ---------------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------------

    /**
     * Request channel data.
     * If data is already cached the observer is notified immediately without a
     * network round-trip.  If a load is already in-flight this call is a no-op.
     */
    public void loadChannels(AssetManager assetManager) {
        if (mCachedChannels != null) {
            mUiState.setValue(ChannelUiState.success(mCachedChannels));
            return;
        }
        if (mIsLoading) return;

        mIsLoading = true;
        mUiState.setValue(ChannelUiState.loading());

        MovieList.loadMoviesFromServer(getApplication(), assetManager, new MovieList.MovieListCallback() {
            @Override
            public void onSuccess(List<Movie> movies) {
                mIsLoading = false;
                if (movies != null && !movies.isEmpty()) {
                    mCachedChannels = movies;
                    mUiState.postValue(ChannelUiState.success(movies));
                } else {
                    String dashboardUrl = SettingsActivity.getServerUrl(getApplication());
                    mUiState.postValue(ChannelUiState.warning(
                            "No channels in your list.\n\nPlease add channels to your account via the dashboard:\n"
                                    + dashboardUrl));
                }
            }

            @Override
            public void onError(String error) {
                mIsLoading = false;
                mUiState.postValue(ChannelUiState.error(
                        "Failed to connect to server.\n\nPlease check your internet connection and TV code in Settings."));
            }
        });
    }

    /**
     * Force a fresh network request, discarding the cached channel list.
     * Call this when the user changes the server URL in Settings.
     */
    public void refresh(AssetManager assetManager) {
        mCachedChannels = null;
        mIsLoading = false;
        loadChannels(assetManager);
    }
}
