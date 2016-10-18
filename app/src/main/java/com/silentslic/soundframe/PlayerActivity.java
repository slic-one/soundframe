package com.silentslic.soundframe;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

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

    ListView songsListView;

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

        Log.i("songList size", String.valueOf(songList.size()));

        initializePlayer();
        initializeMusicList();
        initializeButtons();

        final Handler handler = new Handler();
        PlayerActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (player.isPlaying()) {
                    songSeekBar.setProgress(player.getCurrentPosition() / 1000);

                    Log.i("progress", String.valueOf(player.getCurrentPosition() / 1000));
                }
                handler.postDelayed(this, 1000);

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
        songsListView = ((ListView)findViewById(R.id.songListView));
        songsListView.setAdapter(new SongsAdapter(this, songList));
        songsListView.setSelector(R.drawable.spng_selector);
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

        songSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (player != null && !player.isPlaying()) {
                    player.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
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
        try {
            songView = view;

            i = songsListView.getPositionForView(view);

            currentSongPath = Uri.parse(String.valueOf(view.getTag()));
            player.reset();
            playSong(currentSongPath);
        }
        catch (Exception ex) {
            Log.i("i", String.valueOf(i));
            ex.printStackTrace();
        }
    }

    public void playMusic(View view){

        if (player.isPlaying()) {
            player.pause();
            play.setBackground(getDrawable(R.drawable.ic_play_circle_fill_24dp));
        }
        else {
            player.start();
            play.setBackground(getDrawable(R.drawable.ic_pause_circle_fill_24dp));
        }
    }

    public View getViewByPosition(int pos, ListView listView) {
        final int firstListItemPosition = listView.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

        if (pos < firstListItemPosition || pos > lastListItemPosition ) {
            return listView.getAdapter().getView(pos, null, listView);
        } else {
            final int childIndex = pos - firstListItemPosition;
            return listView.getChildAt(childIndex);
        }
    }

    public void nextTrack(View view){
        i++;
        if (i >= songList.size() - 1)
            i = 0;

        //songsListView.smoothScrollToPosition(i);
        songsListView.setSelection(i);
        songsListView.scrollTo(0, i+1);

        songSelected(getViewByPosition(i, songsListView));
    }

    public void previousTrack(View view){
        i--;
        if (i <= 0)
            i = songList.size() - 1;

        songsListView.setSelection(i - 1);
        songsListView.scrollTo(0, i - 1);

        songSelected(getViewByPosition(i, songsListView));
    }

    public void playSong(Uri songPath) {

        //songView.setBackgroundColor(getResources().getColor(R.color.selected_song_background));

        try {
            player.reset();
            player.setDataSource(getApplicationContext(), songPath);
            player.prepare();

            songSeekBar.setMax(player.getDuration() / 1000);
            Log.i("setMax", String.valueOf(songSeekBar.getMax()));
            songSeekBar.setProgress(0);

            player.start();

            play.setBackground(getDrawable(R.drawable.ic_pause_circle_fill_24dp));
        }
        catch (Exception ex) {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("player", " i is " + i + " " + ex.getMessage());
        }
    }
}
