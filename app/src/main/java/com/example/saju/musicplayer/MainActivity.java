package com.example.saju.musicplayer;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListView;
import android.widget.MediaController.MediaPlayerControl;
import android.widget.Toast;

import com.example.saju.musicplayer.MusicService.MusicBinder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class MainActivity extends AppCompatActivity implements MediaPlayerControl {

    List<Song> songList;
    ListView songListView;

    MusicService musicService;
    Intent playIntent;
    boolean musicBound = false;

    MusicController controller;

    Handler handler = new Handler();
    ServiceConnection musicConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            MusicBinder binder = (MusicBinder) service;
            // get service
            musicService = binder.getService();
            musicService.setList(songList);
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            musicBound = false;
        }
    };
    private boolean paused = false;
    private boolean playbackPaused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

                // MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE is an
                // app-defined int constant

                return;
            }
        }

        getSupportActionBar().show();


        songListView = (ListView) findViewById(R.id.songListViewId);

        songList = new ArrayList<>();

        getSongList();

        // sort the song based on Title
        Collections.sort(songList, new Comparator<Song>() {
            @Override
            public int compare(Song song1, Song song2) {
                return song1.getTitle().compareToIgnoreCase(song2.getTitle());
            }
        });


        SongAdapter adapter = new SongAdapter(this, songList);
        songListView.setAdapter(adapter);


        setController();

        songListView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (controller != null) {
                    controller.show();
                }
                return false;
            }
        });


    }

    @Override
    protected void onStart() {
        super.onStart();
        if (playIntent == null) {
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    @Override
    protected void onDestroy() {
        stopService(playIntent);
        musicService = null;
        super.onDestroy();
    }

    private void getSongList() {

        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if (musicCursor != null && musicCursor.moveToFirst()) {

            // get Columns
            int titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);


            // add songs to the list

            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);

                songList.add(new Song(thisId, thisTitle, thisArtist));
            } while (musicCursor.moveToNext());
        }

    }

    // song picked listener
    public void songPicked(View view) {
        musicService.setSong(Integer.parseInt(view.getTag().toString()));
        musicService.playSong();
        if (playbackPaused) {
            setController();
            playbackPaused = false;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInf = getMenuInflater();
        menuInf.inflate(R.menu.main, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.action_shuffle) {
            musicService.setShuffle();

            if (musicService.isShuffle()) {
                item.setIcon(R.drawable.ic_shuffle_on);
                Toast.makeText(MainActivity.this, "Shuffle On", Toast.LENGTH_SHORT).show();
            } else {
                item.setIcon(R.drawable.ic_shuffle_off);
                Toast.makeText(MainActivity.this, "Shuffle Off", Toast.LENGTH_SHORT).show();

            }

        }

        if (item.getItemId() == R.id.action_end) {
            stopService(playIntent);
            musicService = null;
            System.exit(0);
        }
        return super.onOptionsItemSelected(item);
    }


    public void setController() {

        controller = new MusicController(this);

        controller.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playNext();
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playPrev();
            }
        });

        controller.setMediaPlayer(this);
        controller.setAnchorView(findViewById(R.id.songListViewId));

        handler.post(new Runnable() {
            @Override
            public void run() {
                controller.setEnabled(true);
                //controller.show();
            }
        });


    }

    private void playPrev() {
        musicService.playPrev();
        if (playbackPaused) {
            setController();
            playbackPaused = false;
        }
        controller.show(0);
    }

    private void playNext() {
        musicService.playNext();
        if (playbackPaused) {
            setController();
            playbackPaused = false;
        }
        controller.show(0);
    }


    @Override
    protected void onPause() {
        super.onPause();
        paused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (paused) {
            setController();
            paused = false;
        }
    }

    @Override
    protected void onStop() {
        controller.hide();
        super.onStop();
    }

    @Override
    public void start() {
        musicService.go();
    }

    @Override
    public void pause() {
        playbackPaused = true;
        musicService.pausePlayer();
    }

    @Override
    public int getDuration() {
        if (musicService != null && musicBound && musicService.isPng()) {
            return musicService.getDur();
        }
        return 0;
    }

    @Override
    public int getCurrentPosition() {
        if (musicService != null && musicBound && musicService.isPng()) {
            return musicService.getPos();
        }
        return 0;
    }

    @Override
    public void seekTo(int i) {
        musicService.seek(i);
    }

    @Override
    public boolean isPlaying() {
        if (musicService != null && musicBound) {
            return musicService.isPng();
        }
        return false;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }
}
