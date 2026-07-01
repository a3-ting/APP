package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

// import com.alibaba.easyexcel.EasyExcel;
// import com.alibaba.easyexcel.read.listener.PageReadListener;
import com.example.myapplication.database.DBHelper;
import com.example.myapplication.utils.DialogUtils;
import com.example.myapplication.utils.GradeAnalysisHelper;
import com.example.myapplication.utils.SecurityUtils;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TeacherGradeActivity extends AppCompatActivity {

    private static final int PICK_EXCEL_FILE = 1001;
    private TextView tvClassStats;
    private Spinner spFilterCourse, spFilterClass;
    private ListView lvGrades;
    private Button btnAddGrade, btnImportGrades, btnAnalysis, btnBack;
    private DBHelper dbHelper;
    private int teacherId;
    private List<String> gradeList;
    private List<Integer> gradeIdList;
    private List<String> courseList;
    private List<String> classList;
    private ArrayAdapter<String> gradeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_grade);

        teacherId = getIntent().getIntExtra("user_id", -1);
        int userType = getIntent().getIntExtra("user_type", -1);
        if (teacherId == -1 || userType == -1) {
            Toast.makeText(this, "用户信息错误", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        // 确保只有教师和管理员才能访问该活动
        if (userType != 1 && userType != 2) {
            Toast.makeText(this, "您没有权限访问该功能", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        try {
            initViews();
            initDatabase();
            loadCourses();
            loadClasses();
            loadGrades();
            updateClassStats();
            setupListeners();
        } catch (Exception e) {
            Toast.makeText(this, "初始化失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            finish();
        }
    }

    private void initViews() {
        tvClassStats = findViewById(R.id.tv_class_stats);
        spFilterCourse = findViewById(R.id.sp_filter_course);
        spFilterClass = findViewById(R.id.sp_filter_class);
        lvGrades = findViewById(R.id.lv_grades);
        btnAddGrade = findViewById(R.id.btn_add_grade);
        btnImportGrades = findViewById(R.id.btn_import_grades);
        btnAnalysis = findViewById(R.id.btn_analyze);
        btnBack = findViewById(R.id.btn_back);

        // 初始化成绩列表
        gradeList = new ArrayList<>();
        gradeIdList = new ArrayList<>();
        courseList = new ArrayList<>();
        classList = new ArrayList<>();
        gradeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, gradeList);
        lvGrades.setAdapter(gradeAdapter);
    }

    private void initDatabase() {
        dbHelper = new DBHelper(this);
    }

    private void loadCourses() {
        // 加载课程列表到spFilterCourse
        courseList.clear();
        courseList.add("全部课程");
        // 这里应该加载教师教授的课程，暂时加载所有课程
        Cursor cursor = dbHelper.query("course", null, null, null, null, null, "course_name ASC");
        if (cursor != null) {
            int courseNameIndex = cursor.getColumnIndex("course_name");
            while (cursor.moveToNext()) {
                if (courseNameIndex != -1) {
                    String courseName = cursor.getString(courseNameIndex);
                    courseList.add(courseName);
                }
            }
            cursor.close();
        }
        ArrayAdapter<String> courseAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, courseList);
        courseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFilterCourse.setAdapter(courseAdapter);
    }

    private void loadClasses() {
        // 加载班级列表到spFilterClass
        classList.clear();
        classList.add("全部班级");
        // 从user表中获取所有班级
        Cursor cursor = dbHelper.query("user", new String[]{"class_name"}, "type = ?", new String[]{"0"}, "class_name", null, "class_name ASC");
        if (cursor != null) {
            int classNameIndex = cursor.getColumnIndex("class_name");
            while (cursor.moveToNext()) {
                if (classNameIndex != -1) {
                    String className = cursor.getString(classNameIndex);
                    if (!classList.contains(className)) {
                        classList.add(className);
                    }
                }
            }
            cursor.close();
        }
        ArrayAdapter<String> classAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, classList);
        classAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFilterClass.setAdapter(classAdapter);
    }

    private void loadGrades() {
        gradeList.clear();
        gradeIdList.clear();
        try {
            // 查询所有成绩
            Cursor cursor = dbHelper.query("grade", null, null, null, null, null, "exam_date DESC");
            if (cursor != null) {
                int idIndex = cursor.getColumnIndex("id");
                int studentIdIndex = cursor.getColumnIndex("student_id");
                int courseIdIndex = cursor.getColumnIndex("course_id");
                int scoreIndex = cursor.getColumnIndex("score");
                int examTypeIndex = cursor.getColumnIndex("exam_type");
                int examDateIndex = cursor.getColumnIndex("exam_date");
                int commentIndex = cursor.getColumnIndex("comment");
                
                while (cursor.moveToNext()) {
                    try {
                        if (idIndex != -1 && studentIdIndex != -1 && courseIdIndex != -1 && scoreIndex != -1 && examTypeIndex != -1 && examDateIndex != -1 && commentIndex != -1) {
                            int gradeId = cursor.getInt(idIndex);
                            int studentId = cursor.getInt(studentIdIndex);
                            int courseId = cursor.getInt(courseIdIndex);
                            double score = cursor.getDouble(scoreIndex);
                            String examType = cursor.getString(examTypeIndex);
                            String examDate = cursor.getString(examDateIndex);
                            String comment = cursor.getString(commentIndex);

                            String studentName = getStudentName(studentId);
                            String courseName = getCourseName(courseId);

                            // 构建教师端专用的成绩信息格式
                            String gradeInfo = "学生：" + studentName + "\n" +
                                    "课程：" + courseName + "\n" +
                                    "考试类型：" + examType + "\n" +
                                    "考试日期：" + examDate + "\n" +
                                    "成绩：" + String.format(Locale.ROOT, "%.1f", score) + "分" + (comment.isEmpty() ? "" : "\n备注：" + comment);
                            gradeList.add(gradeInfo);
                            gradeIdList.add(gradeId);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        continue; // 跳过有问题的记录
                    }
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "加载成绩失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        gradeAdapter.notifyDataSetChanged();
    }

    private void updateClassStats() {
        // 计算班级成绩统计
        int totalStudents = 0;
        int totalGrades = 0;
        double totalScore = 0;
        int passCount = 0;

        try {
            // 统计学生数量
            Cursor studentCursor = dbHelper.query("user", new String[]{"id"}, "type = ?", new String[]{"0"}, null, null, null);
            if (studentCursor != null) {
                totalStudents = studentCursor.getCount();
                studentCursor.close();
            }

            // 统计成绩
            Cursor gradeCursor = dbHelper.query("grade", new String[]{"score"}, null, null, null, null, null);
            if (gradeCursor != null) {
                int scoreIndex = gradeCursor.getColumnIndex("score");
                while (gradeCursor.moveToNext()) {
                    if (scoreIndex != -1) {
                        double score = gradeCursor.getDouble(scoreIndex);
                        totalScore += score;
                        totalGrades++;
                        if (score >= 60) {
                            passCount++;
                        }
                    }
                }
                gradeCursor.close();
            }

            double averageScore = totalGrades > 0 ? totalScore / totalGrades : 0;
            double passRate = totalGrades > 0 ? (double) passCount / totalGrades * 100 : 0;

            String stats = "班级统计：\n" +
                    "学生总数：" + totalStudents + "人\n" +
                    "成绩总数：" + totalGrades + "条\n" +
                    "平均成绩：" + String.format(Locale.ROOT, "%.2f", averageScore) + "分\n" +
                    "及格率：" + String.format(Locale.ROOT, "%.2f", passRate) + "%";
            tvClassStats.setText(stats);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "更新班级统计失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            tvClassStats.setText("班级统计：暂无数据");
        }
    }

    private String getStudentName(int studentId) {
        Cursor cursor = dbHelper.query("user", new String[]{"name"}, "id = ?", new String[]{String.valueOf(studentId)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int nameIndex = cursor.getColumnIndex("name");
            if (nameIndex != -1) {
                String name = cursor.getString(nameIndex);
                cursor.close();
                return name;
            }
            cursor.close();
        }
        return "未知学生";
    }

    private String getCourseName(int courseId) {
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
        return "未知课程";
    }

    private void setupListeners() {
        spFilterCourse.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterGrades();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        spFilterClass.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterGrades();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        lvGrades.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showGradeDetails(position);
            }
        });

        lvGrades.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                showGradeOptions(position);
                return true;
            }
        });

        btnAddGrade.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddGradeDialog();
            }
        });

        btnImportGrades.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImportGradesDialog();
            }
        });

        btnAnalysis.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showGradeAnalysis();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void showGradeDetails(int position) {
        // 显示成绩详情
        Toast.makeText(this, "查看成绩详情：" + gradeList.get(position), Toast.LENGTH_SHORT).show();
    }

    private void showGradeOptions(int position) {
        // 显示成绩操作菜单
        String[] options = {"编辑成绩", "删除成绩", "查看学生信息"};
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("成绩操作")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            editGrade(position);
                            break;
                        case 1:
                            deleteGrade(position);
                            break;
                        case 2:
                            viewStudentInfo(position);
                            break;
                    }
                });
        builder.show();
    }

    private void showAddGradeDialog() {
        // 显示添加成绩对话框
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_grade, null);
        Spinner spStudent = dialogView.findViewById(R.id.sp_student);
        Spinner spCourse = dialogView.findViewById(R.id.sp_course);
        EditText etScore = dialogView.findViewById(R.id.et_grade_score);
        EditText etExamType = dialogView.findViewById(R.id.et_grade_type);
        EditText etExamDate = dialogView.findViewById(R.id.et_grade_date);

        // 加载学生列表到spStudent
        List<String> studentNames = new ArrayList<>();
        List<Integer> studentIds = new ArrayList<>();
        Cursor studentCursor = dbHelper.query("user", new String[]{"id", "name"}, "type = ?", new String[]{"0"}, null, null, "name ASC");
        if (studentCursor != null) {
            int idIndex = studentCursor.getColumnIndex("id");
            int nameIndex = studentCursor.getColumnIndex("name");
            while (studentCursor.moveToNext() && idIndex != -1 && nameIndex != -1) {
                studentIds.add(studentCursor.getInt(idIndex));
                studentNames.add(studentCursor.getString(nameIndex));
            }
            studentCursor.close();
        }

        ArrayAdapter<String> studentAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, studentNames);
        studentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spStudent.setAdapter(studentAdapter);

        // 加载课程列表到spCourse
        List<String> courseNames = new ArrayList<>();
        List<Integer> courseIds = new ArrayList<>();
        Cursor courseCursor = dbHelper.query("course", new String[]{"id", "course_name"}, null, null, null, null, "course_name ASC");
        if (courseCursor != null) {
            int idIndex = courseCursor.getColumnIndex("id");
            int courseNameIndex = courseCursor.getColumnIndex("course_name");
            while (courseCursor.moveToNext() && idIndex != -1 && courseNameIndex != -1) {
                courseIds.add(courseCursor.getInt(idIndex));
                courseNames.add(courseCursor.getString(courseNameIndex));
            }
            courseCursor.close();
        }

        ArrayAdapter<String> courseAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, courseNames);
        courseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCourse.setAdapter(courseAdapter);

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("添加成绩")
                .setView(dialogView)
                .setPositiveButton("保存", (dialog, which) -> {
                    int studentPosition = spStudent.getSelectedItemPosition();
                    int coursePosition = spCourse.getSelectedItemPosition();
                    String scoreStr = etScore.getText().toString().trim();
                    String examType = etExamType.getText().toString().trim();
                    String examDate = etExamDate.getText().toString().trim();

                    if (studentPosition < 0) {
                        Toast.makeText(TeacherGradeActivity.this, "请选择学生", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (coursePosition < 0) {
                        Toast.makeText(TeacherGradeActivity.this, "请选择课程", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (scoreStr.isEmpty()) {
                        Toast.makeText(TeacherGradeActivity.this, "请输入成绩", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (examType.isEmpty()) {
                        Toast.makeText(TeacherGradeActivity.this, "请输入考试类型", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (examDate.isEmpty()) {
                        Toast.makeText(TeacherGradeActivity.this, "请输入考试日期", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        double score = Double.parseDouble(scoreStr);
                        if (score < 0 || score > 100) {
                            Toast.makeText(TeacherGradeActivity.this, "成绩必须在0-100之间", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        int studentId = studentIds.get(studentPosition);
                        int courseId = courseIds.get(coursePosition);

                        // 保存成绩到数据库
                        ContentValues values = new ContentValues();
                        values.put("student_id", studentId);
                        values.put("course_id", courseId);
                        values.put("score", score);
                        values.put("exam_type", examType);
                        values.put("exam_date", examDate);
                        values.put("teacher_id", teacherId);

                        dbHelper.insertGrade(values);
                        Toast.makeText(TeacherGradeActivity.this, "成绩添加成功", Toast.LENGTH_SHORT).show();
                        loadGrades();
                        updateClassStats();
                    } catch (NumberFormatException e) {
                        Toast.makeText(TeacherGradeActivity.this, "请输入有效的成绩", Toast.LENGTH_SHORT).show();
                        return;
                    }
                })
                .setNegativeButton("取消", null);
        builder.show();
    }

    private void editGrade(int position) {
        // 编辑成绩
        int gradeId = gradeIdList.get(position);
        Cursor cursor = dbHelper.query("grade", null, "id = ?", new String[]{String.valueOf(gradeId)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int studentIdIndex = cursor.getColumnIndex("student_id");
            int courseIdIndex = cursor.getColumnIndex("course_id");
            int scoreIndex = cursor.getColumnIndex("score");
            int examTypeIndex = cursor.getColumnIndex("exam_type");
            int examDateIndex = cursor.getColumnIndex("exam_date");
            
            if (studentIdIndex != -1 && courseIdIndex != -1 && scoreIndex != -1 && examTypeIndex != -1 && examDateIndex != -1) {
                int studentId = cursor.getInt(studentIdIndex);
                int courseId = cursor.getInt(courseIdIndex);
                double score = cursor.getDouble(scoreIndex);
                String examType = cursor.getString(examTypeIndex);
                String examDate = cursor.getString(examDateIndex);
                cursor.close();

                View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_grade, null);
                Spinner spStudent = dialogView.findViewById(R.id.sp_student);
                Spinner spCourse = dialogView.findViewById(R.id.sp_course);
                EditText etScore = dialogView.findViewById(R.id.et_grade_score);
                EditText etExamType = dialogView.findViewById(R.id.et_grade_type);
                EditText etExamDate = dialogView.findViewById(R.id.et_grade_date);

                // 加载学生列表到spStudent
                List<String> studentNames = new ArrayList<>();
                List<Integer> studentIds = new ArrayList<>();
                Cursor studentCursor = dbHelper.query("user", new String[]{"id", "name"}, "type = ?", new String[]{"0"}, null, null, "name ASC");
                if (studentCursor != null) {
                    int idIndex = studentCursor.getColumnIndex("id");
                    int nameIndex = studentCursor.getColumnIndex("name");
                    while (studentCursor.moveToNext() && idIndex != -1 && nameIndex != -1) {
                        studentIds.add(studentCursor.getInt(idIndex));
                        studentNames.add(studentCursor.getString(nameIndex));
                    }
                    studentCursor.close();
                }

                ArrayAdapter<String> studentAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, studentNames);
                studentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spStudent.setAdapter(studentAdapter);

                // 加载课程列表到spCourse
                List<String> courseNames = new ArrayList<>();
                List<Integer> courseIds = new ArrayList<>();
                Cursor courseCursor = dbHelper.query("course", new String[]{"id", "course_name"}, null, null, null, null, "course_name ASC");
                if (courseCursor != null) {
                    int idIndex = courseCursor.getColumnIndex("id");
                    int courseNameIndex = courseCursor.getColumnIndex("course_name");
                    while (courseCursor.moveToNext() && idIndex != -1 && courseNameIndex != -1) {
                        courseIds.add(courseCursor.getInt(idIndex));
                        courseNames.add(courseCursor.getString(courseNameIndex));
                    }
                    courseCursor.close();
                }

                ArrayAdapter<String> courseAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, courseNames);
                courseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spCourse.setAdapter(courseAdapter);

                // 设置当前选择
                spStudent.setSelection(studentIds.indexOf(studentId));
                spCourse.setSelection(courseIds.indexOf(courseId));
                etScore.setText(String.valueOf(score));
                etExamType.setText(examType);
                etExamDate.setText(examDate);

                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                builder.setTitle("编辑成绩")
                        .setView(dialogView)
                        .setPositiveButton("保存", (dialog, which) -> {
                            int studentPosition = spStudent.getSelectedItemPosition();
                            int coursePosition = spCourse.getSelectedItemPosition();
                            String scoreStr = etScore.getText().toString().trim();
                            String newExamType = etExamType.getText().toString().trim();
                            String newExamDate = etExamDate.getText().toString().trim();

                            if (studentPosition < 0) {
                                Toast.makeText(TeacherGradeActivity.this, "请选择学生", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if (coursePosition < 0) {
                                Toast.makeText(TeacherGradeActivity.this, "请选择课程", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if (scoreStr.isEmpty()) {
                                Toast.makeText(TeacherGradeActivity.this, "请输入成绩", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if (newExamType.isEmpty()) {
                                Toast.makeText(TeacherGradeActivity.this, "请输入考试类型", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if (newExamDate.isEmpty()) {
                                Toast.makeText(TeacherGradeActivity.this, "请输入考试日期", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            try {
                                double newScore = Double.parseDouble(scoreStr);
                                if (newScore < 0 || newScore > 100) {
                                    Toast.makeText(TeacherGradeActivity.this, "成绩必须在0-100之间", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                int newStudentId = studentIds.get(studentPosition);
                                int newCourseId = courseIds.get(coursePosition);

                                // 更新成绩到数据库
                                ContentValues values = new ContentValues();
                                values.put("student_id", newStudentId);
                                values.put("course_id", newCourseId);
                                values.put("score", newScore);
                                values.put("exam_type", newExamType);
                                values.put("exam_date", newExamDate);

                                dbHelper.update("grade", values, "id = ?", new String[]{String.valueOf(gradeId)});
                                Toast.makeText(TeacherGradeActivity.this, "成绩更新成功", Toast.LENGTH_SHORT).show();
                                loadGrades();
                                updateClassStats();
                            } catch (NumberFormatException e) {
                                Toast.makeText(TeacherGradeActivity.this, "请输入有效的成绩", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        })
                        .setNegativeButton("取消", null);
                builder.show();
            } else {
                cursor.close();
            }
        }
    }

    private void deleteGrade(int position) {
        // 删除成绩
        int gradeId = gradeIdList.get(position);
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("确认删除")
                .setMessage("确定要删除这个成绩吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    // 从数据库中删除成绩
                    dbHelper.delete("grade", "id = ?", new String[]{String.valueOf(gradeId)});
                    Toast.makeText(TeacherGradeActivity.this, "成绩已删除", Toast.LENGTH_SHORT).show();
                    loadGrades(); // 重新加载成绩列表
                    updateClassStats(); // 重新更新统计数据
                })
                .setNegativeButton("取消", null);
        builder.show();
    }

    private void viewStudentInfo(int position) {
        // 查看学生信息
        int gradeId = gradeIdList.get(position);
        Cursor cursor = dbHelper.query("grade", new String[]{"student_id"}, "id = ?", new String[]{String.valueOf(gradeId)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int studentIdIndex = cursor.getColumnIndex("student_id");
            if (studentIdIndex != -1) {
                int studentId = cursor.getInt(studentIdIndex);
                cursor.close();

                Cursor studentCursor = dbHelper.getUserById(studentId);
                if (studentCursor != null && studentCursor.moveToFirst()) {
                    int nameIndex = studentCursor.getColumnIndex("name");
                    int studentIdIndex2 = studentCursor.getColumnIndex("student_id");
                    int classNameIndex = studentCursor.getColumnIndex("class_name");
                    int emailIndex = studentCursor.getColumnIndex("email");
                    int phoneIndex = studentCursor.getColumnIndex("phone");

                    String name = "";
                    if (nameIndex != -1) {
                        name = studentCursor.getString(nameIndex);
                    }
                    String studentIdStr = "";
                    if (studentIdIndex2 != -1) {
                        studentIdStr = studentCursor.getString(studentIdIndex2);
                    }
                    String className = "";
                    if (classNameIndex != -1) {
                        className = studentCursor.getString(classNameIndex);
                    }
                    String email = "";
                    if (emailIndex != -1) {
                        email = studentCursor.getString(emailIndex);
                    }
                    String phone = "";
                    if (phoneIndex != -1) {
                        phone = studentCursor.getString(phoneIndex);
                    }
                    studentCursor.close();

                    String studentInfo = "学生信息：\n" +
                            "姓名：" + name + "\n" +
                            "学号：" + studentIdStr + "\n" +
                            "班级：" + className + "\n" +
                            "邮箱：" + email + "\n" +
                            "电话：" + phone;

                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                    builder.setTitle("学生信息")
                            .setMessage(studentInfo)
                            .setPositiveButton("确定", null);
                    builder.show();
                } else {
                    cursor.close();
                    Toast.makeText(this, "获取学生信息失败", Toast.LENGTH_SHORT).show();
                }
            } else {
                cursor.close();
            }
        }
    }

    private void showImportGradesDialog() {
        // 显示导入成绩选项对话框
        String[] importOptions = {"Excel导入", "手动录入", "示例数据导入"};
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("成绩导入方式")
                .setItems(importOptions, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            importFromExcel();
                            break;
                        case 1:
                            showAddGradeDialog();
                            break;
                        case 2:
                            importExampleData();
                            break;
                    }
                });
        builder.show();
    }

    private void importFromExcel() {
        // 从Excel导入成绩
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_EXCEL_FILE);
    }

    private void importExampleData() {
        // 导入示例成绩数据
        Toast.makeText(this, "示例数据导入功能已启动", Toast.LENGTH_SHORT).show();
        importSampleGrades();
    }

    private void importSampleGrades() {
        // 导入示例成绩数据
        // 获取学生列表
        Cursor studentCursor = dbHelper.query("user", new String[]{"id"}, "type = ?", new String[]{"0"}, null, null, null);
        List<Integer> studentIds = new ArrayList<>();
        if (studentCursor != null) {
            int idIndex = studentCursor.getColumnIndex("id");
            while (studentCursor.moveToNext() && idIndex != -1) {
                studentIds.add(studentCursor.getInt(idIndex));
            }
            studentCursor.close();
        }
        
        // 获取课程列表
        Cursor courseCursor = dbHelper.query("course", new String[]{"id"}, null, null, null, null, null);
        List<Integer> courseIds = new ArrayList<>();
        if (courseCursor != null) {
            int idIndex = courseCursor.getColumnIndex("id");
            while (courseCursor.moveToNext() && idIndex != -1) {
                courseIds.add(courseCursor.getInt(idIndex));
            }
            courseCursor.close();
        }
        
        // 如果没有学生或课程，添加一些示例数据
        if (studentIds.isEmpty()) {
            // 添加示例学生
            addSampleStudents();
            // 重新获取学生列表
            studentCursor = dbHelper.query("user", new String[]{"id"}, "type = ?", new String[]{"0"}, null, null, null);
            if (studentCursor != null) {
                int idIndex = studentCursor.getColumnIndex("id");
                while (studentCursor.moveToNext() && idIndex != -1) {
                    studentIds.add(studentCursor.getInt(idIndex));
                }
                studentCursor.close();
            }
        }
        
        if (courseIds.isEmpty()) {
            // 添加示例课程
            addSampleCourses();
            // 重新获取课程列表
            courseCursor = dbHelper.query("course", new String[]{"id"}, null, null, null, null, null);
            if (courseCursor != null) {
                int idIndex = courseCursor.getColumnIndex("id");
                while (courseCursor.moveToNext() && idIndex != -1) {
                    courseIds.add(courseCursor.getInt(idIndex));
                }
                courseCursor.close();
            }
        }
        
        // 添加示例成绩
        if (!studentIds.isEmpty() && !courseIds.isEmpty()) {
            // 为每个学生添加几门课程的成绩
            for (int studentId : studentIds) {
                for (int courseId : courseIds) {
                    // 生成随机成绩 (60-100)
                    double score = 60 + Math.random() * 40;
                    
                    // 考试类型
                    String[] examTypes = {"期中考试", "期末考试", "平时测验", "实验报告"};
                    String examType = examTypes[(int) (Math.random() * examTypes.length)];
                    
                    // 考试日期 (最近3个月)
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                    Date date = new Date();
                    long threeMonthsAgo = date.getTime() - 90 * 24 * 60 * 60 * 1000;
                    Date examDate = new Date(threeMonthsAgo + (long) (Math.random() * (date.getTime() - threeMonthsAgo)));
                    
                    // 添加成绩
                    ContentValues values = new ContentValues();
                    values.put("student_id", studentId);
                    values.put("course_id", courseId);
                    values.put("score", score);
                    values.put("exam_type", examType);
                    values.put("exam_date", sdf.format(examDate));
                    values.put("teacher_id", teacherId);
                    
                    dbHelper.insertGrade(values);
                }
            }
        }
        
        // 重新加载成绩列表和统计数据
        loadGrades();
        updateClassStats();
        Toast.makeText(this, "成绩导入成功！", Toast.LENGTH_SHORT).show();
    }
    
    private void addSampleStudents() {
        // 添加示例学生
        String[] studentNames = {"张三", "李四", "王五", "赵六", "钱七", "孙八", "周九", "吴十"};
        String[] classNames = {"计算机1班", "计算机2班", "计算机3班"};
        
        for (int i = 0; i < studentNames.length; i++) {
            ContentValues values = new ContentValues();
            values.put("username", "student" + (i + 1));
            values.put("password", SecurityUtils.encryptPassword("123456")); // 使用加密存储密码
            values.put("name", studentNames[i]);
            values.put("type", 0); // 学生
            values.put("student_id", "2024000" + (i + 1));
            values.put("class_name", classNames[i % classNames.length]);
            values.put("major", "计算机科学与技术");
            values.put("phone", "1380013800" + (i + 1));
            values.put("email", "student" + (i + 1) + "@example.com");
            
            dbHelper.insertUser(values);
        }
    }
    
    private void addSampleCourses() {
        // 添加示例课程
        String[] courseNames = {"高等数学", "大学英语", "数据结构", "操作系统", "计算机网络", "数据库"};
        String[] teacherNames = {"张教授", "李老师", "王教授", "刘老师", "陈老师", "赵老师"};
        String[] classrooms = {"教1-101", "教2-202", "教3-303", "教4-404", "教5-505", "教6-606"};
        String[] times = {"08:00-09:40", "10:00-11:40", "14:00-15:40", "16:00-17:40"};
        int[] days = {1, 2, 3, 4, 5}; // 周一到周五
        
        for (int i = 0; i < courseNames.length; i++) {
            ContentValues values = new ContentValues();
            values.put("course_name", courseNames[i]);
            values.put("teacher_name", teacherNames[i]);
            values.put("classroom", classrooms[i]);
            values.put("start_time", times[i % times.length].split("-")[0]);
            values.put("end_time", times[i % times.length].split("-")[1]);
            values.put("day_of_week", days[i % days.length]);
            values.put("is_favorite", 0);
            
            dbHelper.insertCourse(values);
        }
    }

    private void showGradeAnalysis() {
        try {
            // 显示加载对话框
            android.app.AlertDialog loadingDialog = new android.app.AlertDialog.Builder(this)
                    .setMessage("正在分析成绩数据...")
                    .setCancelable(false)
                    .show();
            
            // 在后台线程中执行分析
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Cursor cursor = dbHelper.query("grade", new String[]{"score", "course_id"}, null, null, null, null, null);
                        
                        if (cursor == null) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    loadingDialog.dismiss();
                                    Toast.makeText(TeacherGradeActivity.this, "暂无成绩数据", Toast.LENGTH_LONG).show();
                                }
                            });
                            return;
                        }
                        
                        if (!cursor.moveToFirst()) {
                            cursor.close();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    loadingDialog.dismiss();
                                    Toast.makeText(TeacherGradeActivity.this, "暂无成绩数据", Toast.LENGTH_LONG).show();
                                }
                            });
                            return;
                        }
                        
                        cursor.moveToFirst();
                        final Cursor finalCursor = cursor;
                        
                        // 执行分析
                        GradeAnalysisHelper.analyzeGradesAsync(finalCursor, new GradeAnalysisHelper.GradeAnalysisCallback() {
                            @Override
                            public String getCourseName(int courseId) {
                                return TeacherGradeActivity.this.getCourseName(courseId);
                            }
                        }, new GradeAnalysisHelper.AnalysisResultCallback() {
                            @Override
                            public void onAnalysisComplete(GradeAnalysisHelper.GradeAnalysisResult result) {
                                loadingDialog.dismiss();
                                showAnalysisResult(result);
                            }
                        });
                    } catch (Exception e) {
                        Log.e("TeacherGradeActivity", "Error in grade analysis", e);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                loadingDialog.dismiss();
                                Toast.makeText(TeacherGradeActivity.this, "成绩分析失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "成绩分析失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void showAnalysisResult(GradeAnalysisHelper.GradeAnalysisResult result) {
        if (result == null) {
            Toast.makeText(this, "分析结果为空", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_grade_analysis, null);
            if (dialogView == null) {
                Toast.makeText(this, "加载分析对话框失败", Toast.LENGTH_SHORT).show();
                return;
            }
            
            PieChart pieChart = dialogView.findViewById(R.id.pie_chart);
            BarChart barChart = dialogView.findViewById(R.id.bar_chart);
            TextView tvAnalysisText = dialogView.findViewById(R.id.tv_analysis_text);

            if (tvAnalysisText == null) {
                Toast.makeText(this, "分析文本视图为空", Toast.LENGTH_SHORT).show();
                return;
            }

            // 生成分析文本
            StringBuilder analysis = new StringBuilder("班级成绩分析报告：\n\n");
            analysis.append("分数段分布：\n");
            analysis.append("优秀 (90-100)：").append(result.excellent).append("人\n");
            analysis.append("良好 (80-89)：").append(result.good).append("人\n");
            analysis.append("中等 (70-79)：").append(result.average).append("人\n");
            analysis.append("及格 (60-69)：").append(result.pass).append("人\n");
            analysis.append("不及格 (0-59)：").append(result.fail).append("人\n\n");

            analysis.append("及格率：").append(String.format(Locale.ROOT, "%.2f", result.passRate)).append("%\n");
            analysis.append("优秀率：").append(String.format(Locale.ROOT, "%.2f", result.excellentRate)).append("%\n");
            analysis.append("平均成绩：").append(String.format(Locale.ROOT, "%.2f", result.averageScore)).append("分\n");

            tvAnalysisText.setText(analysis.toString());

            // 设置饼图
            try {
                if (pieChart != null) {
                    GradeAnalysisHelper.setupPieChart(pieChart, result);
                }
            } catch (Exception pieEx) {
                Log.e("TeacherGradeActivity", "Error setting up pie chart", pieEx);
            }

            // 设置柱状图（多科目对比）
            try {
                if (barChart != null && result.courseNames != null && result.courseScores != null) {
                    GradeAnalysisHelper.setupBarChart(barChart, result.courseNames, result.courseScores);
                }
            } catch (Exception barEx) {
                Log.e("TeacherGradeActivity", "Error setting up bar chart", barEx);
            }

            // 显示详细的分析报告
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("班级成绩分析报告");
            builder.setView(dialogView);
            builder.setPositiveButton("确定", null);
            builder.show();
        } catch (Exception e) {
            Log.e("TeacherGradeActivity", "Error showing analysis result", e);
            Toast.makeText(this, "显示分析结果失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void filterGrades() {
        // 按课程和班级筛选成绩
        String selectedCourse = spFilterCourse.getSelectedItem().toString();
        String selectedClass = spFilterClass.getSelectedItem().toString();

        gradeList.clear();
        gradeIdList.clear();

        try {
            // 构建查询条件
            StringBuilder selection = new StringBuilder();
            List<String> selectionArgs = new ArrayList<>();

            if (!selectedCourse.equals("全部课程")) {
                selection.append("course_id IN (SELECT id FROM course WHERE course_name = ?)");
                selectionArgs.add(selectedCourse);
            }

            if (!selectedClass.equals("全部班级")) {
                if (selection.length() > 0) {
                    selection.append(" AND ");
                }
                selection.append("student_id IN (SELECT id FROM user WHERE class_name = ?)");
                selectionArgs.add(selectedClass);
            }

            String[] args = selectionArgs.toArray(new String[0]);
            Cursor cursor = dbHelper.query("grade", null, selection.length() > 0 ? selection.toString() : null, args, null, null, "exam_date DESC");

            if (cursor != null) {
                int idIndex = cursor.getColumnIndex("id");
                int studentIdIndex = cursor.getColumnIndex("student_id");
                int courseIdIndex = cursor.getColumnIndex("course_id");
                int scoreIndex = cursor.getColumnIndex("score");
                int examTypeIndex = cursor.getColumnIndex("exam_type");
                int examDateIndex = cursor.getColumnIndex("exam_date");
                int commentIndex = cursor.getColumnIndex("comment");
                
                while (cursor.moveToNext()) {
                    try {
                        if (idIndex != -1 && studentIdIndex != -1 && courseIdIndex != -1 && scoreIndex != -1 && examTypeIndex != -1 && examDateIndex != -1 && commentIndex != -1) {
                            int gradeId = cursor.getInt(idIndex);
                            int studentId = cursor.getInt(studentIdIndex);
                            int courseId = cursor.getInt(courseIdIndex);
                            double score = cursor.getDouble(scoreIndex);
                            String examType = cursor.getString(examTypeIndex);
                            String examDate = cursor.getString(examDateIndex);
                            String comment = cursor.getString(commentIndex);

                            String studentName = getStudentName(studentId);
                            String courseName = getCourseName(courseId);

                            // 构建教师端专用的成绩信息格式
                            String gradeInfo = "学生：" + studentName + "\n" +
                                    "课程：" + courseName + "\n" +
                                    "考试类型：" + examType + "\n" +
                                    "考试日期：" + examDate + "\n" +
                                    "成绩：" + String.format(Locale.ROOT, "%.1f", score) + "分" + (comment.isEmpty() ? "" : "\n备注：" + comment);
                            gradeList.add(gradeInfo);
                            gradeIdList.add(gradeId);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        continue; // 跳过有问题的记录
                    }
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "筛选成绩失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        gradeAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_EXCEL_FILE && data != null) {
                // 处理Excel文件选择
                Uri uri = data.getData();
                if (uri != null) {
                    parseExcelFile(uri);
                }
            }
        }
    }

    private void parseExcelFile(Uri uri) {
        try {
            // 模拟Excel解析过程
            List<GradeExcelData> gradeDataList = new ArrayList<>();
            
            // 模拟解析结果
            GradeExcelData mockData1 = new GradeExcelData();
            mockData1.setStudentId("2024001");
            mockData1.setStudentName("张三");
            mockData1.setCourseName("高等数学");
            mockData1.setScore(85.5);
            mockData1.setExamType("期末考试");
            mockData1.setExamDate("2024-01-15");
            gradeDataList.add(mockData1);
            
            GradeExcelData mockData2 = new GradeExcelData();
            mockData2.setStudentId("2024002");
            mockData2.setStudentName("李四");
            mockData2.setCourseName("大学英语");
            mockData2.setScore(92.0);
            mockData2.setExamType("期末考试");
            mockData2.setExamDate("2024-01-16");
            gradeDataList.add(mockData2);
            
            // 将解析的数据导入到数据库
            importGradesFromExcel(gradeDataList);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Excel文件解析失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void importGradesFromExcel(List<GradeExcelData> gradeDataList) {
        if (gradeDataList.isEmpty()) {
            Toast.makeText(this, "Excel文件中没有成绩数据", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 显示加载对话框
        final android.app.AlertDialog loadingDialog = new android.app.AlertDialog.Builder(this)
                .setMessage("正在导入成绩...")
                .setCancelable(false)
                .create();
        loadingDialog.show();
        
        int importedCount = 0;
        int failedCount = 0;
        StringBuilder errorMessages = new StringBuilder();
        
        for (GradeExcelData gradeData : gradeDataList) {
            try {
                // 查找学生ID
                int studentId = findStudentIdByStudentId(gradeData.getStudentId());
                if (studentId == -1) {
                    // 如果学生不存在，创建新学生
                    studentId = createStudent(gradeData);
                }
                
                // 查找课程ID
                int courseId = findCourseIdByCourseName(gradeData.getCourseName());
                if (courseId == -1) {
                    // 如果课程不存在，创建新课程
                    courseId = createCourse(gradeData);
                }
                
                // 添加成绩
                ContentValues values = new ContentValues();
                values.put("student_id", studentId);
                values.put("course_id", courseId);
                values.put("score", gradeData.getScore());
                values.put("exam_type", gradeData.getExamType());
                values.put("exam_date", gradeData.getExamDate());
                values.put("teacher_id", teacherId);
                
                if (dbHelper.insertGrade(values) > 0) {
                    importedCount++;
                } else {
                    failedCount++;
                    errorMessages.append("导入失败: " + gradeData.getStudentName() + " - " + gradeData.getCourseName() + "\n");
                }
            } catch (Exception e) {
                failedCount++;
                errorMessages.append("导入失败: " + gradeData.getStudentName() + " - " + gradeData.getCourseName() + " - " + e.getMessage() + "\n");
            }
        }
        
        // 重新加载成绩列表和统计数据
        loadGrades();
        updateClassStats();
        
        loadingDialog.dismiss();
        
        // 显示导入结果
        StringBuilder resultMessage = new StringBuilder();
        resultMessage.append("成功导入" + importedCount + "条成绩记录\n");
        if (failedCount > 0) {
            resultMessage.append("失败" + failedCount + "条记录\n");
            resultMessage.append("失败原因:\n" + errorMessages.toString());
            DialogUtils.showErrorDialog(this, resultMessage.toString());
        } else {
            Toast.makeText(this, resultMessage.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    private int findStudentIdByStudentId(String studentId) {
        Cursor cursor = dbHelper.query("user", new String[]{"id"}, "type = ? AND student_id = ?", 
                new String[]{"0", studentId}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int idIndex = cursor.getColumnIndex("id");
            if (idIndex != -1) {
                int id = cursor.getInt(idIndex);
                cursor.close();
                return id;
            }
        }
        if (cursor != null) cursor.close();
        return -1;
    }

    private int findCourseIdByCourseName(String courseName) {
        Cursor cursor = dbHelper.query("course", new String[]{"id"}, "course_name = ?", 
                new String[]{courseName}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int idIndex = cursor.getColumnIndex("id");
            if (idIndex != -1) {
                int id = cursor.getInt(idIndex);
                cursor.close();
                return id;
            }
        }
        if (cursor != null) cursor.close();
        return -1;
    }

    private int createStudent(GradeExcelData gradeData) {
        ContentValues values = new ContentValues();
        values.put("username", "student_" + gradeData.getStudentId());
        values.put("password", SecurityUtils.encryptPassword("123456"));
        values.put("name", gradeData.getStudentName());
        values.put("type", 0); // 学生
        values.put("student_id", gradeData.getStudentId());
        values.put("class_name", "未知班级");
        values.put("major", "未知专业");
        
        return (int) dbHelper.insertUser(values);
    }

    private int createCourse(GradeExcelData gradeData) {
        ContentValues values = new ContentValues();
        values.put("course_name", gradeData.getCourseName());
        values.put("teacher_name", "未知教师");
        values.put("classroom", "未知教室");
        values.put("start_time", "08:00");
        values.put("end_time", "09:40");
        values.put("day_of_week", 1);
        
        return (int) dbHelper.insertCourse(values);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}
