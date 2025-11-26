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
    private SidebarManager sidebarManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // Initialize sidebar with SidebarManager
        sidebarManager = new SidebarManager(this);
        sidebarManager.setup(SidebarManager.SidebarItem.SEARCH);

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