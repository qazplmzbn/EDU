package com.xyz.question_bank_management_system.modules.llm.mapper;

import com.xyz.question_bank_management_system.modules.llm.entity.QbLlmProvider;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface QbLlmProviderMapper {

    @Select("SELECT * FROM qb_llm_provider WHERE is_deleted=0 ORDER BY is_default DESC, updated_at DESC, id DESC")
    List<QbLlmProvider> selectAll();

    @Select("SELECT * FROM qb_llm_provider WHERE id=#{id} AND is_deleted=0")
    QbLlmProvider selectById(@Param("id") Long id);

    @Select("SELECT * FROM qb_llm_provider WHERE provider_key=#{providerKey} AND is_deleted=0 LIMIT 1")
    QbLlmProvider selectByKey(@Param("providerKey") String providerKey);

    @Select("SELECT * FROM qb_llm_provider WHERE is_default=1 AND enabled=1 AND is_deleted=0 ORDER BY updated_at DESC LIMIT 1")
    QbLlmProvider selectDefault();

    @Insert("INSERT INTO qb_llm_provider(provider_key, label, provider_type, base_url, api_key_cipher, model, temperature, supports_temperature, description, tags_json, enabled, is_default, created_by, created_at, updated_at, is_deleted) " +
            "VALUES(#{providerKey}, #{label}, #{providerType}, #{baseUrl}, #{apiKeyCipher}, #{model}, #{temperature}, #{supportsTemperature}, #{description}, #{tagsJson}, #{enabled}, #{isDefault}, #{createdBy}, NOW(3), NOW(3), 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QbLlmProvider provider);

    @Update("UPDATE qb_llm_provider SET provider_key=#{providerKey}, label=#{label}, provider_type=#{providerType}, base_url=#{baseUrl}, api_key_cipher=#{apiKeyCipher}, model=#{model}, temperature=#{temperature}, supports_temperature=#{supportsTemperature}, description=#{description}, tags_json=#{tagsJson}, updated_at=NOW(3) WHERE id=#{id} AND is_deleted=0")
    int update(QbLlmProvider provider);

    @Update("UPDATE qb_llm_provider SET enabled=#{enabled}, updated_at=NOW(3) WHERE id=#{id} AND is_deleted=0")
    int updateEnabled(@Param("id") Long id, @Param("enabled") Integer enabled);

    @Update("UPDATE qb_llm_provider SET is_default=0, updated_at=NOW(3) WHERE is_deleted=0")
    int clearDefault();

    @Update("UPDATE qb_llm_provider SET is_default=1, enabled=1, updated_at=NOW(3) WHERE id=#{id} AND is_deleted=0")
    int markDefault(@Param("id") Long id);

    @Update("UPDATE qb_llm_provider SET is_deleted=1, is_default=0, updated_at=NOW(3) WHERE id=#{id} AND is_deleted=0")
    int softDelete(@Param("id") Long id);
}
