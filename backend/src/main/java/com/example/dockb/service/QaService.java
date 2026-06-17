package com.example.dockb.service;

import com.example.dockb.common.PageResult;
import com.example.dockb.vo.QaAnswerVO;
import com.example.dockb.vo.QaHistoryVO;
import reactor.core.publisher.Flux;

import java.util.List;

public interface QaService {

    /**
     * 问答（同步）。只检索当前用户有权限的文档。
     *
     * @param question 问题
     * @param topK    topK
     * @param model   指定模型，为空则用默认模型
     * @param userId  当前登录用户 ID（null=未登录，只能看到公开文档）
     * @param isAdmin 是否为管理员（管理员可看到所有文档）
     */
    QaAnswerVO ask(String question, Integer topK, String model, Long userId, boolean isAdmin);

    default QaAnswerVO ask(String question, Integer topK, Long userId, boolean isAdmin) {
        return ask(question, topK, null, userId, isAdmin);
    }

    default QaAnswerVO ask(String question, Integer topK) {
        return ask(question, topK, null, null, false);
    }

    /**
     * 分页历史（权限感知）。
     * @param userId  当前登录用户 ID（null=返回所有）
     * @param isAdmin 是否为管理员（管理员可看所有）
     */
    PageResult<QaHistoryVO> history(long page, long size, Long userId, boolean isAdmin);

    default PageResult<QaHistoryVO> history(long page, long size) {
        return history(page, size, null, false);
    }

    /**
     * 为流式问答构建上下文字符串列表（keyword 匹配，选取 topK，权限过滤）。
     */
    List<String> buildContext(String question, Integer topK, Long userId, boolean isAdmin);

    default List<String> buildContext(String question, Integer topK) {
        return buildContext(question, topK, null, false);
    }

    /**
     * 异步保存问答历史（不等待完成）。
     * @param ownerId 提问者 ID（可 null）
     */
    void saveHistoryAsync(String question, String answer, Long ownerId);

    default void saveHistoryAsync(String question, String answer) {
        saveHistoryAsync(question, answer, null);
    }

    /**
     * 流式问答：返回逐 token 的 Flux。
     * @param userId  当前登录用户 ID（null=未登录，只能看到公开文档）
     * @param isAdmin 是否为管理员
     */
    Flux<String> askStream(String question, Integer topK, String model, Long userId, boolean isAdmin);

    default Flux<String> askStream(String question, Integer topK, String model) {
        return askStream(question, topK, model, null, false);
    }
}
