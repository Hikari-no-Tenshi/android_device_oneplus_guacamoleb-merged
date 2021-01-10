/*
 * Copyright (C) 2015-2016 The CyanogenMod Project
 * Copyright (C) 2018 The LineageOS Project
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

package org.lineageos.device.DeviceSettings;

import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.UEventObserver;
import android.os.UserHandle;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseIntArray;

import org.lineageos.device.DeviceSettings.Constants;

public class KeyHandler extends Service {
    private static final String TAG = KeyHandler.class.getSimpleName();

    private static final SparseIntArray sSupportedSliderZenModes = new SparseIntArray();
    private static final SparseIntArray sSupportedSliderRingModes = new SparseIntArray();
    static {
        sSupportedSliderZenModes.put(Constants.KEY_VALUE_TOTAL_SILENCE, Settings.Global.ZEN_MODE_NO_INTERRUPTIONS);
        sSupportedSliderZenModes.put(Constants.KEY_VALUE_SILENT, Settings.Global.ZEN_MODE_OFF);
        sSupportedSliderZenModes.put(Constants.KEY_VALUE_PRIORTY_ONLY, Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS);
        sSupportedSliderZenModes.put(Constants.KEY_VALUE_VIBRATE, Settings.Global.ZEN_MODE_OFF);
        sSupportedSliderZenModes.put(Constants.KEY_VALUE_NORMAL, Settings.Global.ZEN_MODE_OFF);

        sSupportedSliderRingModes.put(Constants.KEY_VALUE_TOTAL_SILENCE, AudioManager.RINGER_MODE_NORMAL);
        sSupportedSliderRingModes.put(Constants.KEY_VALUE_SILENT, AudioManager.RINGER_MODE_SILENT);
        sSupportedSliderRingModes.put(Constants.KEY_VALUE_PRIORTY_ONLY, AudioManager.RINGER_MODE_NORMAL);
        sSupportedSliderRingModes.put(Constants.KEY_VALUE_VIBRATE, AudioManager.RINGER_MODE_VIBRATE);
        sSupportedSliderRingModes.put(Constants.KEY_VALUE_NORMAL, AudioManager.RINGER_MODE_NORMAL);
    }

    private NotificationManager mNotificationManager;
    private AudioManager mAudioManager;
    private Vibrator mVibrator;
    private Handler mHandler;

    @Override
    public void onCreate() {
        super.onCreate();

        mHandler = new Handler(Looper.getMainLooper());
        mNotificationManager = getSystemService(NotificationManager.class);
        mAudioManager = getSystemService(AudioManager.class);
        mVibrator = getSystemService(Vibrator.class);
        if (mVibrator == null || !mVibrator.hasVibrator()) {
            mVibrator = null;
        }
        alertSliderEventObserver.startObserving("DEVPATH=/devices/platform/soc/soc:tri_state_key");

        mCustomSettingsObserver.observe();
        mCustomSettingsObserver.update();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private UEventObserver alertSliderEventObserver = new UEventObserver() {
        private Object lock = new Object();

        @Override
        public void onUEvent(UEventObserver.UEvent event) {
            synchronized(lock) {
                String state = event.get("STATE");
                if (state == null) {
                    return;
                }
                boolean none = state.contains("USB=0");
                boolean vibration = state.contains("HOST=0");
                boolean silent = state.contains("null)=0");

                if (none && !vibration && !silent) {
                    handleMode(Constants.POSITION_BOTTOM);
                } else if (!none && vibration && !silent) {
                    handleMode(Constants.POSITION_MIDDLE);
                } else if (!none && !vibration && silent) {
                    handleMode(Constants.POSITION_TOP);
                }
            }
        }
    };

    private void handleMode(int position) {
        int mode = Constants.getPreferenceInt(this, Constants.sKeyMap.get(position));
        mAudioManager.setRingerModeInternal(sSupportedSliderRingModes.get(mode));
        mNotificationManager.setZenMode(sSupportedSliderZenModes.get(mode), null, TAG);
        doHapticFeedback();

        int positionValue = 0;
        int key = sSupportedSliderRingModes.keyAt(
                sSupportedSliderRingModes.indexOfKey(mode));
            switch (key) {
                case Constants.KEY_VALUE_TOTAL_SILENCE:
                    positionValue = Constants.MODE_TOTAL_SILENCE;
                    break;
                case Constants.KEY_VALUE_SILENT:
                    positionValue = Constants.MODE_SILENT;
                    break;
                case Constants.KEY_VALUE_PRIORTY_ONLY:
                    positionValue = Constants.MODE_PRIORITY_ONLY;
                    break;
                case Constants.KEY_VALUE_VIBRATE:
                    positionValue = Constants.MODE_VIBRATE;
                    break;
                default:
                    case Constants.KEY_VALUE_NORMAL:
                        positionValue = Constants.MODE_RING;
                        break;
            }

        sendUpdateBroadcast(position, positionValue);
    }

    private void sendUpdateBroadcast(int position, int position_value) {
        Intent intent = new Intent(Constants.ACTION_UPDATE_SLIDER_POSITION);
        intent.putExtra(Constants.EXTRA_SLIDER_POSITION, position);
        intent.putExtra(Constants.EXTRA_SLIDER_POSITION_VALUE, position_value);
        sendBroadcastAsUser(intent, UserHandle.CURRENT);
        intent.setFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        Log.d(TAG, "slider change to positon " + position
                + " with value " + position_value);
    }

    private void doHapticFeedback() {
        if (mVibrator == null) {
            return;
        }
        mVibrator.vibrate(50);
    }

    private CustomSettingsObserver mCustomSettingsObserver = new CustomSettingsObserver(mHandler);
    private class CustomSettingsObserver extends ContentObserver {

        CustomSettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = getApplicationContext().getContentResolver();
            resolver.registerContentObserver(Settings.Secure.getUriFor(
                    Settings.Secure.UI_NIGHT_MODE),
                    false, this, UserHandle.USER_ALL);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (uri.equals(Settings.Secure.getUriFor(
                    Settings.Secure.UI_NIGHT_MODE))) {
                updateNightMode();
            }
        }

        public void update() {
            updateNightMode();
        }
    }

    private void updateNightMode() {
        int currentNightMode = getApplicationContext().getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        switch (currentNightMode) {
            case Configuration.UI_MODE_NIGHT_NO:
                Settings.System.putIntForUser(getApplicationContext().getContentResolver(),
                        Settings.System.OEM_BLACK_MODE, 0, UserHandle.USER_CURRENT);
                break;
            case Configuration.UI_MODE_NIGHT_YES:
                Settings.System.putIntForUser(getApplicationContext().getContentResolver(),
                        Settings.System.OEM_BLACK_MODE, 1, UserHandle.USER_CURRENT);
                break;
        }
    }
}
