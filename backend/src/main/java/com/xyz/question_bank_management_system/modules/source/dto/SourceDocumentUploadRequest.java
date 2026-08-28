package com.xyz.question_bank_management_system.modules.source.dto;
import lombok.Data;
@Data public class SourceDocumentUploadRequest { private String title; private String documentType; private String authorOrg; private String sourceUrl; private String version; private Integer authorityLevel; }
