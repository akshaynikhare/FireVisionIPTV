package com.cadnative.firevisioniptv;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.tv.TvContract;
import android.net.Uri;
import android.util.Log;

import androidx.tvprovider.media.tv.TvContractCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.realm.Realm;

/**
 * Manages channel synchronization with Fire TV's TIF database
 * Handles inserting, updating, and removing channels
 */
public class ChannelManager {

    private static final String TAG = "ChannelManager";
    private Context mContext;
    private String mInputId;

    public ChannelManager(Context context) {
        mContext = context;
        mInputId = TvContract.buildInputId(
            new ComponentName(context, FireVisionTvInputService.class)
        );
    }

    /**
     * Sync all entitled channels to Fire TV TIF database
     */
    public void syncChannelsToTif() {
        Log.d(TAG, "Starting channel sync to TIF database");

        // Get current channels from your database (Realm)
        List<Channel> appChannels = getChannelsFromRealm();

        // Get existing TIF channels
        Map<String, Long> existingTifChannels = getExistingTifChannels();

        // Insert or update channels
        for (Channel channel : appChannels) {
            String channelId = channel.getChannelId();

            if (existingTifChannels.containsKey(channelId)) {
                // Update existing channel
                updateChannel(existingTifChannels.get(channelId), channel);
            } else {
                // Insert new channel
                insertChannel(channel);
            }
        }

        Log.d(TAG, "Channel sync completed. Synced " + appChannels.size() + " channels");
    }

    /**
     * Get channels from Realm database
     */
    private List<Channel> getChannelsFromRealm() {
        List<Channel> channels = new ArrayList<>();

        try {
            Realm realm = Realm.getDefaultInstance();
            List<Channel> realmChannels = realm.where(Channel.class).findAll();

            // Copy to regular list
            for (Channel channel : realmChannels) {
                Channel copy = new Channel();
                copy.setChannelId(channel.getChannelId());
                copy.setChannelName(channel.getChannelName());
                copy.setChannelUrl(channel.getChannelUrl());
                copy.setChannelImg(channel.getChannelImg());
                copy.setChannelGroup(channel.getChannelGroup());
                channels.add(copy);
            }

            realm.close();
        } catch (Exception e) {
            Log.e(TAG, "Error getting channels from Realm", e);
        }

        return channels;
    }

    /**
     * Get existing channels from TIF database
     */
    private Map<String, Long> getExistingTifChannels() {
        Map<String, Long> existingChannels = new HashMap<>();
        ContentResolver resolver = mContext.getContentResolver();

        Uri channelsUri = TvContract.buildChannelsUriForInput(mInputId);
        String[] projection = {
            TvContract.Channels._ID,
            TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA
        };

        try (Cursor cursor = resolver.query(channelsUri, projection, null, null, null)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(0);
                    String internalData = cursor.getString(1);

                    // Extract channel ID from internal data
                    try {
                        JSONObject json = new JSONObject(internalData);
                        String channelId = json.optString("channelId");
                        if (!channelId.isEmpty()) {
                            existingChannels.put(channelId, id);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing internal data", e);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying TIF channels", e);
        }

        return existingChannels;
    }

    /**
     * Insert a new channel into TIF database
     */
    private Uri insertChannel(Channel channel) {
        ContentResolver resolver = mContext.getContentResolver();

        try {
            // Build internal provider data with channel info and deep link
            JSONObject internalData = new JSONObject();
            internalData.put("channelId", channel.getChannelId());
            internalData.put("channelUrl", channel.getChannelUrl());

            // Create deep link for playback
            Intent playIntent = new Intent(Intent.ACTION_VIEW);
            playIntent.setComponent(new ComponentName(mContext, PlaybackActivity.class));
            playIntent.putExtra("channel", channel);
            String deepLinkUri = playIntent.toUri(Intent.URI_INTENT_SCHEME);
            internalData.put("playbackDeepLinkUri", deepLinkUri);

            // Build channel content values
            ContentValues values = new ContentValues();
            values.put(TvContract.Channels.COLUMN_INPUT_ID, mInputId);
            values.put(TvContract.Channels.COLUMN_TYPE, TvContract.Channels.TYPE_OTHER);
            values.put(TvContract.Channels.COLUMN_DISPLAY_NAME, truncateName(channel.getChannelName()));
            values.put(TvContract.Channels.COLUMN_DISPLAY_NUMBER, channel.getChannelId());
            values.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA, internalData.toString());

            // Add channel logo if available
            if (channel.getChannelImg() != null && !channel.getChannelImg().isEmpty()) {
                values.put(TvContractCompat.Channels.COLUMN_APP_LINK_ICON_URI, channel.getChannelImg());
            }

            Uri channelUri = resolver.insert(TvContract.Channels.CONTENT_URI, values);
            Log.d(TAG, "Inserted channel: " + channel.getChannelName() + " -> " + channelUri);

            return channelUri;

        } catch (Exception e) {
            Log.e(TAG, "Error inserting channel: " + channel.getChannelName(), e);
            return null;
        }
    }

    /**
     * Update an existing channel in TIF database
     */
    private void updateChannel(long tifChannelId, Channel channel) {
        ContentResolver resolver = mContext.getContentResolver();

        try {
            Uri channelUri = TvContract.buildChannelUri(tifChannelId);

            ContentValues values = new ContentValues();
            values.put(TvContract.Channels.COLUMN_DISPLAY_NAME, truncateName(channel.getChannelName()));

            if (channel.getChannelImg() != null && !channel.getChannelImg().isEmpty()) {
                values.put(TvContractCompat.Channels.COLUMN_APP_LINK_ICON_URI, channel.getChannelImg());
            }

            resolver.update(channelUri, values, null, null);
            Log.d(TAG, "Updated channel: " + channel.getChannelName());

        } catch (Exception e) {
            Log.e(TAG, "Error updating channel: " + channel.getChannelName(), e);
        }
    }

    /**
     * Remove all channels from TIF database
     */
    public void removeAllChannels() {
        ContentResolver resolver = mContext.getContentResolver();
        Uri channelsUri = TvContract.buildChannelsUriForInput(mInputId);

        int deleted = resolver.delete(channelsUri, null, null);
        Log.d(TAG, "Removed " + deleted + " channels from TIF");
    }

    /**
     * Truncate channel name to 25 characters (Fire TV requirement)
     */
    private String truncateName(String name) {
        if (name == null) return "Unknown";
        if (name.length() <= 25) return name;
        return name.substring(0, 22) + "...";
    }

    /**
     * Trigger channel initialization broadcast
     */
    public void triggerChannelUpdate() {
        Intent intent = new Intent(TvContract.ACTION_INITIALIZE_PROGRAMS);
        intent.setComponent(new ComponentName(mContext, ChannelUpdateReceiver.class));
        mContext.sendBroadcast(intent);
        Log.d(TAG, "Triggered channel update broadcast");
    }
}
