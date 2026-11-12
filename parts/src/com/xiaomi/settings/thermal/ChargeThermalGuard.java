/*
 * Copyright (C) 2025 TheMysticle
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.thermal;

import android.util.Log;

import org.lineageos.settings.utils.FileUtils;

/**
 * Stock Xiaomi runs the thermal profile (sconfig) in its default "0" state
 * while fast charging. Higher profiles (gaming/performance/per-app) make
 * thermal-engine treat the phone as in-use and throttle the charge current.
 *
 * While the guard is held, every sconfig write is deferred: the latest
 * requested value is remembered and applied when the guard is released.
 */
public final class ChargeThermalGuard {
    private static final String TAG = "ChargeThermalGuard";
    private static final String THERMAL_SCONFIG = "/sys/class/thermal/thermal_message/sconfig";
    private static final String CHARGING_PROFILE = "0";

    private static boolean sHeld;
    private static String sPending;

    private ChargeThermalGuard() {}

    /** Write sconfig, or defer the write while fast charging. */
    public static synchronized boolean write(String value) {
        if (sHeld) {
            sPending = value;
            Log.d(TAG, "Deferring sconfig " + value + " until charging ends");
            return true;
        }
        return FileUtils.writeLine(THERMAL_SCONFIG, value);
    }

    public static synchronized void hold() {
        if (sHeld) return;
        String current = FileUtils.readOneLine(THERMAL_SCONFIG);
        sPending = current != null ? current.trim() : null;
        sHeld = true;
        if (!CHARGING_PROFILE.equals(sPending)) {
            Log.i(TAG, "Fast charging: sconfig " + sPending + " -> " + CHARGING_PROFILE);
            FileUtils.writeLine(THERMAL_SCONFIG, CHARGING_PROFILE);
        }
    }

    public static synchronized void release() {
        if (!sHeld) return;
        sHeld = false;
        if (sPending != null && !CHARGING_PROFILE.equals(sPending)) {
            Log.i(TAG, "Charging ended: restoring sconfig " + sPending);
            FileUtils.writeLine(THERMAL_SCONFIG, sPending);
        }
        sPending = null;
    }
}
