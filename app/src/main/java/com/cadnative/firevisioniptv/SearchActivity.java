package com.cadnative.firevisioniptv;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

import androidx.fragment.app.FragmentActivity;
import androidx.leanback.app.SearchSupportFragment;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.SpeechRecognitionCallback;

public class SearchActivity extends FragmentActivity {
    private static final boolean FINISH_ON_RECOGNIZER_CANCELED = true;
    private SearchSupportFragment mSearchFragment;
    private SearchResultProvider mSearchResultProvider;

    private View sidebarHome;
    private View sidebarSearch;
    private View sidebarCategories;
    private View sidebarFavorites;
    private View sidebarSettings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // Initialize sidebar
        setupSidebar();

        mSearchFragment = (SearchSupportFragment) getSupportFragmentManager()
                .findFragmentById(R.id.search_fragment);

        mSearchFragment.setOnItemViewClickedListener(new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                      RowPresenter.ViewHolder rowViewHolder, Row row) {
                if (item instanceof Movie) {
                    Movie movie = (Movie) item;
                    Intent intent = new Intent(SearchActivity.this, PlaybackActivity.class);
                    intent.putExtra(DetailsActivity.MOVIE, movie);
                    startActivity(intent);
                }
            }
        });

        // Create and set the search result provider
        mSearchResultProvider = new SearchResultProvider();
        mSearchFragment.setSearchResultProvider(mSearchResultProvider);

        // Enable voice search
        if (!hasVoiceRecognition()) {
            mSearchFragment.setSpeechRecognitionCallback(new SpeechRecognitionCallback() {
                @Override
                public void recognizeSpeech() {
                    // Start voice recognition if available
                    startActivityForResult(mSearchFragment.getRecognizerIntent(), 0);
                }
            });
        }

        // Set search bar background colors
        mSearchFragment.setBadgeDrawable(getResources().getDrawable(R.drawable.search_background));

        // Set search as selected in sidebar
        if (sidebarSearch != null) {
            sidebarSearch.setSelected(true);
        }
    }

    private void setupSidebar() {
        sidebarHome = findViewById(R.id.sidebar_home);
        sidebarSearch = findViewById(R.id.sidebar_search);
        sidebarCategories = findViewById(R.id.sidebar_categories);
        sidebarFavorites = findViewById(R.id.sidebar_favorites);
        sidebarSettings = findViewById(R.id.sidebar_settings);

        // Home - Go back to MainActivity
        sidebarHome.setOnClickListener(v -> {
            finish(); // Close search and return to main
        });

        // Search - Already here, do nothing
        sidebarSearch.setOnClickListener(v -> {
            // Already on search screen
        });

        // Categories - Go back to MainActivity
        sidebarCategories.setOnClickListener(v -> {
            finish(); // Close search and return to main
        });

        // Favorites - Go back to MainActivity with favorites flag
        sidebarFavorites.setOnClickListener(v -> {
            finish(); // Close search and return to main
        });

        // Settings
        sidebarSettings.setOnClickListener(v -> {
            selectSidebarItem(sidebarSettings);
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });

        // Set up focus change listeners for visual feedback
        setupFocusListener(sidebarHome);
        setupFocusListener(sidebarSearch);
        setupFocusListener(sidebarCategories);
        setupFocusListener(sidebarFavorites);
        setupFocusListener(sidebarSettings);
    }

    private void setupFocusListener(View view) {
        view.setOnFocusChangeListener((v, hasFocus) -> {
            if (v.getId() != R.id.sidebar_search) {
                v.setSelected(hasFocus);
            }
        });
    }

    private void selectSidebarItem(View selectedItem) {
        // Deselect all except search (which should stay selected)
        if (selectedItem.getId() != R.id.sidebar_search) {
            sidebarHome.setSelected(false);
        }
        sidebarSearch.setSelected(selectedItem.getId() == R.id.sidebar_search);
        sidebarCategories.setSelected(false);
        sidebarFavorites.setSelected(false);
        sidebarSettings.setSelected(false);

        // Select the clicked item
        selectedItem.setSelected(true);
    }

    private boolean hasVoiceRecognition() {
        return getPackageManager().queryIntentActivities(
                mSearchFragment.getRecognizerIntent(), 0).isEmpty();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Handle voice search activation with microphone button
        if (keyCode == KeyEvent.KEYCODE_BUTTON_3) {
            if (!hasVoiceRecognition()) {
                mSearchFragment.startRecognition();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}