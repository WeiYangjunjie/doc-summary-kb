package com.example.dockb.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SnippetUtil 单元测试。
 */
class SnippetUtilTest {

    @Test
    void nullText_returnsEmpty() {
        assertEquals("", SnippetUtil.snippet(null, "keyword", 20));
    }

    @Test
    void emptyText_returnsEmpty() {
        assertEquals("", SnippetUtil.snippet("", "keyword", 20));
    }

    @Test
    void nullKeyword_returnsClip() {
        String text = "这是一段测试文本内容。";
        String result = SnippetUtil.snippet(text, null, 10);
        assertFalse(result.isEmpty());
        assertTrue(result.length() <= 20 + 4); // radius*2 + ellipsis
    }

    @Test
    void emptyKeyword_returnsClip() {
        String text = "这是一段测试文本内容。";
        String result = SnippetUtil.snippet(text, "", 10);
        assertFalse(result.isEmpty());
    }

    @Test
    void keywordFound_returnsSnippetAround() {
        String text = "前导内容。关键词在中间。后导内容。";
        String result = SnippetUtil.snippet(text, "关键词", 5);
        assertTrue(result.contains("关键词"));
        assertTrue(result.startsWith("…") || !result.startsWith("前"));
        assertTrue(result.endsWith("…") || !result.endsWith("。"));
    }

    @Test
    void keywordCaseInsensitive() {
        String text = "这是MiniMax的内容。";
        String result = SnippetUtil.snippet(text, "minimax", 5);
        assertTrue(result.contains("MiniMax"));
    }

    @Test
    void keywordNotFound_returnsClip() {
        String text = "这是一段完全不相关的文本。";
        String result = SnippetUtil.snippet(text, "关键词不存在", 10);
        // 应该 clip 到文本开头附近
        assertFalse(result.isEmpty());
    }

    @Test
    void keywordAtStart() {
        String text = "关键词后面有很多内容。";
        String result = SnippetUtil.snippet(text, "关键词", 5);
        assertTrue(result.startsWith("关键词"));
        // 开头不应有省略号
        assertFalse(result.startsWith("…关键词"));
    }

    @Test
    void keywordAtEnd() {
        String text = "前面有很多内容，关键词";
        String result = SnippetUtil.snippet(text, "关键词", 5);
        assertTrue(result.contains("关键词"));
    }

    @Test
    void radiusZero_returnsKeywordOnly() {
        String text = "前面内容关键词后面内容";
        String result = SnippetUtil.snippet(text, "关键词", 0);
        assertTrue(result.contains("关键词"));
        assertTrue(result.length() <= 3); // 关键词本身
    }

    @Test
    void textShorterThan2radius_noEllipsis() {
        String text = "短文";
        String result = SnippetUtil.snippet(text, "短", 10);
        assertFalse(result.contains("…"), "short text should not have ellipsis: " + result);
    }

    @Test
    void largeRadius_handlesGracefully() {
        String text = "短文本";
        String result = SnippetUtil.snippet(text, "文", 100);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("短文本") || result.contains("文"));
    }
}
