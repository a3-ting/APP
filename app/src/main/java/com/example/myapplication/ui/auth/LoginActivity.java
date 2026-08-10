package com.example.myapplication.ui.auth;
import com.example.myapplication.ui.main.MainActivity;

import androidx.appcompat.app.AppCompatActivity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.util.Log;

import com.example.myapplication.database.DBHelper;
import com.example.myapplication.utils.SecurityUtils;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private EditText etUsername, etPassword;
    private Button btnLogin, btnRegister;
    private DBHelper dbHelper;
    private boolean isInitializing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_login);
            Log.d(TAG, "Layout loaded successfully");
            
            initViews();
            initDatabase();
            setupListeners();
            initializeDefaultAccounts();
            
            Log.d(TAG, "LoginActivity initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing LoginActivity", e);
            Toast.makeText(this, "应用初始化失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initViews() {
        try {
            etUsername = findViewById(R.id.et_username);
            etPassword = findViewById(R.id.et_password);
            btnLogin = findViewById(R.id.btn_login);
            btnRegister = findViewById(R.id.btn_register);
            
            if (etUsername == null || etPassword == null || btnLogin == null || btnRegister == null) {
                throw new RuntimeException("布局中的必要控件未找到");
            }
            
            Log.d(TAG, "Views initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            throw new RuntimeException("视图初始化失败: " + e.getMessage());
        }
    }

    private void initDatabase() {
        try {
            dbHelper = new DBHelper(this);
            Log.d(TAG, "Database initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing database", e);
            throw new RuntimeException("数据库初始化失败: " + e.getMessage());
        }
    }

    private void setupListeners() {
        try {
            if (btnLogin != null) {
                btnLogin.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        login();
                    }
                });
            }
            
            if (btnRegister != null) {
                btnRegister.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
                        } catch (Exception e) {
                            Log.e(TAG, "Error starting RegisterActivity", e);
                            Toast.makeText(LoginActivity.this, "无法打开注册页面", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
            
            Log.d(TAG, "Listeners set up successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up listeners", e);
        }
    }

    private void login() {
        try {
            if (isInitializing) {
                Toast.makeText(this, "正在初始化，请稍候...", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (etUsername == null || etPassword == null) {
                Toast.makeText(this, "界面加载中，请稍候", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "请输入用户名和密码", Toast.LENGTH_SHORT).show();
                return;
            }

            if (dbHelper == null) {
                Toast.makeText(this, "数据库未初始化，请重启应用", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                Cursor adminCursor = dbHelper.getAdminByUsername(username);
                if (adminCursor != null && adminCursor.moveToFirst()) {
                    try {
                        int passwordIndex = adminCursor.getColumnIndex("password");
                        int idIndex = adminCursor.getColumnIndex("id");
                        
                        if (passwordIndex != -1 && idIndex != -1) {
                            String storedPassword = adminCursor.getString(passwordIndex);
                            int adminId = adminCursor.getInt(idIndex);
                            
                            if (storedPassword != null && SecurityUtils.verifyPassword(password, storedPassword)) {
                                Toast.makeText(this, "管理员登录成功", Toast.LENGTH_SHORT).show();
                                navigateToMainActivity(adminId, 2);
                                return;
                            } else {
                                Toast.makeText(this, "密码错误", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                    } finally {
                        adminCursor.close();
                    }
                }

                Cursor userCursor = dbHelper.getUserByUsername(username);
                if (userCursor != null && userCursor.moveToFirst()) {
                    try {
                        int passwordIndex = userCursor.getColumnIndex("password");
                        int idIndex = userCursor.getColumnIndex("id");
                        int typeIndex = userCursor.getColumnIndex("type");
                        
                        if (passwordIndex != -1 && idIndex != -1 && typeIndex != -1) {
                            String storedPassword = userCursor.getString(passwordIndex);
                            int userId = userCursor.getInt(idIndex);
                            int userType = userCursor.getInt(typeIndex);

                            if (storedPassword != null && SecurityUtils.verifyPassword(password, storedPassword)) {
                                Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show();
                                navigateToMainActivity(userId, userType);
                                return;
                            } else {
                                Toast.makeText(this, "密码错误", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        } else {
                            Toast.makeText(this, "用户账号信息不完整", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } finally {
                        userCursor.close();
                    }
                } else {
                    if (userCursor != null) {
                        userCursor.close();
                    }
                    Toast.makeText(this, "用户不存在", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "Login process error", e);
                Toast.makeText(this, "登录过程中发生错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Login initialization error", e);
            Toast.makeText(this, "登录初始化失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToMainActivity(int userId, int userType) {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("user_id", userId);
        intent.putExtra("user_type", userType);
        startActivity(intent);
        finish();
    }

    private void initializeDefaultAccounts() {
        if (isInitializing) {
            return;
        }
        
        isInitializing = true;
        
        try {
            if (dbHelper == null) {
                Log.e(TAG, "Cannot initialize accounts: dbHelper is null");
                return;
            }
            
            Log.d(TAG, "Starting default account initialization...");
            
            Cursor studentCursor = null;
            try {
                studentCursor = dbHelper.getUserByUsername("student");
                if (studentCursor == null || !studentCursor.moveToFirst()) {
                    if (studentCursor != null) {
                        studentCursor.close();
                        studentCursor = null;
                    }
                    ContentValues studentValues = new ContentValues();
                    studentValues.put("username", "student");
                    studentValues.put("password", SecurityUtils.encryptPassword("student123"));
                    studentValues.put("name", "学生账号");
                    studentValues.put("type", 0); // 学生
                    studentValues.put("student_id", "2024001");
                    studentValues.put("class_name", "计算机1班");
                    studentValues.put("major", "计算机科学与技术");
                    long result = dbHelper.insertUser(studentValues);
                    Log.d(TAG, "Student account created: " + (result > 0 ? "success" : "failed"));
                }
            } finally {
                if (studentCursor != null) {
                    studentCursor.close();
                }
            }
            
            Cursor teacherCursor = null;
            try {
                teacherCursor = dbHelper.getUserByUsername("teacher");
                if (teacherCursor == null || !teacherCursor.moveToFirst()) {
                    if (teacherCursor != null) {
                        teacherCursor.close();
                        teacherCursor = null;
                    }
                    ContentValues teacherValues = new ContentValues();
                    teacherValues.put("username", "teacher");
                    teacherValues.put("password", SecurityUtils.encryptPassword("teacher123"));
                    teacherValues.put("name", "教师账号");
                    teacherValues.put("type", 1); // 教师
                    long result = dbHelper.insertUser(teacherValues);
                    Log.d(TAG, "Teacher account created: " + (result > 0 ? "success" : "failed"));
                }
            } finally {
                if (teacherCursor != null) {
                    teacherCursor.close();
                }
            }
            
            Cursor adminCursor = null;
            try {
                adminCursor = dbHelper.getAdminByUsername("admin");
                if (adminCursor == null || !adminCursor.moveToFirst()) {
                    if (adminCursor != null) {
                        adminCursor.close();
                        adminCursor = null;
                    }
                    ContentValues adminValues = new ContentValues();
                    adminValues.put("username", "admin");
                    adminValues.put("password", SecurityUtils.encryptPassword("admin123"));
                    adminValues.put("name", "系统管理员");
                    long result = dbHelper.insertAdmin(adminValues);
                    Log.d(TAG, "Admin account created: " + (result > 0 ? "success" : "failed"));
                }
            } finally {
                if (adminCursor != null) {
                    adminCursor.close();
                }
            }
            
            Log.d(TAG, "Default account initialization completed");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing default accounts", e);
        } finally {
            isInitializing = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}