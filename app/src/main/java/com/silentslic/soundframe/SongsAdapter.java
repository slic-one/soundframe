package com.silentslic.soundframe;

import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    private static final int NOT_SELECTED = -1;
    private int selectedPos = NOT_SELECTED;

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

        songName.setTypeface(Typeface.createFromAsset(getContext().getAssets(), "fonts/minisystem.ttf"));

        if (position == selectedPos) {
            //songName.setBackgroundColor(getContext().getResources().getColor(R.color.selected_song_background));
            songName.setTextColor(getContext().getResources().getColor(R.color.song_text_selected));
        }
        else {
            //songName.setBackgroundColor(getContext().getResources().getColor(R.color.list_background));
            songName.setTextColor(getContext().getResources().getColor(R.color.song_text));
        }

        return convertView;
    }

    void setSelection(int position) {
        selectedPos = position;
        notifyDataSetChanged();
    }
}
