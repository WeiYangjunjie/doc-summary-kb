package com.example.dockb.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dockb.client.dto.QaResult;
import com.example.dockb.client.dto.RankedHit;
import com.example.dockb.common.BizException;
import com.example.dockb.common.PageResult;
import com.example.dockb.common.ResultCode;
import com.example.dockb.config.AppProperties;
import reactor.core.publisher.Flux;
import com.example.dockb.entity.Document;
import com.example.dockb.entity.DocumentChunk;
import com.example.dockb.entity.QaHistory;
import com.example.dockb.mapper.DocumentChunkMapper;
import com.example.dockb.mapper.DocumentMapper;
import com.example.dockb.mapper.QaHistoryMapper;
import com.example.dockb.service.M3Service;
import com.example.dockb.service.QaService;
import com.example.dockb.util.SnippetUtil;
import com.example.dockb.vo.CitationVO;
import com.example.dockb.vo.QaAnswerVO;
import com.example.dockb.vo.QaHistoryVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class QaServiceImpl implements QaService {

    private final DocumentChunkMapper chunkMapper;
    private final DocumentMapper documentMapper;
    private final QaHistoryMapper qaHistoryMapper;
    private final M3Service m3Service;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    public QaServiceImpl(DocumentChunkMapper chunkMapper,
                         DocumentMapper documentMapper,
                         QaHistoryMapper qaHistoryMapper,
                         M3Service m3Service,
                         AppProperties appProperties,
                         ObjectMapper objectMapper) {
        this.chunkMapper = chunkMapper;
        this.documentMapper = documentMapper;
        this.qaHistoryMapper = qaHistoryMapper;
        this.m3Service = m3Service;
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QaAnswerVO ask(String question, Integer topK, String model, Long userId, boolean isAdmin) {
        if (question == null || question.isBlank()) {
            throw new BizException(ResultCode.QUESTION_EMPTY);
        }
        int k = topK == null ? 5 : topK;
        if (k <= 0) k = 5;
        if (k > 20) k = 20;

        // 1) 候选 chunks（权限过滤）
        List<DocumentChunk> allCandidates = searchVisibleChunks(question.trim(), k * 6, userId, isAdmin);
        if (allCandidates.isEmpty()) {
            return saveAndReturn(question, "知识库中暂无相关资料。", Collections.emptyList(), userId);
        }

        // 2) 重排
        List<String> texts = allCandidates.stream()
                .map(DocumentChunk::getContent)
                .collect(Collectors.toList());
        List<RankedHit> ranked = m3Service.rerankWithFallback(question, texts, model);

        // 3) 选取 top K
        List<Integer> picked = new ArrayList<>();
        if (ranked.isEmpty()) {
            for (int i = 0; i < allCandidates.size() && picked.size() < k; i++) {
                picked.add(i);
            }
        } else {
            for (RankedHit r : ranked) {
                if (r.getIndex() < 0 || r.getIndex() >= allCandidates.size()) continue;
                picked.add(r.getIndex());
                if (picked.size() >= k) break;
            }
        }

        // 4) 拼上下文
        List<String> context = new ArrayList<>();
        Map<Integer, Double> scoreMap = new HashMap<>();
        for (int i = 0; i < picked.size(); i++) {
            int idx = picked.get(i);
            context.add(texts.get(idx));
            scoreMap.put(i, scoreAt(ranked, idx));
        }

        // 5) 调 M3 问答（带降级）
        QaResult result = m3Service.answerWithFallback(question, context, model);

        // 6) 组装 citations
        Map<Long, Document> docMap = lookupDocs(picked, allCandidates);
        List<CitationVO> citations = new ArrayList<>();
        if (result.getCitations() != null) {
            for (QaResult.Citation c : result.getCitations()) {
                int idx = c.getIndex();
                if (idx < 0 || idx >= context.size()) continue;
                DocumentChunk ch = allCandidates.get(picked.get(idx));
                Document d = docMap.get(ch.getDocumentId());
                CitationVO vo = new CitationVO();
                vo.setDocumentId(ch.getDocumentId());
                vo.setTitle(d == null ? null : d.getTitle());
                vo.setChunkId(ch.getId());
                String snip = c.getSnippet() == null || c.getSnippet().isBlank()
                        ? SnippetUtil.snippet(texts.get(picked.get(idx)), question, appProperties.getSearch().getSnippetRadius())
                        : c.getSnippet();
                vo.setSnippet(snip);
                vo.setScore(scoreMap.getOrDefault(idx, 0d));
                citations.add(vo);
            }
        }
        if (citations.isEmpty()) {
            for (int i = 0; i < picked.size(); i++) {
                int idx = picked.get(i);
                DocumentChunk ch = allCandidates.get(idx);
                Document d = docMap.get(ch.getDocumentId());
                CitationVO vo = new CitationVO();
                vo.setDocumentId(ch.getDocumentId());
                vo.setTitle(d == null ? null : d.getTitle());
                vo.setChunkId(ch.getId());
                vo.setSnippet(SnippetUtil.snippet(texts.get(idx), question, appProperties.getSearch().getSnippetRadius()));
                vo.setScore(scoreMap.getOrDefault(i, 0d));
                citations.add(vo);
            }
        }

        return saveAndReturn(question, result.getAnswer() == null ? "" : result.getAnswer(), citations, userId);
    }

    @Override
    public PageResult<QaHistoryVO> history(long page, long size, Long userId, boolean isAdmin) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        if (size > 100) size = 100;

        LambdaQueryWrapper<QaHistory> wrapper = new LambdaQueryWrapper<>();
        // ADMIN 可见所有；普通用户只看自己和匿名的
        if (!isAdmin) {
            if (userId != null) {
                wrapper.and(w -> w.isNull(QaHistory::getOwnerId).or().eq(QaHistory::getOwnerId, userId));
            } else {
                wrapper.isNull(QaHistory::getOwnerId);
            }
        }
        wrapper.orderByDesc(QaHistory::getCreatedAt);

        Page<QaHistory> result = qaHistoryMapper.selectPage(new Page<>(page, size), wrapper);
        List<QaHistoryVO> list = result.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(list, result.getTotal(), page, size);
    }

    @Override
    public Flux<String> askStream(String question, Integer topK, String model, Long userId, boolean isAdmin) {
        int k = topK == null ? 5 : Math.min(Math.max(topK, 1), 20);
        List<String> context = buildContext(question, k, userId, isAdmin);
        return m3Service.answerStream(question, context, model);
    }

    @Override
    public List<String> buildContext(String question, Integer topK, Long userId, boolean isAdmin) {
        if (question == null || question.isBlank()) {
            return Collections.emptyList();
        }
        int k = (topK == null || topK <= 0) ? 5 : Math.min(topK, 20);

        // 权限过滤
        List<DocumentChunk> candidates = searchVisibleChunks(question.trim(), k, userId, isAdmin);
        return candidates.stream()
                .limit(k)
                .map(DocumentChunk::getContent)
                .collect(Collectors.toList());
    }

    @Override
    public void saveHistoryAsync(String question, String answer, Long ownerId) {
        if (question == null && answer == null) {
            return;
        }
        try {
            QaHistory h = new QaHistory();
            h.setQuestion(question == null ? "" : question);
            h.setAnswer(answer == null ? "" : answer);
            h.setCitations("[]");
            h.setOwnerId(ownerId);
            qaHistoryMapper.insert(h);
            log.info("[QaService] async save history id={}, ownerId={}", h.getId(), ownerId);
        } catch (Exception e) {
            log.warn("[QaService] saveHistoryAsync failed: {}", e.getMessage());
        }
    }

    // ----------------------------- 私有方法 -----------------------------

    /**
     * 权限感知的 chunk 搜索。
     * ADMIN：所有 chunks；USER：公开(ownerId=null) + 自己的 chunks；匿名：仅公开 chunks。
     */
    private List<DocumentChunk> searchVisibleChunks(String keyword, int limit, Long userId, boolean isAdmin) {
        int maxCandidates = appProperties.getSearch().getMaxCandidates();
        // 先粗查（不超过 maxCandidates）
        List<DocumentChunk> candidates = chunkMapper.selectList(
                new LambdaQueryWrapper<DocumentChunk>()
                        .like(DocumentChunk::getContent, keyword)
                        .last("LIMIT " + maxCandidates));
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        // 取关联文档做权限过滤
        Set<Long> docIds = candidates.stream()
                .map(DocumentChunk::getDocumentId)
                .collect(Collectors.toSet());
        Map<Long, Document> docMap = new HashMap<>();
        if (!docIds.isEmpty()) {
            documentMapper.selectBatchIds(docIds).forEach(d -> docMap.put(d.getId(), d));
        }

        Set<Long> visibleDocIds = new HashSet<>();
        for (Document d : docMap.values()) {
            if (isAdmin) {
                visibleDocIds.add(d.getId());
            } else if (d.getOwnerId() == null) {
                visibleDocIds.add(d.getId()); // 公开文档
            } else if (userId != null && d.getOwnerId().equals(userId)) {
                visibleDocIds.add(d.getId()); // 自己的文档
            }
        }

        return candidates.stream()
                .filter(c -> visibleDocIds.contains(c.getDocumentId()))
                .collect(Collectors.toList());
    }

    private double scoreAt(List<RankedHit> ranked, int candidateIdx) {
        for (RankedHit r : ranked) {
            if (r.getIndex() == candidateIdx) return r.getScore();
        }
        return 0d;
    }

    private Map<Long, Document> lookupDocs(List<Integer> picked, List<DocumentChunk> candidates) {
        Map<Long, Document> map = new HashMap<>();
        if (picked.isEmpty()) return map;
        List<Long> ids = picked.stream().map(candidates::get).map(DocumentChunk::getDocumentId).distinct().collect(Collectors.toList());
        if (!ids.isEmpty()) {
            documentMapper.selectBatchIds(ids).forEach(d -> map.put(d.getId(), d));
        }
        return map;
    }

    private QaAnswerVO saveAndReturn(String question, String answer, List<CitationVO> citations, Long ownerId) {
        QaHistory h = new QaHistory();
        h.setQuestion(question);
        h.setAnswer(answer == null ? "" : answer);
        h.setOwnerId(ownerId);
        try {
            h.setCitations(objectMapper.writeValueAsString(citations == null ? Collections.emptyList() : citations));
        } catch (Exception e) {
            log.warn("[QaService] citations serialize failed: {}", e.getMessage());
            h.setCitations("[]");
        }
        qaHistoryMapper.insert(h);

        QaAnswerVO vo = new QaAnswerVO();
        vo.setId(h.getId());
        vo.setQuestion(h.getQuestion());
        vo.setAnswer(h.getAnswer());
        vo.setCitations(citations == null ? Collections.emptyList() : citations);
        vo.setCreatedAt(h.getCreatedAt());
        return vo;
    }

    private QaHistoryVO toVO(QaHistory h) {
        QaHistoryVO vo = new QaHistoryVO();
        vo.setId(h.getId());
        vo.setQuestion(h.getQuestion());
        vo.setAnswer(h.getAnswer());
        try {
            String c = h.getCitations();
            if (c == null || c.isBlank() || "null".equalsIgnoreCase(c)) {
                vo.setCitations(Collections.emptyList());
            } else {
                vo.setCitations(objectMapper.readValue(c, new TypeReference<List<CitationVO>>() {}));
            }
        } catch (Exception e) {
            log.warn("[QaService] parse citations failed for id={}: {}", h.getId(), e.getMessage());
            vo.setCitations(Collections.emptyList());
        }
        vo.setCreatedAt(h.getCreatedAt());
        return vo;
    }
}
