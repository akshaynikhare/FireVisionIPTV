package com.cadnative.firevisioniptv;

import android.view.ViewGroup;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.RowPresenter;

/**
 * Custom ListRowPresenter with Netflix-style spacing
 * Uses Fibonacci numbers for spacing (21, 34, 55dp)
 */
public class NetflixListRowPresenter extends ListRowPresenter {
    private static final int ROW_SPACING = 34; // Fibonacci number

    public NetflixListRowPresenter() {
        super();
        // Set shadow enabled for depth
        setShadowEnabled(false); // Netflix style doesn't use shadows
        // Keep focus zoom disabled for subtle experience
        setKeepChildForeground(false);
    }

    @Override
    protected void onBindRowViewHolder(RowPresenter.ViewHolder holder, Object item) {
        super.onBindRowViewHolder(holder, item);

        // Apply Netflix-style spacing
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) holder.view.getLayoutParams();
        if (lp != null) {
            lp.bottomMargin = ROW_SPACING;
            holder.view.setLayoutParams(lp);
        }
    }

    @Override
    protected void onRowViewSelected(RowPresenter.ViewHolder holder, boolean selected) {
        super.onRowViewSelected(holder, selected);
        // Subtle row selection without heavy animations
    }
}
