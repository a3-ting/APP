package com.example.myapplication.ui.ai;

import androidx.appcompat.app.AppCompatActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.api.SiliconFlowService;
import com.example.myapplication.database.DBHelper;
import com.example.myapplication.utils.DialogUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AIChatActivity extends AppCompatActivity {

    private TextView tvChatHistory;
    private EditText etMessage;
    private Button btnSend;
    private ScrollView scrollView;
    private SiliconFlowService siliconFlowService;
    private DBHelper dbHelper;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        userId = getIntent().getIntExtra("user_id", -1);
        if (userId == -1) {
            Toast.makeText(this, "用户信息错误", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        try {
            initViews();
            initService();
            initDatabase();
            setupListeners();
            
            // 添加欢迎消息
            addMessageToHistory("AI: 您好！我是您的AI学习助手。我可以帮您查询成绩、课程、作业等信息，也可以帮您添加或删除课程和作业。请告诉我有什么可以帮助您的？");
        } catch (Exception e) {
            android.util.Log.e("AIChatActivity", "Error initializing", e);
            Toast.makeText(this, "初始化失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        tvChatHistory = findViewById(R.id.tv_chat_history);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);
        scrollView = findViewById(R.id.scroll_view);
        
        if (tvChatHistory == null || etMessage == null || btnSend == null || scrollView == null) {
            throw new RuntimeException("布局中的必要控件未找到");
        }
    }

    private void initService() {
        try {
            siliconFlowService = new SiliconFlowService();
            android.util.Log.d("AIChatActivity", "SiliconFlowService initialized");
        } catch (Exception e) {
            android.util.Log.e("AIChatActivity", "Error initializing service", e);
            throw new RuntimeException("AI服务初始化失败");
        }
    }

    private void initDatabase() {
        try {
            dbHelper = new DBHelper(this);
            android.util.Log.d("AIChatActivity", "DBHelper initialized");
        } catch (Exception e) {
            android.util.Log.e("AIChatActivity", "Error initializing database", e);
            throw new RuntimeException("数据库初始化失败");
        }
    }

    private void setupListeners() {
        if (btnSend == null) {
            throw new RuntimeException("发送按钮未初始化");
        }
        
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (etMessage == null) {
                    Toast.makeText(AIChatActivity.this, "输入框未初始化", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                String message = etMessage.getText().toString().trim();
                if (!message.isEmpty()) {
                    sendMessage(message);
                } else {
                    Toast.makeText(AIChatActivity.this, "请输入消息", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void sendMessage(String message) {
        try {
            // 添加用户消息到聊天历史
            addMessageToHistory("你: " + message);
            
            if (etMessage != null) {
                etMessage.setText("");
            }

            // 禁用发送按钮
            if (btnSend != null) {
                btnSend.setEnabled(false);
            }
            addMessageToHistory("AI: 正在思考...");

            // 异步处理意图识别和数据查询
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String userMessage = message;
                        // 意图识别
                        String intent = identifyIntent(userMessage);
                        String prompt = userMessage;
                        boolean isLocalOperation = false;
                        final String[] localResponse = {""};

                        // 根据意图处理
                        if (intent.equals("grade")) {
                            // 查询成绩数据
                            String gradeData = getGradeData();
                            // 构造包含数据的提示词
                            prompt = "用户询问成绩情况，以下是用户的成绩数据：\n" + gradeData + "\n请根据这些数据回答用户的问题，保持回答友好自然。\n用户的问题：" + userMessage;
                        } else if (intent.equals("course")) {
                            // 查询课程数据
                            String courseData = getCourseData();
                            // 构造包含数据的提示词
                            prompt = "用户询问课程情况，以下是用户的课程数据：\n" + courseData + "\n请根据这些数据回答用户的问题，保持回答友好自然。\n用户的问题：" + userMessage;
                        } else if (intent.equals("add_course")) {
                            // 提取课程信息并添加课程
                            isLocalOperation = true;
                            localResponse[0] = addCourse(userMessage);
                        } else if (intent.equals("delete_course")) {
                            // 提取课程信息并删除课程
                            isLocalOperation = true;
                            localResponse[0] = deleteCourse(userMessage);
                        } else if (intent.equals("assignment")) {
                            // 查询作业数据
                            String assignmentData = getAssignmentData();
                            // 构造包含数据的提示词
                            prompt = "用户询问作业情况，以下是用户的作业数据：\n" + assignmentData + "\n请根据这些数据回答用户的问题，保持回答友好自然。\n用户的问题：" + userMessage;
                        } else if (intent.equals("add_assignment")) {
                            // 提取作业信息并添加作业
                            isLocalOperation = true;
                            localResponse[0] = addAssignment(userMessage);
                        } else if (intent.equals("delete_assignment")) {
                            // 提取作业信息并删除作业
                            isLocalOperation = true;
                            localResponse[0] = deleteAssignment(userMessage);
                        }

                        // 处理响应
                        if (isLocalOperation) {
                            // 本地操作直接返回结果
                            final String response = localResponse[0];
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // 移除"思考中..."消息
                                    removeLastAIMessage();
                                    // 添加AI回复
                                    addMessageToHistory("AI: " + response);
                                    if (btnSend != null) {
                                        btnSend.setEnabled(true);
                                    }
                                }
                            });
                        } else {
                            // 使用异步方法调用API
                            if (siliconFlowService != null) {
                                siliconFlowService.getChatCompletionAsync(prompt, SiliconFlowService.MODEL_QWEN, new SiliconFlowService.Callback() {
                                    @Override
                                    public void onResponse(String response) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                // 移除"思考中..."消息
                                                removeLastAIMessage();
                                                // 添加AI回复
                                                addMessageToHistory("AI: " + response);
                                                if (btnSend != null) {
                                                    btnSend.setEnabled(true);
                                                }
                                            }
                                        });
                                    }
                                });
                            } else {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        removeLastAIMessage();
                                        addMessageToHistory("AI: AI服务未初始化，请重启应用");
                                        if (btnSend != null) {
                                            btnSend.setEnabled(true);
                                        }
                                    }
                                });
                            }
                        }
                    } catch (Exception e) {
                        android.util.Log.e("AIChatActivity", "Error in sendMessage thread", e);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                removeLastAIMessage();
                                addMessageToHistory("AI: 处理消息时发生错误: " + e.getMessage());
                                if (btnSend != null) {
                                    btnSend.setEnabled(true);
                                }
                            }
                        });
                    }
                }
            }).start();
        } catch (Exception e) {
            android.util.Log.e("AIChatActivity", "Error sending message", e);
            Toast.makeText(this, "发送消息失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            if (btnSend != null) {
                btnSend.setEnabled(true);
            }
        }
    }
    
    private void removeLastAIMessage() {
        if (tvChatHistory != null) {
            String history = tvChatHistory.getText().toString();
            int lastIndex = history.lastIndexOf("AI: 正在思考...");
            if (lastIndex != -1) {
                history = history.substring(0, lastIndex);
                tvChatHistory.setText(history);
            }
        }
    }

    private String identifyIntent(String message) {
        // 简单的意图识别，基于关键词匹配
        message = message.toLowerCase(Locale.ROOT);
        if (message.contains("成绩") || message.contains("分数") || message.contains("gpa")) {
            return "grade";
        } else if (message.contains("课程") || message.contains("上课") || message.contains("课表")) {
            if (message.contains("添加") || message.contains("新增") || message.contains("创建")) {
                return "add_course";
            } else if (message.contains("删除") || message.contains("移除")) {
                return "delete_course";
            } else {
                return "course";
            }
        } else if (message.contains("作业") || message.contains("任务") || message.contains("截止")) {
            if (message.contains("添加") || message.contains("新增") || message.contains("创建")) {
                return "add_assignment";
            } else if (message.contains("删除") || message.contains("移除")) {
                return "delete_assignment";
            } else {
                return "assignment";
            }
        } else {
            return "general";
        }
    }

    private String getGradeData() {
        // 查询用户的成绩数据
        StringBuilder gradeData = new StringBuilder();
        try {
            android.database.Cursor cursor = dbHelper.getGradesByStudentId(userId);
            if (cursor != null) {
                int courseIdIndex = cursor.getColumnIndex("course_id");
                int scoreIndex = cursor.getColumnIndex("score");
                int examTypeIndex = cursor.getColumnIndex("exam_type");
                int examDateIndex = cursor.getColumnIndex("exam_date");
                
                while (cursor.moveToNext()) {
                    if (courseIdIndex != -1 && scoreIndex != -1 && examTypeIndex != -1 && examDateIndex != -1) {
                        int courseId = cursor.getInt(courseIdIndex);
                        double score = cursor.getDouble(scoreIndex);
                        String examType = cursor.getString(examTypeIndex);
                        String examDate = cursor.getString(examDateIndex);
                        
                        // 获取课程名称
                        android.database.Cursor courseCursor = dbHelper.query("course", new String[]{"course_name"}, "id = ?", new String[]{String.valueOf(courseId)}, null, null, null);
                        String courseName = "未知课程";
                        if (courseCursor != null && courseCursor.moveToFirst()) {
                            int courseNameIndex = courseCursor.getColumnIndex("course_name");
                            if (courseNameIndex != -1) {
                                courseName = courseCursor.getString(courseNameIndex);
                            }
                            courseCursor.close();
                        }
                        
                        gradeData.append("课程：").append(courseName).append("，考试类型：").append(examType).append("，日期：").append(examDate).append("，成绩：").append(score).append("\n");
                    }
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return gradeData.toString();
    }

    private String getCourseData() {
        // 查询用户的课程数据
        StringBuilder courseData = new StringBuilder();
        try {
            android.database.Cursor cursor = dbHelper.getCoursesByUserId(userId);
            if (cursor != null) {
                int courseNameIndex = cursor.getColumnIndex("course_name");
                int teacherNameIndex = cursor.getColumnIndex("teacher_name");
                int classroomIndex = cursor.getColumnIndex("classroom");
                int dayOfWeekIndex = cursor.getColumnIndex("day_of_week");
                int startTimeIndex = cursor.getColumnIndex("start_time");
                int endTimeIndex = cursor.getColumnIndex("end_time");
                
                while (cursor.moveToNext()) {
                    if (courseNameIndex != -1 && teacherNameIndex != -1 && classroomIndex != -1 && dayOfWeekIndex != -1 && startTimeIndex != -1 && endTimeIndex != -1) {
                        String courseName = cursor.getString(courseNameIndex);
                        String teacherName = cursor.getString(teacherNameIndex);
                        String classroom = cursor.getString(classroomIndex);
                        int dayOfWeek = cursor.getInt(dayOfWeekIndex);
                        String startTime = cursor.getString(startTimeIndex);
                        String endTime = cursor.getString(endTimeIndex);
                        
                        String dayOfWeekStr = getDayOfWeekStr(dayOfWeek);
                        courseData.append("课程：").append(courseName).append("，教师：").append(teacherName).append("，教室：").append(classroom).append("，时间：").append(dayOfWeekStr).append(" " ).append(startTime).append("-").append(endTime).append("\n");
                    }
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return courseData.toString();
    }

    private String getAssignmentData() {
        // 查询用户的作业数据
        StringBuilder assignmentData = new StringBuilder();
        try {
            android.database.Cursor cursor = dbHelper.getAssignmentsByUserId(userId);
            if (cursor != null) {
                int titleIndex = cursor.getColumnIndex("title");
                int descriptionIndex = cursor.getColumnIndex("description");
                int dueDateIndex = cursor.getColumnIndex("due_date");
                int statusIndex = cursor.getColumnIndex("status");
                
                while (cursor.moveToNext()) {
                    if (titleIndex != -1 && dueDateIndex != -1 && statusIndex != -1) {
                        String title = cursor.getString(titleIndex);
                        String description = cursor.getString(descriptionIndex);
                        String dueDate = cursor.getString(dueDateIndex);
                        int status = cursor.getInt(statusIndex);
                        
                        String statusStr = getStatusStr(status);
                        assignmentData.append("标题：").append(title).append("，截止日期：").append(dueDate).append("，状态：").append(statusStr).append("\n");
                    }
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return assignmentData.toString();
    }

    private String getDayOfWeekStr(int dayOfWeek) {
        switch (dayOfWeek) {
            case 1: return "周一";
            case 2: return "周二";
            case 3: return "周三";
            case 4: return "周四";
            case 5: return "周五";
            case 6: return "周六";
            case 7: return "周日";
            default: return "未知";
        }
    }

    private String getStatusStr(int status) {
        switch (status) {
            case 0: return "未开始";
            case 1: return "进行中";
            case 2: return "已完成";
            case 3: return "已逾期";
            default: return "未知";
        }
    }

    private String addCourse(String message) {
        // 简单的课程信息提取，实际项目中可以使用更复杂的NLP技术
        try {
            // 提取课程名称
            String courseName = extractCourseName(message);
            if (courseName.isEmpty()) {
                return "请提供课程名称";
            }
            
            // 提取教师姓名
            String teacherName = extractTeacherName(message);
            
            // 提取教室
            String classroom = extractClassroom(message);
            
            // 提取星期几
            int dayOfWeek = extractDayOfWeek(message);
            if (dayOfWeek == -1) {
                return "请提供上课星期";
            }
            
            // 提取开始和结束时间
            String[] times = extractTime(message);
            if (times == null || times.length != 2) {
                return "请提供上课时间，格式为'8:00-9:40'";
            }
            
            // 添加课程到数据库
            android.content.ContentValues values = new android.content.ContentValues();
            values.put("user_id", userId);
            values.put("course_name", courseName);
            values.put("teacher_name", teacherName);
            values.put("classroom", classroom);
            values.put("start_time", times[0]);
            values.put("end_time", times[1]);
            values.put("day_of_week", dayOfWeek);
            values.put("is_favorite", 0);
            
            long result = dbHelper.insertCourse(values);
            if (result > 0) {
                return "课程添加成功：" + courseName;
            } else {
                return "课程添加失败";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "添加课程时出错";
        }
    }

    private String deleteCourse(String message) {
        try {
            // 提取课程名称
            String courseName = extractCourseName(message);
            if (courseName.isEmpty()) {
                return "请提供要删除的课程名称";
            }
            
            // 查询课程
            android.database.Cursor cursor = dbHelper.query("course", new String[]{"id"}, "user_id = ? AND course_name LIKE ?", 
                    new String[]{String.valueOf(userId), "%" + courseName + "%"}, null, null, null);
            
            if (cursor != null && cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndex("id");
                if (idIndex != -1) {
                    int courseId = cursor.getInt(idIndex);
                    cursor.close();
                    
                    // 删除课程
                    int result = dbHelper.delete("course", "id = ?", new String[]{String.valueOf(courseId)});
                    if (result > 0) {
                        return "课程删除成功：" + courseName;
                    } else {
                        return "课程删除失败";
                    }
                } else {
                    cursor.close();
                    return "课程信息错误";
                }
            } else {
                if (cursor != null) cursor.close();
                return "未找到课程：" + courseName;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "删除课程时出错";
        }
    }

    private String addAssignment(String message) {
        try {
            // 提取作业标题
            String title = extractAssignmentTitle(message);
            if (title.isEmpty()) {
                return "请提供作业标题";
            }
            
            // 提取截止日期
            String dueDate = extractDueDate(message);
            if (dueDate.isEmpty()) {
                return "请提供截止日期";
            }
            
            // 提取描述
            String description = extractAssignmentDescription(message);
            
            // 添加作业到数据库
            android.content.ContentValues values = new android.content.ContentValues();
            values.put("user_id", userId);
            values.put("title", title);
            values.put("description", description);
            values.put("due_date", dueDate);
            values.put("status", 0); // 未开始
            values.put("priority", 1); // 中等优先级
            
            long result = dbHelper.insertAssignment(values);
            if (result > 0) {
                return "作业添加成功：" + title;
            } else {
                return "作业添加失败";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "添加作业时出错";
        }
    }

    private String deleteAssignment(String message) {
        try {
            // 提取作业标题
            String title = extractAssignmentTitle(message);
            if (title.isEmpty()) {
                return "请提供要删除的作业标题";
            }
            
            // 查询作业
            android.database.Cursor cursor = dbHelper.query("assignment", new String[]{"id"}, "user_id = ? AND title LIKE ?", 
                    new String[]{String.valueOf(userId), "%" + title + "%"}, null, null, null);
            
            if (cursor != null && cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndex("id");
                if (idIndex != -1) {
                    int assignmentId = cursor.getInt(idIndex);
                    cursor.close();
                    
                    // 删除作业
                    int result = dbHelper.delete("assignment", "id = ?", new String[]{String.valueOf(assignmentId)});
                    if (result > 0) {
                        return "作业删除成功：" + title;
                    } else {
                        return "作业删除失败";
                    }
                } else {
                    cursor.close();
                    return "作业信息错误";
                }
            } else {
                if (cursor != null) cursor.close();
                return "未找到作业：" + title;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "删除作业时出错";
        }
    }

    // 辅助方法：提取课程名称
    private String extractCourseName(String message) {
        // 简单的提取逻辑，实际项目中可以使用更复杂的NLP技术
        message = message.replace("添加", "").replace("新增", "").replace("创建", "").replace("课程", "").trim();
        // 提取第一个连续的中文字符串作为课程名称
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("[\u4e00-\u9fa5]+");
        java.util.regex.Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return matcher.group();
        }
        return "";
    }

    // 辅助方法：提取教师姓名
    private String extractTeacherName(String message) {
        // 简单的提取逻辑
        if (message.contains("老师") || message.contains("教授")) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("[\u4e00-\u9fa5]+(老师|教授)");
            java.util.regex.Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                return matcher.group();
            }
        }
        return "";
    }

    // 辅助方法：提取教室
    private String extractClassroom(String message) {
        // 简单的提取逻辑
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("[教][0-9]+-[0-9]+");
        java.util.regex.Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return matcher.group();
        }
        return "";
    }

    // 辅助方法：提取星期几
    private int extractDayOfWeek(String message) {
        message = message.toLowerCase(Locale.ROOT);
        if (message.contains("周一") || message.contains("星期一")) return 1;
        if (message.contains("周二") || message.contains("星期二")) return 2;
        if (message.contains("周三") || message.contains("星期三")) return 3;
        if (message.contains("周四") || message.contains("星期四")) return 4;
        if (message.contains("周五") || message.contains("星期五")) return 5;
        if (message.contains("周六") || message.contains("星期六")) return 6;
        if (message.contains("周日") || message.contains("星期日")) return 7;
        return -1;
    }

    // 辅助方法：提取时间
    private String[] extractTime(String message) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("([0-9]+:[0-9]+)-([0-9]+:[0-9]+)");
        java.util.regex.Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return new String[]{matcher.group(1), matcher.group(2)};
        }
        return null;
    }

    // 辅助方法：提取作业标题
    private String extractAssignmentTitle(String message) {
        message = message.replace("添加", "").replace("新增", "").replace("创建", "").replace("作业", "").trim();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("[\u4e00-\u9fa5]+[0-9]*");
        java.util.regex.Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return matcher.group();
        }
        return "";
    }

    // 辅助方法：提取截止日期
    private String extractDueDate(String message) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d{4}-\\d{2}-\\d{2}|\\d{2}/\\d{2}/\\d{4}|\\d{2}/\\d{2})");
        java.util.regex.Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return matcher.group();
        }
        return "";
    }

    // 辅助方法：提取作业描述
    private String extractAssignmentDescription(String message) {
        // 简单的提取逻辑
        return "";
    }

    private void addMessageToHistory(String message) {
        String currentHistory = tvChatHistory.getText().toString();
        if (!currentHistory.isEmpty()) {
            currentHistory += "\n\n";
        }
        tvChatHistory.setText(currentHistory + message);
        // 滚动到底部
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 清除对话记忆
        if (siliconFlowService != null) {
            siliconFlowService.clearConversationHistory();
        }
        // 关闭数据库连接
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}
