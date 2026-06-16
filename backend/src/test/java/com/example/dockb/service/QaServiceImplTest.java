package com.example.dockb.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dockb.client.M3Client;
import com.example.dockb.client.dto.QaResult;
import com.example.dockb.client.dto.RankedHit;
import com.example.dockb.common.BizException;
import com.example.dockb.common.PageResult;
import com.example.dockb.config.AppProperties;
import com.example.dockb.entity.Document;
import com.example.dockb.entity.DocumentChunk;
import com.example.dockb.entity.QaHistory;
import com.example.dockb.mapper.DocumentChunkMapper;
import com.example.dockb.mapper.DocumentMapper;
import com.example.dockb.mapper.QaHistoryMapper;
import com.example.dockb.service.impl.QaServiceImpl;
import com.example.dockb.vo.QaAnswerVO;
import com.example.dockb.vo.QaHistoryVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * QaServiceImpl 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class QaServiceImplTest {

    @Mock
    private DocumentChunkMapper chunkMapper;
    @Mock
    private DocumentMapper documentMapper;
    @Mock
    private QaHistoryMapper qaHistoryMapper;
    @Mock
    private M3Service m3Service;
    @Mock
    private AppProperties appProperties;
    @Mock
    private AppProperties.SearchConfig searchConfig;

    private QaServiceImpl service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        service = new QaServiceImpl(chunkMapper, documentMapper, qaHistoryMapper, m3Service, appProperties, objectMapper);
    }

    @Test
    void ask_emptyQuestion_throws() {
        assertThrows(BizException.class, () -> service.ask(null, 5));
        assertThrows(BizException.class, () -> service.ask("   ", 5));
    }

    @Test
    void ask_noCandidates_returnsNoData() {
        when(appProperties.getSearch()).thenReturn(searchConfig);
        when(searchConfig.getMaxCandidates()).thenReturn(30);
        when(chunkMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        QaAnswerVO result = service.ask("未知问题", 5);

        assertNotNull(result);
        assertTrue(result.getAnswer().contains("暂无相关资料"));
        assertTrue(result.getCitations().isEmpty());
    }

    @Test
    void ask_withCandidates_returnsAnswer() {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setId(1L);
        chunk.setDocumentId(10L);
        chunk.setContent("MiniMax M3 是一个大模型。");

        when(appProperties.getSearch()).thenReturn(searchConfig);
        when(searchConfig.getMaxCandidates()).thenReturn(30);
        when(searchConfig.getSnippetRadius()).thenReturn(80);
        when(chunkMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(chunk));
        when(m3Service.rerankWithFallback(eq("M3是什么"), anyList()))
                .thenReturn(List.of(new RankedHit(0, 0.95)));
        when(m3Service.answerWithFallback(eq("M3是什么"), anyList()))
                .thenReturn(new QaResult("MiniMax M3 是大模型。", Collections.emptyList()));
        when(documentMapper.selectBatchIds(anyList())).thenReturn(List.of());

        QaAnswerVO result = service.ask("M3是什么", 5);

        assertNotNull(result);
        assertEquals("MiniMax M3 是大模型。", result.getAnswer());
    }

    @Test
    void buildContext_emptyQuestion_returnsEmpty() {
        assertTrue(service.buildContext(null, 5).isEmpty());
        assertTrue(service.buildContext("  ", 5).isEmpty());
    }

    @Test
    void buildContext_noCandidates_returnsEmpty() {
        when(appProperties.getSearch()).thenReturn(searchConfig);
        when(searchConfig.getMaxCandidates()).thenReturn(30);
        when(chunkMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        var ctx = service.buildContext("问题", 5);

        assertTrue(ctx.isEmpty());
    }

    @Test
    void buildContext_returnsTopKContent() {
        DocumentChunk c1 = mkChunk(1L, 10L, "内容一");
        DocumentChunk c2 = mkChunk(2L, 10L, "内容二");
        DocumentChunk c3 = mkChunk(3L, 10L, "内容三");

        when(appProperties.getSearch()).thenReturn(searchConfig);
        when(searchConfig.getMaxCandidates()).thenReturn(30);
        when(chunkMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(c1, c2, c3));

        var ctx = service.buildContext("问题", 2);

        assertEquals(2, ctx.size());
        assertEquals("内容一", ctx.get(0));
        assertEquals("内容二", ctx.get(1));
    }

    @Test
    void buildContext_respectsTopKBound() {
        when(appProperties.getSearch()).thenReturn(searchConfig);
        when(searchConfig.getMaxCandidates()).thenReturn(30);
        when(chunkMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(mkChunk(1L, 10L, "A")));

        var ctx = service.buildContext("问题", 100); // 超过上限 20

        // buildContext 内部 clamp 到 20
        assertEquals(1, ctx.size());
    }

    @Test
    void history_empty_returnsEmptyPage() {
        Page<QaHistory> emptyPage = new Page<>(1, 10);
        emptyPage.setRecords(List.of());
        emptyPage.setTotal(0);
        when(qaHistoryMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(emptyPage);

        PageResult<QaHistoryVO> result = service.history(1, 10);

        assertEquals(0, result.getTotal());
        assertTrue(result.getList().isEmpty());
    }

    @Test
    void history_withRecords_returnsVOPage() {
        QaHistory h = new QaHistory();
        h.setId(1L);
        h.setQuestion("什么是 M3？");
        h.setAnswer("M3 是模型。");
        h.setCitations("[]");
        h.setCreatedAt(LocalDateTime.now());

        Page<QaHistory> page = new Page<>(1, 10);
        page.setRecords(List.of(h));
        page.setTotal(1);

        when(qaHistoryMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        PageResult<QaHistoryVO> result = service.history(1, 10);

        assertEquals(1, result.getTotal());
        assertEquals("什么是 M3？", result.getList().get(0).getQuestion());
        assertEquals("M3 是模型。", result.getList().get(0).getAnswer());
    }

    @Test
    void saveHistoryAsync_handlesNullGracefully() {
        // 不应抛异常
        assertDoesNotThrow(() -> service.saveHistoryAsync(null, null));
        assertDoesNotThrow(() -> service.saveHistoryAsync("问题", null));
        assertDoesNotThrow(() -> service.saveHistoryAsync(null, "答案"));
    }

    // ---- helper ----

    private DocumentChunk mkChunk(Long id, Long docId, String content) {
        DocumentChunk c = new DocumentChunk();
        c.setId(id);
        c.setDocumentId(docId);
        c.setContent(content);
        return c;
    }
}
