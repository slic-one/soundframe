package com.silentslic.soundframe;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
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
import java.util.Collections;
import java.util.Random;

public class PlayerActivity extends AppCompatActivity {

    public static MediaPlayer player = null;

    ArrayList<Song> songList;

    ListView songsListView;

    Button playbackButton;

    TextView songDurationTextView;
    TextView songProgressTextView;

    SeekBar songSeekBar;

    static String formattedCurrentSongDuration = "";

    Uri currentSongPath;
    int i = 0;

    SongsAdapter adapter;

    MusicIntentReceiver receiver;

    boolean isRepeatOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        if (songList == null)
            songList = readStorageForMusic();

        Log.i("songList size", String.valueOf(songList.size()));

        if (player == null)
            initializePlayer();
        if (songsListView == null)
            initializeMusicList();
        initializeButtons();
        initializeSongProgressRunnable();

        //select first song from the list
        adapter.setSelection(0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.app_menu, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
        builder1.setMessage("Quit " + getString(R.string.app_name) + "?");

        builder1.setPositiveButton(
                "Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        PlayerActivity.this.finish();
                    }
                });

        builder1.setNeutralButton(
                "No",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                }
        );

        AlertDialog alert11 = builder1.create();
        alert11.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh_list:
                readStorageForMusic();
                return true;
            case R.id.action_toggle_seek_bar:
                View seekBarLine = findViewById(R.id.seekBarLine);
                if (seekBarLine.getVisibility() == View.VISIBLE)
                    seekBarLine.setVisibility(View.GONE);
                else
                    seekBarLine.setVisibility(View.VISIBLE);
                return true;
            case R.id.action_toggle_action_bar:
                ActionBar actionBar = getSupportActionBar();

                if (actionBar != null) {
                    if (actionBar.isShowing())
                        actionBar.hide();
                    else
                        actionBar.show();
                    return true;
                }
                return false;
            case R.id.action_toggle_adv_controls:
                View shuffle = findViewById(R.id.btnShuffle);
                View repeat = findViewById(R.id.btnRepeat);
                // It's enough to get the state of one of the buttons
                if (shuffle.getVisibility() == View.VISIBLE) {
                    shuffle.setVisibility(View.GONE);
                    repeat.setVisibility(View.GONE);
                }
                else {
                    shuffle.setVisibility(View.VISIBLE);
                    repeat.setVisibility(View.VISIBLE);
                }
                return true;
            default:
                // Invoke the superclass to handle unrecognized action.
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(receiver, filter);
        super.onResume();
    }

    @Override
    protected void onStop() {
        try {
            unregisterReceiver(receiver);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (isFinishing()) {
            if (player != null) {
                player.release();
                player = null;
            }
        }
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
                else if (player != null && !player.isPlaying()) {
                    if (playbackButton.getBackground() != getDrawable(R.drawable.ic_play_circle_fill_24dp)) {
                        playbackButton.setBackground(getDrawable(R.drawable.ic_play_circle_fill_24dp));
                    }
                }
                handler.postDelayed(this, 1000);
            }
        });
    }

    public void initializePlayer() {
        if (player == null) {
            player = new MediaPlayer();
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);

            player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    Log.e("player", "Error: " + what + ", " + extra);
                    return true;
                }
            });

            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer mp)
                {
                    if (!isRepeatOn)
                        nextSong(findViewById(R.id.btnNext));
                    else {
                        player.reset();
                        songSeekBar.setProgress(0);
                        playSong(currentSongPath);
                    }
                }
            });

            currentSongPath = Uri.parse(songList.get(i).getPath());

            // starting service for background playback
            Intent intent = new Intent(this, PlayerService.class);

            //register receiver
            receiver = new MusicIntentReceiver();

            startService(intent);
        }
    }

    public void initializeMusicList() {
        songsListView = ((ListView)findViewById(R.id.songListView));
        adapter = new SongsAdapter(this, songList);
        songsListView.setAdapter(adapter);
    }

    public void initializeButtons() {

        if (playbackButton == null) {
            playbackButton = (Button) findViewById(R.id.btnPlay);
        }

        songDurationTextView = (TextView) findViewById(R.id.songDurationTextView);
        songProgressTextView = (TextView) findViewById(R.id.songProgressTextView);


        if (player.isPlaying()) {
            playbackButton.setBackground(getDrawable(R.drawable.ic_pause_circle_fill_24dp));
            songDurationTextView.setText(formattedCurrentSongDuration);
        }

        if (songSeekBar == null) {
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
                    } else {
                        wasPlaying = false;
                    }
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
        }
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

    public void songSelected(View view) {
            i = songsListView.getPositionForView(view);

            adapter.setSelection(i);

            currentSongPath = Uri.parse(String.valueOf(view.getTag()));
            playSong(currentSongPath);
    }

    public void playPause(View view){

        if (player.isPlaying()) {
            player.pause();
            playbackButton.setBackground(getDrawable(R.drawable.ic_play_circle_fill_24dp));
        }
        else {
            player.start();
            playbackButton.setBackground(getDrawable(R.drawable.ic_pause_circle_fill_24dp));
        }
    }

    public void nextSong(View view){
        Log.i("nextSong", String.valueOf(i));

        if (i != songList.size()-1) // if not the last element in the list
            i++;
        else
            i = 0;

        adapter.setSelection(i);

        currentSongPath = Uri.parse(String.valueOf(getViewByPosition(i, songsListView).getTag()));
        playSong(currentSongPath);
    }

    public void previousSong(View view){
        Log.i("previousSong", String.valueOf(i));

        if (i != 0) // if not the first element in the list
            i--;
        else
            i = songList.size()-1;

        adapter.setSelection(i);

        currentSongPath = Uri.parse(String.valueOf(getViewByPosition(i, songsListView).getTag()));
        playSong(currentSongPath);
    }

    public void playSong(Uri songPath) {
        try {
            player.reset();
            player.setDataSource(getApplicationContext(), songPath);
            player.prepare();

            songSeekBar.setProgress(0);
            songSeekBar.setMax(player.getDuration() / 1000);

            // formatted duration of song for textView
            int minutes = ((player.getDuration() / (1000 * 60)) % 60);
            int seconds = ((player.getDuration() / 1000) % 60);
            if (minutes < 10) {
                if (seconds < 10) {
                    formattedCurrentSongDuration = "0"+minutes + ":" + "0" + seconds;
                }
                else  {
                    formattedCurrentSongDuration = "0"+minutes + ":" + seconds;
                }
            }
            else if (seconds < 10) {
                formattedCurrentSongDuration = minutes + ":" + "0" + seconds;
            }
            else {
                formattedCurrentSongDuration = minutes + ":" + seconds;
            }
            songDurationTextView.setText(formattedCurrentSongDuration);

            player.start();



            playbackButton.setBackground(getDrawable(R.drawable.ic_pause_circle_fill_24dp));
        }
        catch (Exception ex) {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("player", " i is " + i + " " + ex.getMessage());
        }
    }

    public void repeat(View view) {
        if (view.getTag().equals("inactive")) {
            isRepeatOn = true;

            view.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.player_controls)));
            view.setTag("active");
        }
        else {
            isRepeatOn = false;

            view.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.player_controls_inactive)));
            view.setTag("inactive");
        }
    }

    public void shuffle(View view) {
        if (view.getTag().equals("inactive")) {

            // shuffle music list and re-initialize
            Collections.shuffle(songList, new Random(System.nanoTime()));
            initializeMusicList();
            
            //TODO CHECK
            adapter.setSelection(i);

            view.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.player_controls)));
            view.setTag("active");
        }
        else {
            songList = readStorageForMusic();
            initializeMusicList();

            view.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.player_controls_inactive)));
            view.setTag("inactive");
        }
    }
}
