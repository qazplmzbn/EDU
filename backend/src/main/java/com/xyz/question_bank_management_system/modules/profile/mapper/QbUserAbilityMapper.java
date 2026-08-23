package com.xyz.question_bank_management_system.modules.profile.mapper;

import com.xyz.question_bank_management_system.modules.profile.entity.QbUserAbility;
import org.apache.ibatis.annotations.*;

@Mapper
public interface QbUserAbilityMapper {

    @Insert("INSERT INTO qb_user_ability(user_id, ability_score, updated_at) VALUES(#{userId}, #{score}, NOW(3)) " +
            "ON DUPLICATE KEY UPDATE ability_score=#{score}, updated_at=NOW(3)")
    int upsert(@Param("userId") Long userId, @Param("score") int score);

    @Select("SELECT * FROM qb_user_ability WHERE user_id=#{userId}")
    QbUserAbility selectByUserId(@Param("userId") Long userId);
}
