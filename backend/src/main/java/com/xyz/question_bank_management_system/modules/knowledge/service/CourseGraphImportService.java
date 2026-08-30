package com.xyz.question_bank_management_system.modules.knowledge.service;

import com.xyz.question_bank_management_system.modules.knowledge.vo.CourseGraphImportDetailVO;
import com.xyz.question_bank_management_system.modules.knowledge.vo.CourseGraphValidationVO;
import org.springframework.web.multipart.MultipartFile;

public interface CourseGraphImportService {
    CourseGraphValidationVO validate(MultipartFile file, String mode);
    CourseGraphImportDetailVO commit(MultipartFile file, String mode, String validationHash, String idempotencyKey, Long operatorId);
    CourseGraphImportDetailVO detail(String importCode);
    CourseGraphImportDetailVO approve(String importCode, Long reviewerId);
}
