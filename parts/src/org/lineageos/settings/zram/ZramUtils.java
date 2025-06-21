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

import android.content.Context;
import android.os.SystemProperties;
import android.util.Log;
import android.widget.Toast;
import androidx.preference.PreferenceManager;
import android.content.SharedPreferences;
import org.lineageos.settings.R;

import java.io.FileWriter;
import java.io.IOException;

public class ZramUtils {
    private static final String TAG = "ZramUtils";
    private static final String ZRAM_PROP = "persist.vendor.zram.size";
    private static final String ZRAM_COMP_PROP = "persist.vendor.zram.comp_algorithm";
    private static final String SWAPPINESS_PROP = "persist.vendor.vm.swappiness";
    private static final String PREF_ZRAM_SIZE = "zram_size";
    private static final String PREF_ZRAM_COMP = "zram_compression";
    private static final String PREF_SWAPPINESS = "swappiness";

    private final Context mContext;
    private final SharedPreferences mSharedPrefs;

    public ZramUtils(Context context) {
        mContext = context;
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public int getCurrentZramSize() {
        // Try system property first
        String propValue = SystemProperties.get(ZRAM_PROP, "-1");
        if (!propValue.equals("-1")) {
            try {
                return Integer.parseInt(propValue);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid ZRAM size in property: " + propValue);
            }
        }
        
        // Fall back to SharedPreferences
        try {
            return Integer.parseInt(mSharedPrefs.getString(PREF_ZRAM_SIZE, "-1"));
        } catch (Exception e) {
            return -1; // Default to dynamic size
        }
    }

    public void setZramSize(int size) {
        // Validate input
        if (size != 0 && size != 2 && size != 4 && size != 8 && size != -1) {
            Log.w(TAG, "Invalid ZRAM size: " + size);
            return;
        }

        // Store in SharedPreferences as String
        mSharedPrefs.edit()
                .putString(PREF_ZRAM_SIZE, String.valueOf(size))
                .apply();
        
        // Set system property
        SystemProperties.set(ZRAM_PROP, String.valueOf(size));
        
        Toast.makeText(mContext, R.string.zram_change_applied, Toast.LENGTH_SHORT).show();
    }
    
    public String getCurrentCompression() {
        String propValue = SystemProperties.get(ZRAM_COMP_PROP, "lz4");
        if (!propValue.isEmpty()) {
            return propValue;
        }
        return mSharedPrefs.getString(PREF_ZRAM_COMP, "lz4");
    }

    public int getCurrentSwappiness() {
        String propValue = SystemProperties.get(SWAPPINESS_PROP, "60");
        try {
            return Integer.parseInt(propValue);
        } catch (NumberFormatException e) {
            return mSharedPrefs.getInt(PREF_SWAPPINESS, 60);
        }
    }

    public void setCompressionAlgorithm(String algorithm) {
        if (!algorithm.equals("lz4") && !algorithm.equals("lzo") && 
            !algorithm.equals("lzo-rle") && !algorithm.equals("zstd")) {
            Log.w(TAG, "Invalid compression algorithm: " + algorithm);
            return;
        }

        mSharedPrefs.edit()
                .putString(PREF_ZRAM_COMP, algorithm)
                .apply();
    
        SystemProperties.set(ZRAM_COMP_PROP, algorithm);
    }

    public void setSwappiness(int value) {
        if (value < 0 || value > 100) {
            Log.w(TAG, "Invalid swappiness value: " + value);
            return;
        }

        // Store in SharedPreferences
       mSharedPrefs.edit()
                .putInt(PREF_SWAPPINESS, value)
                .apply();
    
        // Set system property
        SystemProperties.set(SWAPPINESS_PROP, String.valueOf(value));
    
        // Apply immediately
        try {
            FileWriter writer = new FileWriter("/proc/sys/vm/swappiness");
            writer.write(String.valueOf(value));
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to write to swappiness", e);
        }
    
        Toast.makeText(mContext, R.string.zram_change_applied, Toast.LENGTH_SHORT).show();
    }

    private boolean writeToFile(String path, String value) {
        try (FileWriter writer = new FileWriter(path)) {
            writer.write(value);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Failed to write to " + path, e);
            return false;
        }
    }
}
