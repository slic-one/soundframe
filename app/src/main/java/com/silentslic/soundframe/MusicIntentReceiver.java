package com.silentslic.soundframe;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;

/**
 * BroadcastReciever for handling the AUDIO_BECOMES_NOISY event
 */

public class MusicIntentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(AudioManager.ACTION_HEADSET_PLUG)) {
            if (intent.getIntExtra("state", -1) == 0) {
                PlayerActivity.player.pause();
            }
        }
    }
}
