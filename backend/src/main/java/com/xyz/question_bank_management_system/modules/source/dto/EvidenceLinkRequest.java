package com.xyz.question_bank_management_system.modules.source.dto;
import lombok.Data; import java.math.BigDecimal;
@Data public class EvidenceLinkRequest { private Long sourceChunkId; private String supportType; private BigDecimal relevanceScore; private BigDecimal confidence; private String linkMethod; private String reviewStatus; private Integer citationOrder; }
