package com.xyz.question_bank_management_system.modules.bank.mapper;

import com.xyz.question_bank_management_system.modules.bank.entity.QbPaperQuestion;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface QbPaperQuestionMapper {

    @Select("SELECT * FROM qb_paper_question WHERE paper_id=#{paperId} ORDER BY order_no ASC")
    List<QbPaperQuestion> selectByPaperId(@Param("paperId") Long paperId);

    @Select("SELECT * FROM qb_paper_question WHERE id=#{id}")
    QbPaperQuestion selectById(@Param("id") Long id);

    @Select("SELECT COALESCE(SUM(score),0) FROM qb_paper_question WHERE paper_id=#{paperId}")
    int sumScoreByPaperId(@Param("paperId") Long paperId);

    @Insert("INSERT INTO qb_paper_question(paper_id, question_id, order_no, score, snapshot_json, snapshot_hash) " +
            "VALUES(#{paperId}, #{questionId}, #{orderNo}, #{score}, #{snapshotJson}, #{snapshotHash})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QbPaperQuestion pq);

    @Update("UPDATE qb_paper_question SET order_no=#{orderNo}, score=#{score}, snapshot_json=#{snapshotJson}, snapshot_hash=#{snapshotHash} WHERE id=#{id}")
    int update(QbPaperQuestion pq);

    @Delete("DELETE FROM qb_paper_question WHERE id=#{id}")
    int deleteById(@Param("id") Long id);

    @Delete("DELETE FROM qb_paper_question WHERE paper_id=#{paperId}")
    int deleteByPaperId(@Param("paperId") Long paperId);
}
