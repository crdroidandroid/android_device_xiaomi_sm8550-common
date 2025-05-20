/*
 * Copyright (C) 2025 kenway214
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xiaomi.settings.turbocharging;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceManager;

import com.android.settingslib.widget.MainSwitchPreference;
import com.xiaomi.settings.R;

import java.lang.reflect.Method;

public class TurboChargingFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "TurboChargingFragment";

    private static final String PROP_TURBO_CURRENT = "persist.sys.turbo_charge_current";
    private static final String DEFAULT_OFF_VALUE = "6000000"; // Value for "off" state
    private static final String DEFAULT_ON_VALUE_INDICATOR = ""; // Indicates kernel control

    private static final String PREF_TURBO_ENABLED = "turbo_enable";

    private MainSwitchPreference mTurboEnabled;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.turbocharging, rootKey);

        mTurboEnabled = (MainSwitchPreference) findPreference(PREF_TURBO_ENABLED);
        mTurboEnabled.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mTurboEnabled) {
            boolean turboEnabled = (boolean) newValue;
            updateChargeCurrent(turboEnabled);
            Toast.makeText(getActivity(),
                    turboEnabled ? getString(R.string.toast_turbo_on) : getString(R.string.toast_turbo_off),
                    Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    private void updateChargeCurrent(boolean turboEnabled) {
        String valueToSet = turboEnabled ? DEFAULT_ON_VALUE_INDICATOR : DEFAULT_OFF_VALUE;
        Log.i(TAG, "Setting System property " + PROP_TURBO_CURRENT + " to: " + valueToSet);
        setChargingProperty(valueToSet);
    }

    private void setChargingProperty(String value) {
        try {
            Class<?> sp = Class.forName("android.os.SystemProperties");
            Method setProp = sp.getMethod("set", String.class, String.class);
            setProp.invoke(null, PROP_TURBO_CURRENT, value);
            Log.i(TAG, "System property " + PROP_TURBO_CURRENT + " set to " + value);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set property " + PROP_TURBO_CURRENT, e);
        }
    }
}