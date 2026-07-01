package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.OpenableColumns;
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
import com.example.myapplication.utils.SecurityUtils;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class StudentManageActivity extends AppCompatActivity {

    private TextView tvStudentCount;
    private Spinner spFilterClass;
    private ListView lvStudents;
    private Button btnAddStudent, btnBack, btnResetPassword, btnExport;
    private DBHelper dbHelper;
    private int teacherId;
    private List<String> studentList;
    private List<Integer> studentIdList;
    private ArrayAdapter<String> studentAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_manage);

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
        loadStudents();
        updateStudentStats();
        setupListeners();
    }

    private void initViews() {
        tvStudentCount = findViewById(R.id.tv_student_count);
        spFilterClass = findViewById(R.id.sp_filter_class);
        lvStudents = findViewById(R.id.lv_students);
        btnAddStudent = findViewById(R.id.btn_add_student);
        btnBack = findViewById(R.id.btn_back);
        btnResetPassword = findViewById(R.id.btn_reset_password);
        btnExport = findViewById(R.id.btn_export);

        ArrayAdapter<CharSequence> classAdapter = ArrayAdapter.createFromResource(this,
                R.array.class_options, android.R.layout.simple_spinner_item);
        classAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFilterClass.setAdapter(classAdapter);

        studentList = new ArrayList<>();
        studentIdList = new ArrayList<>();
        studentAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, studentList);
        lvStudents.setAdapter(studentAdapter);
    }

    private void initDatabase() {
        dbHelper = new DBHelper(this);
    }

    private void loadStudents() {
        studentList.clear();
        studentIdList.clear();

        Cursor cursor = dbHelper.query("user", null, "type = ?", new String[]{"0"}, null, null, "username ASC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int userId = 0;
                int idIndex = cursor.getColumnIndex("id");
                if (idIndex != -1) {
                    userId = cursor.getInt(idIndex);
                }
                String username = "";
                int usernameIndex = cursor.getColumnIndex("username");
                if (usernameIndex != -1) {
                    username = cursor.getString(usernameIndex);
                }
                String name = "";
                int nameIndex = cursor.getColumnIndex("name");
                if (nameIndex != -1) {
                    name = cursor.getString(nameIndex);
                }
                String studentId = "";
                int studentIdIndex = cursor.getColumnIndex("student_id");
                if (studentIdIndex != -1) {
                    studentId = cursor.getString(studentIdIndex);
                }
                String className = "";
                int classNameIndex = cursor.getColumnIndex("class_name");
                if (classNameIndex != -1) {
                    className = cursor.getString(classNameIndex);
                }
                String email = "";
                int emailIndex = cursor.getColumnIndex("email");
                if (emailIndex != -1) {
                    email = cursor.getString(emailIndex);
                }
                String phone = "";
                int phoneIndex = cursor.getColumnIndex("phone");
                if (phoneIndex != -1) {
                    phone = cursor.getString(phoneIndex);
                }

                String studentInfo = "学号：" + studentId + "\n" +
                        "用户名：" + username + "\n" +
                        "姓名：" + name + "\n" +
                        "班级：" + className + "\n" +
                        "邮箱：" + email + "\n" +
                        "电话：" + phone;
                studentList.add(studentInfo);
                studentIdList.add(userId);
            }
            cursor.close();
        }
        studentAdapter.notifyDataSetChanged();
    }

    private void updateStudentStats() {
        int studentCount = 0;
        Cursor cursor = dbHelper.query("user", new String[]{"id"}, "type = ?", new String[]{"0"}, null, null, null);
        if (cursor != null) {
            studentCount = cursor.getCount();
            cursor.close();
        }
        tvStudentCount.setText("学生总数：" + studentCount);
    }

    private static final int REQUEST_CODE_PICK_EXCEL = 1001;

    private void setupListeners() {
        spFilterClass.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterStudents();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        lvStudents.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showStudentDetails(position);
            }
        });

        lvStudents.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                showStudentOptions(position);
                return true;
            }
        });

        btnAddStudent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddStudentDialog();
            }
        });

        // 添加批量导入按钮点击事件
        Button btnImportStudents = findViewById(R.id.btn_import_students);
        if (btnImportStudents != null) {
            btnImportStudents.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    importStudentsFromExcel();
                }
            });
        }

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnResetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetSelectedStudentPassword();
            }
        });

        btnExport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportStudentData();
            }
        });
    }

    private void resetSelectedStudentPassword() {
        if (lvStudents.getCheckedItemPosition() == ListView.INVALID_POSITION) {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("重置密码")
                    .setMessage("请先长按选择一个学生")
                    .setPositiveButton("确定", null)
                    .show();
            return;
        }
        
        int position = lvStudents.getCheckedItemPosition();
        if (position >= 0 && position < studentIdList.size()) {
            int studentId = studentIdList.get(position);
            String defaultPassword = SecurityUtils.encryptPassword("123456");
            ContentValues values = new ContentValues();
            values.put("password", defaultPassword);
            
            int result = dbHelper.update("user", values, "id = ?", new String[]{String.valueOf(studentId)});
            if (result > 0) {
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                builder.setTitle("成功")
                        .setMessage("密码已重置为：123456")
                        .setPositiveButton("确定", null)
                        .show();
            } else {
                Toast.makeText(this, "重置密码失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void exportStudentData() {
        StringBuilder data = new StringBuilder();
        data.append("学号,姓名,班级,专业\n");
        
        Cursor cursor = dbHelper.query("user", null, "type = ?", new String[]{"0"}, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String username = cursor.getString(cursor.getColumnIndexOrThrow("username"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String className = cursor.getString(cursor.getColumnIndexOrThrow("class_name"));
                String major = cursor.getString(cursor.getColumnIndexOrThrow("major"));
                
                data.append(username).append(",")
                    .append(name != null ? name : "").append(",")
                    .append(className != null ? className : "").append(",")
                    .append(major != null ? major : "").append("\n");
            }
            cursor.close();
        }
        
        try {
            java.io.File file = new java.io.File(getExternalFilesDir(null), "students_export.csv");
            java.io.FileWriter writer = new java.io.FileWriter(file);
            writer.write(data.toString());
            writer.close();
            
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("导出成功")
                    .setMessage("学生数据已导出到：\n" + file.getAbsolutePath())
                    .setPositiveButton("确定", null)
                    .show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "导出失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void importStudentsFromExcel() {
        // 打开文件选择器
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/vnd.ms-excel");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"});
        startActivityForResult(intent, REQUEST_CODE_PICK_EXCEL);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_EXCEL && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                new ImportExcelTask(StudentManageActivity.this).execute(uri);
            }
        }
    }

    private static class ImportExcelTask extends AsyncTask<Uri, Void, Integer> {
        private final StudentManageActivity activity;
        
        public ImportExcelTask(StudentManageActivity activity) {
            this.activity = activity;
        }

        @Override
        protected void onPreExecute() {
            Toast.makeText(activity, "正在导入学生数据...", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected Integer doInBackground(Uri... params) {
            Uri uri = params[0];
            int importedCount = 0;

            try {
                InputStream inputStream = activity.getContentResolver().openInputStream(uri);
                Workbook workbook;

                // 根据文件扩展名选择工作簿类型
                String fileName = activity.getFileName(uri);
                if (fileName.endsWith(".xlsx")) {
                    workbook = new XSSFWorkbook(inputStream);
                } else {
                    workbook = new HSSFWorkbook(inputStream);
                }

                Sheet sheet = workbook.getSheetAt(0);
                Iterator<Row> rowIterator = sheet.iterator();

                // 跳过表头
                if (rowIterator.hasNext()) {
                    rowIterator.next();
                }

                while (rowIterator.hasNext()) {
                    Row row = rowIterator.next();
                    try {
                        // 假设Excel列顺序：用户名、密码、姓名、学号、班级、邮箱、电话
                        String username = activity.getCellValue(row.getCell(0));
                        String password = activity.getCellValue(row.getCell(1));
                        String name = activity.getCellValue(row.getCell(2));
                        String studentId = activity.getCellValue(row.getCell(3));
                        String className = activity.getCellValue(row.getCell(4));
                        String email = activity.getCellValue(row.getCell(5));
                        String phone = activity.getCellValue(row.getCell(6));

                        if (!username.isEmpty() && !password.isEmpty() && !name.isEmpty() && !studentId.isEmpty()) {
                            // 检查用户名是否已存在
                            Cursor existingCursor = activity.dbHelper.getUserByUsername(username);
                            if (existingCursor == null || existingCursor.getCount() == 0) {
                                // 加密密码
                                String encryptedPassword = SecurityUtils.encryptPassword(password);

                                // 保存学生到数据库
                                ContentValues values = new ContentValues();
                                values.put("username", username);
                                values.put("password", encryptedPassword);
                                values.put("name", name);
                                values.put("student_id", studentId);
                                values.put("class_name", className);
                                // 使用AES加密保护敏感数据
                values.put("email", SecurityUtils.encryptAES(email));
                values.put("phone", SecurityUtils.encryptAES(phone));
                                values.put("type", 0); // 0表示学生

                                activity.dbHelper.insertUser(values);
                                importedCount++;
                            }
                            if (existingCursor != null) {
                                existingCursor.close();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        continue;
                    }
                }

                workbook.close();
                inputStream.close();

            } catch (Exception e) {
                e.printStackTrace();
            }

            return importedCount;
        }

        @Override
        protected void onPostExecute(Integer result) {
            Toast.makeText(activity, "成功导入 " + result + " 名学生", Toast.LENGTH_SHORT).show();
            activity.loadStudents();
            activity.updateStudentStats();
        }
    }

    private String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf((int) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    private String getFileName(Uri uri) {
        String fileName = "";
        String scheme = uri.getScheme();
        if (scheme.equals(ContentResolver.SCHEME_CONTENT)) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (columnIndex != -1) {
                        fileName = cursor.getString(columnIndex);
                    }
                }
                cursor.close();
            }
        } else if (scheme.equals(ContentResolver.SCHEME_FILE)) {
            fileName = new File(uri.getPath()).getName();
        }
        return fileName;
    }

    private void showStudentDetails(int position) {
        int userId = studentIdList.get(position);
        Cursor cursor = dbHelper.getUserById(userId);
        if (cursor != null && cursor.moveToFirst()) {
            String username = "";
            int usernameIndex = cursor.getColumnIndex("username");
            if (usernameIndex != -1) {
                username = cursor.getString(usernameIndex);
            }
            String name = "";
            int nameIndex = cursor.getColumnIndex("name");
            if (nameIndex != -1) {
                name = cursor.getString(nameIndex);
            }
            String studentId = "";
            int studentIdIndex = cursor.getColumnIndex("student_id");
            if (studentIdIndex != -1) {
                studentId = cursor.getString(studentIdIndex);
            }
            String className = "";
            int classNameIndex = cursor.getColumnIndex("class_name");
            if (classNameIndex != -1) {
                className = cursor.getString(classNameIndex);
            }
            String email = "";
            int emailIndex = cursor.getColumnIndex("email");
            if (emailIndex != -1) {
                email = cursor.getString(emailIndex);
            }
            String phone = "";
            int phoneIndex = cursor.getColumnIndex("phone");
            if (phoneIndex != -1) {
                phone = cursor.getString(phoneIndex);
            }
            String major = "";
            int majorIndex = cursor.getColumnIndex("major");
            if (majorIndex != -1) {
                major = cursor.getString(majorIndex);
            }
            cursor.close();

            // 创建详细信息文本
            StringBuilder details = new StringBuilder();
            details.append("学号：").append(studentId).append("\n");
            details.append("用户名：").append(username).append("\n");
            details.append("姓名：").append(name).append("\n");
            details.append("班级：").append(className).append("\n");
            details.append("专业：").append(major).append("\n");
            details.append("邮箱：").append(email).append("\n");
            details.append("电话：").append(phone).append("\n");

            // 创建滚动视图以确保所有信息都能显示
            android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
            scrollView.setPadding(20, 20, 20, 20);

            TextView tvDetails = new TextView(this);
            tvDetails.setText(details.toString());
            tvDetails.setTextSize(16);
            tvDetails.setLineSpacing(8, 1.2f);
            scrollView.addView(tvDetails);

            // 创建对话框
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle(name + " 的详细信息");
            builder.setView(scrollView);
            builder.setPositiveButton("关闭", null);
            
            // 设置对话框尺寸
            android.app.AlertDialog dialog = builder.create();
            dialog.show();
            
            // 获取对话框的窗口并设置尺寸
            android.view.Window window = dialog.getWindow();
            if (window != null) {
                android.view.WindowManager.LayoutParams layoutParams = window.getAttributes();
                layoutParams.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.85);
                layoutParams.height = (int) (getResources().getDisplayMetrics().heightPixels * 0.7);
                window.setAttributes(layoutParams);
            }
        }
    }

    private void showStudentOptions(int position) {
        // 显示学生操作菜单
        String[] options = {"编辑学生信息", "重置密码", "删除学生", "查看学习情况", "发送提醒"};
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("学生操作")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            editStudent(position);
                            break;
                        case 1:
                            resetPassword(position);
                            break;
                        case 2:
                            deleteStudent(position);
                            break;
                        case 3:
                            viewStudyStatus(position);
                            break;
                        case 4:
                            sendReminder(position);
                            break;
                    }
                });
        builder.show();
    }

    private void sendReminder(int position) {
        // 向学生发送提醒通知
        int userId = studentIdList.get(position);
        Cursor userCursor = dbHelper.getUserById(userId);
        if (userCursor != null && userCursor.moveToFirst()) {
            String name = "";
            int nameIndex = userCursor.getColumnIndex("name");
            if (nameIndex != -1) {
                name = userCursor.getString(nameIndex);
            }
            userCursor.close();

            // 显示发送提醒对话框
            EditText etReminderContent = new EditText(this);
            etReminderContent.setHint("请输入提醒内容");
            etReminderContent.setPadding(20, 20, 20, 20);

            Spinner spReminderType = new Spinner(this);
            ArrayAdapter<CharSequence> reminderTypeAdapter = ArrayAdapter.createFromResource(this,
                    R.array.reminder_type_options, android.R.layout.simple_spinner_item);
            reminderTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spReminderType.setAdapter(reminderTypeAdapter);

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.addView(etReminderContent);
            layout.addView(spReminderType);

            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("向 " + name + " 发送提醒")
                    .setView(layout)
                    .setPositiveButton("发送", (dialog, which) -> {
                        String content = etReminderContent.getText().toString().trim();
                        String type = spReminderType.getSelectedItem().toString();

                        if (content.isEmpty()) {
                            Toast.makeText(StudentManageActivity.this, "请输入提醒内容", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // 保存提醒到数据库
                        ContentValues values = new ContentValues();
                        values.put("user_id", userId);
                        values.put("content", content);
                        values.put("type", type);
                        values.put("remind_time", System.currentTimeMillis());
                        values.put("is_sent", 0);

                        dbHelper.insertReminder(values);
                        Toast.makeText(StudentManageActivity.this, "提醒发送成功", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("取消", null);
            builder.show();
        }
    }

    private void showAddStudentDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_student, null);
        EditText etUsername = dialogView.findViewById(R.id.et_username);
        EditText etPassword = dialogView.findViewById(R.id.et_password);
        EditText etName = dialogView.findViewById(R.id.et_name);
        EditText etStudentId = dialogView.findViewById(R.id.et_student_id);
        EditText etClassName = dialogView.findViewById(R.id.et_class_name);
        EditText etMajor = dialogView.findViewById(R.id.et_major);
        EditText etEmail = dialogView.findViewById(R.id.et_email);
        EditText etPhone = dialogView.findViewById(R.id.et_phone);

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("添加学生")
                .setView(dialogView)
                .setPositiveButton("保存", (dialog, which) -> {
                    String username = etUsername.getText().toString().trim();
                    String password = etPassword.getText().toString().trim();
                    String name = etName.getText().toString().trim();
                    String studentId = etStudentId.getText().toString().trim();
                    String className = etClassName.getText().toString().trim();
                    String major = etMajor.getText().toString().trim();
                    String email = etEmail.getText().toString().trim();
                    String phone = etPhone.getText().toString().trim();

                    if (username.isEmpty()) {
                        Toast.makeText(StudentManageActivity.this, "请输入用户名", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (password.isEmpty()) {
                        Toast.makeText(StudentManageActivity.this, "请输入密码", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (name.isEmpty()) {
                        Toast.makeText(StudentManageActivity.this, "请输入姓名", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (studentId.isEmpty()) {
                        Toast.makeText(StudentManageActivity.this, "请输入学号", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (className.isEmpty()) {
                        Toast.makeText(StudentManageActivity.this, "请输入班级", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (major.isEmpty()) {
                        Toast.makeText(StudentManageActivity.this, "请输入专业", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Cursor existingCursor = dbHelper.getUserByUsername(username);
                    if (existingCursor != null && existingCursor.getCount() > 0) {
                        existingCursor.close();
                        Toast.makeText(StudentManageActivity.this, "用户名已存在", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (existingCursor != null) {
                        existingCursor.close();
                    }

                    String encryptedPassword = SecurityUtils.encryptPassword(password);

                    ContentValues values = new ContentValues();
                    values.put("username", username);
                    values.put("password", encryptedPassword);
                    values.put("name", name);
                    values.put("student_id", studentId);
                    values.put("class_name", className);
                    values.put("major", major);
                    values.put("email", email);
                    values.put("phone", phone);
                    values.put("type", 0);

                    dbHelper.insertUser(values);
                    Toast.makeText(StudentManageActivity.this, "学生添加成功", Toast.LENGTH_SHORT).show();
                    loadStudents();
                    updateStudentStats();
                })
                .setNegativeButton("取消", null);
        builder.show();
    }

    private void editStudent(int position) {
        int userId = studentIdList.get(position);
        Cursor cursor = dbHelper.getUserById(userId);
        if (cursor != null && cursor.moveToFirst()) {
            String username = "";
            int usernameIndex = cursor.getColumnIndex("username");
            if (usernameIndex != -1) {
                username = cursor.getString(usernameIndex);
            }
            String name = "";
            int nameIndex = cursor.getColumnIndex("name");
            if (nameIndex != -1) {
                name = cursor.getString(nameIndex);
            }
            String studentId = "";
            int studentIdIndex = cursor.getColumnIndex("student_id");
            if (studentIdIndex != -1) {
                studentId = cursor.getString(studentIdIndex);
            }
            String className = "";
            int classNameIndex = cursor.getColumnIndex("class_name");
            if (classNameIndex != -1) {
                className = cursor.getString(classNameIndex);
            }
            String major = "";
            int majorIndex = cursor.getColumnIndex("major");
            if (majorIndex != -1) {
                major = cursor.getString(majorIndex);
            }
            String email = "";
            int emailIndex = cursor.getColumnIndex("email");
            if (emailIndex != -1) {
                email = cursor.getString(emailIndex);
            }
            String phone = "";
            int phoneIndex = cursor.getColumnIndex("phone");
            if (phoneIndex != -1) {
                phone = cursor.getString(phoneIndex);
            }
            cursor.close();

            View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_student, null);
            EditText etUsername = dialogView.findViewById(R.id.et_username);
            EditText etPassword = dialogView.findViewById(R.id.et_password);
            EditText etName = dialogView.findViewById(R.id.et_name);
            EditText etStudentId = dialogView.findViewById(R.id.et_student_id);
            EditText etClassName = dialogView.findViewById(R.id.et_class_name);
            EditText etMajor = dialogView.findViewById(R.id.et_major);
            EditText etEmail = dialogView.findViewById(R.id.et_email);
            EditText etPhone = dialogView.findViewById(R.id.et_phone);

            etUsername.setText(username);
            etUsername.setEnabled(false);
            etPassword.setHint("不修改请留空");
            etName.setText(name);
            etStudentId.setText(studentId);
            etClassName.setText(className);
            etMajor.setText(major);
            etEmail.setText(email);
            etPhone.setText(phone);

            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("编辑学生信息")
                    .setView(dialogView)
                    .setPositiveButton("保存", (dialog, which) -> {
                        String newPassword = etPassword.getText().toString().trim();
                        String newName = etName.getText().toString().trim();
                        String newStudentId = etStudentId.getText().toString().trim();
                        String newClassName = etClassName.getText().toString().trim();
                        String newMajor = etMajor.getText().toString().trim();
                        String newEmail = etEmail.getText().toString().trim();
                        String newPhone = etPhone.getText().toString().trim();

                        if (newName.isEmpty()) {
                            Toast.makeText(StudentManageActivity.this, "请输入姓名", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (newStudentId.isEmpty()) {
                            Toast.makeText(StudentManageActivity.this, "请输入学号", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (newClassName.isEmpty()) {
                            Toast.makeText(StudentManageActivity.this, "请输入班级", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (newMajor.isEmpty()) {
                            Toast.makeText(StudentManageActivity.this, "请输入专业", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        ContentValues values = new ContentValues();
                        if (!newPassword.isEmpty()) {
                            String encryptedPassword = SecurityUtils.encryptPassword(newPassword);
                            values.put("password", encryptedPassword);
                        }
                        values.put("name", newName);
                        values.put("student_id", newStudentId);
                        values.put("class_name", newClassName);
                        values.put("major", newMajor);
                        values.put("email", newEmail);
                        values.put("phone", newPhone);

                        dbHelper.update("user", values, "id = ?", new String[]{String.valueOf(userId)});
                        Toast.makeText(StudentManageActivity.this, "学生信息更新成功", Toast.LENGTH_SHORT).show();
                        loadStudents();
                    })
                    .setNegativeButton("取消", null);
            builder.show();
        }
    }

    private void resetPassword(int position) {
        // 重置学生密码
        int userId = studentIdList.get(position);
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("重置密码")
                .setMessage("确定要将密码重置为默认值 '123456' 吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    // 加密默认密码
                    String encryptedPassword = SecurityUtils.encryptPassword("123456");
                    // 更新密码到数据库
                    ContentValues values = new ContentValues();
                    values.put("password", encryptedPassword);
                    dbHelper.update("user", values, "id = ?", new String[]{String.valueOf(userId)});
                    Toast.makeText(StudentManageActivity.this, "密码已重置为 '123456'", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null);
        builder.show();
    }

    private void deleteStudent(int position) {
        // 删除学生
        int userId = studentIdList.get(position);
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("确认删除")
                .setMessage("确定要删除这个学生账号吗？删除后相关数据也会被删除。")
                .setPositiveButton("确定", (dialog, which) -> {
                    // 从数据库中删除学生
                    dbHelper.delete("user", "id = ?", new String[]{String.valueOf(userId)});
                    // 删除相关的成绩
                    dbHelper.delete("grade", "student_id = ?", new String[]{String.valueOf(userId)});
                    // 删除相关的作业
                    dbHelper.delete("assignment", "user_id = ?", new String[]{String.valueOf(userId)});
                    // 删除相关的学习计划
                    dbHelper.delete("study_plan", "user_id = ?", new String[]{String.valueOf(userId)});
                    Toast.makeText(StudentManageActivity.this, "学生账号已删除", Toast.LENGTH_SHORT).show();
                    loadStudents(); // 重新加载学生列表
                    updateStudentStats(); // 重新更新统计数据
                })
                .setNegativeButton("取消", null);
        builder.show();
    }

    private void viewStudyStatus(int position) {
        // 查看学生学习情况
        int userId = studentIdList.get(position);
        Cursor userCursor = dbHelper.getUserById(userId);
        if (userCursor != null && userCursor.moveToFirst()) {
            String name = "";
            int nameIndex = userCursor.getColumnIndex("name");
            if (nameIndex != -1) {
                name = userCursor.getString(nameIndex);
            }
            userCursor.close();

            // 统计学生的学习数据
            int totalAssignments = 0;
            int completedAssignments = 0;
            int totalCourses = 0;
            double averageScore = 0;

            // 统计作业
            Cursor assignmentCursor = dbHelper.query("assignment", null, "user_id = ?", new String[]{String.valueOf(userId)}, null, null, null);
            if (assignmentCursor != null) {
                totalAssignments = assignmentCursor.getCount();
                while (assignmentCursor.moveToNext()) {
                    int status = 0;
                int statusIndex = assignmentCursor.getColumnIndex("status");
                if (statusIndex != -1) {
                    status = assignmentCursor.getInt(statusIndex);
                }
                if (status == 2) { // 已完成
                    completedAssignments++;
                }
                }
                assignmentCursor.close();
            }

            // 统计课程
            Cursor courseCursor = dbHelper.query("course", null, "user_id = ?", new String[]{String.valueOf(userId)}, null, null, null);
            if (courseCursor != null) {
                totalCourses = courseCursor.getCount();
                courseCursor.close();
            }

            // 统计平均成绩
            double totalScore = 0;
            int totalGrades = 0;
            Cursor gradeCursor = dbHelper.query("grade", new String[]{"score"}, "student_id = ?", new String[]{String.valueOf(userId)}, null, null, null);
            if (gradeCursor != null) {
                while (gradeCursor.moveToNext()) {
                    int scoreIndex = gradeCursor.getColumnIndex("score");
                if (scoreIndex != -1) {
                    totalScore += gradeCursor.getDouble(scoreIndex);
                }
                    totalGrades++;
                }
                gradeCursor.close();
            }
            if (totalGrades > 0) {
                averageScore = totalScore / totalGrades;
            }

            // 构建学习情况信息
            StringBuilder studyStatus = new StringBuilder();
            studyStatus.append("课程数量：").append(totalCourses).append("门\n");
            studyStatus.append("作业总数：").append(totalAssignments).append("个\n");
            studyStatus.append("已完成作业：").append(completedAssignments).append("个\n");
            studyStatus.append("作业完成率：").append(totalAssignments > 0 ? String.format(Locale.ROOT, "%.2f", (double) completedAssignments / totalAssignments * 100) : "0").append("%\n");
            studyStatus.append("平均成绩：").append(String.format(Locale.ROOT, "%.2f", averageScore)).append("分\n");

            // 创建一个自定义对话框来显示学习情况
            TextView tvStatusInfo = new TextView(this);
            tvStatusInfo.setText(studyStatus.toString());
            tvStatusInfo.setPadding(20, 20, 20, 20);
            tvStatusInfo.setTextSize(14);

            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle(name + " 的学习情况")
                    .setView(tvStatusInfo)
                    .setPositiveButton("关闭", null);
            builder.show();
        }
    }



    private void filterStudents() {
        String selectedClass = spFilterClass.getSelectedItem().toString();
        if (selectedClass.equals("全部班级")) {
            loadStudents();
            return;
        }

        studentList.clear();
        studentIdList.clear();

        Cursor cursor = dbHelper.query("user", null, "type = ? AND class_name = ?", 
                new String[]{"0", selectedClass}, null, null, "username ASC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int userId = 0;
                int idIndex = cursor.getColumnIndex("id");
                if (idIndex != -1) {
                    userId = cursor.getInt(idIndex);
                }
                String username = "";
                int usernameIndex = cursor.getColumnIndex("username");
                if (usernameIndex != -1) {
                    username = cursor.getString(usernameIndex);
                }
                String name = "";
                int nameIndex = cursor.getColumnIndex("name");
                if (nameIndex != -1) {
                    name = cursor.getString(nameIndex);
                }
                String studentId = "";
                int studentIdIndex = cursor.getColumnIndex("student_id");
                if (studentIdIndex != -1) {
                    studentId = cursor.getString(studentIdIndex);
                }
                String className = "";
                int classNameIndex = cursor.getColumnIndex("class_name");
                if (classNameIndex != -1) {
                    className = cursor.getString(classNameIndex);
                }
                String email = "";
                int emailIndex = cursor.getColumnIndex("email");
                if (emailIndex != -1) {
                    email = cursor.getString(emailIndex);
                }
                String phone = "";
                int phoneIndex = cursor.getColumnIndex("phone");
                if (phoneIndex != -1) {
                    phone = cursor.getString(phoneIndex);
                }

                String studentInfo = "学号：" + studentId + "\n" +
                        "用户名：" + username + "\n" +
                        "姓名：" + name + "\n" +
                        "班级：" + className + "\n" +
                        "邮箱：" + email + "\n" +
                        "电话：" + phone;
                studentList.add(studentInfo);
                studentIdList.add(userId);
            }
            cursor.close();
        }
        studentAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}
