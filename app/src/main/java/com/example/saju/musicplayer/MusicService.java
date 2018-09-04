package com.example.saju.musicplayer;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.Random;

/**
 * Created by Saju on 8/21/2018.
 */

public class MusicService extends Service
        implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {


    final static int NOTIFY_ID = 1;
    MediaPlayer player;
    List<Song> songs;
    int songPos;
    IBinder musicBinder = new MusicBinder();
    String songTitle = "";
    boolean shuffle = false;
    Random rand;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return musicBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        player.stop();
        player.release();
        return false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        songPos = 0;
        player = new MediaPlayer();

        initMusicPlayer();

        rand = new Random();
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
    }

    public void initMusicPlayer() {
        player.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);

        player.setOnPreparedListener(this);
        player.setOnErrorListener(this);
        player.setOnCompletionListener(this);
    }

    public void setList(List<Song> songList) {
        this.songs = songList;
    }

    public void playSong() {

        player.reset();
        Song playSong = songs.get(songPos);
        songTitle = playSong.getTitle();
        long currSong = playSong.getId();

        Uri traceUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, currSong);


        try {
            player.setDataSource(getApplicationContext(), traceUri);
        } catch (IOException e) {
            Log.d("exception", "Exception Occured ", e);
        }

        player.prepareAsync();
    }

    public void setSong(int songPos) {
        this.songPos = songPos;
    }

    public int getPos() {
        return player.getCurrentPosition();
    }

    public int getDur() {
        return player.getDuration();
    }

    public boolean isPng() {
        return player.isPlaying();
    }

    public void pausePlayer() {
        player.pause();
    }

    public void seek(int pos) {
        player.seekTo(pos);
    }

    public void go() {
        player.start();
    }

    public void playPrev() {

        if (shuffle) {
            int newSong = songPos;
            while (newSong == songPos) {
                newSong = rand.nextInt(songs.size());
            }
            songPos = newSong;
        } else {
            songPos--;
            if (songPos < 0)
                songPos = songs.size() - 1;
        }

        playSong();
    }

    public void playNext() {

        if (shuffle) {
            int newSong = songPos;
            while (newSong == songPos) {
                newSong = rand.nextInt(songs.size());
            }
            songPos = newSong;
        } else {
            songPos++;
            if (songPos >= songs.size())
                songPos = 0;
        }

        playSong();
    }

    public void setShuffle() {
        if (shuffle) {
            shuffle = false;
        } else {
            shuffle = true;
        }
    }

    public boolean isShuffle() {
        return shuffle;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {

        mediaPlayer.start();

        Intent notIntent = new Intent(this, MainActivity.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendInt = PendingIntent.getActivity(this, 0, notIntent, PendingIntent.FLAG_UPDATE_CURRENT);


        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentIntent(pendInt)
                .setSmallIcon(R.drawable.play)
                .setTicker(songTitle)
                .setOngoing(true)
                .setContentTitle("Playing")
                .setContentText(songTitle);

        Notification not = builder.build();
        startForeground(NOTIFY_ID, not);


    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        mediaPlayer.reset();
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {

        if (player.getCurrentPosition() > 0) {
            mediaPlayer.reset();
            playNext();
        }
    }

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }
}
