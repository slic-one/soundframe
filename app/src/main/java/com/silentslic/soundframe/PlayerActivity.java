package com.silentslic.soundframe;

import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class PlayerActivity extends AppCompatActivity {

    public static MediaPlayer player = null;

    ArrayList<Song> songList;

    // used for highlighting selected ListView item
    View songView;

    ListView songsListView;

    Button playbackButton;

    TextView songDurationTextView;
    TextView songProgressTextView;

    SeekBar songSeekBar;

    Uri currentSongPath;
    int i = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        if (songList == null)
            songList = readStorageForMusic();

        Log.i("songList size", String.valueOf(songList.size()));

        initializePlayer();
        initializeMusicList();
        initializeButtons();
        initializeSongProgressRunnable();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.app_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh_list:
                readStorageForMusic();
                return true;
            default:
                // Invoke the superclass to handle unrecognized action.
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }

    public void initializeSongProgressRunnable() {
        final Handler handler = new Handler();
        PlayerActivity.this.runOnUiThread(new Runnable() {

            String time;

            @Override
            public void run() {
                if (player != null && player.isPlaying()) {
                    songSeekBar.setProgress(player.getCurrentPosition() / 1000);

                    // TODO test performance, optimize if needed

                    int minutes = ((player.getCurrentPosition() / (1000 * 60)) % 60);
                    int seconds = ((player.getCurrentPosition() / 1000) % 60);

                    if (minutes < 10) {
                        if (seconds < 10) {
                            time = "0"+minutes + ":" + "0" + seconds;
                        }
                        else  {
                            time = "0"+minutes + ":" + seconds;
                        }
                    }
                    else {
                        if (seconds < 10) {
                            time = minutes + ":" + "0" + seconds;
                        }
                        else {
                            time = minutes + ":" + seconds;
                        }
                    }

                    songProgressTextView.setText(time);
                }
                handler.postDelayed(this, 1000);
            }
        });
    }

    public void initializePlayer() {
        if (player == null) {
            player = new MediaPlayer();
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);

            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                    nextTrack(findViewById(R.id.btnNext));
                }
            });

            currentSongPath = Uri.parse(songList.get(i).getPath());

            // starting service for background playback
            Intent intent = new Intent(this, PlayerService.class);
            startService(intent);
        }
    }

    public void initializeMusicList() {
        songsListView = ((ListView)findViewById(R.id.songListView));
        songsListView.setAdapter(new SongsAdapter(this, songList));
    }

    public void initializeButtons() {

        playbackButton = (Button)findViewById(R.id.btnPlay);
        playbackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playMusic(playbackButton);
            }
        });

        songDurationTextView = (TextView) findViewById(R.id.songDurationTextView);
        songProgressTextView = (TextView) findViewById(R.id.songProgressTextView);

        songSeekBar = (SeekBar) findViewById(R.id.songSeekBar);

        songSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            boolean wasPlaying = false;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (player != null && !player.isPlaying()) {
                    player.seekTo(progress * 1000);
                    if (wasPlaying)
                        player.start();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (player.isPlaying()) {
                    wasPlaying = true;
                    player.pause();
                }
                else {
                    wasPlaying = false;
                }
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
        try {
            Log.i("songSelected before", String.valueOf(i));

            songList.get(i).isSelected = false;
            songsListView.getAdapter().getView(i, songView, songsListView);

            songView = view;

            // nullPointer when view is recycled
            i = songsListView.getPositionForView(view);

            Log.i("songSelected after", String.valueOf(i));

            songList.get(i).isSelected = true;
            songsListView.getAdapter().getView(i, songView, songsListView);

            currentSongPath = Uri.parse(String.valueOf(view.getTag()));
            player.reset();
            songSeekBar.setProgress(0);
            playSong(currentSongPath);
        }
        catch (Exception ex) {
            Log.e("i", String.valueOf(i));
            ex.printStackTrace();
        }
    }

    public void playMusic(View view){

        if (player.isPlaying()) {
            player.pause();
            playbackButton.setBackground(getDrawable(R.drawable.ic_play_circle_fill_24dp));
        }
        else {
            player.start();
            playbackButton.setBackground(getDrawable(R.drawable.ic_pause_circle_fill_24dp));
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
        Log.i("nextTrack", String.valueOf(i));

        songsListView.setSelection(i);
        songsListView.scrollTo(0, i + 1);

        songSelected(getViewByPosition(i+1, songsListView));
    }

    public void previousTrack(View view){
        Log.i("previousTrack", String.valueOf(i));

        songsListView.scrollTo(0, i - 2);
        songsListView.setSelection(i - 2);

        songSelected(getViewByPosition(i-1, songsListView));
    }

    public void playSong(Uri songPath) {
        try {
            player.reset();
            player.setDataSource(getApplicationContext(), songPath);
            player.prepare();

            songSeekBar.setMax(player.getDuration() / 1000);

            // formatted duration of song for textView
            String time;
            int minutes = ((player.getDuration() / (1000 * 60)) % 60);
            int seconds = ((player.getDuration() / 1000) % 60);
            if (minutes < 10) {
                if (seconds < 10) {
                    time = "0"+minutes + ":" + "0" + seconds;
                }
                else  {
                    time = "0"+minutes + ":" + seconds;
                }
            }
            else {
                if (seconds < 10) {
                    time = minutes + ":" + "0" + seconds;
                }
                else {
                    time = minutes + ":" + seconds;
                }
            }
            songDurationTextView.setText(time);

            player.start();

            playbackButton.setBackground(getDrawable(R.drawable.ic_pause_circle_fill_24dp));
        }
        catch (Exception ex) {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("player", " i is " + i + " " + ex.getMessage());
        }
    }
}
