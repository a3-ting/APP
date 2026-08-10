package com.example.myapplication.ui.main;
import com.example.myapplication.service.ReminderService;
import com.example.myapplication.ui.ai.AIChatActivity;
import com.example.myapplication.ui.assignment.AssignmentActivity;
import com.example.myapplication.ui.auth.LoginActivity;
import com.example.myapplication.ui.course.CourseActivity;
import com.example.myapplication.ui.course.TeacherCourseActivity;
import com.example.myapplication.ui.grade.GradeActivity;
import com.example.myapplication.ui.grade.TeacherGradeActivity;
import com.example.myapplication.ui.notice.NoticeActivity;
import com.example.myapplication.ui.reminder.ReminderActivity;
import com.example.myapplication.ui.settings.SystemSettingsActivity;
import com.example.myapplication.ui.study.StudyPlanActivity;
import com.example.myapplication.ui.user.ProfileActivity;
import com.example.myapplication.ui.user.StudentManageActivity;
import com.example.myapplication.ui.user.TeacherManageActivity;
import com.example.myapplication.ui.user.UserManageActivity;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.myapplication.utils.ToastUtils;
import com.example.myapplication.utils.BackupUtils;
import com.example.myapplication.utils.LogUtils;
import com.example.myapplication.utils.NewDialogUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import android.app.AlertDialog;

