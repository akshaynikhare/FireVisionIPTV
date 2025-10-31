package com.cadnative.firevisioniptv.api;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.cadnative.firevisioniptv.Channel;

public class ApiClient {
    private static final String TAG = "ApiClient";
    private static final String BASE_URL = "https://tv.cadnative.com";
    private static final int TIMEOUT = 30000; // 30 seconds

    public interface ChannelListCallback {
        void onSuccess(List<Channel> channels);
        void onError(String error);
    }

    public interface AppVersionCallback {
        void onSuccess(AppVersionInfo versionInfo);
        void onError(String error);
    }

    public static class AppVersionInfo {
        public boolean updateAvailable;
        public boolean isMandatory;
        public String versionName;
        public int versionCode;
        public String downloadUrl;
        public long fileSize;
        public String releaseNotes;

        public AppVersionInfo(JSONObject json) throws Exception {
            this.updateAvailable = json.optBoolean("updateAvailable", false);
            this.isMandatory = json.optBoolean("isMandatory", false);

            if (json.has("latestVersion")) {
                JSONObject latest = json.getJSONObject("latestVersion");
                this.versionName = latest.optString("versionName", "");
                this.versionCode = latest.optInt("versionCode", 0);
                this.downloadUrl = latest.optString("downloadUrl", "");
                this.fileSize = latest.optLong("apkFileSize", 0);
                this.releaseNotes = latest.optString("releaseNotes", "");
            }
        }
    }

    /**
     * Fetch channel list from server
     */
    public static void fetchChannelList(final ChannelListCallback callback) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(BASE_URL + "/api/v1/channels");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(TIMEOUT);
                connection.setReadTimeout(TIMEOUT);
                connection.setRequestProperty("Accept", "application/json");

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String response = readStream(connection.getInputStream());
                    JSONObject jsonResponse = new JSONObject(response);

                    if (jsonResponse.optBoolean("success", false)) {
                        JSONArray channelsArray = jsonResponse.getJSONArray("data");
                        List<Channel> channels = parseChannels(channelsArray);

                        if (callback != null) {
                            callback.onSuccess(channels);
                        }
                    } else {
                        if (callback != null) {
                            callback.onError("Server returned error");
                        }
                    }
                } else {
                    if (callback != null) {
                        callback.onError("Server responded with code: " + responseCode);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching channel list", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    /**
     * Check for app updates
     */
    public static void checkForUpdates(int currentVersionCode, final AppVersionCallback callback) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(BASE_URL + "/api/v1/app/version?currentVersion=" + currentVersionCode);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(TIMEOUT);
                connection.setReadTimeout(TIMEOUT);
                connection.setRequestProperty("Accept", "application/json");

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String response = readStream(connection.getInputStream());
                    JSONObject jsonResponse = new JSONObject(response);

                    if (jsonResponse.optBoolean("success", false)) {
                        AppVersionInfo versionInfo = new AppVersionInfo(jsonResponse);
                        if (callback != null) {
                            callback.onSuccess(versionInfo);
                        }
                    } else {
                        if (callback != null) {
                            callback.onError("Failed to check version");
                        }
                    }
                } else {
                    if (callback != null) {
                        callback.onError("Server responded with code: " + responseCode);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking for updates", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    /**
     * Parse JSON array to Channel list
     */
    private static List<Channel> parseChannels(JSONArray jsonArray) throws Exception {
        List<Channel> channels = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject channelObj = jsonArray.getJSONObject(i);
            Channel channel = new Channel();

            channel.setChannelId(channelObj.optString("channelId", ""));
            channel.setChannelName(channelObj.optString("channelName", "Unknown"));
            channel.setChannelUrl(channelObj.optString("channelUrl", ""));
            channel.setChannelImg(channelObj.optString("channelImg", ""));
            channel.setChannelGroup(channelObj.optString("channelGroup", "Uncategorized"));
            channel.setChannelDrmKey(channelObj.optString("channelDrmKey", ""));
            channel.setChannelDrmType(channelObj.optString("channelDrmType", ""));

            channels.add(channel);
        }

        return channels;
    }

    /**
     * Read input stream to string
     */
    private static String readStream(InputStream inputStream) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder result = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            result.append(line);
        }

        reader.close();
        return result.toString();
    }
}
