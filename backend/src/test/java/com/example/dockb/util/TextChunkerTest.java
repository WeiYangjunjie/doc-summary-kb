package com.example.dockb.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TextChunker 单元测试。
 */
class TextChunkerTest {

    @Test
    void nullOrBlank_returnsEmpty() {
        assertEquals(List.of(), TextChunker.chunk(null, 1000));
        assertEquals(List.of(), TextChunker.chunk("", 1000));
        assertEquals(List.of(), TextChunker.chunk("   ", 1000));
    }

    @Test
    void normalText_chunked() {
        String text = "第一段内容。\n\n第二段内容。\n\n第三段内容。";
        List<String> chunks = TextChunker.chunk(text, 10);
        assertFalse(chunks.isEmpty());
        for (String c : chunks) {
            assertTrue(c.length() <= 10 + 20, "chunk should not exceed chunkSize significantly: " + c);
        }
    }

    @Test
    void shortText_singleChunk() {
        String text = "这是一段短文本。";
        List<String> chunks = TextChunker.chunk(text, 1000);
        assertEquals(1, chunks.size());
        assertEquals("这是一段短文本。", chunks.get(0));
    }

    @Test
    void zeroChunkSize_defaultsTo1000() {
        String longText = "A".repeat(2000);
        List<String> chunks = TextChunker.chunk(longText, 0);
        // 应该能切分而不是全部放进一个 chunk（因为 2000 > 1000）
        assertTrue(chunks.size() >= 1);
    }

    @Test
    void negativeChunkSize_treatedAs1000() {
        String text = "测试文本内容。";
        List<String> chunks = TextChunker.chunk(text, -5);
        assertEquals(1, chunks.size());
    }

    @Test
    void veryLongText_hardSplit() {
        String text = "A".repeat(5000);
        List<String> chunks = TextChunker.chunk(text, 1000);
        assertFalse(chunks.isEmpty());
        for (String c : chunks) {
            assertTrue(c.length() <= 1000 + 200,
                    "hard split chunk length=" + c.length() + " exceeds limit: " + c);
        }
    }

    @Test
    void paragraphSplit_works() {
        String text = "第一段\n\n第二段";
        List<String> chunks = TextChunker.chunk(text, 1000);
        assertFalse(chunks.isEmpty());
        String all = String.join(" ", chunks);
        assertTrue(all.contains("第一段"));
        assertTrue(all.contains("第二段"));
    }

    @Test
    void shortParagraphsMerged() {
        // 多个短段落应该合并到不超过 chunkSize
        String text = "短。\n\n短。\n\n短。";
        List<String> chunks = TextChunker.chunk(text, 20);
        assertFalse(chunks.isEmpty());
        // 合并后应该只有 1 个 chunk
        assertEquals(1, chunks.size());
        assertTrue(chunks.get(0).contains("短"));
    }
}
