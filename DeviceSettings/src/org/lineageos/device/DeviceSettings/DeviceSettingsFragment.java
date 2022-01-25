/*
* Copyright (C) 2016 The OmniROM Project
* Copyright (C) 2020-2022 crDroid Android Project
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*
*/
package org.lineageos.device.DeviceSettings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.MenuItem;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceManager;
import androidx.preference.TwoStatePreference;

public class DeviceSettingsFragment extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener {
    public static final String KEY_HBM_AUTOBRIGHTNESS_SWITCH = "hbm_autobrightness";

    private ListPreference mTopKeyPref;
    private ListPreference mMiddleKeyPref;
    private ListPreference mBottomKeyPref;
    private TwoStatePreference mHBMAutobrightnessSwitch;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.main);
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);

        mTopKeyPref = findPreference(Constants.NOTIF_SLIDER_TOP_KEY);
        if (mTopKeyPref != null) {
            mTopKeyPref.setValueIndex(Constants.getPreferenceInt(getContext(), Constants.NOTIF_SLIDER_TOP_KEY));
            mTopKeyPref.setOnPreferenceChangeListener(this);
        }
        mMiddleKeyPref = findPreference(Constants.NOTIF_SLIDER_MIDDLE_KEY);
        if (mMiddleKeyPref != null) {
            mMiddleKeyPref.setValueIndex(Constants.getPreferenceInt(getContext(), Constants.NOTIF_SLIDER_MIDDLE_KEY));
            mMiddleKeyPref.setOnPreferenceChangeListener(this);
        }
        mBottomKeyPref = findPreference(Constants.NOTIF_SLIDER_BOTTOM_KEY);
        if (mBottomKeyPref != null) {
            mBottomKeyPref.setValueIndex(Constants.getPreferenceInt(getContext(), Constants.NOTIF_SLIDER_BOTTOM_KEY));
            mBottomKeyPref.setOnPreferenceChangeListener(this);
        }
        boolean isAutomaticBrightnessEnabled = Settings.System.getIntForUser(getContext().getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL, UserHandle.USER_CURRENT) == 1;
        mHBMAutobrightnessSwitch = findPreference(KEY_HBM_AUTOBRIGHTNESS_SWITCH);
        if (mHBMAutobrightnessSwitch != null) {
            mHBMAutobrightnessSwitch.setEnabled(isAutomaticBrightnessEnabled);
            mHBMAutobrightnessSwitch.setChecked(PreferenceManager.getDefaultSharedPreferences(
                    getContext()).getBoolean(DeviceSettingsFragment.KEY_HBM_AUTOBRIGHTNESS_SWITCH, false));
            mHBMAutobrightnessSwitch.setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mTopKeyPref) {
            Constants.setPreferenceInt(getContext(), preference.getKey(), Integer.parseInt((String) newValue));
            return true;
        } else if (preference == mMiddleKeyPref) {
            Constants.setPreferenceInt(getContext(), preference.getKey(), Integer.parseInt((String) newValue));
            return true;
        } else if (preference == mBottomKeyPref) {
            Constants.setPreferenceInt(getContext(), preference.getKey(), Integer.parseInt((String) newValue));
            return true;
        } else if (preference == mHBMAutobrightnessSwitch) {
            Boolean enabled = (Boolean) newValue;
            SharedPreferences.Editor prefChange = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
            prefChange.putBoolean(KEY_HBM_AUTOBRIGHTNESS_SWITCH, enabled).apply();
            Utils.enableService(getContext());
            return true;
        }
        return false;
    }

    public static boolean isHBMAutobrightnessEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                DeviceSettingsFragment.KEY_HBM_AUTOBRIGHTNESS_SWITCH, false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            getActivity().finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
