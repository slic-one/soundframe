package com.silentslic.soundframe;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.telephony.TelephonyManager;
import android.util.Log;

public class MusicIntentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(AudioManager.ACTION_HEADSET_PLUG)) {
            if (intent.getIntExtra("state", -1) == 0) {
                Log.i("MusicIntentReceiver", "Headphones unplug fired");
                PlayerActivity.player.pause();
            }
        }
        else if (intent.getAction().equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
            Log.i("MusicIntentReceiver", "In Call fired with state " + intent.getStringExtra("state"));
            if (intent.getStringExtra("state").equals("OFFHOOK"))
                PlayerActivity.player.pause();
            else if (intent.getStringExtra("state").equals("IDLE"))
                PlayerActivity.player.start();
        }
    }
}
