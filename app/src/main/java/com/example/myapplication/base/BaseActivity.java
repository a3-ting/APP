package com.example.myapplication.base;
import com.example.myapplication.ui.auth.LoginActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseActivity extends AppCompatActivity {
    
    private static final String TAG = "BaseActivity";
    
    protected int userId = -1;
    protected int userType = -1;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: " + getClass().getSimpleName());
        
        try {
            extractIntentData();
        } catch (Exception e) {
            Log.e(TAG, "Failed to extract intent data", e);
            handleIntentError("页面数据加载失败");
        }
    }
    
    private void extractIntentData() {
        Intent intent = getIntent();
        if (intent == null) {
            Log.w(TAG, "Intent is null in " + getClass().getSimpleName());
            throw new IllegalStateException("Intent is null");
        }
        
        userId = intent.getIntExtra("user_id", -1);
        userType = intent.getIntExtra("user_type", -1);
        
        Log.d(TAG, String.format("%s: userId=%d, userType=%d", 
                getClass().getSimpleName(), userId, userType));
    }
    
    protected boolean validateUserData() {
        return validateUserData(true);
    }
    
    protected boolean validateUserData(boolean requireLogin) {
        if (userId == -1 || userType == -1) {
            Log.w(TAG, "Invalid user data in " + getClass().getSimpleName() + 
                    ": userId=" + userId + ", userType=" + userType);
            
            if (requireLogin) {
                handleIntentError("用户信息无效，请重新登录");
            }
            return false;
        }
        return true;
    }
    
    protected void handleIntentError(String message) {
        safeShowToast(message);
        if (!isFinishing()) {
            navigateBack();
        }
    }
    
    protected void navigateBack() {
        if (!isFinishing()) {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStack();
            } else {
                finish();
            }
        }
    }
    
    protected void navigateBackOrLogin() {
        if (!isFinishing()) {
            if (userId == -1) {
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
                              Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
            finish();
        }
    }
    
    protected void safeFinish() {
        if (!isFinishing() && !isDestroyed()) {
            finish();
        }
    }
    
    protected void safeShowToast(String message) {
        if (!isFinishing() && !isDestroyed()) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }
    
    protected void safeShowToast(String message, int duration) {
        if (!isFinishing() && !isDestroyed()) {
            Toast.makeText(this, message, duration).show();
        }
    }
    
    protected boolean isActivityActive() {
        return !isFinishing() && !isDestroyed();
    }
    
    protected void safeRunOnUiThread(Runnable runnable) {
        if (isActivityActive()) {
            runOnUiThread(runnable);
        }
    }
    
    protected void handleException(Exception e, String errorMessage) {
        Log.e(TAG, "Exception occurred", e);
        if (isActivityActive()) {
            safeShowToast(errorMessage + ": " + e.getMessage());
        }
    }
    
    protected void handleExceptionWithFallback(Exception e, String errorMessage) {
        Log.e(TAG, "Exception occurred, falling back", e);
        if (isActivityActive()) {
            safeShowToast(errorMessage + "，正在返回...");
            navigateBack();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: " + getClass().getSimpleName());
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: " + getClass().getSimpleName());
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: " + getClass().getSimpleName());
    }
}
