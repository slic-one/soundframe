package com.silentslic.soundframe;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.ColorInt;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.jrummyapps.android.colorpicker.ColorPickerDialog;
import com.jrummyapps.android.colorpicker.ColorPickerDialogListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class PlayerActivity extends AppCompatActivity implements ColorPickerDialogListener {

    public static MediaPlayer player = null;

    ArrayList<Song> songList;

    ListView songsListView;
    Button playbackButton;
    TextView songDurationTextView;
    TextView songProgressTextView;
    Drawable pauseDrawable;
    Drawable playDrawable;
    SeekBar songSeekBar;

    static String formattedCurrentSongDuration = "";

    Uri currentSongPath;
    int i = 0;

    SongsAdapter adapter;
    MusicIntentReceiver receiver;

    boolean isRepeatOn = false;

    private SharedPreferences sharedPreferences;

    NotificationManager notificationManager;

    UIUtil uiUtil;

    String[] drawerItems;
    ListView drawerList;
    DrawerLayout drawerLayout;

    EditText searchBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        sharedPreferences  = PreferenceManager.getDefaultSharedPreferences(this);
        i = sharedPreferences.getInt("i", 0);

        Log.i("i", "loaded " + i);

        if (songList == null)
            songList = readStorageForMusic();

        Log.i("songList size", String.valueOf(songList.size()));

        if (player == null)
            initializePlayer();
        if (songsListView == null)
            initializeMusicList();
        initializeButtons();
        initializeSongProgressRunnable();
        initializeNavigationDrawer();

        loadPreferences();

        createNotification();

        //select last playing song
        adapter.setSelection(i);

        uiUtil = new UIUtil(this, sharedPreferences);

        try {
            player.reset();
            player.setDataSource(getApplicationContext(), currentSongPath);
            player.prepare();
        }catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private class DigitalTextViewAdapter extends ArrayAdapter<String> {
        DigitalTextViewAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull String[] objects) {
            super(context, resource, objects);
        }

        private Typeface font = Typeface.createFromAsset(getContext().getAssets(), "fonts/digit.ttf");

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.digital_textvew, parent, false);
            }
            TextView text = (TextView) convertView.findViewById(R.id.digital_text);
            text.setTypeface(font);
            return super.getView(position, convertView, parent);
        }
    }

    private void initializeNavigationDrawer() {
        drawerItems = getResources().getStringArray(R.array.drawer_items);
        drawerList = (ListView) findViewById(R.id.left_drawer);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

//        drawerLayout.setScrimColor(Color.argb(200, 0, 0, 0));

//        drawerList.setAdapter(new ArrayAdapter<>(this, R.layout.digital_textvew,drawerItems));
        drawerList.setAdapter(new DigitalTextViewAdapter(this, R.layout.digital_textvew, drawerItems));

        drawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        uiUtil.toggleActionBar();
                        break;
                    case 1:
                        uiUtil.toggleSeekBar();
                        break;
                    case 2:
                        uiUtil.toggleAdditionalButtons();
                        break;
                    case 3:
                        ColorPickerDialog.newBuilder().setColor(ContextCompat.getColor(PlayerActivity.this, R.color.song_text)).show(PlayerActivity.this);
                        break;
                    case 4:
                        Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
                        getIntent.setType("image/*");

                        Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        pickIntent.setType("image/*");

                        Intent chooserIntent = Intent.createChooser(getIntent, "Select Image");
                        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] {pickIntent});

                        startActivityForResult(chooserIntent, 9);
                        break;
                    case 5:
                        uiUtil.startShutdownTimer();
                        break;
                    case 6:
                        // TODO equalizer

                        break;
                    case 7:
                        startActivity(new Intent(PlayerActivity.this, SettingsActivity.class));
                        break;
                    default:
                        Toast.makeText(PlayerActivity.this, "default", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        sharedPreferences.edit().putInt("i", i).apply();
    }

    private void loadPreferences() {
        if (sharedPreferences.getInt("seekBarVisibility", View.VISIBLE) == View.GONE)
            findViewById(R.id.seekBarLine).setVisibility(View.GONE);
        if (sharedPreferences.getInt("shuffleRepeatVisibility", View.GONE) == View.VISIBLE) {
            findViewById(R.id.btnShuffle).setVisibility(View.VISIBLE);
            findViewById(R.id.btnRepeat).setVisibility(View.VISIBLE);
        }
        if (!sharedPreferences.getBoolean("actionBarShowing", true) && (getSupportActionBar() != null))
            getSupportActionBar().hide();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.app_menu, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (searchBar.getVisibility() == View.VISIBLE)
            searchBar.setVisibility(View.GONE);
        else
            uiUtil.showQuitDialog();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                uiUtil.toggleViewVisibility(searchBar);
                return true;
            default:
                // Invoke the superclass to handle unrecognized action.
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (isFinishing()) {
            try {
                unregisterReceiver(receiver);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
            notificationManager.cancel(41304130);
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
            int minutes;
            int seconds;

            @Override
            public void run() {
                if (player != null && player.isPlaying()) {
                    songSeekBar.setProgress(player.getCurrentPosition() / 1000);

                    minutes = ((player.getCurrentPosition() / (1000 * 60)) % 60);
                    seconds = ((player.getCurrentPosition() / 1000) % 60);

                    if (minutes < 10) {
                        if (seconds < 10) time = "0"+minutes + ":" + "0" + seconds;
                        else time = "0"+minutes + ":" + seconds;
                    }
                    else {
                        if (seconds < 10) time = minutes + ":" + "0" + seconds;
                        else time = minutes + ":" + seconds;
                    }

                    songProgressTextView.setText(time);
                }

                // TODO redo this
//                else if (player != null && !player.isPlaying())
//                    if (playbackButton.getBackground() != getDrawable(R.drawable.ic_play_circle_fill_24dp))
//                        playbackButton.setBackground(getDrawable(R.drawable.ic_play_circle_fill_24dp));
//                else if (player != null && player.isPlaying())
//                    if (playbackButton.getBackground() != getDrawable(R.drawable.ic_pause_circle_fill_24dp))
//                        playbackButton.setBackground(getDrawable(R.drawable.ic_pause_circle_fill_24dp));

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
            startService(intent);

            //register receiver
            receiver = new MusicIntentReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_HEADSET_PLUG);
            registerReceiver(receiver, filter);

//            try {
//                player.setDataSource(getApplicationContext(), currentSongPath);
//                player.prepare();
//            }
//            catch (Exception ex) {
//                ex.printStackTrace();
//            }
        }
    }

    public void initializeMusicList() {
        songsListView = ((ListView)findViewById(R.id.songListView));
        adapter = new SongsAdapter(this, songList);
        songsListView.setAdapter(adapter);
    }

    public void initializeButtons() {
        playDrawable = getDrawable(R.drawable.ic_play_circle_fill_24dp);
        pauseDrawable = getDrawable(R.drawable.ic_pause_circle_fill_24dp);

        if (playbackButton == null) {
            playbackButton = (Button) findViewById(R.id.btnPlay);
        }

        songDurationTextView = (TextView) findViewById(R.id.songDurationTextView);
        songProgressTextView = (TextView) findViewById(R.id.songProgressTextView);

        searchBar = (EditText)findViewById(R.id.song_search);
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = searchBar.getText().toString();
                adapter.getFilter().filter(text);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        if (player.isPlaying()) {
            playbackButton.setBackground(pauseDrawable);
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
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }
    }

    public void createNotification() {
        NotificationCompat.Builder mBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentText("Song - Name");

        Intent intent = new Intent(this, PlayerActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setContentIntent(pendingIntent);

        notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(41304130, mBuilder.build());
    }

    // TODO fix refresh issue
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
            playbackButton.setBackground(playDrawable);
        }
        else {
            player.start();
            playbackButton.setBackground(pauseDrawable);
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

            playbackButton.setBackground(pauseDrawable);
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

            i = 0;
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

    @Override
    public void onColorSelected(int dialogId, @ColorInt int color) {
        // TODO set color to all rows
        //ContextCompat.getColor(this, R.color.song_  text);

        //((TextView) findViewById(R.id.songNameLine)).setTextColor(color);
        adapter.setFontColor(color);
        adapter.notifyDataSetChanged();
        sharedPreferences.edit().putInt("fontColor", color).apply();
    }

    @Override
    public void onDialogDismissed(int dialogId) {

    }

    public SongsAdapter getAdapter() {
        return adapter;
    }
}
