package com.xyz.question_bank_management_system.modules.profile.entity;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AbilityDimension {
    private Long id;
    private String dimensionCode;
    private String dimensionName;
    private String description;
    private BigDecimal scoreMin;
    private BigDecimal scoreMax;
    private String version;
    private Integer status;
}
