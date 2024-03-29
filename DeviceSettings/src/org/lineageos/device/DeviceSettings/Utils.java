/*
* Copyright (C) 2013 The OmniROM Project
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
import android.content.Intent;
import android.os.UserHandle;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;

public class Utils {
    private static boolean mServiceEnabled = false;

    private static void startService(Context context) {
        context.startServiceAsUser(new Intent(context, AutoHighBrightnessModeService.class),
                UserHandle.CURRENT);
        mServiceEnabled = true;
    }

    private static void stopService(Context context) {
        mServiceEnabled = false;
        context.stopServiceAsUser(new Intent(context, AutoHighBrightnessModeService.class),
                UserHandle.CURRENT);
    }

    public static void enableService(Context context) {
        if (DeviceSettingsFragment.isHBMAutobrightnessEnabled(context) && !mServiceEnabled) {
            startService(context);
        } else if (!DeviceSettingsFragment.isHBMAutobrightnessEnabled(context) && mServiceEnabled) {
            stopService(context);
        }
        context.startService(new Intent(context, KeyHandler.class));
    }

    /**
     * Write a string value to the specified file.
     * @param filename      The filename
     * @param value         The value
     */
    public static void writeValue(String filename, String value) {
        if (filename == null) {
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(filename);
            fos.write(value.getBytes());
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String readLine(String filename) {
        if (filename == null) {
            return null;
        }
        BufferedReader br = null;
        String line;
        try {
            br = new BufferedReader(new FileReader(filename), 1024);
            line = br.readLine();
        } catch (IOException e) {
            return null;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        return line;
    }

    public static String getFileValue(String filename, String defValue) {
        String fileValue = readLine(filename);
        if(fileValue!=null){
            return fileValue;
        }
        return defValue;
    }
}
