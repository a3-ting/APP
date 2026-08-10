package com.example.myapplication.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SiliconFlowService {

    private static final boolean SERVICE_AVAILABLE = true;
    private static final String[] DEFAULT_REPLIES = {
            "我理解你的问题，让我为你详细解答...",
            "这是一个很好的问题！以下是我的建议...",
            "根据你的描述，我认为你可以这样做...",
            "感谢你的提问！让我来帮你分析一下...",
            "好的，我来为你提供相关的信息和建议..."
    };

    public static String sendMessage(String userMessage, List<String> history) {
        return generateResponse(userMessage);
    }

    public static String generateResponse(String prompt) {
        if (prompt == null || prompt.isEmpty()) {
            return "请输入有效的问题内容。";
        }
        Random random = new Random();
        int index = random.nextInt(DEFAULT_REPLIES.length);
        String prefix = DEFAULT_REPLIES[index];
        return prefix + "\n\n关于「" + prompt + "」的智能回复内容（模拟）。";
    }

    public static boolean isServiceAvailable() {
        return SERVICE_AVAILABLE;
    }
}
