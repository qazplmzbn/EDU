package com.xyz.question_bank_management_system.modules.bank.mapper;

import com.xyz.question_bank_management_system.modules.bank.entity.QuestionKnowledge;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface QuestionKnowledgeMapper {
    @Select("SELECT * FROM question_knowledge WHERE question_id=#{questionId} ORDER BY is_primary DESC,id") List<QuestionKnowledge> selectByQuestionId(Long questionId);
    @Select("SELECT knowledge_point_id FROM question_knowledge WHERE question_id=#{questionId} ORDER BY is_primary DESC,id") List<Long> selectKnowledgePointIdsByQuestionId(Long questionId);
    @Select({"<script>","SELECT question_id,knowledge_point_id FROM question_knowledge WHERE question_id IN","<foreach collection='questionIds' item='id' open='(' close=')' separator=','>","#{id}","</foreach>","</script>"}) List<QuestionKnowledge> selectByQuestionIds(@Param("questionIds") List<Long> questionIds);
    @Delete("DELETE FROM question_knowledge WHERE question_id=#{questionId}") int deleteByQuestionId(Long questionId);
    @Insert({"<script>","INSERT INTO question_knowledge(question_id,knowledge_point_id,weight,relation_type,is_primary,confidence,source_type,created_at) VALUES","<foreach collection='relations' item='r' separator=','>","(#{questionId},#{r.knowledgePointId},#{r.weight},#{r.relationType},#{r.isPrimary},#{r.confidence},#{r.sourceType},NOW(3))","</foreach>","</script>"}) int batchInsert(@Param("questionId") Long questionId,@Param("relations") List<QuestionKnowledge> relations);
}
