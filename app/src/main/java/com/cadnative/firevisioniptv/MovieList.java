package com.cadnative.firevisioniptv;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.cadnative.firevisioniptv.api.ApiClient;


public final class MovieList {

    private static final String TAG = "MovieList";
    static List<Movie> list;
    private static long count = 0;
    private static boolean isLoadingFromServer = false;

    public interface MovieListCallback {
        void onSuccess(List<Movie> movies);
        void onError(String error);
    }



    /**
     * Load movies from server with fallback to local M3U
     */
    public static void loadMoviesFromServer(Context context, AssetManager assetManager, final MovieListCallback callback) {
        if (isLoadingFromServer) {
            Log.d(TAG, "Already loading from server, skipping duplicate request");
            return;
        }

        isLoadingFromServer = true;

        ApiClient.fetchChannelList(context, new ApiClient.ChannelListCallback() {
            @Override
            public void onSuccess(List<Channel> channels) {
                isLoadingFromServer = false;
                Log.d(TAG, "Successfully loaded " + channels.size() + " channels from server");

                list = new ArrayList<>();
                count = 0;

                String bgImageUrl[] = {
                        "https://firebasestorage.googleapis.com/v0/b/firevisioniptv.appspot.com/o/bk1.jpg?alt=media&token=bc5dafeb-33a8-48d4-b283-6ff22bf3a7e5",
                        "https://firebasestorage.googleapis.com/v0/b/firevisioniptv.appspot.com/o/bk3.png?alt=media&token=bd37b51c-7e9b-4500-9b62-11c98603e9b3",
                        "https://firebasestorage.googleapis.com/v0/b/firevisioniptv.appspot.com/o/bk4.png?alt=media&token=30e5bfc3-f7b3-4d69-bdfd-71fa9ff9b789",
                };

                for (Channel channel : channels) {
                    String name = channel.getChannelName();
                    if (name == null || name.isEmpty()) {
                        name = channel.getChannelId();
                    }

                    String cardImage = channel.getChannelImg();

                    list.add(
                            buildMovieInfo(
                                    name,
                                    name,
                                    channel.getChannelId(),
                                    channel.getChannelUrl(),
                                    channel.getChannelGroup(),
                                    channel.getChannelLanguage(),
                                    cardImage,
                                    bgImageUrl[new Random().nextInt(bgImageUrl.length)]));
                }

                if (callback != null) {
                    callback.onSuccess(list);
                }
            }

            @Override
            public void onError(String error) {
                isLoadingFromServer = false;
                Log.e(TAG, "Failed to load from server: " + error);

                // No fallback - return empty list or error
                if (callback != null) {
                    callback.onError(error);
                }
            }
        });
    }



    private static Movie buildMovieInfo(
            String title,
            String description,
            String studio,
            String videoUrl,
            String group,
            String language,
            String cardImageUrl,
            String backgroundImageUrl) {
        Movie movie = new Movie();
        movie.setId(count++);
        movie.setTitle(title);
        movie.setDescription(description);
        movie.setGroup(group);
        movie.setLanguage(language);
        movie.setStudio(studio);
        movie.setCardImageUrl(cardImageUrl);
        movie.setBackgroundImageUrl(backgroundImageUrl);
        movie.setVideoUrl(videoUrl);
        return movie;
    }
}