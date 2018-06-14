/*
 * Copyright (C) 2020-2022 crDroid Android Project
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

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Spline;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class AutoHighBrightnessModeService extends Service {
    private static final String BRIGHTNESS_FILE =
            "/sys/class/backlight/panel0-backlight/brightness";
    private static final String HBM_BRIGHTNESS_FILE =
            "/sys/devices/platform/soc/ae00000.qcom,mdss_mdp/drm/card0/card0-DSI-1/hbm_brightness";

    private SensorManager mSensorManager;
    private PowerManager mPowerManager;
    private Spline mHBMLuxToBacklightSpline;
    private boolean mAutoHBMSensorEnabled = false;
    private boolean mIsAutomaticBrightnessEnabled = false;
    private int mLightSensorRate = 0;
    private ExecutorService mExecutorService;

    private float getHBMBrightness(float lux) {
        return mHBMLuxToBacklightSpline.interpolate(lux);
    }

    private void activateLightSensorRead() {
        submit(() -> {
            mSensorManager.registerListener(
                    mSensorEventListener,
                    mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT),
                    mLightSensorRate);
            mAutoHBMSensorEnabled = true;
        });
    }

    private void deactivateLightSensorRead() {
        submit(() -> {
            mSensorManager.unregisterListener(mSensorEventListener);
            mAutoHBMSensorEnabled = false;
        });
    }

    private final SensorEventListener mSensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (mAutoHBMSensorEnabled && mIsAutomaticBrightnessEnabled) {
                float lux = event.values[0];
                if ((lux > 6500.0f && getCurrentBrightness() == 1023) && mPowerManager.isInteractive()) {
                    int hbm_brightness = Math.round(getHBMBrightness(lux));
                    Utils.writeValue(HBM_BRIGHTNESS_FILE, String.valueOf(hbm_brightness));
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // do nothing
        }
    };

    private final BroadcastReceiver mScreenStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch(action) {
                case Intent.ACTION_SCREEN_ON:
                    activateLightSensorRead();
                    break;
                case Intent.ACTION_SCREEN_OFF:
                    deactivateLightSensorRead();
                    break;
            }
        }
    };

    @Override
    public void onCreate() {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mExecutorService = Executors.newSingleThreadExecutor();
        IntentFilter screenStateFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mScreenStateReceiver, screenStateFilter);
        mLightSensorRate = getResources().getInteger(
                com.android.internal.R.integer.config_autoBrightnessLightSensorRate);
        float[] hbm_brightness = getFloatArray(
                getResources().obtainTypedArray(R.array.config_HBMBrightnessBacklight));
        float[] hbm_lux = getFloatArray(
                getResources().obtainTypedArray(R.array.config_HBMautoBrightnessLevels));
        mHBMLuxToBacklightSpline = Spline.createSpline(hbm_lux, hbm_brightness);
        mCustomSettingsObserver.observe();
        activateLightSensorRead();
    }

    private Future<?> submit(Runnable runnable) {
        return mExecutorService.submit(runnable);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mScreenStateReceiver);
        deactivateLightSensorRead();
        mCustomSettingsObserver.unobserve();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private final CustomSettingsObserver mCustomSettingsObserver =
            new CustomSettingsObserver(new Handler(Looper.getMainLooper()));
    private class CustomSettingsObserver extends ContentObserver {

        CustomSettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.SCREEN_BRIGHTNESS_MODE),
                    false, this, UserHandle.USER_ALL);
            update();
        }

        void unobserve() {
            getContentResolver().unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (uri.equals(Settings.System.getUriFor(
                    Settings.System.SCREEN_BRIGHTNESS_MODE))) {
                update();
            }
        }

        public void update() {
            updateBrightnessMode();
        }
    }

    private void updateBrightnessMode() {
        mIsAutomaticBrightnessEnabled = Settings.System.getIntForUser(
                getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
                UserHandle.USER_CURRENT) == 1;
    }

    private int getCurrentBrightness() {
        return Integer.parseInt(Utils.getFileValue(BRIGHTNESS_FILE, "0"));
    }

    private static float[] getFloatArray(TypedArray array) {
        int length = array.length();
        float[] floatArray = new float[length];
        for (int i = 0; i < length; i++) {
            floatArray[i] = array.getFloat(i, Float.NaN);
        }
        array.recycle();
        return floatArray;
    }
}
