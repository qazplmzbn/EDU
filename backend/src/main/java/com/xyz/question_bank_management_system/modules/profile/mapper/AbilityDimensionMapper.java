package com.xyz.question_bank_management_system.modules.profile.mapper;

import com.xyz.question_bank_management_system.modules.profile.entity.AbilityDimension;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface AbilityDimensionMapper {
    @Select("SELECT * FROM ability_dimension WHERE status=1 AND version='v1' ORDER BY id")
    List<AbilityDimension> selectActive();

    @Select("SELECT * FROM ability_dimension WHERE dimension_code=#{code} AND version='v1' AND status=1 LIMIT 1")
    AbilityDimension selectActiveByCode(@Param("code") String code);
}
