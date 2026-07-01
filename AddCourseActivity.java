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

public class AddCourseActivity extends AppCompatActivity {

    private EditText etCourseName, etTeacherName, etClassroom, etStartTime, etEndTime;
    private Spinner spDayOfWeek;
    private Button btnSave, btnCancel;
    private DBHelper dbHelper;
    private int userId;
    private int courseId = -1; // -1表示添加新课程，否则表示编辑现有课程

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_course);

        userId = getIntent().getIntExtra("user_id", -1);
        courseId = getIntent().getIntExtra("course_id", -1);

        if (userId == -1) {
            Toast.makeText(this, "用户信息错误", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        initDatabase();
        setupListeners();

        // 如果是编辑模式，加载现有课程信息
        if (courseId != -1) {
            loadCourseInfo();
        }
    }

    private void initViews() {
        etCourseName = findViewById(R.id.et_course_name);
        etTeacherName = findViewById(R.id.et_teacher_name);
        etClassroom = findViewById(R.id.et_classroom);
        etStartTime = findViewById(R.id.et_start_time);
        etEndTime = findViewById(R.id.et_end_time);
        spDayOfWeek = findViewById(R.id.sp_day_of_week);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);

        // 设置星期几选项
        ArrayAdapter<CharSequence> dayAdapter = ArrayAdapter.createFromResource(this,
                R.array.day_of_week_options, android.R.layout.simple_spinner_item);
        dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDayOfWeek.setAdapter(dayAdapter);
    }

    private void initDatabase() {
        dbHelper = new DBHelper(this);
    }

    private void setupListeners() {
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveCourse();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void loadCourseInfo() {
        // 从数据库加载现有课程信息
        Cursor cursor = dbHelper.query("course", null, "id = ?", new String[]{String.valueOf(courseId)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int courseNameIndex = cursor.getColumnIndex("course_name");
            int teacherNameIndex = cursor.getColumnIndex("teacher_name");
            int classroomIndex = cursor.getColumnIndex("classroom");
            int startTimeIndex = cursor.getColumnIndex("start_time");
            int endTimeIndex = cursor.getColumnIndex("end_time");
            int dayOfWeekIndex = cursor.getColumnIndex("day_of_week");

            if (courseNameIndex != -1) {
                etCourseName.setText(cursor.getString(courseNameIndex));
            }
            if (teacherNameIndex != -1) {
                etTeacherName.setText(cursor.getString(teacherNameIndex));
            }
            if (classroomIndex != -1) {
                etClassroom.setText(cursor.getString(classroomIndex));
            }
            if (startTimeIndex != -1) {
                etStartTime.setText(cursor.getString(startTimeIndex));
            }
            if (endTimeIndex != -1) {
                etEndTime.setText(cursor.getString(endTimeIndex));
            }
            if (dayOfWeekIndex != -1) {
                int dayOfWeek = cursor.getInt(dayOfWeekIndex);
                spDayOfWeek.setSelection(dayOfWeek - 1); // 调整为Spinner的索引（0-6）
            }

            cursor.close();
        }
    }

    private void saveCourse() {
        String courseName = etCourseName.getText().toString().trim();
        String teacherName = etTeacherName.getText().toString().trim();
        String classroom = etClassroom.getText().toString().trim();
        String startTime = etStartTime.getText().toString().trim();
        String endTime = etEndTime.getText().toString().trim();
        int dayOfWeek = spDayOfWeek.getSelectedItemPosition() + 1; // 调整为星期几（1-7）

        // 验证输入
        if (courseName.isEmpty()) {
            Toast.makeText(this, "请输入课程名称", Toast.LENGTH_SHORT).show();
            return;
        }
        if (teacherName.isEmpty()) {
            Toast.makeText(this, "请输入教师姓名", Toast.LENGTH_SHORT).show();
            return;
        }
        if (classroom.isEmpty()) {
            Toast.makeText(this, "请输入教室", Toast.LENGTH_SHORT).show();
            return;
        }
        if (startTime.isEmpty()) {
            Toast.makeText(this, "请输入开始时间", Toast.LENGTH_SHORT).show();
            return;
        }
        if (endTime.isEmpty()) {
            Toast.makeText(this, "请输入结束时间", Toast.LENGTH_SHORT).show();
            return;
        }

        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("course_name", courseName);
        values.put("teacher_name", teacherName);
        values.put("classroom", classroom);
        values.put("start_time", startTime);
        values.put("end_time", endTime);
        values.put("day_of_week", dayOfWeek);
        values.put("is_favorite", 0); // 默认不是收藏课程

        if (courseId == -1) {
            // 添加新课程
            long result = dbHelper.insertCourse(values);
            if (result > 0) {
                Toast.makeText(this, "课程添加成功", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "课程添加失败", Toast.LENGTH_SHORT).show();
            }
        } else {
            // 更新现有课程
            int result = dbHelper.update("course", values, "id = ?", new String[]{String.valueOf(courseId)});
            if (result > 0) {
                Toast.makeText(this, "课程更新成功", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "课程更新失败", Toast.LENGTH_SHORT).show();
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
