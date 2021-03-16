package org.lineageos.device.DeviceSettings;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
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

public class AutoHighBrightnessModeService extends Service {
    private static final String HBM_BRIGHTNESS_FILE = "/sys/devices/platform/soc/ae00000.qcom,mdss_mdp/drm/card0/card0-DSI-1/hbm_brightness";
    private static final String HBM_FILE = "/sys/devices/platform/soc/ae00000.qcom,mdss_mdp/drm/card0/card0-DSI-1/hbm";

    private static boolean mAutoHBMSensorEnabled = false;
    private static boolean mAutoHBMActive = false;
    private static boolean mIsGoingToSleep = false;
    private boolean mIsAutomaticBrightnessEnabled;
    private Handler mHandler;
    private Spline mHBMLuxToBacklightSpline;

    private SensorManager mSensorManager;
    Sensor mLightSensor;

    private float getHBMBrightness(float lux) {
        return mHBMLuxToBacklightSpline.interpolate(lux);
    }

    public void activateLightSensorRead() {
        mSensorManager = (SensorManager) getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mSensorManager.registerListener(mSensorEventListener, mLightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        mAutoHBMSensorEnabled = true;
    }

    public void deactivateLightSensorRead() {
        mSensorManager.unregisterListener(mSensorEventListener);
        mAutoHBMSensorEnabled = false;
        mIsGoingToSleep = false;
    }

    SensorEventListener mSensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (mAutoHBMSensorEnabled) {
                float lux = event.values[0];
                if (lux > 20000.0f && !mIsGoingToSleep) {
                    int hbm_brightness = Math.round(getHBMBrightness(lux));
                    Utils.writeValue(HBM_BRIGHTNESS_FILE, String.valueOf(hbm_brightness));
                    mAutoHBMActive = true;
                } else if (lux < 20000.0f && mAutoHBMActive && !mIsAutomaticBrightnessEnabled) {
                    Utils.writeValue(HBM_BRIGHTNESS_FILE, "0");
                    Utils.writeValue(HBM_FILE, "0");
                    mAutoHBMActive = false;
                } else {
                    mAutoHBMActive = false;
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // do nothing
        }
    };

    private BroadcastReceiver mScreenStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                activateLightSensorRead();
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                deactivateLightSensorRead();
            }

            if (intent.getAction().equals(
                    com.android.internal.util.crdroid.content.Intent.ACTION_GO_TO_SLEEP)) {
                mIsGoingToSleep = true;
            }
        }
    };

    @Override
    public void onCreate() {
        mHandler = new Handler(Looper.getMainLooper());
        IntentFilter screenStateFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
        screenStateFilter.addAction(
                com.android.internal.util.crdroid.content.Intent.ACTION_GO_TO_SLEEP);
        registerReceiver(mScreenStateReceiver, screenStateFilter);
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        final Resources res = getApplicationContext().getResources();
        float[] hbm_brightness = getFloatArray(res.obtainTypedArray(R.array.config_HBMBrightnessBacklight));
        float[] hbm_lux = getFloatArray(res.obtainTypedArray(R.array.config_HBMautoBrightnessLevels));
        mHBMLuxToBacklightSpline = Spline.createSpline(hbm_lux, hbm_brightness);
        mCustomSettingsObserver.observe();
        mCustomSettingsObserver.update();
        if (pm.isInteractive()) {
            activateLightSensorRead();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mScreenStateReceiver);
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm.isInteractive()) {
            deactivateLightSensorRead();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private CustomSettingsObserver mCustomSettingsObserver = new CustomSettingsObserver(mHandler);
    private class CustomSettingsObserver extends ContentObserver {

        CustomSettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = getApplicationContext().getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.SCREEN_BRIGHTNESS_MODE),
                    false, this, UserHandle.USER_ALL);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (uri.equals(Settings.System.getUriFor(
                    Settings.System.SCREEN_BRIGHTNESS_MODE))) {
                updateBrightnessMode();
            }
        }

        public void update() {
            updateBrightnessMode();
        }
    }

    private void updateBrightnessMode() {
        mIsAutomaticBrightnessEnabled = Settings.System.getIntForUser(
                getApplicationContext().getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
                UserHandle.USER_CURRENT) == 1;
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
