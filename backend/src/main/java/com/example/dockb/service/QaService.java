package com.example.dockb.service;

import com.example.dockb.common.PageResult;
import com.example.dockb.vo.QaAnswerVO;
import com.example.dockb.vo.QaHistoryVO;

public interface QaService {

    QaAnswerVO ask(String question, Integer topK);

    PageResult<QaHistoryVO> history(long page, long size);
}