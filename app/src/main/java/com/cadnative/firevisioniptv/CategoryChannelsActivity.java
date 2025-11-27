package com.cadnative.firevisioniptv;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;

/**
 * Activity to display channels within a selected category or language
 */
public class CategoryChannelsActivity extends FragmentActivity {
    
    private static final String TAG = "CategoryChannelsActivity";
    private SidebarManager sidebarManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Get category/language info from intent
        Intent intent = getIntent();
        String categoryName = intent.getStringExtra("category_name");
        String categoryType = intent.getStringExtra("category_type"); // "category" or "language"
        
        if (categoryName == null || categoryType == null) {
            Log.e(TAG, "Missing category information");
            finish();
            return;
        }
        
        // Initialize sidebar
        sidebarManager = new SidebarManager(this);
        if ("language".equals(categoryType)) {
            sidebarManager.setup(SidebarManager.SidebarItem.LANGUAGES);
        } else {
            sidebarManager.setup(SidebarManager.SidebarItem.CATEGORIES);
        }
        
        if (savedInstanceState == null) {
            // Create MainFragment filtered by category/language
            MainFragment fragment = MainFragment.newInstanceForCategory(categoryName, categoryType);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_browse_fragment, fragment)
                    .commitNow();
        }
    }
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Navigate back to category or language list
        finish();
    }
}
