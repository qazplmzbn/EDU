package com.xyz.question_bank_management_system.modules.user.service;

import com.xyz.question_bank_management_system.common.PageResponse;
import com.xyz.question_bank_management_system.modules.user.vo.AdminAuditLogItemVO;
import com.xyz.question_bank_management_system.modules.user.vo.AdminLoginLogItemVO;

import java.time.LocalDateTime;

public interface AdminLogService {

    PageResponse<AdminLoginLogItemVO> pageLoginLogs(String username,
                                                    Boolean successFlag,
                                                    LocalDateTime startTime,
                                                    LocalDateTime endTime,
                                                    long page,
                                                    long size);

    PageResponse<AdminAuditLogItemVO> pageAuditLogs(String username,
                                                    String action,
                                                    String entityType,
                                                    LocalDateTime startTime,
                                                    LocalDateTime endTime,
                                                    long page,
                                                    long size);
}
