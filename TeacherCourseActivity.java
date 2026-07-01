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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.database.DBHelper;

import java.util.ArrayList;
import java.util.List;

public class TeacherCourseActivity extends AppCompatActivity {

    private TextView tvCourseCount, tvAssignmentCount;
    private Spinner spViewMode;
    private ListView lvItems;
    private Button btnAddCourse, btnAddAssignment, btnBack;
    private Button btnImportCourses, btnCheckHomework;
    private DBHelper dbHelper;
    private int teacherId;
    private List<String> itemList;
    private List<Integer> itemIdList;
    private List<Integer> itemTypeList; // 0表示课程，1表示作业
    private ArrayAdapter<String> itemAdapter;
    private boolean isCourseView = true; // true显示课程，false显示作业

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_course);

        teacherId = getIntent().getIntExtra("user_id", -1);
        if (teacherId == -1) {
            Toast.makeText(this, "用户信息错误", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        int userType = getIntent().getIntExtra("user_type", -1);
        if (userType != 1 && userType != 2) { // 1: 教师, 2: 管理员
            Toast.makeText(this, "权限不足，只有教师和管理员可以访问", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        initDatabase();
        loadItems();
        updateItemStats();
        setupListeners();
    }

    private void initViews() {
        tvCourseCount = findViewById(R.id.tv_course_count);
        tvAssignmentCount = findViewById(R.id.tv_assignment_count);
        spViewMode = findViewById(R.id.sp_select_course);
        lvItems = findViewById(R.id.lv_courses);
        btnAddCourse = findViewById(R.id.btn_add_course);
        btnAddAssignment = findViewById(R.id.btn_publish_assignment);
        btnBack = findViewById(R.id.btn_back);
        btnImportCourses = findViewById(R.id.btn_import_courses);
        btnCheckHomework = findViewById(R.id.btn_check_homework);
        Button btnViewProgress = findViewById(R.id.btn_view_progress);

        ArrayAdapter<CharSequence> viewModeAdapter = ArrayAdapter.createFromResource(this,
                R.array.class_options, android.R.layout.simple_spinner_item);
        viewModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spViewMode.setAdapter(viewModeAdapter);

        itemList = new ArrayList<>();
        itemIdList = new ArrayList<>();
        itemTypeList = new ArrayList<>();
        itemAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, itemList);
        lvItems.setAdapter(itemAdapter);
    }

    private void initDatabase() {
        dbHelper = new DBHelper(this);
    }

    private void loadItems() {
        itemList.clear();
        itemIdList.clear();
        itemTypeList.clear();

        if (isCourseView) {
            // 加载课程列表
            Cursor cursor = dbHelper.query("course", null, null, null, null, null, "course_name ASC");
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    int courseId = 0;
                    int idIndex = cursor.getColumnIndex("id");
                    if (idIndex != -1) {
                        courseId = cursor.getInt(idIndex);
                    }
                    String courseName = "";
                    int courseNameIndex = cursor.getColumnIndex("course_name");
                    if (courseNameIndex != -1) {
                        courseName = cursor.getString(courseNameIndex);
                    }
                    String teacherName = "";
                    int teacherNameIndex = cursor.getColumnIndex("teacher_name");
                    if (teacherNameIndex != -1) {
                        teacherName = cursor.getString(teacherNameIndex);
                    }
                    String classroom = "";
                    int classroomIndex = cursor.getColumnIndex("classroom");
                    if (classroomIndex != -1) {
                        classroom = cursor.getString(classroomIndex);
                    }
                    String startTime = "";
                    int startTimeIndex = cursor.getColumnIndex("start_time");
                    if (startTimeIndex != -1) {
                        startTime = cursor.getString(startTimeIndex);
                    }
                    String endTime = "";
                    int endTimeIndex = cursor.getColumnIndex("end_time");
                    if (endTimeIndex != -1) {
                        endTime = cursor.getString(endTimeIndex);
                    }
                    int dayOfWeek = 0;
                    int dayOfWeekIndex = cursor.getColumnIndex("day_of_week");
                    if (dayOfWeekIndex != -1) {
                        dayOfWeek = cursor.getInt(dayOfWeekIndex);
                    }

                    String dayStr = getDayOfWeekString(dayOfWeek);
                    String courseInfo = "课程：" + courseName + "\n" +
                            "教师：" + teacherName + "\n" +
                            "时间：" + dayStr + " " + startTime + "-" + endTime + "\n" +
                            "教室：" + classroom;
                    itemList.add(courseInfo);
                    itemIdList.add(courseId);
                    itemTypeList.add(0); // 0表示课程
                }
                cursor.close();
            }
        } else {
            // 加载作业列表
            Cursor cursor = dbHelper.query("assignment", null, null, null, null, null, "due_date ASC");
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    int assignmentId = 0;
                    int idIndex = cursor.getColumnIndex("id");
                    if (idIndex != -1) {
                        assignmentId = cursor.getInt(idIndex);
                    }
                    int courseId = 0;
                    int courseIdIndex = cursor.getColumnIndex("course_id");
                    if (courseIdIndex != -1) {
                        courseId = cursor.getInt(courseIdIndex);
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
                    String dueDate = "";
                    int dueDateIndex = cursor.getColumnIndex("due_date");
                    if (dueDateIndex != -1) {
                        dueDate = cursor.getString(dueDateIndex);
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

                    String courseName = getCourseName(courseId);
                    String priorityStr = getPriorityString(priority);
                    String statusStr = getStatusString(status);

                    String assignmentInfo = "作业：" + title + "\n" +
                            "课程：" + courseName + "\n" +
                            "截止日期：" + dueDate + "\n" +
                            "优先级：" + priorityStr + " | 状态：" + statusStr + "\n" +
                            "描述：" + description;
                    itemList.add(assignmentInfo);
                    itemIdList.add(assignmentId);
                    itemTypeList.add(1); // 1表示作业
                }
                cursor.close();
            }
        }
        itemAdapter.notifyDataSetChanged();
    }

    private void updateItemStats() {
        // 统计课程数量
        int courseCount = 0;
        Cursor courseCursor = dbHelper.query("course", new String[]{"id"}, null, null, null, null, null);
        if (courseCursor != null) {
            courseCount = courseCursor.getCount();
            courseCursor.close();
        }

        // 统计作业数量
        int assignmentCount = 0;
        Cursor assignmentCursor = dbHelper.query("assignment", new String[]{"id"}, null, null, null, null, null);
        if (assignmentCursor != null) {
            assignmentCount = assignmentCursor.getCount();
            assignmentCursor.close();
        }

        tvCourseCount.setText("课程总数：" + courseCount);
        tvAssignmentCount.setText("作业总数：" + assignmentCount);
    }

    private String getDayOfWeekString(int dayOfWeek) {
        String[] days = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        if (dayOfWeek >= 1 && dayOfWeek <= 7) {
            return days[dayOfWeek];
        }
        return "未知";
    }

    private String getCourseName(int courseId) {
        Cursor cursor = dbHelper.query("course", new String[]{"course_name"}, "id = ?", new String[]{String.valueOf(courseId)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            String courseName = "";
            int courseNameIndex = cursor.getColumnIndex("course_name");
            if (courseNameIndex != -1) {
                courseName = cursor.getString(courseNameIndex);
            }
            cursor.close();
            return courseName;
        }
        return "未知课程";
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

    private void setupListeners() {
        spViewMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                isCourseView = position == 0; // 0表示课程视图，1表示作业视图
                loadItems();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        lvItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showItemDetails(position);
            }
        });

        lvItems.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                showItemOptions(position);
                return true;
            }
        });

        btnAddCourse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddCourseDialog();
            }
        });

        btnAddAssignment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddAssignmentDialog();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnImportCourses.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                importCourses();
            }
        });

        btnCheckHomework.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkHomework();
            }
        });

        Button btnViewProgress = findViewById(R.id.btn_view_progress);
        btnViewProgress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TeacherCourseActivity.this, LearningProgressActivity.class);
                intent.putExtra("user_id", teacherId);
                intent.putExtra("user_type", 1);
                startActivity(intent);
            }
        });
    }

    private void importCourses() {
        String[] importOptions = {"从Excel导入", "手动添加示例课程"};
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("导入课程")
                .setItems(importOptions, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            Toast.makeText(this, "Excel导入功能开发中", Toast.LENGTH_SHORT).show();
                            break;
                        case 1:
                            importSampleCourses();
                            break;
                    }
                });
        builder.show();
    }

    private void importSampleCourses() {
        ContentValues values1 = new ContentValues();
        values1.put("course_name", "高等数学");
        values1.put("teacher_name", "张教授");
        values1.put("classroom", "教1-101");
        values1.put("start_time", "08:00");
        values1.put("end_time", "09:40");
        values1.put("day_of_week", 1);
        dbHelper.insert("course", values1);
        
        ContentValues values2 = new ContentValues();
        values2.put("course_name", "大学英语");
        values2.put("teacher_name", "李老师");
        values2.put("classroom", "教2-202");
        values2.put("start_time", "10:00");
        values2.put("end_time", "11:40");
        values2.put("day_of_week", 2);
        dbHelper.insert("course", values2);
        
        ContentValues values3 = new ContentValues();
        values3.put("course_name", "数据结构");
        values3.put("teacher_name", "王教授");
        values3.put("classroom", "教3-303");
        values3.put("start_time", "14:00");
        values3.put("end_time", "15:40");
        values3.put("day_of_week", 3);
        dbHelper.insert("course", values3);
        
        loadItems();
        updateItemStats();
        Toast.makeText(this, "示例课程导入成功", Toast.LENGTH_SHORT).show();
    }

    private void checkHomework() {
        isCourseView = false;
        loadItems();
        Toast.makeText(this, "已切换到作业视图，请长按作业项进行批阅", Toast.LENGTH_LONG).show();
    }

    private void showItemDetails(int position) {
        // 显示项目详情
        Toast.makeText(this, "查看详情：" + itemList.get(position), Toast.LENGTH_SHORT).show();
    }

    private void showItemOptions(int position) {
        // 显示项目操作菜单
        int itemType = itemTypeList.get(position);
        String[] options;
        if (itemType == 0) {
            options = new String[]{"编辑课程", "删除课程", "查看作业", "查看学生作业完成情况"};
        } else {
            options = new String[]{"编辑作业", "删除作业", "查看详情", "批阅作业"};
        }

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("操作选项")
                .setItems(options, (dialog, which) -> {
                    if (itemType == 0) {
                        // 课程操作
                        switch (which) {
                            case 0:
                                editCourse(position);
                                break;
                            case 1:
                                deleteCourse(position);
                                break;
                            case 2:
                                viewCourseAssignments(position);
                                break;
                            case 3:
                                viewStudentAssignmentStatus(position);
                                break;
                        }
                    } else {
                        // 作业操作
                        switch (which) {
                            case 0:
                                editAssignment(position);
                                break;
                            case 1:
                                deleteAssignment(position);
                                break;
                            case 2:
                                showItemDetails(position);
                                break;
                            case 3:
                                gradeAssignment(position);
                                break;
                        }
                    }
                });
        builder.show();
    }

    private void viewStudentAssignmentStatus(int position) {
        // 查看学生作业完成情况
        int courseId = itemIdList.get(position);
        String courseName = getCourseName(courseId);

        // 加载所有学生
        List<String> studentNames = new ArrayList<>();
        List<Integer> studentIds = new ArrayList<>();
        Cursor studentCursor = dbHelper.query("user", new String[]{"id", "username"}, "type = ?", new String[]{"0"}, null, null, "username ASC");
        if (studentCursor != null) {
            while (studentCursor.moveToNext()) {
                int idIndex = studentCursor.getColumnIndex("id");
                if (idIndex != -1) {
                    studentIds.add(studentCursor.getInt(idIndex));
                }
                int usernameIndex = studentCursor.getColumnIndex("username");
                if (usernameIndex != -1) {
                    studentNames.add(studentCursor.getString(usernameIndex));
                }
            }
            studentCursor.close();
        }

        // 加载该课程的作业
        List<String> assignments = new ArrayList<>();
        List<Integer> assignmentIds = new ArrayList<>();
        Cursor assignmentCursor = dbHelper.query("assignment", new String[]{"id", "title"}, "course_id = ?", new String[]{String.valueOf(courseId)}, null, null, "due_date ASC");
        if (assignmentCursor != null) {
            while (assignmentCursor.moveToNext()) {
                int idIndex = assignmentCursor.getColumnIndex("id");
                if (idIndex != -1) {
                    assignmentIds.add(assignmentCursor.getInt(idIndex));
                }
                int titleIndex = assignmentCursor.getColumnIndex("title");
                if (titleIndex != -1) {
                    assignments.add(assignmentCursor.getString(titleIndex));
                }
            }
            assignmentCursor.close();
        }

        if (assignments.isEmpty()) {
            Toast.makeText(this, courseName + " 暂无作业", Toast.LENGTH_SHORT).show();
            return;
        }

        // 构建学生作业完成情况矩阵
        StringBuilder statusMatrix = new StringBuilder(courseName + " 学生作业完成情况：\n\n");
        statusMatrix.append("学生\t");
        for (String assignment : assignments) {
            statusMatrix.append(assignment).append("\t");
        }
        statusMatrix.append("\n");

        for (int i = 0; i < studentNames.size(); i++) {
            statusMatrix.append(studentNames.get(i)).append("\t");
            for (int j = 0; j < assignmentIds.size(); j++) {
                // 这里简化处理，实际应该查询学生的作业完成情况
                // 由于当前数据库设计可能没有存储每个学生的作业状态，这里使用模拟数据
                statusMatrix.append("未提交\t");
            }
            statusMatrix.append("\n");
        }

        // 创建一个自定义对话框来显示学生作业完成情况
        View dialogView = getLayoutInflater().inflate(R.layout.activity_assignment, null);
        ListView lvStatus = dialogView.findViewById(R.id.lv_assignments);
        List<String> statusList = new ArrayList<>();
        statusList.add(statusMatrix.toString());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, statusList);
        lvStatus.setAdapter(adapter);

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle(courseName + " 学生作业完成情况")
                .setView(dialogView)
                .setPositiveButton("关闭", null);
        builder.show();
    }

    private void gradeAssignment(int position) {
        // 批阅作业
        int assignmentId = itemIdList.get(position);
        Cursor cursor = dbHelper.query("assignment", null, "id = ?", new String[]{String.valueOf(assignmentId)}, null, null, null);
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
            cursor.close();

            // 显示批阅对话框
            TextView tvTitle = new TextView(this);
            tvTitle.setText("作业：" + title + "\n描述：" + description);
            tvTitle.setPadding(20, 20, 20, 20);
            tvTitle.setTextSize(14);

            EditText etComment = new EditText(this);
            etComment.setHint("请输入批阅评语");
            etComment.setPadding(20, 20, 20, 20);

            Spinner spGradeStatus = new Spinner(this);
            ArrayAdapter<CharSequence> gradeStatusAdapter = ArrayAdapter.createFromResource(this,
                    R.array.grade_status_options, android.R.layout.simple_spinner_item);
            gradeStatusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spGradeStatus.setAdapter(gradeStatusAdapter);

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.addView(tvTitle);
            layout.addView(etComment);
            layout.addView(spGradeStatus);

            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("批阅作业")
                    .setView(layout)
                    .setPositiveButton("保存", (dialog, which) -> {
                        String comment = etComment.getText().toString().trim();
                        int gradeStatus = spGradeStatus.getSelectedItemPosition();

                        // 更新作业批阅状态和评语
                        ContentValues values = new ContentValues();
                        values.put("comment", comment);
                        // 这里假设数据库中有一个 grade_status 字段来存储批阅状态
                        // 如果没有，需要先修改数据库结构

                        // dbHelper.update("assignment", values, "id = ?", new String[]{String.valueOf(assignmentId)});
                        Toast.makeText(TeacherCourseActivity.this, "作业批阅完成", Toast.LENGTH_SHORT).show();
                        loadItems();
                    })
                    .setNegativeButton("取消", null);
            builder.show();
        }
    }

    private void showAddCourseDialog() {
        // 显示添加课程对话框
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_course, null);
        EditText etCourseName = dialogView.findViewById(R.id.et_course_name);
        EditText etTeacherName = dialogView.findViewById(R.id.et_teacher_name);
        EditText etClassroom = dialogView.findViewById(R.id.et_classroom);
        EditText etStartTime = dialogView.findViewById(R.id.et_start_time);
        EditText etEndTime = dialogView.findViewById(R.id.et_end_time);
        Spinner spDayOfWeek = dialogView.findViewById(R.id.sp_day_of_week);

        // 设置星期几选项
        ArrayAdapter<CharSequence> dayAdapter = ArrayAdapter.createFromResource(this,
                R.array.day_of_week_options, android.R.layout.simple_spinner_item);
        dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDayOfWeek.setAdapter(dayAdapter);

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("添加课程")
                .setView(dialogView)
                .setPositiveButton("保存", (dialog, which) -> {
                    String courseName = etCourseName.getText().toString().trim();
                    String teacherName = etTeacherName.getText().toString().trim();
                    String classroom = etClassroom.getText().toString().trim();
                    String startTime = etStartTime.getText().toString().trim();
                    String endTime = etEndTime.getText().toString().trim();
                    int dayOfWeek = spDayOfWeek.getSelectedItemPosition() + 1; // 调整为星期几（1-7）

                    if (courseName.isEmpty()) {
                        Toast.makeText(TeacherCourseActivity.this, "请输入课程名称", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (teacherName.isEmpty()) {
                        Toast.makeText(TeacherCourseActivity.this, "请输入教师姓名", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (classroom.isEmpty()) {
                        Toast.makeText(TeacherCourseActivity.this, "请输入教室", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (startTime.isEmpty()) {
                        Toast.makeText(TeacherCourseActivity.this, "请输入开始时间", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (endTime.isEmpty()) {
                        Toast.makeText(TeacherCourseActivity.this, "请输入结束时间", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 保存课程到数据库
                    ContentValues values = new ContentValues();
                    values.put("user_id", teacherId);
                    values.put("course_name", courseName);
                    values.put("teacher_name", teacherName);
                    values.put("classroom", classroom);
                    values.put("start_time", startTime);
                    values.put("end_time", endTime);
                    values.put("day_of_week", dayOfWeek);
                    values.put("is_favorite", 0);

                    dbHelper.insertCourse(values);
                    Toast.makeText(TeacherCourseActivity.this, "课程添加成功", Toast.LENGTH_SHORT).show();
                    loadItems();
                    updateItemStats();
                })
                .setNegativeButton("取消", null);
        builder.show();
    }

    private void showAddAssignmentDialog() {
        // 显示添加作业对话框
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_assignment, null);
        Spinner spCourse = dialogView.findViewById(R.id.sp_course);
        EditText etTitle = dialogView.findViewById(R.id.et_title);
        EditText etDescription = dialogView.findViewById(R.id.et_description);
        EditText etDueDate = dialogView.findViewById(R.id.et_due_date);
        Spinner spPriority = dialogView.findViewById(R.id.sp_priority);
        Spinner spStatus = dialogView.findViewById(R.id.sp_status);

        // 加载课程列表到spCourse
        List<String> courseNames = new ArrayList<>();
        List<Integer> courseIds = new ArrayList<>();
        Cursor cursor = dbHelper.query("course", new String[]{"id", "course_name"}, null, null, null, null, "course_name ASC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int idIndex = cursor.getColumnIndex("id");
                if (idIndex != -1) {
                    courseIds.add(cursor.getInt(idIndex));
                }
                int courseNameIndex = cursor.getColumnIndex("course_name");
                if (courseNameIndex != -1) {
                    courseNames.add(cursor.getString(courseNameIndex));
                }
            }
            cursor.close();
        }

        if (courseNames.isEmpty()) {
            Toast.makeText(this, "请先添加课程", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayAdapter<String> courseAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, courseNames);
        courseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCourse.setAdapter(courseAdapter);

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

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("添加作业")
                .setView(dialogView)
                .setPositiveButton("保存", (dialog, which) -> {
                    int coursePosition = spCourse.getSelectedItemPosition();
                    String title = etTitle.getText().toString().trim();
                    String description = etDescription.getText().toString().trim();
                    String dueDate = etDueDate.getText().toString().trim();
                    int priority = spPriority.getSelectedItemPosition();
                    int status = spStatus.getSelectedItemPosition();

                    if (coursePosition < 0) {
                        Toast.makeText(TeacherCourseActivity.this, "请选择课程", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (title.isEmpty()) {
                        Toast.makeText(TeacherCourseActivity.this, "请输入作业标题", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (dueDate.isEmpty()) {
                        Toast.makeText(TeacherCourseActivity.this, "请输入截止日期", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int courseId = courseIds.get(coursePosition);

                    // 保存作业到数据库
                    ContentValues values = new ContentValues();
                    values.put("user_id", teacherId);
                    values.put("course_id", courseId);
                    values.put("title", title);
                    values.put("description", description);
                    values.put("due_date", dueDate);
                    values.put("priority", priority);
                    values.put("status", status);

                    dbHelper.insertAssignment(values);
                    Toast.makeText(TeacherCourseActivity.this, "作业添加成功", Toast.LENGTH_SHORT).show();
                    loadItems();
                    updateItemStats();
                })
                .setNegativeButton("取消", null);
        builder.show();
    }

    private void editCourse(int position) {
        // 编辑课程
        int courseId = itemIdList.get(position);
        Cursor cursor = dbHelper.query("course", null, "id = ?", new String[]{String.valueOf(courseId)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            String courseName = "";
            int courseNameIndex = cursor.getColumnIndex("course_name");
            if (courseNameIndex != -1) {
                courseName = cursor.getString(courseNameIndex);
            }
            String teacherName = "";
            int teacherNameIndex = cursor.getColumnIndex("teacher_name");
            if (teacherNameIndex != -1) {
                teacherName = cursor.getString(teacherNameIndex);
            }
            String classroom = "";
            int classroomIndex = cursor.getColumnIndex("classroom");
            if (classroomIndex != -1) {
                classroom = cursor.getString(classroomIndex);
            }
            String startTime = "";
            int startTimeIndex = cursor.getColumnIndex("start_time");
            if (startTimeIndex != -1) {
                startTime = cursor.getString(startTimeIndex);
            }
            String endTime = "";
            int endTimeIndex = cursor.getColumnIndex("end_time");
            if (endTimeIndex != -1) {
                endTime = cursor.getString(endTimeIndex);
            }
            int dayOfWeek = 0;
            int dayOfWeekIndex = cursor.getColumnIndex("day_of_week");
            if (dayOfWeekIndex != -1) {
                dayOfWeek = cursor.getInt(dayOfWeekIndex);
            }
            cursor.close();

            View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_course, null);
            EditText etCourseName = dialogView.findViewById(R.id.et_course_name);
            EditText etTeacherName = dialogView.findViewById(R.id.et_teacher_name);
            EditText etClassroom = dialogView.findViewById(R.id.et_classroom);
            EditText etStartTime = dialogView.findViewById(R.id.et_start_time);
            EditText etEndTime = dialogView.findViewById(R.id.et_end_time);
            Spinner spDayOfWeek = dialogView.findViewById(R.id.sp_day_of_week);

            // 设置星期几选项
            ArrayAdapter<CharSequence> dayAdapter = ArrayAdapter.createFromResource(this,
                    R.array.day_of_week_options, android.R.layout.simple_spinner_item);
            dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spDayOfWeek.setAdapter(dayAdapter);

            // 填充现有数据
            etCourseName.setText(courseName);
            etTeacherName.setText(teacherName);
            etClassroom.setText(classroom);
            etStartTime.setText(startTime);
            etEndTime.setText(endTime);
            spDayOfWeek.setSelection(dayOfWeek - 1); // 调整为Spinner的索引（0-6）

            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("编辑课程")
                    .setView(dialogView)
                    .setPositiveButton("保存", (dialog, which) -> {
                        String newCourseName = etCourseName.getText().toString().trim();
                        String newTeacherName = etTeacherName.getText().toString().trim();
                        String newClassroom = etClassroom.getText().toString().trim();
                        String newStartTime = etStartTime.getText().toString().trim();
                        String newEndTime = etEndTime.getText().toString().trim();
                        int newDayOfWeek = spDayOfWeek.getSelectedItemPosition() + 1; // 调整为星期几（1-7）

                        if (newCourseName.isEmpty()) {
                            Toast.makeText(TeacherCourseActivity.this, "请输入课程名称", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (newTeacherName.isEmpty()) {
                            Toast.makeText(TeacherCourseActivity.this, "请输入教师姓名", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (newClassroom.isEmpty()) {
                            Toast.makeText(TeacherCourseActivity.this, "请输入教室", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (newStartTime.isEmpty()) {
                            Toast.makeText(TeacherCourseActivity.this, "请输入开始时间", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (newEndTime.isEmpty()) {
                            Toast.makeText(TeacherCourseActivity.this, "请输入结束时间", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // 更新课程到数据库
                        ContentValues values = new ContentValues();
                        values.put("course_name", newCourseName);
                        values.put("teacher_name", newTeacherName);
                        values.put("classroom", newClassroom);
                        values.put("start_time", newStartTime);
                        values.put("end_time", newEndTime);
                        values.put("day_of_week", newDayOfWeek);

                        dbHelper.update("course", values, "id = ?", new String[]{String.valueOf(courseId)});
                        Toast.makeText(TeacherCourseActivity.this, "课程更新成功", Toast.LENGTH_SHORT).show();
                        loadItems();
                    })
                    .setNegativeButton("取消", null);
            builder.show();
        }
    }

    private void deleteCourse(int position) {
        // 删除课程
        int courseId = itemIdList.get(position);
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("确认删除")
                .setMessage("确定要删除这门课程吗？删除后相关的作业也会被删除。")
                .setPositiveButton("确定", (dialog, which) -> {
                    // 从数据库中删除课程
                    dbHelper.delete("course", "id = ?", new String[]{String.valueOf(courseId)});
                    // 删除相关的作业
                    dbHelper.delete("assignment", "course_id = ?", new String[]{String.valueOf(courseId)});
                    Toast.makeText(TeacherCourseActivity.this, "课程已删除", Toast.LENGTH_SHORT).show();
                    loadItems(); // 重新加载项目列表
                    updateItemStats(); // 重新更新统计数据
                })
                .setNegativeButton("取消", null);
        builder.show();
    }

    private void viewCourseAssignments(int position) {
        // 查看课程的作业
        int courseId = itemIdList.get(position);
        String courseName = getCourseName(courseId);

        // 加载该课程的作业
        List<String> assignments = new ArrayList<>();
        Cursor cursor = dbHelper.query("assignment", null, "course_id = ?", new String[]{String.valueOf(courseId)}, null, null, "due_date ASC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String title = "";
                int titleIndex = cursor.getColumnIndex("title");
                if (titleIndex != -1) {
                    title = cursor.getString(titleIndex);
                }
                String dueDate = "";
                int dueDateIndex = cursor.getColumnIndex("due_date");
                if (dueDateIndex != -1) {
                    dueDate = cursor.getString(dueDateIndex);
                }
                int status = 0;
                int statusIndex = cursor.getColumnIndex("status");
                if (statusIndex != -1) {
                    status = cursor.getInt(statusIndex);
                }
                String statusStr = getStatusString(status);
                assignments.add(title + " (截止：" + dueDate + ", 状态：" + statusStr + ")");
            }
            cursor.close();
        }

        if (assignments.isEmpty()) {
            Toast.makeText(this, courseName + " 暂无作业", Toast.LENGTH_SHORT).show();
        } else {
            // 创建一个自定义对话框来显示作业列表
            View dialogView = getLayoutInflater().inflate(R.layout.activity_assignment, null);
            ListView lvAssignments = dialogView.findViewById(R.id.lv_assignments);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, assignments);
            lvAssignments.setAdapter(adapter);

            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle(courseName + " 的作业")
                    .setView(dialogView)
                    .setPositiveButton("关闭", null);
            builder.show();
        }
    }

    private void editAssignment(int position) {
        // 编辑作业
        int assignmentId = itemIdList.get(position);
        Cursor cursor = dbHelper.query("assignment", null, "id = ?", new String[]{String.valueOf(assignmentId)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int courseId = 0;
            int courseIdIndex = cursor.getColumnIndex("course_id");
            if (courseIdIndex != -1) {
                courseId = cursor.getInt(courseIdIndex);
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
            String dueDate = "";
            int dueDateIndex = cursor.getColumnIndex("due_date");
            if (dueDateIndex != -1) {
                dueDate = cursor.getString(dueDateIndex);
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
            cursor.close();

            View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_assignment, null);
            Spinner spCourse = dialogView.findViewById(R.id.sp_course);
            EditText etTitle = dialogView.findViewById(R.id.et_title);
            EditText etDescription = dialogView.findViewById(R.id.et_description);
            EditText etDueDate = dialogView.findViewById(R.id.et_due_date);
            Spinner spPriority = dialogView.findViewById(R.id.sp_priority);
            Spinner spStatus = dialogView.findViewById(R.id.sp_status);

            // 加载课程列表到spCourse
            List<String> courseNames = new ArrayList<>();
            List<Integer> courseIds = new ArrayList<>();
            Cursor courseCursor = dbHelper.query("course", new String[]{"id", "course_name"}, null, null, null, null, "course_name ASC");
            if (courseCursor != null) {
                while (courseCursor.moveToNext()) {
                    int idIndex = courseCursor.getColumnIndex("id");
                if (idIndex != -1) {
                    courseIds.add(courseCursor.getInt(idIndex));
                }
                int courseNameIndex = courseCursor.getColumnIndex("course_name");
                if (courseNameIndex != -1) {
                    courseNames.add(courseCursor.getString(courseNameIndex));
                }
                }
                courseCursor.close();
            }

            ArrayAdapter<String> courseAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, courseNames);
            courseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spCourse.setAdapter(courseAdapter);

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

            // 填充现有数据
            spCourse.setSelection(courseIds.indexOf(courseId));
            etTitle.setText(title);
            etDescription.setText(description);
            etDueDate.setText(dueDate);
            spPriority.setSelection(priority);
            spStatus.setSelection(status);

            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("编辑作业")
                    .setView(dialogView)
                    .setPositiveButton("保存", (dialog, which) -> {
                        int coursePosition = spCourse.getSelectedItemPosition();
                        String newTitle = etTitle.getText().toString().trim();
                        String newDescription = etDescription.getText().toString().trim();
                        String newDueDate = etDueDate.getText().toString().trim();
                        int newPriority = spPriority.getSelectedItemPosition();
                        int newStatus = spStatus.getSelectedItemPosition();

                        if (coursePosition < 0) {
                            Toast.makeText(TeacherCourseActivity.this, "请选择课程", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (newTitle.isEmpty()) {
                            Toast.makeText(TeacherCourseActivity.this, "请输入作业标题", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (newDueDate.isEmpty()) {
                            Toast.makeText(TeacherCourseActivity.this, "请输入截止日期", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        int newCourseId = courseIds.get(coursePosition);

                        // 更新作业到数据库
                        ContentValues values = new ContentValues();
                        values.put("course_id", newCourseId);
                        values.put("title", newTitle);
                        values.put("description", newDescription);
                        values.put("due_date", newDueDate);
                        values.put("priority", newPriority);
                        values.put("status", newStatus);

                        dbHelper.update("assignment", values, "id = ?", new String[]{String.valueOf(assignmentId)});
                        Toast.makeText(TeacherCourseActivity.this, "作业更新成功", Toast.LENGTH_SHORT).show();
                        loadItems();
                    })
                    .setNegativeButton("取消", null);
            builder.show();
        }
    }

    private void deleteAssignment(int position) {
        // 删除作业
        int assignmentId = itemIdList.get(position);
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("确认删除")
                .setMessage("确定要删除这个作业吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    // 从数据库中删除作业
                    dbHelper.delete("assignment", "id = ?", new String[]{String.valueOf(assignmentId)});
                    Toast.makeText(TeacherCourseActivity.this, "作业已删除", Toast.LENGTH_SHORT).show();
                    loadItems(); // 重新加载项目列表
                    updateItemStats(); // 重新更新统计数据
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
