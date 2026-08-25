package com.xyz.question_bank_management_system.modules.competency.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.exception.ErrorCode;
import com.xyz.question_bank_management_system.modules.competency.dto.CompetencyImportCommitRequest;
import com.xyz.question_bank_management_system.modules.competency.dto.CompetencyImportValidateRequest;
import com.xyz.question_bank_management_system.modules.competency.dto.CompetencyImportValidateRequest.KnowledgePointInput;
import com.xyz.question_bank_management_system.modules.competency.dto.CompetencyImportValidateRequest.OccupationInput;
import com.xyz.question_bank_management_system.modules.competency.mapper.DataSyncRecordMapper;
import com.xyz.question_bank_management_system.modules.competency.mapper.OccupationMapper;
import com.xyz.question_bank_management_system.modules.competency.mapper.SkillMapper;
import com.xyz.question_bank_management_system.modules.competency.service.DataSyncRecordService;
import com.xyz.question_bank_management_system.modules.competency.vo.ImportValidationVO;
import com.xyz.question_bank_management_system.modules.knowledge.mapper.KnowledgePointMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CompetencyImportServiceImplTest {

    @Mock private OccupationMapper occupationMapper;
    @Mock private SkillMapper skillMapper;
    @Mock private KnowledgePointMapper knowledgePointMapper;
    @Mock private DataSyncRecordMapper recordMapper;
    @Mock private DataSyncRecordService recordService;
    @Mock private CompetencyImportWriter writer;

    private CompetencyImportServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CompetencyImportServiceImpl(new ObjectMapper(), occupationMapper, skillMapper,
                knowledgePointMapper, recordMapper, recordService, writer);
    }

    @Test
    void validate_shouldRejectDuplicateOccupationBusinessKeys() {
        CompetencyImportValidateRequest request = validRequest();
        OccupationInput first = occupation("occ-java", "Java 开发工程师");
        OccupationInput duplicate = occupation("occ-java", "Java 后端工程师");
        request.setOccupations(List.of(first, duplicate));

        ImportValidationVO result = service.validate(request);

        assertFalse(result.isValid());
        assertEquals(1, result.getFailedCount());
    }

    @Test
    void validate_shouldRejectKnowledgePointWithUnknownParent() {
        CompetencyImportValidateRequest request = validRequest();
        KnowledgePointInput child = new KnowledgePointInput();
        child.setCode("kp-child");
        child.setName("子知识点");
        child.setParentCode("missing-parent");
        request.setKnowledgePoints(List.of(child));

        ImportValidationVO result = service.validate(request);

        assertFalse(result.isValid());
        assertEquals(1, result.getFailedCount());
    }

    @Test
    void commit_shouldRejectPayloadWhoseValidationHashChanged() {
        CompetencyImportCommitRequest request = new CompetencyImportCommitRequest();
        request.setSourceName("stage04-test");
        request.setSyncVersion("v1");
        request.setValidationHash("stale-validation-hash");

        BizException ex = assertThrows(BizException.class, () -> service.commit(request, 1001L));

        assertEquals(ErrorCode.CONFLICT, ex.getCode());
        verifyNoInteractions(recordService, writer);
    }

    private CompetencyImportValidateRequest validRequest() {
        CompetencyImportValidateRequest request = new CompetencyImportValidateRequest();
        request.setSourceName("stage04-test");
        request.setSyncVersion("v1");
        return request;
    }

    private OccupationInput occupation(String sourceRef, String name) {
        OccupationInput input = new OccupationInput();
        input.setSourceRef(sourceRef);
        input.setNameZh(name);
        return input;
    }
}
