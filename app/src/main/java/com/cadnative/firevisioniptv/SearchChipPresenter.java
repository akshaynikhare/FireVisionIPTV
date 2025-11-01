package com.cadnative.firevisioniptv;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.leanback.widget.Presenter;

public class SearchChipPresenter extends Presenter {

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_chip, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        String searchQuery = (String) item;
        TextView chipText = viewHolder.view.findViewById(R.id.chip_text);
        chipText.setText(searchQuery);
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
        // Clean up
    }
}
