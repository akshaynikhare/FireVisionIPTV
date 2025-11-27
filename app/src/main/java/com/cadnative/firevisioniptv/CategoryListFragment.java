package com.cadnative.firevisioniptv;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cadnative.firevisioniptv.api.ApiClient;

/**
 * Fragment to display content categories (Sports, News, Movies, etc.)
 */
public class CategoryListFragment extends Fragment {
    
    private static final String TAG = "CategoryListFragment";
    
    private RecyclerView categoryGrid;
    private ProgressBar loadingProgress;
    private TextView emptyText;
    private TextView headerTitle;
    
    private CategoryCardAdapter adapter;
    private List<CategoryItem> categoryItems = new ArrayList<>();
    private List<Channel> allChannels = new ArrayList<>();
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_category_list, container, false);
        
        categoryGrid = view.findViewById(R.id.category_grid);
        loadingProgress = view.findViewById(R.id.loading_progress);
        emptyText = view.findViewById(R.id.empty_text);
        headerTitle = view.findViewById(R.id.header_title);
        
        headerTitle.setText(R.string.browse_categories);
        
        // Setup grid layout manager
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 4);
        categoryGrid.setLayoutManager(layoutManager);
        
        // Setup adapter
        adapter = new CategoryCardAdapter(getContext(), categoryItems, this::onCategoryClick);
        categoryGrid.setAdapter(adapter);
        
        // Load categories
        loadCategories();
        
        return view;
    }
    
    private void loadCategories() {
        loadingProgress.setVisibility(View.VISIBLE);
        categoryGrid.setVisibility(View.GONE);
        emptyText.setVisibility(View.GONE);
        
        ApiClient.fetchChannelList(getContext(), new ApiClient.ChannelListCallback() {
            @Override
            public void onSuccess(List<Channel> channels) {
                allChannels.clear();
                allChannels.addAll(channels);
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> processCategories());
                }
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to load channels: " + error);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> showError("Failed to load categories: " + error));
                }
            }
        });
    }
    
    private void processCategories() {
        // Group channels by category and count them
        Map<String, Integer> categoryCount = new HashMap<>();
        
        for (Channel channel : allChannels) {
            String category = channel.getChannelGroup();
            if (category == null || category.isEmpty()) {
                category = "General";
            }
            categoryCount.put(category, categoryCount.getOrDefault(category, 0) + 1);
        }
        
        // Create CategoryItem objects
        categoryItems.clear();
        for (Map.Entry<String, Integer> entry : categoryCount.entrySet()) {
            int iconResId = CategoryIconMapper.getIconForCategory(entry.getKey());
            CategoryItem item = new CategoryItem(
                    entry.getKey(),
                    iconResId,
                    entry.getValue(),
                    CategoryItem.Type.CATEGORY
            );
            categoryItems.add(item);
        }
        
        // Sort alphabetically (A to Z)
        categoryItems.sort((item1, item2) -> item1.getName().compareToIgnoreCase(item2.getName()));
        
        // Update UI
        loadingProgress.setVisibility(View.GONE);
        if (categoryItems.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            emptyText.setText(R.string.no_categories_found);
            categoryGrid.setVisibility(View.GONE);
        } else {
            categoryGrid.setVisibility(View.VISIBLE);
            emptyText.setVisibility(View.GONE);
            adapter.updateData(categoryItems);
        }
    }
    
    private void showError(String message) {
        loadingProgress.setVisibility(View.GONE);
        categoryGrid.setVisibility(View.GONE);
        emptyText.setVisibility(View.VISIBLE);
        emptyText.setText(message);
    }
    
    private void onCategoryClick(CategoryItem item) {
        // Navigate to MainFragment with category filter
        MainFragment fragment = MainFragment.newInstanceForCategory(item.getName(), "category");
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_browse_fragment, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }
}
