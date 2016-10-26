package com.silentslic.soundframe;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import static com.silentslic.soundframe.PlayerActivity.player;

/**
 * MediaPlayer service for background playback
 */

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

                //if (player == null) initializePlayer();
                //else if (!player.isPlaying()) player.start();

                if (!player.isPlaying() && player != null)
                    player.start();
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                if(player.isPlaying())
                    player.stop();
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