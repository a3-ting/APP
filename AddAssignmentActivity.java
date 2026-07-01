package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.content.ContentValues;
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

import java.util.ArrayList;
import java.util.List;

public class AddAssignmentActivity extends AppCompatActivity {

    private EditText etTitle, etDescription, etDueDate;
    private Spinner spCourse, spPriority, spStatus;
    private Button btnSave, btnCancel;
    private DBHelper dbHelper;
    private int userId;
    private int assignmentId = -1; // -1表示添加新作业，否则表示编辑现有作业
    private List<Integer> courseIdList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_assignment);

        userId = getIntent().getIntExtra("user_id", -1);
        assignmentId = getIntent().getIntExtra("assignment_id", -1);

        if (userId == -1) {
            Toast.makeText(this, "用户信息错误", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        initDatabase();
        loadCourses();
        setupListeners();

        // 如果是编辑模式，加载现有作业信息
        if (assignmentId != -1) {
            loadAssignmentInfo();
        }
    }

    private void initViews() {
        etTitle = findViewById(R.id.et_title);
        etDescription = findViewById(R.id.et_description);
        etDueDate = findViewById(R.id.et_due_date);
        spCourse = findViewById(R.id.sp_course);
        spPriority = findViewById(R.id.sp_priority);
        spStatus = findViewById(R.id.sp_status);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);

        // 设置优先级选项
        ArrayAdapter<CharSequence> priorityAdapter = ArrayAdapter.createFromResource(this,
                R.array.priority_options, android.R.layout.simple_spinner_item);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPriority.setAdapter(priorityAdapter);

        // 设置状态选项
        ArrayAdapter<CharSequence> statusAdapter = ArrayAdapter.createFromResource(this,
                R.array.assignment_status_options, android.R.layout.simple_spinner_item);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spStatus.setAdapter(statusAdapter);

        courseIdList = new ArrayList<>();
    }

    private void initDatabase() {
        dbHelper = new DBHelper(this);
    }

    private void loadCourses() {
        // 加载用户的课程列表
        List<String> courseNames = new ArrayList<>();
        courseIdList.clear();

        Cursor cursor = dbHelper.getCoursesByUserId(userId);
        if (cursor != null) {
            int idColumnIndex = cursor.getColumnIndex("id");
            int courseNameColumnIndex = cursor.getColumnIndex("course_name");
            while (cursor.moveToNext() && idColumnIndex != -1 && courseNameColumnIndex != -1) {
                int courseId = cursor.getInt(idColumnIndex);
                String courseName = cursor.getString(courseNameColumnIndex);
                courseNames.add(courseName);
                courseIdList.add(courseId);
            }
            cursor.close();
        }

        if (courseNames.isEmpty()) {
            courseNames.add("无可用课程");
            courseIdList.add(-1);
        }

        ArrayAdapter<String> courseAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, courseNames);
        courseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCourse.setAdapter(courseAdapter);
    }

    private void setupListeners() {
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAssignment();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void loadAssignmentInfo() {
        // 从数据库加载现有作业信息
        Cursor cursor = dbHelper.query("assignment", null, "id = ?", new String[]{String.valueOf(assignmentId)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int titleIndex = cursor.getColumnIndex("title");
            int descriptionIndex = cursor.getColumnIndex("description");
            int dueDateIndex = cursor.getColumnIndex("due_date");
            int priorityIndex = cursor.getColumnIndex("priority");
            int statusIndex = cursor.getColumnIndex("status");
            int courseIdIndex = cursor.getColumnIndex("course_id");

            if (titleIndex != -1) {
                etTitle.setText(cursor.getString(titleIndex));
            }
            if (descriptionIndex != -1) {
                etDescription.setText(cursor.getString(descriptionIndex));
            }
            if (dueDateIndex != -1) {
                etDueDate.setText(cursor.getString(dueDateIndex));
            }
            if (priorityIndex != -1) {
                int priority = cursor.getInt(priorityIndex);
                spPriority.setSelection(priority);
            }
            if (statusIndex != -1) {
                int status = cursor.getInt(statusIndex);
                spStatus.setSelection(status);
            }
            if (courseIdIndex != -1) {
                int courseId = cursor.getInt(courseIdIndex);
                // 设置课程选择
                for (int i = 0; i < courseIdList.size(); i++) {
                    if (courseIdList.get(i) == courseId) {
                        spCourse.setSelection(i);
                        break;
                    }
                }
            }

            cursor.close();
        }
    }

    private void saveAssignment() {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String dueDate = etDueDate.getText().toString().trim();
        int coursePosition = spCourse.getSelectedItemPosition();
        int priority = spPriority.getSelectedItemPosition();
        int status = spStatus.getSelectedItemPosition();

        // 验证输入
        if (title.isEmpty()) {
            Toast.makeText(this, "请输入作业标题", Toast.LENGTH_SHORT).show();
            return;
        }
        if (dueDate.isEmpty()) {
            Toast.makeText(this, "请输入截止日期", Toast.LENGTH_SHORT).show();
            return;
        }
        if (coursePosition < 0 || courseIdList.get(coursePosition) == -1) {
            Toast.makeText(this, "请先添加课程", Toast.LENGTH_SHORT).show();
            return;
        }

        int courseId = courseIdList.get(coursePosition);

        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("course_id", courseId);
        values.put("title", title);
        values.put("description", description);
        values.put("due_date", dueDate);
        values.put("priority", priority);
        values.put("status", status);

        if (assignmentId == -1) {
            // 添加新作业
            long result = dbHelper.insertAssignment(values);
            if (result > 0) {
                Toast.makeText(this, "作业添加成功", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "作业添加失败", Toast.LENGTH_SHORT).show();
            }
        } else {
            // 更新现有作业
            int result = dbHelper.update("assignment", values, "id = ?", new String[]{String.valueOf(assignmentId)});
            if (result > 0) {
                Toast.makeText(this, "作业更新成功", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "作业更新失败", Toast.LENGTH_SHORT).show();
            }
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
