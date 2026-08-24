package com.xyz.question_bank_management_system.modules.learning.mapper;

import com.xyz.question_bank_management_system.modules.learning.entity.ResourceKnowledge;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ResourceKnowledgeMapper {
    @Select("SELECT * FROM resource_knowledge WHERE resource_id=#{resourceId} ORDER BY is_primary DESC,id") List<ResourceKnowledge> selectByResourceId(Long resourceId);
    @Delete("DELETE FROM resource_knowledge WHERE resource_id=#{resourceId}") int deleteByResourceId(Long resourceId);
    @Insert({"<script>","INSERT INTO resource_knowledge(resource_id,knowledge_point_id,relation_type,coverage_weight,is_primary,created_at) VALUES","<foreach collection='relations' item='r' separator=','>","(#{resourceId},#{r.knowledgePointId},#{r.relationType},#{r.coverageWeight},#{r.isPrimary},NOW(3))","</foreach>","</script>"}) int batchInsert(@Param("resourceId") Long resourceId,@Param("relations") List<ResourceKnowledge> relations);
}
