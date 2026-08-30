package com.xyz.question_bank_management_system.modules.agent.service;
import java.time.LocalDateTime;import java.util.List;
public interface ResourceAssessmentReleaseService {String release(Long userId,String bundleCode,String itemCode,LocalDateTime expiresAt);List<?> active(Long userId,String bundleCode);}
