package com.xyz.question_bank_management_system.modules.profile.entity;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class StudentProfileCategoryStat {
    private Long id;
    private Long userId;
    private String categoryType;
    private String periodType;
    private Integer totalCount;
    private Integer strongCount;
    private Integer weakCount;
    private BigDecimal averageScore;
    private BigDecimal coverageRate;
    private String topStrengthsJson;
    private String topWeaknessesJson;
}
