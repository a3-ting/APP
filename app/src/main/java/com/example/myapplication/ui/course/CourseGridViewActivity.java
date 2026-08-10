package com.example.myapplication.ui.course;

import androidx.appcompat.app.AppCompatActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.myapplication.database.DBHelper;

import java.util.HashMap;
import java.util.Map;

public class CourseGridViewActivity extends AppCompatActivity {

    private DBHelper dbHelper;
    private int userId;
    private Map<String, LinearLayout> courseCells;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_grid_view);

        userId = getIntent().getIntExtra("user_id", -1);
        int userType = getIntent().getIntExtra("user_type", -1);
        
        if (userId == -1) {
            finish();
            return;
        }
        
        if (userType != 0 && userType != 2) {
            finish();
            return;
        }

        initDatabase();
        initCourseCells();
        loadCourses();
        setupListeners();
    }

    private void initDatabase() {
        dbHelper = new DBHelper(this);
    }

    private void initCourseCells() {
        courseCells = new HashMap<>();
        String[] days = {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};
        for (String day : days) {
            for (int i = 1; i <= 5; i++) {
                int cellId = getResources().getIdentifier(day + "_course_" + i, "id", getPackageName());
                if (cellId != 0) {
                    LinearLayout cell = findViewById(cellId);
                    courseCells.put(day + "_" + i, cell);
                }
            }
        }
    }

    private void loadCourses() {
        Cursor cursor = dbHelper.getCoursesByUserId(userId);
        if (cursor != null) {
            int courseNameIndex = cursor.getColumnIndex("course_name");
            int teacherNameIndex = cursor.getColumnIndex("teacher_name");
            int classroomIndex = cursor.getColumnIndex("classroom");
            int startTimeIndex = cursor.getColumnIndex("start_time");
            int endTimeIndex = cursor.getColumnIndex("end_time");
            int dayOfWeekIndex = cursor.getColumnIndex("day_of_week");

            while (cursor.moveToNext() && courseNameIndex != -1 && teacherNameIndex != -1 && classroomIndex != -1 && startTimeIndex != -1 && endTimeIndex != -1 && dayOfWeekIndex != -1) {
                String courseName = cursor.getString(courseNameIndex);
                String teacherName = cursor.getString(teacherNameIndex);
                String classroom = cursor.getString(classroomIndex);
                String startTime = cursor.getString(startTimeIndex);
                String endTime = cursor.getString(endTimeIndex);
                int dayOfWeek = cursor.getInt(dayOfWeekIndex);

                String dayStr = getDayString(dayOfWeek);
                int timeSlot = getTimeSlot(startTime);
                if (!dayStr.isEmpty() && timeSlot != -1) {
                    String key = dayStr + "_" + timeSlot;
                    LinearLayout cell = courseCells.get(key);
                    if (cell != null) {
                        cell.setBackgroundColor(getResources().getColor(android.R.color.holo_purple));
                        cell.removeAllViews();

                        TextView courseInfo = new TextView(this);
                        courseInfo.setText(courseName + "\n" + teacherName + "\n" + classroom + "\n" + startTime + "-" + endTime);
                        courseInfo.setTextSize(12);
                        courseInfo.setPadding(10, 10, 10, 10);
                        cell.addView(courseInfo);
                    }
                }
            }
            cursor.close();
        }
    }

    private String getDayString(int dayOfWeek) {
        switch (dayOfWeek) {
            case 1:
                return "monday";
            case 2:
                return "tuesday";
            case 3:
                return "wednesday";
            case 4:
                return "thursday";
            case 5:
                return "friday";
            case 6:
                return "saturday";
            case 7:
                return "sunday";
            default:
                return "";
        }
    }

    private int getTimeSlot(String startTime) {
        switch (startTime) {
            case "08:00":
                return 1;
            case "10:00":
                return 2;
            case "14:00":
                return 3;
            case "16:00":
                return 4;
            case "19:00":
                return 5;
            default:
                return -1;
        }
    }

    private void setupListeners() {
        Button btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}