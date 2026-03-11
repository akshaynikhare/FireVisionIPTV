package com.cadnative.firevisioniptv;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.cadnative.firevisioniptv.api.ApiClient;


public final class MovieList {

    private static final String TAG = "MovieList";

    public interface MovieListCallback {
        void onSuccess(List<Movie> movies);
        void onError(String error);
    }

    /**
     * Fetch channels from the server and convert them to Movie objects.
     * De-duplication / caching is handled by {@link ChannelViewModel}; this method
     * always performs a live network request.
     */
    public static void loadMoviesFromServer(Context context, AssetManager assetManager,
                                            final MovieListCallback callback) {
        ApiClient.fetchChannelList(context, new ApiClient.ChannelListCallback() {
            @Override
            public void onSuccess(List<Channel> channels) {
                Log.d(TAG, "Successfully loaded " + channels.size() + " channels from server");

                String[] bgImageUrls = {
                        "https://firebasestorage.googleapis.com/v0/b/firevisioniptv.appspot.com/o/bk1.jpg?alt=media&token=bc5dafeb-33a8-48d4-b283-6ff22bf3a7e5",
                        "https://firebasestorage.googleapis.com/v0/b/firevisioniptv.appspot.com/o/bk3.png?alt=media&token=bd37b51c-7e9b-4500-9b62-11c98603e9b3",
                        "https://firebasestorage.googleapis.com/v0/b/firevisioniptv.appspot.com/o/bk4.png?alt=media&token=30e5bfc3-f7b3-4d69-bdfd-71fa9ff9b789",
                };
                Random random = new Random();

                List<Movie> movies = new ArrayList<>(channels.size());
                for (int i = 0; i < channels.size(); i++) {
                    Channel channel = channels.get(i);
                    String name = channel.getChannelName();
                    if (name == null || name.isEmpty()) {
                        name = channel.getChannelId();
                    }
                    movies.add(buildMovieInfo(
                            i,
                            name,
                            channel.getChannelId(),
                            channel.getChannelUrl(),
                            channel.getChannelGroup(),
                            channel.getChannelLanguage(),
                            channel.getChannelImg(),
                            bgImageUrls[random.nextInt(bgImageUrls.length)]));
                }

                if (callback != null) {
                    callback.onSuccess(movies);
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to load from server: " + error);
                if (callback != null) {
                    callback.onError(error);
                }
            }
        });
    }

    private static Movie buildMovieInfo(
            long id,
            String title,
            String studio,
            String videoUrl,
            String group,
            String language,
            String cardImageUrl,
            String backgroundImageUrl) {
        Movie movie = new Movie();
        movie.setId(id);
        movie.setTitle(title);
        movie.setDescription(title);
        movie.setGroup(group);
        movie.setLanguage(language);
        movie.setStudio(studio);
        movie.setCardImageUrl(cardImageUrl);
        movie.setBackgroundImageUrl(backgroundImageUrl);
        movie.setVideoUrl(videoUrl);
        return movie;
    }
}
