package com.cadnative.firevisioniptv;

import android.os.Parcel;
import android.os.Parcelable;

public class Channel implements Parcelable {

    private String channelName;
    private String channelId;
    private String channelUrl;
    private String channelImg;
    private String channelGroup;
    private String channelLanguage;
    private String channelDrmKey;
    private String channelDrmType;

    public Channel() {
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getChannelUrl() {
        return channelUrl;
    }

    public void setChannelUrl(String channelUrl) {
        this.channelUrl = channelUrl;
    }

    public String getChannelImg() {
        return channelImg;
    }

    public void setChannelImg(String channelImg) {
        this.channelImg = channelImg;
    }

    public String getChannelGroup() {
        return channelGroup;
    }

    public void setChannelGroup(String channelGroup) {
        this.channelGroup = channelGroup;
    }

    public String getChannelLanguage() {
        return channelLanguage;
    }

    public void setChannelLanguage(String channelLanguage) {
        this.channelLanguage = channelLanguage;
    }

    public String getChannelDrmKey() {
        return channelDrmKey;
    }

    public void setChannelDrmKey(String channelDrmKey) {
        this.channelDrmKey = channelDrmKey;
    }

    public String getChannelDrmType() {
        return channelDrmType;
    }

    public void setChannelDrmType(String channelDrmType) {
        this.channelDrmType = channelDrmType;
    }

    @Override
    public String toString() {
        return "Channel{" +
                "channelName='" + channelName + '\'' +
                ", channelUrl='" + channelUrl + '\'' +
                ", channelImg='" + channelImg + '\'' +
                ", channelGroup='" + channelGroup + '\'' +
                ", channelLanguage='" + channelLanguage + '\'' +
                ", channelDrmKey='" + channelDrmKey + '\'' +
                ", channelDrmType='" + channelDrmType + '\'' +
                '}';
    }

    public Channel(Parcel in) {
        String[] data = new String[7];
        in.readStringArray(data);
        this.channelName = data[0];
        this.channelUrl = data[1];
        this.channelImg = data[2];
        this.channelGroup = data[3];
        this.channelLanguage = data[4];
        this.channelDrmKey = data[5];
        this.channelDrmType = data[6];
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeStringArray(new String[]{this.channelName, this.channelUrl, this.channelImg,
                this.channelGroup, this.channelLanguage, this.channelDrmKey, this.channelDrmType});
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator<Channel>() {
        public Channel createFromParcel(Parcel in) {
            return new Channel(in);
        }

        @Override
        public Channel[] newArray(int i) {
            return new Channel[i];
        }
    };

}