package com.cadnative.firevisioniptv;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.leanback.app.SearchSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.ObjectAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchResultProvider implements SearchSupportFragment.SearchResultProvider {
    private static final String TAG = "SearchResultProvider";
    private static final int SEARCH_DELAY_MS = 300;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    
    private final ArrayObjectAdapter mRowsAdapter;
    private final List<Movie> mAllMovies;
    private final Handler mHandler;
    private final Map<String, List<String>> mSearchSuggestions;
    
    private Runnable mDelayedLoad;

    SearchResultProvider() {
        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        // Movies should be loaded from server via loadMoviesFromServer
        mAllMovies = new ArrayList<>();
        mHandler = new Handler(Looper.getMainLooper());
        mSearchSuggestions = buildSearchSuggestions();
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

    public void loadQueryResults(String query) {
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
}