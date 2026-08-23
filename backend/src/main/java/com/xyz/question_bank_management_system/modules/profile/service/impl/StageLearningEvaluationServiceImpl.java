package com.xyz.question_bank_management_system.modules.profile.service.impl;

import com.xyz.question_bank_management_system.modules.bank.entity.QbAttempt;
import com.xyz.question_bank_management_system.modules.profile.entity.QbTagMastery;
import com.xyz.question_bank_management_system.modules.profile.entity.QbUserAbility;
import com.xyz.question_bank_management_system.modules.user.entity.SysUser;
import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.exception.ErrorCode;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbAttemptMapper;
import com.xyz.question_bank_management_system.modules.bank.mapper.QbAssignmentTargetMapper;
import com.xyz.question_bank_management_system.modules.org.mapper.QbClassMemberMapper;
import com.xyz.question_bank_management_system.modules.profile.mapper.QbTagMasteryMapper;
import com.xyz.question_bank_management_system.modules.profile.mapper.QbUserAbilityMapper;
import com.xyz.question_bank_management_system.modules.profile.service.StageLearningEvaluationService;
import com.xyz.question_bank_management_system.modules.user.mapper.SysUserMapper;
import com.xyz.question_bank_management_system.modules.profile.vo.StageLearningEvaluationVO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

@Service
public class StageLearningEvaluationServiceImpl implements StageLearningEvaluationService {

    private static final String PLACEHOLDER_TEXT =
            "阶段性学习评价算法框架已预留：后续可接入学生掌握状态超图、阶段目标达成度、知识点迁移路径和学习行为序列模型。";

    private final QbAttemptMapper attemptMapper;
    private final QbAssignmentTargetMapper assignmentTargetMapper;
    private final QbTagMasteryMapper tagMasteryMapper;
    private final QbUserAbilityMapper userAbilityMapper;
    private final QbClassMemberMapper classMemberMapper;
    private final SysUserMapper sysUserMapper;

    public StageLearningEvaluationServiceImpl(QbAttemptMapper attemptMapper,
                                          QbAssignmentTargetMapper assignmentTargetMapper,
                                          QbTagMasteryMapper tagMasteryMapper,
                                          QbUserAbilityMapper userAbilityMapper,
                                          QbClassMemberMapper classMemberMapper,
                                          SysUserMapper sysUserMapper) {
        this.attemptMapper = attemptMapper;
        this.assignmentTargetMapper = assignmentTargetMapper;
        this.tagMasteryMapper = tagMasteryMapper;
        this.userAbilityMapper = userAbilityMapper;
        this.classMemberMapper = classMemberMapper;
        this.sysUserMapper = sysUserMapper;
    }

    public StageLearningEvaluationVO myEvaluation(Long userId, String stage, LocalDate startDate, LocalDate endDate) {
        return buildEvaluation(userId, resolveStage(stage, startDate, endDate));
    }

    public List<StageLearningEvaluationVO> teacherEvaluations(Long teacherId,
                                                              boolean admin,
                                                              Long studentId,
                                                              String stage,
                                                              LocalDate startDate,
                                                              LocalDate endDate) {
        StageWindow window = resolveStage(stage, startDate, endDate);
        List<Long> studentIds;
        if (studentId != null) {
            if (!admin && !teacherVisibleStudentIds(teacherId).contains(studentId)) {
                throw BizException.of(ErrorCode.FORBIDDEN, "只能查看自己班级学生的阶段评价");
            }
            studentIds = List.of(studentId);
        } else {
            studentIds = admin ? attemptMapper.listRecentStudentIds(80) : teacherVisibleStudentIds(teacherId);
        }
        return studentIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .limit(80)
                .map(id -> buildEvaluation(id, window))
                .toList();
    }

