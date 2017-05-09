package com.silentslic.soundframe;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.jrummyapps.android.colorpicker.ColorPickerDialog;

public class UIUtil {

    private SharedPreferences sharedPreferences;
    private PlayerActivity parent;

    public UIUtil(PlayerActivity playerActivity, SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
        parent = playerActivity;
    }

    public void toggleActionBar() {
        sharedPreferences  = PreferenceManager.getDefaultSharedPreferences(parent);
        ActionBar actionBar = parent.getSupportActionBar();

        if (actionBar != null) {
            if (actionBar.isShowing())
                actionBar.hide();
            else
                actionBar.show();

            sharedPreferences.edit().putBoolean("actionBarShowing", actionBar.isShowing()).apply();
        }
    }
    public void toggleSeekBar() {
        View seekBarLine = parent.findViewById(R.id.seekBarLine);
        if (seekBarLine.getVisibility() == View.VISIBLE)
            seekBarLine.setVisibility(View.GONE);
        else
            seekBarLine.setVisibility(View.VISIBLE);

        sharedPreferences.edit().putInt("seekBarVisibility", seekBarLine.getVisibility()).apply();
    }
    public void toggleAdditionalButtons() {
        View shuffle = parent.findViewById(R.id.btnShuffle);
        View repeat = parent.findViewById(R.id.btnRepeat);

        if (shuffle.getVisibility() == View.VISIBLE) {
            shuffle.setVisibility(View.GONE);
            repeat.setVisibility(View.GONE);
        }
        else {
            shuffle.setVisibility(View.VISIBLE);
            repeat.setVisibility(View.VISIBLE);
        }

        sharedPreferences.edit().putInt("shuffleRepeatVisibility", shuffle.getVisibility()).apply();
    }
    public void foregroundColorPicker() {
        ColorPickerDialog.newBuilder().setColor(ContextCompat.getColor(parent, R.color.song_text)).show(parent);
        // TODO make this universal
    }


    public void startShutdownTimer() {
        final Spinner picker = new Spinner(parent);
        String[] values = {"15", "30", "60" };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(parent, android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        picker.setAdapter(adapter);

        AlertDialog.Builder builder1 = new AlertDialog.Builder(parent);
        builder1.setMessage("Shutdown timer");
        builder1.setView(picker);

        builder1.setPositiveButton(
                "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        int val = Integer.parseInt(picker.getSelectedItem().toString());
                        val *= 60000;
                        Log.i("CountdownTimer", "Set timer for " + val + "ms.");
                        new CountDownTimer(val, 1000) {
                            @Override
                            public void onTick(long millisUntilFinished) {}

                            @Override
                            public void onFinish() {
                                parent.finish();
                            }
                        }.start();
                        Toast.makeText(parent, "Timer set for " + picker.getSelectedItem().toString() + " minutes.", Toast.LENGTH_SHORT).show();
                    }
                });

        builder1.setNeutralButton(
                "Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                }
        );

        AlertDialog alert11 = builder1.create();
        alert11.show();
    }

    public void showQuitDialog() {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(parent);
        builder1.setMessage("Quit " + parent.getString(R.string.app_name) + "?");

        builder1.setPositiveButton(
                "Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        parent.finish();
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
}
