package com.silentslic.soundframe;

import android.content.Context;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 *  Custom Adapter for music list ListView
 */

class SongsAdapter extends ArrayAdapter<Song> {

    SongsAdapter(Context context, ArrayList<Song> songs) {
        super(context, 0, songs);
    }

    private int fontColor = PreferenceManager.getDefaultSharedPreferences(getContext()).getInt("fontColor", ContextCompat.getColor(getContext(), R.color.song_text));
    private int selectedSongColor = PreferenceManager.getDefaultSharedPreferences(getContext()).getInt("selectedSongColor", ContextCompat.getColor(getContext(), R.color.song_text_selected));

    private static final int NOT_SELECTED = -1;
    private int selectedPos = NOT_SELECTED;

    private Typeface font = Typeface.createFromAsset(getContext().getAssets(), "fonts/digit.ttf");

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        // Get the data item for this position
        Song song = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.song_name, parent, false);
        }

        TextView songName = (TextView) convertView.findViewById(R.id.songNameLine);
        songName.setText(song.getName());
        songName.setTag(song.getPath());

        songName.setTypeface(font);

        if (position == selectedPos) {
            songName.setTextColor(selectedSongColor);
        }
        else {
            songName.setTextColor(fontColor);
        }

        return convertView;
    }

    void setFontColor(int color) {this.fontColor = color;}

    void setSelectedSongColor(int color) {this.selectedSongColor = color;}

    void setSelection(int position) {
        selectedPos = position;
        notifyDataSetChanged();
    }
}
