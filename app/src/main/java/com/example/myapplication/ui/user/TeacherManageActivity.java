package com.example.myapplication.ui.user;

import androidx.appcompat.app.AppCompatActivity;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.example.myapplication.database.DBHelper;
import com.example.myapplication.utils.SecurityUtils;

import java.util.ArrayList;
import java.util.List;

public class TeacherManageActivity extends AppCompatActivity {

    private ListView lvTeachers;
    private Button btnAddTeacher, btnBack;
    private DBHelper dbHelper;
    private int adminId;
    private List<String> teacherList;
    private List<Integer> teacherIdList;
    private ArrayAdapter<String> teacherAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_manage);

        adminId = getIntent().getIntExtra("user_id", -1);
        int userType = getIntent().getIntExtra("user_type", -1);
        
        if (adminId == -1 || userType != 2) {
            Toast.makeText(this, "权限不足，只有管理员可以访问", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        initDatabase();
        loadTeachers();
        setupListeners();
    }

    private void initViews() {
        lvTeachers = findViewById(R.id.lv_teachers);
        btnAddTeacher = findViewById(R.id.btn_add_teacher);
        btnBack = findViewById(R.id.btn_back);

        teacherList = new ArrayList<>();
        teacherIdList = new ArrayList<>();
        teacherAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, teacherList);
        lvTeachers.setAdapter(teacherAdapter);
    }

    private void initDatabase() {
        dbHelper = new DBHelper(this);
    }

    private void loadTeachers() {
        teacherList.clear();
        teacherIdList.clear();

        Cursor cursor = dbHelper.query("user", null, "type = ?", new String[]{"1"}, null, null, "username ASC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int userId = 0;
                int idIndex = cursor.getColumnIndex("id");
                if (idIndex != -1) {
                    userId = cursor.getInt(idIndex);
                }
                String username = "";
                int usernameIndex = cursor.getColumnIndex("username");
                if (usernameIndex != -1) {
                    username = cursor.getString(usernameIndex);
                }
                String name = "";
                int nameIndex = cursor.getColumnIndex("name");
                if (nameIndex != -1) {
                    name = cursor.getString(nameIndex);
                }
                String email = "";
                int emailIndex = cursor.getColumnIndex("email");
                if (emailIndex != -1) {
                    email = cursor.getString(emailIndex);
                }
                String phone = "";
                int phoneIndex = cursor.getColumnIndex("phone");
                if (phoneIndex != -1) {
                    phone = cursor.getString(phoneIndex);
                }

                String teacherInfo = "用户名：" + username + "\n" +
                        "姓名：" + name + "\n" +
                        "邮箱：" + email + "\n" +
                        "电话：" + phone;
                teacherList.add(teacherInfo);
                teacherIdList.add(userId);
            }
            cursor.close();
        }
        teacherAdapter.notifyDataSetChanged();
    }

    private void setupListeners() {
        lvTeachers.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                showTeacherOptions(position);
                return true;
            }
        });

        btnAddTeacher.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddTeacherDialog();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void showTeacherOptions(int position) {
        String[] options = {"编辑教师信息", "重置密码", "删除教师"};
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("教师操作")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            editTeacher(position);
                            break;
                        case 1:
                            resetPassword(position);
                            break;
                        case 2:
                            deleteTeacher(position);
                            break;
                    }
                });
        builder.show();
    }

    private void showAddTeacherDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_teacher, null);
        EditText etUsername = dialogView.findViewById(R.id.et_username);
        EditText etPassword = dialogView.findViewById(R.id.et_password);
        EditText etName = dialogView.findViewById(R.id.et_name);
        EditText etEmail = dialogView.findViewById(R.id.et_email);
        EditText etPhone = dialogView.findViewById(R.id.et_phone);

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("添加教师")
                .setView(dialogView)
                .setPositiveButton("保存", (dialog, which) -> {
                    String username = etUsername.getText().toString().trim();
                    String password = etPassword.getText().toString().trim();
                    String name = etName.getText().toString().trim();
                    String email = etEmail.getText().toString().trim();
                    String phone = etPhone.getText().toString().trim();

                    if (username.isEmpty()) {
                        Toast.makeText(TeacherManageActivity.this, "请输入用户名", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (password.isEmpty()) {
                        Toast.makeText(TeacherManageActivity.this, "请输入密码", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (name.isEmpty()) {
                        Toast.makeText(TeacherManageActivity.this, "请输入姓名", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Cursor existingCursor = dbHelper.getUserByUsername(username);
                    if (existingCursor != null && existingCursor.getCount() > 0) {
                        existingCursor.close();
                        Toast.makeText(TeacherManageActivity.this, "用户名已存在", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (existingCursor != null) {
                        existingCursor.close();
                    }

                    String encryptedPassword = SecurityUtils.encryptPassword(password);

                    ContentValues values = new ContentValues();
                    values.put("username", username);
                    values.put("password", encryptedPassword);
                    values.put("name", name);
                    values.put("email", email);
                    values.put("phone", phone);
                    values.put("type", 1);

                    dbHelper.insertUser(values);
                    Toast.makeText(TeacherManageActivity.this, "教师添加成功", Toast.LENGTH_SHORT).show();
                    loadTeachers();
                })
                .setNegativeButton("取消", null);
        builder.show();
    }

    private void editTeacher(int position) {
        int userId = teacherIdList.get(position);
        Cursor cursor = dbHelper.getUserById(userId);
        if (cursor != null && cursor.moveToFirst()) {
            String username = "";
            int usernameIndex = cursor.getColumnIndex("username");
            if (usernameIndex != -1) {
                username = cursor.getString(usernameIndex);
            }
            String name = "";
            int nameIndex = cursor.getColumnIndex("name");
            if (nameIndex != -1) {
                name = cursor.getString(nameIndex);
            }
            String email = "";
            int emailIndex = cursor.getColumnIndex("email");
            if (emailIndex != -1) {
                email = cursor.getString(emailIndex);
            }
            String phone = "";
            int phoneIndex = cursor.getColumnIndex("phone");
            if (phoneIndex != -1) {
                phone = cursor.getString(phoneIndex);
            }
            cursor.close();

            View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_teacher, null);
            EditText etUsername = dialogView.findViewById(R.id.et_username);
            EditText etPassword = dialogView.findViewById(R.id.et_password);
            EditText etName = dialogView.findViewById(R.id.et_name);
            EditText etEmail = dialogView.findViewById(R.id.et_email);
            EditText etPhone = dialogView.findViewById(R.id.et_phone);

            etUsername.setText(username);
            etUsername.setEnabled(false);
            etPassword.setHint("不修改请留空");
            etName.setText(name);
            etEmail.setText(email);
            etPhone.setText(phone);

            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("编辑教师信息")
                    .setView(dialogView)
                    .setPositiveButton("保存", (dialog, which) -> {
                        String newPassword = etPassword.getText().toString().trim();
                        String newName = etName.getText().toString().trim();
                        String newEmail = etEmail.getText().toString().trim();
                        String newPhone = etPhone.getText().toString().trim();

                        if (newName.isEmpty()) {
                            Toast.makeText(TeacherManageActivity.this, "请输入姓名", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        ContentValues values = new ContentValues();
                        if (!newPassword.isEmpty()) {
                            String encryptedPassword = SecurityUtils.encryptPassword(newPassword);
                            values.put("password", encryptedPassword);
                        }
                        values.put("name", newName);
                        values.put("email", newEmail);
                        values.put("phone", newPhone);

                        dbHelper.update("user", values, "id = ?", new String[]{String.valueOf(userId)});
                        Toast.makeText(TeacherManageActivity.this, "教师信息更新成功", Toast.LENGTH_SHORT).show();
                        loadTeachers();
                    })
                    .setNegativeButton("取消", null);
            builder.show();
        }
    }

    private void resetPassword(int position) {
        int userId = teacherIdList.get(position);
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("重置密码")
                .setMessage("确定要将密码重置为默认值 '123456' 吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    String encryptedPassword = SecurityUtils.encryptPassword("123456");
                    ContentValues values = new ContentValues();
                    values.put("password", encryptedPassword);
                    dbHelper.update("user", values, "id = ?", new String[]{String.valueOf(userId)});
                    Toast.makeText(TeacherManageActivity.this, "密码已重置为 '123456'", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null);
        builder.show();
    }

    private void deleteTeacher(int position) {
        int userId = teacherIdList.get(position);
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("确认删除")
                .setMessage("确定要删除这个教师账号吗？删除后相关数据也会被删除。")
                .setPositiveButton("确定", (dialog, which) -> {
                    dbHelper.delete("user", "id = ?", new String[]{String.valueOf(userId)});
                    Toast.makeText(TeacherManageActivity.this, "教师账号已删除", Toast.LENGTH_SHORT).show();
                    loadTeachers();
                })
                .setNegativeButton("取消", null);
        builder.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}