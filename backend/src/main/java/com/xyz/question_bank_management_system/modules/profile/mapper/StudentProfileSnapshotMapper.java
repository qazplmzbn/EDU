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
    @Select("SELECT * FROM student_profile_snapshot WHERE user_id=#{userId} AND course_id=#{courseId} ORDER BY profile_version DESC,id DESC LIMIT 1") StudentProfileSnapshot selectLatest(@Param("userId")Long userId,@Param("courseId")Long courseId);
    @Select("SELECT * FROM student_profile_snapshot WHERE user_id=#{userId} AND course_id=#{courseId} ORDER BY profile_version DESC,id DESC LIMIT 1 FOR UPDATE") StudentProfileSnapshot selectLatestForUpdate(@Param("userId")Long userId,@Param("courseId")Long courseId);
    @Insert("INSERT INTO student_profile_snapshot(user_id,course_id,profile_version,calculated_at,algorithm_version,correlation_id,knowledge_state_json,resource_preference_json,cognitive_profile_json,initiative_json,regularity_json,profile_summary,trigger_type,trigger_id,evidence_count,created_at) VALUES(#{userId},#{courseId},#{profileVersion},#{calculatedAt},#{algorithmVersion},#{correlationId},#{knowledgeStateJson},#{resourcePreferenceJson},#{cognitiveProfileJson},#{initiativeJson},#{regularityJson},#{profileSummary},#{triggerType},#{triggerId},#{evidenceCount},NOW(3))") @Options(useGeneratedKeys=true,keyProperty="id") int insertVersioned(StudentProfileSnapshot snapshot);
}
