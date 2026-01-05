/*
 * Copyright (C) 2023 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.UserHandle;
import android.os.Looper;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.lineageos.settings.autohbm.AutoHbmActivity;
import org.lineageos.settings.autohbm.AutoHbmFragment;
import org.lineageos.settings.autohbm.AutoHbmTileService;
import org.lineageos.settings.thermal.ThermalService;
import org.lineageos.settings.thermal.ThermalUtils;
import org.lineageos.settings.touch.TouchOrientationService;
import org.lineageos.settings.touch.TouchUtils;
import org.lineageos.settings.utils.ComponentUtils;

public class BootCompletedReceiver extends BroadcastReceiver {
    private static final String TAG = "XiaomiParts";
    private static final boolean DEBUG = true;
    private static final int GESTURE_INIT_DELAY_MS = 5000; // 5 seconds

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (!intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            return;
        }
        if (DEBUG) Log.d(TAG, "Received boot completed intent");
            
        PreferenceManager.setDefaultValues(context, R.xml.hypercharge_settings, false);
        try {
            ThermalUtils thermalUtils = ThermalUtils.getInstance(context);
            if (thermalUtils.isEnabled()) {
                Intent thermalServiceIntent = new Intent(context, ThermalService.class);
                context.startService(thermalServiceIntent);
                if (DEBUG) Log.d(TAG, "Started ThermalService");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start ThermalService", e);
        }

        try {
            if (DEBUG) Log.d(TAG, "Starting Auto HBM service components");
            AutoHbmFragment.toggleAutoHbmService(context);
            ComponentUtils.toggleComponent(context, AutoHbmActivity.class, true);
            ComponentUtils.toggleComponent(context, AutoHbmTileService.class, true);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start AutoHBM components", e);
        }

        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            boolean isHyperChargeEnabled = prefs.getBoolean(Constants.KEY_HYPERCHARGE_STATUS, true);

            if (DEBUG) Log.d(TAG, "HyperCharge state on boot: " + (isHyperChargeEnabled ? "ON" : "OFF (Limited)"));

            if (!isHyperChargeEnabled) {
                if (DEBUG) Log.d(TAG, "HyperCharge is set to OFF, starting limit service on boot.");
                Intent serviceIntent = new Intent(context, org.lineageos.settings.hypercharge.HyperChargeService.class);
                context.startService(serviceIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start HyperChargeService", e);
        }

        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            boolean isEdgeRejectionEnabled = prefs.getBoolean(Constants.KEY_EDGE_REJECTION, true);
            if (DEBUG) Log.d(TAG, "Setting initial edge rejection state to: " + isEdgeRejectionEnabled);
            TouchUtils.setEdgeRejectionEnabled(isEdgeRejectionEnabled);

            if (isEdgeRejectionEnabled) {
                if (DEBUG) Log.d(TAG, "Edge rejection is enabled, starting TouchOrientationService.");
                context.startServiceAsUser(new Intent(context, TouchOrientationService.class), UserHandle.CURRENT);
            } else {
                if (DEBUG) Log.d(TAG, "Edge rejection is disabled, TouchOrientationService will not be started.");
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to set initial edge rejection state or start service", e);
        }

    }
}