package com.example.saju.musicplayer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Saju on 8/21/2018.
 */

public class SongAdapter extends BaseAdapter {

    List<Song> songs;
    LayoutInflater songInf;

    Context context;

    SongAdapter(Context context, List<Song> songs) {
        this.context = context;
        this.songs = songs;
        songInf = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return songs.size();
    }

    @Override
    public Object getItem(int i) {
        return songs.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        LinearLayout songLay = (LinearLayout) songInf.inflate(R.layout.song, viewGroup, false);

        TextView songTitleView = songLay.findViewById(R.id.song_title);
        TextView songArtistView = songLay.findViewById(R.id.song_artist);

        Song currentSong = songs.get(i);

        songTitleView.setText(currentSong.getTitle());
        songArtistView.setText(currentSong.getArtist());

        // set position as tag
        songLay.setTag(i);

        return songLay;
    }
}
