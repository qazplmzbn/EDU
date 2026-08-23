package com.xyz.question_bank_management_system.modules.user.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SysUserRoleMapper {

    @Insert("INSERT INTO sys_user_role(user_id, role_id, created_at) VALUES(#{userId}, #{roleId}, NOW(3))")
    int insert(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Delete("DELETE FROM sys_user_role WHERE user_id=#{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    @Select("SELECT role_id FROM sys_user_role WHERE user_id=#{userId}")
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);
}
