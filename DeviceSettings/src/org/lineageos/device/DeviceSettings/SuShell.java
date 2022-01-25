package org.lineageos.device.DeviceSettings;

import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class SuShell {
    private static final String TAG = SuShell.class.getSimpleName();

    // WARNING: setting this to true will dump passwords to logcat
    // set to false for release
    static final boolean DEBUG = false;

    private SuShell() {
    }

    public static ArrayList<String> runWithSuCheck(String... commands) throws SuDeniedException {
        String suTestScript = "#!/system/bin/sh\necho ";
        String suTestScriptValid = "crDroidSuPermsOk";

        String[] commandsWithCheck = new String[commands.length+1];
        commandsWithCheck[0] = suTestScript + suTestScriptValid;
        System.arraycopy(commands, 0, commandsWithCheck, 1, commands.length);

        ArrayList<String> output = runWithSu(commandsWithCheck);
        if (output.size() >= 1
                && output.get(0).trim().equals(suTestScriptValid)) {
            if (DEBUG) {
                Log.d(TAG, "Superuser command auth confirmed");
            }
            output.remove(0);
            return output;
        } else {
            if (DEBUG) {
                Log.d(TAG, "Superuser command auth refused");
            }
            throw new SuDeniedException();
        }
    }

    public static ArrayList<String> runWithSu(String... command) {
        return run("su", command);
    }

    public static ArrayList<String> run(String shell, String... commands) {
        ArrayList<String> output = new ArrayList<>();

        try {
            Process process = Runtime.getRuntime().exec(shell);

            BufferedOutputStream shellInput = new BufferedOutputStream(
                    process.getOutputStream());
            BufferedReader shellOutput = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            for (String command : commands) {
                if (DEBUG) {
                    Log.i(TAG, "command: " + command);
                }
                shellInput.write((command + " 2>&1\n").getBytes());
            }

            shellInput.write("exit\n".getBytes());
            shellInput.flush();

            String line;
            while ((line = shellOutput.readLine()) != null) {
                if (DEBUG) {
                    Log.d(TAG, "command output: " + line);
                }
                output.add(line);
            }

            process.waitFor();
        } catch (IOException e) {
            Log.e(TAG, "IO Error: " + e.getMessage(), e);
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted Error: " + e.getMessage(), e);
            throw new RuntimeException(e);
        }

        return output;
    }

    public static boolean detectValidSuInPath() {
        String env_path = System.getenv("PATH");

        if (env_path != null) {
            String[] pathToTest = env_path.split(":");

            for (String path : pathToTest) {
                File su = new File(path + "/su");
                if (su.exists()) {
                    // should check if it is suid
                    if (DEBUG) {
                        Log.d(TAG, "Found su at " + su.getAbsolutePath());
                    }

                    return true;
                }
            }
        }

        return false;
    }

    public static class SuDeniedException extends Exception {}
}
