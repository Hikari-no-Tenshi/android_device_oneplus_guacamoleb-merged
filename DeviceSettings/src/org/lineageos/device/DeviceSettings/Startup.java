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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class Startup extends BroadcastReceiver {
    private static final String LOCKED_BOOT_COMPLETED = "android.intent.action.LOCKED_BOOT_COMPLETED";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (LOCKED_BOOT_COMPLETED.equals(intent.getAction())) {
            Utils.enableService(context);
        }
    }
}
