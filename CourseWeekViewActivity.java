package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.example.myapplication.database.DBHelper;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class CourseWeekViewActivity extends AppCompatActivity {

    private DBHelper dbHelper;
    private int userId;
    private int userType;
    private Map<String, LinearLayout> courseCells;
    private TextView tvWeekDateRange;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_week_view);

        userId = getIntent().getIntExtra("user_id", -1);
        userType = getIntent().getIntExtra("user_type", -1);
        
        if (userId == -1) {
            finish();
            return;
        }
        
        if (userType != 0 && userType != 2) {
            finish();
            return;
        }

        initViews();
        showWeekDateRange();
        
        initDatabase();
        initCourseCells();
        loadTimeSlots();
        loadCourses();
        setupListeners();
    }



    private void initViews() {
        tvWeekDateRange = findViewById(R.id.tv_week_date_range);
    }

    private void showWeekDateRange() {
        // 计算当前周的开始和结束日期
        Calendar calendar = Calendar.getInstance();
        
        // 获取当前是星期几（1-7，1表示星期日）
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        
        // 计算本周一的日期
        Calendar monday = (Calendar) calendar.clone();
        monday.add(Calendar.DAY_OF_WEEK, - (dayOfWeek - 2)); // 调整到本周一
        if (dayOfWeek == 1) { // 如果是星期日
            monday.add(Calendar.DAY_OF_WEEK, -7);
        }
        
        // 计算本周日的日期
        Calendar sunday = (Calendar) monday.clone();
        sunday.add(Calendar.DAY_OF_WEEK, 6);
        
        // 显示周日期范围
        String weekRange = String.format("%d年%d月%d日 - %d年%d月%d日",
                monday.get(Calendar.YEAR), monday.get(Calendar.MONTH) + 1, monday.get(Calendar.DAY_OF_MONTH),
                sunday.get(Calendar.YEAR), sunday.get(Calendar.MONTH) + 1, sunday.get(Calendar.DAY_OF_MONTH));
        tvWeekDateRange.setText(weekRange);
        
        // 更新星期标题，显示具体日期
        updateDayHeaders(monday);
    }

    private void updateDayHeaders(Calendar startDate) {
        // 更新星期标题，显示具体日期
        String[] dayTexts = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        TextView[] dayHeaders = new TextView[7];
        
        // 直接通过ID获取星期标题TextView
        dayHeaders[0] = findViewById(R.id.day_header_monday);
        dayHeaders[1] = findViewById(R.id.day_header_tuesday);
        dayHeaders[2] = findViewById(R.id.day_header_wednesday);
        dayHeaders[3] = findViewById(R.id.day_header_thursday);
        dayHeaders[4] = findViewById(R.id.day_header_friday);
        dayHeaders[5] = findViewById(R.id.day_header_saturday);
        dayHeaders[6] = findViewById(R.id.day_header_sunday);
        
        for (int i = 0; i < 7; i++) {
            Calendar currentDay = (Calendar) startDate.clone();
            currentDay.add(Calendar.DAY_OF_WEEK, i);
            
            if (dayHeaders[i] != null) {
                String dayText = dayTexts[i] + "\n" + 
                               (currentDay.get(Calendar.MONTH) + 1) + "/" + currentDay.get(Calendar.DAY_OF_MONTH);
                dayHeaders[i].setText(dayText);
            }
        }
    }

    private String getDayOfWeekString(int dayOfWeek) {
        switch (dayOfWeek) {
            case 1:
                return "周一";
            case 2:
                return "周二";
            case 3:
                return "周三";
            case 4:
                return "周四";
            case 5:
                return "周五";
            case 6:
                return "周六";
            case 7:
                return "周日";
            default:
                return "";
        }
    }

    private void initDatabase() {
        dbHelper = new DBHelper(this);
    }

    private void initCourseCells() {
        courseCells = new HashMap<>();
        try {
            // 加载默认的5个时间槽
            courseCells.put("1_1", findViewById(R.id.monday_1));
            courseCells.put("1_2", findViewById(R.id.tuesday_1));
            courseCells.put("1_3", findViewById(R.id.wednesday_1));
            courseCells.put("1_4", findViewById(R.id.thursday_1));
            courseCells.put("1_5", findViewById(R.id.friday_1));
            courseCells.put("1_6", findViewById(R.id.saturday_1));
            courseCells.put("1_7", findViewById(R.id.sunday_1));

            courseCells.put("2_1", findViewById(R.id.monday_2));
            courseCells.put("2_2", findViewById(R.id.tuesday_2));
            courseCells.put("2_3", findViewById(R.id.wednesday_2));
            courseCells.put("2_4", findViewById(R.id.thursday_2));
            courseCells.put("2_5", findViewById(R.id.friday_2));
            courseCells.put("2_6", findViewById(R.id.saturday_2));
            courseCells.put("2_7", findViewById(R.id.sunday_2));

            courseCells.put("3_1", findViewById(R.id.monday_3));
            courseCells.put("3_2", findViewById(R.id.tuesday_3));
            courseCells.put("3_3", findViewById(R.id.wednesday_3));
            courseCells.put("3_4", findViewById(R.id.thursday_3));
            courseCells.put("3_5", findViewById(R.id.friday_3));
            courseCells.put("3_6", findViewById(R.id.saturday_3));
            courseCells.put("3_7", findViewById(R.id.sunday_3));

            courseCells.put("4_1", findViewById(R.id.monday_4));
            courseCells.put("4_2", findViewById(R.id.tuesday_4));
            courseCells.put("4_3", findViewById(R.id.wednesday_4));
            courseCells.put("4_4", findViewById(R.id.thursday_4));
            courseCells.put("4_5", findViewById(R.id.friday_4));
            courseCells.put("4_6", findViewById(R.id.saturday_4));
            courseCells.put("4_7", findViewById(R.id.sunday_4));

            courseCells.put("5_1", findViewById(R.id.monday_5));
            courseCells.put("5_2", findViewById(R.id.tuesday_5));
            courseCells.put("5_3", findViewById(R.id.wednesday_5));
            courseCells.put("5_4", findViewById(R.id.thursday_5));
            courseCells.put("5_5", findViewById(R.id.friday_5));
            courseCells.put("5_6", findViewById(R.id.saturday_5));
            courseCells.put("5_7", findViewById(R.id.sunday_5));
        } catch (Exception e) {
            e.printStackTrace();
            // 如果出现异常，不影响其他功能
        }
    }

    private void loadTimeSlots() {
        // 从数据库加载时间槽并更新UI
        try {
            Cursor cursor = dbHelper.query("course_time_slot", null, "user_id = ?", 
                    new String[]{String.valueOf(userId)}, null, null, "slot_order ASC");

            if (cursor != null) {
                int slotOrderIndex = cursor.getColumnIndex("slot_order");
                int startTimeIndex = cursor.getColumnIndex("start_time");
                int endTimeIndex = cursor.getColumnIndex("end_time");

                int slotCount = 0;
                while (cursor.moveToNext()) {
                    slotCount++;
                    if (slotOrderIndex != -1 && startTimeIndex != -1 && endTimeIndex != -1) {
                        int slotOrder = cursor.getInt(slotOrderIndex);
                        String startTime = cursor.getString(startTimeIndex);
                        String endTime = cursor.getString(endTimeIndex);
                        
                        // 更新时间槽的显示时间
                        updateTimeSlotTime(slotOrder, startTime, endTime);
                    }
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 只有管理员登录时才显示错误信息
            if (userType == 2) {
                android.util.Log.e("CourseWeekView", "加载时间槽失败: " + e.getMessage());
            }
        }
    }

    private void updateTimeSlotTime(int slotOrder, String startTime, String endTime) {
        // 根据slotOrder更新对应的时间槽时间显示
        try {
            int timeTextViewId = 0;
            switch (slotOrder) {
                case 1:
                    timeTextViewId = R.id.time_slot_1;
                    break;
                case 2:
                    timeTextViewId = R.id.time_slot_2;
                    break;
                case 3:
                    timeTextViewId = R.id.time_slot_3;
                    break;
                case 4:
                    timeTextViewId = R.id.time_slot_4;
                    break;
                case 5:
                    timeTextViewId = R.id.time_slot_5;
                    break;
                default:
                    return;
            }
            
            TextView timeTextView = findViewById(timeTextViewId);
            if (timeTextView != null) {
                timeTextView.setText(startTime + "-" + endTime);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 只有管理员登录时才显示错误信息
            if (userType == 2) {
                android.util.Log.e("CourseWeekView", "更新时间槽时间失败: " + e.getMessage());
            }
        }
    }

    private void loadCourses() {
        try {
            // 先清空所有课程单元格
            if (courseCells != null) {
                for (LinearLayout cell : courseCells.values()) {
                    if (cell != null) {
                        cell.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                        cell.removeAllViews();
                    }
                }
            }
            
            Cursor cursor = dbHelper.getCoursesByUserId(userId);
            if (cursor != null) {
                int courseNameIndex = cursor.getColumnIndex("course_name");
                int teacherNameIndex = cursor.getColumnIndex("teacher_name");
                int classroomIndex = cursor.getColumnIndex("classroom");
                int startTimeIndex = cursor.getColumnIndex("start_time");
                int endTimeIndex = cursor.getColumnIndex("end_time");
                int dayOfWeekIndex = cursor.getColumnIndex("day_of_week");

                // 检查所有必要的列索引是否有效
                if (courseNameIndex != -1 && teacherNameIndex != -1 && classroomIndex != -1 && 
                    startTimeIndex != -1 && endTimeIndex != -1 && dayOfWeekIndex != -1) {
                    while (cursor.moveToNext()) {
                        String courseName = cursor.getString(courseNameIndex);
                        String teacherName = cursor.getString(teacherNameIndex);
                        String classroom = cursor.getString(classroomIndex);
                        String startTime = cursor.getString(startTimeIndex);
                        String endTime = cursor.getString(endTimeIndex);
                        int dayOfWeek = cursor.getInt(dayOfWeekIndex);

                        // 跳过无效数据
                        if (courseName == null || startTime == null || dayOfWeek < 1 || dayOfWeek > 7) continue;

                        int timeSlot = getTimeSlot(startTime);
                        if (timeSlot != -1) {
                            String key = timeSlot + "_" + dayOfWeek;
                            LinearLayout cell = courseCells.get(key);
                            if (cell != null) {
                                cell.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
                                cell.removeAllViews();

                                // 构建课程信息文本，确保所有信息都能显示
                                StringBuilder courseInfoText = new StringBuilder();
                                courseInfoText.append(courseName).append("\n");
                                if (teacherName != null && !teacherName.isEmpty()) {
                                    courseInfoText.append(teacherName).append("\n");
                                }
                                if (classroom != null && !classroom.isEmpty()) {
                                    courseInfoText.append(classroom).append("\n");
                                }
                                if (startTime != null && endTime != null) {
                                    courseInfoText.append(startTime).append("-").append(endTime);
                                }

                                TextView courseInfo = new TextView(this);
                                courseInfo.setText(courseInfoText.toString());
                                courseInfo.setTextSize(12);
                                courseInfo.setPadding(5, 5, 5, 5);
                                courseInfo.setTextColor(getResources().getColor(android.R.color.white));
                                courseInfo.setSingleLine(false);
                                courseInfo.setEllipsize(null);
                                cell.addView(courseInfo);
                            }
                        }
                    }
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 如果出现异常，不影响其他功能
        }
    }

    private int getTimeSlot(String startTime) {
        // 处理空值情况
        if (startTime == null || startTime.isEmpty()) {
            return -1;
        }
        
        // 从数据库加载用户自定义的时间槽
        Cursor cursor = dbHelper.query("course_time_slot", null, "user_id = ?", 
                new String[]{String.valueOf(userId)}, null, null, "slot_order ASC");

        if (cursor != null) {
            int slotOrderIndex = cursor.getColumnIndex("slot_order");
            int startTimeIndex = cursor.getColumnIndex("start_time");

            if (slotOrderIndex != -1 && startTimeIndex != -1) {
                while (cursor.moveToNext()) {
                    int slotOrder = cursor.getInt(slotOrderIndex);
                    String slotStartTime = cursor.getString(startTimeIndex);
                    if (slotStartTime != null && slotStartTime.equals(startTime)) {
                        cursor.close();
                        return slotOrder;
                    }
                }
            }
            cursor.close();
        }

        // 如果没有找到匹配的时间槽，使用默认时间槽
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
                // 对于不在默认时间槽中的时间，尝试根据时间范围分配到合适的时间槽
                try {
                    String[] parts = startTime.split(":");
                    if (parts.length == 2) {
                        int hour = Integer.parseInt(parts[0]);
                        if (hour >= 8 && hour < 10) return 1;
                        if (hour >= 10 && hour < 14) return 2;
                        if (hour >= 14 && hour < 16) return 3;
                        if (hour >= 16 && hour < 19) return 4;
                        if (hour >= 19) return 5;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // 如果无法解析时间，尝试使用默认时间槽
                // 对于示例数据，确保能分配到时间槽
                return 1; // 默认返回第一个时间槽
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