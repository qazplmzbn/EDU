package com.xyz.question_bank_management_system.modules.competency.mapper;

import com.xyz.question_bank_management_system.modules.competency.entity.QbCompetencyJobSyncRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface QbCompetencyJobSyncRecordMapper {

    @Insert("INSERT INTO qb_competency_job_sync_record(trigger_type, trigger_by, platform, status, keyword_count, city_count, fetched_candidate_count, success_count, failure_count, offline_count, error_message, started_at, finished_at, created_at, updated_at, is_deleted) " +
            "VALUES(#{triggerType}, #{triggerBy}, #{platform}, #{status}, #{keywordCount}, #{cityCount}, #{fetchedCandidateCount}, #{successCount}, #{failureCount}, #{offlineCount}, #{errorMessage}, #{startedAt}, #{finishedAt}, NOW(3), NOW(3), 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QbCompetencyJobSyncRecord record);

    @Update("UPDATE qb_competency_job_sync_record SET status=#{status}, fetched_candidate_count=#{fetchedCandidateCount}, success_count=#{successCount}, failure_count=#{failureCount}, offline_count=#{offlineCount}, error_message=#{errorMessage}, finished_at=#{finishedAt}, updated_at=NOW(3) WHERE id=#{id}")
    int updateCompletion(QbCompetencyJobSyncRecord record);

    @Select("SELECT * FROM qb_competency_job_sync_record WHERE is_deleted=0 ORDER BY started_at DESC, id DESC LIMIT #{limit}")
    List<QbCompetencyJobSyncRecord> selectRecent(@Param("limit") int limit);

    @Select("SELECT * FROM qb_competency_job_sync_record WHERE is_deleted=0 AND status='success' ORDER BY started_at DESC, id DESC LIMIT 1")
    QbCompetencyJobSyncRecord selectLatestSuccess();

    @Select("SELECT * FROM qb_competency_job_sync_record WHERE is_deleted=0 ORDER BY started_at DESC, id DESC LIMIT 1")
    QbCompetencyJobSyncRecord selectLatest();
}
