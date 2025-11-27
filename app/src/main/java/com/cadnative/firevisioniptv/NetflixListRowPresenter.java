package com.cadnative.firevisioniptv;

import android.view.View;
import android.view.ViewGroup;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.RowPresenter;

/**
 * Optimized ListRowPresenter for tight spacing
 * Uses standard Android Leanback layouts with minimal configuration
 */
public class NetflixListRowPresenter extends ListRowPresenter {

    public NetflixListRowPresenter() {
        super();
        // Remove headers to save vertical space
        setHeaderPresenter(null);
        // Disable shadows for cleaner look
        setShadowEnabled(false);
        // Disable selection effects that add padding
        setSelectEffectEnabled(false);
        // Set to 0 for minimal row height
        setExpandedRowHeight(0);
        // Don't keep child in foreground to reduce overhead
        setKeepChildForeground(false);
    }

    @Override
    protected void onBindRowViewHolder(RowPresenter.ViewHolder holder, Object item) {
        super.onBindRowViewHolder(holder, item);
        
        // Set minimal spacing between rows
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) holder.view.getLayoutParams();
        if (lp != null) {
            lp.topMargin = 3;
            lp.bottomMargin = 3;
            lp.leftMargin = 0;
            lp.rightMargin = 0;
            holder.view.setLayoutParams(lp);
        }
        
        // Remove padding from the row
        holder.view.setPadding(0, 0, 0, 0);
        
        // Access the HorizontalGridView for further optimization
        if (holder instanceof ListRowPresenter.ViewHolder) {
            ListRowPresenter.ViewHolder listHolder = (ListRowPresenter.ViewHolder) holder;
            androidx.leanback.widget.HorizontalGridView gridView = listHolder.getGridView();
            if (gridView != null) {
                // Minimal horizontal padding
                gridView.setPadding(12, 0, 12, 0);
                // Remove vertical spacing
                gridView.setVerticalSpacing(0);
            }
        }
    }
}
