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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.service.quicksettings.TileService;
import android.view.ContextThemeWrapper;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

public class LogTile extends TileService {
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onTileAdded() {
        super.onTileAdded();
    }

    @Override
    public void onTileRemoved() {
        super.onTileRemoved();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
    }

    public Dialog logDialog() {
        CharSequence[] options = new CharSequence[] {
                "Logcat", "LogcatRadio", "Dmesg" };
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                new ContextThemeWrapper(this, R.style.AlertDialogTheme));
        alertDialog.setTitle(R.string.quick_settings_log_tile_dialog_title);
        alertDialog.setItems(options, (dialog, which) -> {
            if (SuShell.detectValidSuInPath()) {
                switch (which) {
                    case 0:
                        new CreateLogTask(LogTile.this).execute(true, false, false);
                        break;
                    case 1:
                        new CreateLogTask(LogTile.this).execute(false, true, false);
                        break;
                    case 2:
                        new CreateLogTask(LogTile.this).execute(false, false, true);
                        break;
                }
            } else {
                Toast.makeText(LogTile.this,
                        R.string.cannot_get_su, Toast.LENGTH_SHORT).show();
            }
        });
        return alertDialog.create();
    }

    public static void makeLogcat() throws SuShell.SuDeniedException, IOException {
        final String LOGCAT_FILE = new File(Environment
            .getExternalStorageDirectory(), "LogCat.txt").getAbsolutePath();
        String command = "logcat -d";
        command += " > " + LOGCAT_FILE;
        SuShell.runWithSuCheck(command);
    }

    public static void makeLogcatRadio() throws SuShell.SuDeniedException, IOException {
        final String LOGCAT_RADIO_FILE = new File(Environment
            .getExternalStorageDirectory(), "LogcatRadio.txt").getAbsolutePath();
        String command = "logcat -d -b radio";
        command += " > " + LOGCAT_RADIO_FILE;
        SuShell.runWithSuCheck(command);
    }

    public static void makeDmesg() throws SuShell.SuDeniedException, IOException {
        final String DMESG_FILE = new File(Environment
            .getExternalStorageDirectory(), "Dmesg.txt").getAbsolutePath();
        String command = "dmesg -T";
        command += " > " + DMESG_FILE;
        SuShell.runWithSuCheck(command);
    }

    private static class CreateLogTask extends AsyncTask<Boolean, Void, Void> {
        private Exception mException = null;
        private final WeakReference<Context> mContext;

        CreateLogTask(Context context) {
            mContext = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Boolean... params) {
            try {
                if (params[0]) {
                    makeLogcat();
                }
                if (params[1]) {
                    makeLogcatRadio();
                }
                if (params[2]) {
                    makeDmesg();
                }
            } catch (SuShell.SuDeniedException e) {
                mException = e;
            } catch (IOException e) {
                e.printStackTrace();
                mException = e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if (mException instanceof SuShell.SuDeniedException) {
                Toast.makeText(mContext.get(), R.string.cannot_get_su,
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onClick() {
        super.onClick();
        showDialog(logDialog());
    }
}
