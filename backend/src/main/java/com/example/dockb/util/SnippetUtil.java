package com.example.dockb.util;

/**
 * 高亮片段工具：在给定文本中找到 keyword 第一次出现位置，前后各取 radius 个字符。
 */
public final class SnippetUtil {

    private SnippetUtil() {
    }

    public static String snippet(String text, String keyword, int radius) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (keyword == null || keyword.isEmpty()) {
            return clip(text, 0, radius);
        }
        int idx = indexOfIgnoreCase(text, keyword);
        if (idx < 0) {
            return clip(text, 0, radius);
        }
        int start = Math.max(0, idx - radius);
        int end = Math.min(text.length(), idx + keyword.length() + radius);
        StringBuilder sb = new StringBuilder();
        if (start > 0) {
            sb.append("…");
        }
        sb.append(text, start, end);
        if (end < text.length()) {
            sb.append("…");
        }
        return sb.toString();
    }

    private static String clip(String text, int start, int radius) {
        int end = Math.min(text.length(), start + radius * 2);
        StringBuilder sb = new StringBuilder();
        if (start > 0) {
            sb.append("…");
        }
        sb.append(text, start, end);
        if (end < text.length()) {
            sb.append("…");
        }
        return sb.toString();
    }

    private static int indexOfIgnoreCase(String text, String keyword) {
        int n = text.length() - keyword.length();
        for (int i = 0; i <= n; i++) {
            if (regionMatchesIgnoreCase(text, i, keyword)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean regionMatchesIgnoreCase(String text, int offset, String other) {
        if (offset < 0 || other.length() > text.length() - offset) {
            return false;
        }
        for (int i = 0; i < other.length(); i++) {
            char a = Character.toLowerCase(text.charAt(offset + i));
            char b = Character.toLowerCase(other.charAt(i));
            if (a != b) {
                return false;
            }
        }
        return true;
    }
}