package com.cadnative.firevisioniptv;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.HorizontalGridView;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.VerticalGridPresenter;
import androidx.leanback.widget.VerticalGridView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomSearchFragment extends Fragment {
    private static final String TAG = "CustomSearchFragment";
    private static final int SEARCH_DELAY_MS = 300;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final String PREFS_NAME = "SearchPrefs";
    private static final String KEY_SEARCH_HISTORY = "search_history";
    private static final String KEY_CHANNEL_CLICKS = "channel_clicks";
    private static final int MAX_SEARCH_HISTORY = 5;

    private EditText searchInput;
    private VerticalGridView searchResults;
    private ArrayObjectAdapter resultsAdapter;
    private List<Movie> allMovies;
    private Handler handler;
    private Runnable delayedSearch;
    private View searchHistorySection;
    private HorizontalGridView searchHistoryChips;
    private ArrayObjectAdapter chipsAdapter;
    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_custom_search, container, false);

        searchInput = view.findViewById(R.id.search_input);
        searchResults = view.findViewById(R.id.search_results);
        searchHistorySection = view.findViewById(R.id.search_history_section);
        searchHistoryChips = view.findViewById(R.id.search_history_chips);

        handler = new Handler(Looper.getMainLooper());
        // Movies should be loaded from server via loadMoviesFromServer
        allMovies = new ArrayList<>();
        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        setupSearchInput();
        setupSearchResults();
        setupSearchHistoryChips();

        // Show search history and popular channels initially
        handler.postDelayed(() -> {
            if (searchInput != null) {
                searchInput.requestFocus();
            }
            // Display search history and popular channels
            showSearchHistoryAndPopular();
        }, 100);

        return view;
    }

    private void setupSearchInput() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Cancel any pending search
                if (delayedSearch != null) {
                    handler.removeCallbacks(delayedSearch);
                }

                // Schedule new search with delay
                delayedSearch = () -> performSearch(s.toString());
                handler.postDelayed(delayedSearch, SEARCH_DELAY_MS);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                if (delayedSearch != null) {
                    handler.removeCallbacks(delayedSearch);
                }
                performSearch(searchInput.getText().toString());
                return true;
            }
            return false;
        });
    }

    private void setupSearchHistoryChips() {
        chipsAdapter = new ArrayObjectAdapter(new SearchChipPresenter());

        ItemBridgeAdapter chipsBridgeAdapter = new ItemBridgeAdapter(chipsAdapter);
        searchHistoryChips.setAdapter(chipsBridgeAdapter);
        searchHistoryChips.setNumRows(1);

        // Handle chip clicks
        chipsBridgeAdapter.setAdapterListener(new ItemBridgeAdapter.AdapterListener() {
            @Override
            public void onCreate(ItemBridgeAdapter.ViewHolder viewHolder) {
                viewHolder.itemView.setOnClickListener(v -> {
                    int position = viewHolder.getAdapterPosition();
                    if (position >= 0 && position < chipsAdapter.size()) {
                        String query = (String) chipsAdapter.get(position);
                        searchInput.setText(query);
                        searchInput.setSelection(query.length());
                        performSearch(query);
                    }
                });
            }

            @Override
            public void onBind(ItemBridgeAdapter.ViewHolder viewHolder) {}

            @Override
            public void onUnbind(ItemBridgeAdapter.ViewHolder viewHolder) {}

            @Override
            public void onAttachedToWindow(ItemBridgeAdapter.ViewHolder viewHolder) {}

            @Override
            public void onDetachedFromWindow(ItemBridgeAdapter.ViewHolder viewHolder) {}
        });
    }

    private void setupSearchResults() {
        // Use ListRowPresenter to show horizontal rows (like main screen)
        ListRowPresenter listRowPresenter = new ListRowPresenter();
        listRowPresenter.setShadowEnabled(false);
        resultsAdapter = new ArrayObjectAdapter(listRowPresenter);

        // Set to 1 column - each row will be a horizontal list
        searchResults.setNumColumns(1);

        // Create bridge adapter to connect ArrayObjectAdapter to VerticalGridView
        ItemBridgeAdapter bridgeAdapter = new ItemBridgeAdapter(resultsAdapter);
        searchResults.setAdapter(bridgeAdapter);
    }

    private void showAllChannels() {
        EXECUTOR.execute(() -> {
            // Update UI on main thread
            handler.post(() -> {
                resultsAdapter.clear();
                if (!allMovies.isEmpty()) {
                    resultsAdapter.addAll(0, allMovies);
                }
            });
        });
    }

    private void performSearch(String query) {
        if (TextUtils.isEmpty(query)) {
            // Show search history and popular channels when query is empty
            showSearchHistoryAndPopular();
            return;
        }

        // Save search query to history
        saveSearchQuery(query);

        EXECUTOR.execute(() -> {
            List<Movie> results = new ArrayList<>();
            String lowerQuery = query.toLowerCase();

            for (Movie movie : allMovies) {
                // Search by title
                if (movie.getTitle().toLowerCase().contains(lowerQuery)) {
                    results.add(movie);
                }
                // Also search by group
                else if (!TextUtils.isEmpty(movie.getGroup()) &&
                        movie.getGroup().toLowerCase().contains(lowerQuery)) {
                    results.add(movie);
                }
            }

            // Update UI on main thread
            handler.post(() -> {
                // Hide search history section when showing search results
                if (searchHistorySection != null) {
                    searchHistorySection.setVisibility(View.GONE);
                }

                resultsAdapter.clear();
                if (!results.isEmpty()) {
                    // Create a horizontal row with search results
                    HeaderItem header = new HeaderItem("Search Results (" + results.size() + " channels)");
                    ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new SearchCardPresenter());
                    listRowAdapter.addAll(0, results);
                    resultsAdapter.add(new ListRow(header, listRowAdapter));
                }
            });
        });
    }

    private void showSearchHistoryAndPopular() {
        EXECUTOR.execute(() -> {
            // Get search history and popular channels
            List<String> searchHistory = getSearchHistory();
            List<Movie> popularChannels = getPopularChannels();

            // Update UI on main thread
            handler.post(() -> {
                resultsAdapter.clear();

                // Show search history chips
                if (searchHistorySection != null && !searchHistory.isEmpty()) {
                    searchHistorySection.setVisibility(View.VISIBLE);
                    chipsAdapter.clear();
                    chipsAdapter.addAll(0, searchHistory);
                } else if (searchHistorySection != null) {
                    searchHistorySection.setVisibility(View.GONE);
                }

                // Add popular channels as a horizontal row
                if (!popularChannels.isEmpty()) {
                    HeaderItem header = new HeaderItem("Popular Channels");
                    ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new SearchCardPresenter());
                    listRowAdapter.addAll(0, popularChannels);
                    resultsAdapter.add(new ListRow(header, listRowAdapter));
                }
            });
        });
    }

    private void saveSearchQuery(String query) {
        if (TextUtils.isEmpty(query)) return;

        Set<String> history = new LinkedHashSet<>(getSearchHistory());
        // Add to front
        history.remove(query); // Remove if already exists
        List<String> historyList = new ArrayList<>(history);
        historyList.add(0, query);

        // Keep only the most recent searches
        if (historyList.size() > MAX_SEARCH_HISTORY) {
            historyList = historyList.subList(0, MAX_SEARCH_HISTORY);
        }

        prefs.edit().putStringSet(KEY_SEARCH_HISTORY, new LinkedHashSet<>(historyList)).apply();
    }

    private List<String> getSearchHistory() {
        Set<String> history = prefs.getStringSet(KEY_SEARCH_HISTORY, new LinkedHashSet<>());
        return new ArrayList<>(history);
    }

    private void trackChannelClick(Movie movie) {
        if (movie == null || TextUtils.isEmpty(movie.getTitle())) return;

        // Get current click counts
        Map<String, Integer> clicks = getChannelClicks();
        int currentCount = clicks.getOrDefault(movie.getTitle(), 0);
        clicks.put(movie.getTitle(), currentCount + 1);

        // Save back to preferences (convert to string format: "title:count,title:count,...")
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : clicks.entrySet()) {
            if (sb.length() > 0) sb.append(",");
            sb.append(entry.getKey()).append(":").append(entry.getValue());
        }
        prefs.edit().putString(KEY_CHANNEL_CLICKS, sb.toString()).apply();
    }

    private Map<String, Integer> getChannelClicks() {
        String clicksStr = prefs.getString(KEY_CHANNEL_CLICKS, "");
        Map<String, Integer> clicks = new HashMap<>();

        if (!TextUtils.isEmpty(clicksStr)) {
            String[] entries = clicksStr.split(",");
            for (String entry : entries) {
                String[] parts = entry.split(":");
                if (parts.length == 2) {
                    try {
                        clicks.put(parts[0], Integer.parseInt(parts[1]));
                    } catch (NumberFormatException e) {
                        // Skip invalid entries
                    }
                }
            }
        }
        return clicks;
    }

    private List<Movie> getPopularChannels() {
        Map<String, Integer> clicks = getChannelClicks();

        // Create list of movies with click counts
        List<Map.Entry<Movie, Integer>> movieClicks = new ArrayList<>();
        for (Movie movie : allMovies) {
            int clickCount = clicks.getOrDefault(movie.getTitle(), 0);
            if (clickCount > 0) {
                movieClicks.add(new HashMap.SimpleEntry<>(movie, clickCount));
            }
        }

        // Sort by click count descending
        Collections.sort(movieClicks, new Comparator<Map.Entry<Movie, Integer>>() {
            @Override
            public int compare(Map.Entry<Movie, Integer> e1, Map.Entry<Movie, Integer> e2) {
                return e2.getValue().compareTo(e1.getValue());
            }
        });

        // Return top 10 popular channels
        List<Movie> popular = new ArrayList<>();
        int count = Math.min(10, movieClicks.size());
        for (int i = 0; i < count; i++) {
            popular.add(movieClicks.get(i).getKey());
        }

        return popular;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (delayedSearch != null) {
            handler.removeCallbacks(delayedSearch);
        }
    }

    /**
     * Custom CardPresenter for search results that handles clicks
     */
    private class SearchCardPresenter extends CardPresenter {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent) {
            ViewHolder vh = super.onCreateViewHolder(parent);

            // Set up click listener
            vh.view.setOnClickListener(v -> {
                // Get the movie from the view holder
                if (vh.view.getTag() instanceof Movie) {
                    Movie movie = (Movie) vh.view.getTag();
                    trackChannelClick(movie);
                    Intent intent = new Intent(getActivity(), PlaybackActivity.class);
                    intent.putExtra(DetailsActivity.MOVIE, movie);
                    startActivity(intent);
                }
            });

            return vh;
        }

        @Override
        public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
            super.onBindViewHolder(viewHolder, item);
            // Store the movie in the view tag for click handling
            if (item instanceof Movie) {
                viewHolder.view.setTag(item);
            }
        }

        @Override
        public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
            super.onUnbindViewHolder(viewHolder);
            viewHolder.view.setTag(null);
        }
    }
}
