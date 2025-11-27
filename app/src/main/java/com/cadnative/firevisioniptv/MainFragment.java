package com.cadnative.firevisioniptv;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.HorizontalGridView;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.ListRowView;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.leanback.widget.VerticalGridView;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class MainFragment extends BrowseSupportFragment {
    private static final String TAG = "MainFragment";
    private static final String ARG_FAVORITES_ONLY = "favorites_only";
    private static final String ARG_FILTER_CATEGORY = "filter_category";
    private static final String ARG_FILTER_TYPE = "filter_type";

    private static final int BACKGROUND_UPDATE_DELAY = 300;
    private static final int GRID_ITEM_WIDTH = 200;
    private static final int GRID_ITEM_HEIGHT = 200;
    private static final int MAX_NUM_COLS = 6; // Increased for modern widescreen layout

    private final Handler mHandler = new Handler(Looper.myLooper());
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;
    private Timer mBackgroundTimer;
    private String mBackgroundUri;
    private BackgroundManager mBackgroundManager;

    private AssetManager assetManager;
    private ProgressBar loadingSpinner;
    private View errorContainer;
    private android.widget.ImageView errorIcon;
    private TextView errorTitle;
    private TextView errorMessage;
    private boolean showFavoritesOnly = false;
    private String filterCategory = null;
    private String filterType = null;

    /**
     * Create a new instance showing only favorites
     */
    public static MainFragment newInstanceForFavorites() {
        MainFragment fragment = new MainFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_FAVORITES_ONLY, true);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Create a new instance filtered by category or language
     */
    public static MainFragment newInstanceForCategory(String categoryName, String categoryType) {
        MainFragment fragment = new MainFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FILTER_CATEGORY, categoryName);
        args.putString(ARG_FILTER_TYPE, categoryType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");

        assetManager = getContext().getAssets();

        // Check if we should show only favorites
        if (getArguments() != null) {
            showFavoritesOnly = getArguments().getBoolean(ARG_FAVORITES_ONLY, false);
            filterCategory = getArguments().getString(ARG_FILTER_CATEGORY);
            filterType = getArguments().getString(ARG_FILTER_TYPE);
        }

        super.onActivityCreated(savedInstanceState);

        // Initialize loading spinner and error container
        loadingSpinner = getActivity().findViewById(R.id.loading_spinner);
        errorContainer = getActivity().findViewById(R.id.error_container);
        errorIcon = getActivity().findViewById(R.id.error_icon);
        errorTitle = getActivity().findViewById(R.id.error_title);
        errorMessage = getActivity().findViewById(R.id.error_message);

        prepareBackgroundManager();

        setupUIElements();

        loadRows();

        setupEventListeners();


        // Ensure rows are loaded and then select the first item
        //  mHandler.postDelayed(() -> selectFirstItem(), 500); // delay to ensure rows are loaded

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != mBackgroundTimer) {
            Log.d(TAG, "onDestroy: " + mBackgroundTimer.toString());
            mBackgroundTimer.cancel();
        }
    }

    private void showLoadingSpinner() {
        if (loadingSpinner != null) {
            loadingSpinner.setVisibility(View.VISIBLE);
        }
    }

    private void hideLoadingSpinner() {
        if (loadingSpinner != null) {
            loadingSpinner.setVisibility(View.GONE);
        }
    }

    private void showErrorMessage(String message) {
        showErrorMessage(message, false);
    }

    private void showErrorMessage(String message, boolean isWarning) {
        if (errorContainer != null) {
            errorContainer.setVisibility(View.VISIBLE);
        }
        
        if (errorIcon != null) {
            if (isWarning) {
                // Warning style - orange triangle
                errorIcon.setImageResource(R.drawable.ic_warning);
                errorIcon.setColorFilter(null); // Remove tint to show original orange color
            } else {
                // Error style - red settings icon
                errorIcon.setImageResource(R.drawable.ic_settings);
                errorIcon.setColorFilter(0xFFE74C3C); // Red tint
            }
        }
        
        if (errorTitle != null) {
            if (isWarning) {
                errorTitle.setText("No Channels Available");
                errorTitle.setTextColor(0xFFFFA500); // Orange
            } else {
                errorTitle.setText("Server Connection Failed");
                errorTitle.setTextColor(0xFFE74C3C); // Red
            }
        }
        
        if (errorMessage != null && message != null) {
            errorMessage.setText(message);
        }
        // Hide the browse fragment content
        if (getView() != null) {
            getView().setVisibility(View.GONE);
        }
    }

    private void hideErrorMessage() {
        if (errorContainer != null) {
            errorContainer.setVisibility(View.GONE);
        }
        // Show the browse fragment content
        if (getView() != null) {
            getView().setVisibility(View.VISIBLE);
        }
    }

    private void loadRows() {
        showLoadingSpinner();
        hideErrorMessage();

        // Load channels from server
        MovieList.loadMoviesFromServer(getContext(), assetManager, new MovieList.MovieListCallback() {
            @Override
            public void onSuccess(List<Movie> list) {
                getActivity().runOnUiThread(() -> {
                    hideLoadingSpinner();
                    if (list != null && !list.isEmpty()) {
                        hideErrorMessage();
                        displayChannels(list);
                    } else {
                        // Server connected successfully but no channels available
                        showErrorMessage("No channels in your list.\n\nPlease add channels to your account via the dashboard:\n" + 
                                       SettingsActivity.getServerUrl(getContext()), true);
                    }
                });
            }

            @Override
            public void onError(String error) {
                getActivity().runOnUiThread(() -> {
                    hideLoadingSpinner();
                    showErrorMessage("Failed to connect to server.\n\nPlease check your internet connection and TV code in Settings.");
                    Log.e(TAG, "Channel load error: " + error);
                });
            }
        });
    }

    private void displayChannels(List<Movie> list) {
        ArrayObjectAdapter rowsAdapter = new ArrayObjectAdapter(new NetflixListRowPresenter());
        CardPresenter cardPresenter = new CardPresenter();

        // Filter for favorites if needed
        if (showFavoritesOnly) {
            FavoritesManager favManager = FavoritesManager.getInstance(getContext());
            List<Movie> favoritesList = new ArrayList<>();
            for (Movie movie : list) {
                if (favManager.isFavorite(String.valueOf(movie.getId()))) {
                    favoritesList.add(movie);
                }
            }
            list = favoritesList;

            // Update title to show we're in favorites view
            setTitle("My Favorites");
        }
        
        // Filter by category or language if needed
        if (filterCategory != null && filterType != null) {
            List<Movie> filteredList = new ArrayList<>();
            for (Movie movie : list) {
                if ("category".equals(filterType)) {
                    // Filter by category (channelGroup)
                    if (filterCategory.equals(movie.getGroup())) {
                        filteredList.add(movie);
                    }
                } else if ("language".equals(filterType)) {
                    // Filter by language - check if the language field contains the selected language
                    // Since languages can be comma-separated (e.g., "Urdu, Hindi, English"),
                    // we check if the selected language is present in the string
                    String movieLanguages = movie.getLanguage();
                    if (movieLanguages != null && movieLanguages.contains(filterCategory)) {
                        filteredList.add(movie);
                    }
                }
            }
            list = filteredList;
            
            // Update title to show current filter
            setTitle(filterCategory);
        }

        Map<String, List<Movie>> groupedMovies = new TreeMap<>();  // Using TreeMap for alphabetical sorting

        for (Movie movie : list) {
            String group = movie.getGroup();
            if (group == null || group.isEmpty()) {
                group = "zzz_uncategorized";  // Prefix with 'zzz_' to ensure it's last alphabetically
            } else if (group.equalsIgnoreCase("Uncategorized") || group.equalsIgnoreCase("General") || group.equalsIgnoreCase("Other")) {
                group = "zzz_uncategorized";  // Move Uncategorized, General, and Other to the end
            }
            if (!groupedMovies.containsKey(group)) {
                groupedMovies.put(group, new ArrayList<>());
            }
            groupedMovies.get(group).add(movie);
        }

// Move "uncategorized" group to a separate variable and remove it from the TreeMap
        List<Movie> otherMovies = groupedMovies.remove("zzz_uncategorized");

        for (Map.Entry<String, List<Movie>> entry : groupedMovies.entrySet()) {
            String group = entry.getKey();
            List<Movie> moviesInGroup = entry.getValue();

            // Calculate the number of rows needed for this group
            int numRows = (moviesInGroup.size() + MAX_NUM_COLS - 1) / MAX_NUM_COLS;

            for (int i = 0; i < numRows; i++) {
                ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(cardPresenter);

                // Calculate start and end indices for this row
                int startIndex = i * MAX_NUM_COLS;
                int endIndex = Math.min((i + 1) * MAX_NUM_COLS, moviesInGroup.size());

                // Add movies to this row
                listRowAdapter.addAll(0, moviesInGroup.subList(startIndex, endIndex));

                // Create header for this row
                String headerText = group;
                if (numRows > 1) {
                    int firstMovieIndex = startIndex + 1;  // Adding 1 to convert from 0-based to 1-based indexing
                    int lastMovieIndex = endIndex;
                    headerText += " (" + firstMovieIndex + "-" + lastMovieIndex + ")";
                } else {
                    headerText += " (" + moviesInGroup.size() + ")";
                }

                HeaderItem header = new HeaderItem(0, headerText);
                rowsAdapter.add(new ListRow(header, listRowAdapter));
            }
        }

// Now handle the "uncategorized" group if it exists
        if (otherMovies != null && !otherMovies.isEmpty()) {
            int numRows = (otherMovies.size() + MAX_NUM_COLS - 1) / MAX_NUM_COLS;

            for (int i = 0; i < numRows; i++) {
                ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(cardPresenter);

                int startIndex = i * MAX_NUM_COLS;
                int endIndex = Math.min((i + 1) * MAX_NUM_COLS, otherMovies.size());

                listRowAdapter.addAll(0, otherMovies.subList(startIndex, endIndex));

                String headerText = "Uncategorized";
                if (numRows > 1) {
                    int firstMovieIndex = startIndex + 1;
                    int lastMovieIndex = endIndex;
                    headerText += " (" + firstMovieIndex + "-" + lastMovieIndex + ")";
                } else {
                    headerText += " (" + otherMovies.size() + ")";
                }

                HeaderItem header = new HeaderItem(0, headerText);
                rowsAdapter.add(new ListRow(header, listRowAdapter));
            }
        }

//        HeaderItem gridHeader = new HeaderItem(i, "PREFERENCES");
//
//        GridItemPresenter mGridPresenter = new GridItemPresenter();
//        ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(mGridPresenter);
//        gridRowAdapter.add(getResources().getString(R.string.grid_view));
//        gridRowAdapter.add(getString(R.string.error_fragment));
//        gridRowAdapter.add(getResources().getString(R.string.personal_settings));
//        rowsAdapter.add(new ListRow(gridHeader, gridRowAdapter));

        setAdapter(rowsAdapter);

        if (rowsAdapter.size() > 0) {
            ListRow firstRow = (ListRow) rowsAdapter.get(0);
            if (firstRow != null && firstRow.getAdapter() != null && firstRow.getAdapter().size() > 0) {
                Object firstItem = firstRow.getAdapter().get(0);
                if (firstItem != null) {
                    setSelectedPosition(0); // Select the first row

                }
            }
        }

    }

    private void prepareBackgroundManager() {

        mBackgroundManager = BackgroundManager.getInstance(getActivity());
        if (!mBackgroundManager.isAttached()) {
            mBackgroundManager.attach(getActivity().getWindow());
        }

        mDefaultBackground = ContextCompat.getDrawable(getContext(), R.drawable.default_background);
        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    private void setupUIElements() {

        String appVersion = getAppVersion(requireContext());
        setTitle(getString(R.string.browse_title) + " (v" + appVersion + ")");

        // Disable headers - we use custom sidebar navigation
        setHeadersState(HEADERS_DISABLED);
        setHeadersTransitionOnBackEnabled(false);

        // Set pure black background for Netflix aesthetic
        setBrandColor(ContextCompat.getColor(getContext(), R.color.fastlane_background));

        // Hide search icon - we use sidebar navigation instead
        getView().findViewById(androidx.leanback.R.id.title_orb).setVisibility(View.GONE);

        // Tweak VerticalGridView for minimal spacing
        setupVerticalGridView();
    }

    private void setupVerticalGridView() {
        // Access the RowsSupportFragment to tweak the VerticalGridView
        // We need to post this to ensure the fragment's view is created
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            androidx.leanback.app.RowsSupportFragment rowsFragment = getRowsSupportFragment();
            if (rowsFragment != null) {
                VerticalGridView gridView = rowsFragment.getVerticalGridView();
                if (gridView != null) {
                    // Set minimal spacing between rows
                    gridView.setItemSpacing(4); 
                    // Add top padding to account for title space
                    gridView.setPadding(0, 80, 0, 0);
                    // Ensure it doesn't clip children for focus effects
                    gridView.setClipChildren(false);
                    gridView.setClipToPadding(false);
                    
                    // Add scroll listener to hide/show title
                    setupTitleAutoHide(gridView);
                }
            }
        }, 500);
    }
    
    private void setupTitleAutoHide(VerticalGridView gridView) {
        final View titleView = getView().findViewById(androidx.leanback.R.id.browse_title_group);
        if (titleView == null) return;
        
        gridView.addOnScrollListener(new androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            private int scrollState = androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE;
            private int totalScrollY = 0;
            
            @Override
            public void onScrollStateChanged(@NonNull androidx.recyclerview.widget.RecyclerView recyclerView, int newState) {
                scrollState = newState;
            }
            
            @Override
            public void onScrolled(@NonNull androidx.recyclerview.widget.RecyclerView recyclerView, int dx, int dy) {
                totalScrollY += dy;
                
                // Hide title when scrolling down, show when at top
                if (totalScrollY > 50 && titleView.getVisibility() == View.VISIBLE) {
                    titleView.animate().alpha(0f).setDuration(200).withEndAction(() -> 
                        titleView.setVisibility(View.GONE)
                    );
                } else if (totalScrollY <= 50 && titleView.getVisibility() == View.GONE) {
                    titleView.setVisibility(View.VISIBLE);
                    titleView.animate().alpha(1f).setDuration(200);
                }
            }
        });
    }


    private String getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            // Handle exception
            return "Unknown";
        }
    }

    private void setupEventListeners() {
        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnItemViewSelectedListener(new ItemViewSelectedListener());
    }

    private void updateBackground(String uri) {
        int width = mMetrics.widthPixels;
        int height = mMetrics.heightPixels;
        Glide.with(getActivity())
                .load(uri)
                .centerCrop()
                .error(mDefaultBackground)
                .into(new SimpleTarget<Drawable>(width, height) {
                    @Override
                    public void onResourceReady(@NonNull Drawable drawable,
                                                @Nullable Transition<? super Drawable> transition) {
                        mBackgroundManager.setDrawable(drawable);
                    }
                });
        mBackgroundTimer.cancel();
    }

    private void startBackgroundTimer() {
        if (null != mBackgroundTimer) {
            mBackgroundTimer.cancel();
        }
        mBackgroundTimer = new Timer();
        mBackgroundTimer.schedule(new UpdateBackgroundTask(), BACKGROUND_UPDATE_DELAY);
    }



    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof Movie) {
                Movie movie = (Movie) item;
                Log.d(TAG, "Item: " + item.toString());
                Intent intent = new Intent(getActivity(), PlaybackActivity.class);
                intent.putExtra(DetailsActivity.MOVIE, movie);
                getActivity().startActivity(intent);
            } else if (item instanceof String) {
                if (((String) item).contains(getString(R.string.error_fragment))) {
                    Intent intent = new Intent(getActivity(), BrowseErrorActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(getActivity(), ((String) item), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(
                Presenter.ViewHolder itemViewHolder,
                Object item,
                RowPresenter.ViewHolder rowViewHolder,
                Row row) {
            if (item instanceof Movie) {
                mBackgroundUri = ((Movie) item).getBackgroundImageUrl();
                startBackgroundTimer();
            }
        }
    }

    private class UpdateBackgroundTask extends TimerTask {

        @Override
        public void run() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateBackground(mBackgroundUri);
                }
            });
        }
    }

    private class GridItemPresenter extends Presenter {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent) {
            TextView view = new TextView(parent.getContext());
            view.setLayoutParams(new ViewGroup.LayoutParams(GRID_ITEM_WIDTH, GRID_ITEM_HEIGHT));
            view.setFocusable(true);
            view.setFocusableInTouchMode(true);
            view.setBackgroundColor(
                    ContextCompat.getColor(getContext(), R.color.default_background));
            view.setTextColor(Color.WHITE);
            view.setGravity(Gravity.CENTER);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Object item) {
            ((TextView) viewHolder.view).setText((String) item);
        }

        @Override
        public void onUnbindViewHolder(ViewHolder viewHolder) {
        }
    }





}