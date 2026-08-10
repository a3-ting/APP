package com.example.myapplication.ui.notice;

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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NoticeActivity extends AppCompatActivity {

    private ListView lvNotices;
    private Button btnAddNotice, btnBack;
    private DBHelper dbHelper;
    private int userId;
    private int userType;
    private List<String> noticeList;
    private List<Integer> noticeIdList;
    private ArrayAdapter<String> noticeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notice);

        userId = getIntent().getIntExtra("user_id", -1);
        userType = getIntent().getIntExtra("user_type", -1);
        if (userId == -1) {
            Toast.makeText(this, "用户信息错误", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        initDatabase();
        loadNotices();
        setupListeners();
    }

    private void initViews() {
        lvNotices = findViewById(R.id.lv_notices);
        btnAddNotice = findViewById(R.id.btn_add_notice);
        btnBack = findViewById(R.id.btn_back);

        noticeList = new ArrayList<>();
        noticeIdList = new ArrayList<>();
        noticeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, noticeList);
        lvNotices.setAdapter(noticeAdapter);

        btnAddNotice.setVisibility(userType == 2 ? View.VISIBLE : View.GONE);
    }

    private void initDatabase() {
        dbHelper = new DBHelper(this);
    }

    private void loadNotices() {
        noticeList.clear();
        noticeIdList.clear();

        try {
            Cursor cursor = dbHelper.getAllNotices();
            if (cursor != null) {
                int idIndex = cursor.getColumnIndex("id");
                int titleIndex = cursor.getColumnIndex("title");
                int contentIndex = cursor.getColumnIndex("content");
                int publisherIndex = cursor.getColumnIndex("publisher");
                int publishDateIndex = cursor.getColumnIndex("publish_date");
                int isReadIndex = cursor.getColumnIndex("is_read");

                while (cursor.moveToNext()) {
                    if (idIndex != -1 && titleIndex != -1 && contentIndex != -1 && 
                        publisherIndex != -1 && publishDateIndex != -1) {
                        int noticeId = cursor.getInt(idIndex);
                        String title = cursor.getString(titleIndex);
                        String content = cursor.getString(contentIndex);
                        String publisher = cursor.getString(publisherIndex);
                        String publishDate = cursor.getString(publishDateIndex);

                        String noticeInfo = title + "\n" +
                                "发布者：" + publisher + "\n" +
                                "发布时间：" + publishDate + "\n" +
                                "内容：" + content;
                        noticeList.add(noticeInfo);
                        noticeIdList.add(noticeId);
                    }
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "加载公告失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        noticeAdapter.notifyDataSetChanged();
    }

    private void setupListeners() {
        lvNotices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showNoticeDetails(position);
            }
        });

        lvNotices.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (userType == 2) {
                    showNoticeOptions(position);
                    return true;
                }
                return false;
            }
        });

        btnAddNotice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddNoticeDialog();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void showNoticeDetails(int position) {
        if (position < 0 || position >= noticeList.size()) {
            Toast.makeText(this, "公告索引无效", Toast.LENGTH_SHORT).show();
            return;
        }
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("公告详情")
                .setMessage(noticeList.get(position))
                .setPositiveButton("确定", null);
        builder.show();
    }

    private void showNoticeOptions(int position) {
        String[] options = {"编辑公告", "删除公告"};
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("公告操作")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            editNotice(position);
                            break;
                        case 1:
                            deleteNotice(position);
                            break;
                    }
                });
        builder.show();
    }

    private void showAddNoticeDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_notice, null);
        EditText etTitle = dialogView.findViewById(R.id.et_notice_title);
        EditText etContent = dialogView.findViewById(R.id.et_notice_content);
        EditText etPublisher = dialogView.findViewById(R.id.et_notice_publisher);

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("发布公告")
                .setView(dialogView)
                .setPositiveButton("发布", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    String content = etContent.getText().toString().trim();
                    String publisher = etPublisher.getText().toString().trim();

                    if (title.isEmpty()) {
                        Toast.makeText(NoticeActivity.this, "请输入公告标题", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (content.isEmpty()) {
                        Toast.makeText(NoticeActivity.this, "请输入公告内容", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (publisher.isEmpty()) {
                        Toast.makeText(NoticeActivity.this, "请输入发布者", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    ContentValues values = new ContentValues();
                    values.put("title", title);
                    values.put("content", content);
                    values.put("publisher", publisher);
                    values.put("publish_date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(new Date()));
                    values.put("is_read", 0);

                    dbHelper.insert("notice", values);
                    Toast.makeText(NoticeActivity.this, "公告发布成功", Toast.LENGTH_SHORT).show();
                    loadNotices();
                })
                .setNegativeButton("取消", null);
        builder.show();
    }

    private void editNotice(int position) {
        int noticeId = noticeIdList.get(position);
        Cursor cursor = dbHelper.query("notice", null, "id = ?", new String[]{String.valueOf(noticeId)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            String title = "";
            int titleIndex = cursor.getColumnIndex("title");
            if (titleIndex != -1) {
                title = cursor.getString(titleIndex);
            }
            String content = "";
            int contentIndex = cursor.getColumnIndex("content");
            if (contentIndex != -1) {
                content = cursor.getString(contentIndex);
            }
            String publisher = "";
            int publisherIndex = cursor.getColumnIndex("publisher");
            if (publisherIndex != -1) {
                publisher = cursor.getString(publisherIndex);
            }
            cursor.close();

            View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_notice, null);
            EditText etTitle = dialogView.findViewById(R.id.et_notice_title);
            EditText etContent = dialogView.findViewById(R.id.et_notice_content);
            EditText etPublisher = dialogView.findViewById(R.id.et_notice_publisher);

            etTitle.setText(title);
            etContent.setText(content);
            etPublisher.setText(publisher);

            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("编辑公告")
                    .setView(dialogView)
                    .setPositiveButton("保存", (dialog, which) -> {
                        String newTitle = etTitle.getText().toString().trim();
                        String newContent = etContent.getText().toString().trim();
                        String newPublisher = etPublisher.getText().toString().trim();

                        if (newTitle.isEmpty()) {
                            Toast.makeText(NoticeActivity.this, "请输入公告标题", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (newContent.isEmpty()) {
                            Toast.makeText(NoticeActivity.this, "请输入公告内容", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        ContentValues values = new ContentValues();
                        values.put("title", newTitle);
                        values.put("content", newContent);
                        values.put("publisher", newPublisher);

                        dbHelper.update("notice", values, "id = ?", new String[]{String.valueOf(noticeId)});
                        Toast.makeText(NoticeActivity.this, "公告更新成功", Toast.LENGTH_SHORT).show();
                        loadNotices();
                    })
                    .setNegativeButton("取消", null);
            builder.show();
        }
    }

    private void deleteNotice(int position) {
        int noticeId = noticeIdList.get(position);
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("确认删除")
                .setMessage("确定要删除这篇公告吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    dbHelper.delete("notice", "id = ?", new String[]{String.valueOf(noticeId)});
                    Toast.makeText(NoticeActivity.this, "公告已删除", Toast.LENGTH_SHORT).show();
                    loadNotices();
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