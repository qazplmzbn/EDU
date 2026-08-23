package com.xyz.question_bank_management_system.modules.bank.mapper;

import com.xyz.question_bank_management_system.modules.bank.entity.QbTag;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface QbTagMapper {

    @Select("SELECT * FROM qb_tag WHERE id=#{id} AND is_deleted=0")
    QbTag selectById(@Param("id") Long id);

    @Select("SELECT * FROM qb_tag WHERE is_deleted=0 ORDER BY sort_order ASC, id ASC")
    List<QbTag> selectAll();

    @Select("SELECT * FROM qb_tag WHERE is_deleted=0 AND tag_name LIKE CONCAT('%', #{keyword}, '%') ORDER BY sort_order ASC, id ASC")
    List<QbTag> selectByKeyword(@Param("keyword") String keyword);

    @Select("SELECT COUNT(1) FROM qb_tag WHERE is_deleted=0 AND tag_name=#{tagName}")
    long countByName(@Param("tagName") String tagName);

    @Insert("INSERT INTO qb_tag(tag_name, tag_code, parent_id, tag_level, tag_type, sort_order, created_at, updated_at, is_deleted) " +
            "VALUES(#{tagName}, #{tagCode}, #{parentId}, #{tagLevel}, #{tagType}, #{sortOrder}, NOW(3), NOW(3), 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QbTag tag);

    @Update("UPDATE qb_tag SET tag_name=#{tagName}, tag_code=#{tagCode}, parent_id=#{parentId}, tag_level=#{tagLevel}, tag_type=#{tagType}, sort_order=#{sortOrder}, updated_at=NOW(3) " +
            "WHERE id=#{id} AND is_deleted=0")
    int update(QbTag tag);

    @Update("UPDATE qb_tag SET is_deleted=1, updated_at=NOW(3) WHERE id=#{id}")
    int softDelete(@Param("id") Long id);
}
