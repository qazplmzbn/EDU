package com.xyz.question_bank_management_system.modules.llm.service.impl;

import com.xyz.question_bank_management_system.common.PageResponse;
import com.xyz.question_bank_management_system.modules.llm.entity.QbLlmCall;
import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.exception.ErrorCode;
import com.xyz.question_bank_management_system.modules.llm.mapper.QbLlmCallMapper;
import com.xyz.question_bank_management_system.modules.llm.service.LlmCallQueryService;
import com.xyz.question_bank_management_system.util.PageParamUtil;
import com.xyz.question_bank_management_system.modules.llm.vo.LlmCallDetailVO;
import com.xyz.question_bank_management_system.modules.llm.vo.LlmCallListItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LlmCallQueryServiceImpl implements LlmCallQueryService {

    private final QbLlmCallMapper llmCallMapper;

    @Override
    public PageResponse<LlmCallListItemVO> page(Integer bizType, Long bizId, long page, long size, Long viewerId, boolean isAdmin) {
        long safePage = PageParamUtil.normalizePage(page);
        long safeSize = PageParamUtil.normalizeSize(size);
        long offset = PageParamUtil.offset(safePage, safeSize);

        List<LlmCallListItemVO> rows;
        long total;
        if (isAdmin) {
            rows = llmCallMapper.pageByFilter(bizType, bizId, offset, safeSize);
            total = llmCallMapper.countByFilter(bizType, bizId);
        } else {
            rows = llmCallMapper.pageByFilterForTeacher(bizType, bizId, viewerId, offset, safeSize);
            total = llmCallMapper.countByFilterForTeacher(bizType, bizId, viewerId);
        }
        return PageResponse.of(safePage, safeSize, total, rows);
    }

    @Override
    public LlmCallDetailVO detail(Long llmCallId, Long viewerId, boolean isAdmin) {
        QbLlmCall call = isAdmin
                ? llmCallMapper.selectById(llmCallId)
                : llmCallMapper.selectByIdForTeacher(llmCallId, viewerId);
        if (call == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "大模型调用记录不存在");
        }
        LlmCallDetailVO vo = new LlmCallDetailVO();
        vo.setLlmCallId(call.getId());
        vo.setBizType(call.getBizType());
        vo.setBizId(call.getBizId());
        vo.setModelName(call.getModelName());
        vo.setPromptText(call.getPromptText());
        vo.setResponseText(call.getResponseText());
        vo.setResponseJson(call.getResponseJson());
        vo.setCallStatus(call.getCallStatus());
        vo.setTokensPrompt(call.getTokensPrompt());
        vo.setTokensCompletion(call.getTokensCompletion());
        vo.setLatencyMs(call.getLatencyMs());
        vo.setCostAmount(call.getCostAmount());
        vo.setCreatedAt(call.getCreatedAt());
        return vo;
    }
}
