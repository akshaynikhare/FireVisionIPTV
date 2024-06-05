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

    private static final int BACKGROUND_UPDATE_DELAY = 300;
    private static final int GRID_ITEM_WIDTH = 200;
    private static final int GRID_ITEM_HEIGHT = 200;
    private static final int MAX_NUM_COLS =5;

    private final Handler mHandler = new Handler(Looper.myLooper());
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;
    private Timer mBackgroundTimer;
    private String mBackgroundUri;
    private BackgroundManager mBackgroundManager;

    private AssetManager assetManager;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");

        assetManager = getContext().getAssets();

        super.onActivityCreated(savedInstanceState);

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



    private void loadRows() {


        List<Movie> list = MovieList.setupMovies(assetManager);
        ArrayObjectAdapter rowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        CardPresenter cardPresenter = new CardPresenter();

        Map<String, List<Movie>> groupedMovies = new TreeMap<>();  // Using TreeMap for alphabetical sorting

        for (Movie movie : list) {
            String group = movie.getGroup();
            if (group == null || group.isEmpty()) {
                group = "zzz_other";  // Prefix with 'zzz_' to ensure it's last alphabetically
            }
            if (!groupedMovies.containsKey(group)) {
                groupedMovies.put(group, new ArrayList<>());
            }
            groupedMovies.get(group).add(movie);
        }

// Move "other" group to a separate variable and remove it from the TreeMap
        List<Movie> otherMovies = groupedMovies.remove("zzz_other");

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

// Now handle the "other" group if it exists
        if (otherMovies != null && !otherMovies.isEmpty()) {
            int numRows = (otherMovies.size() + MAX_NUM_COLS - 1) / MAX_NUM_COLS;

            for (int i = 0; i < numRows; i++) {
                ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(cardPresenter);

                int startIndex = i * MAX_NUM_COLS;
                int endIndex = Math.min((i + 1) * MAX_NUM_COLS, otherMovies.size());

                listRowAdapter.addAll(0, otherMovies.subList(startIndex, endIndex));

                String headerText = "Other";
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
        mBackgroundManager.attach(getActivity().getWindow());

        mDefaultBackground = ContextCompat.getDrawable(getContext(), R.drawable.default_background);
        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    private void setupUIElements() {

        String appVersion = getAppVersion(requireContext());
        setTitle(getString(R.string.browse_title) + " (v" + appVersion + ")");


        setHeadersState(HEADERS_HIDDEN);
        setHeadersTransitionOnBackEnabled(true);

        // set fastLane (or headers) background color
        setBrandColor(ContextCompat.getColor(getContext(), R.color.fastlane_background));
        // set search icon color
        setSearchAffordanceColor(ContextCompat.getColor(getContext(), R.color.search_opaque));

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

        setOnSearchClickedListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), SearchActivity.class);
                startActivity(intent);
            }
        });


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

    private List<Movie> loadAllMovies() {
        return MovieList.setupMovies(FirevisionApplication.getAppContext().getAssets());
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