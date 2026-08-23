package com.xyz.question_bank_management_system.modules.bank.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AppealMyItemVO {
    private Long appealId;
    private Long answerId;
    private Integer appealStatus;
    private LocalDateTime createdAt;
}
