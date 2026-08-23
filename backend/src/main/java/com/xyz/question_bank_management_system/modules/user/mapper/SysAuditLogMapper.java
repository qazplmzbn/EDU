package com.xyz.question_bank_management_system.modules.user.mapper;

import com.xyz.question_bank_management_system.modules.user.entity.SysAuditLog;
import com.xyz.question_bank_management_system.modules.user.vo.AdminAuditLogItemVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SysAuditLogMapper {

    @Insert("INSERT INTO sys_audit_log(user_id, action, entity_type, entity_id, before_json, after_json, ip_addr) " +
            "VALUES(#{userId}, #{action}, #{entityType}, #{entityId}, #{beforeJson}, #{afterJson}, #{ipAddr})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysAuditLog log);

    @Select({
            "<script>",
            "SELECT COUNT(1)",
            "FROM sys_audit_log l",
            "LEFT JOIN sys_user u ON u.id = l.user_id",
            "WHERE 1 = 1",
            "<if test='username != null and username != \"\"'>",
            "  AND u.username LIKE CONCAT('%', #{username}, '%')",
            "</if>",
            "<if test='action != null and action != \"\"'>",
            "  AND l.action = #{action}",
            "</if>",
            "<if test='entityType != null and entityType != \"\"'>",
            "  AND l.entity_type = #{entityType}",
            "</if>",
            "<if test='startTime != null'>",
            "  AND l.created_at &gt;= #{startTime}",
            "</if>",
            "<if test='endTime != null'>",
            "  AND l.created_at &lt;= #{endTime}",
            "</if>",
            "</script>"
    })
    long countByFilter(@Param("username") String username,
                       @Param("action") String action,
                       @Param("entityType") String entityType,
                       @Param("startTime") LocalDateTime startTime,
                       @Param("endTime") LocalDateTime endTime);

    @Select({
            "<script>",
            "SELECT l.id AS log_id,",
            "       u.username,",
            "       l.action,",
            "       l.entity_type,",
            "       l.entity_id,",
            "       CAST(l.before_json AS CHAR) AS before_json,",
            "       CAST(l.after_json AS CHAR) AS after_json,",
            "       l.ip_addr,",
            "       l.created_at",
            "FROM sys_audit_log l",
            "LEFT JOIN sys_user u ON u.id = l.user_id",
            "WHERE 1 = 1",
            "<if test='username != null and username != \"\"'>",
            "  AND u.username LIKE CONCAT('%', #{username}, '%')",
            "</if>",
            "<if test='action != null and action != \"\"'>",
            "  AND l.action = #{action}",
            "</if>",
            "<if test='entityType != null and entityType != \"\"'>",
            "  AND l.entity_type = #{entityType}",
            "</if>",
            "<if test='startTime != null'>",
            "  AND l.created_at &gt;= #{startTime}",
            "</if>",
            "<if test='endTime != null'>",
            "  AND l.created_at &lt;= #{endTime}",
            "</if>",
            "ORDER BY l.created_at DESC, l.id DESC",
            "LIMIT #{offset}, #{size}",
            "</script>"
    })
    List<AdminAuditLogItemVO> pageByFilter(@Param("username") String username,
                                           @Param("action") String action,
                                           @Param("entityType") String entityType,
                                           @Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime,
                                           @Param("offset") long offset,
                                           @Param("size") long size);
}
