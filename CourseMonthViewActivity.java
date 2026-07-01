package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.myapplication.database.DBHelper;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class CourseMonthViewActivity extends AppCompatActivity {

    private TextView tvCurrentMonth;
    private Button btnPreviousMonth, btnNextMonth, btnBack;
    private LinearLayout calendarGrid;
    private Map<String, LinearLayout> dayCells;
    private Calendar calendar;
    private DBHelper dbHelper;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_month_view);

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
        initViews();
        initCalendar();
        initDayCells();
        updateCalendar();
        setupListeners();
    }

    private void initDatabase() {
        dbHelper = new DBHelper(this);
    }

    private void initViews() {
        tvCurrentMonth = findViewById(R.id.tv_current_month);
        btnPreviousMonth = findViewById(R.id.btn_previous_month);
        btnNextMonth = findViewById(R.id.btn_next_month);
        btnBack = findViewById(R.id.btn_back);
        calendarGrid = findViewById(R.id.calendar_grid);
    }

    private void initCalendar() {
        calendar = Calendar.getInstance();
    }

    private void initDayCells() {
        dayCells = new HashMap<>();
        for (int i = 1; i <= 35; i++) {
            int dayId = getResources().getIdentifier("day_" + i, "id", getPackageName());
            if (dayId != 0) {
                LinearLayout cell = findViewById(dayId);
                dayCells.put(String.valueOf(i), cell);
            }
        }
    }

    private void updateCalendar() {
        // 更新月份显示
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        tvCurrentMonth.setText(year + "年" + month + "月");

        // 清空所有单元格
        for (LinearLayout cell : dayCells.values()) {
            cell.setBackgroundColor(getResources().getColor(android.R.color.white));
            cell.removeAllViews();
        }

        // 获取当月第一天是星期几（0-6，0表示星期日）
        Calendar tempCalendar = (Calendar) calendar.clone();
        tempCalendar.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = tempCalendar.get(Calendar.DAY_OF_WEEK) - 1; // 转换为0-6，0表示星期日

        // 获取当月的天数
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        // 填充日历
        for (int day = 1; day <= daysInMonth; day++) {
            int position = firstDayOfWeek + day - 1;
            if (position >= 0 && position < 35) {
                LinearLayout cell = dayCells.get(String.valueOf(position + 1));
                if (cell != null) {
                    // 添加日期
                    TextView dayTextView = new TextView(this);
                    dayTextView.setText(String.valueOf(day));
                    dayTextView.setTextSize(14);
                    cell.addView(dayTextView);

                    // 加载当天的课程
                    loadCoursesForDay(year, month, day, cell);
                }
            }
        }
    }

    private void loadCoursesForDay(int year, int month, int day, LinearLayout cell) {
        // 从数据库加载当天的课程
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

                // 计算当天是星期几
                Calendar tempCalendar = Calendar.getInstance();
                tempCalendar.set(year, month - 1, day);
                int currentDayOfWeek = tempCalendar.get(Calendar.DAY_OF_WEEK);
                if (currentDayOfWeek == 1) currentDayOfWeek = 7; // 将星期日从1改为7
                else currentDayOfWeek -= 1; // 将其他星期从2-7改为1-6

                if (dayOfWeek == currentDayOfWeek) {
                    TextView courseInfo = new TextView(this);
                    courseInfo.setText(courseName + "\n" + startTime + "-" + endTime);
                    courseInfo.setTextSize(10);
                    courseInfo.setPadding(0, 5, 0, 0);
                    cell.addView(courseInfo);
                    cell.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
                }
            }
            cursor.close();
        }
    }

    private void setupListeners() {
        btnPreviousMonth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calendar.add(Calendar.MONTH, -1);
                updateCalendar();
            }
        });

        btnNextMonth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calendar.add(Calendar.MONTH, 1);
                updateCalendar();
            }
        });

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