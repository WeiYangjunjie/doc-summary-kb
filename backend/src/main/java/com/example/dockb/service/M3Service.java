package com.example.dockb.service;

import com.example.dockb.client.M3Client;
import com.example.dockb.client.dto.QaResult;
import com.example.dockb.client.dto.RankedHit;

import java.util.List;

/**
 * M3 编排服务：调用 M3Client，失败时降级到本地启发式。
 *
 * <p>异步任务中也用这个服务。
 */
public interface M3Service {

    /** M3 可达性。 */
    boolean isReachable();

    String classifyWithFallback(String text, List<String> candidates, String defaultCategory);

    String summarizeWithFallback(String text, String defaultSummary);

    List<String> extractTagsWithFallback(String text, List<String> defaults);

    /**
     * 重排：M3 失败时降级为 TF 词频打分（调用方负责实现）。
     */
    List<RankedHit> rerankWithFallback(String query, List<String> candidates);

    /**
     * 问答：返回 {@link QaResult}。M3 失败时构造一个简单回退答案。
     */
    QaResult answerWithFallback(String question, List<String> context);
}