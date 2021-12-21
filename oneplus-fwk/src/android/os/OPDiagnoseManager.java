package android.os;

import android.app.job.JobInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Telephony;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.oneplus.android.context.IOneplusContext;
import com.oneplus.android.context.OneplusContext;
import java.util.Timer;
import java.util.TimerTask;

public class OPDiagnoseManager {
    private static final String ACTION_OPDIAGNOSE_GET_IMEI = "android.os.OPDIAGNOSE_GET_INFO";
    private static final boolean DEBUG = false;
    public static int ISSUE_ANSWER_FAIL_NET = 82;
    public static int ISSUE_APK_INSTALL_FAIL = 6;
    public static int ISSUE_AUDIOTRACK_CREATE_FAIL = 47;
    public static int ISSUE_AUDIO_ADSP_FAIL = 52;
    public static int ISSUE_AUDIO_LOAD_HWMODULES_FAIL = 91;
    public static int ISSUE_AUDIO_PLAYBACK_FAIL = 92;
    public static int ISSUE_AUDIO_RECORD_FAIL = 93;
    public static int ISSUE_AUDIO_ROUTE_FAIL = 90;
    public static int ISSUE_AUDIO_SET_VOLUME_FAIL = 96;
    public static int ISSUE_AUDIO_STREAM_MISUSE = 49;
    public static int ISSUE_AUDIO_STREAM_MISUSE2 = 50;
    public static int ISSUE_AUDIO_STREAM_MISUSE3 = 51;
    public static int ISSUE_AUDIO_STREAM_MISUSE4 = 84;
    public static int ISSUE_AUDIO_VOICE_CALL_FAIL = 94;
    public static int ISSUE_AUDIO_VOIP_FAIL = 95;
    public static int ISSUE_BATTER_ERROR = 57;
    public static int ISSUE_BLOCK_SYSTEM_SERVER = 4;
    public static int ISSUE_BMS_BATTERY_HEALTH = 89;
    public static int ISSUE_BT_PAIR_FAILED = 45;
    public static int ISSUE_CANNOT_USE_4G_NETWORK = 73;
    public static int ISSUE_CANNOT_USE_4G_SIM = 76;
    public static int ISSUE_CHARGER_INSERT = 63;
    public static int ISSUE_CHARGER_REMOVE = 64;
    public static int ISSUE_CHARGE_ABNORMAL_STOP = 61;
    public static int ISSUE_CHARGE_CURRENT_LOW = 60;
    public static int ISSUE_CHARGE_CYCLE = 59;
    public static int ISSUE_CHARGE_STOP = 58;
    public static int ISSUE_CRASH_APP = 31;
    public static int ISSUE_CRASH_MODEM = 3;
    public static int ISSUE_CRASH_SYSTEM_SERVER = 2;
    public static int ISSUE_CURRENT_STANDBY = 5;
    public static int ISSUE_DASH_CHARGE_ERROR = 62;
    public static int ISSUE_DASH_FAIL = 37;
    public static int ISSUE_DISPLAY_ELECTROSTATIC_DETECTION = 85;
    public static int ISSUE_DROP_CALL_MO = 65;
    public static int ISSUE_DROP_CALL_MT_CSFB = 66;
    public static int ISSUE_DROP_CALL_MT_DISCONNECT = 72;
    public static int ISSUE_DROP_CALL_MT_NW_REJECT = 67;
    public static int ISSUE_DROP_CALL_MT_PAGING = 71;
    public static int ISSUE_DROP_CALL_MT_RACH = 69;
    public static int ISSUE_DROP_CALL_MT_RLF = 70;
    public static int ISSUE_DROP_CALL_MT_RRC = 68;
    public static int ISSUE_DROP_CALL_WEAK_SIGNAL = 81;
    public static int ISSUE_FAKE_BS = 79;
    public static int ISSUE_FP_DIE = 10;
    public static int ISSUE_FP_HW_ERROR = 12;
    public static int ISSUE_FP_RESET_BYHM = 11;
    public static int ISSUE_GPS_LOCATION_FAILED = 42;
    public static int ISSUE_GPS_SIGNAL_LOW = 86;
    public static int ISSUE_HEAT_CAMERA = 28;
    public static int ISSUE_HEAT_CAMERA_WHEN_CHARGE = 27;
    public static int ISSUE_HEAT_CPU_LOAD = 25;
    public static int ISSUE_HEAT_PLAY_WHEN_CHARGE = 24;
    public static int ISSUE_HEAT_PLAY_WHEN_DASH = 23;
    public static int ISSUE_HEAT_UNKNOWN_REASON = 26;
    public static int ISSUE_KERNEL_PANIC = 22;
    public static int ISSUE_LAG = 30;
    public static int ISSUE_LAG_SOUND = 33;
    public static int ISSUE_LOSE_SIMCARD = 29;
    public static int ISSUE_NETWORK_DATA_DISCONNECT = 78;
    public static int ISSUE_NETWORK_DISCONNECT = 13;
    public static int ISSUE_NFC_ESE_LOCKED = 43;
    public static int ISSUE_NFC_POWER_CONSUMPTION = 44;
    public static int ISSUE_NO_DATA_APN = 77;
    public static int ISSUE_NO_SERVICE_DENIED = 74;
    public static int ISSUE_NO_SERVICE_ERR_LOGIN_FAILED = 75;
    public static int ISSUE_NO_SIGNAL = 14;
    public static int ISSUE_OTA_FAIL = 7;
    public static int ISSUE_POWER_ALARM_WAKEUP = 19;
    public static int ISSUE_POWER_DOWNLOAD = 16;
    public static int ISSUE_POWER_HW_SUBSYSTEM = 20;
    public static int ISSUE_POWER_MODEM_WAKEUP = 18;
    public static int ISSUE_POWER_MUSIC = 15;
    public static int ISSUE_POWER_NETWORK_WAKEUP = 54;
    public static int ISSUE_POWER_NO_SIGNAL = 55;
    public static int ISSUE_POWER_OTHER = 21;
    public static int ISSUE_POWER_TELECOM_SHORT_MESSAGE = 56;
    public static int ISSUE_POWER_WIFI_WAKEUP = 17;
    public static int ISSUE_POWRE_NO_SIGNAL_INTERSECTION = 83;
    public static int ISSUE_RECORD_AUDIO = 35;
    public static int ISSUE_RECORD_INPUT_BE_OPENED = 48;
    public static int ISSUE_RECORD_VIDEO = 34;
    public static int ISSUE_REGISTER_SOUND = 36;
    public static int ISSUE_RESTART_UNKNOWN_REASON = 8;
    public static int ISSUE_ROOT = 32;
    public static int ISSUE_RTC = 87;
    public static int ISSUE_RTC_UPLOAD = 88;
    public static int ISSUE_SUBSYSTEM_ERROR = 9;
    public static int ISSUE_TOTAL_RESTART = 1;
    public static int ISSUE_WIFI_CONN_FAIL = 38;
    public static int ISSUE_WIFI_DISCONNECT = 40;
    public static int ISSUE_WIFI_OPEN_CLOSE_FAIL = 46;
    public static int ISSUE_WIFI_OPEN_FAIL = 39;
    public static int ISSUE_WIFI_SUSPEND_FAILED = 41;
    public static int ISSUE_WIFI_SYMBOL_ERROR = 53;
    public static int ISSUS_ONLY_FAKE_BS = 80;
    private static final String TAG = "OPDiagnoseManager";
    private static String mImei = "***************";
    private static TelephonyManager mTelephonyManager;
    private static final Object sLock = new Object();
    private static boolean sNativeClassInited = false;
    private static IOnePlusDiagnoseUtils sOnePlusDiagnoseUtils;
    private static Timer timer;
    private final Context mContext;
    private final Looper mMainLooper;
    private final long mNativeInstance;
    private BroadcastReceiver mReceiver;

