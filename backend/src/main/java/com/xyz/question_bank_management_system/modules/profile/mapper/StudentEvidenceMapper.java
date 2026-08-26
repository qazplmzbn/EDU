package com.xyz.question_bank_management_system.modules.profile.mapper;

import com.xyz.question_bank_management_system.modules.profile.entity.StudentEvidence;
import org.apache.ibatis.annotations.*;

@Mapper
public interface StudentEvidenceMapper {
    @Insert("INSERT IGNORE INTO student_evidence(user_id,evidence_type,source_entity_type,source_entity_id,target_type,target_id,evidence_value,evidence_direction,confidence,evidence_text,occurred_at,extract_version,created_at) VALUES(#{userId},#{evidenceType},#{sourceEntityType},#{sourceEntityId},#{targetType},#{targetId},#{evidenceValue},#{evidenceDirection},#{confidence},#{evidenceText},#{occurredAt},#{extractVersion},NOW(3))")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertIgnore(StudentEvidence evidence);

    @Select("SELECT COUNT(*) FROM student_evidence WHERE user_id=#{userId}")
    int countByUserId(@Param("userId") Long userId);
}
