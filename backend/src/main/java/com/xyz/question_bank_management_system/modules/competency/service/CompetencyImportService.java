package com.xyz.question_bank_management_system.modules.competency.service;

import com.xyz.question_bank_management_system.common.PageResponse;
import com.xyz.question_bank_management_system.modules.competency.dto.CompetencyImportCommitRequest;
import com.xyz.question_bank_management_system.modules.competency.dto.CompetencyImportValidateRequest;
import com.xyz.question_bank_management_system.modules.competency.vo.DataSyncRecordVO;
import com.xyz.question_bank_management_system.modules.competency.vo.ImportValidationVO;

public interface CompetencyImportService {
    ImportValidationVO validate(CompetencyImportValidateRequest request);
    ImportValidationVO commit(CompetencyImportCommitRequest request, Long operatorId);
    PageResponse<DataSyncRecordVO> records(Integer page, Integer size);
    DataSyncRecordVO record(Long id);
}
