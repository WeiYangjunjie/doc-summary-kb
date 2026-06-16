package com.example.dockb.service;

import com.example.dockb.common.PageResult;
import com.example.dockb.entity.Document;
import com.example.dockb.vo.DocumentVO;
import com.example.dockb.vo.SearchHitVO;
import com.example.dockb.vo.SearchResponseVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface DocumentService {

    /** 上传：保存文件 + 写库 + 切块 + 异步触发摘要分类。 */
    Long upload(MultipartFile file) throws IOException;

    PageResult<DocumentVO> page(long page, long size, String keyword, String category);

    DocumentVO detail(Long id);

    void delete(Long id);

    byte[] loadFileBytes(Long id);

    List<String> listCategories();

    SearchResponseVO search(String q, int topK);

    /** 异步处理：分类/摘要/标签（失败不影响上传）。 */
    void asyncEnrichDocument(Long id);
}