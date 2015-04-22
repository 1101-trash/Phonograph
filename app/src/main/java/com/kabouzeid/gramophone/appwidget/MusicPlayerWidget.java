package com.kabouzeid.gramophone.appwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.widget.RemoteViews;

import com.kabouzeid.gramophone.R;
import com.kabouzeid.gramophone.helper.MusicPlayerRemote;
import com.kabouzeid.gramophone.model.Song;
import com.kabouzeid.gramophone.service.MusicService;
import com.kabouzeid.gramophone.ui.activities.MusicControllerActivity;
import com.kabouzeid.gramophone.util.MusicUtil;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

public class MusicPlayerWidget extends AppWidgetProvider {
    private static RemoteViews widgetLayout;
    private static Future albumArtTask;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        updateWidgets(context, MusicPlayerRemote.getCurrentSong(), MusicPlayerRemote.isPlaying());
        for (int widgetId : appWidgetIds) {
            appWidgetManager.updateAppWidget(widgetId, widgetLayout);
        }
    }

    public static void updateWidgets(final Context context, final Song song, boolean isPlaying) {
        widgetLayout = new RemoteViews(context.getPackageName(), R.layout.music_player_widget);
        linkButtons(context, widgetLayout);
        if (song.id != -1) {
            widgetLayout.setTextViewText(R.id.song_title, song.title);
        }
        updateWidgetsPlayState(context, isPlaying);
        loadAlbumArt(context, song);
    }

    public static void updateWidgetsPlayState(final Context context, boolean isPlaying) {
        if (widgetLayout == null)
            widgetLayout = new RemoteViews(context.getPackageName(), R.layout.music_player_widget);
        int playPauseRes = isPlaying ? R.drawable.ic_pause_black_36dp : R.drawable.ic_play_arrow_black_36dp;
        widgetLayout.setImageViewResource(R.id.button_toggle_play_pause, playPauseRes);
        updateWidgets(context);
    }

    private static void updateWidgets(final Context context) {
        AppWidgetManager man = AppWidgetManager.getInstance(context);
        int[] ids = man.getAppWidgetIds(
                new ComponentName(context, MusicPlayerWidget.class));
        for (int widgetId : ids) {
            man.updateAppWidget(widgetId, widgetLayout);
        }
    }

    private static void loadAlbumArt(final Context context, final Song song) {
        if (albumArtTask != null) albumArtTask.cancel();
        albumArtTask = Ion.with(context)
                .load(MusicUtil.getAlbumArtUri(song.albumId).toString())
                .noCache()
                .asBitmap()
                .setCallback(new FutureCallback<Bitmap>() {
                    @Override
                    public void onCompleted(Exception e, Bitmap result) {
                        if (result != null) setAlbumArt(context, result);
                        else resetAlbumArt(context);
                    }
                });
    }

    private static void resetAlbumArt(final Context context) {
        widgetLayout.setImageViewResource(R.id.album_art, R.drawable.default_album_art);
        updateWidgets(context);
    }

    private static void setAlbumArt(final Context context, final Bitmap albumArt) {
        widgetLayout.setImageViewBitmap(R.id.album_art, albumArt);
        updateWidgets(context);
    }

    private static void linkButtons(final Context context, final RemoteViews views) {
        views.setOnClickPendingIntent(R.id.album_art, retrievePlaybackActions(context, 0));
        views.setOnClickPendingIntent(R.id.button_toggle_play_pause, retrievePlaybackActions(context, 1));
        views.setOnClickPendingIntent(R.id.button_next, retrievePlaybackActions(context, 2));
        views.setOnClickPendingIntent(R.id.button_prev, retrievePlaybackActions(context, 3));
    }

    private static PendingIntent retrievePlaybackActions(final Context context, final int which) {
        Intent action;
        PendingIntent pendingIntent;
        final ComponentName serviceName = new ComponentName(context, MusicService.class);
        switch (which) {
            case 0:
                action = new Intent(context, MusicControllerActivity.class);
                pendingIntent = PendingIntent.getActivity(context, 0, action, 0);
                return pendingIntent;
            case 1:
                action = new Intent(MusicService.ACTION_TOGGLE_PLAYBACK);
                action.setComponent(serviceName);
                pendingIntent = PendingIntent.getService(context, 1, action, 0);
                return pendingIntent;
            case 2:
                action = new Intent(MusicService.ACTION_SKIP);
                action.setComponent(serviceName);
                pendingIntent = PendingIntent.getService(context, 2, action, 0);
                return pendingIntent;
            case 3:
                action = new Intent(MusicService.ACTION_REWIND);
                action.setComponent(serviceName);
                pendingIntent = PendingIntent.getService(context, 3, action, 0);
                return pendingIntent;
        }
        return null;
    }
}

