package com.example.dockb.controller;

import com.example.dockb.common.Result;
import com.example.dockb.entity.Document;
import com.example.dockb.mapper.DocumentMapper;
import com.example.dockb.service.DocumentService;
import com.example.dockb.vo.DocumentVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档管理接口（契约 §5.1, §5.2, §5.3）。
 */
@Slf4j
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentMapper documentMapper;

    @Autowired
    public DocumentController(DocumentService documentService, DocumentMapper documentMapper) {
        this.documentService = documentService;
        this.documentMapper = documentMapper;
    }

    /** 上传。 */
    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        Long id = documentService.upload(file);
        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("status", "pending");
        return Result.success(data);
    }

    /** 列表分页 + 模糊查询 + 分类过滤。 */
    @GetMapping
    public Result<?> list(@RequestParam(defaultValue = "1") long page,
                          @RequestParam(defaultValue = "10") long size,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) String category) {
        return Result.success(documentService.page(page, size, keyword, category));
    }

    /** 详情。 */
    @GetMapping("/{id}")
    public Result<DocumentVO> detail(@PathVariable Long id) {
        return Result.success(documentService.detail(id));
    }

    /** 删除。 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        documentService.delete(id);
        return Result.success();
    }

    /** 在线预览：直接返回二进制。 */
    @GetMapping("/{id}/file")
    public ResponseEntity<ByteArrayResource> preview(@PathVariable Long id) {
        Document doc = documentMapper.selectById(id);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }
        byte[] bytes = documentService.loadFileBytes(id);
        MediaType type = mediaTypeFor(doc.getFileType());
        String filename = doc.getOriginalName() == null ? ("document-" + id) : doc.getOriginalName();
        String encoded = java.net.URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(type)
                .contentLength(bytes.length)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"; filename*=UTF-8''" + encoded)
                .body(new ByteArrayResource(bytes));
    }

    /** 分类聚合。 */
    @GetMapping("/categories")
    public Result<List<String>> categories() {
        return Result.success(documentService.listCategories());
    }

    /** 检索。 */
    @GetMapping("/search")
    public Result<?> search(@RequestParam("q") String q,
                            @RequestParam(defaultValue = "5") int topK) {
        return Result.success(documentService.search(q, topK));
    }

    private MediaType mediaTypeFor(String type) {
        if (type == null) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        return switch (type.toLowerCase()) {
            case "pdf" -> MediaType.APPLICATION_PDF;
            case "txt" -> MediaType.TEXT_PLAIN;
            case "md" -> MediaType.TEXT_MARKDOWN;
            case "docx" -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }
}