package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.database.DBHelper;

import java.util.ArrayList;
import java.util.List;

public class LearningProgressActivity extends AppCompatActivity {

    private ListView lvStudents;
    private Spinner spCourse;
    private Button btnBack;
    private DBHelper dbHelper;
    private int teacherId;
    private List<String> studentList;
    private List<Integer> studentIdList;
    private List<String> courseList;
    private List<Integer> courseIdList;
    private ArrayAdapter<String> studentAdapter;
    private ArrayAdapter<String> courseAdapter;
    private int selectedCourseId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_learning_progress);

        teacherId = getIntent().getIntExtra("user_id", -1);
        int userType = getIntent().getIntExtra("user_type", -1);
        
        if (teacherId == -1 || (userType != 1 && userType != 2)) {
            Toast.makeText(this, "权限不足，只有教师和管理员可以访问", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        initDatabase();
        loadCourses();
        setupListeners();
    }

    private void initViews() {
        lvStudents = findViewById(R.id.lv_students);
        spCourse = findViewById(R.id.sp_course);
        btnBack = findViewById(R.id.btn_back);

        studentList = new ArrayList<>();
        studentIdList = new ArrayList<>();
        courseList = new ArrayList<>();
        courseIdList = new ArrayList<>();
        
        studentAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, studentList);
        lvStudents.setAdapter(studentAdapter);
    }

    private void initDatabase() {
        dbHelper = new DBHelper(this);
    }

    private void loadCourses() {
        courseList.clear();
        courseIdList.clear();
        courseList.add("全部课程");
        courseIdList.add(-1);

        Cursor cursor = dbHelper.query("course", new String[]{"id", "course_name"}, null, null, null, null, "course_name ASC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int idIndex = cursor.getColumnIndex("id");
                int nameIndex = cursor.getColumnIndex("course_name");
                if (idIndex != -1 && nameIndex != -1) {
                    courseIdList.add(cursor.getInt(idIndex));
                    courseList.add(cursor.getString(nameIndex));
                }
            }
            cursor.close();
        }

        courseAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, courseList);
        courseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCourse.setAdapter(courseAdapter);
    }

    private void loadStudentsWithProgress() {
        studentList.clear();
        studentIdList.clear();

        Cursor studentCursor = dbHelper.query("user", new String[]{"id", "username", "name"}, 
                "type = ?", new String[]{"0"}, null, null, "username ASC");
        if (studentCursor != null) {
            while (studentCursor.moveToNext()) {
                int userIdIndex = studentCursor.getColumnIndex("id");
                int usernameIndex = studentCursor.getColumnIndex("username");
                int nameIndex = studentCursor.getColumnIndex("name");
                
                if (userIdIndex != -1 && usernameIndex != -1) {
                    int studentId = studentCursor.getInt(userIdIndex);
                    String username = studentCursor.getString(usernameIndex);
                    String name = nameIndex != -1 ? studentCursor.getString(nameIndex) : "未知";
                    
                    String progressInfo = getStudentProgressInfo(studentId);
                    studentList.add(name + " (" + username + ")\n" + progressInfo);
                    studentIdList.add(studentId);
                }
            }
            studentCursor.close();
        }
        
        studentAdapter.notifyDataSetChanged();
    }

    private String getStudentProgressInfo(int studentId) {
        StringBuilder info = new StringBuilder();
        
        int completedAssignments = 0;
        int totalAssignments = 0;
        
        String courseFilter = selectedCourseId > 0 ? "AND course_id = " + selectedCourseId : "";
        
        Cursor progressCursor = dbHelper.query("progress", 
                new String[]{"completed_assignments", "total_assignments"}, 
                "student_id = ?" + courseFilter, 
                new String[]{String.valueOf(studentId)}, 
                null, null, null);
        
        if (progressCursor != null) {
            while (progressCursor.moveToNext()) {
                int completedIndex = progressCursor.getColumnIndex("completed_assignments");
                int totalIndex = progressCursor.getColumnIndex("total_assignments");
                if (completedIndex != -1 && totalIndex != -1) {
                    completedAssignments += progressCursor.getInt(completedIndex);
                    totalAssignments += progressCursor.getInt(totalIndex);
                }
            }
            progressCursor.close();
        }
        
        if (totalAssignments > 0) {
            int percentage = (completedAssignments * 100) / totalAssignments;
            info.append("作业完成率: ").append(percentage).append("% (").append(completedAssignments).append("/").append(totalAssignments).append(")");
        } else {
            info.append("暂无作业记录");
        }
        
        return info.toString();
    }

    private void setupListeners() {
        spCourse.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCourseId = courseIdList.get(position);
                loadStudentsWithProgress();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        lvStudents.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showStudentDetail(position);
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void showStudentDetail(int position) {
        int studentId = studentIdList.get(position);
        
        Cursor cursor = dbHelper.query("user", null, "id = ?", new String[]{String.valueOf(studentId)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            String username = "";
            String name = "";
            String className = "";
            String major = "";
            
            int usernameIndex = cursor.getColumnIndex("username");
            int nameIndex = cursor.getColumnIndex("name");
            int classNameIndex = cursor.getColumnIndex("class_name");
            int majorIndex = cursor.getColumnIndex("major");
            
            if (usernameIndex != -1) username = cursor.getString(usernameIndex);
            if (nameIndex != -1) name = cursor.getString(nameIndex);
            if (classNameIndex != -1) className = cursor.getString(classNameIndex);
            if (majorIndex != -1) major = cursor.getString(majorIndex);
            
            cursor.close();
            
            String progressInfo = getStudentProgressInfo(studentId);
            
            String detail = "用户名：" + username + "\n" +
                    "姓名：" + name + "\n" +
                    "班级：" + className + "\n" +
                    "专业：" + major + "\n\n" +
                    "学习进度：\n" + progressInfo;

            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("学生详情")
                    .setMessage(detail)
                    .setPositiveButton("确定", null);
            builder.show();
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