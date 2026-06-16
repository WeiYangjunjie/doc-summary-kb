package com.example.dockb.service;

import com.example.dockb.common.PageResult;
import com.example.dockb.vo.QaAnswerVO;
import com.example.dockb.vo.QaHistoryVO;

public interface QaService {

    QaAnswerVO ask(String question, Integer topK);

    PageResult<QaHistoryVO> history(long page, long size);

    /**
     * 为流式问答构建上下文字符串列表（keyword 匹配，选取 topK）。
     * 用于 {@code /api/qa/ask/stream}。
     */
    List<String> buildContext(String question, Integer topK);

    /**
     * 异步保存问答历史（不等待完成）。
     * 用于流式问答结束后将完整答案入库。
     */
    void saveHistoryAsync(String question, String answer);
}