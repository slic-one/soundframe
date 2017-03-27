package com.silentslic.soundframe;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.v4.content.ContextCompat;

import com.jrummyapps.android.colorpicker.ColorPickerDialog;

public class SettingsActivity extends PreferenceActivity {

    PlayerActivity playerActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        playerActivity = ((PlayerActivity) getParent());

        Preference pref = findPreference("foregroundColorPref");
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                ColorPickerDialog.newBuilder().setColor(ContextCompat.getColor(getApplicationContext(), R.color.song_text)).show(getParent());
                return true;
            }
        });
    }
}
