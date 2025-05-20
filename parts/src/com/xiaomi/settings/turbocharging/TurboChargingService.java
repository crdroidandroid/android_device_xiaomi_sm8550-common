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

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.UEventObserver;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.xiaomi.settings.R;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;

public class TurboChargingService extends Service {
    private static final String TAG = "TurboChargingService";
    private static final String CHARGE_CURRENT_FILE = "/sys/class/power_supply/battery/constant_charge_current";

    private static final String PROP_TURBO_CURRENT = "persist.sys.turbo_charge_current";

    private static final String DEFAULT_OFF_VALUE = "6000000";
    private static final String DEFAULT_ON_VALUE_INDICATOR = ""; // Indicates kernel control

    private UEventObserver mObserver;
    private Handler mHandler = new Handler();
    private Runnable mMonitorRunnable;

    @Override
    public void onCreate() {
        Log.d(TAG, "Starting TurboChargingService");

        mObserver = new UEventObserver() {
            @Override
            public void onUEvent(UEvent event) {
                String chargerStatus = event.get("POWER_SUPPLY_ONLINE");
                if (chargerStatus != null && chargerStatus.equals("1")) {
                    updateChargeCurrent();
                }
            }
        };
        mObserver.startObserving("DEVPATH=/sys/class/power_supply/usb");

        updateChargeCurrent();
        startMonitoring();
    }

    private void updateChargeCurrent() {
        boolean turboEnabled = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("turbo_enable", false);

        if (!turboEnabled) {
            Log.i(TAG, "Updating charge current with value: " + DEFAULT_OFF_VALUE);
            setChargingProperty(DEFAULT_OFF_VALUE);
        } else {
            Log.i(TAG, "Turbo Charging is enabled. Not writing to charge current node.");
            // Do not write to CHARGE_CURRENT_FILE when turbo is enabled.
            // This allows the kernel to control it.
        }
    }

    private void setChargingProperty(String value) {
        try {
            Class<?> sp = Class.forName("android.os.SystemProperties");
            Method setProp = sp.getMethod("set", String.class, String.class);
            setProp.invoke(null, PROP_TURBO_CURRENT, value);
            Log.i(TAG, "Property " + PROP_TURBO_CURRENT + " set to " + value);
            if (!value.isEmpty()) { // Only write to file if we have a value to set (i.e., not kernel control)
                writeChargeCurrent(value);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to set property " + PROP_TURBO_CURRENT, e);
        }
    }

    private void writeChargeCurrent(String value) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CHARGE_CURRENT_FILE))) {
            writer.write(value);
            Log.i(TAG, "Updated charging current node to " + value);
        } catch (IOException e) {
            Log.e(TAG, "Failed to update charge current", e);
        }
    }

    private String readChargeCurrent() {
        try (BufferedReader reader = new BufferedReader(new FileReader(CHARGE_CURRENT_FILE))) {
            return reader.readLine();
        } catch (IOException e) {
            Log.e(TAG, "Failed to read charge current", e);
            return "";
        }
    }

    private void startMonitoring() {
        mMonitorRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    boolean turboEnabled = PreferenceManager.getDefaultSharedPreferences(TurboChargingService.this)
                            .getBoolean("turbo_enable", false);

                    if (!turboEnabled) {
                        String currentNodeValue = readChargeCurrent();
                        if (!DEFAULT_OFF_VALUE.equals(currentNodeValue)) {
                            Log.i(TAG, "Detected mismatch in sysfs node (found: " + currentNodeValue +
                                    ", desired: " + DEFAULT_OFF_VALUE + "). Updating...");
                            writeChargeCurrent(DEFAULT_OFF_VALUE);
                        }
                    } else {
                        Log.d(TAG, "Monitoring: Turbo Charging is enabled. Not enforcing charge current.");
                    }
                } finally {
                    mHandler.postDelayed(this, 5000);
                }
            }
        };
        mHandler.post(mMonitorRunnable);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        mObserver.stopObserving();
        mHandler.removeCallbacks(mMonitorRunnable);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}