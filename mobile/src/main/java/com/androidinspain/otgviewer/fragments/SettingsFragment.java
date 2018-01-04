package com.androidinspain.otgviewer.fragments;

/**
 * Created by roberto on 29/08/15.
 */

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.Menu;

import com.androidinspain.otgviewer.R;


public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String enable_transitions = "enable_transitions";
    private static final boolean enable_transitions_def = true;
    private static final String enable_shake = "enable_shake";
    private static final boolean enable_shake_def = true;
    private static final String low_ram = "low_ram";
    private static final boolean low_ram_def = false;
    private static final String showcase_speed = "showcase_speed";
    private static final String showcase_speed_def = "5000"; // medium

    private SettingsCallback mMainActivity;
    private String TAG = getClass().getSimpleName();

    public SettingsFragment() {
        // Required empty public constructor
    }

    public static boolean areTransitionsEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(enable_transitions, enable_transitions_def);
    }

    public static boolean isShakeEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(enable_shake, enable_shake_def);
    }

    public static boolean isLowRamEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(low_ram, low_ram_def);
    }

    public static int getShowcaseSpeed(Context context) {
        return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString(showcase_speed, showcase_speed_def));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Change something dinamically
    }

    public interface SettingsCallback {
        public void setABTitle(String title, boolean showMenu);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        addPreferencesFromResource(R.xml.settings);
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_settings).setVisible(false);
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mMainActivity = (SettingsCallback) activity;
            updateUI();
        } catch (ClassCastException castException) {
            /** The activity does not implement the listener. */
        }

    }

    private void updateUI() {
        mMainActivity.setABTitle(getString(R.string.settings_title), true);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
