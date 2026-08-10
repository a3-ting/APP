package com.example.myapplication.utils;

import java.util.Collections;
import java.util.List;

public class GradeAnalysisHelper {

    public static double calculateAverage(List<Double> scores) {
        if (scores == null || scores.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (Double score : scores) {
            if (score != null) {
                sum += score;
            }
        }
        return sum / scores.size();
    }

    public static double calculateMax(List<Double> scores) {
        if (scores == null || scores.isEmpty()) {
            return 0.0;
        }
        double max = Double.MIN_VALUE;
        for (Double score : scores) {
            if (score != null && score > max) {
                max = score;
            }
        }
        return max == Double.MIN_VALUE ? 0.0 : max;
    }

    public static double calculateMin(List<Double> scores) {
        if (scores == null || scores.isEmpty()) {
            return 0.0;
        }
        double min = Double.MAX_VALUE;
        for (Double score : scores) {
            if (score != null && score < min) {
                min = score;
            }
        }
        return min == Double.MAX_VALUE ? 0.0 : min;
    }

    public static double calculatePassRate(List<Double> scores, double passLine) {
        if (scores == null || scores.isEmpty()) {
            return 0.0;
        }
        int passCount = 0;
        int totalCount = 0;
        for (Double score : scores) {
            if (score != null) {
                totalCount++;
                if (score >= passLine) {
                    passCount++;
                }
            }
        }
        return totalCount == 0 ? 0.0 : (double) passCount / totalCount;
    }

    public static String getGradeLevel(double score) {
        if (score >= 90) {
            return "优秀";
        } else if (score >= 80) {
            return "良好";
        } else if (score >= 70) {
            return "中等";
        } else if (score >= 60) {
            return "及格";
        } else {
            return "不及格";
        }
    }
}
