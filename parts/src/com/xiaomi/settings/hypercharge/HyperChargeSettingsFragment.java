/*
 * Copyright (C) 2025 TheMysticle
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.hypercharge;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceManager;

import com.android.settingslib.widget.MainSwitchPreference;

import org.lineageos.settings.Constants;
import org.lineageos.settings.R;

public class HyperChargeSettingsFragment extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "HyperChargeSettings";

    private MainSwitchPreference mMainSwitch;
    private ListPreference mLimitPref;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // 1. Repair corrupted data before inflating
        sanitizePreferences(getContext());

        // 2. Load the UI
        addPreferencesFromResource(R.xml.hypercharge_settings);

        mMainSwitch = findPreference(Constants.KEY_HYPERCHARGE_STATUS);
        mMainSwitch.setOnPreferenceChangeListener(this);

        mLimitPref = findPreference(Constants.KEY_HYPERCHARGE_LIMIT);
        mLimitPref.setOnPreferenceChangeListener(this);

        updateEnabledStates(mMainSwitch.isChecked());
    }

    private void sanitizePreferences(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        boolean changed = false;

        // Check the Main Switch (Should be Boolean)
        try {
            prefs.getBoolean(Constants.KEY_HYPERCHARGE_STATUS, false);
        } catch (ClassCastException e) {
            Log.w(TAG, "Corrupt hypercharge_status pref — resetting.");
            editor.remove(Constants.KEY_HYPERCHARGE_STATUS);
            changed = true;
        }

        // Check the Limit List (Should be String)
        try {
            prefs.getString(Constants.KEY_HYPERCHARGE_LIMIT, Constants.CHARGE_LIMIT_120W);
        } catch (ClassCastException e) {
            Log.w(TAG, "Corrupt hypercharge_limit pref — resetting.");
            editor.remove(Constants.KEY_HYPERCHARGE_LIMIT);
            changed = true;
        }

        if (changed) editor.apply();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean isEnabled = preference.equals(mMainSwitch) ? (Boolean) newValue : mMainSwitch.isChecked();
        String limit = preference.equals(mLimitPref) ? (String) newValue : mLimitPref.getValue();

        if (preference.equals(mMainSwitch)) {
            updateEnabledStates((Boolean) newValue);
        }

        Intent serviceIntent = new Intent(getContext(), HyperChargeService.class);

        if (isEnabled && Constants.CHARGE_LIMIT_120W.equals(limit)) {
            Log.i(TAG, "Switch ON + 120W selected — stopping service, kernel controls charging.");
            getContext().stopService(serviceIntent);
        } else {
            Log.i(TAG, "Starting service (isEnabled=" + isEnabled + ", limit=" + limit + ").");
            getContext().startService(serviceIntent);
        }

        return true;
    }

    private void updateEnabledStates(boolean masterEnabled) {
        mLimitPref.setEnabled(masterEnabled);
    }
}
