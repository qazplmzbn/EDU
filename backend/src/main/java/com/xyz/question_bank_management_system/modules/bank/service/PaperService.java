package com.xyz.question_bank_management_system.modules.bank.service;

import com.xyz.question_bank_management_system.common.PageResponse;
import com.xyz.question_bank_management_system.modules.bank.dto.PaperAddQuestionRequest;
import com.xyz.question_bank_management_system.modules.bank.dto.PaperQuestionBatchUpdateRequest;
import com.xyz.question_bank_management_system.modules.bank.dto.PaperQuestionUpdateRequest;
import com.xyz.question_bank_management_system.modules.bank.dto.PaperUpsertRequest;
import com.xyz.question_bank_management_system.modules.bank.entity.QbPaper;
import com.xyz.question_bank_management_system.modules.bank.vo.PaperDetailVO;

public interface PaperService {

    Long create(PaperUpsertRequest request, Long creatorId);

    void update(Long paperId, PaperUpsertRequest request, Long actorId, boolean isAdmin);

    void delete(Long paperId, Long actorId, boolean isAdmin);

    PageResponse<QbPaper> page(long page, long size, Long actorId, boolean isAdmin);

    PaperDetailVO detail(Long paperId, Long actorId, boolean isAdmin);

    Long addQuestion(Long paperId, PaperAddQuestionRequest request, Long actorId, boolean isAdmin);

    void batchUpdatePaperQuestions(Long paperId, PaperQuestionBatchUpdateRequest request, Long actorId, boolean isAdmin);

    void updatePaperQuestion(Long paperQuestionId, PaperQuestionUpdateRequest request, Long actorId, boolean isAdmin);

    void removePaperQuestion(Long paperQuestionId, Long actorId, boolean isAdmin);
}
