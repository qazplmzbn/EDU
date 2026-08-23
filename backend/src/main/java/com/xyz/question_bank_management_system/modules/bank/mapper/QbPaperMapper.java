package com.xyz.question_bank_management_system.modules.bank.mapper;

import com.xyz.question_bank_management_system.modules.bank.entity.QbPaper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface QbPaperMapper {

    @Insert("INSERT INTO qb_paper(paper_title, paper_desc, paper_type, total_score, creator_id, created_at, updated_at, is_deleted) " +
            "VALUES(#{paperTitle}, #{paperDesc}, #{paperType}, #{totalScore}, #{creatorId}, NOW(3), NOW(3), 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QbPaper paper);

    @Update("UPDATE qb_paper SET paper_title=#{paperTitle}, paper_desc=#{paperDesc}, paper_type=#{paperType}, total_score=#{totalScore}, updated_at=NOW(3) WHERE id=#{id} AND is_deleted=0")
    int update(QbPaper paper);

    @Select("SELECT * FROM qb_paper WHERE id=#{id} AND is_deleted=0")
    QbPaper selectById(@Param("id") Long id);

    @Update("UPDATE qb_paper SET is_deleted=1, updated_at=NOW(3) WHERE id=#{id}")
    int softDelete(@Param("id") Long id);

    @Select("SELECT * FROM qb_paper WHERE is_deleted=0 ORDER BY updated_at DESC, id DESC LIMIT #{offset}, #{size}")
    List<QbPaper> page(@Param("offset") long offset, @Param("size") long size);

    @Select("SELECT COUNT(1) FROM qb_paper WHERE is_deleted=0")
    long countAll();

    @Select("SELECT * FROM qb_paper WHERE is_deleted=0 AND creator_id=#{creatorId} ORDER BY updated_at DESC, id DESC LIMIT #{offset}, #{size}")
    List<QbPaper> pageByCreator(@Param("creatorId") Long creatorId, @Param("offset") long offset, @Param("size") long size);

    @Select("SELECT COUNT(1) FROM qb_paper WHERE is_deleted=0 AND creator_id=#{creatorId}")
    long countByCreator(@Param("creatorId") Long creatorId);
}
