package com.cadnative.firevisioniptv;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

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

import java.util.ArrayList;
import java.util.List;

import com.cadnative.firevisioniptv.CardPresenter;
import com.cadnative.firevisioniptv.DetailsActivity;
import com.cadnative.firevisioniptv.PlaybackActivity;
import com.cadnative.firevisioniptv.Movie;
import com.cadnative.firevisioniptv.MovieList;
import com.cadnative.firevisioniptv.FirevisionApplication;

public class SearchResultProvider implements SearchSupportFragment.SearchResultProvider {
    private ArrayObjectAdapter mRowsAdapter;
    private List<Movie> mAllMovies;

    SearchResultProvider() {
        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        mAllMovies = MovieList.setupMovies(FirevisionApplication.getAppContext().getAssets());
    }

    @Override
    public ObjectAdapter getResultsAdapter() {
        return mRowsAdapter;
    }

    @Override
    public boolean onQueryTextChange(String newQuery) {
        loadQueryResults(newQuery);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        loadQueryResults(query);
        return true;
    }

    private void loadQueryResults(String query) {
        mRowsAdapter.clear();
        List<Movie> results = new ArrayList<>();
        for (Movie movie : mAllMovies) {
            if (movie.getTitle().toLowerCase().contains(query.toLowerCase())) {
                results.add(movie);
            }
        }

        HeaderItem header = new HeaderItem("Search Results");
        ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new CardPresenter());
        listRowAdapter.addAll(0, results);
        mRowsAdapter.add(new ListRow(header, listRowAdapter));
    }
}