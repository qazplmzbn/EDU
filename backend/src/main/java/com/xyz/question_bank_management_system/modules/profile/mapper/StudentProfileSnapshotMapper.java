package com.xyz.question_bank_management_system.modules.profile.mapper;

import com.xyz.question_bank_management_system.modules.profile.entity.StudentProfileSnapshot;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface StudentProfileSnapshotMapper {
    @Insert("INSERT INTO student_profile_snapshot(user_id,basic_state_json,knowledge_state_json,skill_state_json,ability_state_json,preference_state_json,goal_state_json,category_stat_json,profile_summary,trigger_type,trigger_id,evidence_count,created_at) VALUES(#{userId},#{basicStateJson},#{knowledgeStateJson},#{skillStateJson},#{abilityStateJson},#{preferenceStateJson},#{goalStateJson},#{categoryStatJson},#{profileSummary},#{triggerType},#{triggerId},#{evidenceCount},NOW(3))")
    @Options(useGeneratedKeys=true,keyProperty="id")
    int insert(StudentProfileSnapshot snapshot);

    @Select("SELECT * FROM student_profile_snapshot WHERE id=#{id} AND user_id=#{userId}")
    StudentProfileSnapshot selectByIdAndUserId(@Param("id") Long id,@Param("userId") Long userId);

    @Select("SELECT * FROM student_profile_snapshot WHERE user_id=#{userId} ORDER BY created_at DESC,id DESC LIMIT #{limit}")
    List<StudentProfileSnapshot> selectRecent(@Param("userId") Long userId,@Param("limit") int limit);
}