    private static native int nativeAddIssueCount(long j, int i, int i2);

    private static native void nativeClassInit();

    private static native long nativeCreate(String str);

    private static native int nativeReadDiagData(long j, int i);

    private static native int nativeSaveDiagLog(long j, int i);

    private static native int nativeSaveQxdmLog(long j, int i, String str);

    private static native int nativeSetDiagData(long j, int i, String str, int i2);

    private static native int nativeSetIssueNumber(long j, int i, int i2);

    private static native int nativeWriteDiagData(long j, int i, String str);

    public static boolean verify(String elalts, String esalvc) {
        initInstance();
        IOnePlusDiagnoseUtils iOnePlusDiagnoseUtils = sOnePlusDiagnoseUtils;
        if (iOnePlusDiagnoseUtils != null) {
            return iOnePlusDiagnoseUtils.verify(elalts, esalvc);
        }
        return false;
    }

    private void registerClientReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_OPDIAGNOSE_GET_IMEI);
        mReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d(OPDiagnoseManager.TAG, "Received action: " + action);
                if (!action.equals(OPDiagnoseManager.ACTION_OPDIAGNOSE_GET_IMEI)) {
                    return;
                }
                if (OPDiagnoseManager.verify(Long.toString(intent.getLongExtra("timeStamp", 0)), (String) intent.getExtra("verifyCode"))) {
                    setResult(1, OPDiagnoseManager.mImei, null);
                } else {
                    setResult(1, "***************", null);
                }
            }
        };
        if (DEBUG) {
            Log.d(TAG, "register receiver");
        }
        mContext.registerReceiver(mReceiver, filter);
    }

    private void unRegisterClientReceiver() {
        BroadcastReceiver broadcastReceiver;
        Context context = mContext;
        if (!(context == null || (broadcastReceiver = mReceiver) == null)) {
            context.unregisterReceiver(broadcastReceiver);
        }
        mReceiver = null;
    }

    private static void initInstance() {
        if (sOnePlusDiagnoseUtils == null) {
            sOnePlusDiagnoseUtils = (IOnePlusDiagnoseUtils) OneplusContext.queryInterface(IOneplusContext.EType.ONEPLUS_OPDIAGNOSEUTILS);
        }
    }

    public OPDiagnoseManager(Context context, Looper mainLooper) {
        boolean z;
        synchronized (sLock) {
            z = true;
            if (!sNativeClassInited) {
                sNativeClassInited = true;
                nativeClassInit();
            }
        }
        mMainLooper = mainLooper;
        mContext = context;
        mNativeInstance = nativeCreate(context.getOpPackageName());
        int uid = Process.myUid();
        String packagename = context.getPackageManager().getNameForUid(uid);
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("check reason = ");
            sb.append(mImei == null);
            sb.append("/");
            sb.append("***************".equals(mImei));
            sb.append("/");
            sb.append(timer != null ? false : z);
            Log.d(TAG, sb.toString());
            Log.d(TAG, "uid = " + uid + ", pkgName = " + packagename);
        }
        if (!packagename.contains("android.uid.system")) {
            return;
        }
        if ("***************".equals(mImei) || timer == null) {
            if (DEBUG) {
                Log.d(TAG, Telephony.BaseMmsColumns.START);
            }
            if (timer == null) {
                Timer timer2 = new Timer();
                timer = timer2;
                timer2.scheduleAtFixedRate(new RefreshTask(), 0, JobInfo.MIN_BACKOFF_MILLIS);
            }
        }
    }

    class RefreshTask extends TimerTask {
        RefreshTask() {
        }

        @Override
        public void run() {
            OPDiagnoseManager oPDiagnoseManager = OPDiagnoseManager.this;
            String unused = OPDiagnoseManager.mImei = oPDiagnoseManager.getImei1(oPDiagnoseManager.mContext);
            if (OPDiagnoseManager.mImei != null && !"***************".equals(OPDiagnoseManager.mImei)) {
                try {
                    OPDiagnoseManager.timer.cancel();
                    Timer unused2 = OPDiagnoseManager.timer = null;
                    if (OPDiagnoseManager.DEBUG) {
                        Log.d(OPDiagnoseManager.TAG, "retry finished");
                    }
                } catch (Exception e) {
                }
                OPDiagnoseManager.this.registerClientReceiver();
            }
        }
    }

    private static TelephonyManager getTelephonyManager(Context context) {
        if (mTelephonyManager == null) {
            mTelephonyManager = (TelephonyManager) context.getSystemService("phone");
        }
        return mTelephonyManager;
    }

    public String getImei1(Context context) {
        try {
            return getTelephonyManager(context).getImei(0);
        } catch (SecurityException e) {
            Log.e(TAG, e.getMessage());
            return "***************";
        } catch (Exception e2) {
            Log.e(TAG, e2.getMessage());
            return "***************";
        }
    }

    public boolean addIssueCount(int type, int count) {
        return nativeAddIssueCount(mNativeInstance, type, count) == 0;
    }

    public boolean setIssueNumber(int type, int count) {
        return nativeSetIssueNumber(mNativeInstance, type, count) == 0;
    }

    public boolean writeDiagData(int type, String issueDesc) {
        return nativeWriteDiagData(mNativeInstance, type, issueDesc) == 0;
    }

    public boolean setDiagData(int type, String issueDesc, int count) {
        return nativeSetDiagData(mNativeInstance, type, issueDesc, count) == 0;
    }

    public boolean saveDiagLog(int type) {
        return nativeSaveDiagLog(mNativeInstance, type) == 0;
    }

    public boolean saveQxdmLog(int type, String mask_type) {
        return nativeSaveQxdmLog(mNativeInstance, type, mask_type) == 0;
    }

    public boolean readDiagData(int type) {
        return nativeReadDiagData(mNativeInstance, type) == 0;
    }
}
