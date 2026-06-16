package com.example.dockb.controller;

import com.example.dockb.common.Result;
import com.example.dockb.config.CurrentUser;
import com.example.dockb.entity.Document;
import com.example.dockb.mapper.DocumentMapper;
import com.example.dockb.service.DocumentService;
import com.example.dockb.util.AuthContext;
import com.example.dockb.vo.DocumentVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档管理接口（契约 §5.1, §5.2, §5.3）。
 *
 * <p>权限说明：
 * <ul>
 *   <li>上传：必须登录，文档归属于当前用户</li>
 *   <li>列表/详情：ADMIN 可见所有；USER 可见公开(ownerId=null) + 自己的文档</li>
 *   <li>删除：ADMIN 可删除任意；USER 可删除自己的文档</li>
 *   <li>预览/检索：同上可见性规则</li>
 * </ul>
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

    /** 上传：必须登录，owner_id = 当前用户。 */
    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) throws IOException {
        Long userId = AuthContext.getUserId(request);
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        Long id = documentService.upload(file, userId);
        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("status", "pending");
        return Result.success(data);
    }

    /** 列表分页 + 模糊查询 + 分类过滤。ADMIN 可见全部；USER 可见公开+自己的。 */
    @GetMapping
    public Result<?> list(HttpServletRequest request,
                         @RequestParam(defaultValue = "1") long page,
                         @RequestParam(defaultValue = "10") long size,
                         @RequestParam(required = false) String keyword,
                         @RequestParam(required = false) String category) {
        Long userId = AuthContext.getUserId(request);
        boolean isAdmin = AuthContext.isAdmin(request);
        return Result.success(documentService.page(page, size, keyword, category, userId, isAdmin));
    }

    /** 详情：同上可见性规则。 */
    @GetMapping("/{id}")
    public Result<DocumentVO> detail(@PathVariable Long id, HttpServletRequest request) {
        Document doc = documentMapper.selectById(id);
        if (doc == null) {
            return Result.error(404, "文档不存在");
        }
        Long userId = AuthContext.getUserId(request);
        boolean isAdmin = AuthContext.isAdmin(request);

        if (!isAdmin && (doc.getOwnerId() != null && !doc.getOwnerId().equals(userId))) {
            return Result.error(403, "无权访问该文档");
        }
        return Result.success(documentService.detail(id));
    }

    /** 删除：ADMIN 可删任意；USER 可删自己的。 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id, HttpServletRequest request) {
        Document doc = documentMapper.selectById(id);
        if (doc == null) {
            return Result.error(404, "文档不存在");
        }
        Long userId = AuthContext.getUserId(request);
        boolean isAdmin = AuthContext.isAdmin(request);

        if (!isAdmin && (doc.getOwnerId() == null || !doc.getOwnerId().equals(userId))) {
            return Result.error(403, "无权删除该文档");
        }
        documentService.delete(id);
        return Result.success();
    }

    /** 在线预览。 */
    @GetMapping("/{id}/file")
    public ResponseEntity<ByteArrayResource> preview(@PathVariable Long id, HttpServletRequest request) {
        Document doc = documentMapper.selectById(id);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }
        Long userId = AuthContext.getUserId(request);
        boolean isAdmin = AuthContext.isAdmin(request);
        if (!isAdmin && (doc.getOwnerId() != null && !doc.getOwnerId().equals(userId))) {
            return ResponseEntity.status(403).build();
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

    /** 分类聚合：可见性同上。 */
    @GetMapping("/categories")
    public Result<List<String>> categories(HttpServletRequest request) {
        Long userId = AuthContext.getUserId(request);
        boolean isAdmin = AuthContext.isAdmin(request);
        return Result.success(documentService.listCategories(userId, isAdmin));
    }

    /** 检索：可见性同上。 */
    @GetMapping("/search")
    public Result<?> search(HttpServletRequest request,
                            @RequestParam("q") String q,
                            @RequestParam(defaultValue = "5") int topK) {
        Long userId = AuthContext.getUserId(request);
        boolean isAdmin = AuthContext.isAdmin(request);
        return Result.success(documentService.search(q, topK, userId, isAdmin));
    }

    private MediaType mediaTypeFor(String type) {
        if (type == null) return MediaType.APPLICATION_OCTET_STREAM;
        return switch (type.toLowerCase()) {
            case "pdf" -> MediaType.APPLICATION_PDF;
            case "txt" -> MediaType.TEXT_PLAIN;
            case "md" -> MediaType.TEXT_MARKDOWN;
            case "docx" -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }
}
