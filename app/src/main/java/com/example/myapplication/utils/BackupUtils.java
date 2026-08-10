package com.example.myapplication.utils;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BackupUtils {

    private static final String BACKUP_DIR = "backups";
    private static final String DB_NAME = "school_management.db";

    public static String createBackup(Context context) {
        File dbFile = context.getDatabasePath(DB_NAME);
        if (!dbFile.exists()) {
            return null;
        }

        File backupDir = new File(context.getFilesDir(), BACKUP_DIR);
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String backupFileName = "backup_" + sdf.format(new Date()) + ".db";
        File backupFile = new File(backupDir, backupFileName);

        try {
            copyFile(dbFile, backupFile);
            return backupFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean restoreBackup(Context context, String backupPath) {
        File backupFile = new File(backupPath);
        if (!backupFile.exists()) {
            return false;
        }

        File dbFile = context.getDatabasePath(DB_NAME);
        try {
            copyFile(backupFile, dbFile);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static File[] getBackupFiles(Context context) {
        File backupDir = new File(context.getFilesDir(), BACKUP_DIR);
        if (!backupDir.exists()) {
            return new File[0];
        }
        return backupDir.listFiles();
    }

    private static void copyFile(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);
        byte[] buffer = new byte[1024];
        int length;
        while ((length = in.read(buffer)) > 0) {
            out.write(buffer, 0, length);
        }
        in.close();
        out.close();
    }
}
