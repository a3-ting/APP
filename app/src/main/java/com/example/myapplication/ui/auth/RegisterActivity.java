package com.example.myapplication.ui.auth;

import androidx.appcompat.app.AppCompatActivity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.myapplication.database.DBHelper;
import com.example.myapplication.utils.SecurityUtils;

public class RegisterActivity extends AppCompatActivity {

    private EditText etUsername, etPassword, etConfirmPassword, etName, etStudentId, etClass, etMajor, etPhone, etEmail;
    private Button btnRegister, btnBack;
    private Spinner spinnerUserType;
    private DBHelper dbHelper;
    private int selectedUserType = 0; // 默认为学生

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initViews();
        initDatabase();
        setupUserTypeSpinner();
        setupListeners();
    }

    private void initViews() {
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        etName = findViewById(R.id.et_name);
        etStudentId = findViewById(R.id.et_student_id);
        etClass = findViewById(R.id.et_class);
        etMajor = findViewById(R.id.et_major);
        etPhone = findViewById(R.id.et_phone);
        etEmail = findViewById(R.id.et_email);
        btnRegister = findViewById(R.id.btn_register);
        btnBack = findViewById(R.id.btn_back);
        spinnerUserType = findViewById(R.id.spinner_user_type);
    }

    private void initDatabase() {
        dbHelper = new DBHelper(this);
    }

    private void setupUserTypeSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.user_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUserType.setAdapter(adapter);
        
        spinnerUserType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedUserType = position; // 0: 学生, 1: 教师
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedUserType = 0; // 默认为学生
            }
        });
    }

    private void setupListeners() {
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                register();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void register() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String name = etName.getText().toString().trim();
        String studentId = etStudentId.getText().toString().trim();
        String className = etClass.getText().toString().trim();
        String major = etMajor.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || name.isEmpty()) {
            Toast.makeText(this, "请填写必填字段", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "两次输入的密码不一致", Toast.LENGTH_SHORT).show();
            return;
        }

        Cursor cursor = null;
        try {
            cursor = dbHelper.getUserByUsername(username);
            if (cursor != null && cursor.moveToFirst()) {
                Toast.makeText(this, "用户名已存在", Toast.LENGTH_SHORT).show();
                return;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("password", SecurityUtils.encryptPassword(password));
        values.put("name", name);
        values.put("type", selectedUserType); // 根据选择设置用户类型
        
        // 如果是学生，则保存学生相关信息
        if (selectedUserType == 0) { // 学生
            values.put("student_id", studentId);
            values.put("class_name", className);
            values.put("major", major);
        }
        
        // 使用AES加密保护敏感数据
        values.put("phone", SecurityUtils.encryptAES(phone));
        values.put("email", SecurityUtils.encryptAES(email));

        long result = dbHelper.insertUser(values);
        if (result > 0) {
            Toast.makeText(this, "注册成功", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        } else {
            Toast.makeText(this, "注册失败，请重试", Toast.LENGTH_SHORT).show();
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