package com.cadnative.firevisioniptv;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class to map category names to their respective icons
 */
public class CategoryIconMapper {
    
    private static final Map<String, Integer> categoryIconMap = new HashMap<>();
    
    static {
        // Initialize category to icon mapping
        categoryIconMap.put("Sports", R.drawable.ic_category_sports);
        categoryIconMap.put("sport", R.drawable.ic_category_sports);
        categoryIconMap.put("sports", R.drawable.ic_category_sports);
        
        categoryIconMap.put("News", R.drawable.ic_category_news);
        categoryIconMap.put("news", R.drawable.ic_category_news);
        
        categoryIconMap.put("Movies", R.drawable.ic_category_movies);
        categoryIconMap.put("movie", R.drawable.ic_category_movies);
        categoryIconMap.put("movies", R.drawable.ic_category_movies);
        categoryIconMap.put("films", R.drawable.ic_category_movies);
        categoryIconMap.put("cinema", R.drawable.ic_category_movies);
        
        categoryIconMap.put("Entertainment", R.drawable.ic_category_entertainment);
        categoryIconMap.put("entertainment", R.drawable.ic_category_entertainment);
        
        categoryIconMap.put("Music", R.drawable.ic_category_music);
        categoryIconMap.put("music", R.drawable.ic_category_music);
        
        categoryIconMap.put("Kids", R.drawable.ic_category_kids);
        categoryIconMap.put("kids", R.drawable.ic_category_kids);
        categoryIconMap.put("children", R.drawable.ic_category_kids);
        categoryIconMap.put("cartoon", R.drawable.ic_category_kids);
        categoryIconMap.put("cartoons", R.drawable.ic_category_kids);
        
        categoryIconMap.put("Documentary", R.drawable.ic_category_documentary);
        categoryIconMap.put("documentary", R.drawable.ic_category_documentary);
        categoryIconMap.put("documentaries", R.drawable.ic_category_documentary);
        categoryIconMap.put("docu", R.drawable.ic_category_documentary);
        
        categoryIconMap.put("General", R.drawable.ic_category_general);
        categoryIconMap.put("general", R.drawable.ic_category_general);
        categoryIconMap.put("Uncategorized", R.drawable.ic_category_general);
        categoryIconMap.put("uncategorized", R.drawable.ic_category_general);
        categoryIconMap.put("Other", R.drawable.ic_category_general);
        categoryIconMap.put("other", R.drawable.ic_category_general);
    }
    
    /**
     * Get icon resource ID for a category name
     * Returns general icon if category is not found
     */
    public static int getIconForCategory(String categoryName) {
        if (categoryName == null || categoryName.isEmpty()) {
            return R.drawable.ic_category_general;
        }
        
        // Try exact match first
        Integer icon = categoryIconMap.get(categoryName);
        if (icon != null) {
            return icon;
        }
        
        // Try case-insensitive match
        icon = categoryIconMap.get(categoryName.toLowerCase());
        if (icon != null) {
            return icon;
        }
        
        // Try to find partial match
        String lowerCategory = categoryName.toLowerCase();
        for (Map.Entry<String, Integer> entry : categoryIconMap.entrySet()) {
            if (lowerCategory.contains(entry.getKey().toLowerCase()) || 
                entry.getKey().toLowerCase().contains(lowerCategory)) {
                return entry.getValue();
            }
        }
        
        // Default to general icon
        return R.drawable.ic_category_general;
    }
    
    /**
     * Get first two letters of language for display
     * e.g., "English" -> "EN", "Hindi" -> "HI"
     */
    public static String getLanguageShortCode(String languageName) {
        if (languageName == null || languageName.isEmpty()) {
            return "??";
        }
        
        if (languageName.length() >= 2) {
            return languageName.substring(0, 2).toUpperCase();
        }
        
        return languageName.toUpperCase();
    }
}
