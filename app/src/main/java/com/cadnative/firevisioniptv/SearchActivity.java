package com.cadnative.firevisioniptv;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;
import androidx.leanback.app.SearchSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.ObjectAdapter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

import com.cadnative.firevisioniptv.DetailsActivity;
import com.cadnative.firevisioniptv.Movie;
import com.cadnative.firevisioniptv.PlaybackActivity;
import com.cadnative.firevisioniptv.R;
import com.cadnative.firevisioniptv.SearchResultProvider;

public class SearchActivity extends FragmentActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        SearchSupportFragment searchFragment = (SearchSupportFragment) getSupportFragmentManager()
                .findFragmentById(R.id.search_fragment);

        searchFragment.setOnItemViewClickedListener(new OnItemViewClickedListener() {
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

        searchFragment.setSearchResultProvider(new SearchResultProvider());
    }
}