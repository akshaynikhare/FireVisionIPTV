package com.cadnative.firevisioniptv;

import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class fileReader {

    private static final String TAG = "FileReader";
    private static final String EXT_INF_SP = "#EXTINF:";
    private static final String KOD_IP_DROP_TYPE = "#KODIPROP:inputstream.adaptive.license_type=";
    private static final String KOD_IP_DROP_KEY = "#KODIPROP:inputstream.adaptive.license_key=";
    private static final String TVG_NAME = "tvg-name=";
    private static final String TVG_ID = "tvg-id=";
    private static final String TVG_LOGO = "tvg-logo=";
    private static final String GROUP_TITLE = "group-title=";
    private static final String COMMA = ",";
    private static final String HTTP = "http://";
    private static final String HTTPS = "https://";

    private final List<Channel> channelList;

    public fileReader() {
        this.channelList = new ArrayList<>();
    }

    public List<Channel> readFile(AssetManager assetManager) {

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(assetManager.open("playlist.m3u")))) {
            String currentLine;
            Channel channel = null;

            while ((currentLine = bufferedReader.readLine()) != null) {
                try {
                    currentLine = currentLine.replace(",", " ,");
                    currentLine = currentLine.replace("\"", "");

                    if (currentLine.startsWith(EXT_INF_SP)) {
                        channel = new Channel();
                        String[] parts = currentLine.split(",");
                        if (parts.length > 1) {
                            String[] attributes = parts[0].split(" ");
                            for (String attr : attributes) {
                                if (attr.startsWith(TVG_ID)) {
                                    String[] attributesSplit= attr.split("=");
                                    if (attributesSplit.length > 1) {
                                        channel.setChannelId(attributesSplit[1]);
                                    }
                                } else if (attr.startsWith(TVG_NAME)) {
                                        String[] attributesSplit= attr.split("=");
                                        if (attributesSplit.length > 1) {
                                            channel.setChannelName(attributesSplit[1]);
                                        }
                                } else if (attr.startsWith(TVG_LOGO)) {
                                    String[] attributesSplit= attr.split("=");
                                    if (attributesSplit.length > 1) {
                                        channel.setChannelImg(attributesSplit[1]);
                                    }
                                } else if (attr.startsWith(GROUP_TITLE)) {
                                    String[] attributesSplit= attr.split("=");
                                    if (attributesSplit.length > 1) {
                                        channel.setChannelGroup(attributesSplit[1]);
                                    }
                                }
                            }
                            channel.setChannelName(parts[1].trim());
                        }
                    } else if (currentLine.startsWith(KOD_IP_DROP_TYPE)) {
                        if (channel != null) {
                            channel.setChannelDrmType(currentLine.split(KOD_IP_DROP_TYPE)[1].trim());
                        }
                    } else if (currentLine.startsWith(KOD_IP_DROP_KEY)) {
                        if (channel != null) {
                            channel.setChannelDrmKey(currentLine.split(KOD_IP_DROP_KEY)[1].trim());
                        }
                    } else if (currentLine.startsWith(HTTP) || currentLine.startsWith(HTTPS)) {
                        if (channel != null) {
                            channel.setChannelUrl(currentLine);
                            channelList.add(channel);
                        }
                    }
                } catch (Exception e) {
                   // throw new RuntimeException(e);
                    Log.e(TAG, "Error reading file: " + e.getMessage(), e);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading file: " + e.getMessage(), e);
        }

        if (!channelList.isEmpty()) {
            return channelList;
        } else {
            Log.e(TAG, "Error: No channels found in the file");
            return null;
        }
    }
}
