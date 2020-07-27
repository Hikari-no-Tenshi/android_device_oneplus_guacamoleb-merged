package org.lineageos.device.DeviceSettings;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

public class AutoHighBrightnessModeService extends Service {
    private static final String HBM_BRIGHTNESS_FILE = "/sys/devices/platform/soc/ae00000.qcom,mdss_mdp/drm/card0/card0-DSI-1/hbm_brightness";
    private static final String HBM_FILE = "/sys/devices/platform/soc/ae00000.qcom,mdss_mdp/drm/card0/card0-DSI-1/hbm";

    private final int[][] HBM_AUTOBRIGHTNESS_ARRAY = {
        new int[]{20000, 10},
        new int[]{23000, 40},
        new int[]{26000, 70},
        new int[]{29000, 100},
        new int[]{32000, 150},
        new int[]{35000, 200},
        new int[]{38000, 300},
        new int[]{41000, 400},
        new int[]{44000, 600},
        new int[]{47000, 800},
        new int[]{50000, 1023},
    };

    private static boolean mAutoHBMSensorEnabled = false;
    private static boolean mAutoHBMActive = false;
    private boolean mIsAutomaticBrightnessEnabled;
    private Handler mHandler;

    private SensorManager mSensorManager;
    Sensor mLightSensor;

    private int interpolate(int i, int i2, int i3, int i4, int i5) {
        int i6 = i5 - i4;
        int i7 = i - i2;
        int i8 = ((i6 * 2) * i7) / (i3 - i2);
        int i9 = i8 / 2;
        int i10 = i2 - i3;
        return i4 + i9 + (i8 % 2) + ((i10 == 0 || i6 == 0) ? 0 : (((i7 * 2) * (i - i3)) / i6) / i10);
    }

    private float getHBMBrightness(float lux) {
        int length = HBM_AUTOBRIGHTNESS_ARRAY.length;
        int i = 0;
        while (i < length && HBM_AUTOBRIGHTNESS_ARRAY[i][0] < (int)lux) {
            i++;
        }
        if (i == 0) {
            return HBM_AUTOBRIGHTNESS_ARRAY[0][1];
        }
        if (i == length) {
            return HBM_AUTOBRIGHTNESS_ARRAY[length - 1][1];
        }
        int[][] iArr = HBM_AUTOBRIGHTNESS_ARRAY;
        int i2 = i - 1;
        return interpolate((int)lux, iArr[i2][0], iArr[i][0], iArr[i2][1], iArr[i][1]);
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
    }

    SensorEventListener mSensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (mAutoHBMSensorEnabled) {
                float lux = event.values[0];
                if (lux > 20000.0f) {
                    Utils.writeValue(HBM_BRIGHTNESS_FILE, String.valueOf((int)getHBMBrightness(lux)));
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
        }
    };

    @Override
    public void onCreate() {
        mHandler = new Handler(Looper.getMainLooper());
        IntentFilter screenStateFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mScreenStateReceiver, screenStateFilter);
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
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
}
