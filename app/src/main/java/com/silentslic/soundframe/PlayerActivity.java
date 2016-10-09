package com.silentslic.soundframe;

import android.app.Activity;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

public class PlayerActivity extends Activity {

    MediaPlayer player;

    ArrayList<String> songNameList;
    ArrayList<String> songPathList;

    Button play;
    Button pause;
    Button next;
    Button prev;

    Uri currentSongPath;
    int i = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        player = new MediaPlayer();
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);

        songNameList = new ArrayList<>();
        songPathList = new ArrayList<>();

        getAllSongs();
        initializeMusicList();

        initializeButtons();

        currentSongPath = Uri.parse(songPathList.get(i));

//        player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
//            @Override
//            public void onPrepared(MediaPlayer mp) {
//                mp.start();
//            }
//        });

        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                nextTrack(next);
            }
        });

        try {
            player.setDataSource(getApplicationContext(), currentSongPath);
            player.prepare();
        }
        catch (Exception ex) {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("player", "" + ex.getMessage());
        }
    }

    // TODO seekbar, select song from list

    public void initializeButtons() {

        play = (Button)findViewById(R.id.btnPlay);
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playMusic(play);
            }
        });
        pause = (Button)findViewById(R.id.btnPause);
        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pauseMusic(pause);
            }
        });
        next = (Button)findViewById(R.id.btnNext);
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nextTrack(next);
            }
        });
        prev = (Button)findViewById(R.id.btnPrev);
        prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                previousTrack(prev);
            }
        });
    }

    @Override
    protected void onStop() {
        if (player != null) {
            player.release();
            player = null;
        }
        super.onStop();
    }

    public void getAllSongs() {

        Uri allsongsuri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        Cursor cursor = getContentResolver().query(allsongsuri, null, null, null, selection);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    String songName = "" + cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
                    //skip every audio file except .mp3 (My way to do it)
                    if (!songName.substring(songName.length()-4).equals(".mp3"))
                        continue;

                    int song_id = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID));

                    String fullpath = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                    String Duration = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));

                    songNameList.add(songName);
                    songPathList.add(fullpath);

                } while (cursor.moveToNext());

            }
            cursor.close();
        }
    }

    public void initializeMusicList() {
        ((ListView)findViewById(R.id.songListView)).setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, songNameList));
    }


    //TODO better button icon switch
    public void playMusic(View view){
        player.start();
        play.setVisibility(View.GONE);
        pause.setVisibility(View.VISIBLE);
    }

    public void pauseMusic(View view){
        player.pause();
        pause.setVisibility(View.GONE);
        play.setVisibility(View.VISIBLE);
    }

    public void nextTrack(View view){
        if (++i >= songPathList.size() - 1)
            i = 0;

        currentSongPath = Uri.parse(songPathList.get(i));
        try {
            player.reset();
            player.setDataSource(getApplicationContext(), currentSongPath);
            player.prepare();
            player.start();
        }
        catch (Exception ex) {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("player", " i is " + i + " " + ex.getMessage());
            Log.i("currentSongPath", currentSongPath.toString());
        }
    }

    public void previousTrack(View view){
        if (--i <= 0)
            i = songPathList.size() - 1;

        currentSongPath = Uri.parse(songPathList.get(i));
        try {
            player.reset();
            player.setDataSource(getApplicationContext(), currentSongPath);
            player.prepare();
            player.start();
        }
        catch (Exception ex) {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("player", " i is " + i + " " + ex.getMessage());
        }
    }


    /*
        http://developer.android.com/guide/topics/media/mediaplayer.html
        http://code.tutsplus.com/tutorials/create-a-music-player-on-android-project-setup--mobile-22764
        http://code.tutsplus.com/tutorials/create-a-music-player-on-android-song-playback--mobile-22778
        http://code.tutsplus.com/tutorials/create-a-music-player-on-android-user-controls--mobile-22787
    */
}
