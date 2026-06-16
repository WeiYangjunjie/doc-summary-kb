package com.example.dockb;

import com.example.dockb.client.M3Client;
import com.example.dockb.client.dto.QaResult;
import com.example.dockb.common.PageResult;
import com.example.dockb.config.M3Properties;
import com.example.dockb.config.ai.ModelRegistry;
import com.example.dockb.controller.DocumentController;
import com.example.dockb.controller.HealthController;
import com.example.dockb.mapper.DocumentMapper;
import com.example.dockb.service.DocumentService;
import com.example.dockb.service.impl.M3ServiceImpl;
import com.example.dockb.vo.SearchResponseVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 集成测试：Mock 掉 M3Client 后，验证 /api/health 与 /api/documents 列表路由可正确响应。
 *
 * <p>不依赖真实 M3 与 MySQL，使用 @WebMvcTest 切片。
 */
@WebMvcTest(controllers = {DocumentController.class, HealthController.class})
@Import({M3ServiceImpl.class, M3Properties.class,
        com.example.dockb.common.GlobalExceptionHandler.class,
        com.example.dockb.config.AppProperties.class})
@AutoConfigureMockMvc
class DocumentControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private M3Client m3Client;

    @MockBean
    private ModelRegistry modelRegistry;

    @MockBean
    private DocumentService documentService;

    @MockBean
    private DocumentMapper documentMapper;

    @Test
    void healthReturnsUp() throws Exception {
        when(m3Client.ping()).thenReturn(true);

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.status", is("up")))
                .andExpect(jsonPath("$.data.m3Model", is("MiniMax-M3")));
    }

    @Test
    void documentsListEmpty() throws Exception {
        when(documentService.page(anyLong(), anyLong(), any(), any()))
                .thenAnswer(inv -> PageResult.empty(1, 10));

        mockMvc.perform(get("/api/documents").param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.total", is(0)))
                .andExpect(jsonPath("$.data.list", notNullValue()));
    }

    @Test
    void searchEmpty() throws Exception {
        when(documentService.search(anyString(), anyInt()))
                .thenReturn(new SearchResponseVO("test", Collections.emptyList()));

        mockMvc.perform(get("/api/documents/search").param("q", "test").param("topK", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.hits", notNullValue()));
    }

    @Test
    void m3ClientMockWorks() {
        when(m3Client.classify(anyString(), any())).thenReturn("技术");
        when(m3Client.summarize(anyString())).thenReturn("测试摘要");
        when(m3Client.extractTags(anyString())).thenReturn(List.of("AI", "测试"));
        when(m3Client.answer(anyString(), any())).thenReturn(new QaResult("answer", Collections.emptyList()));
        when(m3Client.ping()).thenReturn(true);

        org.junit.jupiter.api.Assertions.assertEquals("技术", m3Client.classify("x", List.of("技术", "其他")));
        org.junit.jupiter.api.Assertions.assertEquals("测试摘要", m3Client.summarize("x"));
        org.junit.jupiter.api.Assertions.assertEquals(List.of("AI", "测试"), m3Client.extractTags("x"));
        org.junit.jupiter.api.Assertions.assertTrue(m3Client.ping());
    }
}