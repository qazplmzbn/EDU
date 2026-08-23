package com.xyz.question_bank_management_system.modules.bank.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xyz.question_bank_management_system.common.PageResponse;
import com.xyz.question_bank_management_system.modules.bank.dto.PaperAddQuestionRequest;
import com.xyz.question_bank_management_system.modules.bank.dto.PaperQuestionBatchUpdateItem;
import com.xyz.question_bank_management_system.modules.bank.dto.PaperQuestionBatchUpdateRequest;
import com.xyz.question_bank_management_system.modules.bank.dto.PaperQuestionUpdateRequest;
import com.xyz.question_bank_management_system.modules.bank.dto.PaperUpsertRequest;
import com.xyz.question_bank_management_system.modules.bank.entity.QbPaper;
import com.xyz.question_bank_management_system.modules.bank.entity.QbPaperQuestion;
import com.xyz.question_bank_management_system.modules.bank.entity.QbQuestion;
import com.xyz.question_bank_management_system.modules.bank.entity.QbQuestionOption;
import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.exception.ErrorCode;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbPaperMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbPaperQuestionMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbQuestionMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbQuestionOptionMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbQuestionTagMapper;
import com.xyz.question_bank_management_system.modules.user.service.AuditLogService;
import com.xyz.question_bank_management_system.modules.bank.service.PaperService;
import com.xyz.question_bank_management_system.util.HashUtil;
import com.xyz.question_bank_management_system.util.PageParamUtil;
import com.xyz.question_bank_management_system.modules.bank.vo.PaperDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PaperServiceImpl implements PaperService {

    private final QbPaperMapper paperMapper;
    private final QbPaperQuestionMapper paperQuestionMapper;
    private final QbQuestionMapper questionMapper;
    private final QbQuestionOptionMapper optionMapper;
    private final QbQuestionTagMapper questionTagMapper;
    private final AuditLogService auditLogService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public Long create(PaperUpsertRequest request, Long creatorId) {
        QbPaper p = new QbPaper();
        applyPaperUpsert(p, request);
        p.setTotalScore(0);
        p.setCreatorId(creatorId);
        paperMapper.insert(p);
        recordAudit(creatorId, "PAPER_CREATE", "PAPER", p.getId(), null, paperAuditSnapshot(p));
        return p.getId();
    }

    @Override
    public void update(Long paperId, PaperUpsertRequest request, Long actorId, boolean isAdmin) {
        QbPaper p = loadPaperForManage(paperId, actorId, isAdmin);
        Map<String, Object> before = paperAuditSnapshot(p);
        applyPaperUpsert(p, request);
        paperMapper.update(p);
        recordAudit(actorId, "PAPER_UPDATE", "PAPER", paperId, before, paperAuditSnapshot(p));
    }

    @Override
    @Transactional
    public void delete(Long paperId, Long actorId, boolean isAdmin) {
        QbPaper paper = loadPaperForManage(paperId, actorId, isAdmin);
        paperMapper.softDelete(paperId);
        paperQuestionMapper.deleteByPaperId(paperId);
        recordAudit(actorId, "PAPER_DELETE", "PAPER", paperId, paperAuditSnapshot(paper), null);
    }

    @Override
    public PageResponse<QbPaper> page(long page, long size, Long actorId, boolean isAdmin) {
        long safePage = PageParamUtil.normalizePage(page);
        long safeSize = PageParamUtil.normalizeSize(size);
        long offset = PageParamUtil.offset(safePage, safeSize);

        List<QbPaper> rows;
        long total;
        if (isAdmin) {
            rows = paperMapper.page(offset, safeSize);
            total = paperMapper.countAll();
        } else {
            rows = paperMapper.pageByCreator(actorId, offset, safeSize);
            total = paperMapper.countByCreator(actorId);
        }
        return PageResponse.of(safePage, safeSize, total, rows);
    }

    @Override
    public PaperDetailVO detail(Long paperId, Long actorId, boolean isAdmin) {
        QbPaper p = loadPaperForManage(paperId, actorId, isAdmin);

        PaperDetailVO vo = new PaperDetailVO();
        vo.setId(p.getId());
        vo.setPaperTitle(p.getPaperTitle());
        vo.setPaperDesc(p.getPaperDesc());
        vo.setPaperType(p.getPaperType());
        vo.setTotalScore(p.getTotalScore());
        vo.setCreatorId(p.getCreatorId());
        vo.setCreatedAt(p.getCreatedAt());
        vo.setUpdatedAt(p.getUpdatedAt());

        List<QbPaperQuestion> pqs = paperQuestionMapper.selectByPaperId(paperId);
        List<PaperDetailVO.PaperQuestionVO> qvos = new ArrayList<>();
        for (QbPaperQuestion pq : pqs) {
            qvos.add(toPaperQuestionVO(pq));
        }
        vo.setQuestions(qvos);
        return vo;
    }

    @Override
    @Transactional
    public Long addQuestion(Long paperId, PaperAddQuestionRequest request, Long actorId, boolean isAdmin) {
        loadPaperForManage(paperId, actorId, isAdmin);

        QbQuestion q = questionMapper.selectById(request.getQuestionId());
        if (q == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "题目不存在");
        }
        if (!isAdmin && !canTeacherUseQuestion(q, actorId)) {
            throw BizException.of(ErrorCode.FORBIDDEN, "无权使用该题目");
        }

        String snapshotJson = buildQuestionSnapshot(q.getId());
        String snapshotHash = HashUtil.sha256(snapshotJson);

        QbPaperQuestion pq = new QbPaperQuestion();
        pq.setPaperId(paperId);
        pq.setQuestionId(request.getQuestionId());
        pq.setOrderNo(request.getOrderNo());
        pq.setScore(request.getScore());
        pq.setSnapshotJson(snapshotJson);
        pq.setSnapshotHash(snapshotHash);
        try {
            paperQuestionMapper.insert(pq);
        } catch (DuplicateKeyException e) {
            throw BizException.of(ErrorCode.CONFLICT, "该排序位置已存在试题");
        }

        refreshTotalScore(paperId);
        recordAudit(actorId,
                "PAPER_ADD_QUESTION",
                "PAPER",
                paperId,
                null,
                paperQuestionAuditSnapshot(pq));
        return pq.getId();
    }

    @Override
    @Transactional
    public void batchUpdatePaperQuestions(Long paperId, PaperQuestionBatchUpdateRequest request, Long actorId, boolean isAdmin) {
        loadPaperForManage(paperId, actorId, isAdmin);

        List<QbPaperQuestion> existingQuestions = paperQuestionMapper.selectByPaperId(paperId);
        List<PaperQuestionBatchUpdateItem> items = request.getQuestions();
        if (items == null || items.isEmpty()) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "试卷题目不能为空");
        }
        if (items.size() != existingQuestions.size()) {
            throw BizException.of(ErrorCode.PARAM_ERROR, "必须包含试卷中的全部题目");
        }

        Map<Long, QbPaperQuestion> existingById = new HashMap<>();
        for (QbPaperQuestion question : existingQuestions) {
            existingById.put(question.getId(), question);
        }

        Set<Long> seenIds = new HashSet<>();
        Set<Integer> seenOrderNos = new HashSet<>();
        int itemCount = items.size();
        for (PaperQuestionBatchUpdateItem item : items) {
            if (!seenIds.add(item.getId())) {
                throw BizException.of(ErrorCode.PARAM_ERROR, "试卷题目编号不能重复");
            }
            if (item.getOrderNo() == null || item.getOrderNo() < 1 || item.getOrderNo() > itemCount) {
                throw BizException.of(ErrorCode.PARAM_ERROR, "题目顺序超出允许范围");
            }
            if (!seenOrderNos.add(item.getOrderNo())) {
                throw BizException.of(ErrorCode.PARAM_ERROR, "题目顺序不能重复");
            }
            if (!existingById.containsKey(item.getId())) {
                throw BizException.of(ErrorCode.NOT_FOUND, "试卷题目不存在");
            }
        }

        int currentMaxOrder = existingQuestions.stream()
                .map(QbPaperQuestion::getOrderNo)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
        int tempBase = currentMaxOrder + itemCount + 100;
        int tempOrder = tempBase;
        for (PaperQuestionBatchUpdateItem item : items) {
            QbPaperQuestion question = existingById.get(item.getId());
            question.setOrderNo(++tempOrder);
            question.setScore(item.getScore());
            paperQuestionMapper.update(question);
        }

        for (PaperQuestionBatchUpdateItem item : items) {
            QbPaperQuestion question = existingById.get(item.getId());
            question.setOrderNo(item.getOrderNo());
            question.setScore(item.getScore());
            try {
                paperQuestionMapper.update(question);
            } catch (DuplicateKeyException e) {
                throw BizException.of(ErrorCode.CONFLICT, "该排序位置已存在试题");
            }
        }

        refreshTotalScore(paperId);
        recordAudit(actorId,
                "PAPER_BATCH_UPDATE",
                "PAPER",
                paperId,
                null,
                Map.of("questionCount", items.size(), "questions", items));
    }

    @Override
    public void updatePaperQuestion(Long paperQuestionId, PaperQuestionUpdateRequest request, Long actorId, boolean isAdmin) {
        QbPaperQuestion pq = paperQuestionMapper.selectById(paperQuestionId);
        if (pq == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "试卷题目不存在");
        }
        loadPaperForManage(pq.getPaperId(), actorId, isAdmin);
        Map<String, Object> before = paperQuestionAuditSnapshot(pq);

        pq.setOrderNo(request.getOrderNo());
        pq.setScore(request.getScore());
        try {
            paperQuestionMapper.update(pq);
        } catch (DuplicateKeyException e) {
            throw BizException.of(ErrorCode.CONFLICT, "该排序位置已存在试题");
        }
        refreshTotalScore(pq.getPaperId());
        recordAudit(actorId,
                "PAPER_UPDATE_QUESTION",
                "PAPER",
                pq.getPaperId(),
                before,
                paperQuestionAuditSnapshot(pq));
    }

    @Override
    public void removePaperQuestion(Long paperQuestionId, Long actorId, boolean isAdmin) {
        QbPaperQuestion pq = paperQuestionMapper.selectById(paperQuestionId);
        if (pq == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "试卷题目不存在");
        }
        loadPaperForManage(pq.getPaperId(), actorId, isAdmin);
        Map<String, Object> before = paperQuestionAuditSnapshot(pq);

        paperQuestionMapper.deleteById(paperQuestionId);
        refreshTotalScore(pq.getPaperId());
        recordAudit(actorId, "PAPER_REMOVE_QUESTION", "PAPER", pq.getPaperId(), before, null);
    }

    private void refreshTotalScore(Long paperId) {
        QbPaper p = paperMapper.selectById(paperId);
        if (p == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "试卷不存在");
        }
        int total = paperQuestionMapper.sumScoreByPaperId(paperId);
        p.setTotalScore(total);
        paperMapper.update(p);
    }

    private void applyPaperUpsert(QbPaper paper, PaperUpsertRequest request) {
        paper.setPaperTitle(request.getPaperTitle());
        paper.setPaperDesc(request.getPaperDesc());
        paper.setPaperType(request.getPaperType());
    }

    private PaperDetailVO.PaperQuestionVO toPaperQuestionVO(QbPaperQuestion paperQuestion) {
        PaperDetailVO.PaperQuestionVO vo = new PaperDetailVO.PaperQuestionVO();
        vo.setId(paperQuestion.getId());
        vo.setQuestionId(paperQuestion.getQuestionId());
        vo.setOrderNo(paperQuestion.getOrderNo());
        vo.setScore(paperQuestion.getScore());
        vo.setSnapshotJson(paperQuestion.getSnapshotJson());
        vo.setSnapshotHash(paperQuestion.getSnapshotHash());
        return vo;
    }

    private QbPaper loadPaperForManage(Long paperId, Long actorId, boolean isAdmin) {
        QbPaper p = paperMapper.selectById(paperId);
        if (p == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "试卷不存在");
        }
        if (!isAdmin && !Objects.equals(p.getCreatorId(), actorId)) {
            throw BizException.of(ErrorCode.FORBIDDEN, "无权管理该试卷");
        }
        return p;
    }

    private boolean canTeacherUseQuestion(QbQuestion q, Long teacherId) {
        if (Objects.equals(q.getCreatedBy(), teacherId)) {
            return true;
        }
        if (q.getStatus() == null || q.getStatus() != 2) {
            return false;
        }
        return q.getBankReviewStatus() != null && q.getBankReviewStatus() == 2;
    }

    private String buildQuestionSnapshot(Long questionId) {
        try {
            QbQuestion q = questionMapper.selectById(questionId);
            List<QbQuestionOption> opts = optionMapper.selectByQuestionId(questionId);
            List<Long> tagIds = questionTagMapper.selectTagIdsByQuestionId(questionId);

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", q.getId());
            m.put("title", q.getTitle());
            m.put("questionType", q.getQuestionType());
            m.put("difficulty", q.getDifficulty());
            m.put("chapter", q.getChapter());
            m.put("stem", q.getStem());
            m.put("standardAnswer", q.getStandardAnswer());
            m.put("answerFormat", q.getAnswerFormat());
            m.put("analysisText", q.getAnalysisText());
            m.put("analysisSource", q.getAnalysisSource());
            m.put("tagIds", tagIds);
            m.put("options", opts);
            return objectMapper.writeValueAsString(m);
        } catch (Exception e) {
            throw new BizException(ErrorCode.BIZ_ERROR, "failed to build question snapshot: " + e.getMessage());
        }
    }

    private Map<String, Object> paperAuditSnapshot(QbPaper paper) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", paper.getId());
        snapshot.put("paperTitle", paper.getPaperTitle());
        snapshot.put("paperDesc", paper.getPaperDesc());
        snapshot.put("paperType", paper.getPaperType());
        snapshot.put("totalScore", paper.getTotalScore());
        snapshot.put("creatorId", paper.getCreatorId());
        return snapshot;
    }

    private Map<String, Object> paperQuestionAuditSnapshot(QbPaperQuestion paperQuestion) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("paperQuestionId", paperQuestion.getId());
        snapshot.put("questionId", paperQuestion.getQuestionId());
        snapshot.put("orderNo", paperQuestion.getOrderNo());
        snapshot.put("score", paperQuestion.getScore());
        return snapshot;
    }

    private void recordAudit(Long userId,
                             String action,
                             String entityType,
                             Long entityId,
                             Object beforeData,
                             Object afterData) {
        if (auditLogService == null) {
            return;
        }
        auditLogService.record(userId, action, entityType, entityId, beforeData, afterData);
    }
}
