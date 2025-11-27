package com.cadnative.firevisioniptv;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adapter for displaying category/language cards in a grid
 */
public class CategoryCardAdapter extends RecyclerView.Adapter<CategoryCardAdapter.CategoryViewHolder> {
    
    private Context context;
    private List<CategoryItem> items;
    private OnCategoryClickListener clickListener;
    
    public interface OnCategoryClickListener {
        void onCategoryClick(CategoryItem item);
    }
    
    public CategoryCardAdapter(Context context, List<CategoryItem> items, OnCategoryClickListener listener) {
        this.context = context;
        this.items = items;
        this.clickListener = listener;
    }
    
    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category_card, parent, false);
        return new CategoryViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        CategoryItem item = items.get(position);
        
        holder.categoryName.setText(item.getName());
        holder.channelCount.setText(context.getString(R.string.channel_count, item.getChannelCount()));
        
        // Show icon for categories, text for languages
        if (item.getType() == CategoryItem.Type.CATEGORY) {
            holder.categoryIcon.setVisibility(View.VISIBLE);
            holder.languageText.setVisibility(View.GONE);
            holder.categoryIcon.setImageResource(item.getIconResId());
        } else {
            holder.categoryIcon.setVisibility(View.GONE);
            holder.languageText.setVisibility(View.VISIBLE);
            holder.languageText.setText(CategoryIconMapper.getLanguageShortCode(item.getName()));
        }
        
        // Click listener
        holder.card.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onCategoryClick(item);
            }
        });
        
        // Focus change listener for visual feedback
        holder.card.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                holder.focusOverlay.setVisibility(View.VISIBLE);
                holder.card.setCardElevation(context.getResources().getDimension(R.dimen.card_elevation_focused));
            } else {
                holder.focusOverlay.setVisibility(View.INVISIBLE);
                holder.card.setCardElevation(context.getResources().getDimension(R.dimen.card_elevation));
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return items.size();
    }
    
    public void updateData(List<CategoryItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }
    
    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        CardView card;
        ImageView categoryIcon;
        TextView languageText;
        TextView categoryName;
        TextView channelCount;
        View focusOverlay;
        
        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            card = (CardView) itemView;
            categoryIcon = itemView.findViewById(R.id.category_icon);
            languageText = itemView.findViewById(R.id.language_text);
            categoryName = itemView.findViewById(R.id.category_name);
            channelCount = itemView.findViewById(R.id.channel_count);
            focusOverlay = itemView.findViewById(R.id.focus_overlay);
        }
    }
}
