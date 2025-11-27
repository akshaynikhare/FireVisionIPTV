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
 * Fragment to display language categories (English, Hindi, etc.)
 */
public class LanguageListFragment extends Fragment {
    
    private static final String TAG = "LanguageListFragment";
    
    private RecyclerView categoryGrid;
    private ProgressBar loadingProgress;
    private TextView emptyText;
    private TextView headerTitle;
    
    private CategoryCardAdapter adapter;
    private List<CategoryItem> languageItems = new ArrayList<>();
    private List<Channel> allChannels = new ArrayList<>();
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_category_list, container, false);
        
        categoryGrid = view.findViewById(R.id.category_grid);
        loadingProgress = view.findViewById(R.id.loading_progress);
        emptyText = view.findViewById(R.id.empty_text);
        headerTitle = view.findViewById(R.id.header_title);
        
        headerTitle.setText(R.string.browse_languages);
        
        // Setup grid layout manager
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 4);
        categoryGrid.setLayoutManager(layoutManager);
        
        // Setup adapter
        adapter = new CategoryCardAdapter(getContext(), languageItems, this::onLanguageClick);
        categoryGrid.setAdapter(adapter);
        
        // Load languages
        loadLanguages();
        
        return view;
    }
    
    private void loadLanguages() {
        loadingProgress.setVisibility(View.VISIBLE);
        categoryGrid.setVisibility(View.GONE);
        emptyText.setVisibility(View.GONE);
        
        ApiClient.fetchChannelList(getContext(), new ApiClient.ChannelListCallback() {
            @Override
            public void onSuccess(List<Channel> channels) {
                allChannels.clear();
                allChannels.addAll(channels);
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> processLanguages());
                }
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to load channels: " + error);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> showError("Failed to load languages: " + error));
                }
            }
        });
    }
    
    private void processLanguages() {
        // Group channels by individual language and count them
        Map<String, Integer> languageCount = new HashMap<>();
        
        for (Channel channel : allChannels) {
            String languages = channel.getChannelLanguage();
            if (languages == null || languages.isEmpty() || languages.equals("Unknown")) {
                continue; // Skip unknown languages
            }
            
            // Split multiple languages (e.g., "Urdu, Hindi, English" -> ["Urdu", "Hindi", "English"])
            String[] languageArray = languages.split(",");
            for (String lang : languageArray) {
                String trimmedLang = lang.trim();
                if (!trimmedLang.isEmpty() && !trimmedLang.equals("Unknown")) {
                    languageCount.put(trimmedLang, languageCount.getOrDefault(trimmedLang, 0) + 1);
                }
            }
        }
        
        // Create CategoryItem objects (using LANGUAGE type)
        languageItems.clear();
        for (Map.Entry<String, Integer> entry : languageCount.entrySet()) {
            CategoryItem item = new CategoryItem(
                    entry.getKey(),
                    0, // No icon for languages
                    entry.getValue(),
                    CategoryItem.Type.LANGUAGE
            );
            languageItems.add(item);
        }
        
        // Sort by channel count (highest to lowest)
        languageItems.sort((item1, item2) -> Integer.compare(item2.getChannelCount(), item1.getChannelCount()));
        
        // Update UI
        loadingProgress.setVisibility(View.GONE);
        if (languageItems.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            emptyText.setText(R.string.no_languages_found);
            categoryGrid.setVisibility(View.GONE);
        } else {
            categoryGrid.setVisibility(View.VISIBLE);
            emptyText.setVisibility(View.GONE);
            adapter.updateData(languageItems);
        }
    }
    
    private void showError(String message) {
        loadingProgress.setVisibility(View.GONE);
        categoryGrid.setVisibility(View.GONE);
        emptyText.setVisibility(View.VISIBLE);
        emptyText.setText(message);
    }
    
    private void onLanguageClick(CategoryItem item) {
        // Navigate to MainFragment with language filter
        MainFragment fragment = MainFragment.newInstanceForCategory(item.getName(), "language");
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_browse_fragment, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }
}
