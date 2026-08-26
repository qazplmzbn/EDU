package com.xyz.question_bank_management_system.modules.llm.mapper;

import com.xyz.question_bank_management_system.modules.llm.entity.ModelConfig;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ModelConfigMapper {
    @Select("SELECT * FROM model_config WHERE owner_type=#{ownerType} AND COALESCE(owner_id,0)=COALESCE(#{ownerId},0) ORDER BY is_default DESC,updated_at DESC,id DESC")
    List<ModelConfig> selectByOwner(@Param("ownerType") String ownerType, @Param("ownerId") Long ownerId);

    @Select("SELECT * FROM model_config WHERE provider_key=#{providerKey} AND owner_type=#{ownerType} AND COALESCE(owner_id,0)=COALESCE(#{ownerId},0) LIMIT 1")
    ModelConfig selectByOwnerAndKey(@Param("ownerType") String ownerType, @Param("ownerId") Long ownerId, @Param("providerKey") String providerKey);

    @Select("SELECT * FROM model_config WHERE id=#{id} LIMIT 1")
    ModelConfig selectById(@Param("id") Long id);

    @Select("SELECT * FROM model_config WHERE owner_type='system' AND enabled=1 AND is_default=1 ORDER BY updated_at DESC,id DESC LIMIT 1")
    ModelConfig selectSystemDefault();

    @Select("SELECT * FROM model_config WHERE owner_type='user' AND owner_id=#{userId} AND enabled=1 AND is_default=1 ORDER BY updated_at DESC,id DESC LIMIT 1")
    ModelConfig selectUserDefault(@Param("userId") Long userId);

    @Insert("INSERT INTO model_config(owner_type,owner_id,provider_key,label,provider_type,base_url,api_key_cipher,model,temperature,enabled,is_default,updated_at) VALUES(#{ownerType},#{ownerId},#{providerKey},#{label},#{providerType},#{baseUrl},#{apiKeyCipher},#{model},#{temperature},#{enabled},#{isDefault},NOW(3))")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ModelConfig row);

    @Update("UPDATE model_config SET provider_key=#{providerKey},label=#{label},provider_type=#{providerType},base_url=#{baseUrl},api_key_cipher=#{apiKeyCipher},model=#{model},temperature=#{temperature},enabled=#{enabled},is_default=#{isDefault},updated_at=NOW(3) WHERE id=#{id}")
    int update(ModelConfig row);

    @Update("UPDATE model_config SET is_default=0,updated_at=NOW(3) WHERE owner_type=#{ownerType} AND COALESCE(owner_id,0)=COALESCE(#{ownerId},0)")
    int clearDefault(@Param("ownerType") String ownerType, @Param("ownerId") Long ownerId);

    @Delete("DELETE FROM model_config WHERE id=#{id}")
    int delete(@Param("id") Long id);

    @Select("SELECT * FROM model_config WHERE api_key_cipher LIKE 'v1:%'")
    List<ModelConfig> selectLegacyEncryptedKeys();

    @Update("UPDATE model_config SET api_key_cipher=#{cipher},updated_at=NOW(3) WHERE id=#{id}")
    int updateCipher(@Param("id") Long id, @Param("cipher") String cipher);
}
