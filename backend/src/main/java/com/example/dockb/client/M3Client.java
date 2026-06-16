package com.example.dockb.client;

import com.example.dockb.client.dto.ChatRequest;
import com.example.dockb.client.dto.ChatResponse;
import com.example.dockb.client.dto.QaResult;
import com.example.dockb.client.dto.RankedHit;

import java.util.List;

/**
 * M3 客户端接口（契约 §6）。
 *
 * <p>所有方法在 M3 不可达或返回解析失败时抛 {@link M3Exception}。
 */
public interface M3Client {

    /** 简单可达性探测。 */
    boolean ping();

    /** 分类：从候选类别中挑一个最合适的。 */
    String classify(String text, List<String> candidates);

    /** 摘要：返回 200~500 字。 */
    String summarize(String text);

    /** 抽取 tags（逗号或 JSON 数组）。 */
    List<String> extractTags(String text);

    /** 重排：返回按相关性倒序排列的 (原下标, score)。 */
    List<RankedHit> rerank(String query, List<String> candidates);

    /** 问答 + 引用：返回模型生成的答案与引用下标/片段。 */
    QaResult answer(String question, List<String> context);

    /** 通用调用入口（用于自定义 prompt）。 */
    ChatResponse chat(ChatRequest request);
}