package com.example.dockb.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 文本分块工具：
 *
 * <ol>
 *   <li>先按段落（\n\n 或 \r\n\r\n）切分；</li>
 *   <li>每个段落若长度 ≤ chunkSize 则作为一段；</li>
 *   <li>若段落过长，按句号/问号/感叹号/换行截断后合并到 chunkSize 以内；</li>
 *   <li>短段落若合并后仍不超过 chunkSize 则合并。</li>
 * </ol>
 */
public final class TextChunker {

    private static final Pattern PARA_SPLIT = Pattern.compile("\\r?\\n\\r?\\n");
    private static final Pattern SENT_SPLIT = Pattern.compile("(?<=[。！？!?；;\\n])");

    private TextChunker() {
    }

    public static List<String> chunk(String text, int chunkSize) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        if (chunkSize <= 0) {
            chunkSize = 1000;
        }
        // 1) 段落切分
        String[] paragraphs = PARA_SPLIT.split(text.trim());
        List<String> pieces = new ArrayList<>();
        for (String p : paragraphs) {
            String trimmed = p.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.length() <= chunkSize) {
                pieces.add(trimmed);
            } else {
                pieces.addAll(splitLong(trimmed, chunkSize));
            }
        }

        // 2) 短段落合并到不超过 chunkSize
        List<String> merged = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String p : pieces) {
            if (cur.length() == 0) {
                cur.append(p);
            } else if (cur.length() + p.length() + 2 <= chunkSize) {
                cur.append("\n\n").append(p);
            } else {
                merged.add(cur.toString());
                cur.setLength(0);
                cur.append(p);
            }
        }
        if (cur.length() > 0) {
            merged.add(cur.toString());
        }
        return merged;
    }

    private static List<String> splitLong(String para, int chunkSize) {
        List<String> out = new ArrayList<>();
        String[] sentences = SENT_SPLIT.split(para);
        StringBuilder cur = new StringBuilder();
        for (String s : sentences) {
            String t = s.trim();
            if (t.isEmpty()) {
                continue;
            }
            // 单句过长：硬切
            if (t.length() > chunkSize) {
                if (cur.length() > 0) {
                    out.add(cur.toString());
                    cur.setLength(0);
                }
                for (int i = 0; i < t.length(); i += chunkSize) {
                    int end = Math.min(i + chunkSize, t.length());
                    out.add(t.substring(i, end));
                }
                continue;
            }
            if (cur.length() == 0) {
                cur.append(t);
            } else if (cur.length() + t.length() <= chunkSize) {
                cur.append(t);
            } else {
                out.add(cur.toString());
                cur.setLength(0);
                cur.append(t);
            }
        }
        if (cur.length() > 0) {
            out.add(cur.toString());
        }
        return out;
    }
}