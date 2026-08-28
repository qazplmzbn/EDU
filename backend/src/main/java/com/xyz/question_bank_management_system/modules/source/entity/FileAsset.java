package com.xyz.question_bank_management_system.modules.source.entity;
import lombok.Data; import java.time.LocalDateTime;
@Data public class FileAsset { private Long id; private String bizType; private Long bizId; private String fileName; private String fileExt; private String mimeType; private String storageType; private String storagePath; private Long fileSize; private String fileHash; private Long uploadedBy; private LocalDateTime createdAt; private Integer isDeleted; }
