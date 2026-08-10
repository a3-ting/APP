package com.example.myapplication.ui.user;

import androidx.appcompat.app.AppCompatActivity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import com.example.myapplication.database.DBHelper;
import com.example.myapplication.utils.ToastUtils;
import com.example.myapplication.utils.SecurityUtils;

public class UserManageActivity extends AppCompatActivity {

    private ListView lvUsers;
    private Button btnAddUser;
    private DBHelper dbHelper;
    private SimpleCursorAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_manage);

        int userType = getIntent().getIntExtra("user_type", -1);
        if (userType != 2) { // 2: 管理员
            ToastUtils.showShort(this, "权限不足，只有管理员可以访问");
            finish();
            return;
        }

        initViews();
        initDatabase();
        loadUsers();
        setupListeners();
    }

    private void initViews() {
        lvUsers = findViewById(R.id.lv_users);
        btnAddUser = findViewById(R.id.btn_add_user);
        Button btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());
    }

    private void initDatabase() {
        dbHelper = new DBHelper(this);
    }

    private void loadUsers() {
        Cursor cursor = dbHelper.query("user", new String[] {"id", "username", "name", "type"}, null, null, null, null, "id DESC");
        String[] from = {"username", "name", "type"};
        int[] to = {R.id.tv_username, R.id.tv_name, R.id.tv_type};
        adapter = new SimpleCursorAdapter(this, R.layout.user_item, cursor, from, to, 0);
        adapter.setViewBinder((view, c, columnIndex) -> {
            if (columnIndex == c.getColumnIndex("type")) {
                int type = c.getInt(columnIndex);
                String typeStr = "未知";
                switch (type) {
                    case 0: typeStr = "学生";
                        break;
                    case 1: typeStr = "教师";
                        break;
                    case 2: typeStr = "管理员";
                        break;
                }
                ((android.widget.TextView) view).setText(typeStr);
                return true;
            }
            return false;
        });
        lvUsers.setAdapter(adapter);
    }

    private void setupListeners() {
        btnAddUser.setOnClickListener(v -> {
            // 打开添加用户对话框
            showAddUserDialog();
        });

        lvUsers.setOnItemClickListener((parent, view, position, id) -> {
            // 打开用户详情对话框
            Cursor cursor = (Cursor) adapter.getItem(position);
            if (cursor != null) {
                int idIndex = cursor.getColumnIndex("id");
                if (idIndex != -1) {
                    int userId = cursor.getInt(idIndex);
                    showUserDetailDialog(userId);
                }
            }
        });
    }

    private void showAddUserDialog() {
        // 创建添加用户对话框
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("添加用户");
        View view = getLayoutInflater().inflate(R.layout.dialog_add_user, null);
        builder.setView(view);

        android.widget.EditText etUsername = view.findViewById(R.id.et_username);
        android.widget.EditText etPassword = view.findViewById(R.id.et_password);
        android.widget.EditText etName = view.findViewById(R.id.et_name);
        android.widget.Spinner spType = view.findViewById(R.id.sp_type);

        builder.setPositiveButton("确定", (dialog, which) -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String name = etName.getText().toString().trim();
            int type = spType.getSelectedItemPosition();

            if (username.isEmpty() || password.isEmpty() || name.isEmpty()) {
                ToastUtils.showShort(this, "请填写所有字段");
                return;
            }

            ContentValues values = new ContentValues();
            values.put("username", username);
            values.put("password", SecurityUtils.encryptPassword(password)); // 使用加密存储密码
            values.put("name", name);
            values.put("type", type);

            long result = dbHelper.insertUser(values);
            if (result > 0) {
                ToastUtils.showShort(this, "用户添加成功");
                loadUsers(); // 刷新列表
            } else {
                ToastUtils.showShort(this, "用户添加失败");
            }
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void showUserDetailDialog(int userId) {
        // 创建用户详情对话框
        Cursor cursor = dbHelper.query("user", null, "id = ?", new String[]{String.valueOf(userId)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int usernameIndex = cursor.getColumnIndex("username");
            int nameIndex = cursor.getColumnIndex("name");
            int typeIndex = cursor.getColumnIndex("type");
            
            if (usernameIndex != -1 && nameIndex != -1 && typeIndex != -1) {
                String username = cursor.getString(usernameIndex);
                String name = cursor.getString(nameIndex);
                int type = cursor.getInt(typeIndex);

                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                builder.setTitle("用户详情");
                builder.setMessage(
                    "用户名：" + username + "\n"
                    + "姓名：" + name + "\n"
                    + "类型：" + (type == 0 ? "学生" : type == 1 ? "教师" : "管理员")
                );

            builder.setPositiveButton("编辑", (dialog, which) -> {
                // 打开编辑用户对话框
                showEditUserDialog(userId, username, name, type);
            });

            builder.setNegativeButton("删除", (dialog, which) -> {
                // 确认删除
                android.app.AlertDialog.Builder confirmBuilder = new android.app.AlertDialog.Builder(this);
                confirmBuilder.setTitle("确认删除");
                confirmBuilder.setMessage("确定要删除这个用户吗？");
                confirmBuilder.setPositiveButton("确定", (dialog1, which1) -> {
                    int result = dbHelper.delete("user", "id = ?", new String[]{String.valueOf(userId)});
                    if (result > 0) {
                        ToastUtils.showShort(this, "用户删除成功");
                        loadUsers(); // 刷新列表
                    } else {
                        ToastUtils.showShort(this, "用户删除失败");
                    }
                });
                confirmBuilder.setNegativeButton("取消", null);
                confirmBuilder.show();
            });

                builder.setNeutralButton("取消", null);
                builder.show();
            }

            cursor.close();
        }
    }

    private void showEditUserDialog(int userId, String username, String name, int type) {
        // 创建编辑用户对话框
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("编辑用户");
        View view = getLayoutInflater().inflate(R.layout.dialog_add_user, null);
        builder.setView(view);

        android.widget.EditText etUsername = view.findViewById(R.id.et_username);
        android.widget.EditText etPassword = view.findViewById(R.id.et_password);
        android.widget.EditText etName = view.findViewById(R.id.et_name);
        android.widget.Spinner spType = view.findViewById(R.id.sp_type);

        etUsername.setText(username);
        etName.setText(name);
        spType.setSelection(type);

        builder.setPositiveButton("确定", (dialog, which) -> {
            String newUsername = etUsername.getText().toString().trim();
            String newPassword = etPassword.getText().toString().trim();
            String newName = etName.getText().toString().trim();
            int newType = spType.getSelectedItemPosition();

            if (newUsername.isEmpty() || newName.isEmpty()) {
                ToastUtils.showShort(this, "请填写用户名和姓名");
                return;
            }

            ContentValues values = new ContentValues();
            values.put("username", newUsername);
            if (!newPassword.isEmpty()) {
                values.put("password", SecurityUtils.encryptPassword(newPassword)); // 使用加密存储密码
            }
            values.put("name", newName);
            values.put("type", newType);

            int result = dbHelper.update("user", values, "id = ?", new String[]{String.valueOf(userId)});
            if (result > 0) {
                ToastUtils.showShort(this, "用户编辑成功");
                loadUsers(); // 刷新列表
            } else {
                ToastUtils.showShort(this, "用户编辑失败");
            }
        });

        builder.setNegativeButton("取消", null);
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