package com.cadnative.firevisioniptv;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Model class representing a category or language folder
 * Used for displaying category/language selection screen
 */
public class CategoryItem implements Parcelable {
    
    public enum Type {
        CATEGORY,
        LANGUAGE
    }
    
    private String name;
    private int iconResId; // For categories (drawable resource ID)
    private int channelCount;
    private Type type;
    
    public CategoryItem(String name, int iconResId, int channelCount, Type type) {
        this.name = name;
        this.iconResId = iconResId;
        this.channelCount = channelCount;
        this.type = type;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public int getIconResId() {
        return iconResId;
    }
    
    public void setIconResId(int iconResId) {
        this.iconResId = iconResId;
    }
    
    public int getChannelCount() {
        return channelCount;
    }
    
    public void setChannelCount(int channelCount) {
        this.channelCount = channelCount;
    }
    
    public Type getType() {
        return type;
    }
    
    public void setType(Type type) {
        this.type = type;
    }
    
    // Parcelable implementation
    protected CategoryItem(Parcel in) {
        name = in.readString();
        iconResId = in.readInt();
        channelCount = in.readInt();
        type = Type.values()[in.readInt()];
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeInt(iconResId);
        dest.writeInt(channelCount);
        dest.writeInt(type.ordinal());
    }
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    public static final Creator<CategoryItem> CREATOR = new Creator<CategoryItem>() {
        @Override
        public CategoryItem createFromParcel(Parcel in) {
            return new CategoryItem(in);
        }
        
        @Override
        public CategoryItem[] newArray(int size) {
            return new CategoryItem[size];
        }
    };
}
