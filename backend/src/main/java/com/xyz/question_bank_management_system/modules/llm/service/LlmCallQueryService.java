package com.xyz.question_bank_management_system.modules.llm.service;

import com.xyz.question_bank_management_system.common.PageResponse;
import com.xyz.question_bank_management_system.modules.llm.vo.LlmCallDetailVO;
import com.xyz.question_bank_management_system.modules.llm.vo.LlmCallListItemVO;

public interface LlmCallQueryService {

    PageResponse<LlmCallListItemVO> page(Integer bizType, Long bizId, long page, long size, Long viewerId, boolean isAdmin);

    LlmCallDetailVO detail(Long llmCallId, Long viewerId, boolean isAdmin);
}
