package com.xyz.question_bank_management_system.modules.knowledge.mapper;

import com.xyz.question_bank_management_system.modules.knowledge.entity.CourseGraphImport;
import com.xyz.question_bank_management_system.modules.knowledge.entity.CourseGraphImportIssue;
import com.xyz.question_bank_management_system.modules.knowledge.entity.KnowledgePointLegacyMapping;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CourseGraphImportMapper {
    @Insert("INSERT INTO course_graph_import(import_code,idempotency_key,course_code,course_name,schema_version,mode,source_file_name,source_file_hash,normalized_hash,validation_hash,status,course_id,graph_version_id,node_count,module_count,category_count,knowledge_point_count,contains_count,prerequisite_count,similar_count,error_count,warning_count,created_by,correlation_id,created_at,updated_at) VALUES(#{importCode},#{idempotencyKey},#{courseCode},#{courseName},#{schemaVersion},#{mode},#{sourceFileName},#{sourceFileHash},#{normalizedHash},#{validationHash},#{status},#{courseId},#{graphVersionId},#{nodeCount},#{moduleCount},#{categoryCount},#{knowledgePointCount},#{containsCount},#{prerequisiteCount},#{similarCount},#{errorCount},#{warningCount},#{createdBy},#{correlationId},NOW(3),NOW(3))")
    @Options(useGeneratedKeys=true,keyProperty="id") int insert(CourseGraphImport value);

    @Select("SELECT * FROM course_graph_import WHERE import_code=#{code} LIMIT 1") CourseGraphImport selectByCode(String code);
    @Select("SELECT * FROM course_graph_import WHERE id=#{id} LIMIT 1") CourseGraphImport selectById(Long id);
    @Select("SELECT * FROM course_graph_import WHERE import_code=#{code} LIMIT 1 FOR UPDATE") CourseGraphImport selectByCodeForUpdate(String code);
    @Select("SELECT * FROM course_graph_import WHERE created_by=#{userId} AND idempotency_key=#{key} LIMIT 1") CourseGraphImport selectByIdempotency(@Param("userId")Long userId,@Param("key")String key);
    @Select("SELECT * FROM course_graph_import WHERE course_code=#{courseCode} AND source_file_hash=#{fileHash} AND schema_version=#{schemaVersion} LIMIT 1") CourseGraphImport selectBySource(@Param("courseCode")String courseCode,@Param("fileHash")String fileHash,@Param("schemaVersion")String schemaVersion);

    @Insert({"<script>","INSERT INTO course_graph_import_issue(import_id,severity,issue_code,location_type,location_code,message,resolved,created_at) VALUES","<foreach collection='rows' item='r' separator=','>","(#{r.importId},#{r.severity},#{r.issueCode},#{r.locationType},#{r.locationCode},#{r.message},0,NOW(3))","</foreach>","</script>"})
    int batchInsertIssues(@Param("rows")List<CourseGraphImportIssue> rows);
    @Select("SELECT * FROM course_graph_import_issue WHERE import_id=#{importId} ORDER BY severity,issue_code,id") List<CourseGraphImportIssue> selectIssues(Long importId);
    @Select("SELECT COUNT(*) FROM course_graph_import_issue WHERE import_id=#{importId} AND severity='ERROR' AND resolved=0") int countUnresolvedErrors(Long importId);
    @Select("SELECT COUNT(*) FROM course_graph_import_issue WHERE import_id=#{importId} AND severity=#{severity}") int countIssues(@Param("importId")Long importId,@Param("severity")String severity);

    @Update("UPDATE course_graph_import SET status=#{status},course_id=#{courseId},graph_version_id=#{graphVersionId},updated_at=NOW(3) WHERE id=#{id}") int updateImported(CourseGraphImport value);
    @Update("UPDATE course_graph_import SET status='FAILED',updated_at=NOW(3) WHERE id=#{id}") int markFailed(Long id);
    @Update("UPDATE course_graph_import SET status='APPROVED',reviewed_by=#{reviewerId},reviewed_at=NOW(3),updated_at=NOW(3) WHERE id=#{id} AND status='IMPORTED'") int approve(@Param("id")Long id,@Param("reviewerId")Long reviewerId);

    @Insert("INSERT INTO knowledge_point_legacy_mapping(import_id,legacy_knowledge_point_id,target_type,target_external_code,mapping_type,confidence,review_status,notes,created_at) VALUES(#{importId},#{legacyKnowledgePointId},#{targetType},#{targetExternalCode},#{mappingType},#{confidence},#{reviewStatus},#{notes},NOW(3))")
    @Options(useGeneratedKeys=true,keyProperty="id") int insertLegacyMapping(KnowledgePointLegacyMapping value);
    @Select("SELECT * FROM knowledge_point_legacy_mapping WHERE import_id=#{importId} ORDER BY legacy_knowledge_point_id,id") List<KnowledgePointLegacyMapping> selectLegacyMappings(Long importId);
    @Update("UPDATE knowledge_point_legacy_mapping SET review_status='APPROVED' WHERE import_id=#{importId}") int approveLegacyMappings(Long importId);

    @Select("SELECT COUNT(*) FROM question_knowledge WHERE knowledge_point_id IN(1,2,3,4,5,6,7)") int countLegacyQuestionReferences();
    @Select("SELECT COUNT(*) FROM resource_knowledge WHERE knowledge_point_id IN(1,2,3,4,5,6,7)") int countLegacyResourceReferences();
    @Select("SELECT COUNT(*) FROM student_knowledge_state WHERE knowledge_point_id IN(1,2,3,4,5,6,7)") int countLegacyStateReferences();
    @Update("UPDATE knowledge_point SET status='DISABLED',updated_at=NOW(3) WHERE id IN(1,2,3,4,5,6,7) AND is_deleted=0") int disableLegacyPoints();
    @Delete("DELETE ck FROM course_knowledge ck JOIN course c ON c.id=ck.course_id WHERE c.course_code='C' AND ck.knowledge_point_id IN(1,2,3,4,5,6,7)") int deleteLegacyBridge();
    @Select("SELECT COUNT(*) FROM course_chapter WHERE course_id=#{courseId} AND status='ACTIVE'") int countActiveChapters(Long courseId);
    @Select("SELECT COUNT(*) FROM knowledge_point WHERE course_id=#{courseId} AND status='ACTIVE' AND is_deleted=0") int countActivePoints(Long courseId);
    @Select("SELECT COUNT(*) FROM knowledge_relation WHERE graph_version_id=#{graphVersionId} AND is_deleted=0") int countGraphRelations(Long graphVersionId);
    @Select("SELECT COUNT(*) FROM knowledge_relation_source s JOIN knowledge_relation r ON r.id=s.relation_id WHERE r.graph_version_id=#{graphVersionId} AND r.is_deleted=0") int countGraphEvidence(Long graphVersionId);
}
