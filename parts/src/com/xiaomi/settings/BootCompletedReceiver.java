/*
 * Copyright (C) 2023 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.os.UserHandle;
import android.util.Log;
import android.view.Display;
import android.view.Display.HdrCapabilities;

import com.xiaomi.settings.turbocharging.TurboChargingService;

public class BootCompletedReceiver extends BroadcastReceiver {
    private static final String TAG = "XiaomiParts";
    private static final boolean DEBUG = true;

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }

        if (DEBUG)
            Log.d(TAG, "Received boot completed intent");

        // Start TurboChargingService
        try {
            Intent turboChargingIntent = new Intent(context, TurboChargingService.class);
            context.startService(turboChargingIntent);
            if (DEBUG) Log.d(TAG, "Started TurboChargingService");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start TurboChargingService", e);
        }

        // Override HDR types to enable Dolby Vision
        try {
            final DisplayManager displayManager = context.getSystemService(DisplayManager.class);
            if (displayManager != null) {
                displayManager.overrideHdrTypes(Display.DEFAULT_DISPLAY, new int[] {
                        HdrCapabilities.HDR_TYPE_DOLBY_VISION,
                        HdrCapabilities.HDR_TYPE_HDR10,
                        HdrCapabilities.HDR_TYPE_HLG,
                        HdrCapabilities.HDR_TYPE_HDR10_PLUS
                });
                if (DEBUG) Log.d(TAG, "HDR types overridden successfully.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to override HDR types", e);
        }
    }
}
