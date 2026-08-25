package com.xyz.question_bank_management_system.modules.user.service.impl;

import com.xyz.question_bank_management_system.common.PageResponse;
import com.xyz.question_bank_management_system.modules.user.mapper.SysAuditLogMapper;
import com.xyz.question_bank_management_system.modules.user.mapper.SysLoginLogMapper;
import com.xyz.question_bank_management_system.modules.user.service.AdminLogService;
import com.xyz.question_bank_management_system.modules.user.vo.AdminAuditLogItemVO;
import com.xyz.question_bank_management_system.util.PageParamUtil;
import com.xyz.question_bank_management_system.modules.user.vo.AdminLoginLogItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminLogServiceImpl implements AdminLogService {

    private static final Map<String, String> ACTION_LABELS = buildActionLabels();

    private final SysAuditLogMapper sysAuditLogMapper;
    private final SysLoginLogMapper sysLoginLogMapper;

    @Override
    public PageResponse<AdminLoginLogItemVO> pageLoginLogs(String username,
                                                           Boolean successFlag,
                                                           LocalDateTime startTime,
                                                           LocalDateTime endTime,
                                                           long page,
                                                           long size) {
        long safePage = PageParamUtil.normalizePage(page);
        long safeSize = PageParamUtil.normalizeSize(size);
        long offset = PageParamUtil.offset(safePage, safeSize);
        String safeUsername = username == null ? null : username.trim();
        if (safeUsername != null && safeUsername.isEmpty()) {
            safeUsername = null;
        }
        Integer successFlagInt = successFlag == null ? null : (successFlag ? 1 : 0);

        List<AdminLoginLogItemVO> rows = sysLoginLogMapper.pageByFilter(
                safeUsername, successFlagInt, startTime, endTime, offset, safeSize
        );
        long total = sysLoginLogMapper.countByFilter(safeUsername, successFlagInt, startTime, endTime);
        return PageResponse.of(safePage, safeSize, total, rows);
    }

    @Override
    public PageResponse<AdminAuditLogItemVO> pageAuditLogs(String username,
                                                           String action,
                                                           String entityType,
                                                           LocalDateTime startTime,
                                                           LocalDateTime endTime,
                                                           long page,
                                                           long size) {
        long safePage = PageParamUtil.normalizePage(page);
        long safeSize = PageParamUtil.normalizeSize(size);
        long offset = PageParamUtil.offset(safePage, safeSize);

        String safeUsername = username == null ? null : username.trim();
        if (safeUsername != null && safeUsername.isEmpty()) {
            safeUsername = null;
        }
        String safeAction = action == null ? null : action.trim();
        if (safeAction != null && safeAction.isEmpty()) {
            safeAction = null;
        }
        String safeEntityType = entityType == null ? null : entityType.trim();
        if (safeEntityType != null && safeEntityType.isEmpty()) {
            safeEntityType = null;
        }

        List<AdminAuditLogItemVO> rows = sysAuditLogMapper.pageByFilter(
                safeUsername, safeAction, safeEntityType, startTime, endTime, offset, safeSize
        );
        rows.forEach(row -> row.setOperationLabel(resolveOperationLabel(row.getAction())));
        long total = sysAuditLogMapper.countByFilter(
                safeUsername, safeAction, safeEntityType, startTime, endTime
        );
        return PageResponse.of(safePage, safeSize, total, rows);
    }

    private String resolveOperationLabel(String action) {
        if (action == null || action.isBlank()) {
            return "未知操作";
        }
        return ACTION_LABELS.getOrDefault(action, action);
    }

    private static Map<String, String> buildActionLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("USER_REGISTER", "注册");
        labels.put("USER_CREATE", "创建用户");
        labels.put("USER_UPDATE", "更新用户");
        labels.put("USER_ROLE_UPDATE", "修改角色");
        labels.put("QUESTION_CREATE", "创建题目");
        labels.put("QUESTION_UPDATE", "更新题目");
        labels.put("QUESTION_DELETE", "删除题目");
        labels.put("QUESTION_PUBLISH", "发布题目");
        labels.put("QUESTION_BANK_SUBMIT", "提交总题库审核");
        labels.put("QUESTION_BANK_CANCEL", "取消总题库审核");
        labels.put("QUESTION_BANK_REVIEW", "审核总题库题目");
        labels.put("PAPER_CREATE", "创建试卷");
        labels.put("PAPER_UPDATE", "更新试卷");
        labels.put("PAPER_DELETE", "删除试卷");
        labels.put("PAPER_ADD_QUESTION", "试卷加题");
        labels.put("PAPER_BATCH_UPDATE", "批量调整试卷题目");
        labels.put("PAPER_UPDATE_QUESTION", "修改试卷题目");
        labels.put("PAPER_REMOVE_QUESTION", "移除试卷题目");
        labels.put("ASSIGNMENT_CREATE", "创建作业/考试");
        labels.put("ASSIGNMENT_UPDATE", "更新作业/考试");
        labels.put("ASSIGNMENT_DELETE", "删除作业/考试");
        labels.put("ASSIGNMENT_PUBLISH", "发布作业/考试");
        labels.put("ASSIGNMENT_CLOSE", "关闭作业/考试");
        labels.put("ASSIGNMENT_SET_TARGETS", "设置作业目标");
        labels.put("ASSIGNMENT_REMOVE_STUDENT_TARGET", "移除作业独立学生目标");
        labels.put("ATTEMPT_START_ASSIGNMENT", "开始作业/考试");
        labels.put("ATTEMPT_START_PRACTICE", "开始题库练习");
        labels.put("ATTEMPT_SUBMIT", "提交作答");
        return labels;
    }
}
