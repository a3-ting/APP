package com.example.myapplication.ui.reminder;

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
import android.widget.Spinner;
import android.widget.Toast;

import com.example.myapplication.database.DBHelper;
import com.example.myapplication.utils.DialogUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ReminderActivity extends AppCompatActivity {

    private ListView lvReminders;
    private Button btnAddReminder, btnBack;
    private DBHelper dbHelper;
    private int userId;
    private List<String> reminderList;
    private List<Integer> reminderIdList;
    private ArrayAdapter<String> reminderAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder);

        userId = getIntent().getIntExtra("user_id", -1);
        int userType = getIntent().getIntExtra("user_type", -1);
        if (userId == -1) {
            DialogUtils.showErrorDialog(this, "用户信息错误");
            finish();
            return;
        }

        initViews();
        initDatabase();
        loadReminders();
        setupListeners();
    }

    private void initViews() {
        lvReminders = findViewById(R.id.lv_reminders);
        btnAddReminder = findViewById(R.id.btn_add_reminder);
        btnBack = findViewById(R.id.btn_back);

        // 初始化提醒列表
        reminderList = new ArrayList<>();
        reminderIdList = new ArrayList<>();
        reminderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, reminderList);
        lvReminders.setAdapter(reminderAdapter);
    }

    private void initDatabase() {
        dbHelper = new DBHelper(this);
    }

    private void loadReminders() {
        reminderList.clear();
        reminderIdList.clear();

        try {
            Cursor cursor = dbHelper.getPendingReminders(userId);
            if (cursor != null) {
                int idIndex = cursor.getColumnIndex("id");
                int typeIndex = cursor.getColumnIndex("type");
                int titleIndex = cursor.getColumnIndex("title");
                int contentIndex = cursor.getColumnIndex("content");
                int remindTimeIndex = cursor.getColumnIndex("remind_time");

                while (cursor.moveToNext()) {
                    if (idIndex != -1 && typeIndex != -1 && titleIndex != -1 && contentIndex != -1 && remindTimeIndex != -1) {
                        int reminderId = cursor.getInt(idIndex);
                        int type = cursor.getInt(typeIndex);
                        String title = cursor.getString(titleIndex);
                        String content = cursor.getString(contentIndex);
                        String remindTime = cursor.getString(remindTimeIndex);

                        String typeStr = getReminderTypeString(type);
                        String reminderInfo = typeStr + "：" + title + "\n" +
                                "内容：" + content + "\n" +
                                "提醒时间：" + remindTime;
                        reminderList.add(reminderInfo);
                        reminderIdList.add(reminderId);
                    }
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "加载提醒失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        reminderAdapter.notifyDataSetChanged();
    }

    private String getReminderTypeString(int type) {
        switch (type) {
            case 0:
                return "课程提醒";
            case 1:
                return "作业提醒";
            case 2:
                return "其他提醒";
            default:
                return "未知提醒";
        }
    }

    private void setupListeners() {
        lvReminders.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showReminderDetails(position);
            }
        });

        lvReminders.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                showReminderOptions(position);
                return true;
            }
        });

        btnAddReminder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddReminderDialog();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void showReminderDetails(int position) {
        // 显示提醒详情
        Toast.makeText(this, "查看提醒详情：" + reminderList.get(position), Toast.LENGTH_SHORT).show();
    }

    private void showReminderOptions(int position) {
        // 显示提醒操作菜单
        String[] options = {"编辑提醒", "删除提醒", "标记为已发送"};
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("提醒操作")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            editReminder(position);
                            break;
                        case 1:
                            deleteReminder(position);
                            break;
                        case 2:
                            markAsSent(position);
                            break;
                    }
                });
        builder.show();
    }

    private void showAddReminderDialog() {
        // 显示添加提醒对话框
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_reminder, null);
        Spinner spReminderType = dialogView.findViewById(R.id.sp_reminder_type);
        EditText etTitle = dialogView.findViewById(R.id.et_reminder_title);
        EditText etContent = dialogView.findViewById(R.id.et_reminder_content);
        EditText etReminderTime = dialogView.findViewById(R.id.et_reminder_time);

        // 设置提醒类型选项
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.reminder_type_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spReminderType.setAdapter(adapter);

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("添加提醒")
                .setView(dialogView)
                .setPositiveButton("保存", (dialog, which) -> {
                    int type = spReminderType.getSelectedItemPosition();
                    String title = etTitle.getText().toString().trim();
                    String content = etContent.getText().toString().trim();
                    String reminderTime = etReminderTime.getText().toString().trim();

                    if (title.isEmpty()) {
                        Toast.makeText(ReminderActivity.this, "请输入提醒标题", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (reminderTime.isEmpty()) {
                        Toast.makeText(ReminderActivity.this, "请输入提醒时间", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 保存提醒到数据库
                    ContentValues values = new ContentValues();
                    values.put("user_id", userId);
                    values.put("type", type);
                    values.put("title", title);
                    values.put("content", content);
                    values.put("remind_time", reminderTime);
                    values.put("is_sent", 0);

                    dbHelper.insertReminder(values);
                    Toast.makeText(ReminderActivity.this, "提醒添加成功", Toast.LENGTH_SHORT).show();
                    loadReminders();
                })
                .setNegativeButton("取消", null);
        builder.show();
    }

    private void editReminder(int position) {
        // 编辑提醒
        int reminderId = reminderIdList.get(position);
        Cursor cursor = dbHelper.query("reminder", null, "id = ?", new String[]{String.valueOf(reminderId)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int typeIndex = cursor.getColumnIndex("type");
            int titleIndex = cursor.getColumnIndex("title");
            int contentIndex = cursor.getColumnIndex("content");
            int remindTimeIndex = cursor.getColumnIndex("remind_time");

            if (typeIndex != -1 && titleIndex != -1 && contentIndex != -1 && remindTimeIndex != -1) {
                int type = cursor.getInt(typeIndex);
                String title = cursor.getString(titleIndex);
                String content = cursor.getString(contentIndex);
                String remindTime = cursor.getString(remindTimeIndex);
                cursor.close();

                View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_reminder, null);
                Spinner spReminderType = dialogView.findViewById(R.id.sp_reminder_type);
                EditText etTitle = dialogView.findViewById(R.id.et_reminder_title);
                EditText etContent = dialogView.findViewById(R.id.et_reminder_content);
                EditText etReminderTime = dialogView.findViewById(R.id.et_reminder_time);

                // 设置提醒类型选项
                ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                        R.array.reminder_type_options, android.R.layout.simple_spinner_item);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spReminderType.setAdapter(adapter);

                // 设置当前值
                spReminderType.setSelection(type);
                etTitle.setText(title);
                etContent.setText(content);
                etReminderTime.setText(remindTime);

                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                builder.setTitle("编辑提醒")
                        .setView(dialogView)
                        .setPositiveButton("保存", (dialog, which) -> {
                            int newType = spReminderType.getSelectedItemPosition();
                            String newTitle = etTitle.getText().toString().trim();
                            String newContent = etContent.getText().toString().trim();
                            String newReminderTime = etReminderTime.getText().toString().trim();

                            if (newTitle.isEmpty()) {
                                Toast.makeText(ReminderActivity.this, "请输入提醒标题", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if (newReminderTime.isEmpty()) {
                                Toast.makeText(ReminderActivity.this, "请输入提醒时间", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            // 更新提醒到数据库
                            ContentValues values = new ContentValues();
                            values.put("type", newType);
                            values.put("title", newTitle);
                            values.put("content", newContent);
                            values.put("remind_time", newReminderTime);

                            dbHelper.update("reminder", values, "id = ?", new String[]{String.valueOf(reminderId)});
                            Toast.makeText(ReminderActivity.this, "提醒更新成功", Toast.LENGTH_SHORT).show();
                            loadReminders();
                        })
                        .setNegativeButton("取消", null);
                builder.show();
            } else {
                cursor.close();
            }
        }
    }

    private void deleteReminder(int position) {
        // 删除提醒
        int reminderId = reminderIdList.get(position);
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("确认删除")
                .setMessage("确定要删除这个提醒吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    // 从数据库中删除提醒
                    dbHelper.delete("reminder", "id = ?", new String[]{String.valueOf(reminderId)});
                    Toast.makeText(ReminderActivity.this, "提醒已删除", Toast.LENGTH_SHORT).show();
                    loadReminders(); // 重新加载提醒列表
                })
                .setNegativeButton("取消", null);
        builder.show();
    }

    private void markAsSent(int position) {
        // 标记提醒为已发送
        int reminderId = reminderIdList.get(position);
        ContentValues values = new ContentValues();
        values.put("is_sent", 1);
        dbHelper.update("reminder", values, "id = ?", new String[]{String.valueOf(reminderId)});
        Toast.makeText(this, "提醒已标记为已发送", Toast.LENGTH_SHORT).show();
        loadReminders(); // 重新加载提醒列表
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}
