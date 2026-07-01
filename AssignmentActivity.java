package com.example.myapplication;

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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.database.DBHelper;
import com.example.myapplication.utils.DialogUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AssignmentActivity extends AppCompatActivity {

    private TextView tvAssignmentCount;
    private Spinner spFilterStatus, spFilterCourse;
    private ListView lvAssignments;
    private Button btnAddAssignment, btnStatistics, btnBack;
    private DBHelper dbHelper;
    private int userId;
    private List<String> assignmentList;
    private List<Integer> assignmentIdList;
    private ArrayAdapter<String> assignmentAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assignment);

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
        loadCourses();
        loadAssignments();
        setupListeners();
    }

    private void initViews() {
        tvAssignmentCount = findViewById(R.id.tv_assignment_count);
        spFilterStatus = findViewById(R.id.sp_filter_status);
        spFilterCourse = findViewById(R.id.sp_filter_course);
        lvAssignments = findViewById(R.id.lv_assignments);
        btnAddAssignment = findViewById(R.id.btn_add_assignment);
        btnStatistics = findViewById(R.id.btn_statistics);
        btnBack = findViewById(R.id.btn_back);

        // 设置状态筛选选项
        ArrayAdapter<CharSequence> statusAdapter = ArrayAdapter.createFromResource(this,
                R.array.assignment_status_options, android.R.layout.simple_spinner_item);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFilterStatus.setAdapter(statusAdapter);

        // 初始化作业列表
        assignmentList = new ArrayList<>();
        assignmentIdList = new ArrayList<>();
        assignmentAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, assignmentList);
        lvAssignments.setAdapter(assignmentAdapter);
    }

    private void initDatabase() {
        dbHelper = new DBHelper(this);
    }

    private void loadCourses() {
        // 加载课程列表到spFilterCourse
        List<String> courseOptions = new ArrayList<>();
        courseOptions.add("全部课程");
        Cursor cursor = dbHelper.getCoursesByUserId(userId);
        if (cursor != null) {
            int courseNameIndex = cursor.getColumnIndex("course_name");
            while (cursor.moveToNext() && courseNameIndex != -1) {
                String courseName = cursor.getString(courseNameIndex);
                courseOptions.add(courseName);
            }
            cursor.close();
        }
        ArrayAdapter<String> courseAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, courseOptions);
        courseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFilterCourse.setAdapter(courseAdapter);
    }

    private void loadAssignments() {
        assignmentList.clear();
        assignmentIdList.clear();
        Cursor cursor = dbHelper.getAssignmentsByUserId(userId);
        if (cursor != null) {
            int count = 0;
            int idIndex = cursor.getColumnIndex("id");
            int titleIndex = cursor.getColumnIndex("title");
            int descriptionIndex = cursor.getColumnIndex("description");
            int dueDateIndex = cursor.getColumnIndex("due_date");
            int priorityIndex = cursor.getColumnIndex("priority");
            int statusIndex = cursor.getColumnIndex("status");
            int courseIdIndex = cursor.getColumnIndex("course_id");

            while (cursor.moveToNext() && idIndex != -1 && titleIndex != -1 && dueDateIndex != -1 && priorityIndex != -1 && statusIndex != -1 && courseIdIndex != -1) {
                int assignmentId = cursor.getInt(idIndex);
                String title = cursor.getString(titleIndex);
                String description = descriptionIndex != -1 ? cursor.getString(descriptionIndex) : "";
                String dueDate = cursor.getString(dueDateIndex);
                int priority = cursor.getInt(priorityIndex);
                int status = cursor.getInt(statusIndex);
                int courseId = cursor.getInt(courseIdIndex);

                String priorityStr = getPriorityString(priority);
                String statusStr = getStatusString(status);
                String courseName = getCourseName(courseId);
                
                String assignmentInfo = title + "\n" +
                        "课程：" + courseName + "\n" +
                        "截止日期：" + dueDate + "\n" +
                        "优先级：" + priorityStr + " | 状态：" + statusStr;
                assignmentList.add(assignmentInfo);
                assignmentIdList.add(assignmentId);
                count++;
            }
            tvAssignmentCount.setText("作业总数：" + count);
            cursor.close();
        }
        assignmentAdapter.notifyDataSetChanged();
    }

    private String getPriorityString(int priority) {
        String[] priorities = {"低", "中", "高"};
        if (priority >= 0 && priority < priorities.length) {
            return priorities[priority];
        }
        return "未知";
    }

    private String getStatusString(int status) {
        String[] statuses = {"未开始", "进行中", "已完成", "已逾期"};
        if (status >= 0 && status < statuses.length) {
            return statuses[status];
        }
        return "未知";
    }

    private String getCourseName(int courseId) {
        // 从数据库查询课程名称
        Cursor cursor = dbHelper.query("course", new String[]{"course_name"}, "id = ?", new String[]{String.valueOf(courseId)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int courseNameIndex = cursor.getColumnIndex("course_name");
            if (courseNameIndex != -1) {
                String courseName = cursor.getString(courseNameIndex);
                cursor.close();
                return courseName;
            }
            cursor.close();
        }
        return "课程" + courseId;
    }

    private void setupListeners() {
        spFilterStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String status = parent.getItemAtPosition(position).toString();
                filterAssignments(status, spFilterCourse.getSelectedItem().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        spFilterCourse.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String course = parent.getItemAtPosition(position).toString();
                filterAssignments(spFilterStatus.getSelectedItem().toString(), course);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        lvAssignments.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 点击作业项，可以进行编辑或查看详情
                showAssignmentDetails(position);
            }
        });

        lvAssignments.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                // 长按作业项，显示操作菜单
                showAssignmentOptions(position);
                return true;
            }
        });

        btnAddAssignment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AssignmentActivity.this, AddAssignmentActivity.class);
                intent.putExtra("user_id", userId);
                startActivityForResult(intent, 1);
            }
        });

        btnStatistics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAssignmentStatistics();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void showAssignmentStatistics() {
        // 简化版的作业统计，实际应用中可以使用AlertDialog或单独的Activity
        int total = assignmentList.size();
        int completed = 0;
        int pending = 0;
        int overdue = 0;

        // 从数据库查询更详细的统计信息
        Cursor cursor = dbHelper.getAssignmentsByUserId(userId);
        if (cursor != null) {
            int statusIndex = cursor.getColumnIndex("status");
            while (cursor.moveToNext() && statusIndex != -1) {
                int status = cursor.getInt(statusIndex);
                switch (status) {
                    case 2:
                        completed++;
                        break;
                    case 3:
                        overdue++;
                        break;
                    default:
                        pending++;
                        break;
                }
            }
            cursor.close();
        }

        String stats = "作业统计报告\n\n" +
                "总作业数：" + total + "\n" +
                "已完成：" + completed + "\n" +
                "进行中：" + pending + "\n" +
                "已逾期：" + overdue + "\n\n" +
                "完成率：" + (total > 0 ? String.format(Locale.ROOT, "%.1f%%", (double) completed / total * 100) : "0%");

        DialogUtils.showInfoDialog(this, "作业统计", stats);
    }

    private void filterAssignments(String status, String course) {
        // 筛选作业
        assignmentList.clear();
        assignmentIdList.clear();
        Cursor cursor = dbHelper.getAssignmentsByUserId(userId);
        if (cursor != null) {
            int idIndex = cursor.getColumnIndex("id");
            int titleIndex = cursor.getColumnIndex("title");
            int descriptionIndex = cursor.getColumnIndex("description");
            int dueDateIndex = cursor.getColumnIndex("due_date");
            int priorityIndex = cursor.getColumnIndex("priority");
            int statusIndex = cursor.getColumnIndex("status");
            int courseIdIndex = cursor.getColumnIndex("course_id");

            while (cursor.moveToNext() && idIndex != -1 && titleIndex != -1 && dueDateIndex != -1 && priorityIndex != -1 && statusIndex != -1 && courseIdIndex != -1) {
                int assignmentId = cursor.getInt(idIndex);
                String title = cursor.getString(titleIndex);
                String description = descriptionIndex != -1 ? cursor.getString(descriptionIndex) : "";
                String dueDate = cursor.getString(dueDateIndex);
                int priority = cursor.getInt(priorityIndex);
                int statusValue = cursor.getInt(statusIndex);
                int courseId = cursor.getInt(courseIdIndex);

                String priorityStr = getPriorityString(priority);
                String statusStr = getStatusString(statusValue);
                String courseName = getCourseName(courseId);

                // 应用筛选条件
                boolean statusMatch = status.equals("全部状态") || status.equals(statusStr);
                boolean courseMatch = course.equals("全部课程") || course.equals(courseName);

                if (statusMatch && courseMatch) {
                    String assignmentInfo = title + "\n" +
                            "课程：" + courseName + "\n" +
                            "截止日期：" + dueDate + "\n" +
                            "优先级：" + priorityStr + " | 状态：" + statusStr;
                    assignmentList.add(assignmentInfo);
                    assignmentIdList.add(assignmentId);
                }
            }
            cursor.close();
        }
        assignmentAdapter.notifyDataSetChanged();
        tvAssignmentCount.setText("作业总数：" + assignmentList.size());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            loadAssignments();
        }
    }

    private void showAssignmentDetails(int position) {
        // 显示作业详情
        Toast.makeText(this, "查看作业详情：" + assignmentList.get(position), Toast.LENGTH_SHORT).show();
    }

    private void showAssignmentOptions(int position) {
        // 显示作业操作菜单
        String[] options = {"编辑作业", "删除作业", "更新状态", "设置优先级", "添加附件", "提交作业", "分享作业"};
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("作业操作")
                .setItems(options, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                editAssignment(position);
                                break;
                            case 1:
                                deleteAssignment(position);
                                break;
                            case 2:
                                updateAssignmentStatus(position);
                                break;
                            case 3:
                                setAssignmentPriority(position);
                                break;
                            case 4:
                                addAttachment(position);
                                break;
                            case 5:
                                submitAssignment(position);
                                break;
                            case 6:
                                shareAssignment(position);
                                break;
                        }
                    }
                });
        builder.show();
    }

    private void editAssignment(int position) {
        // 编辑作业
        int assignmentId = assignmentIdList.get(position);
        Intent intent = new Intent(AssignmentActivity.this, AddAssignmentActivity.class);
        intent.putExtra("user_id", userId);
        intent.putExtra("assignment_id", assignmentId);
        startActivityForResult(intent, 1);
    }

    private void deleteAssignment(int position) {
        // 删除作业
        int assignmentId = assignmentIdList.get(position);
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("确认删除")
                .setMessage("确定要删除这个作业吗？")
                .setPositiveButton("确定", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        // 从数据库中删除作业
                        dbHelper.delete("assignment", "id = ?", new String[]{String.valueOf(assignmentId)});
                        Toast.makeText(AssignmentActivity.this, "作业已删除", Toast.LENGTH_SHORT).show();
                        loadAssignments(); // 重新加载作业列表
                    }
                })
                .setNegativeButton("取消", null);
        builder.show();
    }

    private void updateAssignmentStatus(int position) {
        // 更新作业状态
        int assignmentId = assignmentIdList.get(position);
        String[] statuses = {"未开始", "进行中", "已完成", "已逾期"};
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("更新作业状态")
                .setItems(statuses, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        // 更新作业状态到数据库
                        ContentValues values = new ContentValues();
                        values.put("status", which);
                        dbHelper.update("assignment", values, "id = ?", new String[]{String.valueOf(assignmentId)});
                        Toast.makeText(AssignmentActivity.this, "作业状态已更新为：" + statuses[which], Toast.LENGTH_SHORT).show();
                        loadAssignments(); // 重新加载作业列表
                    }
                });
        builder.show();
    }

    private void setAssignmentPriority(int position) {
        // 设置作业优先级
        int assignmentId = assignmentIdList.get(position);
        String[] priorities = {"低", "中", "高"};
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("设置作业优先级")
                .setItems(priorities, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        // 更新作业优先级到数据库
                        ContentValues values = new ContentValues();
                        values.put("priority", which);
                        dbHelper.update("assignment", values, "id = ?", new String[]{String.valueOf(assignmentId)});
                        Toast.makeText(AssignmentActivity.this, "作业优先级已设置为：" + priorities[which], Toast.LENGTH_SHORT).show();
                        loadAssignments(); // 重新加载作业列表
                    }
                });
        builder.show();
    }

    private void addAttachment(int position) {
        // 添加附件
        int assignmentId = assignmentIdList.get(position);
        EditText etAttachmentPath = new EditText(this);
        etAttachmentPath.setHint("请输入附件路径或URL");
        etAttachmentPath.setPadding(20, 20, 20, 20);

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("添加附件")
                .setView(etAttachmentPath)
                .setPositiveButton("保存", (dialog, which) -> {
                    String attachmentPath = etAttachmentPath.getText().toString().trim();

                    if (attachmentPath.isEmpty()) {
                        Toast.makeText(AssignmentActivity.this, "请输入附件路径或URL", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 保存附件路径到数据库
                    ContentValues values = new ContentValues();
                    values.put("attachment", attachmentPath);
                    dbHelper.update("assignment", values, "id = ?", new String[]{String.valueOf(assignmentId)});

                    // 这里可以添加文件选择器的代码
                    // 由于需要权限，暂时只保存附件路径

                    Toast.makeText(AssignmentActivity.this, "附件添加成功", Toast.LENGTH_SHORT).show();
                    loadAssignments(); // 重新加载作业列表
                })
                .setNegativeButton("取消", null);
        builder.show();
    }

    private void submitAssignment(int position) {
        // 提交作业
        int assignmentId = assignmentIdList.get(position);
        EditText etSubmission = new EditText(this);
        etSubmission.setHint("请输入提交内容或文件路径");
        etSubmission.setPadding(20, 20, 20, 20);

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("提交作业")
                .setView(etSubmission)
                .setPositiveButton("保存", (dialog, which) -> {
                    String submission = etSubmission.getText().toString().trim();

                    if (submission.isEmpty()) {
                        Toast.makeText(AssignmentActivity.this, "请输入提交内容或文件路径", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 保存提交状态到数据库
                    ContentValues values = new ContentValues();
                    values.put("status", 2); // 标记为已完成
                    values.put("attachment", submission); // 保存提交内容或文件路径
                    dbHelper.update("assignment", values, "id = ?", new String[]{String.valueOf(assignmentId)});

                    // 这里可以添加文件选择器的代码
                    // 由于需要权限，暂时只保存提交内容

                    Toast.makeText(AssignmentActivity.this, "作业提交成功", Toast.LENGTH_SHORT).show();
                    loadAssignments(); // 重新加载作业列表
                })
                .setNegativeButton("取消", null);
        builder.show();
    }

    private void shareAssignment(int position) {
        // 分享作业
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, assignmentList.get(position));
        startActivity(Intent.createChooser(shareIntent, "分享作业"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}
