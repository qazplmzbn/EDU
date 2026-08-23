package com.xyz.question_bank_management_system.modules.user.mapper;

import com.xyz.question_bank_management_system.modules.user.entity.SysLoginLog;
import com.xyz.question_bank_management_system.modules.user.vo.AdminLoginLogItemVO;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SysLoginLogMapper {

    @Insert("INSERT INTO sys_login_log(user_id, username, success_flag, fail_reason, ip_addr, user_agent, login_at) " +
            "VALUES(#{userId}, #{username}, #{successFlag}, #{failReason}, #{ipAddr}, #{userAgent}, NOW(3))")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysLoginLog log);

    @Select({
            "<script>",
            "SELECT COUNT(1)",
            "FROM sys_login_log",
            "WHERE 1=1",
            "<if test='username != null and username != \"\"'>",
            "  AND username = #{username}",
            "</if>",
            "<if test='successFlag != null'>",
            "  AND success_flag = #{successFlag}",
            "</if>",
            "<if test='startTime != null'>",
            "  AND login_at &gt;= #{startTime}",
            "</if>",
            "<if test='endTime != null'>",
            "  AND login_at &lt;= #{endTime}",
            "</if>",
            "</script>"
    })
    long countByFilter(@Param("username") String username,
                       @Param("successFlag") Integer successFlag,
                       @Param("startTime") LocalDateTime startTime,
                       @Param("endTime") LocalDateTime endTime);

    @Select({
            "<script>",
            "SELECT id AS log_id, username, success_flag, ip_addr, login_at",
            "FROM sys_login_log",
            "WHERE 1=1",
            "<if test='username != null and username != \"\"'>",
            "  AND username = #{username}",
            "</if>",
            "<if test='successFlag != null'>",
            "  AND success_flag = #{successFlag}",
            "</if>",
            "<if test='startTime != null'>",
            "  AND login_at &gt;= #{startTime}",
            "</if>",
            "<if test='endTime != null'>",
            "  AND login_at &lt;= #{endTime}",
            "</if>",
            "ORDER BY login_at DESC, id DESC",
            "LIMIT #{offset}, #{size}",
            "</script>"
    })
    List<AdminLoginLogItemVO> pageByFilter(@Param("username") String username,
                                           @Param("successFlag") Integer successFlag,
                                           @Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime,
                                           @Param("offset") long offset,
                                           @Param("size") long size);
}
