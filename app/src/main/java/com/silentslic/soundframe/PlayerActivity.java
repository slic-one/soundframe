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
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.database.Cursor;

import java.util.ArrayList;

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

    static MediaPlayer player = null;

    ArrayList<Song> songList;

    View songView;

    Button play;
    Button next;
    Button prev;

    SeekBar songSeekBar;

    Uri currentSongPath;
    int i = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        songList = readStorageForMusic();

        initializePlayer();
        initializeMusicList();
        initializeButtons();
    }

    @Override
    protected void onDestroy() {
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }

    public void initializePlayer() {
        if (player == null) {
            player = new MediaPlayer();
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);

            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                    nextTrack(next);
                }
            });

            currentSongPath = Uri.parse(songList.get(i).getPath());

            // service for background playback
            Intent intent = new Intent(this, PlayerService.class);
            startService(intent);
        }
    }

    public void initializeMusicList() {
        ((ListView)findViewById(R.id.songListView)).setAdapter(new SongsAdapter(this, songList));
        ((ListView)findViewById(R.id.songListView)).setSelector(R.drawable.spng_selector);
    }

    public void initializeButtons() {

        play = (Button)findViewById(R.id.btnPlay);
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playMusic(play);
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

        songSeekBar = (SeekBar) findViewById(R.id.songSeekBar);
    }

    public ArrayList<Song> readStorageForMusic() {
        ArrayList<Song> list = new ArrayList<>();

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
                    String duration = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));

                    list.add(new Song(song_id, songName, fullpath, duration));

                } while (cursor.moveToNext());

            }
            cursor.close();
        }
        return list;
    }

    public void songSelected(View view) {
        // reset background before setting new one
        if (songView != null) {
            //songView.setBackground(null);
        }
        songView = view;

        i = ((ListView)findViewById(R.id.songListView)).getPositionForView(view);

        currentSongPath = Uri.parse(String.valueOf(view.getTag()));
        playSong(currentSongPath);
    }

    public void playMusic(View view){

        if (player.isPlaying()) {
            player.pause();
            play.setBackground(getDrawable(R.drawable.ic_play_circle_fill_24dp));
        }
        else {
            playSong(currentSongPath);
        }
    }

    public void nextTrack(View view){
        if (++i >= songList.size())
            i = 0;

        songSelected(((ListView)findViewById(R.id.songListView)).getChildAt(i));
    }

    public void previousTrack(View view){
        if (--i <= -1)
            i = songList.size() - 1;

        songSelected(((ListView)findViewById(R.id.songListView)).getChildAt(i));
    }

    public void playSong(Uri songPath) {

        //songView.setBackgroundColor(getResources().getColor(R.color.selected_song_background));

        try {
            player.reset();
            player.setDataSource(getApplicationContext(), songPath);
            player.prepare();
            player.start();
            play.setBackground(getDrawable(R.drawable.ic_pause_circle_fill_24dp));
        }
        catch (Exception ex) {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("player", " i is " + i + " " + ex.getMessage());
        }
    }
}
