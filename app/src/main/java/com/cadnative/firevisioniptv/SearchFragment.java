package com.cadnative.firevisioniptv;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

import androidx.leanback.app.SearchSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.ObjectAdapter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.SpeechRecognitionCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchFragment extends SearchSupportFragment implements SearchSupportFragment.SearchResultProvider {
    private static final String TAG = "SearchFragment";
    private static final int SEARCH_DELAY_MS = 300;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private ArrayObjectAdapter mRowsAdapter;
    private List<Movie> mAllMovies;
    private Handler mHandler;
    private Map<String, List<String>> mSearchSuggestions;
    private Runnable mDelayedLoad;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        // Movies should be loaded from server via loadMoviesFromServer
        mAllMovies = new ArrayList<>();
        mHandler = new Handler(Looper.getMainLooper());
        mSearchSuggestions = buildSearchSuggestions();

        setOnItemViewClickedListener(new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                      RowPresenter.ViewHolder rowViewHolder, Row row) {
                if (item instanceof Movie) {
                    Movie movie = (Movie) item;
                    Intent intent = new Intent(getActivity(), PlaybackActivity.class);
                    intent.putExtra(DetailsActivity.MOVIE, movie);
                    startActivity(intent);
                }
            }
        });

        // Set this fragment as its own search result provider
        setSearchResultProvider(this);

        // Enable voice search
        if (!hasVoiceRecognition()) {
            setSpeechRecognitionCallback(new SpeechRecognitionCallback() {
                @Override
                public void recognizeSpeech() {
                    // Start voice recognition if available
                    startActivityForResult(getRecognizerIntent(), 0);
                }
            });
        }

        // Customize search fragment appearance
        setBadgeDrawable(null); // Remove the badge/logo to avoid duplicate icons

        // Set search colors to match theme (Netflix red)
        int searchColor = getResources().getColor(R.color.search_orb_color, null);
        setSearchAffordanceColors(
            new androidx.leanback.widget.SearchOrbView.Colors(
                searchColor,  // Main color
                searchColor,  // Bright color
                getResources().getColor(R.color.search_color, null)  // Icon color
            )
        );
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set the background color to match main screen
        if (view != null) {
            view.setBackgroundColor(getResources().getColor(R.color.default_background, null));
        }
    }

    private Map<String, List<String>> buildSearchSuggestions() {
        Map<String, List<String>> suggestions = new HashMap<>();

        // Group-based suggestions
        Map<String, List<String>> groupSuggestions = new TreeMap<>();
        for (Movie movie : mAllMovies) {
            String group = movie.getGroup();
            if (!TextUtils.isEmpty(group)) {
                if (!groupSuggestions.containsKey(group)) {
                    groupSuggestions.put(group, new ArrayList<>());
                }
                groupSuggestions.get(group).add(movie.getTitle());
            }
        }
        suggestions.put("Groups", new ArrayList<>(groupSuggestions.keySet()));

        return suggestions;
    }

    @Override
    public ObjectAdapter getResultsAdapter() {
        return mRowsAdapter;
    }

    @Override
    public boolean onQueryTextChange(String newQuery) {
        if (TextUtils.isEmpty(newQuery)) {
            showSearchSuggestions();
            return true;
        }

        // Cancel any pending search
        if (mDelayedLoad != null) {
            mHandler.removeCallbacks(mDelayedLoad);
        }

        // Schedule a new search with delay
        mDelayedLoad = new Runnable() {
            @Override
            public void run() {
                loadQueryResults(newQuery);
            }
        };
        mHandler.postDelayed(mDelayedLoad, SEARCH_DELAY_MS);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if (mDelayedLoad != null) {
            mHandler.removeCallbacks(mDelayedLoad);
        }
        loadQueryResults(query);
        return true;
    }

    private void loadQueryResults(String query) {
        EXECUTOR.execute(() -> {
            final List<Movie> titleResults = new ArrayList<>();
            final Map<String, List<Movie>> groupResults = new HashMap<>();

            String lowerQuery = query.toLowerCase();

            for (Movie movie : mAllMovies) {
                // Search by title
                if (movie.getTitle().toLowerCase().contains(lowerQuery)) {
                    titleResults.add(movie);
                }

                // Search by group
                String group = movie.getGroup();
                if (!TextUtils.isEmpty(group) && group.toLowerCase().contains(lowerQuery)) {
                    if (!groupResults.containsKey(group)) {
                        groupResults.put(group, new ArrayList<>());
                    }
                    groupResults.get(group).add(movie);
                }
            }

            // Update UI on main thread
            mHandler.post(() -> {
                mRowsAdapter.clear();

                // Add title results
                if (!titleResults.isEmpty()) {
                    HeaderItem titleHeader = new HeaderItem("Channels");
                    ArrayObjectAdapter titleAdapter = new ArrayObjectAdapter(new CardPresenter());
                    titleAdapter.addAll(0, titleResults);
                    mRowsAdapter.add(new ListRow(titleHeader, titleAdapter));
                }

                // Add group results
                for (Map.Entry<String, List<Movie>> entry : groupResults.entrySet()) {
                    HeaderItem groupHeader = new HeaderItem(entry.getKey());
                    ArrayObjectAdapter groupAdapter = new ArrayObjectAdapter(new CardPresenter());
                    groupAdapter.addAll(0, entry.getValue());
                    mRowsAdapter.add(new ListRow(groupHeader, groupAdapter));
                }

                // Show message if no results found
                if (titleResults.isEmpty() && groupResults.isEmpty()) {
                    showNoResults();
                }
            });
        });
    }

    private void showSearchSuggestions() {
        mRowsAdapter.clear();

        // Add suggestion categories
        for (Map.Entry<String, List<String>> entry : mSearchSuggestions.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                HeaderItem header = new HeaderItem(entry.getKey());
                ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new StringPresenter());
                listRowAdapter.addAll(0, entry.getValue());
                mRowsAdapter.add(new ListRow(header, listRowAdapter));
            }
        }
    }

    private void showNoResults() {
        HeaderItem header = new HeaderItem("No Results");
        ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new StringPresenter());
        listRowAdapter.add("No channels found matching your search");
        mRowsAdapter.add(new ListRow(header, listRowAdapter));
    }

    private boolean hasVoiceRecognition() {
        return getActivity().getPackageManager().queryIntentActivities(
                getRecognizerIntent(), 0).isEmpty();
    }

    public boolean handleKeyDown(int keyCode, KeyEvent event) {
        // Handle voice search activation with microphone button
        if (keyCode == KeyEvent.KEYCODE_BUTTON_3) {
            if (!hasVoiceRecognition()) {
                startRecognition();
                return true;
            }
        }
        return false;
    }
}
