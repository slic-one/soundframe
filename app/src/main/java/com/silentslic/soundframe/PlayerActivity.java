package com.silentslic.soundframe;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
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

    public class PlayerService extends Service implements AudioManager.OnAudioFocusChangeListener {
        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN);

            if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                Log.e("AUDIOFOCUS", "could not get audio focus.");
            }
            return super.onStartCommand(intent, flags, startId);
        }

        @Override
        public void onAudioFocusChange(int focusChange) {
            switch(focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    // resume playback
                    if (player == null) initializePlayer();
                    else if (!player.isPlaying()) player.start();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    // Lost focus for an unbounded amount of time: stop playback and release media player
                    if(player.isPlaying()) player.stop();
                    player.release();
                    player = null;
                    break;
                case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
                    // Lost focus for a short time, but we have to stop playback
                    if(player.isPlaying()) player.pause();
                    break;
            }
        }

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }


    public MediaPlayer player;

    ArrayList<String> songNameList;
    ArrayList<String> songPathList;

    Button play;
    Button pause;
    Button next;
    Button prev;

    Uri currentSongPath;
    int i = 0;

    // TODO notification

    // TODO fix rotation
    //calls onCreate and onResume methods

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        initializePlayer();

        songNameList = new ArrayList<>();
        songPathList = new ArrayList<>();

        getAllSongs();
        initializeMusicList();

        initializeButtons();

        currentSongPath = Uri.parse(songPathList.get(i));

//        player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
//            @Override
//            public void onPrepared(MediaPlayer mp) {
//                if (mp != null)
//                    mp.start();
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

        Intent intent = new Intent(this, PlayerService.class);
        startService(intent);
    }

    // TODO seekbar, select song from list

    public void initializePlayer() {
        player = new MediaPlayer();
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
    }

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
    protected void onDestroy() {
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
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
                    //Log.i("Duration", String.valueOf(song_id));

                    String fullpath = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                    String Duration = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                    //Log.i("Duration", Duration);

                    songNameList.add(songName);
                    songPathList.add(fullpath);

                } while (cursor.moveToNext());

            }
            cursor.close();
        }
    }

    public void initializeMusicList() {
        ((ListView)findViewById(R.id.songListView)).setAdapter(new ArrayAdapter<>(this, R.layout.song_name, songNameList));
    }

    public void songSelected(View view) {
        Toast.makeText(this, "Some song selected", Toast.LENGTH_LONG).show();
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
