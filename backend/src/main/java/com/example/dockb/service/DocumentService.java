package com.example.dockb.service;

import com.example.dockb.common.PageResult;
import com.example.dockb.entity.Document;
import com.example.dockb.vo.DocumentVO;
import com.example.dockb.vo.SearchResponseVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface DocumentService {

    /**
     * 上传：保存文件 + 写库（owner_id = userId）+ 切块 + 异步触发摘要分类。
     */
    Long upload(MultipartFile file, Long ownerId) throws IOException;

    // ========== 分页列表（权限感知） ==========

    /**
     * ADMIN：返回所有文档。
     * USER：返回公开(ownerId=null) + 当前用户的文档。
     */
    PageResult<DocumentVO> page(long page, long size, String keyword, String category, Long userId, boolean isAdmin);

    default PageResult<DocumentVO> page(long page, long size, String keyword, String category) {
        return page(page, size, keyword, category, null, false);
    }

    // ========== 详情 ==========
    DocumentVO detail(Long id);

    // ========== 删除 ==========
    void delete(Long id);

    // ========== 文件 ==========
    byte[] loadFileBytes(Long id);

    // ========== 分类聚合（权限感知） ==========

    List<String> listCategories(Long userId, boolean isAdmin);

    default List<String> listCategories() {
        return listCategories(null, false);
    }

    // ========== 检索（权限感知） ==========

    SearchResponseVO search(String q, int topK, Long userId, boolean isAdmin);

    default SearchResponseVO search(String q, int topK) {
        return search(q, topK, null, false);
    }

    // ========== 异步处理 ==========
    /** 异步处理：分类/摘要/标签（失败不影响上传）。 */
    void asyncEnrichDocument(Long id);
}
