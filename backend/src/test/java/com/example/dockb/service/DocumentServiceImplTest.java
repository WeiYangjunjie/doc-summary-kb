package com.example.dockb.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dockb.config.AppProperties;
import com.example.dockb.entity.Document;
import com.example.dockb.mapper.DocumentMapper;
import com.example.dockb.mapper.QaHistoryMapper;
import com.example.dockb.service.impl.DocumentServiceImpl;
import com.example.dockb.vo.DocumentVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DocumentService 单元测试（Mock Mapper 层）。
 */
@ExtendWith(MockitoExtension.class)
class DocumentServiceImplTest {

    @Mock
    private DocumentMapper documentMapper;

    @Mock
    private com.example.dockb.mapper.DocumentChunkMapper chunkMapper;

    @Mock
    private QaHistoryMapper qaHistoryMapper;

    @Mock
    private AppProperties appProperties;

    @Mock
    private AppProperties.SearchConfig searchConfig;

    private DocumentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DocumentServiceImpl(documentMapper, chunkMapper, qaHistoryMapper, appProperties, new ObjectMapper());
    }

    @Test
    void page_withDefaults_returnsFirstPage() {
        Document doc = mockDoc(1L, "测试文档");
        Page<Document> page = new Page<>(1, 10);
        page.setRecords(List.of(doc));
        page.setTotal(1);

        when(appProperties.getSearch()).thenReturn(searchConfig);
        when(documentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        var result = service.page(1, 10, null, null);

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getList().size());
        assertEquals("测试文档", result.getList().get(0).getTitle());
    }

    @Test
    void page_invalidPage_usesDefault() {
        Page<Document> emptyPage = new Page<>(1, 10);
        emptyPage.setRecords(List.of());
        emptyPage.setTotal(0);

        when(appProperties.getSearch()).thenReturn(searchConfig);
        when(documentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(emptyPage);

        var result = service.page(-1, 0, null, null);

        assertEquals(0, result.getTotal());
        assertTrue(result.getList().isEmpty());
    }

    @Test
    void detail_existingDoc_returnsVO() {
        Document doc = new Document();
        doc.setId(5L);
        doc.setTitle("测试文档");
        doc.setOriginalName("test.pdf");
        doc.setFileType("pdf");
        doc.setFileSize(1024L);
        doc.setCategory("技术");
        doc.setTags("[\"AI\",\"测试\"]");
        doc.setSummary("这是一个摘要");
        doc.setStatus("done");
        doc.setCreatedAt(LocalDateTime.now());
        doc.setUpdatedAt(LocalDateTime.now());

        when(documentMapper.selectById(5L)).thenReturn(doc);

        DocumentVO vo = service.detail(5L);

        assertEquals(5L, vo.getId());
        assertEquals("测试文档", vo.getTitle());
        assertEquals("test.pdf", vo.getOriginalName());
        assertEquals("pdf", vo.getFileType());
        assertEquals(1024L, vo.getFileSize());
        assertEquals("done", vo.getStatus());
    }

    @Test
    void detail_nonExistingDoc_returnsNull() {
        when(documentMapper.selectById(999L)).thenReturn(null);

        DocumentVO vo = service.detail(999L);

        assertNull(vo);
    }

    @Test
    void detail_pendingDoc_hasNoErrorMsg() {
        Document doc = new Document();
        doc.setId(10L);
        doc.setTitle("处理中");
        doc.setStatus("pending");
        doc.setCreatedAt(LocalDateTime.now());
        doc.setUpdatedAt(LocalDateTime.now());

        when(documentMapper.selectById(10L)).thenReturn(doc);

        DocumentVO vo = service.detail(10L);

        assertEquals("pending", vo.getStatus());
        assertNull(vo.getErrorMsg());
    }

    @Test
    void delete_existingDoc_success() {
        when(documentMapper.deleteById(7L)).thenReturn(1);

        service.delete(7L);

        verify(documentMapper).deleteById(7L);
    }

    @Test
    void categories_returnsDistinctCategories() {
        when(documentMapper.selectDistinctCategories()).thenReturn(List.of("技术", "产品", "运营"));

        var cats = service.categories();

        assertEquals(3, cats.size());
        assertTrue(cats.contains("技术"));
        assertTrue(cats.contains("产品"));
        assertTrue(cats.contains("运营"));
    }

    @Test
    void categories_emptyDb_returnsEmpty() {
        when(documentMapper.selectDistinctCategories()).thenReturn(List.of());

        var cats = service.categories();

        assertTrue(cats.isEmpty());
    }

    // ---- helper ----

    private Document mockDoc(Long id, String title) {
        Document doc = new Document();
        doc.setId(id);
        doc.setTitle(title);
        doc.setOriginalName(title + ".pdf");
        doc.setFileType("pdf");
        doc.setFileSize(2048L);
        doc.setCategory("技术");
        doc.setTags("[]");
        doc.setSummary("摘要");
        doc.setStatus("done");
        doc.setCreatedAt(LocalDateTime.now());
        doc.setUpdatedAt(LocalDateTime.now());
        return doc;
    }
}
