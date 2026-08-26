package com.xyz.question_bank_management_system.modules.profile.mapper;

import com.xyz.question_bank_management_system.modules.profile.entity.StudentAbilityState;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface StudentAbilityStateMapper {
    @Select("SELECT s.*,d.dimension_code,d.dimension_name FROM student_ability_state s JOIN ability_dimension d ON d.id=s.dimension_id WHERE s.user_id=#{userId} ORDER BY d.id")
    List<StudentAbilityState> selectByUserId(@Param("userId") Long userId);

    @Select("SELECT s.*,d.dimension_code,d.dimension_name FROM student_ability_state s JOIN ability_dimension d ON d.id=s.dimension_id WHERE s.user_id=#{userId} AND d.dimension_code=#{code} AND d.version='v1' LIMIT 1")
    StudentAbilityState selectByUserIdAndCode(@Param("userId") Long userId, @Param("code") String code);

    @Insert("INSERT INTO student_ability_state(user_id,dimension_id,score,level,confidence,evidence_count,updated_at) VALUES(#{userId},#{dimensionId},#{score},#{level},#{confidence},#{evidenceCount},NOW(3)) ON DUPLICATE KEY UPDATE score=VALUES(score),level=VALUES(level),confidence=VALUES(confidence),evidence_count=VALUES(evidence_count),updated_at=NOW(3)")
    int upsert(@Param("userId") Long userId, @Param("dimensionId") Long dimensionId, @Param("score") BigDecimal score,
               @Param("level") String level, @Param("confidence") BigDecimal confidence, @Param("evidenceCount") int evidenceCount);
}
