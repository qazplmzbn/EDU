package com.xyz.question_bank_management_system.modules.competency.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data public class CareerMappingImportBatch { private Long id; private String batchCode; private String fileName; private String fileHash; private String status; private Integer rowCount; private Integer candidateCount; private Integer unresolvedCount; private Integer outOfCatalogCount; private Long createdBy; private LocalDateTime createdAt; }
