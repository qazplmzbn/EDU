package com.xyz.question_bank_management_system.modules.competency.mapper;

import com.xyz.question_bank_management_system.modules.competency.entity.DataSyncRecord;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface DataSyncRecordMapper {
    @Insert("INSERT INTO data_sync_record(sync_type,source_name,trigger_type,trigger_by,sync_version,status,fetched_count,inserted_count,updated_count,failed_count,error_message,started_at,created_at) VALUES(#{syncType},#{sourceName},#{triggerType},#{triggerBy},#{syncVersion},#{status},#{fetchedCount},#{insertedCount},#{updatedCount},#{failedCount},#{errorMessage},NOW(3),NOW(3))") @Options(useGeneratedKeys=true,keyProperty="id") int insert(DataSyncRecord entity);
    @Update("UPDATE data_sync_record SET status=#{status},fetched_count=#{fetchedCount},inserted_count=#{insertedCount},updated_count=#{updatedCount},failed_count=#{failedCount},error_message=#{errorMessage},finished_at=NOW(3) WHERE id=#{id}") int finish(DataSyncRecord entity);
    @Select("SELECT * FROM data_sync_record WHERE id=#{id}") DataSyncRecord selectById(Long id);
    @Select("SELECT COUNT(*) FROM data_sync_record") long countPage();
    @Select("SELECT * FROM data_sync_record ORDER BY started_at DESC,id DESC LIMIT #{limit} OFFSET #{offset}") List<DataSyncRecord> selectPage(@Param("offset") long offset,@Param("limit") int limit);
}
