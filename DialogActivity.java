package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class DialogActivity extends AppCompatActivity {

    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_TYPE = "type";
    public static final String EXTRA_POSITIVE_BUTTON = "positive_button";
    public static final String EXTRA_NEGATIVE_BUTTON = "negative_button";
    public static final String EXTRA_CANCELABLE = "cancelable";

    public static final int TYPE_INFO = 0;
    public static final int TYPE_SUCCESS = 1;
    public static final int TYPE_ERROR = 2;
    public static final int TYPE_WARNING = 3;
    public static final int TYPE_CONFIRM = 4;

    private TextView tvTitle, tvMessage;
    private Button btnPositive, btnNegative;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog);

        initViews();
        setupData();
        setupListeners();
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tv_dialog_title);
        tvMessage = findViewById(R.id.tv_dialog_message);
        btnPositive = findViewById(R.id.btn_dialog_positive);
        btnNegative = findViewById(R.id.btn_dialog_negative);
    }

    private void setupData() {
        Intent intent = getIntent();
        if (intent != null) {
            // 设置标题
            String title = intent.getStringExtra(EXTRA_TITLE);
            if (title != null) {
                tvTitle.setText(title);
            }

            // 设置消息
            String message = intent.getStringExtra(EXTRA_MESSAGE);
            if (message != null) {
                tvMessage.setText(message);
            }

            // 设置按钮文本
            String positiveButton = intent.getStringExtra(EXTRA_POSITIVE_BUTTON);
            if (positiveButton != null) {
                btnPositive.setText(positiveButton);
            }

            String negativeButton = intent.getStringExtra(EXTRA_NEGATIVE_BUTTON);
            if (negativeButton != null) {
                btnNegative.setText(negativeButton);
                btnNegative.setVisibility(View.VISIBLE);
            } else {
                btnNegative.setVisibility(View.GONE);
            }

            // 设置弹窗类型（影响样式）
            int type = intent.getIntExtra(EXTRA_TYPE, TYPE_INFO);
            setupDialogType(type);

            // 设置是否可取消
            boolean cancelable = intent.getBooleanExtra(EXTRA_CANCELABLE, true);
            setFinishOnTouchOutside(cancelable);
        }
    }

    private void setupDialogType(int type) {
        // 根据类型设置不同的样式
        switch (type) {
            case TYPE_SUCCESS:
                tvTitle.setTextColor(getResources().getColor(R.color.success));
                break;
            case TYPE_ERROR:
                tvTitle.setTextColor(getResources().getColor(R.color.error));
                break;
            case TYPE_WARNING:
                tvTitle.setTextColor(getResources().getColor(R.color.warning));
                break;
            default:
                tvTitle.setTextColor(getResources().getColor(R.color.info));
                break;
        }
    }

    private void setupListeners() {
        btnPositive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_OK);
                finish();
            }
        });

        btnNegative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }

    @Override
    public void onBackPressed() {
        // 允许通过返回键关闭弹窗
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }
}
