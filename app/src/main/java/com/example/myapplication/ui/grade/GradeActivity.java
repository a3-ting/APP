package com.example.myapplication.ui.grade;

import android.content.ContentValues;
import android.database.Cursor;
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

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.database.DBHelper;
import com.example.myapplication.utils.GradeAnalysisHelper;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

/**
     * 成绩管理活动
     * 提供成绩的查看、添加、编辑、删除和分析功能
     */
public class GradeActivity extends AppCompatActivity {
    
    // 常量定义
    private static final String TAG = "GradeActivity";
    private static final double MIN_SCORE = 0.0;
    private static final double MAX_SCORE = 100.0;
    private static final double DIALOG_WIDTH_RATIO = 0.85;
    private static final double DIALOG_HEIGHT_RATIO = 0.7;

    private TextView tvAverageScore, tvGpa, tvWeakSubject;
    private Spinner spFilterCourse;
    private ListView lvGrades;
    private Button btnAddGrade, btnAnalysis, btnBack;
    private DBHelper dbHelper;
    private int userId;
    private int userType;
    private List<String> gradeList;
    private List<Integer> gradeIdList;
    private List<String> courseList;
    private ArrayAdapter<String> gradeAdapter;

    /**
     * 创建活动
     * @param savedInstanceState 保存的实例状态
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grade);

        userId = getIntent().getIntExtra("user_id", -1);
        this.userType = getIntent().getIntExtra("user_type", -1);
        if (userId == -1 || userType == -1) {
            Toast.makeText(this, "用户信息错误", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        // 学生、教师、管理员都可以访问成绩管理
        if (userType != 0 && userType != 1 && userType != 2) {
            Toast.makeText(this, "您没有权限访问该功能", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        try {
            initViews();
            initDatabase();
            loadCourses();
            loadGrades();
            updateGradeStats();
            setupListeners();
        } catch (Exception e) {
            Toast.makeText(this, "初始化失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            finish();
        }
    }

    /**
     * 初始化视图
     */
    private void initViews() {
        tvAverageScore = findViewById(R.id.tv_average_grade);
        tvGpa = findViewById(R.id.tv_gpa);
        tvWeakSubject = findViewById(R.id.tv_weak_subject);
        spFilterCourse = findViewById(R.id.sp_grade_type);
        lvGrades = findViewById(R.id.lv_grades);
        btnAddGrade = findViewById(R.id.btn_add_grade);
        btnAnalysis = findViewById(R.id.btn_analyze);
        btnBack = findViewById(R.id.btn_back);

        // 初始化成绩列表
        gradeList = new ArrayList<>();
        gradeIdList = new ArrayList<>();
        courseList = new ArrayList<>();
        gradeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, gradeList);
        lvGrades.setAdapter(gradeAdapter);
    }

    /**
     * 初始化数据库
     */
    private void initDatabase() {
        dbHelper = new DBHelper(this);
    }

    /**
     * 加载课程列表
     */
    private void loadCourses() {
        // 加载课程列表到spFilterCourse
        courseList.clear();
        courseList.add("全部课程");
        Cursor cursor = dbHelper.getCoursesByUserId(userId);
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

    /**
     * 加载成绩列表
     */
    private void loadGrades() {
        gradeList.clear();
        gradeIdList.clear();
        try {
            if (dbHelper == null) {
                Log.e(TAG, "Database helper is null");
                Toast.makeText(this, "数据库初始化失败", Toast.LENGTH_SHORT).show();
                return;
            }
            
            Cursor cursor = dbHelper.query("grade", null, "student_id = ?", 
                    new String[]{String.valueOf(userId)}, null, null, "id DESC");
            if (cursor != null) {
                try {
                    while (cursor.moveToNext()) {
                        try {
                            int idIndex = cursor.getColumnIndex("id");
                            int courseIdIndex = cursor.getColumnIndex("course_id");
                            int scoreIndex = cursor.getColumnIndex("score");
                            int examTypeIndex = cursor.getColumnIndex("exam_type");
                            int examDateIndex = cursor.getColumnIndex("exam_date");
                            int commentIndex = cursor.getColumnIndex("comment");
                            
                            if (idIndex != -1 && courseIdIndex != -1 && scoreIndex != -1) {
                                int gradeId = cursor.getInt(idIndex);
                                int courseId = cursor.getInt(courseIdIndex);
                                String courseName = getCourseName(courseId);
                                double score = cursor.getDouble(scoreIndex);
                                
                                String examType = examTypeIndex != -1 ? cursor.getString(examTypeIndex) : "";
                                String examDate = examDateIndex != -1 ? cursor.getString(examDateIndex) : "";
                                String comment = commentIndex != -1 ? cursor.getString(commentIndex) : "";

                                String gradeInfo = courseName + "\n" +
                                        "考试类型：" + examType + "\n" +
                                        "考试日期：" + examDate + "\n" +
                                        "成绩：" + String.format(Locale.ROOT, "%.1f", score) + "分" + (comment == null || comment.isEmpty() ? "" : "\n备注：" + comment);
                                gradeList.add(gradeInfo);
                                gradeIdList.add(gradeId);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing grade record", e);
                            continue;
                        }
                    }
                } finally {
                    if (!cursor.isClosed()) {
                        cursor.close();
                    }
                }
            } else {
                Log.w(TAG, "No cursor returned from database query");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading grades", e);
            Toast.makeText(this, "加载成绩失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        gradeAdapter.notifyDataSetChanged();
    }

    /**
     * 更新成绩统计信息
     */
    private void updateGradeStats() {
        int totalCourses = 0;
        double averageScore = 0;
        int highestScore = 0;
        int lowestScore = 100;
        double totalScore = 0;
        String weakSubject = "暂无数据";

        try {
            Cursor cursor = dbHelper.getGradesByStudentId(userId);
            if (cursor != null) {
                int scoreIndex = cursor.getColumnIndex("score");
                int courseIdIndex = cursor.getColumnIndex("course_id");
                
                while (cursor.moveToNext()) {
                    try {
                        if (scoreIndex != -1) {
                            double score = cursor.getDouble(scoreIndex);
                            totalScore += score;
                            totalCourses++;
                            if (score > highestScore) {
                                highestScore = (int) score;
                            }
                            if (score < lowestScore) {
                                lowestScore = (int) score;
                                // 记录最低分的科目
                                if (courseIdIndex != -1) {
                                    int courseId = cursor.getInt(courseIdIndex);
                                    weakSubject = getCourseName(courseId);
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        continue; // 跳过有问题的记录
                    }
                }
                cursor.close();
            }

            if (totalCourses > 0) {
                averageScore = totalScore / totalCourses;
            }

            // 计算GPA（4.0分制）
            double gpa = calculateGPA(averageScore);

            tvAverageScore.setText(String.format(Locale.ROOT, "%.2f", averageScore));
            tvGpa.setText(String.format(Locale.ROOT, "%.2f", gpa));
            tvWeakSubject.setText("薄弱科目：" + weakSubject);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "更新成绩统计失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            // 设置默认值
            tvAverageScore.setText("0.00");
            tvGpa.setText("0.00");
            tvWeakSubject.setText("薄弱科目：暂无数据");
        }
    }

    /**
     * 计算GPA
     * @param averageScore 平均成绩
     * @return GPA值
     */
    private double calculateGPA(double averageScore) {
        // 4.0分制GPA计算
        if (averageScore >= 90) {
            return 4.0;
        } else if (averageScore >= 85) {
            return 3.7;
        } else if (averageScore >= 80) {
            return 3.3;
        } else if (averageScore >= 75) {
            return 3.0;
        } else if (averageScore >= 70) {
            return 2.7;
        } else if (averageScore >= 65) {
            return 2.3;
        } else if (averageScore >= 60) {
            return 2.0;
        } else {
            return 0.0;
        }
    }

    /**
     * 获取课程名称
     * @param courseId 课程ID
     * @return 课程名称
     */
    private String getCourseName(int courseId) {
        Cursor cursor = null;
        try {
            // 安全检查：确保 courseId 有效
            if (courseId <= 0) {
                return "未知课程";
            }
            
            cursor = dbHelper.query("course", new String[]{"course_name"}, "id = ?", new String[]{String.valueOf(courseId)}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int courseNameIndex = cursor.getColumnIndex("course_name");
                if (courseNameIndex != -1) {
                    String courseName = cursor.getString(courseNameIndex);
                    if (courseName != null && !courseName.isEmpty()) {
                        return courseName;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                try {
                    cursor.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return "未知课程";
    }

    /**
     * 设置监听器
     */
    private void setupListeners() {
        spFilterCourse.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCourse = parent.getItemAtPosition(position).toString();
                filterGrades(selectedCourse);
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

    /**
     * 显示成绩详情
     * @param position 成绩位置
     */
    private void showGradeDetails(int position) {
        // 显示成绩详情
        if (position >= 0 && position < gradeList.size()) {
            Toast.makeText(this, "查看成绩详情：" + gradeList.get(position), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 显示成绩操作菜单
     * @param position 成绩位置
     */
    private void showGradeOptions(int position) {
        // 显示成绩操作菜单
        if (position < 0 || position >= gradeList.size() || position >= gradeIdList.size()) {
            return;
        }
        
        String[] options = {"编辑成绩", "删除成绩", "分享成绩"};
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
                            shareGrade(position);
                            break;
                    }
                });
        builder.show();
    }

    /**
     * 加载课程列表
     * @return 包含课程ID和名称的映射
     */
    private Map<Integer, String> loadCourseList() {
        Map<Integer, String> courseMap = new HashMap<>();
        Cursor cursor = dbHelper.getCoursesByUserId(userId);
        if (cursor != null) {
            try {
                int idIndex = cursor.getColumnIndex("id");
                int courseNameIndex = cursor.getColumnIndex("course_name");
                while (cursor.moveToNext() && idIndex != -1 && courseNameIndex != -1) {
                    int courseId = cursor.getInt(idIndex);
                    String courseName = cursor.getString(courseNameIndex);
                    courseMap.put(courseId, courseName);
                }
            } finally {
                if (!cursor.isClosed()) {
                    cursor.close();
                }
            }
        }
        return courseMap;
    }

    /**
     * 显示添加成绩对话框
     */
    private void showAddGradeDialog() {
        // 显示添加成绩对话框
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_grade, null);
        Spinner spCourse = dialogView.findViewById(R.id.sp_course);
        EditText etScore = dialogView.findViewById(R.id.et_grade_score);
        EditText etExamType = dialogView.findViewById(R.id.et_grade_type);
        EditText etExamDate = dialogView.findViewById(R.id.et_grade_date);

        // 加载课程列表到spCourse
        Map<Integer, String> courseMap = loadCourseList();
        List<String> courseNames = new ArrayList<>(courseMap.values());
        List<Integer> courseIds = new ArrayList<>(courseMap.keySet());

        ArrayAdapter<String> courseAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, courseNames);
        courseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCourse.setAdapter(courseAdapter);

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("添加成绩")
                .setView(dialogView)
                .setPositiveButton("保存", (dialog, which) -> {
                    int coursePosition = spCourse.getSelectedItemPosition();
                    String scoreStr = etScore.getText().toString().trim();
                    String examType = etExamType.getText().toString().trim();
                    String examDate = etExamDate.getText().toString().trim();

                    if (validateGradeInput(coursePosition, scoreStr, examType, examDate)) {
                        try {
                            double score = Double.parseDouble(scoreStr);
                            int courseId = courseIds.get(coursePosition);

                            // 保存成绩到数据库
                            ContentValues values = new ContentValues();
                            values.put("student_id", userId);
                            values.put("course_id", courseId);
                            values.put("score", score);
                            values.put("exam_type", examType);
                            values.put("exam_date", examDate);

                            dbHelper.insertGrade(values);
                            Toast.makeText(GradeActivity.this, "成绩添加成功", Toast.LENGTH_SHORT).show();
                            loadGrades();
                            updateGradeStats();
                        } catch (NumberFormatException e) {
                            Toast.makeText(GradeActivity.this, "请输入有效的成绩", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("取消", null);
        builder.show();
    }

    /**
     * 编辑成绩
     * @param position 成绩位置
     */
    private void editGrade(int position) {
        // 编辑成绩
        if (position < 0 || position >= gradeIdList.size()) {
            return;
        }
        
        int gradeId = gradeIdList.get(position);
        Cursor cursor = dbHelper.query("grade", null, "id = ?", new String[]{String.valueOf(gradeId)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int courseIdIndex = cursor.getColumnIndex("course_id");
            int scoreIndex = cursor.getColumnIndex("score");
            int examTypeIndex = cursor.getColumnIndex("exam_type");
            int examDateIndex = cursor.getColumnIndex("exam_date");
            
            if (courseIdIndex != -1 && scoreIndex != -1 && examTypeIndex != -1 && examDateIndex != -1) {
                int courseId = cursor.getInt(courseIdIndex);
                double score = cursor.getDouble(scoreIndex);
                String examType = cursor.getString(examTypeIndex);
                String examDate = cursor.getString(examDateIndex);
                cursor.close();

                View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_grade, null);
                Spinner spCourse = dialogView.findViewById(R.id.sp_course);
                EditText etScore = dialogView.findViewById(R.id.et_grade_score);
                EditText etExamType = dialogView.findViewById(R.id.et_grade_type);
                EditText etExamDate = dialogView.findViewById(R.id.et_grade_date);

                // 加载课程列表到spCourse
                Map<Integer, String> courseMap = loadCourseList();
                List<String> courseNames = new ArrayList<>(courseMap.values());
                List<Integer> courseIds = new ArrayList<>(courseMap.keySet());

                ArrayAdapter<String> courseAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, courseNames);
                courseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spCourse.setAdapter(courseAdapter);

                // 设置当前课程选择
                int coursePosition = courseIds.indexOf(courseId);
                if (coursePosition >= 0) {
                    spCourse.setSelection(coursePosition);
                }
                etScore.setText(String.valueOf(score));
                etExamType.setText(examType);
                etExamDate.setText(examDate);

                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                builder.setTitle("编辑成绩")
                        .setView(dialogView)
                        .setPositiveButton("保存", (dialog, which) -> {
                            int selectedCoursePosition = spCourse.getSelectedItemPosition();
                            String scoreStr = etScore.getText().toString().trim();
                            String newExamType = etExamType.getText().toString().trim();
                            String newExamDate = etExamDate.getText().toString().trim();

                            if (validateGradeInput(selectedCoursePosition, scoreStr, newExamType, newExamDate)) {
                                try {
                                    double newScore = Double.parseDouble(scoreStr);
                                    int newCourseId = courseIds.get(selectedCoursePosition);

                                    // 更新成绩到数据库
                                    ContentValues values = new ContentValues();
                                    values.put("course_id", newCourseId);
                                    values.put("score", newScore);
                                    values.put("exam_type", newExamType);
                                    values.put("exam_date", newExamDate);

                                    dbHelper.update("grade", values, "id = ?", new String[]{String.valueOf(gradeId)});
                                    Toast.makeText(GradeActivity.this, "成绩更新成功", Toast.LENGTH_SHORT).show();
                                    loadGrades();
                                    updateGradeStats();
                                } catch (NumberFormatException e) {
                                    Toast.makeText(GradeActivity.this, "请输入有效的成绩", Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .setNegativeButton("取消", null);
                builder.show();
            } else {
                cursor.close();
            }
        } else if (cursor != null) {
            cursor.close();
        }
    }

    /**
     * 验证成绩输入
     * @param coursePosition 课程位置
     * @param scoreStr 成绩字符串
     * @param examType 考试类型
     * @param examDate 考试日期
     * @return 是否验证通过
     */
    private boolean validateGradeInput(int coursePosition, String scoreStr, String examType, String examDate) {
        if (coursePosition < 0) {
            Toast.makeText(this, "请选择课程", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (scoreStr.isEmpty()) {
            Toast.makeText(this, "请输入成绩", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (examType.isEmpty()) {
            Toast.makeText(this, "请输入考试类型", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (examDate.isEmpty()) {
            Toast.makeText(this, "请输入考试日期", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        try {
            double score = Double.parseDouble(scoreStr);
            if (score < MIN_SCORE || score > MAX_SCORE) {
                Toast.makeText(this, "成绩必须在" + MIN_SCORE + "-" + MAX_SCORE + "之间", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "请输入有效的成绩", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        return true;
    }

    /**
     * 删除成绩
     * @param position 成绩位置
     */
    private void deleteGrade(int position) {
        // 删除成绩
        if (position < 0 || position >= gradeIdList.size()) {
            return;
        }
        
        int gradeId = gradeIdList.get(position);
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("确认删除")
                .setMessage("确定要删除这个成绩吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    // 从数据库中删除成绩
                    dbHelper.delete("grade", "id = ?", new String[]{String.valueOf(gradeId)});
                    Toast.makeText(GradeActivity.this, "成绩已删除", Toast.LENGTH_SHORT).show();
                    loadGrades(); // 重新加载成绩列表
                    updateGradeStats(); // 重新更新统计数据
                })
                .setNegativeButton("取消", null);
        builder.show();
    }

    /**
     * 分享成绩
     * @param position 成绩位置
     */
    private void shareGrade(int position) {
        // 分享成绩
        if (position < 0 || position >= gradeList.size()) {
            return;
        }
        
        android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, gradeList.get(position));
        startActivity(android.content.Intent.createChooser(shareIntent, "分享成绩"));
    }

    /**
     * 显示成绩分析
     */
    private void showGradeAnalysis() {
        try {
            // 显示加载对话框
            android.app.AlertDialog loadingDialog = showLoadingDialog();
            
            // 在后台线程中执行分析
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final Cursor cursor;
                        // 根据用户类型查询不同的成绩数据
                        if (userType == 0) {
                            // 学生：查询自己的成绩
                            cursor = dbHelper.getGradesByStudentId(userId);
                        } else {
                            // 教师和管理员：查询所有学生的成绩
                            cursor = dbHelper.query("grade", new String[]{"score", "course_id"}, null, null, null, null, null);
                        }
                        
                        if (cursor == null) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (loadingDialog != null && loadingDialog.isShowing()) {
                                        loadingDialog.dismiss();
                                    }
                                    Toast.makeText(GradeActivity.this, "暂无成绩数据，请先添加成绩", Toast.LENGTH_LONG).show();
                                }
                            });
                            return;
                        }
                        
                        if (!cursor.moveToFirst()) {
                            cursor.close();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (loadingDialog != null && loadingDialog.isShowing()) {
                                        loadingDialog.dismiss();
                                    }
                                    Toast.makeText(GradeActivity.this, "暂无成绩数据，请先添加成绩", Toast.LENGTH_LONG).show();
                                }
                            });
                            return;
                        }
                        
                        // 重置cursor位置
                        cursor.moveToFirst();
                        final Cursor finalCursor = cursor;
                        
                        // 执行分析
                        GradeAnalysisHelper.analyzeGradesAsync(finalCursor, new GradeAnalysisHelper.GradeAnalysisCallback() {
                            @Override
                            public String getCourseName(int courseId) {
                                return GradeActivity.this.getCourseName(courseId);
                            }
                        }, new GradeAnalysisHelper.AnalysisResultCallback() {
                            @Override
                            public void onAnalysisComplete(GradeAnalysisHelper.GradeAnalysisResult result) {
                                if (loadingDialog != null && loadingDialog.isShowing()) {
                                    loadingDialog.dismiss();
                                }
                                showAnalysisResult(result);
                            }
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Error in grade analysis", e);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (loadingDialog != null && loadingDialog.isShowing()) {
                                    loadingDialog.dismiss();
                                }
                                Toast.makeText(GradeActivity.this, "成绩分析失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
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

    /**
     * 显示加载对话框
     * @return 加载对话框实例
     */
    private android.app.AlertDialog showLoadingDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("分析中");
        builder.setMessage("正在分析成绩数据，请稍候...");
        builder.setCancelable(false);
        return builder.show();
    }

    /**
     * 显示分析结果
     * @param result 分析结果
     */
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
            LineChart lineChart = dialogView.findViewById(R.id.line_chart);
            BarChart barChart = dialogView.findViewById(R.id.bar_chart);
            TextView tvAnalysisText = dialogView.findViewById(R.id.tv_analysis_text);

            if (tvAnalysisText == null) {
                Toast.makeText(this, "分析文本视图为空", Toast.LENGTH_SHORT).show();
                return;
            }

            StringBuilder analysis = new StringBuilder("成绩分析报告：\n\n");
            analysis.append("分数段分布：\n");
            analysis.append("优秀 (90-100)：").append(result.excellent).append("门\n");
            analysis.append("良好 (80-89)：").append(result.good).append("门\n");
            analysis.append("中等 (70-79)：").append(result.average).append("门\n");
            analysis.append("及格 (60-69)：").append(result.pass).append("门\n");
            analysis.append("不及格 (0-59)：").append(result.fail).append("门\n\n");

            analysis.append("及格率：").append(String.format(Locale.ROOT, "%.2f", result.passRate)).append("%\n");
            analysis.append("优秀率：").append(String.format(Locale.ROOT, "%.2f", result.excellentRate)).append("%\n");
            analysis.append("平均成绩：").append(String.format(Locale.ROOT, "%.2f", result.averageScore)).append("分\n\n");

            if (result.weakSubjects != null && !result.weakSubjects.isEmpty()) {
                analysis.append("薄弱科目分析：\n");
                for (int i = 0; i < result.weakSubjects.size(); i++) {
                    String subject = result.weakSubjects.get(i);
                    Double score = result.weakScores.get(i);
                    analysis.append(subject).append(" (").append(String.format(Locale.ROOT, "%.1f", score != null ? score : 0)).append("分)").append("\n");
                }
                analysis.append("\n");

                analysis.append("复习建议：\n");
                for (String subject : result.weakSubjects) {
                    analysis.append(getReviewSuggestion(subject)).append("\n");
                }
            } else {
                analysis.append("薄弱科目：无\n");
                analysis.append("\n复习建议：继续保持良好的学习状态！\n");
            }

            tvAnalysisText.setText(analysis.toString());

            // 设置图表
            try {
                if (pieChart != null) {
                    GradeAnalysisHelper.setupPieChart(pieChart, result);
                }
                if (lineChart != null && result.courseScoreMap != null) {
                    GradeAnalysisHelper.setupLineChart(lineChart, result.courseScoreMap);
                }
                if (barChart != null && result.courseNames != null && result.courseScores != null) {
                    GradeAnalysisHelper.setupBarChart(barChart, result.courseNames, result.courseScores);
                }
            } catch (Exception chartEx) {
                Log.e(TAG, "Error setting up charts", chartEx);
                Toast.makeText(this, "图表初始化失败，但分析数据已生成", Toast.LENGTH_SHORT).show();
            }

            // 显示对话框
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("成绩分析报告");
            builder.setView(dialogView);
            builder.setPositiveButton("确定", null);
            
            android.app.AlertDialog dialog = builder.create();
            dialog.show();
            
            // 设置对话框大小
            if (dialog.getWindow() != null) {
                int width = (int) (getResources().getDisplayMetrics().widthPixels * DIALOG_WIDTH_RATIO);
                int height = (int) (getResources().getDisplayMetrics().heightPixels * DIALOG_HEIGHT_RATIO);
                dialog.getWindow().setLayout(width, height);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing analysis result", e);
            Toast.makeText(this, "显示分析结果失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 获取复习建议
     * @param subject 科目
     * @return 复习建议
     */
    private String getReviewSuggestion(String subject) {
        // 根据科目提供复习建议
        switch (subject) {
            case "高等数学":
                return "- 高等数学：重点复习微积分和线性代数部分，多做练习题，理解基本概念和定理。";
            case "大学英语":
                return "- 大学英语：加强词汇积累，多听多说，提高阅读理解能力，定期做模拟题。";
            case "数据结构":
                return "- 数据结构：理解各种数据结构的原理和应用，多做算法题，提高编程能力。";
            case "操作系统":
                return "- 操作系统：重点理解进程管理、内存管理和文件系统，结合实际案例学习。";
            case "计算机网络":
                return "- 计算机网络：掌握网络协议栈，理解TCP/IP模型，多做网络配置实验。";
            case "数据库":
                return "- 数据库：学习SQL语句，理解数据库设计原则，多做数据库设计和优化练习。";
            default:
                return "- " + subject + "：制定合理的学习计划，重点掌握核心知识点，多做练习巩固。";
        }
    }

    /**
     * 按课程筛选成绩
     * @param courseName 课程名称
     */
    private void filterGrades(String courseName) {
        // 按课程筛选成绩
        gradeList.clear();
        gradeIdList.clear();
        Cursor cursor = dbHelper.getGradesByStudentId(userId);
        if (cursor != null) {
            int idIndex = cursor.getColumnIndex("id");
            int courseIdIndex = cursor.getColumnIndex("course_id");
            int scoreIndex = cursor.getColumnIndex("score");
            int examTypeIndex = cursor.getColumnIndex("exam_type");
            int examDateIndex = cursor.getColumnIndex("exam_date");
            int commentIndex = cursor.getColumnIndex("comment");
            
            while (cursor.moveToNext() && idIndex != -1 && courseIdIndex != -1 && scoreIndex != -1 && examTypeIndex != -1 && examDateIndex != -1 && commentIndex != -1) {
                int gradeId = cursor.getInt(idIndex);
                int courseId = cursor.getInt(courseIdIndex);
                String currentCourseName = getCourseName(courseId);
                double score = cursor.getDouble(scoreIndex);
                String examType = cursor.getString(examTypeIndex);
                String examDate = cursor.getString(examDateIndex);
                String comment = cursor.getString(commentIndex);

                // 应用筛选条件
                if (courseName.equals("全部课程") || courseName.equals(currentCourseName)) {
                    String gradeInfo = currentCourseName + "\n" +
                            "考试类型：" + examType + "\n" +
                            "考试日期：" + examDate + "\n" +
                            "成绩：" + String.format(Locale.ROOT, "%.1f", score) + "分" + (comment.isEmpty() ? "" : "\n备注：" + comment);
                    gradeList.add(gradeInfo);
                    gradeIdList.add(gradeId);
                }
            }
            cursor.close();
        }
        gradeAdapter.notifyDataSetChanged();
    }

    /**
     * 销毁活动
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}
