package com.example.myapplication.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import com.example.myapplication.database.DBHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ReminderService extends BroadcastReceiver {

    private static final String ACTION_COURSE_REMINDER = "com.example.myapplication.COURSE_REMINDER";
    private static final String ACTION_ASSIGNMENT_REMINDER = "com.example.myapplication.ASSIGNMENT_REMINDER";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_COURSE_REMINDER:
                    showCourseReminder(context, intent);
                    break;
                case ACTION_ASSIGNMENT_REMINDER:
                    showAssignmentReminder(context, intent);
                    break;
                case Intent.ACTION_BOOT_COMPLETED:
                    scheduleAllReminders(context);
                    break;
            }
        }
    }

    private void showCourseReminder(Context context, Intent intent) {
        String courseName = intent.getStringExtra("course_name");
        String classroom = intent.getStringExtra("classroom");
        String time = intent.getStringExtra("time");
        
        Toast.makeText(context, 
            "🎒 课前提醒：" + courseName + "\n地点：" + classroom + "\n时间：" + time, 
            Toast.LENGTH_LONG).show();
    }

    private void showAssignmentReminder(Context context, Intent intent) {
        String title = intent.getStringExtra("title");
        String courseName = intent.getStringExtra("course_name");
        
        Toast.makeText(context, 
            "📝 作业提醒：" + title + "\n课程：" + courseName + "\n即将截止，请及时提交！", 
            Toast.LENGTH_LONG).show();
    }

    public static void scheduleAllReminders(Context context) {
        scheduleCourseReminders(context);
        scheduleAssignmentReminders(context);
    }

    public static void scheduleCourseReminders(Context context) {
        if (context == null) {
            return;
        }
        
        DBHelper dbHelper = null;
        Cursor cursor = null;
        
        try {
            dbHelper = new DBHelper(context);
            cursor = dbHelper.query("course", null, null, null, null, null, null);
            
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    try {
                        int courseNameIndex = cursor.getColumnIndex("course_name");
                        int classroomIndex = cursor.getColumnIndex("classroom");
                        int startTimeIndex = cursor.getColumnIndex("start_time");
                        int dayOfWeekIndex = cursor.getColumnIndex("day_of_week");
                        
                        if (courseNameIndex != -1 && classroomIndex != -1 && 
                            startTimeIndex != -1 && dayOfWeekIndex != -1) {
                            
                            String courseName = cursor.getString(courseNameIndex);
                            String classroom = cursor.getString(classroomIndex);
                            String startTime = cursor.getString(startTimeIndex);
                            int dayOfWeek = cursor.getInt(dayOfWeekIndex);
                            
                            if (courseName != null && startTime != null && dayOfWeek >= 1 && dayOfWeek <= 7) {
                                scheduleCourseReminder(context, courseName, classroom, startTime, dayOfWeek);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (dbHelper != null) {
                try {
                    dbHelper.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void scheduleCourseReminder(Context context, String courseName, 
            String classroom, String startTime, int dayOfWeek) {
        try {
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.CHINA);
            Date time = sdf.parse(startTime);
            
            Calendar reminderTime = Calendar.getInstance();
            reminderTime.set(Calendar.HOUR_OF_DAY, time.getHours());
            reminderTime.set(Calendar.MINUTE, time.getMinutes() - 15);
            reminderTime.set(Calendar.SECOND, 0);
            
            int targetDay = dayOfWeek;
            if (dayOfWeek == 7) {
                targetDay = Calendar.SUNDAY;
            } else {
                targetDay = dayOfWeek + 1;
            }
            reminderTime.set(Calendar.DAY_OF_WEEK, targetDay);
            
            if (reminderTime.before(Calendar.getInstance())) {
                reminderTime.add(Calendar.WEEK_OF_YEAR, 1);
            }

            Intent intent = new Intent(context, ReminderService.class);
            intent.setAction(ACTION_COURSE_REMINDER);
            intent.putExtra("course_name", courseName);
            intent.putExtra("classroom", classroom);
            intent.putExtra("time", startTime);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context, 
                    courseName.hashCode(), 
                    intent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            reminderTime.getTimeInMillis(),
                            pendingIntent);
                } else {
                    alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            reminderTime.getTimeInMillis(),
                            pendingIntent);
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public static void scheduleAssignmentReminders(Context context) {
        if (context == null) {
            return;
        }
        
        DBHelper dbHelper = null;
        Cursor cursor = null;
        
        try {
            dbHelper = new DBHelper(context);
            cursor = dbHelper.query("assignment", null, null, null, null, null, null);
            
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    try {
                        int titleIndex = cursor.getColumnIndex("title");
                        int courseIdIndex = cursor.getColumnIndex("course_id");
                        int dueDateIndex = cursor.getColumnIndex("due_date");
                        int statusIndex = cursor.getColumnIndex("status");
                        
                        if (titleIndex != -1 && courseIdIndex != -1 && 
                            dueDateIndex != -1 && statusIndex != -1) {
                            
                            String title = cursor.getString(titleIndex);
                            int courseId = cursor.getInt(courseIdIndex);
                            String dueDate = cursor.getString(dueDateIndex);
                            int status = cursor.getInt(statusIndex);
                            
                            if (title != null && dueDate != null && status != 2) {
                                String courseName = getCourseName(dbHelper, courseId);
                                scheduleAssignmentReminder(context, title, courseName, dueDate);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (dbHelper != null) {
                try {
                    dbHelper.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static String getCourseName(DBHelper dbHelper, int courseId) {
        Cursor cursor = dbHelper.query("course", new String[]{"course_name"}, 
                "id = ?", new String[]{String.valueOf(courseId)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int nameIndex = cursor.getColumnIndex("course_name");
            if (nameIndex != -1) {
                String name = cursor.getString(nameIndex);
                cursor.close();
                return name;
            }
            cursor.close();
        }
        return "未知课程";
    }

    private static void scheduleAssignmentReminder(Context context, String title, 
            String courseName, String dueDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);
            Date date = sdf.parse(dueDate);
            
            Calendar reminderTime = Calendar.getInstance();
            reminderTime.setTime(date);
            reminderTime.add(Calendar.HOUR_OF_DAY, -24);
            
            if (reminderTime.before(Calendar.getInstance())) {
                return;
            }

            Intent intent = new Intent(context, ReminderService.class);
            intent.setAction(ACTION_ASSIGNMENT_REMINDER);
            intent.putExtra("title", title);
            intent.putExtra("course_name", courseName);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context, 
                    ("assignment_" + title).hashCode(), 
                    intent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            reminderTime.getTimeInMillis(),
                            pendingIntent);
                } else {
                    alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            reminderTime.getTimeInMillis(),
                            pendingIntent);
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}