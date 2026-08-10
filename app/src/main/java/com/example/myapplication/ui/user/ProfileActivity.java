package com.example.myapplication.ui.user;

import androidx.appcompat.app.AppCompatActivity;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.database.DBHelper;
import com.example.myapplication.utils.SecurityUtils;
import com.example.myapplication.utils.DialogUtils;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvUserType;
    private EditText etUsername, etName, etStudentId, etClass, etMajor, etPhone, etEmail, etOldPassword, etNewPassword, etConfirmNewPassword;
    private Button btnSave, btnChangePassword, btnBack;
    private DBHelper dbHelper;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        userId = getIntent().getIntExtra("user_id", -1);
        int userType = getIntent().getIntExtra("user_type", -1);
        if (userId == -1) {
            DialogUtils.showErrorDialog(this, "用户信息错误");
            finish();
            return;
        }

        initViews();
        initDatabase();
        loadUserInfo();
        setupListeners();
    }

    private void initViews() {
        tvUserType = findViewById(R.id.tv_user_type);
        etUsername = findViewById(R.id.et_username);
        etName = findViewById(R.id.et_name);
        etStudentId = findViewById(R.id.et_student_id);
        etClass = findViewById(R.id.et_class);
        etMajor = findViewById(R.id.et_major);
        etPhone = findViewById(R.id.et_phone);
        etEmail = findViewById(R.id.et_email);
        etOldPassword = findViewById(R.id.et_old_password);
        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmNewPassword = findViewById(R.id.et_confirm_new_password);
        btnSave = findViewById(R.id.btn_save);
        btnChangePassword = findViewById(R.id.btn_change_password);
        btnBack = findViewById(R.id.btn_back);
    }

    private void initDatabase() {
        dbHelper = new DBHelper(this);
    }

    private void loadUserInfo() {
        Cursor cursor = dbHelper.getUserById(userId);
        if (cursor != null && cursor.moveToFirst()) {
            int usernameIndex = cursor.getColumnIndex("username");
            int nameIndex = cursor.getColumnIndex("name");
            int studentIdIndex = cursor.getColumnIndex("student_id");
            int classNameIndex = cursor.getColumnIndex("class_name");
            int majorIndex = cursor.getColumnIndex("major");
            int phoneIndex = cursor.getColumnIndex("phone");
            int emailIndex = cursor.getColumnIndex("email");
            int typeIndex = cursor.getColumnIndex("type");

            if (usernameIndex != -1) {
                etUsername.setText(cursor.getString(usernameIndex));
            }
            if (nameIndex != -1) {
                etName.setText(cursor.getString(nameIndex));
            }
            if (studentIdIndex != -1) {
                etStudentId.setText(cursor.getString(studentIdIndex));
            }
            if (classNameIndex != -1) {
                etClass.setText(cursor.getString(classNameIndex));
            }
            if (majorIndex != -1) {
                etMajor.setText(cursor.getString(majorIndex));
            }
            if (phoneIndex != -1) {
                String encryptedPhone = cursor.getString(phoneIndex);
                etPhone.setText(SecurityUtils.decryptAES(encryptedPhone));
            }
            if (emailIndex != -1) {
                String encryptedEmail = cursor.getString(emailIndex);
                etEmail.setText(SecurityUtils.decryptAES(encryptedEmail));
            }

            if (typeIndex != -1) {
                int userType = cursor.getInt(typeIndex);
                tvUserType.setText(getUserTypeString(userType));
            }

            cursor.close();
        }
    }

    private String getUserTypeString(int userType) {
        switch (userType) {
            case 0:
                return "学生";
            case 1:
                return "教师";
            case 2:
                return "管理员";
            default:
                return "未知";
        }
    }

    private void setupListeners() {
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUserInfo();
            }
        });

        btnChangePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changePassword();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void saveUserInfo() {
        ContentValues values = new ContentValues();
        values.put("name", etName.getText().toString().trim());
        values.put("student_id", etStudentId.getText().toString().trim());
        values.put("class_name", etClass.getText().toString().trim());
        values.put("major", etMajor.getText().toString().trim());
        // 使用AES加密保护敏感数据
        values.put("phone", SecurityUtils.encryptAES(etPhone.getText().toString().trim()));
        values.put("email", SecurityUtils.encryptAES(etEmail.getText().toString().trim()));

        int result = dbHelper.update("user", values, "id = ?", new String[]{String.valueOf(userId)});
        if (result > 0) {
            Toast.makeText(this, "信息更新成功", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "信息更新失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void changePassword() {
        String oldPassword = etOldPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmNewPassword = etConfirmNewPassword.getText().toString().trim();

        if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmNewPassword.isEmpty()) {
            Toast.makeText(this, "请输入所有密码字段", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPassword.equals(confirmNewPassword)) {
            Toast.makeText(this, "两次输入的新密码不一致", Toast.LENGTH_SHORT).show();
            return;
        }

        // 验证旧密码是否正确
        Cursor cursor = dbHelper.getUserById(userId);
        if (cursor != null && cursor.moveToFirst()) {
            int passwordIndex = cursor.getColumnIndex("password");
            if (passwordIndex != -1) {
                String storedPassword = cursor.getString(passwordIndex);
                // 检查旧密码是否正确
                if (SecurityUtils.verifyPassword(oldPassword, storedPassword)) {
                    // 更新密码
                    ContentValues values = new ContentValues();
                    values.put("password", SecurityUtils.encryptPassword(newPassword));
                    int result = dbHelper.update("user", values, "id = ?", new String[]{String.valueOf(userId)});
                    if (result > 0) {
                        Toast.makeText(this, "密码修改成功", Toast.LENGTH_SHORT).show();
                        // 清空密码输入框
                        etOldPassword.setText("");
                        etNewPassword.setText("");
                        etConfirmNewPassword.setText("");
                    } else {
                        Toast.makeText(this, "密码修改失败", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "旧密码错误", Toast.LENGTH_SHORT).show();
                }
            }
            cursor.close();
        } else {
            Toast.makeText(this, "用户信息获取失败", Toast.LENGTH_SHORT).show();
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