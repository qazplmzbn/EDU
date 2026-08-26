package com.xyz.question_bank_management_system.modules.llm.mapper;

import com.xyz.question_bank_management_system.modules.llm.entity.QbLlmProvider;
import org.apache.ibatis.annotations.*;
import java.util.List;

/** Compatibility mapper; Stage 06 reads the authoritative model_config table only. */
@Mapper
public interface QbLlmProviderMapper {
    String COLUMNS = "id,provider_key,label,provider_type,base_url,api_key_cipher,model,temperature,1 AS supports_temperature,NULL AS description,NULL AS tags_json,enabled,is_default,NULL AS created_by,NULL AS created_at,updated_at,0 AS is_deleted";
    @Select("SELECT " + COLUMNS + " FROM model_config WHERE owner_type='system' ORDER BY is_default DESC,updated_at DESC,id DESC") List<QbLlmProvider> selectAll();
    @Select("SELECT " + COLUMNS + " FROM model_config WHERE id=#{id} AND owner_type='system' LIMIT 1") QbLlmProvider selectById(@Param("id") Long id);
    @Select("SELECT " + COLUMNS + " FROM model_config WHERE owner_type='system' AND provider_key=#{providerKey} LIMIT 1") QbLlmProvider selectByKey(@Param("providerKey") String providerKey);
    @Select("SELECT " + COLUMNS + " FROM model_config WHERE owner_type='system' AND is_default=1 AND enabled=1 ORDER BY updated_at DESC LIMIT 1") QbLlmProvider selectDefault();
    @Insert("INSERT INTO model_config(owner_type,owner_id,provider_key,label,provider_type,base_url,api_key_cipher,model,temperature,enabled,is_default,updated_at) VALUES('system',0,#{providerKey},#{label},#{providerType},#{baseUrl},#{apiKeyCipher},#{model},#{temperature},#{enabled},#{isDefault},NOW(3))") @Options(useGeneratedKeys=true,keyProperty="id") int insert(QbLlmProvider row);
    @Update("UPDATE model_config SET provider_key=#{providerKey},label=#{label},provider_type=#{providerType},base_url=#{baseUrl},api_key_cipher=#{apiKeyCipher},model=#{model},temperature=#{temperature},enabled=#{enabled},is_default=#{isDefault},updated_at=NOW(3) WHERE id=#{id} AND owner_type='system'") int update(QbLlmProvider row);
    @Update("UPDATE model_config SET enabled=#{enabled},updated_at=NOW(3) WHERE id=#{id} AND owner_type='system'") int updateEnabled(@Param("id") Long id,@Param("enabled") Integer enabled);
    @Update("UPDATE model_config SET is_default=0,updated_at=NOW(3) WHERE owner_type='system'") int clearDefault();
    @Update("UPDATE model_config SET is_default=1,enabled=1,updated_at=NOW(3) WHERE id=#{id} AND owner_type='system'") int markDefault(@Param("id") Long id);
    @Delete("DELETE FROM model_config WHERE id=#{id} AND owner_type='system'") int softDelete(@Param("id") Long id);
}
