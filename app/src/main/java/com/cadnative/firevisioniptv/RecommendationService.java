package com.cadnative.firevisioniptv;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.media.tv.TvContract;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.tvprovider.media.tv.Channel;
import androidx.tvprovider.media.tv.ChannelLogoUtils;
import androidx.tvprovider.media.tv.PreviewProgram;
import androidx.tvprovider.media.tv.TvContractCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

import java.util.ArrayList;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.O)
public class RecommendationService extends JobService {
    private static final String TAG = "RecommendationService";
    private AsyncTask<Void, Void, Void> mRecommendationTask;

    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        mRecommendationTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                List<Movie> movies = MovieList.setupMovies(getAssets());
                updateRecommendations(movies);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                jobFinished(jobParameters, false);
            }
        };
        mRecommendationTask.execute();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        if (mRecommendationTask != null) {
            mRecommendationTask.cancel(true);
        }
        return true;
    }

    private void updateRecommendations(List<Movie> movies) {
        try {
            // Create app channel if it doesn't exist
            Channel.Builder channelBuilder = new Channel.Builder();
            channelBuilder.setType(TvContractCompat.Channels.TYPE_PREVIEW)
                    .setDisplayName(getString(R.string.app_name))
                    .setAppLinkIntentUri(Uri.parse("firevisioniptv://channels"));

            // Add channel to TV Provider
            Uri channelUri = getContentResolver().insert(
                    TvContractCompat.Channels.CONTENT_URI,
                    channelBuilder.build().toContentValues());

            if (channelUri != null) {
                long channelId = ContentUris.parseId(channelUri);

                // Enable the channel
                ContentValues values = new ContentValues();
                values.put(TvContractCompat.Channels.COLUMN_BROWSABLE, 1);
                getContentResolver().update(
                        TvContractCompat.Channels.CONTENT_URI,
                        values,
                        TvContractCompat.Channels._ID + "=?",
                        new String[]{String.valueOf(channelId)});

                // Add programs to channel
                for (Movie movie : movies) {
                    PreviewProgram.Builder programBuilder = new PreviewProgram.Builder()
                            .setChannelId(channelId)
                            .setTitle(movie.getTitle())
                            .setDescription(movie.getDescription())
                            .setPosterArtUri(Uri.parse(movie.getCardImageUrl()))
                            .setIntentUri(buildProgramIntent(movie))
                            .setPreviewVideoUri(Uri.parse(movie.getVideoUrl()))
                            .setType(TvContractCompat.PreviewPrograms.TYPE_MOVIE);

                    getContentResolver().insert(
                            TvContractCompat.PreviewPrograms.CONTENT_URI,
                            programBuilder.build().toContentValues());
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException when updating recommendations. TV Provider permissions may be missing.", e);
        } catch (Exception e) {
            Log.e(TAG, "Error updating recommendations", e);
        }
    }

    private Uri buildProgramIntent(Movie movie) {
        return new Uri.Builder()
                .scheme("firevisioniptv")
                .authority("play")
                .appendPath("movie")
                .appendPath(String.valueOf(movie.getId()))
                .build();
    }

    public static void scheduleRecommendationUpdate(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.job.JobScheduler scheduler =
                    (android.app.job.JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

            android.app.job.JobInfo.Builder builder = new android.app.job.JobInfo.Builder(
                    1,
                    new android.content.ComponentName(context, RecommendationService.class));

            // Run job periodically
            builder.setPeriodic(3 * 60 * 60 * 1000) // 3 hours
                    .setRequiredNetworkType(android.app.job.JobInfo.NETWORK_TYPE_ANY)
                    .setPersisted(true);

            scheduler.schedule(builder.build());
        }
    }
}