package com.xyz.question_bank_management_system.modules.knowledge.mapper;

import com.xyz.question_bank_management_system.modules.knowledge.entity.KnowledgeGraphSyncRecord;
import org.apache.ibatis.annotations.*;

@Mapper
public interface KnowledgeGraphSyncRecordMapper {
    @Insert("INSERT INTO knowledge_graph_sync_record(graph_version_id,sync_code,status,node_count,edge_count,content_hash,error_message,correlation_id,started_at,finished_at,created_at) VALUES(#{graphVersionId},#{syncCode},#{status},#{nodeCount},#{edgeCount},#{contentHash},#{errorMessage},#{correlationId},#{startedAt},#{finishedAt},NOW(3))")
    @Options(useGeneratedKeys=true,keyProperty="id") int insert(KnowledgeGraphSyncRecord value);
    @Select("SELECT * FROM knowledge_graph_sync_record WHERE graph_version_id=#{graphVersionId} AND status='SUCCESS' ORDER BY id DESC LIMIT 1") KnowledgeGraphSyncRecord selectLatestSuccess(Long graphVersionId);
}
