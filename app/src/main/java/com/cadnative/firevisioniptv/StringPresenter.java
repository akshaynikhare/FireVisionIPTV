package com.cadnative.firevisioniptv;

import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.leanback.widget.Presenter;

public class StringPresenter extends Presenter {
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        TextView view = new TextView(parent.getContext());
        view.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        view.setTextColor(ContextCompat.getColor(parent.getContext(), R.color.search_opaque));
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(20, 20, 20, 20);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        ((TextView) viewHolder.view).setText((String) item);
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
        // Nothing to unbind
    }
}