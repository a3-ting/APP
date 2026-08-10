package com.example.myapplication.ui.study;

import androidx.appcompat.app.AppCompatActivity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.database.DBHelper;
import com.example.myapplication.utils.DialogUtils;
import com.example.myapplication.utils.StudyDataAnalyzer;
import com.example.myapplication.utils.AIStudyPlanRecommender;

import java.util.ArrayList;
import java.util.List;

public class StudyPlanActivity extends AppCompatActivity {

    private TextView tvPlanCount, tvCompletedCount;
    private Spinner spPlanStatus;
    private ListView lvStudyPlans;
    private Button btnAddPlan, btnRecommendTime, btnBack;
    private DBHelper dbHelper;
    private int userId;
    private List<String> planList;
    private List<Integer> planIdList;
    private ArrayAdapter<String> planAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_plan);

        userId = getIntent().getIntExtra("user_id", -1);
        int userType = getIntent().getIntExtra("user_type", -1);
        if (userId == -1) {
            DialogUtils.showErrorDialog(this, "用户信息错误");
            finish();
            return;
        }
        if (userType != 0 && userType != 2) {
            DialogUtils.showWarningDialog(this, "您没有权限访问该功能");
            finish();
            return;
        }

        initViews();
        initDatabase();
        loadStudyPlans();
        updatePlanStats();
        setupListeners();
    }

    private void initViews() {
        tvPlanCount = findViewById(R.id.tv_plan_count);
        tvCompletedCount = findViewById(R.id.tv_completed_count);
        spPlanStatus = findViewById(R.id.sp_plan_status);
        lvStudyPlans = findViewById(R.id.lv_study_plans);
        btnAddPlan = findViewById(R.id.btn_add_plan);
        btnRecommendTime = findViewById(R.id.btn_recommend_time);
        btnBack = findViewById(R.id.btn_back);

        // 设置计划状态筛选选项
        ArrayAdapter<CharSequence> statusAdapter = ArrayAdapter.createFromResource(this,
                R.array.study_plan_status_options, android.R.layout.simple_spinner_item);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPlanStatus.setAdapter(statusAdapter);

        // 初始化学习计划列表
        planList = new ArrayList<>();
        planIdList = new ArrayList<>();
        planAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, planList);
        lvStudyPlans.setAdapter(planAdapter);
    }

    private void initDatabase() {
        dbHelper = new DBHelper(this);
    }

    private void loadStudyPlans() {
        planList.clear();
        planIdList.clear();
        Cursor cursor = dbHelper.getStudyPlansByUserId(userId);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int planId = 0;
                int idIndex = cursor.getColumnIndex("id");
                if (idIndex != -1) {
                    planId = cursor.getInt(idIndex);
                }
                String title = "";
                int titleIndex = cursor.getColumnIndex("title");
                if (titleIndex != -1) {
                    title = cursor.getString(titleIndex);
                }
                String description = "";
                int descriptionIndex = cursor.getColumnIndex("description");
                if (descriptionIndex != -1) {
                    description = cursor.getString(descriptionIndex);
                }
                String startDate = "";
                int startDateIndex = cursor.getColumnIndex("start_date");
                if (startDateIndex != -1) {
                    startDate = cursor.getString(startDateIndex);
                }
                String endDate = "";
                int endDateIndex = cursor.getColumnIndex("end_date");
                if (endDateIndex != -1) {
                    endDate = cursor.getString(endDateIndex);
                }
                int progress = 0;
                int progressIndex = cursor.getColumnIndex("progress");
                if (progressIndex != -1) {
                    progress = cursor.getInt(progressIndex);
                }
                int priority = 0;
                int priorityIndex = cursor.getColumnIndex("priority");
                if (priorityIndex != -1) {
                    priority = cursor.getInt(priorityIndex);
                }
                int status = 0;
                int statusIndex = cursor.getColumnIndex("status");
                if (statusIndex != -1) {
                    status = cursor.getInt(statusIndex);
                }

                String priorityStr = getPriorityString(priority);
                String statusStr = getStatusString(status);
                
                String planInfo = title + "\n" +
                        "描述：" + description + "\n" +
                        "时间：" + startDate + " 至 " + endDate + "\n" +
                        "进度：" + progress + "% | 优先级：" + priorityStr + " | 状态：" + statusStr;
                planList.add(planInfo);
                planIdList.add(planId);
            }
            cursor.close();
        }
        planAdapter.notifyDataSetChanged();
    }

    private void updatePlanStats() {
        int totalPlans = planList.size();
        int completedPlans = countCompletedPlans();
        tvPlanCount.setText("总计划数：" + totalPlans);
        tvCompletedCount.setText("已完成：" + completedPlans);
    }

    private int countCompletedPlans() {
        int count = 0;
        Cursor cursor = dbHelper.getStudyPlansByUserId(userId);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int status = 0;
                int statusIndex = cursor.getColumnIndex("status");
                if (statusIndex != -1) {
                    status = cursor.getInt(statusIndex);
                }
                if (status == 2) { // 已完成
                    count++;
                }
            }
            cursor.close();
        }
        return count;
    }

    private String getPriorityString(int priority) {
        String[] priorities = {"低", "中", "高"};
        if (priority >= 0 && priority < priorities.length) {
            return priorities[priority];
        }
        return "未知";
    }

    private String getStatusString(int status) {
        String[] statuses = {"未开始", "进行中", "已完成"};
        if (status >= 0 && status < statuses.length) {
            return statuses[status];
        }
        return "未知";
    }

    private void setupListeners() {
        spPlanStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String status = parent.getItemAtPosition(position).toString();
                filterPlans(status);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        lvStudyPlans.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 点击学习计划项，可以进行编辑或查看详情
                showPlanDetails(position);
            }
        });

        lvStudyPlans.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                // 长按学习计划项，显示操作菜单
                showPlanOptions(position);
                return true;
            }
        });

        btnAddPlan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddPlanDialog();
            }
        });

        btnRecommendTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recommendStudyTime();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void showAddPlanDialog() {
        // 简化版的学习计划添加，实际应用中可以使用AlertDialog或单独的Activity
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_study_plan, null);
        EditText etTitle = dialogView.findViewById(R.id.et_plan_title);
        EditText etDescription = dialogView.findViewById(R.id.et_plan_description);
        EditText etTarget = dialogView.findViewById(R.id.et_plan_target);
        EditText etDeadline = dialogView.findViewById(R.id.et_plan_deadline);
        Button btnSave = dialogView.findViewById(R.id.btn_save_plan);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel_plan);

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("添加学习计划")
                .setView(dialogView)
                .setPositiveButton("保存", (dialog, which) -> {
                    String title = etTitle.getText().toString();
                    String description = etDescription.getText().toString();
                    String target = etTarget.getText().toString();
                    String deadline = etDeadline.getText().toString();

                    if (title.isEmpty() || deadline.isEmpty()) {
                        Toast.makeText(StudyPlanActivity.this, "请填写完整信息", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 保存学习计划到数据库
                    ContentValues values = new ContentValues();
                    values.put("user_id", userId);
                    values.put("title", title);
                    values.put("description", description);
                    values.put("start_date", deadline);
                    values.put("end_date", deadline);
                    values.put("progress", 0);
                    values.put("priority", 1); // 默认中等优先级
                    values.put("status", 0); // 未开始

                    dbHelper.insertStudyPlan(values);
                    Toast.makeText(StudyPlanActivity.this, "学习计划已添加", Toast.LENGTH_SHORT).show();
                    loadStudyPlans();
                    updatePlanStats();
                })
                .setNegativeButton("取消", null);
        builder.show();
    }

    private void recommendStudyTime() {
        // 收集用户学习数据
        StudyDataAnalyzer.UserStudyData userData = StudyDataAnalyzer.collectUserStudyData(dbHelper, userId);
        
        // 生成AI推荐
        List<AIStudyPlanRecommender.StudyPlanRecommendation> recommendations = AIStudyPlanRecommender.generateRecommendations(userData);
        
        // 生成详细学习计划
        String detailedPlan = AIStudyPlanRecommender.generateDetailedStudyPlan(userData, recommendations);

        DialogUtils.showLongMessageDialog(this, "AI 个性化学习计划推荐", detailedPlan);
    }

    private void showPlanDetails(int position) {
        // 显示学习计划详情
        Toast.makeText(this, "查看学习计划详情：" + planList.get(position), Toast.LENGTH_SHORT).show();
    }

    private void showPlanOptions(int position) {
        // 显示学习计划操作菜单
        String[] options = {"编辑计划", "删除计划", "更新进度", "调整优先级", "调整时间", "设置提醒", "分享计划"};
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("学习计划操作")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            editPlan(position);
                            break;
                        case 1:
                            deletePlan(position);
                            break;
                        case 2:
                            updateProgress(position);
                            break;
                        case 3:
                            adjustPriority(position);
                            break;
                        case 4:
                            adjustTime(position);
                            break;
                        case 5:
                            setReminder(position);
                            break;
                        case 6:
                            sharePlan(position);
                            break;
                    }
                });
        builder.show();
    }

    private void setReminder(int position) {
        // 设置学习计划提醒
        int planId = planIdList.get(position);
        EditText etReminderTime = new EditText(this);
        etReminderTime.setHint("请输入提醒时间 (格式：2024-01-01 08:00)");
        etReminderTime.setPadding(20, 20, 20, 20);

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("设置学习计划提醒")
                .setView(etReminderTime)
                .setPositiveButton("保存", (dialog, which) -> {
                    String reminderTime = etReminderTime.getText().toString().trim();

                    if (reminderTime.isEmpty()) {
                        Toast.makeText(StudyPlanActivity.this, "请输入提醒时间", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 保存提醒到提醒表
                    ContentValues values = new ContentValues();
                    values.put("user_id", userId);
                    values.put("type", 0); // 课程提醒
                    values.put("title", "学习计划提醒");
                    values.put("content", "您有一个学习计划需要完成");
                    values.put("remind_time", reminderTime);
                    values.put("is_sent", 0);
                    dbHelper.insertReminder(values);

                    // 这里可以添加设置系统闹钟的代码
                    // 由于需要权限，暂时只保存提醒设置

                    Toast.makeText(StudyPlanActivity.this, "学习计划提醒设置成功", Toast.LENGTH_SHORT).show();
                    loadStudyPlans(); // 重新加载学习计划列表
                })
                .setNegativeButton("取消", null);
        builder.show();
    }

    private void editPlan(int position) {
        // 编辑学习计划
        int planId = planIdList.get(position);
        Cursor cursor = dbHelper.query("study_plan", null, "id = ?", new String[]{String.valueOf(planId)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            String title = "";
            int titleIndex = cursor.getColumnIndex("title");
            if (titleIndex != -1) {
                title = cursor.getString(titleIndex);
            }
            String description = "";
            int descriptionIndex = cursor.getColumnIndex("description");
            if (descriptionIndex != -1) {
                description = cursor.getString(descriptionIndex);
            }
            String startDate = "";
            int startDateIndex = cursor.getColumnIndex("start_date");
            if (startDateIndex != -1) {
                startDate = cursor.getString(startDateIndex);
            }
            String endDate = "";
            int endDateIndex = cursor.getColumnIndex("end_date");
            if (endDateIndex != -1) {
                endDate = cursor.getString(endDateIndex);
            }
            int priorityValue = 1; // 默认中等优先级
            int priorityIndex = cursor.getColumnIndex("priority");
            if (priorityIndex != -1) {
                priorityValue = cursor.getInt(priorityIndex);
            }
            final int finalPriority = priorityValue;
            cursor.close();

            View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_study_plan, null);
            EditText etTitle = dialogView.findViewById(R.id.et_plan_title);
            EditText etDescription = dialogView.findViewById(R.id.et_plan_description);
            EditText etTarget = dialogView.findViewById(R.id.et_plan_target);
            EditText etDeadline = dialogView.findViewById(R.id.et_plan_deadline);
            Button btnSave = dialogView.findViewById(R.id.btn_save_plan);
            Button btnCancel = dialogView.findViewById(R.id.btn_cancel_plan);

            etTitle.setText(title);
            etDescription.setText(description);
            etTarget.setText("");
            etDeadline.setText(startDate);

            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("编辑学习计划")
                    .setView(dialogView)
                    .setPositiveButton("保存", (dialog, which) -> {
                        String newTitle = etTitle.getText().toString();
                        String newDescription = etDescription.getText().toString();
                        String newTarget = etTarget.getText().toString();
                        String newDeadline = etDeadline.getText().toString();

                        if (newTitle.isEmpty() || newDeadline.isEmpty()) {
                            Toast.makeText(StudyPlanActivity.this, "请填写完整信息", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // 更新学习计划到数据库
                        ContentValues values = new ContentValues();
                        values.put("title", newTitle);
                        values.put("description", newDescription);
                        values.put("start_date", newDeadline);
                        values.put("end_date", newDeadline);
                        values.put("priority", finalPriority);

                        dbHelper.update("study_plan", values, "id = ?", new String[]{String.valueOf(planId)});
                        Toast.makeText(StudyPlanActivity.this, "学习计划已更新", Toast.LENGTH_SHORT).show();
                        loadStudyPlans();
                    })
                    .setNegativeButton("取消", null);
            builder.show();
        }
    }

    private void deletePlan(int position) {
        // 删除学习计划
        int planId = planIdList.get(position);
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("确认删除")
                .setMessage("确定要删除这个学习计划吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    // 从数据库中删除学习计划
                    dbHelper.delete("study_plan", "id = ?", new String[]{String.valueOf(planId)});
                    Toast.makeText(StudyPlanActivity.this, "学习计划已删除", Toast.LENGTH_SHORT).show();
                    loadStudyPlans(); // 重新加载学习计划列表
                    updatePlanStats(); // 重新更新统计数据
                })
                .setNegativeButton("取消", null);
        builder.show();
    }

    private void updateProgress(int position) {
        // 更新学习计划进度
        int planId = planIdList.get(position);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_update_progress, null);
        SeekBar sbProgress = dialogView.findViewById(R.id.sb_progress);
        TextView tvProgress = dialogView.findViewById(R.id.tv_progress_value);
        EditText etProgress = dialogView.findViewById(R.id.et_progress);

        // 获取当前进度
        Cursor cursor = dbHelper.query("study_plan", new String[]{"progress"}, "id = ?", new String[]{String.valueOf(planId)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int currentProgress = 0;
            int progressIndex = cursor.getColumnIndex("progress");
            if (progressIndex != -1) {
                currentProgress = cursor.getInt(progressIndex);
            }
            sbProgress.setProgress(currentProgress);
            tvProgress.setText(currentProgress + "%");
            etProgress.setText(String.valueOf(currentProgress));
            cursor.close();
        }

        sbProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvProgress.setText(progress + "%");
                etProgress.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("更新进度")
                .setView(dialogView)
                .setPositiveButton("保存", (dialog, which) -> {
                    int newProgress = sbProgress.getProgress();

                    // 更新进度到数据库
                    ContentValues values = new ContentValues();
                    values.put("progress", newProgress);
                    // 根据进度更新状态
                    if (newProgress >= 100) {
                        values.put("status", 2); // 已完成
                    } else if (newProgress > 0) {
                        values.put("status", 1); // 进行中
                    } else {
                        values.put("status", 0); // 未开始
                    }

                    dbHelper.update("study_plan", values, "id = ?", new String[]{String.valueOf(planId)});
                    Toast.makeText(StudyPlanActivity.this, "进度更新成功", Toast.LENGTH_SHORT).show();
                    loadStudyPlans(); // 重新加载学习计划列表
                    updatePlanStats(); // 重新更新统计数据
                })
                .setNegativeButton("取消", null);
        builder.show();
    }

    private void adjustPriority(int position) {
        // 调整学习计划优先级
        int planId = planIdList.get(position);
        String[] priorities = {"低", "中", "高"};
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("调整优先级")
                .setItems(priorities, (dialog, which) -> {
                    // 更新优先级到数据库
                    ContentValues values = new ContentValues();
                    values.put("priority", which);
                    dbHelper.update("study_plan", values, "id = ?", new String[]{String.valueOf(planId)});
                    Toast.makeText(StudyPlanActivity.this, "优先级已调整为：" + priorities[which], Toast.LENGTH_SHORT).show();
                    loadStudyPlans(); // 重新加载学习计划列表
                });
        builder.show();
    }

    private void adjustTime(int position) {
        // 调整学习计划时间
        int planId = planIdList.get(position);
        Cursor cursor = dbHelper.query("study_plan", new String[]{"start_date", "end_date"}, "id = ?", new String[]{String.valueOf(planId)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            String startDate = "";
            int startDateIndex = cursor.getColumnIndex("start_date");
            if (startDateIndex != -1) {
                startDate = cursor.getString(startDateIndex);
            }
            String endDate = "";
            int endDateIndex = cursor.getColumnIndex("end_date");
            if (endDateIndex != -1) {
                endDate = cursor.getString(endDateIndex);
            }
            cursor.close();

            View dialogView = getLayoutInflater().inflate(R.layout.dialog_adjust_time, null);
            EditText etStartDate = dialogView.findViewById(R.id.et_start_date);
            EditText etEndDate = dialogView.findViewById(R.id.et_end_date);

            etStartDate.setText(startDate);
            etEndDate.setText(endDate);

            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("调整时间")
                    .setView(dialogView)
                    .setPositiveButton("保存", (dialog, which) -> {
                        String newStartDate = etStartDate.getText().toString();
                        String newEndDate = etEndDate.getText().toString();

                        if (newStartDate.isEmpty() || newEndDate.isEmpty()) {
                            Toast.makeText(StudyPlanActivity.this, "请填写完整时间", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // 更新时间到数据库
                        ContentValues values = new ContentValues();
                        values.put("start_date", newStartDate);
                        values.put("end_date", newEndDate);
                        dbHelper.update("study_plan", values, "id = ?", new String[]{String.valueOf(planId)});
                        Toast.makeText(StudyPlanActivity.this, "时间调整成功", Toast.LENGTH_SHORT).show();
                        loadStudyPlans(); // 重新加载学习计划列表
                    })
                    .setNegativeButton("取消", null);
            builder.show();
        }
    }

    private void sharePlan(int position) {
        // 分享学习计划
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, planList.get(position));
        startActivity(Intent.createChooser(shareIntent, "分享学习计划"));
    }

    private void filterPlans(String status) {
        // 按状态筛选学习计划
        planList.clear();
        planIdList.clear();
        Cursor cursor = dbHelper.getStudyPlansByUserId(userId);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int planId = 0;
                int idIndex = cursor.getColumnIndex("id");
                if (idIndex != -1) {
                    planId = cursor.getInt(idIndex);
                }
                String title = "";
                int titleIndex = cursor.getColumnIndex("title");
                if (titleIndex != -1) {
                    title = cursor.getString(titleIndex);
                }
                String description = "";
                int descriptionIndex = cursor.getColumnIndex("description");
                if (descriptionIndex != -1) {
                    description = cursor.getString(descriptionIndex);
                }
                String startDate = "";
                int startDateIndex = cursor.getColumnIndex("start_date");
                if (startDateIndex != -1) {
                    startDate = cursor.getString(startDateIndex);
                }
                String endDate = "";
                int endDateIndex = cursor.getColumnIndex("end_date");
                if (endDateIndex != -1) {
                    endDate = cursor.getString(endDateIndex);
                }
                int progress = 0;
                int progressIndex = cursor.getColumnIndex("progress");
                if (progressIndex != -1) {
                    progress = cursor.getInt(progressIndex);
                }
                int priority = 0;
                int priorityIndex = cursor.getColumnIndex("priority");
                if (priorityIndex != -1) {
                    priority = cursor.getInt(priorityIndex);
                }
                int statusValue = 0;
                int statusIndex = cursor.getColumnIndex("status");
                if (statusIndex != -1) {
                    statusValue = cursor.getInt(statusIndex);
                }

                String priorityStr = getPriorityString(priority);
                String statusStr = getStatusString(statusValue);

                // 应用筛选条件
                if (status.equals("全部状态") || status.equals(statusStr)) {
                    String planInfo = title + "\n" +
                            "描述：" + description + "\n" +
                            "时间：" + startDate + " 至 " + endDate + "\n" +
                            "进度：" + progress + "% | 优先级：" + priorityStr + " | 状态：" + statusStr;
                    planList.add(planInfo);
                    planIdList.add(planId);
                }
            }
            cursor.close();
        }
        planAdapter.notifyDataSetChanged();
        updatePlanStats();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}
