package com.xyz.question_bank_management_system.modules.profile.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.exception.ErrorCode;
import com.xyz.question_bank_management_system.modules.org.mapper.QbClassMemberMapper;
import com.xyz.question_bank_management_system.modules.profile.entity.StageEvaluation;
import com.xyz.question_bank_management_system.modules.profile.entity.StudentAbilityState;
import com.xyz.question_bank_management_system.modules.profile.entity.StudentProfileSnapshot;
import com.xyz.question_bank_management_system.modules.profile.mapper.StageEvaluationMapper;
import com.xyz.question_bank_management_system.modules.profile.mapper.StudentAbilityStateMapper;
import com.xyz.question_bank_management_system.modules.profile.service.StageLearningEvaluationService;
import com.xyz.question_bank_management_system.modules.profile.service.StudentProfileService;
import com.xyz.question_bank_management_system.modules.profile.vo.StageLearningEvaluationVO;
import com.xyz.question_bank_management_system.modules.user.entity.SysUser;
import com.xyz.question_bank_management_system.modules.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StageLearningEvaluationServiceImpl implements StageLearningEvaluationService {
    private final StageEvaluationMapper evaluationMapper;
    private final StudentAbilityStateMapper abilityStateMapper;
    private final StudentProfileService studentProfileService;
    private final QbClassMemberMapper classMemberMapper;
    private final SysUserMapper userMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public StageLearningEvaluationVO generate(Long operatorId, boolean admin, Long studentId, String stage, LocalDate startDate, LocalDate endDate) {
        requireVisible(operatorId, admin, studentId);
        StageWindow window = window(stage, startDate, endDate);
        studentProfileService.refreshAssessment(studentId);
        StudentProfileSnapshot snapshot = studentProfileService.createSnapshot(studentId, "assessment", null);
        List<StudentAbilityState> states = abilityStateMapper.selectByUserId(studentId);
        double overall = states.stream().map(StudentAbilityState::getScore).filter(Objects::nonNull).mapToDouble(BigDecimal::doubleValue).average().orElse(0d);
        StageEvaluation evaluation = new StageEvaluation();
        evaluation.setUserId(studentId); evaluation.setStageType(window.type); evaluation.setStartDate(window.start); evaluation.setEndDate(window.end);
        evaluation.setProfileSnapshotId(snapshot.getId()); evaluation.setOverallScore(BigDecimal.valueOf(overall));
        evaluation.setDimensionScoresJson(json(states));
        evaluation.setEvaluationText("基于持久化学生画像生成的" + window.name + "阶段评价。");
        evaluation.setEvaluatorType(admin ? "system" : "teacher"); evaluation.setStatus("final"); evaluationMapper.insert(evaluation);
        return toVo(evaluation, true);
    }

    @Override
    public StageLearningEvaluationVO myEvaluation(Long userId, String stage, LocalDate startDate, LocalDate endDate) {
        StageWindow window = window(stage, startDate, endDate);
        StageEvaluation evaluation = evaluationMapper.latest(userId, window.type, window.start, window.end);
        return evaluation == null ? notGenerated(userId, window) : toVo(evaluation, true);
    }

    @Override
    public List<StageLearningEvaluationVO> teacherEvaluations(Long teacherId, boolean admin, Long studentId, String stage, LocalDate startDate, LocalDate endDate) {
        StageWindow window = window(stage, startDate, endDate);
        List<Long> ids = studentId == null ? (admin ? userMapper.listActiveStudentIds() : classMemberMapper.listStudentIdsByTeacherId(teacherId)) : List.of(studentId);
        if (studentId != null) requireVisible(teacherId, admin, studentId);
        return ids.stream().filter(Objects::nonNull).distinct().map(id -> {
            StageEvaluation evaluation = evaluationMapper.latest(id, window.type, window.start, window.end);
            return evaluation == null ? notGenerated(id, window) : toVo(evaluation, true);
        }).toList();
    }

    @Override
    public List<StageLearningEvaluationVO> history(Long requesterId, boolean admin, Long studentId, int limit) {
        requireVisible(requesterId, admin, studentId);
        return evaluationMapper.history(studentId, Math.max(1, Math.min(100, limit))).stream().map(e -> toVo(e, true)).toList();
    }

    private StageLearningEvaluationVO toVo(StageEvaluation evaluation, boolean generated) {
        StageLearningEvaluationVO vo = new StageLearningEvaluationVO();
        SysUser student = userMapper.selectById(evaluation.getUserId());
        vo.setEvaluationId(evaluation.getId()); vo.setGenerated(generated); vo.setStudentId(evaluation.getUserId());
        vo.setStudentName(student == null ? String.valueOf(evaluation.getUserId()) : student.getDisplayName());
        vo.setStageKey(evaluation.getStageType()); vo.setStageName(stageName(evaluation.getStageType()));
        vo.setStageStart(evaluation.getStartDate()); vo.setStageEnd(evaluation.getEndDate()); vo.setGeneratedAt(evaluation.getCreatedAt());
        vo.setAbilityScore(studentProfileService.abilityScore(evaluation.getUserId()));
        vo.setOverallLevel(level(evaluation.getOverallScore() == null ? 0 : evaluation.getOverallScore().doubleValue()));
        vo.setSummary(evaluation.getEvaluationText()); vo.setAlgorithmStatus("PERSISTED");
        vo.setAlgorithmPlaceholder("评价结果已固化为历史版本，不会因后续学习行为而改变。");
        vo.setDimensions(dimensions(evaluation.getDimensionScoresJson()));
        vo.setSuggestions(List.of("优先复习当前薄弱知识点。", "完成新的学习活动后，可由教师重新生成下一版阶段评价。"));
        return vo;
    }

    private StageLearningEvaluationVO notGenerated(Long userId, StageWindow window) {
        StageLearningEvaluationVO vo = new StageLearningEvaluationVO(); vo.setGenerated(false); vo.setStudentId(userId);
        SysUser student = userMapper.selectById(userId); vo.setStudentName(student == null ? String.valueOf(userId) : student.getDisplayName());
        vo.setStageKey(window.type); vo.setStageName(window.name); vo.setStageStart(window.start); vo.setStageEnd(window.end);
        vo.setAbilityScore(studentProfileService.abilityScore(userId)); vo.setOverallLevel("pending");
        vo.setSummary("该阶段尚未生成正式评价。"); vo.setAlgorithmStatus("NOT_GENERATED");
        vo.setAlgorithmPlaceholder("请由本班教师或管理员显式生成阶段评价。"); return vo;
    }

    private List<StageLearningEvaluationVO.Dimension> dimensions(String json) {
        try {
            List<StudentAbilityState> states = objectMapper.readValue(json == null ? "[]" : json, new TypeReference<List<StudentAbilityState>>() {});
            return states.stream().map(state -> { StageLearningEvaluationVO.Dimension d = new StageLearningEvaluationVO.Dimension();
                d.setCode(state.getDimensionCode()); d.setName(state.getDimensionName()); int score = state.getScore() == null ? 0 : state.getScore().intValue();
                d.setScore(score); d.setLevel(level(score)); d.setDescription("持久化能力维度状态"); return d; }).toList();
        } catch (Exception ignored) { return List.of(); }
    }

    private void requireVisible(Long teacherId, boolean admin, Long studentId) {
        if (studentId == null) throw BizException.of(ErrorCode.PARAM_ERROR, "学生不能为空");
        if (!admin && !classMemberMapper.listStudentIdsByTeacherId(teacherId).contains(studentId)) throw BizException.of(ErrorCode.FORBIDDEN, "只能操作自己班级学生的阶段评价");
    }
    private StageWindow window(String stage, LocalDate start, LocalDate end) {
        if (start != null || end != null) {
            if (start == null || end == null || end.isBefore(start)) throw BizException.of(ErrorCode.PARAM_ERROR, "自定义阶段必须提供合法的开始和结束日期");
            return new StageWindow("custom", "自定义阶段", start, end);
        }
        LocalDate today = LocalDate.now(); String type = stage == null || stage.isBlank() ? "month" : stage.toLowerCase(Locale.ROOT);
        return switch (type) { case "week" -> new StageWindow("week", "本周", today.with(java.time.DayOfWeek.MONDAY), today.with(java.time.DayOfWeek.SUNDAY));
            case "term" -> new StageWindow("term", "本学期", today.getMonthValue() <= 6 ? LocalDate.of(today.getYear(), 1, 1) : LocalDate.of(today.getYear(), 7, 1), today.getMonthValue() <= 6 ? LocalDate.of(today.getYear(), 6, 30) : LocalDate.of(today.getYear(), 12, 31));
            default -> new StageWindow("month", "本月", today.withDayOfMonth(1), today.with(TemporalAdjusters.lastDayOfMonth())); };
    }
    private String stageName(String type) { return switch (type) { case "week" -> "本周"; case "term" -> "本学期"; case "custom" -> "自定义阶段"; default -> "本月"; }; }
    private String level(double score) { return score >= 80 ? "mastered" : score >= 50 ? "basic" : "weak"; }
    private String json(Object value) { try { return objectMapper.writeValueAsString(value); } catch (Exception e) { return "[]"; } }
    private record StageWindow(String type, String name, LocalDate start, LocalDate end) {}
}
