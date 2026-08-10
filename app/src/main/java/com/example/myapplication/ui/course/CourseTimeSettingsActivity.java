package com.example.myapplication.ui.course;

import androidx.appcompat.app.AppCompatActivity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.text.InputType;

import com.example.myapplication.database.DBHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CourseTimeSettingsActivity extends AppCompatActivity {

    private DBHelper dbHelper;
    private int userId;
    private LinearLayout timeSlotsContainer;
    private Button btnSave, btnBack;
    private List<TimeSlot> timeSlots;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_time_settings);

        userId = getIntent().getIntExtra("user_id", -1);
        int userType = getIntent().getIntExtra("user_type", -1);
        if (userId == -1) {
            Toast.makeText(this, "用户信息错误", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (userType != 0 && userType != 2) {
            Toast.makeText(this, "您没有权限访问该功能", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        initDatabase();
        loadTimeSlots();
        setupListeners();
    }

    private void initViews() {
        timeSlotsContainer = findViewById(R.id.time_slots_container);
        btnSave = findViewById(R.id.btn_save);
        btnBack = findViewById(R.id.btn_back);
        Button btnAddSlot = findViewById(R.id.btn_add_slot);
        Button btnRemoveSlot = findViewById(R.id.btn_remove_slot);
        timeSlots = new ArrayList<>();
        
        btnAddSlot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNewTimeSlot();
            }
        });
        
        btnRemoveSlot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeTimeSlot();
            }
        });
    }

    private void initDatabase() {
        dbHelper = new DBHelper(this);
    }

    private void loadTimeSlots() {
        // 加载默认时间槽
        timeSlots.clear();
        timeSlotsContainer.removeAllViews();

        // 检查数据库中是否有时间设置
        Cursor cursor = dbHelper.query("course_time_slot", null, "user_id = ?", 
                new String[]{String.valueOf(userId)}, null, null, "slot_order ASC");

        if (cursor != null && cursor.getCount() > 0) {
            // 从数据库加载时间槽
            while (cursor.moveToNext()) {
                int idIndex = cursor.getColumnIndex("id");
                int slotOrderIndex = cursor.getColumnIndex("slot_order");
                int startTimeIndex = cursor.getColumnIndex("start_time");
                int endTimeIndex = cursor.getColumnIndex("end_time");

                if (idIndex != -1 && slotOrderIndex != -1 && startTimeIndex != -1 && endTimeIndex != -1) {
                    int id = cursor.getInt(idIndex);
                    int slotOrder = cursor.getInt(slotOrderIndex);
                    String startTime = cursor.getString(startTimeIndex);
                    String endTime = cursor.getString(endTimeIndex);

                    TimeSlot timeSlot = new TimeSlot(id, slotOrder, startTime, endTime);
                    timeSlots.add(timeSlot);
                    addTimeSlotView(timeSlot);
                }
            }
            cursor.close();
        } else {
            // 使用默认时间槽
            String[][] defaultTimes = {
                    {"08:00", "09:40"},
                    {"10:00", "11:40"},
                    {"14:00", "15:40"},
                    {"16:00", "17:40"},
                    {"19:00", "20:40"}
            };

            for (int i = 0; i < defaultTimes.length; i++) {
                TimeSlot timeSlot = new TimeSlot(0, i + 1, defaultTimes[i][0], defaultTimes[i][1]);
                timeSlots.add(timeSlot);
                addTimeSlotView(timeSlot);
            }
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void addTimeSlotView(TimeSlot timeSlot) {
        LinearLayout timeSlotLayout = new LinearLayout(this);
        timeSlotLayout.setOrientation(LinearLayout.HORIZONTAL);
        timeSlotLayout.setPadding(0, 10, 0, 10);
        timeSlotLayout.setWeightSum(1f);

        TextView tvSlotNumber = new TextView(this);
        tvSlotNumber.setText("第" + timeSlot.getSlotOrder() + "节");
        tvSlotNumber.setWidth(80);
        tvSlotNumber.setGravity(Gravity.CENTER);
        timeSlotLayout.addView(tvSlotNumber);

        EditText etStartTime = new EditText(this);
        etStartTime.setText(timeSlot.getStartTime());
        etStartTime.setHint("开始时间");
        etStartTime.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.25f));
        etStartTime.setInputType(InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        etStartTime.setTag("start_" + timeSlot.getSlotOrder());
        timeSlotLayout.addView(etStartTime);

        TextView tvSeparator = new TextView(this);
        tvSeparator.setText("-");
        tvSeparator.setWidth(40);
        tvSeparator.setGravity(Gravity.CENTER);
        timeSlotLayout.addView(tvSeparator);

        EditText etEndTime = new EditText(this);
        etEndTime.setText(timeSlot.getEndTime());
        etEndTime.setHint("结束时间");
        etEndTime.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.25f));
        etEndTime.setInputType(InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        etEndTime.setTag("end_" + timeSlot.getSlotOrder());
        timeSlotLayout.addView(etEndTime);

        // 添加时间选择按钮
        Button btnTimePicker = new Button(this);
        btnTimePicker.setText("选择");
        btnTimePicker.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.2f));
        btnTimePicker.setTag("picker_" + timeSlot.getSlotOrder());
        btnTimePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePickerDialog(timeSlot.getSlotOrder());
            }
        });
        timeSlotLayout.addView(btnTimePicker);

        timeSlotsContainer.addView(timeSlotLayout);
    }

    private void setupListeners() {
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveTimeSlots();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void addNewTimeSlot() {
        int newSlotOrder = timeSlots.size() + 1;
        TimeSlot timeSlot = new TimeSlot(0, newSlotOrder, "00:00", "00:00");
        timeSlots.add(timeSlot);
        addTimeSlotView(timeSlot);
    }

    private void removeTimeSlot() {
        if (timeSlots.size() > 1) {
            timeSlots.remove(timeSlots.size() - 1);
            timeSlotsContainer.removeViewAt(timeSlotsContainer.getChildCount() - 1);
        } else {
            Toast.makeText(this, "至少保留一个时间槽", Toast.LENGTH_SHORT).show();
        }
    }

    private void showTimePickerDialog(int slotOrder) {
        // 创建时间选择对话框
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("选择时间");

        // 创建时间选择器
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);

        TextView startTimeLabel = new TextView(this);
        startTimeLabel.setText("开始时间:");
        layout.addView(startTimeLabel);

        TimePicker startTimePicker = new TimePicker(this);
        startTimePicker.setIs24HourView(true);
        layout.addView(startTimePicker);

        TextView endTimeLabel = new TextView(this);
        endTimeLabel.setText("结束时间:");
        endTimeLabel.setPadding(0, 20, 0, 0);
        layout.addView(endTimeLabel);

        TimePicker endTimePicker = new TimePicker(this);
        endTimePicker.setIs24HourView(true);
        layout.addView(endTimePicker);

        builder.setView(layout);

        // 设置确定按钮
        builder.setPositiveButton("确定", (dialog, which) -> {
            // 获取选择的时间
            int startHour = startTimePicker.getCurrentHour();
            int startMinute = startTimePicker.getCurrentMinute();
            int endHour = endTimePicker.getCurrentHour();
            int endMinute = endTimePicker.getCurrentMinute();

            // 格式化时间
            String startTime = String.format(Locale.ROOT, "%02d:%02d", startHour, startMinute);
            String endTime = String.format(Locale.ROOT, "%02d:%02d", endHour, endMinute);

            // 更新UI
            for (int i = 0; i < timeSlotsContainer.getChildCount(); i++) {
                View child = timeSlotsContainer.getChildAt(i);
                if (child instanceof LinearLayout) {
                    LinearLayout timeSlotLayout = (LinearLayout) child;
                    for (int j = 0; j < timeSlotLayout.getChildCount(); j++) {
                        View view = timeSlotLayout.getChildAt(j);
                        if (view instanceof EditText) {
                            EditText editText = (EditText) view;
                            String tag = (String) editText.getTag();
                            if (tag.equals("start_" + slotOrder)) {
                                editText.setText(startTime);
                            } else if (tag.equals("end_" + slotOrder)) {
                                editText.setText(endTime);
                            }
                        }
                    }
                }
            }
        });

        // 设置取消按钮
        builder.setNegativeButton("取消", null);

        // 显示对话框
        builder.show();
    }

    private void saveTimeSlots() {
        // 验证时间格式和逻辑
        if (!validateTimeSlots()) {
            return;
        }

        // 清空现有时间槽
        dbHelper.delete("course_time_slot", "user_id = ?", new String[]{String.valueOf(userId)});

        // 保存新的时间槽，重新排序slot_order
        for (int i = 0; i < timeSlots.size(); i++) {
            TimeSlot timeSlot = timeSlots.get(i);
            View timeSlotView = timeSlotsContainer.getChildAt(i);

            if (timeSlotView instanceof LinearLayout) {
                LinearLayout layout = (LinearLayout) timeSlotView;
                for (int j = 0; j < layout.getChildCount(); j++) {
                    View child = layout.getChildAt(j);
                    if (child instanceof EditText) {
                        EditText editText = (EditText) child;
                        String tag = (String) editText.getTag();
                        if (tag.startsWith("start_")) {
                            timeSlot.setStartTime(editText.getText().toString());
                        } else if (tag.startsWith("end_")) {
                            timeSlot.setEndTime(editText.getText().toString());
                        }
                    }
                }

                // 保存到数据库，重新排序slot_order
                ContentValues values = new ContentValues();
                values.put("user_id", userId);
                values.put("slot_order", i + 1);
                values.put("start_time", timeSlot.getStartTime());
                values.put("end_time", timeSlot.getEndTime());
                dbHelper.insert("course_time_slot", values);
            }
        }

        Toast.makeText(this, "时间设置保存成功", Toast.LENGTH_SHORT).show();
        finish();
    }

    private boolean validateTimeSlots() {
        for (int i = 0; i < timeSlotsContainer.getChildCount(); i++) {
            View timeSlotView = timeSlotsContainer.getChildAt(i);
            if (timeSlotView instanceof LinearLayout) {
                LinearLayout layout = (LinearLayout) timeSlotView;
                String startTime = "";
                String endTime = "";

                for (int j = 0; j < layout.getChildCount(); j++) {
                    View child = layout.getChildAt(j);
                    if (child instanceof EditText) {
                        EditText editText = (EditText) child;
                        String tag = (String) editText.getTag();
                        if (tag.startsWith("start_")) {
                            startTime = editText.getText().toString().trim();
                        } else if (tag.startsWith("end_")) {
                            endTime = editText.getText().toString().trim();
                        }
                    }
                }

                // 验证时间格式
                if (!isValidTimeFormat(startTime)) {
                    Toast.makeText(this, "第" + (i + 1) + "节开始时间格式错误", Toast.LENGTH_SHORT).show();
                    return false;
                }

                if (!isValidTimeFormat(endTime)) {
                    Toast.makeText(this, "第" + (i + 1) + "节结束时间格式错误", Toast.LENGTH_SHORT).show();
                    return false;
                }

                // 验证时间逻辑
                if (!isValidTimeLogic(startTime, endTime)) {
                    Toast.makeText(this, "第" + (i + 1) + "节时间逻辑错误，结束时间必须晚于开始时间", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isValidTimeFormat(String time) {
        if (time == null || time.isEmpty()) {
            return false;
        }

        // 验证时间格式：HH:MM
        String timePattern = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$";
        return time.matches(timePattern);
    }

    private boolean isValidTimeLogic(String startTime, String endTime) {
        try {
            String[] startParts = startTime.split(":");
            String[] endParts = endTime.split(":");

            int startHour = Integer.parseInt(startParts[0]);
            int startMinute = Integer.parseInt(startParts[1]);
            int endHour = Integer.parseInt(endParts[0]);
            int endMinute = Integer.parseInt(endParts[1]);

            int startTotalMinutes = startHour * 60 + startMinute;
            int endTotalMinutes = endHour * 60 + endMinute;

            return endTotalMinutes > startTotalMinutes;
        } catch (Exception e) {
            return false;
        }
    }

    private class TimeSlot {
        private int id;
        private int slotOrder;
        private String startTime;
        private String endTime;

        public TimeSlot(int id, int slotOrder, String startTime, String endTime) {
            this.id = id;
            this.slotOrder = slotOrder;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public int getId() {
            return id;
        }

        public int getSlotOrder() {
            return slotOrder;
        }

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }
    }
}
