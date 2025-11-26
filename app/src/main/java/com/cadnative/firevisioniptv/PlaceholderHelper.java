package com.cadnative.firevisioniptv;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

/**
 * Helper class to create modern text-based placeholders for channel logos
 * Uses Material Design inspired approach with channel initials
 */
public class PlaceholderHelper {

    /**
     * Create a modern text-based placeholder with channel initials
     * Similar to Material Design avatar placeholders
     */
    public static Drawable createTextPlaceholder(Context context, String channelName) {
        // Get initials from channel name (max 2 characters)
        String initials = getInitials(channelName);
        
        // Generate color based on channel name hash for consistency
        int backgroundColor = generateColorFromText(channelName);
        
        // Create bitmap with text
        int size = 400; // High resolution for scaling
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        
        // Draw background
        Paint bgPaint = new Paint();
        bgPaint.setColor(backgroundColor);
        bgPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(0, 0, size, size, bgPaint);
        
        // Draw text
        Paint textPaint = new Paint();
        textPaint.setColor(0xFFFFFFFF); // White text
        textPaint.setTextSize(size * 0.4f); // 40% of size
        textPaint.setAntiAlias(true);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);
        
        // Calculate text position (centered)
        Rect bounds = new Rect();
        textPaint.getTextBounds(initials, 0, initials.length(), bounds);
        float x = size / 2f;
        float y = (size / 2f) + (bounds.height() / 2f);
        
        canvas.drawText(initials, x, y, textPaint);
        
        return new BitmapDrawable(context.getResources(), bitmap);
    }
    
    /**
     * Extract initials from channel name (max 2 characters)
     */
    private static String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "TV";
        }
        
        name = name.trim().toUpperCase();
        
        // Remove common prefixes
        name = name.replaceFirst("^(THE|A|AN)\\s+", "");
        
        String[] words = name.split("\\s+");
        
        if (words.length >= 2) {
            // Take first letter of first two words
            return String.valueOf(words[0].charAt(0)) + words[1].charAt(0);
        } else if (name.length() >= 2) {
            // Take first two letters of single word
            return name.substring(0, 2);
        } else {
            // Single letter
            return String.valueOf(name.charAt(0));
        }
    }
    
    /**
     * Generate a consistent color based on text hash
     * Uses Material Design color palette
     */
    private static int generateColorFromText(String text) {
        if (text == null || text.isEmpty()) {
            return 0xFF3498DB; // Default blue
        }
        
        // Material Design inspired color palette with good contrast
        int[] colors = {
            0xFF1ABC9C, // Turquoise
            0xFF2ECC71, // Emerald
            0xFF3498DB, // Blue
            0xFF9B59B6, // Purple
            0xFFE74C3C, // Red
            0xFFE67E22, // Orange
            0xFFF39C12, // Yellow
            0xFF16A085, // Green Sea
            0xFF27AE60, // Nephritis
            0xFF2980B9, // Belize Hole
            0xFF8E44AD, // Wisteria
            0xFFC0392B, // Pomegranate
            0xFFD35400, // Pumpkin
            0xFF34495E, // Wet Asphalt
            0xFF7F8C8D, // Asbestos
            0xFFE91E63, // Pink
            0xFF00BCD4, // Cyan
            0xFF009688, // Teal
            0xFFFF5722, // Deep Orange
            0xFF607D8B  // Blue Grey
        };
        
        // Use hash code to select color consistently
        int hash = Math.abs(text.hashCode());
        return colors[hash % colors.length];
    }
}
