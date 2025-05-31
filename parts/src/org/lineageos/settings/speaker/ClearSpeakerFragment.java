/*
 * Copyright (C) 2025 Paranoid Android
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

package org.lineageos.settings.speaker;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import org.lineageos.settings.R;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ClearSpeakerFragment extends PreferenceFragmentCompat implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = ClearSpeakerFragment.class.getSimpleName();
    private static final String PREF_CLEAR_SPEAKER = "clear_speaker_pref";

    private AudioManager mAudioManager;
    private Handler mHandler;
    private MediaPlayer mMediaPlayer;
    private SwitchPreference mClearSpeakerPref;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.clear_speaker_settings);

        mClearSpeakerPref = findPreference(PREF_CLEAR_SPEAKER);
        if (mClearSpeakerPref != null) {
            mClearSpeakerPref.setOnPreferenceChangeListener(this);
        }

        mHandler = new Handler();
        mAudioManager = (AudioManager) requireContext().getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mClearSpeakerPref) {
            boolean value = (Boolean) newValue;
            if (value) {
                if (startPlaying()) {
                    mHandler.removeCallbacksAndMessages(null);
                    mHandler.postDelayed(this::stopPlaying, 30000);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onStop() {
        super.onStop();
        stopPlaying();
    }

    private boolean startPlaying() {
        mAudioManager.setParameters("status_earpiece_clean=on");
        mMediaPlayer = new MediaPlayer();
        requireActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setLooping(true);
        
        try {
            AssetFileDescriptor file = getResources().openRawResourceFd(R.raw.clear_speaker_sound);
            try {
                mMediaPlayer.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
            } finally {
                file.close();
            }
            mClearSpeakerPref.setEnabled(false);
            mMediaPlayer.setVolume(1.0f, 1.0f);
            mMediaPlayer.prepare();
            mMediaPlayer.start();
        } catch (IOException ioe) {
            Log.e(TAG, "Failed to play speaker clean sound!", ioe);
            return false;
        }
        return true;
    }

    private void stopPlaying() {
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
                mMediaPlayer.reset();
                mMediaPlayer.release();
                mMediaPlayer = null;
            }
        }
        mAudioManager.setParameters("status_earpiece_clean=off");
        if (mClearSpeakerPref != null) {
            mClearSpeakerPref.setEnabled(true);
            mClearSpeakerPref.setChecked(false);
        }
    }

    public static class SpeakerCleanerHelper {
        private static final String TAG = "SpeakerCleanerHelper";
        
        public static void scheduleCleanTask(Context context, String frequency) {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            
            switch (frequency) {
                case "daily":
                    scheduleRecurringTask(context, TimeUnit.HOURS.toMillis(24));
                    break;
                case "weekly":
                    scheduleRecurringTask(context, TimeUnit.DAYS.toMillis(7));
                    break;
                case "monthly":
                    scheduleRecurringTask(context, TimeUnit.DAYS.toMillis(30));
                    break;
                case "after_call":
                    break;
                case "on_detect":
                    break;
            }
        }

        private static void scheduleRecurringTask(Context context, long interval) {
            // Implementation for scheduling recurring task
        }
    }
}