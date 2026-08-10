package com.example.myapplication.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StudyDataAnalyzer {

    public static class AnalysisResult {
        private double totalHours;
        private double averageHours;
        private String mostProductiveDay;
        private List<String> suggestions;

        public AnalysisResult() {
            this.totalHours = 0.0;
            this.averageHours = 0.0;
            this.mostProductiveDay = "";
            this.suggestions = new ArrayList<>();
        }

        public double getTotalHours() {
            return totalHours;
        }

        public void setTotalHours(double totalHours) {
            this.totalHours = totalHours;
        }

        public double getAverageHours() {
            return averageHours;
        }

        public void setAverageHours(double averageHours) {
            this.averageHours = averageHours;
        }

        public String getMostProductiveDay() {
            return mostProductiveDay;
        }

        public void setMostProductiveDay(String mostProductiveDay) {
            this.mostProductiveDay = mostProductiveDay;
        }

        public List<String> getSuggestions() {
            return suggestions;
        }

        public void setSuggestions(List<String> suggestions) {
            this.suggestions = suggestions;
        }
    }

    public static AnalysisResult analyzeStudyHours(Map<String, Integer> data) {
        AnalysisResult result = new AnalysisResult();
        if (data == null || data.isEmpty()) {
            return result;
        }

        int totalHours = 0;
        String mostDay = "";
        int maxHours = 0;

        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            int hours = entry.getValue() != null ? entry.getValue() : 0;
            totalHours += hours;
            if (hours > maxHours) {
                maxHours = hours;
                mostDay = entry.getKey();
            }
        }

        result.setTotalHours(totalHours);
        result.setAverageHours((double) totalHours / data.size());
        result.setMostProductiveDay(mostDay);

        List<String> suggestions = new ArrayList<>();
        if (result.getAverageHours() < 2) {
            suggestions.add("建议每天增加学习时间，至少保证2小时以上");
        }
        if (maxHours > 8) {
            suggestions.add("注意劳逸结合，单次学习时间不宜过长");
        }
        result.setSuggestions(suggestions);

        return result;
    }

    public static double calculateCompletionRate(int completed, int total) {
        if (total <= 0) {
            return 0.0;
        }
        return (double) completed / total;
    }

    public static List<String> generateStudySuggestions(List<String> data) {
        List<String> suggestions = new ArrayList<>();
        if (data == null || data.isEmpty()) {
            suggestions.add("请先完善学习数据记录");
            return suggestions;
        }
        suggestions.add("建议制定详细的学习计划，按部就班执行");
        suggestions.add("定期复习已学内容，巩固知识点");
        suggestions.add("适当做练习题，检验学习效果");
        return suggestions;
    }
}
