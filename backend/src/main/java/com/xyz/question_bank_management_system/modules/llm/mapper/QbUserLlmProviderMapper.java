package com.xyz.question_bank_management_system.modules.llm.mapper;

import com.xyz.question_bank_management_system.modules.llm.entity.QbUserLlmProvider;
import org.apache.ibatis.annotations.*;
import java.util.List;

/** Compatibility mapper; personal providers are model_config(owner_type='user'). */
@Mapper
public interface QbUserLlmProviderMapper {
    String COLUMNS = "id,owner_id AS user_id,provider_key,label,provider_type,base_url,api_key_cipher,model,temperature,1 AS supports_temperature,NULL AS description,NULL AS tags_json,enabled,is_default,NULL AS created_at,updated_at,0 AS is_deleted";
    @Select("SELECT " + COLUMNS + " FROM model_config WHERE owner_type='user' AND owner_id=#{userId} ORDER BY is_default DESC,updated_at DESC,id DESC") List<QbUserLlmProvider> selectByUserId(@Param("userId") Long userId);
    @Select("SELECT " + COLUMNS + " FROM model_config WHERE id=#{id} AND owner_type='user' AND owner_id=#{userId} LIMIT 1") QbUserLlmProvider selectOwnedById(@Param("id") Long id,@Param("userId") Long userId);
    @Select("SELECT " + COLUMNS + " FROM model_config WHERE owner_type='user' AND owner_id=#{userId} AND provider_key=#{providerKey} LIMIT 1") QbUserLlmProvider selectByUserAndKey(@Param("userId") Long userId,@Param("providerKey") String providerKey);
    @Select("SELECT " + COLUMNS + " FROM model_config WHERE owner_type='user' AND owner_id=#{userId} AND is_default=1 AND enabled=1 ORDER BY updated_at DESC LIMIT 1") QbUserLlmProvider selectDefaultByUserId(@Param("userId") Long userId);
    @Insert("INSERT INTO model_config(owner_type,owner_id,provider_key,label,provider_type,base_url,api_key_cipher,model,temperature,enabled,is_default,updated_at) VALUES('user',#{userId},#{providerKey},#{label},#{providerType},#{baseUrl},#{apiKeyCipher},#{model},#{temperature},#{enabled},#{isDefault},NOW(3))") @Options(useGeneratedKeys=true,keyProperty="id") int insert(QbUserLlmProvider row);
    @Update("UPDATE model_config SET provider_key=#{providerKey},label=#{label},provider_type=#{providerType},base_url=#{baseUrl},api_key_cipher=#{apiKeyCipher},model=#{model},temperature=#{temperature},enabled=#{enabled},is_default=#{isDefault},updated_at=NOW(3) WHERE id=#{id} AND owner_type='user' AND owner_id=#{userId}") int update(QbUserLlmProvider row);
    @Update("UPDATE model_config SET enabled=#{enabled},updated_at=NOW(3) WHERE id=#{id} AND owner_type='user' AND owner_id=#{userId}") int updateEnabled(@Param("id") Long id,@Param("userId") Long userId,@Param("enabled") Integer enabled);
    @Update("UPDATE model_config SET is_default=0,updated_at=NOW(3) WHERE owner_type='user' AND owner_id=#{userId}") int clearDefault(@Param("userId") Long userId);
    @Update("UPDATE model_config SET is_default=1,enabled=1,updated_at=NOW(3) WHERE id=#{id} AND owner_type='user' AND owner_id=#{userId}") int markDefault(@Param("id") Long id,@Param("userId") Long userId);
    @Delete("DELETE FROM model_config WHERE id=#{id} AND owner_type='user' AND owner_id=#{userId}") int softDelete(@Param("id") Long id,@Param("userId") Long userId);
}
