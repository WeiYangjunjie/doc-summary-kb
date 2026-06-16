package com.example.dockb.controller;

import com.example.dockb.client.M3Client;
import com.example.dockb.client.dto.QaResult;
import com.example.dockb.common.PageResult;
import com.example.dockb.config.ai.ModelRegistry;
import com.example.dockb.dto.QaAskRequest;
import com.example.dockb.entity.DocumentChunk;
import com.example.dockb.mapper.DocumentChunkMapper;
import com.example.dockb.mapper.DocumentMapper;
import com.example.dockb.mapper.QaHistoryMapper;
import com.example.dockb.service.impl.QaServiceImpl;
import com.example.dockb.service.impl.M3ServiceImpl;
import com.example.dockb.config.AppProperties;
import com.example.dockb.config.M3Properties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * QaController 集成测试（Mock 所有依赖）。
 */
@WebMvcTest(controllers = QaController.class)
@Import({QaServiceImpl.class, M3ServiceImpl.class, M3Properties.class,
        AppProperties.class, ObjectMapper.class,
        com.example.dockb.common.GlobalExceptionHandler.class})
@AutoConfigureMockMvc
class QaControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private M3Client m3Client;

    @MockBean
    private ModelRegistry modelRegistry;

    @MockBean
    private DocumentChunkMapper chunkMapper;

    @MockBean
    private DocumentMapper documentMapper;

    @MockBean
    private QaHistoryMapper qaHistoryMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void ask_returnsAnswer() throws Exception {
        when(m3Client.answer(anyString(), anyList()))
                .thenReturn(new QaResult("M3 是一个模型。", Collections.emptyList()));

        QaAskRequest req = new QaAskRequest();
        req.setQuestion("M3 是什么？");
        req.setTopK(5);

        mockMvc.perform(post("/api/qa/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", org.hamcrest.Matchers.is(0)))
                .andExpect(jsonPath("$.data.answer", org.hamcrest.Matchers.containsString("模型")));
    }

    @Test
    void ask_emptyQuestion_returns400() throws Exception {
        QaAskRequest req = new QaAskRequest();
        req.setQuestion("");

        mockMvc.perform(post("/api/qa/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void history_empty_returnsEmptyPage() throws Exception {
        mockMvc.perform(get("/api/qa/history").param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", org.hamcrest.Matchers.is(0)))
                .andExpect(jsonPath("$.data.total", org.hamcrest.Matchers.is(0)));
    }

    @Test
    void askStream_returnsSSE() throws Exception {
        // Mock buildContext 返回上下文
        when(chunkMapper.selectList(any())).thenReturn(List.of());

        // Mock 流式返回：发 3 个 token 后结束
        when(m3Client.answerStream(anyString(), anyList()))
                .thenReturn(Flux.just("M", "3", "[DONE]"));

        QaAskRequest req = new QaAskRequest();
        req.setQuestion("M3 是什么？");
        req.setTopK(3);

        mockMvc.perform(post("/api/qa/ask/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data: M")));
    }
}
