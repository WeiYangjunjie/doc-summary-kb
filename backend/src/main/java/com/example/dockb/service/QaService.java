package com.example.dockb.service;

import com.example.dockb.common.PageResult;
import com.example.dockb.vo.QaAnswerVO;
import com.example.dockb.vo.QaHistoryVO;
import reactor.core.publisher.Flux;

import java.util.List;

public interface QaService {

    /**
     * 问答（同步）。
     * @param question 问题
     * @param topK topK
     * @param model 指定模型，为空则用默认模型
     */
    QaAnswerVO ask(String question, Integer topK, String model);

    QaAnswerVO ask(String question, Integer topK);

    PageResult<QaHistoryVO> history(long page, long size);

    /**
     * 为流式问答构建上下文字符串列表（keyword 匹配，选取 topK）。
     */
    List<String> buildContext(String question, Integer topK);

    /**
     * 异步保存问答历史（不等待完成）。
     */
    void saveHistoryAsync(String question, String answer);

    /**
     * 流式问答：返回逐 token 的 Flux。
     * @param question 问题
     * @param topK topK
     * @param model 指定模型，为空则用默认
     */
    Flux<String> askStream(String question, Integer topK, String model);
}
