package com.silentslic.soundframe;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.annotation.ColorInt;
import android.util.Log;
import android.widget.Toast;

import com.jrummyapps.android.colorpicker.ColorPickerDialog;
import com.jrummyapps.android.colorpicker.ColorPickerDialogListener;
import com.jrummyapps.android.colorpicker.ColorPreference;

public class SettingsActivity extends PreferenceActivity  implements ColorPickerDialogListener {

    PlayerActivity playerActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        playerActivity = ((PlayerActivity) getParent());

        Preference pref = findPreference("bublik");
        pref.setOnPreferenceClickListener(new ColorPreference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Toast.makeText(getApplicationContext(), "Color set.", Toast.LENGTH_SHORT).show();
                ColorPickerDialog.newBuilder().setColor(0x111111).show(playerActivity);
                return false;
            }
        });
}

//        Preference pref = findPreference("foregroundColorPref");
//        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
//            @Override
//            public boolean onPreferenceClick(Preference preference) {
//                //ColorPickerDialog.newBuilder().setColor(0x111111).show(Activity);
//                return true;
//            }
//        });


    @Override
    public void onColorSelected(int dialogId, @ColorInt int color) {
        Log.i("dialogId", String.valueOf(dialogId));
        playerActivity.getAdapter().setFontColor(color);
        Toast.makeText(this, "Color set.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDialogDismissed(int dialogId) {

    }
}
