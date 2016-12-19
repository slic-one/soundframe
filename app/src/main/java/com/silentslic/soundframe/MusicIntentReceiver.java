package com.silentslic.soundframe;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * BroadcastReciever for handling the headset plug event
 */

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
//            if(telephonyManager.getDataState() == TelephonyManager.STATE){
//
//            }
            Log.i("MusicIntentReceiver", "In Call fired");
            PlayerActivity.player.pause();
        }
//        else if (intent.getAction().equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
//            Log.d("MusicIntentReceiver", "ACTION_AUDIO_BECOMING_NOISY fired");
//        }
    }
}
