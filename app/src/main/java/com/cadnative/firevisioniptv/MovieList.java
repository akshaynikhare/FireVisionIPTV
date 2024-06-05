package com.cadnative.firevisioniptv;

import android.content.res.AssetManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public final class MovieList {

    static List<Movie> list;
    private static long count = 0;

    public static List<Movie> getAllMovies(AssetManager assetManager) {
        return setupMovies(assetManager);
    }
    public static List<Movie> setupMovies(AssetManager assetManager) {
        DatabaseReference channelsRef = FirebaseDatabase.getInstance().getReference("channels");
        list = new ArrayList<>();

        String bgImageUrl[] = {
                "https://firebasestorage.googleapis.com/v0/b/firevisioniptv.appspot.com/o/bk1.jpg?alt=media&token=bc5dafeb-33a8-48d4-b283-6ff22bf3a7e5",
                "https://firebasestorage.googleapis.com/v0/b/firevisioniptv.appspot.com/o/bk3.png?alt=media&token=bd37b51c-7e9b-4500-9b62-11c98603e9b3",
                "https://firebasestorage.googleapis.com/v0/b/firevisioniptv.appspot.com/o/bk4.png?alt=media&token=30e5bfc3-f7b3-4d69-bdfd-71fa9ff9b789",
        };

        List<Channel> listChannel = new fileReader().readFile(assetManager);


        for (int index = 0; index < listChannel.size(); ++index) {

            String name= listChannel.get(index).getChannelName();
            if(name.isEmpty()){
                name= listChannel.get(index).getChannelId();
            }

            //String cardImage = "https://commondatastorage.googleapis.com/android-tv/Sample%20videos/Zeitgeist/Zeitgeist%202010_%20Year%20in%20Review/card.jpg";
            String cardImage = listChannel.get(index).getChannelImg();



            list.add(
                    buildMovieInfo(
                            name,
                            name,
                            listChannel.get(index).getChannelId(),
                            listChannel.get(index).getChannelUrl(),
                            listChannel.get(index).getChannelGroup(),
                            cardImage,
                            bgImageUrl[new Random().nextInt(bgImageUrl.length)]));
        }

        return list;
    }

    private static Movie buildMovieInfo(
            String title,
            String description,
            String studio,
            String videoUrl,
            String group,
            String cardImageUrl,
            String backgroundImageUrl) {
        Movie movie = new Movie();
        movie.setId(count++);
        movie.setTitle(title);
        movie.setDescription(description);
        movie.setGroup(group);
        movie.setStudio(studio);
        movie.setCardImageUrl(cardImageUrl);
        movie.setBackgroundImageUrl(backgroundImageUrl);
        movie.setVideoUrl(videoUrl);
        return movie;
    }
}