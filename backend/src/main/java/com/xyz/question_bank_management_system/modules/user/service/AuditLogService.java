package com.xyz.question_bank_management_system.modules.user.service;

public interface AuditLogService {

    void record(Long userId,
                String action,
                String entityType,
                Long entityId,
                Object beforeData,
                Object afterData);

    void recordRequired(Long userId,
                        String action,
                        String entityType,
                        Long entityId,
                        Object beforeData,
                        Object afterData);
}
