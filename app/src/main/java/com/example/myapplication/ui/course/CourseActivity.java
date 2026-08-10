package com.example.myapplication.ui.course;
import com.example.myapplication.model.CourseExcelData;

import androidx.appcompat.app.AppCompatActivity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CourseActivity extends AppCompatActivity {

    private TextView tvViewMode, tvCurrentDate;
    private Spinner spViewMode;
    private ListView lvCourses;
    private Button btnAddCourse, btnImportCourse, btnBack, btnTimeSettings;
    private DBHelper dbHelper;
    private int userId = -1;
    private int userType = -1;
    private List<String> courseList;
    private List<Integer> courseIdList;
    private ArrayAdapter<String> courseAdapter;
    private static final String TAG = "CourseActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course);
        
        android.util.Log.d(TAG, "onCreate started");

        try {
            Intent intent = getIntent();
            if (intent == null) {
                android.util.Log.e(TAG, "Intent is null");
                handleErrorAndFinish("启动参数错误");
                return;
            }
            
            userId = intent.getIntExtra("user_id", -1);
            userType = intent.getIntExtra("user_type", -1);
            
            android.util.Log.d(TAG, String.format("userId=%d, userType=%d", userId, userType));
            
            if (userId == -1 || userType == -1) {
                android.util.Log.e(TAG, "Invalid user data");
                handleErrorAndFinish("用户信息错误，请重新登录");
                return;
            }
            
            if (userType != 0 && userType != 2) {
                android.util.Log.w(TAG, "User does not have permission: userType=" + userType);
                handleErrorAndFinish("您没有权限访问该功能");
                return;
            }

            initViews();
            initDatabase();
            loadCourses();
            setupListeners();
            
            android.util.Log.d(TAG, "onCreate completed successfully");
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error in onCreate", e);
            handleErrorAndFinish("页面加载失败: " + e.getMessage());
        }
    }
    
    private void handleErrorAndFinish(String message) {
        if (!isFinishing() && !isDestroyed()) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        tvViewMode = findViewById(R.id.tv_view_mode);
        tvCurrentDate = findViewById(R.id.tv_current_date);
        spViewMode = findViewById(R.id.sp_view_mode);
        lvCourses = findViewById(R.id.lv_courses);
        btnAddCourse = findViewById(R.id.btn_add_course);
        btnImportCourse = findViewById(R.id.btn_import_course);
        btnBack = findViewById(R.id.btn_back);
        btnTimeSettings = findViewById(R.id.btn_time_settings);

        // 显示当天日期
        updateCurrentDate();

        // 设置视图模式选项
        ArrayAdapter<CharSequence> viewModeAdapter = ArrayAdapter.createFromResource(this,
                R.array.view_mode_options, android.R.layout.simple_spinner_item);
        viewModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spViewMode.setAdapter(viewModeAdapter);

        // 初始化课程列表
        courseList = new ArrayList<>();
        courseIdList = new ArrayList<>();
        courseAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, courseList);
        lvCourses.setAdapter(courseAdapter);
    }

    private void updateCurrentDate() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.CHINA);
        String currentDate = sdf.format(calendar.getTime());
        tvCurrentDate.setText("今天：" + currentDate);
    }

    private void initDatabase() {
        dbHelper = new DBHelper(this);
    }

    private void loadCourses() {
        android.util.Log.d(TAG, "loadCourses started for userId=" + userId);
        
        if (userId == -1) {
            android.util.Log.e(TAG, "Invalid userId in loadCourses");
            Toast.makeText(this, "用户信息错误", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            courseList.clear();
            courseIdList.clear();
            Cursor cursor = null;
            
            try {
                cursor = dbHelper.getCoursesByUserId(userId);
                
                if (cursor == null) {
                    android.util.Log.w(TAG, "Cursor is null when loading courses");
                    courseAdapter.notifyDataSetChanged();
                    return;
                }
                
                android.util.Log.d(TAG, "Courses found: " + cursor.getCount());
                
                while (cursor.moveToNext()) {
                    try {
                        int courseIdIndex = cursor.getColumnIndex("id");
                        int courseNameIndex = cursor.getColumnIndex("course_name");
                        int teacherNameIndex = cursor.getColumnIndex("teacher_name");
                        int classroomIndex = cursor.getColumnIndex("classroom");
                        int startTimeIndex = cursor.getColumnIndex("start_time");
                        int endTimeIndex = cursor.getColumnIndex("end_time");
                        int dayOfWeekIndex = cursor.getColumnIndex("day_of_week");
                        int isFavoriteIndex = cursor.getColumnIndex("is_favorite");
                        
                        if (courseIdIndex == -1 || courseNameIndex == -1 || 
                            teacherNameIndex == -1 || classroomIndex == -1 ||
                            startTimeIndex == -1 || endTimeIndex == -1 ||
                            dayOfWeekIndex == -1 || isFavoriteIndex == -1) {
                            android.util.Log.w(TAG, "Missing required column in course table");
                            continue;
                        }
                        
                        int courseId = cursor.getInt(courseIdIndex);
                        String courseName = cursor.getString(courseNameIndex);
                        String teacherName = cursor.getString(teacherNameIndex);
                        String classroom = cursor.getString(classroomIndex);
                        String startTime = cursor.getString(startTimeIndex);
                        String endTime = cursor.getString(endTimeIndex);
                        int dayOfWeek = cursor.getInt(dayOfWeekIndex);
                        int isFavorite = cursor.getInt(isFavoriteIndex);

                        String dayStr = getDayOfWeekString(dayOfWeek);
                        String favoriteStr = isFavorite == 1 ? "★ " : "";
                        String courseInfo = favoriteStr + courseName + " - " + teacherName + "\n" +
                                dayStr + " " + startTime + "-" + endTime + " " + classroom;
                        courseList.add(courseInfo);
                        courseIdList.add(courseId);
                        
                        android.util.Log.d(TAG, "Loaded course: " + courseName);
                    } catch (Exception e) {
                        android.util.Log.e(TAG, "Error processing course row", e);
                    }
                }
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error loading courses", e);
                Toast.makeText(this, "加载课程失败", Toast.LENGTH_SHORT).show();
            } finally {
                if (cursor != null) {
                    try {
                        cursor.close();
                    } catch (Exception e) {
                        android.util.Log.e(TAG, "Error closing cursor", e);
                    }
                }
            }
            
            courseAdapter.notifyDataSetChanged();
            android.util.Log.d(TAG, "loadCourses completed. Total courses: " + courseList.size());
        } catch (Exception e) {
            android.util.Log.e(TAG, "Unexpected error in loadCourses", e);
            Toast.makeText(this, "加载课程时发生错误", Toast.LENGTH_SHORT).show();
        }
    }

    private String getDayOfWeekString(int dayOfWeek) {
        String[] days = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        if (dayOfWeek >= 1 && dayOfWeek <= 7) {
            return days[dayOfWeek];
        }
        return "未知";
    }

    private void setupListeners() {
        spViewMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        loadCourses();
                        break;
                    case 1:
                        showWeekView();
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        lvCourses.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 点击课程项，可以进行编辑或查看详情
                showCourseDetails(position);
            }
        });

        lvCourses.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                // 长按课程项，显示操作菜单
                showCourseOptions(position);
                return true;
            }
        });

        btnAddCourse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddCourseDialog();
            }
        });

        btnImportCourse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImportCourseDialog();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnTimeSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CourseActivity.this, CourseTimeSettingsActivity.class);
                intent.putExtra("user_id", userId);
                intent.putExtra("user_type", userType);
                startActivity(intent);
            }
        });
    }

    private void showAddCourseDialog() {
        // 打开AddCourseActivity来添加课程
        Intent intent = new Intent(CourseActivity.this, AddCourseActivity.class);
        intent.putExtra("user_id", userId);
        startActivityForResult(intent, 1);
    }

    private static final int PICK_EXCEL_FILE = 1001;
    private static final int PICK_IMAGE_FILE = 1002;

    private void showImportCourseDialog() {
        // 显示课程导入选项对话框
        String[] importOptions = {"手动录入", "Excel导入", "图片导入", "示例数据导入"};
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("课程导入方式")
                .setItems(importOptions, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                showAddCourseDialog(); // 手动录入
                                break;
                            case 1:
                                importFromExcel();
                                break;
                            case 2:
                                importFromImage();
                                break;
                            case 3:
                                importExampleData();
                                break;
                        }
                    }
                });
        builder.show();
    }

    private void importFromExcel() {
        // 从Excel导入课程
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_EXCEL_FILE);
    }

    private void importFromImage() {
        // 从图片导入课程
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_IMAGE_FILE);
    }

    private void importExampleData() {
        DialogUtils.showInfoDialog(this, "提示", "示例数据导入功能已启动");
        importSampleCourses();
    }

    private void importSampleCourses() {
        // 显示加载对话框
        android.app.AlertDialog loadingDialog = new android.app.AlertDialog.Builder(this)
                .setMessage("正在导入示例课程...")
                .setCancelable(false)
                .create();
        loadingDialog.show();

        try {
            // 导入示例课程数据
            // 课程1: 高等数学
            ContentValues values1 = new ContentValues();
            values1.put("user_id", userId);
            values1.put("course_name", "高等数学");
            values1.put("teacher_name", "张教授");
            values1.put("classroom", "教1-101");
            values1.put("start_time", "08:00");
            values1.put("end_time", "09:40");
            values1.put("day_of_week", 1); // 周一
            values1.put("is_favorite", 0);
            dbHelper.insertCourse(values1);
            
            // 课程2: 大学英语
            ContentValues values2 = new ContentValues();
            values2.put("user_id", userId);
            values2.put("course_name", "大学英语");
            values2.put("teacher_name", "李老师");
            values2.put("classroom", "教2-202");
            values2.put("start_time", "10:00");
            values2.put("end_time", "11:40");
            values2.put("day_of_week", 2); // 周二
            values2.put("is_favorite", 0);
            dbHelper.insertCourse(values2);
            
            // 课程3: 数据结构
            ContentValues values3 = new ContentValues();
            values3.put("user_id", userId);
            values3.put("course_name", "数据结构");
            values3.put("teacher_name", "王教授");
            values3.put("classroom", "教3-303");
            values3.put("start_time", "14:00");
            values3.put("end_time", "15:40");
            values3.put("day_of_week", 3); // 周三
            values3.put("is_favorite", 0);
            dbHelper.insertCourse(values3);
            
            // 重新加载课程列表
            loadCourses();
            loadingDialog.dismiss();
            DialogUtils.showSuccessDialog(this, "课程导入成功！");
        } catch (Exception e) {
            loadingDialog.dismiss();
            e.printStackTrace();
            DialogUtils.showErrorDialog(this, "导入示例课程失败");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            loadCourses();
        } else if (requestCode == PICK_EXCEL_FILE && resultCode == RESULT_OK && data != null) {
            // 处理Excel文件选择
            Uri uri = data.getData();
            if (uri != null) {
                parseExcelFile(uri);
            }
        } else if (requestCode == PICK_IMAGE_FILE && resultCode == RESULT_OK && data != null) {
            // 处理图片文件选择
            Uri uri = data.getData();
            if (uri != null) {
                handleImageFile(uri);
            }
        }
    }

    private void parseExcelFile(Uri uri) {
        try {
            // 模拟Excel解析过程
            List<CourseExcelData> courseDataList = new ArrayList<>();
            
            // 模拟解析结果
            CourseExcelData mockData1 = new CourseExcelData();
            mockData1.setCourseName("高等数学");
            mockData1.setTeacherName("张教授");
            mockData1.setClassroom("教1-101");
            mockData1.setStartTime("08:00");
            mockData1.setEndTime("09:40");
            mockData1.setDayOfWeek(1);
            courseDataList.add(mockData1);
            
            CourseExcelData mockData2 = new CourseExcelData();
            mockData2.setCourseName("大学英语");
            mockData2.setTeacherName("李老师");
            mockData2.setClassroom("教2-202");
            mockData2.setStartTime("10:00");
            mockData2.setEndTime("11:40");
            mockData2.setDayOfWeek(2);
            courseDataList.add(mockData2);
            
            // 将解析的数据导入到数据库
            importCoursesFromExcel(courseDataList);
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtils.showErrorDialog(this, "Excel文件解析失败: " + e.getMessage());
        }
    }

    private void importCoursesFromExcel(List<CourseExcelData> courseDataList) {
        if (courseDataList.isEmpty()) {
            DialogUtils.showWarningDialog(this, "Excel文件中没有课程数据");
            return;
        }
        
        int importedCount = 0;
        for (CourseExcelData courseData : courseDataList) {
            ContentValues values = new ContentValues();
            values.put("user_id", userId);
            values.put("course_name", courseData.getCourseName());
            values.put("teacher_name", courseData.getTeacherName());
            values.put("classroom", courseData.getClassroom());
            values.put("start_time", courseData.getStartTime());
            values.put("end_time", courseData.getEndTime());
            values.put("day_of_week", courseData.getDayOfWeek());
            values.put("is_favorite", 0);
            
            if (dbHelper.insertCourse(values) > 0) {
                importedCount++;
            }
        }
        
        loadCourses();
        DialogUtils.showSuccessDialog(this, "成功导入" + importedCount + "门课程");
    }

    private void handleImageFile(Uri uri) {
        // 显示加载对话框
        final android.app.AlertDialog loadingDialog = new android.app.AlertDialog.Builder(this)
                .setMessage("正在处理图片...")
                .setCancelable(false)
                .create();
        loadingDialog.show();

        try {
            // 获取图片Bitmap
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            if (bitmap != null) {
                try {
                    // 创建InputImage
                    InputImage image = InputImage.fromBitmap(bitmap, 0);
                    
                    // 创建文本识别器
                    TextRecognizer recognizer = TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
                    
                    // 处理识别结果
                    recognizer.process(image)
                            .addOnSuccessListener(text -> {
                                loadingDialog.dismiss();
                                // 处理识别到的文本
                                processOCRResult(text);
                            })
                            .addOnFailureListener(e -> {
                                loadingDialog.dismiss();
                                e.printStackTrace();
                                DialogUtils.showErrorDialog(this, "图片识别失败: " + e.getMessage());
                            });
                } catch (Exception e) {
                    loadingDialog.dismiss();
                    e.printStackTrace();
                    DialogUtils.showErrorDialog(this, "图片处理失败: " + e.getMessage());
                }
            } else {
                loadingDialog.dismiss();
                DialogUtils.showErrorDialog(this, "无法加载图片");
            }
        } catch (Exception e) {
            loadingDialog.dismiss();
            e.printStackTrace();
            DialogUtils.showErrorDialog(this, "图片处理失败: " + e.getMessage());
        }
    }

    private void processOCRResult(Text text) {
        StringBuilder recognizedText = new StringBuilder();
        for (Text.TextBlock block : text.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                recognizedText.append(line.getText()).append("\n");
            }
        }

        // 解析识别到的文本，提取课程信息
        List<CourseExcelData> courseDataList = parseOCRText(recognizedText.toString());
        
        if (!courseDataList.isEmpty()) {
            // 导入识别到的课程
            importCoursesFromExcel(courseDataList);
        } else {
            DialogUtils.showWarningDialog(this, "未识别到课程信息，请确保图片清晰且包含完整的课程信息");
        }
    }

    private List<CourseExcelData> parseOCRText(String text) {
        List<CourseExcelData> courseDataList = new ArrayList<>();
        
        // 简单的文本解析逻辑，根据识别结果提取课程信息
        // 实际项目中可能需要更复杂的解析逻辑
        String[] lines = text.split("\n");
        
        CourseExcelData currentCourse = null;
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            // 检测课程名称（简单示例：包含"课程"或"课"的行）
            if (line.contains("课程") || line.contains("课")) {
                if (currentCourse != null) {
                    courseDataList.add(currentCourse);
                }
                currentCourse = new CourseExcelData();
                currentCourse.setCourseName(line);
            } 
            // 检测教师（包含"老师"或"教授"的行）
            else if (currentCourse != null && (line.contains("老师") || line.contains("教授"))) {
                currentCourse.setTeacherName(line);
            }
            // 检测教室（包含"教"和数字的行）
            else if (currentCourse != null && line.contains("教") && line.matches(".*[0-9].*")) {
                currentCourse.setClassroom(line);
            }
            // 检测时间（包含"时间"或"点"的行）
            else if (currentCourse != null && (line.contains("时间") || line.contains("点"))) {
                // 简单解析时间格式，如 "08:00-09:40"
                if (line.contains("-")) {
                    String[] timeParts = line.split("-");
                    if (timeParts.length == 2) {
                        currentCourse.setStartTime(timeParts[0].trim());
                        currentCourse.setEndTime(timeParts[1].trim());
                    }
                }
            }
            // 检测星期（包含"周一"到"周日"的行）
            else if (currentCourse != null) {
                if (line.contains("周一")) currentCourse.setDayOfWeek(1);
                else if (line.contains("周二")) currentCourse.setDayOfWeek(2);
                else if (line.contains("周三")) currentCourse.setDayOfWeek(3);
                else if (line.contains("周四")) currentCourse.setDayOfWeek(4);
                else if (line.contains("周五")) currentCourse.setDayOfWeek(5);
                else if (line.contains("周六")) currentCourse.setDayOfWeek(6);
                else if (line.contains("周日")) currentCourse.setDayOfWeek(7);
            }
        }
        
        // 添加最后一个课程
        if (currentCourse != null) {
            courseDataList.add(currentCourse);
        }
        
        return courseDataList;
    }

    private void showCourseDetails(int position) {
        try {
            // 显示课程详情
            if (position < 0 || position >= courseIdList.size()) {
                DialogUtils.showErrorDialog(this, "课程索引无效");
                return;
            }
            
            int courseId = courseIdList.get(position);
            Cursor cursor = null;
            try {
                cursor = dbHelper.getCourseById(courseId);
                if (cursor != null && cursor.moveToFirst()) {
                    int courseNameIndex = cursor.getColumnIndex("course_name");
                    int teacherNameIndex = cursor.getColumnIndex("teacher_name");
                    int classroomIndex = cursor.getColumnIndex("classroom");
                    int startTimeIndex = cursor.getColumnIndex("start_time");
                    int endTimeIndex = cursor.getColumnIndex("end_time");
                    int dayOfWeekIndex = cursor.getColumnIndex("day_of_week");
                    int isFavoriteIndex = cursor.getColumnIndex("is_favorite");

                    if (courseNameIndex != -1 && teacherNameIndex != -1 && classroomIndex != -1 && startTimeIndex != -1 && endTimeIndex != -1 && dayOfWeekIndex != -1 && isFavoriteIndex != -1) {
                        String courseName = cursor.getString(courseNameIndex);
                        String teacherName = cursor.getString(teacherNameIndex);
                        String classroom = cursor.getString(classroomIndex);
                        String startTime = cursor.getString(startTimeIndex);
                        String endTime = cursor.getString(endTimeIndex);
                        int dayOfWeek = cursor.getInt(dayOfWeekIndex);
                        int isFavorite = cursor.getInt(isFavoriteIndex);

                        String dayStr = getDayOfWeekString(dayOfWeek);
                        String favoriteStr = isFavorite == 1 ? "★ 已收藏" : "☆ 未收藏";

                        // 创建对话框显示课程详情
                        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                        builder.setTitle("课程详情");
                        builder.setMessage(
                            "课程名称：" + courseName + "\n" +
                            "任课教师：" + teacherName + "\n" +
                            "上课地点：" + classroom + "\n" +
                            "上课时间：" + dayStr + " " + startTime + "-" + endTime + "\n" +
                            "收藏状态：" + favoriteStr
                        );
                        builder.setPositiveButton("确定", null);
                        builder.setNegativeButton("导航", new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(android.content.DialogInterface dialog, int which) {
                                navigateToClassroom(classroom);
                            }
                        });
                        builder.show();
                    } else {
                        DialogUtils.showErrorDialog(this, "课程信息不完整");
                    }
                } else {
                    DialogUtils.showErrorDialog(this, "获取课程详情失败");
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtils.showErrorDialog(this, "显示课程详情失败");
        }
    }

    private void showCourseOptions(int position) {
        // 显示课程操作菜单
        String[] options = {"编辑课程", "删除课程", "收藏课程", "取消收藏", "分享课程"};
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("课程操作")
                .setItems(options, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                editCourse(position);
                                break;
                            case 1:
                                deleteCourse(position);
                                break;
                            case 2:
                                favoriteCourse(position, true);
                                break;
                            case 3:
                                favoriteCourse(position, false);
                                break;
                            case 4:
                                shareCourse(position);
                                break;
                        }
                    }
                });
        builder.show();
    }

    private void editCourse(int position) {
        // 编辑课程
        int courseId = courseIdList.get(position);
        Intent intent = new Intent(CourseActivity.this, AddCourseActivity.class);
        intent.putExtra("user_id", userId);
        intent.putExtra("course_id", courseId);
        startActivityForResult(intent, 1);
    }

    private void deleteCourse(int position) {
        try {
            // 删除课程
            if (position < 0 || position >= courseIdList.size()) {
                DialogUtils.showErrorDialog(this, "课程索引无效");
                return;
            }
            
            int courseId = courseIdList.get(position);
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("确认删除")
                    .setMessage("确定要删除这门课程吗？")
                    .setPositiveButton("确定", new android.content.DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(android.content.DialogInterface dialog, int which) {
                            try {
                                int result = dbHelper.delete("course", "id = ?", new String[]{String.valueOf(courseId)});
                                if (result > 0) {
                                    DialogUtils.showSuccessDialog(CourseActivity.this, "课程已删除");
                                    loadCourses();
                                } else {
                                    DialogUtils.showErrorDialog(CourseActivity.this, "删除课程失败");
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                DialogUtils.showErrorDialog(CourseActivity.this, "删除课程时发生错误");
                            }
                        }
                    })
                    .setNegativeButton("取消", null);
            builder.show();
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtils.showErrorDialog(this, "显示删除确认对话框失败");
        }
    }

    private void favoriteCourse(int position, boolean favorite) {
        try {
            if (position < 0 || position >= courseIdList.size() || position >= courseList.size()) {
                DialogUtils.showErrorDialog(this, "课程索引无效");
                return;
            }
            
            int courseId = courseIdList.get(position);
            ContentValues values = new ContentValues();
            values.put("is_favorite", favorite ? 1 : 0);
            int result = dbHelper.update("course", values, "id = ?", new String[]{String.valueOf(courseId)});
            if (result > 0) {
                String status = favorite ? "已收藏" : "已取消收藏";
                DialogUtils.showSuccessDialog(this, status + "：" + courseList.get(position));
                loadCourses();
            } else {
                DialogUtils.showErrorDialog(this, "更新收藏状态失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtils.showErrorDialog(this, "更新收藏状态时发生错误");
        }
    }

    private void shareCourse(int position) {
        try {
            // 分享课程
            if (position < 0 || position >= courseList.size()) {
                DialogUtils.showErrorDialog(this, "课程索引无效");
                return;
            }
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, courseList.get(position));
            if (shareIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(Intent.createChooser(shareIntent, "分享课程"));
            } else {
                DialogUtils.showWarningDialog(this, "没有可用的分享应用");
            }
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtils.showErrorDialog(this, "分享课程失败");
        }
    }

    private void navigateToClassroom(String classroom) {
        try {
            // 上课地点一键导航
            if (classroom == null || classroom.isEmpty()) {
                DialogUtils.showErrorDialog(this, "上课地点为空");
                return;
            }
            
            // 构建导航意图，启动地图应用
            String uri = "geo:0,0?q=" + classroom;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                // 如果没有安装Google Maps，使用系统默认地图应用
                intent.setPackage(null);
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    DialogUtils.showWarningDialog(this, "无法启动地图应用");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtils.showErrorDialog(this, "启动导航失败");
        }
    }

    private void showDayView() {
        try {
            courseList.clear();
            courseIdList.clear();
            
            Calendar calendar = Calendar.getInstance();
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            int currentDay = dayOfWeek == 1 ? 7 : dayOfWeek - 1;
            
            Cursor cursor = dbHelper.query("course", null, "user_id = ? AND day_of_week = ?", 
                    new String[]{String.valueOf(userId), String.valueOf(currentDay)}, null, null, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    int courseId = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    String courseName = cursor.getString(cursor.getColumnIndexOrThrow("course_name"));
                    String teacherName = cursor.getString(cursor.getColumnIndexOrThrow("teacher_name"));
                    String classroom = cursor.getString(cursor.getColumnIndexOrThrow("classroom"));
                    String startTime = cursor.getString(cursor.getColumnIndexOrThrow("start_time"));
                    String endTime = cursor.getString(cursor.getColumnIndexOrThrow("end_time"));
                    String courseInfo = courseName + " - " + teacherName + " - " + classroom + " (" + startTime + "-" + endTime + ")";
                    courseList.add(courseInfo);
                    courseIdList.add(courseId);
                }
                cursor.close();
            }
            courseAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtils.showErrorDialog(this, "加载日视图失败");
        }
    }

    private void showWeekView() {
        Intent intent = new Intent(CourseActivity.this, CourseWeekViewActivity.class);
        intent.putExtra("user_id", userId);
        intent.putExtra("user_type", userType);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        android.util.Log.d(TAG, "onDestroy started");
        
        if (dbHelper != null) {
            try {
                dbHelper.close();
                android.util.Log.d(TAG, "Database closed successfully");
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error closing database", e);
            }
        }
        
        super.onDestroy();
        android.util.Log.d(TAG, "onDestroy completed");
    }
}
