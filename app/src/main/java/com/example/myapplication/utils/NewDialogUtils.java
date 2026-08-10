package com.example.myapplication.utils;

import android.app.AlertDialog;
import android.content.Context;

public class NewDialogUtils {

    public static void showSuccessDialog(Context context, String message) {
        new AlertDialog.Builder(context)
                .setTitle("成功")
                .setMessage(message)
                .setPositiveButton("确定", null)
                .show();
    }

    public static void showErrorDialog(Context context, String message) {
        new AlertDialog.Builder(context)
                .setTitle("错误")
                .setMessage(message)
                .setPositiveButton("确定", null)
                .show();
    }

    public static void showWarningDialog(Context context, String message) {
        new AlertDialog.Builder(context)
                .setTitle("警告")
                .setMessage(message)
                .setPositiveButton("确定", null)
                .show();
    }

    public static void showInfoDialog(Context context, String message) {
        new AlertDialog.Builder(context)
                .setTitle("提示")
                .setMessage(message)
                .setPositiveButton("确定", null)
                .show();
    }

    public static void handleActivityResult(int requestCode, int resultCode) {
    }
}
