package com.example.myapplication.utils;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LogUtils {

    private static final String LOG_DIR = "logs";
    private static final String LOG_FILE = "app_log.txt";

    public static void logInfo(Context context, String tag, String message) {
        Log.i(tag, message);
        writeLog(context, "INFO", tag, message, null);
    }

    public static void logWarning(Context context, String tag, String message) {
        Log.w(tag, message);
        writeLog(context, "WARN", tag, message, null);
    }

    public static void logError(Context context, String tag, String message, Exception e) {
        Log.e(tag, message, e);
        writeLog(context, "ERROR", tag, message, e);
    }

    public static File getLogFile(Context context) {
        File dir = new File(context.getFilesDir(), LOG_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, LOG_FILE);
    }

    private static void writeLog(Context context, String level, String tag, String message, Exception e) {
        File logFile = getLogFile(context);
        try {
            FileWriter writer = new FileWriter(logFile, true);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String time = sdf.format(new Date());
            writer.write(String.format("[%s] %s/%s: %s\n", time, level, tag, message));
            if (e != null) {
                writer.write(String.format("Exception: %s\n", e.toString()));
                for (StackTraceElement ste : e.getStackTrace()) {
                    writer.write(String.format("\tat %s\n", ste.toString()));
                }
            }
            writer.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
