package com.xyz.question_bank_management_system.modules.competency.mapper;

import com.xyz.question_bank_management_system.modules.competency.entity.*;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CareerRecommendationMapper {
    @Select("SELECT os.id AS occupation_skill_id,os.occupation_id,os.skill_id,s.name_zh AS skill_name,os.requirement_type,os.importance_score,os.required_level,os.published_batch_code,os.required_level_version FROM occupation_skill os JOIN skill s ON s.id=os.skill_id AND s.is_deleted=0 WHERE os.occupation_id=#{occupationId} AND os.required_level IS NOT NULL AND os.published_batch_code IS NOT NULL ORDER BY os.requirement_type='essential' DESC,os.importance_score DESC,os.id")
    List<CareerSkillRequirement> selectPublishedRequirements(@Param("occupationId") Long occupationId);

    @Select({"<script>", "SELECT sk.skill_id,sk.knowledge_point_id,sk.requirement_type,sk.weight AS mapping_weight,sk.confidence AS mapping_confidence,COALESCE(sks.mastery_value,0) AS mastery_value,COALESCE(sks.confidence,0) AS state_confidence,COALESCE(sks.evidence_count,0) AS evidence_count FROM skill_knowledge sk JOIN knowledge_point kp ON kp.id=sk.knowledge_point_id AND kp.is_deleted=0 LEFT JOIN student_knowledge_state sks ON sks.knowledge_point_id=sk.knowledge_point_id AND sks.user_id=#{userId} WHERE sk.skill_id IN <foreach collection='skillIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>", "</script>"})
    List<CareerKnowledgeEvidence> selectKnowledgeEvidence(@Param("userId") Long userId, @Param("skillIds") List<Long> skillIds);

    @Insert("INSERT INTO student_skill_state(user_id,skill_id,proficiency_value,core_proficiency_value,proficiency_level,confidence,knowledge_coverage_rate,evidence_count,calculation_version,calculated_at,last_evidence_at,updated_at) VALUES(#{userId},#{skillId},#{proficiencyValue},#{coreProficiencyValue},#{proficiencyLevel},#{confidence},#{knowledgeCoverageRate},#{evidenceCount},#{calculationVersion},NOW(3),NOW(3),NOW(3)) ON DUPLICATE KEY UPDATE proficiency_value=VALUES(proficiency_value),core_proficiency_value=VALUES(core_proficiency_value),proficiency_level=VALUES(proficiency_level),confidence=VALUES(confidence),knowledge_coverage_rate=VALUES(knowledge_coverage_rate),evidence_count=VALUES(evidence_count),calculation_version=VALUES(calculation_version),calculated_at=VALUES(calculated_at),updated_at=NOW(3)")
    int upsertStudentSkillState(CareerStudentSkillState state);

    @Insert("INSERT INTO student_occupation_skill_gap(snapshot_code,user_id,occupation_id,occupation_skill_id,skill_id,required_level,current_level,current_confidence,gap_value,priority_score,gap_status,target_batch_code,calculation_version,calculated_at,correlation_id) VALUES(#{snapshotCode},#{userId},#{occupationId},#{occupationSkillId},#{skillId},#{requiredLevel},#{currentLevel},#{currentConfidence},#{gapValue},#{priorityScore},#{gapStatus},#{targetBatchCode},#{calculationVersion},NOW(3),#{snapshotCode})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertGap(StudentOccupationSkillGap gap);

    @Select("SELECT g.*,s.name_zh AS skill_name,os.requirement_type,os.importance_score FROM student_occupation_skill_gap g JOIN skill s ON s.id=g.skill_id JOIN occupation_skill os ON os.id=g.occupation_skill_id WHERE g.snapshot_code=#{snapshotCode} ORDER BY g.priority_score DESC,g.id")
    List<StudentOccupationSkillGap> selectGapsBySnapshot(@Param("snapshotCode") String snapshotCode);

    @Select("SELECT snapshot_code FROM student_occupation_skill_gap WHERE user_id=#{userId} AND occupation_id=#{occupationId} ORDER BY calculated_at DESC,id DESC LIMIT 1")
    String selectLatestGapSnapshotCode(@Param("userId") Long userId, @Param("occupationId") Long occupationId);

    @Select({"<script>", "SELECT c.id AS course_id,c.course_code,c.course_name,sk.skill_id,sk.knowledge_point_id,ck.is_core AS course_core,sk.requirement_type AS mapping_type,ck.coverage_weight FROM course c JOIN course_knowledge ck ON ck.course_id=c.id JOIN skill_knowledge sk ON sk.knowledge_point_id=ck.knowledge_point_id WHERE c.status='active' AND c.is_deleted=0 AND sk.skill_id IN <foreach collection='skillIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>", "</script>"})
    List<CareerCourseCoverage> selectCourseCoverage(@Param("skillIds") List<Long> skillIds);

    @Insert("INSERT INTO career_course_recommendation_snapshot(snapshot_code,gap_snapshot_code,user_id,occupation_id,target_batch_code,profile_version,graph_versions_json,skill_state_version,course_catalog_hash,algorithm_version,status,request_json,result_summary_json,correlation_id,created_at) VALUES(#{snapshotCode},#{gapSnapshotCode},#{userId},#{occupationId},#{targetBatchCode},NULL,NULL,#{algorithmVersion},NULL,#{algorithmVersion},'READY',#{requestJson},#{resultSummaryJson},#{snapshotCode},NOW(3))")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertRecommendationSnapshot(CareerRecommendationSnapshot snapshot);

    @Insert("INSERT INTO career_course_recommendation_item(snapshot_id,course_id,rank_no,course_score,coverage_score,core_coverage_rate,difficulty_fit,unfinished_factor,course_quality,estimated_hours,reason_json,covered_skill_ids_json,covered_knowledge_point_ids_json,fallback_type,status) VALUES(#{snapshotId},#{courseId},#{rankNo},#{courseScore},#{coverageScore},#{coreCoverageRate},1,1,1,NULL,#{reasonJson},#{coveredSkillIdsJson},#{coveredKnowledgePointIdsJson},#{fallbackType},'RECOMMENDED')")
    int insertRecommendationItem(CareerRecommendationItem item);

    @Select("SELECT * FROM career_course_recommendation_snapshot WHERE snapshot_code=#{snapshotCode} AND user_id=#{userId}")
    CareerRecommendationSnapshot selectRecommendationSnapshot(@Param("snapshotCode") String snapshotCode, @Param("userId") Long userId);

    @Select("SELECT * FROM career_course_recommendation_snapshot WHERE user_id=#{userId} AND occupation_id=#{occupationId} ORDER BY created_at DESC,id DESC LIMIT 1")
    CareerRecommendationSnapshot selectLatestRecommendationSnapshot(@Param("userId") Long userId, @Param("occupationId") Long occupationId);

    @Select("SELECT i.*,c.course_code,c.course_name FROM career_course_recommendation_item i JOIN course c ON c.id=i.course_id WHERE i.snapshot_id=#{snapshotId} ORDER BY i.rank_no,i.id")
    List<CareerRecommendationItem> selectRecommendationItems(@Param("snapshotId") Long snapshotId);

    @Select("SELECT i.*,c.course_code,c.course_name FROM career_course_recommendation_item i JOIN career_course_recommendation_snapshot s ON s.id=i.snapshot_id JOIN course c ON c.id=i.course_id WHERE s.snapshot_code=#{snapshotCode} AND s.user_id=#{userId} AND i.course_id=#{courseId} LIMIT 1")
    CareerRecommendationItem selectRecommendationItem(@Param("snapshotCode")String snapshotCode,@Param("userId")Long userId,@Param("courseId")Long courseId);

    @Select("SELECT * FROM career_course_recommendation_acceptance WHERE snapshot_id=#{snapshotId} AND course_id=#{courseId} LIMIT 1")
    CareerRecommendationAcceptance selectAcceptance(@Param("snapshotId")Long snapshotId,@Param("courseId")Long courseId);

    @Insert("INSERT INTO career_course_recommendation_acceptance(snapshot_id,user_id,course_id,learning_path_code,status,accepted_at) VALUES(#{snapshotId},#{userId},#{courseId},#{learningPathCode},#{status},NOW(3))")
    @Options(useGeneratedKeys=true,keyProperty="id") int insertAcceptance(CareerRecommendationAcceptance acceptance);

    @Select("SELECT a.* FROM career_course_recommendation_acceptance a JOIN career_course_recommendation_snapshot s ON s.id=a.snapshot_id WHERE a.user_id=#{userId} AND s.occupation_id=#{occupationId} ORDER BY a.accepted_at DESC")
    List<CareerRecommendationAcceptance> selectAcceptances(@Param("userId") Long userId, @Param("occupationId") Long occupationId);

    @Select("SELECT a.* FROM career_course_recommendation_acceptance a JOIN career_course_recommendation_snapshot s ON s.id=a.snapshot_id WHERE a.user_id=#{userId} AND s.occupation_id=#{occupationId} ORDER BY a.accepted_at DESC LIMIT 1")
    CareerRecommendationAcceptance selectLatestAcceptance(@Param("userId") Long userId, @Param("occupationId") Long occupationId);

    @Update("UPDATE occupation_skill SET required_level=#{requiredLevel},importance_score=#{importanceScore},requirement_type=#{requirementType},required_level_source=#{source},required_level_version=#{levelVersion},published_batch_code=#{batchCode},required_level_updated_at=NOW(3) WHERE id=#{occupationSkillId}")
    int publishOccupationSkill(@Param("occupationSkillId") Long occupationSkillId, @Param("requiredLevel") java.math.BigDecimal requiredLevel, @Param("importanceScore") java.math.BigDecimal importanceScore, @Param("requirementType") String requirementType, @Param("source") String source, @Param("levelVersion") String levelVersion, @Param("batchCode") String batchCode);
}
