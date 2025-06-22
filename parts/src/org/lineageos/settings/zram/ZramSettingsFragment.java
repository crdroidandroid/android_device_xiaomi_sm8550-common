/*
 * Copyright (C) 2025 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.settings.zram;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import org.lineageos.settings.R;
import org.lineageos.settings.zram.CustomSeekBarPreference;

public class ZramSettingsFragment extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY_ZRAM_SIZE = "zram_size";
    private static final String KEY_ZRAM_COMPRESSION = "zram_compression";
    private static final String KEY_SWAPPINESS = "swappiness";
    
    private ListPreference mZramSizePreference;
    private ListPreference mZramCompressionPreference;
    private CustomSeekBarPreference mSwappinessPreference;
    private ZramUtils mZramUtils;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.zram_settings, rootKey);
        mZramUtils = new ZramUtils(getActivity());
    
        // Initialize ZRAM size preference
        mZramSizePreference = (ListPreference) findPreference(KEY_ZRAM_SIZE);
        if (mZramSizePreference != null) {
            int currentSize = mZramUtils.getCurrentZramSize();
            mZramSizePreference.setValue(String.valueOf(currentSize));
            updateSummary(currentSize);
            mZramSizePreference.setOnPreferenceChangeListener(this);
        }
    
        // Initialize compression algorithm preference
        mZramCompressionPreference = (ListPreference) findPreference(KEY_ZRAM_COMPRESSION);
        if (mZramCompressionPreference != null) {
            String currentComp = mZramUtils.getCurrentCompression();
            mZramCompressionPreference.setValue(currentComp);
            mZramCompressionPreference.setSummary(currentComp);
            mZramCompressionPreference.setOnPreferenceChangeListener(this);
        }
    
        // Initialize swappiness preference
        CustomSeekBarPreference swappinessPref = (CustomSeekBarPreference) findPreference(KEY_SWAPPINESS);
        if (swappinessPref != null) {
            int currentSwappiness = mZramUtils.getCurrentSwappiness();
            swappinessPref.setValue(currentSwappiness);
            swappinessPref.setOnPreferenceChangeListener(this);
            updateSwappinessSummary(currentSwappiness);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (KEY_ZRAM_SIZE.equals(preference.getKey())) {
            try {
                int value = Integer.parseInt((String) newValue);
                mZramUtils.setZramSize(value);
                updateSummary(value);
                
                new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.zram_reboot_recommended)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        } else if (KEY_ZRAM_COMPRESSION.equals(preference.getKey())) {
            String algorithm = (String) newValue;
            mZramUtils.setCompressionAlgorithm(algorithm);
            mZramCompressionPreference.setSummary(algorithm);
            
            new AlertDialog.Builder(getActivity())
                .setMessage(R.string.zram_reboot_recommended)
                .setPositiveButton(android.R.string.ok, null)
                .show();
            return true;
        } else if (KEY_SWAPPINESS.equals(preference.getKey())) {
            int value = (Integer) newValue;
            mZramUtils.setSwappiness(value);
            updateSwappinessSummary(value);
            return true;
        }
        return false;
    }

    private void updateSummary(int value) {
        String summary;
        switch (value) {
            case 0: summary = getString(R.string.zram_disabled); break;
            case 2: summary = getString(R.string.zram_size_2gb); break;
            case 4: summary = getString(R.string.zram_size_4gb); break;
            case 8: summary = getString(R.string.zram_size_8gb); break;
            default: summary = getString(R.string.zram_size_dynamic);
        }
        mZramSizePreference.setSummary(summary);
    }
    
    private void updateSwappinessSummary(int value) {
        if (mSwappinessPreference != null) {
            mSwappinessPreference.setSummary(getString(R.string.swappiness_value, value));
        }
    }
}
