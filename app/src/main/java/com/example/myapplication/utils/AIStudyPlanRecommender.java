package com.example.myapplication.utils;

import java.util.ArrayList;
import java.util.List;

public class AIStudyPlanRecommender {

    public static List<String> generateStudyPlan(String userProfile) {
        List<String> plan = new ArrayList<>();
        plan.add("每日早晨复习昨日知识点，约30分钟");
        plan.add("上午学习新内容，约2小时");
        plan.add("下午做练习题巩固，约1.5小时");
        plan.add("晚上进行总结和预习，约1小时");
        plan.add("每周日进行本周内容总复习");
        return plan;
    }

    public static List<String> recommendCourses(String interest, String level) {
        List<String> courses = new ArrayList<>();
        if (interest != null) {
            if (interest.contains("数学") || interest.contains("Math")) {
                courses.add("高等数学基础");
                courses.add("线性代数入门");
                courses.add("概率论与数理统计");
            } else if (interest.contains("编程") || interest.contains("代码") || interest.contains("程序")) {
                courses.add("Java 程序设计");
                courses.add("数据结构与算法");
                courses.add("Android 开发实战");
            } else if (interest.contains("英语") || interest.contains("English")) {
                courses.add("大学英语精读");
                courses.add("英语口语提升");
                courses.add("英语写作技巧");
            } else {
                courses.add("通用学习方法课程");
                courses.add("时间管理技巧");
                courses.add("思维导图入门");
            }
        }
        if (level != null && level.equals("高级")) {
            courses.add("进阶专题课程");
            courses.add("项目实战训练");
        }
        return courses;
    }

    public static List<String> adjustStudyPlan(List<String> currentPlan, float progress) {
        List<String> adjustedPlan = new ArrayList<>();
        if (currentPlan != null) {
            adjustedPlan.addAll(currentPlan);
        }
        if (progress < 0.5f) {
            adjustedPlan.add("建议增加每天学习时间，加快进度");
            adjustedPlan.add("适当减少休息时间，集中精力完成任务");
        } else if (progress > 0.9f) {
            adjustedPlan.add("进度良好，可以适当增加拓展内容");
            adjustedPlan.add("可以提前预习下一阶段内容");
        } else {
            adjustedPlan.add("保持当前节奏，稳步推进");
        }
        return adjustedPlan;
    }
}
