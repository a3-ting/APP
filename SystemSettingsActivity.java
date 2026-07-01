package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.widget.SwitchCompat;
import com.example.myapplication.utils.ToastUtils;

public class SystemSettingsActivity extends AppCompatActivity {

    private SwitchCompat swNotifications;
    private SwitchCompat swDarkMode;
    private SwitchCompat swAutoBackup;
    private Button btnClearCache;
    private Button btnResetSettings;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_settings);

        int userType = getIntent().getIntExtra("user_type", -1);
        if (userType != 2) { // 2: 管理员
            ToastUtils.showShort(this, "权限不足，只有管理员可以访问");
            finish();
            return;
        }

        initViews();
        initSharedPreferences();
        loadSettings();
        setupListeners();
    }

    private void initViews() {
        swNotifications = findViewById(R.id.sw_notifications);
        swDarkMode = findViewById(R.id.sw_dark_mode);
        swAutoBackup = findViewById(R.id.sw_auto_backup);
        btnClearCache = findViewById(R.id.btn_clear_cache);
        btnResetSettings = findViewById(R.id.btn_reset_settings);
        Button btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());
    }

    private void initSharedPreferences() {
        sharedPreferences = getSharedPreferences("system_settings", MODE_PRIVATE);
    }

    private void loadSettings() {
        swNotifications.setChecked(sharedPreferences.getBoolean("notifications", true));
        swDarkMode.setChecked(sharedPreferences.getBoolean("dark_mode", false));
        swAutoBackup.setChecked(sharedPreferences.getBoolean("auto_backup", true));
    }

    private void setupListeners() {
        swNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("notifications", isChecked).apply();
            ToastUtils.showShort(this, "通知设置已更新");
        });

        swDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("dark_mode", isChecked).apply();
            ToastUtils.showShort(this, "主题设置已更新");
            // 实际应用中应该在这里切换主题
        });

        swAutoBackup.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("auto_backup", isChecked).apply();
            ToastUtils.showShort(this, "自动备份设置已更新");
        });

        btnClearCache.setOnClickListener(v -> {
            // 清除缓存
            clearCache();
            ToastUtils.showShort(this, "缓存已清除");
        });

        btnResetSettings.setOnClickListener(v -> {
            // 重置所有设置
            resetSettings();
            loadSettings();
            ToastUtils.showShort(this, "设置已重置为默认值");
        });
    }

    private void clearCache() {
        // 实际应用中应该在这里清除缓存
        // 例如：删除临时文件、清理数据库缓存等
    }

    private void resetSettings() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.putBoolean("notifications", true);
        editor.putBoolean("dark_mode", false);
        editor.putBoolean("auto_backup", true);
        editor.apply();
    }
}