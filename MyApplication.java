package com.example.myapplication;

import android.app.Application;
import android.util.Log;

import com.example.myapplication.utils.GlobalExceptionHandler;

public class MyApplication extends Application {
    
    private static final String TAG = "MyApplication";
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Application onCreate");
        
        // 初始化全局异常处理器
        initGlobalExceptionHandler();
        
        // 其他初始化操作
    }
    
    private void initGlobalExceptionHandler() {
        try {
            GlobalExceptionHandler handler = GlobalExceptionHandler.getInstance(this);
            handler.init();
            Log.d(TAG, "GlobalExceptionHandler initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize GlobalExceptionHandler", e);
        }
    }
}
