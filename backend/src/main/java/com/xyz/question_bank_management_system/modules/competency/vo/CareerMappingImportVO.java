package com.xyz.question_bank_management_system.modules.competency.vo;

import lombok.Data;

@Data public class CareerMappingImportVO { private String batchCode; private Integer rowCount; private Integer candidateCount; private Integer unresolvedCount; private Integer outOfCatalogCount; private String status; }