    private List<Long> teacherVisibleStudentIds(Long teacherId) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        List<Long> classStudentIds = classMemberMapper.listStudentIdsByTeacherId(teacherId);
        if (classStudentIds != null) {
            ids.addAll(classStudentIds);
        }
        List<Long> targetStudentIds = assignmentTargetMapper.listUserIdsByTeacherAssignments(teacherId);
        if (targetStudentIds != null) {
            ids.addAll(targetStudentIds);
        }
        List<Long> attemptedStudentIds = attemptMapper.listStudentIdsByTeacherAssignments(teacherId);
        if (attemptedStudentIds != null) {
            ids.addAll(attemptedStudentIds);
        }
        return ids.stream().filter(Objects::nonNull).toList();
    }

    private StageLearningEvaluationVO buildEvaluation(Long studentId, StageWindow window) {
        SysUser student = sysUserMapper.selectById(studentId);
        List<QbAttempt> attempts = attemptMapper.selectByUserAndSubmittedRange(
                studentId,
                window.start().atStartOfDay(),
                window.end().plusDays(1).atStartOfDay()
        );
        List<QbTagMastery> masteryRows = tagMasteryMapper.selectByUserIdAndTagType(studentId, null);
        QbUserAbility ability = userAbilityMapper.selectByUserId(studentId);

        int completed = attempts == null ? 0 : attempts.size();
        double averageScore = attempts == null || attempts.isEmpty()
                ? 0.0
                : attempts.stream()
                .map(QbAttempt::getTotalScore)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
        double masteryAverage = masteryRows == null || masteryRows.isEmpty()
                ? 0.0
                : masteryRows.stream()
                .map(QbTagMastery::getMasteryValue)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
        int abilityScore = ability == null || ability.getAbilityScore() == null ? 0 : ability.getAbilityScore();

        StageLearningEvaluationVO vo = new StageLearningEvaluationVO();
        vo.setStudentId(studentId);
        vo.setStudentName(displayName(student, studentId));
        vo.setStageKey(window.key());
        vo.setStageName(window.name());
        vo.setStageStart(window.start());
        vo.setStageEnd(window.end());
        vo.setGeneratedAt(LocalDateTime.now());
        vo.setAbilityScore(abilityScore);
        vo.setAttemptCount(completed);
        vo.setCompletedAttemptCount(completed);
        vo.setAverageScore(round1(averageScore));
        vo.setMasteryAverage(round3(masteryAverage));
        vo.setOverallLevel(levelOf((int) Math.round((abilityScore + averageScore + masteryAverage * 100) / 3.0)));
        vo.setAlgorithmStatus("FRAMEWORK_ONLY");
        vo.setAlgorithmPlaceholder(PLACEHOLDER_TEXT);
        vo.setSummary(buildSummary(vo));
        vo.setDimensions(List.of(
                dimension("ABILITY", "能力水平", abilityScore, "由现有能力值生成，后续可替换为阶段能力增长模型。"),
                dimension("MASTERY", "知识掌握", (int) Math.round(masteryAverage * 100), "由知识点掌握度均值生成，后续可接入掌握状态超图。"),
                dimension("PERFORMANCE", "作答表现", (int) Math.round(averageScore), "由阶段作答平均分生成，后续可加入题型、难度和目标达成度。"),
                dimension("PARTICIPATION", "学习参与", Math.min(100, completed * 12), "由阶段作答次数生成，后续可融合资源学习和行为序列。")
        ));
        vo.setWeakKnowledgePoints(buildWeakPoints(masteryRows));
        vo.setSuggestions(buildSuggestions(vo));
        return vo;
    }

    private List<StageLearningEvaluationVO.WeakKnowledgePoint> buildWeakPoints(List<QbTagMastery> masteryRows) {
        if (masteryRows == null) {
            return List.of();
        }
        return masteryRows.stream()
                .filter(row -> row.getTagId() != null)
                .sorted(Comparator.comparing(row -> row.getMasteryValue() == null ? 0.0 : row.getMasteryValue()))
                .limit(6)
                .map(row -> {
                    StageLearningEvaluationVO.WeakKnowledgePoint point = new StageLearningEvaluationVO.WeakKnowledgePoint();
                    point.setTagId(row.getTagId());
                    point.setTagName(row.getTagName());
                    point.setMasteryValue(row.getMasteryValue() == null ? 0.0 : row.getMasteryValue());
                    point.setAttemptCount(row.getAttemptCount());
                    return point;
                })
                .toList();
    }

    private List<String> buildSuggestions(StageLearningEvaluationVO vo) {
        return List.of(
                "根据阶段评价框架，优先复习掌握度较低的知识点。",
                "后续算法可在此接入掌握状态超图，自动生成下一阶段训练题目。",
                "教师可结合该评价结果调整作业难度、知识点覆盖和复核重点。"
        );
    }

    private StageLearningEvaluationVO.Dimension dimension(String code, String name, int score, String description) {
        int safeScore = Math.max(0, Math.min(100, score));
        StageLearningEvaluationVO.Dimension dimension = new StageLearningEvaluationVO.Dimension();
        dimension.setCode(code);
        dimension.setName(name);
        dimension.setScore(safeScore);
        dimension.setLevel(levelOf(safeScore));
        dimension.setDescription(description);
        return dimension;
    }

    private String buildSummary(StageLearningEvaluationVO vo) {
        return "当前阶段评价已生成基础画像：能力值 " + vo.getAbilityScore()
                + "，阶段作答 " + vo.getCompletedAttemptCount()
                + " 次，平均分 " + vo.getAverageScore()
                + "。核心评价算法后续可在该模块中补充。";
    }

    private StageWindow resolveStage(String stage, LocalDate startDate, LocalDate endDate) {
        String key = stage == null || stage.isBlank() ? "month" : stage.trim().toLowerCase();
        LocalDate today = LocalDate.now();
        if (startDate != null && endDate != null) {
            return new StageWindow("custom", "自定义阶段", startDate, endDate);
        }
        if ("week".equals(key)) {
            LocalDate start = today.minusDays(6);
            return new StageWindow("week", "近 7 天", start, today);
        }
        if ("term".equals(key)) {
            LocalDate start = today.getMonthValue() >= 9 ? LocalDate.of(today.getYear(), 9, 1) : LocalDate.of(today.getYear(), 3, 1);
            return new StageWindow("term", "本学期", start, today);
        }
        LocalDate start = today.with(TemporalAdjusters.firstDayOfMonth());
        return new StageWindow("month", "本月", start, today);
    }

    private String levelOf(int score) {
        if (score >= 85) return "优秀";
        if (score >= 70) return "良好";
        if (score >= 55) return "发展中";
        return "需关注";
    }

    private String displayName(SysUser user, Long fallbackId) {
        if (user == null) return "学生 " + fallbackId;
        if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) return user.getDisplayName();
        return user.getUsername() == null ? "学生 " + fallbackId : user.getUsername();
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private double round3(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private record StageWindow(String key, String name, LocalDate start, LocalDate end) {
    }
}

