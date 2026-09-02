package com.xyz.question_bank_management_system.modules.competency.mapper;

import com.xyz.question_bank_management_system.modules.competency.entity.OccupationSkillLevelAnalysis;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface OccupationSkillLevelAnalysisMapper {
    @Insert("INSERT INTO occupation_skill_level_analysis(batch_code,occupation_id,round_no,provider_key,model_name,input_json,output_json,status,error_message,created_by,created_at) VALUES(#{batchCode},#{occupationId},#{roundNo},#{providerKey},#{modelName},#{inputJson},#{outputJson},#{status},#{errorMessage},#{createdBy},NOW(3))")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(OccupationSkillLevelAnalysis row);

    @Select("SELECT * FROM occupation_skill_level_analysis WHERE occupation_id=#{occupationId} ORDER BY created_at DESC,id DESC LIMIT #{limit}")
    List<OccupationSkillLevelAnalysis> list(@Param("occupationId") Long occupationId, @Param("limit") int limit);

    @Select("SELECT * FROM occupation_skill_level_analysis WHERE occupation_id=#{occupationId} AND batch_code=#{batchCode} ORDER BY round_no,id FOR UPDATE")
    List<OccupationSkillLevelAnalysis> selectBatchForUpdate(@Param("occupationId") Long occupationId, @Param("batchCode") String batchCode);

    @Update("UPDATE occupation_skill_level_analysis SET status=#{status},error_message=#{errorMessage} WHERE batch_code=#{batchCode} AND occupation_id=#{occupationId}")
    int updateBatchStatus(@Param("occupationId") Long occupationId, @Param("batchCode") String batchCode,
                          @Param("status") String status, @Param("errorMessage") String errorMessage);
}
