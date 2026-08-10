package com.example.myapplication.utils;

import android.content.Context;
import android.os.Process;

import java.lang.Thread.UncaughtExceptionHandler;

public class GlobalExceptionHandler implements UncaughtExceptionHandler {

    private static GlobalExceptionHandler instance;
    private Context context;
    private UncaughtExceptionHandler defaultHandler;

    private GlobalExceptionHandler(Context context) {
        this.context = context.getApplicationContext();
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    public static synchronized GlobalExceptionHandler getInstance(Context context) {
        if (instance == null) {
            instance = new GlobalExceptionHandler(context);
        }
        return instance;
    }

    public void init() {
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        LogUtils.logError(context, "GlobalExceptionHandler", "未捕获的异常", new Exception(ex));

        if (defaultHandler != null) {
            defaultHandler.uncaughtException(thread, ex);
        } else {
            Process.killProcess(Process.myPid());
            System.exit(1);
        }
    }
}
