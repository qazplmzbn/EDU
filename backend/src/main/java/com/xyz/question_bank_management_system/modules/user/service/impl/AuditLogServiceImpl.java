package com.xyz.question_bank_management_system.modules.user.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xyz.question_bank_management_system.modules.user.entity.SysAuditLog;
import com.xyz.question_bank_management_system.modules.user.mapper.SysAuditLogMapper;
import com.xyz.question_bank_management_system.modules.user.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final SysAuditLogMapper sysAuditLogMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void record(Long userId,
                       String action,
                       String entityType,
                       Long entityId,
                       Object beforeData,
                       Object afterData) {
        try {
            SysAuditLog logEntity = new SysAuditLog();
            logEntity.setUserId(userId);
            logEntity.setAction(action);
            logEntity.setEntityType(entityType);
            logEntity.setEntityId(entityId);
            logEntity.setBeforeJson(toJson(beforeData));
            logEntity.setAfterJson(toJson(afterData));
            logEntity.setIpAddr(resolveClientIp());
            sysAuditLogMapper.insert(logEntity);
        } catch (Exception ex) {
            log.warn("Failed to write audit log action={} entityType={} entityId={}", action, entityType, entityId, ex);
        }
    }

    @Override
    public void recordRequired(Long userId,
                               String action,
                               String entityType,
                               Long entityId,
                               Object beforeData,
                               Object afterData) {
        SysAuditLog logEntity = new SysAuditLog();
        logEntity.setUserId(userId);
        logEntity.setAction(action);
        logEntity.setEntityType(entityType);
        logEntity.setEntityId(entityId);
        logEntity.setBeforeJson(toJson(beforeData));
        logEntity.setAfterJson(toJson(afterData));
        logEntity.setIpAddr(resolveClientIp());
        if (sysAuditLogMapper.insert(logEntity) != 1) {
            throw new IllegalStateException("Failed to write required audit log");
        }
    }

    private String toJson(Object data) {
        if (data == null) {
            return null;
        }
        if (data instanceof String str) {
            return str;
        }
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception ex) {
            return String.valueOf(data);
        }
    }

    private String resolveClientIp() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof ServletRequestAttributes servletAttrs)) {
            return null;
        }
        HttpServletRequest request = servletAttrs.getRequest();
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
