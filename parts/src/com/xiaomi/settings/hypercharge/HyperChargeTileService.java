/*
 * Copyright (C) 2025 TheMysticle
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.hypercharge;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;
import androidx.preference.PreferenceManager;

import org.lineageos.settings.Constants;
import org.lineageos.settings.R;

public class HyperChargeTileService extends TileService {

    private static final String TAG = "HyperChargeTileService";

    private SharedPreferences mSharedPrefs;

    @Override
    public void onCreate() {
        super.onCreate();
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTileState();
    }

    @Override
    public void onClick() {
        super.onClick();
        toggleState();
        updateTileState();
    }

    private void toggleState() {
        boolean currentState = mSharedPrefs.getBoolean(Constants.KEY_HYPERCHARGE_STATUS, false);
        boolean newState = !currentState;
        mSharedPrefs.edit().putBoolean(Constants.KEY_HYPERCHARGE_STATUS, newState).apply();

        String limit = mSharedPrefs.getString(Constants.KEY_HYPERCHARGE_LIMIT, Constants.CHARGE_LIMIT_120W);
        Intent serviceIntent = new Intent(this, HyperChargeService.class);

        if (newState && Constants.CHARGE_LIMIT_120W.equals(limit)) {
            Log.i(TAG, "Tile toggled ON with 120W — stopping service, kernel controls charging.");
            stopService(serviceIntent);
        } else {
            Log.i(TAG, "Tile toggled — starting service (newState=" + newState + ", limit=" + limit + ").");
            startService(serviceIntent);
        }
    }

    private void updateTileState() {
        boolean isActive = mSharedPrefs.getBoolean(Constants.KEY_HYPERCHARGE_STATUS, false);
        String limit = mSharedPrefs.getString(Constants.KEY_HYPERCHARGE_LIMIT, Constants.CHARGE_LIMIT_120W);

        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }

        // Main label is always "HyperCharge"
        tile.setLabel(getString(R.string.hypercharge_tile_label));

        if (!isActive) {
            tile.setState(Tile.STATE_INACTIVE);
            tile.setSubtitle(getString(R.string.tile_off));
            tile.setIcon(Icon.createWithResource(this, R.drawable.ic_qs_hypercharge_off));
        } else {
            tile.setState(Tile.STATE_ACTIVE);
            tile.setIcon(Icon.createWithResource(this, R.drawable.ic_qs_hypercharge_on));
            tile.setSubtitle(getSpeedLabel(limit));
        }

        tile.updateTile();
    }

    /**
     * Returns a label for the given charge limit value.
     */
    private String getSpeedLabel(String limit) {
        switch (limit) {
            case Constants.CHARGE_LIMIT_120W: return getString(R.string.hypercharge_speed_120w);
            case Constants.CHARGE_LIMIT_90W:  return getString(R.string.hypercharge_speed_90w);
            case Constants.CHARGE_LIMIT_67W:  return getString(R.string.hypercharge_speed_67w);
            case Constants.CHARGE_LIMIT_50W:  return getString(R.string.hypercharge_speed_50w);
            case Constants.CHARGE_LIMIT_33W:  return getString(R.string.hypercharge_speed_33w);
            case Constants.CHARGE_LIMIT_30W:  return getString(R.string.hypercharge_speed_30w);
            default:                          return limit;
        }
    }
}
