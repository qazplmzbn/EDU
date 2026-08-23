package com.xyz.question_bank_management_system.modules.competency.mapper;

import com.xyz.question_bank_management_system.modules.competency.entity.QbCompetencyJobSnapshot;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface QbCompetencyJobSnapshotMapper {

    @Select("SELECT * FROM qb_competency_job_snapshot WHERE is_deleted=0 AND availability_status <> 'offline' ORDER BY updated_at DESC, id DESC LIMIT #{limit}")
    List<QbCompetencyJobSnapshot> selectActiveSnapshots(@Param("limit") int limit);

    @Select("SELECT COUNT(1) FROM qb_competency_job_snapshot WHERE is_deleted=0")
    long countAll();

    @Select("SELECT COUNT(1) FROM qb_competency_job_snapshot WHERE is_deleted=0 AND availability_status <> 'offline'")
    long countVisible();

    @Select("SELECT * FROM qb_competency_job_snapshot WHERE source_url=#{sourceUrl} AND is_deleted=0 LIMIT 1")
    QbCompetencyJobSnapshot selectBySourceUrl(@Param("sourceUrl") String sourceUrl);

    @Insert("INSERT INTO qb_competency_job_snapshot(source_platform, source_url, source_job_key, title, dimension, skill, location, salary, experience, education, company, description, tags_json, source_updated_at, last_seen_at, availability_status, sync_version, created_at, updated_at, is_deleted) " +
            "VALUES(#{sourcePlatform}, #{sourceUrl}, #{sourceJobKey}, #{title}, #{dimension}, #{skill}, #{location}, #{salary}, #{experience}, #{education}, #{company}, #{description}, #{tagsJson}, #{sourceUpdatedAt}, #{lastSeenAt}, #{availabilityStatus}, #{syncVersion}, NOW(3), NOW(3), 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QbCompetencyJobSnapshot snapshot);

    @Update("UPDATE qb_competency_job_snapshot SET source_platform=#{sourcePlatform}, source_job_key=#{sourceJobKey}, title=#{title}, dimension=#{dimension}, skill=#{skill}, location=#{location}, salary=#{salary}, experience=#{experience}, education=#{education}, company=#{company}, description=#{description}, tags_json=#{tagsJson}, source_updated_at=#{sourceUpdatedAt}, last_seen_at=#{lastSeenAt}, availability_status=#{availabilityStatus}, sync_version=#{syncVersion}, updated_at=NOW(3) WHERE id=#{id}")
    int update(QbCompetencyJobSnapshot snapshot);

    @Update("UPDATE qb_competency_job_snapshot SET availability_status=#{status}, updated_at=NOW(3) WHERE source_url=#{sourceUrl} AND is_deleted=0")
    int updateAvailability(@Param("sourceUrl") String sourceUrl, @Param("status") String status);

    @Update("UPDATE qb_competency_job_snapshot SET availability_status='offline', updated_at=NOW(3) WHERE is_deleted=0 AND availability_status <> 'offline' AND (last_seen_at IS NULL OR last_seen_at < #{threshold})")
    int markMissingAsOffline(@Param("threshold") LocalDateTime threshold);
}