public class MainActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private Button btnCourse, btnAssignment, btnStudyPlan, btnProfile, btnLogout;
    private Button btnStudentManage, btnTeacherCourse, btnGradeManage;
    private Button btnAIChat, btnGrade, btnReminder, btnSystemConfig, btnNotice;
    private LinearLayout layoutStudentSection, layoutStudentButtons;
    private LinearLayout layoutTeacherSection, layoutTeacherButtons;
    private int userId;
    private int userType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            userId = getIntent().getIntExtra("user_id", -1);
            userType = getIntent().getIntExtra("user_type", -1);

            if (userId == -1 || userType == -1) {
                ToastUtils.showShort(this, "用户信息错误");
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
                return;
            }

            initViews();
            setupUIByUserType();
            setupListeners();
            
            scheduleRemindersSafely();
        } catch (Exception e) {
            LogUtils.logError(this, "MainActivity", "onCreate error", e);
            ToastUtils.showError(this, "应用启动失败: " + e.getMessage());
            finish();
        }
    }
    
    private void scheduleRemindersSafely() {
        try {
            ReminderService.scheduleAllReminders(this);
            LogUtils.logInfo(this, "MainActivity", "Reminders scheduled successfully");
        } catch (Exception e) {
            LogUtils.logWarning(this, "MainActivity", "Failed to schedule reminders: " + e.getMessage());
        }
    }

    private void initViews() {
        tvWelcome = findViewById(R.id.tv_welcome);
        btnCourse = findViewById(R.id.btn_course);
        btnAssignment = findViewById(R.id.btn_assignment);
        btnStudyPlan = findViewById(R.id.btn_study_plan);
        btnProfile = findViewById(R.id.btn_profile);
        btnAIChat = findViewById(R.id.btn_ai_chat);
        btnGrade = findViewById(R.id.btn_grade);
        btnGradeManage = findViewById(R.id.btn_grade_manage);
        btnReminder = findViewById(R.id.btn_reminder);
        btnLogout = findViewById(R.id.btn_logout);
        btnStudentManage = findViewById(R.id.btn_student_manage);
        btnTeacherCourse = findViewById(R.id.btn_teacher_course);
        btnSystemConfig = findViewById(R.id.btn_system_config);
        btnNotice = findViewById(R.id.btn_notice);
        layoutStudentSection = findViewById(R.id.layout_student_section);
        layoutStudentButtons = findViewById(R.id.layout_student_buttons);
        layoutTeacherSection = findViewById(R.id.layout_teacher_section);
        layoutTeacherButtons = findViewById(R.id.layout_teacher_buttons);
    }

    private void setupUIByUserType() {
        switch (userType) {
            case 0: // 学生
                tvWelcome.setText("欢迎，同学！");
                // 学生功能区域显示
                layoutStudentSection.setVisibility(View.VISIBLE);
                layoutStudentButtons.setVisibility(View.VISIBLE);
                // 教师功能区域隐藏
                layoutTeacherSection.setVisibility(View.GONE);
                layoutTeacherButtons.setVisibility(View.GONE);
                // 系统配置隐藏
                btnSystemConfig.setVisibility(View.GONE);
                break;
            case 1: // 教师
                tvWelcome.setText("欢迎，老师！");
                // 学生功能区域隐藏
                layoutStudentSection.setVisibility(View.GONE);
                layoutStudentButtons.setVisibility(View.GONE);
                // 教师功能区域显示
                layoutTeacherSection.setVisibility(View.VISIBLE);
                layoutTeacherButtons.setVisibility(View.VISIBLE);
                // 系统配置隐藏
                btnSystemConfig.setVisibility(View.GONE);
                break;
            case 2: // 管理员
                tvWelcome.setText("欢迎，管理员！");
                // 学生功能区域显示
                layoutStudentSection.setVisibility(View.VISIBLE);
                layoutStudentButtons.setVisibility(View.VISIBLE);
                // 教师功能区域显示
                layoutTeacherSection.setVisibility(View.VISIBLE);
                layoutTeacherButtons.setVisibility(View.VISIBLE);
                // 系统配置显示
                btnSystemConfig.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void setupListeners() {
        btnCourse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CourseActivity.class);
                intent.putExtra("user_id", userId);
                intent.putExtra("user_type", userType);
                startActivity(intent);
            }
        });

        btnAssignment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AssignmentActivity.class);
                intent.putExtra("user_id", userId);
                intent.putExtra("user_type", userType);
                startActivity(intent);
            }
        });



        btnStudyPlan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, StudyPlanActivity.class);
                intent.putExtra("user_id", userId);
                intent.putExtra("user_type", userType);
                startActivity(intent);
            }
        });

        btnProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                intent.putExtra("user_id", userId);
                intent.putExtra("user_type", userType);
                startActivity(intent);
            }
        });

        btnAIChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AIChatActivity.class);
                intent.putExtra("user_id", userId);
                intent.putExtra("user_type", userType);
                startActivity(intent);
            }
        });

        btnGrade.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, GradeActivity.class);
                intent.putExtra("user_id", userId);
                intent.putExtra("user_type", userType);
                startActivity(intent);
            }
        });

        btnReminder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ReminderActivity.class);
                intent.putExtra("user_id", userId);
                intent.putExtra("user_type", userType);
                startActivity(intent);
            }
        });

        btnNotice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NoticeActivity.class);
                intent.putExtra("user_id", userId);
                intent.putExtra("user_type", userType);
                startActivity(intent);
            }
        });

        btnGradeManage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, TeacherGradeActivity.class);
                intent.putExtra("user_id", userId);
                intent.putExtra("user_type", userType);
                startActivity(intent);
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
            }
        });

        // 教师端功能
        btnStudentManage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, StudentManageActivity.class);
                intent.putExtra("user_id", userId);
                intent.putExtra("user_type", userType);
                startActivity(intent);
            }
        });



        btnTeacherCourse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, TeacherCourseActivity.class);
                intent.putExtra("user_id", userId);
                intent.putExtra("user_type", userType);
                startActivity(intent);
            }
        });

        // 管理员专属功能
        btnSystemConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSystemConfigDialog();
            }
        });
    }

    private void showSystemConfigDialog() {
        String[] options = {
            "用户管理",
            "教师管理",
            "系统设置",
            "数据备份",
            "数据恢复",
            "系统日志",
            "关于系统"
        };

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("系统配置与全局管理")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            Intent intent = new Intent(MainActivity.this, UserManageActivity.class);
                            intent.putExtra("user_id", userId);
                            intent.putExtra("user_type", userType);
                            startActivity(intent);
                            break;
                        case 1:
                            Intent teacherIntent = new Intent(MainActivity.this, TeacherManageActivity.class);
                            teacherIntent.putExtra("user_id", userId);
                            teacherIntent.putExtra("user_type", userType);
                            startActivity(teacherIntent);
                            break;
                        case 2:
                            Intent settingsIntent = new Intent(MainActivity.this, SystemSettingsActivity.class);
                            settingsIntent.putExtra("user_type", userType);
                            startActivity(settingsIntent);
                            break;
                        case 3:
                            handleDataBackup();
                            break;
                        case 4:
                            handleDataRestore();
                            break;
                        case 5:
                            showSystemLogs();
                            break;
                        case 6:
                            showAboutSystemDialog();
                            break;
                    }
                });
        builder.show();
    }

    private void showAboutSystemDialog() {
        // 显示关于系统对话框
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("关于系统")
                .setMessage(
                    "学生课程与学习管理系统\n" +
                    "版本：1.0.0\n" +
                    "开发者：谢子健\n" +
                    "联系方式：2409861778@qq.com\n\n" +
                    "本系统旨在帮助学生管理课程、作业和成绩，" +
                    "同时为教师和管理员提供便捷的管理工具。"
                )
                .setPositiveButton("确定", null);
        builder.show();
    }

    private void handleDataBackup() {
        // 显示加载对话框
        android.app.AlertDialog loadingDialog = new android.app.AlertDialog.Builder(this)
                .setMessage("正在备份数据...")
                .setCancelable(false)
                .create();
        loadingDialog.show();

        try {
            String backupPath = BackupUtils.createBackup(this);
            loadingDialog.dismiss();
            if (backupPath != null && !backupPath.isEmpty()) {
                NewDialogUtils.showSuccessDialog(this, "数据备份成功\n备份路径：" + backupPath);
                LogUtils.logInfo(this, "MainActivity", "Data backup successful: " + backupPath);
            } else {
                NewDialogUtils.showErrorDialog(this, "数据备份失败");
                LogUtils.logWarning(this, "MainActivity", "Data backup failed");
            }
        } catch (Exception e) {
            loadingDialog.dismiss();
            NewDialogUtils.showErrorDialog(this, "数据备份过程中发生错误");
            LogUtils.logError(this, "MainActivity", "Error during backup", e);
        }
    }

    private void handleDataRestore() {
        // 显示加载对话框
        android.app.AlertDialog loadingDialog = new android.app.AlertDialog.Builder(this)
                .setMessage("正在获取备份文件...")
                .setCancelable(false)
                .create();
        loadingDialog.show();

        try {
            File[] backupFiles = BackupUtils.getBackupFiles(this);
            loadingDialog.dismiss();
            if (backupFiles == null || backupFiles.length == 0) {
                NewDialogUtils.showWarningDialog(this, "没有找到备份文件");
                return;
            }

            // 显示备份文件选择对话框
            String[] backupFileNames = new String[backupFiles.length];
            for (int i = 0; i < backupFiles.length; i++) {
                if (backupFiles[i] != null) {
                    backupFileNames[i] = backupFiles[i].getName();
                } else {
                    backupFileNames[i] = "未知文件";
                }
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("选择备份文件")
                    .setItems(backupFileNames, (dialog, which) -> {
                        // 显示恢复加载对话框
                        android.app.AlertDialog restoreDialog = new android.app.AlertDialog.Builder(this)
                                .setMessage("正在恢复数据...")
                                .setCancelable(false)
                                .create();
                        restoreDialog.show();

                        try {
                            if (which >= 0 && which < backupFiles.length && backupFiles[which] != null) {
                                String backupPath = backupFiles[which].getAbsolutePath();
                                if (backupPath != null && !backupPath.isEmpty()) {
                                    boolean success = BackupUtils.restoreBackup(this, backupPath);
                                    restoreDialog.dismiss();
                                    if (success) {
                                        NewDialogUtils.showSuccessDialog(this, "数据恢复成功\n请重启应用");
                                        LogUtils.logInfo(this, "MainActivity", "Data restore successful: " + backupPath);
                                    } else {
                                        NewDialogUtils.showErrorDialog(this, "数据恢复失败");
                                        LogUtils.logWarning(this, "MainActivity", "Data restore failed: " + backupPath);
                                    }
                                } else {
                                    restoreDialog.dismiss();
                                    NewDialogUtils.showErrorDialog(this, "备份文件路径无效");
                                }
                            } else {
                                restoreDialog.dismiss();
                                NewDialogUtils.showErrorDialog(this, "选择的备份文件无效");
                            }
                        } catch (Exception e) {
                            restoreDialog.dismiss();
                            NewDialogUtils.showErrorDialog(this, "数据恢复过程中发生错误");
                            LogUtils.logError(this, "MainActivity", "Error during restore", e);
                        }
                    })
                    .setNegativeButton("取消", null);
            builder.show();
        } catch (Exception e) {
            loadingDialog.dismiss();
            NewDialogUtils.showErrorDialog(this, "获取备份文件列表失败");
            LogUtils.logError(this, "MainActivity", "Error getting backup files", e);
        }
    }

    private void showSystemLogs() {
        try {
            File logFile = LogUtils.getLogFile(this);
            if (logFile == null || !logFile.exists()) {
                NewDialogUtils.showWarningDialog(this, "没有系统日志文件");
                return;
            }

            // 读取日志文件内容
            StringBuilder logContentBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                String line;
                int lineCount = 0;
                while ((line = reader.readLine()) != null && lineCount < 200) {
                    logContentBuilder.append(line).append("\n");
                    lineCount++;
                }
            }
            
            String logContent = logContentBuilder.toString();

            if (logContent != null && !logContent.isEmpty()) {
                if (logContent.length() > 5000) {
                    logContent = logContent.substring(0, 5000) + "\n...（日志内容过长，已截断）";
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("系统日志")
                        .setMessage(logContent)
                        .setPositiveButton("确定", null);
                builder.show();
            } else {
                NewDialogUtils.showErrorDialog(this, "日志文件内容为空");
            }
        } catch (IOException e) {
            NewDialogUtils.showErrorDialog(this, "读取日志文件失败");
            LogUtils.logError(this, "MainActivity", "Error reading log file", e);
        } catch (Exception e) {
            NewDialogUtils.showErrorDialog(this, "显示系统日志时发生错误");
            LogUtils.logError(this, "MainActivity", "Error showing system logs", e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        NewDialogUtils.handleActivityResult(requestCode, resultCode);
    }
}