package com.xyz.question_bank_management_system.modules.user.service.impl;

import com.xyz.question_bank_management_system.common.PageResponse;
import com.xyz.question_bank_management_system.common.enums.RoleCode;
import com.xyz.question_bank_management_system.modules.user.dto.AdminCreateUserRequest;
import com.xyz.question_bank_management_system.modules.user.dto.AdminUpdateUserRequest;
import com.xyz.question_bank_management_system.modules.user.dto.AdminUpdateUserRoleRequest;
import com.xyz.question_bank_management_system.modules.user.dto.LoginRequest;
import com.xyz.question_bank_management_system.modules.user.vo.LoginResponse;
import com.xyz.question_bank_management_system.modules.user.dto.RegisterRequest;
import com.xyz.question_bank_management_system.modules.user.entity.SysLoginLog;
import com.xyz.question_bank_management_system.modules.user.entity.SysRole;
import com.xyz.question_bank_management_system.modules.user.entity.SysUser;
import com.xyz.question_bank_management_system.exception.BizException;
import com.xyz.question_bank_management_system.exception.ErrorCode;
import com.xyz.question_bank_management_system.modules.user.mapper.SysLoginLogMapper;
import com.xyz.question_bank_management_system.modules.user.mapper.SysRoleMapper;
import com.xyz.question_bank_management_system.modules.user.mapper.SysUserMapper;
import com.xyz.question_bank_management_system.modules.user.mapper.SysUserRoleMapper;
import com.xyz.question_bank_management_system.security.JwtUtil;
import com.xyz.question_bank_management_system.modules.user.service.AuditLogService;
import com.xyz.question_bank_management_system.modules.user.service.AuthService;
import com.xyz.question_bank_management_system.util.PageParamUtil;
import com.xyz.question_bank_management_system.util.SecurityContextUtil;
import com.xyz.question_bank_management_system.modules.user.vo.UserListItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String MSG_USERNAME_EXISTS = "\u7528\u6237\u540d\u5df2\u5b58\u5728";
    private static final String MSG_SUCCESS = "\u6210\u529f";
    private static final String MSG_USER_NOT_FOUND = "\u7528\u6237\u4e0d\u5b58\u5728";
    private static final String MSG_LOGIN_FAILED = "\u7528\u6237\u540d\u6216\u5bc6\u7801\u9519\u8bef";
    private static final String MSG_ACCOUNT_DISABLED = "\u8d26\u53f7\u5df2\u7981\u7528";
    private static final String MSG_PASSWORD_ERROR = "\u5bc6\u7801\u9519\u8bef";
    private static final String MSG_NOT_LOGIN = "\u672a\u767b\u5f55";
    private static final String MSG_ROLE_NOT_FOUND = "\u7528\u6237\u89d2\u8272\u4e0d\u5b58\u5728";
    private static final String MSG_ROLE_REQUIRED = "\u89d2\u8272\u4e0d\u80fd\u4e3a\u7a7a";
    private static final String MSG_REGISTER_ROLE_INVALID = "\u6ce8\u518c\u89d2\u8272\u53c2\u6570\u4e0d\u5408\u6cd5";
    private static final String MSG_ROLE_INVALID = "\u89d2\u8272\u53c2\u6570\u4e0d\u5408\u6cd5";
    private static final String MSG_LOGIN_ROLE_MISMATCH = "\u767b\u5f55\u89d2\u8272\u4e0e\u8d26\u53f7\u89d2\u8272\u4e0d\u5339\u914d";

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysLoginLogMapper sysLoginLogMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public LoginResponse register(RegisterRequest request, String ip, String userAgent) {
        SysUser existed = sysUserMapper.selectByUsername(request.getUsername());
        if (existed != null) {
            return new LoginResponse(false, null, null, MSG_USERNAME_EXISTS);
        }

        String roleCode = normalizeRegisterRole(request.getRole());

        SysUser u = new SysUser();
        u.setUsername(request.getUsername());
        u.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        String displayName = request.getDisplayName();
        if (displayName == null || displayName.isBlank()) {
            displayName = request.getUsername();
        }
        u.setDisplayName(displayName);
        u.setEmail(request.getEmail());
        u.setStatus(1);
        u.setIsDeleted(0);
        sysUserMapper.insert(u);

        bindRole(u.getId(), roleCode);
        recordAudit(u.getId(), "USER_REGISTER", "USER", u.getId(), null, userAuditSnapshot(u, roleCode));

        String token = jwtUtil.generateToken(u.getId(), u.getUsername(), roleCode);
        sysUserMapper.updateLastLoginAt(u.getId(), LocalDateTime.now());
        writeLoginLog(u.getId(), u.getUsername(), 1, null, ip, userAgent);

        LoginResponse.UserDTO userDTO = new LoginResponse.UserDTO(
                u.getId(),
                u.getUsername(),
                u.getDisplayName(),
                u.getEmail(),
                roleCode
        );
        return new LoginResponse(true, token, userDTO, MSG_SUCCESS);
    }

    @Override
    public LoginResponse login(LoginRequest request, String ip, String userAgent) {
        SysUser user = sysUserMapper.selectByUsername(request.getUsername());
        if (user == null) {
            writeLoginLog(null, request.getUsername(), 0, MSG_USER_NOT_FOUND, ip, userAgent);
            return new LoginResponse(false, null, null, MSG_LOGIN_FAILED);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            writeLoginLog(user.getId(), user.getUsername(), 0, MSG_ACCOUNT_DISABLED, ip, userAgent);
            return new LoginResponse(false, null, null, MSG_ACCOUNT_DISABLED);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            writeLoginLog(user.getId(), user.getUsername(), 0, MSG_PASSWORD_ERROR, ip, userAgent);
            return new LoginResponse(false, null, null, MSG_LOGIN_FAILED);
        }

        String roleCode = getRoleOrThrow(user.getId());
        String requestedRole = normalizeLoginRole(request.getRole());
        if (requestedRole != null && !requestedRole.equalsIgnoreCase(roleCode)) {
            writeLoginLog(user.getId(), user.getUsername(), 0, MSG_LOGIN_ROLE_MISMATCH, ip, userAgent);
            return new LoginResponse(false, null, null, MSG_LOGIN_ROLE_MISMATCH);
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), roleCode);

        sysUserMapper.updateLastLoginAt(user.getId(), LocalDateTime.now());
        writeLoginLog(user.getId(), user.getUsername(), 1, null, ip, userAgent);

        LoginResponse.UserDTO userDTO = new LoginResponse.UserDTO(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                roleCode
        );
        return new LoginResponse(true, token, userDTO, MSG_SUCCESS);
    }

    @Override
    public LoginResponse.UserDTO me() {
        Long uid = SecurityContextUtil.currentUserId();
        if (uid == null) {
            throw BizException.of(ErrorCode.UNAUTHORIZED, MSG_NOT_LOGIN);
        }

        SysUser user = sysUserMapper.selectById(uid);
        if (user == null) {
            throw BizException.of(ErrorCode.UNAUTHORIZED, MSG_USER_NOT_FOUND);
        }

        String roleCode = getRoleOrThrow(uid);
        return new LoginResponse.UserDTO(user.getId(), user.getUsername(), user.getDisplayName(), user.getEmail(), roleCode);
    }

    @Override
    public PageResponse<UserListItemVO> pageUsers(long page, long size) {
        long safePage = PageParamUtil.normalizePage(page);
        long safeSize = PageParamUtil.normalizeSize(size);
        long offset = PageParamUtil.offset(safePage, safeSize);

        List<SysUser> users = sysUserMapper.page(offset, safeSize);
        long total = sysUserMapper.countAll();

        List<UserListItemVO> list = new ArrayList<>();
        for (SysUser u : users) {
            UserListItemVO vo = new UserListItemVO();
            vo.setId(u.getId());
            vo.setUsername(u.getUsername());
            vo.setDisplayName(u.getDisplayName());
            vo.setEmail(u.getEmail());
            vo.setStatus(u.getStatus());
            vo.setCreatedAt(u.getCreatedAt());
            vo.setRole(sysRoleMapper.selectRoleCodeByUserId(u.getId()));
            list.add(vo);
        }
        return PageResponse.of(safePage, safeSize, total, list);
    }

    @Override
    @Transactional
    public Long createUser(AdminCreateUserRequest request) {
        SysUser existed = sysUserMapper.selectByUsername(request.getUsername());
        if (existed != null) {
            throw BizException.of(ErrorCode.CONFLICT, MSG_USERNAME_EXISTS);
        }

        SysUser u = new SysUser();
        u.setUsername(request.getUsername());
        u.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        u.setDisplayName(request.getDisplayName());
        u.setEmail(request.getEmail());
        u.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        u.setIsDeleted(0);
        sysUserMapper.insert(u);

        String roleCode = normalizeAdminRole(request.getRole());
        bindRole(u.getId(), roleCode);
        recordAudit(SecurityContextUtil.currentUserId(),
                "USER_CREATE",
                "USER",
                u.getId(),
                null,
                userAuditSnapshot(u, roleCode));
        return u.getId();
    }

    @Override
    public void updateUser(Long userId, AdminUpdateUserRequest request) {
        SysUser u = sysUserMapper.selectById(userId);
        if (u == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, MSG_USER_NOT_FOUND);
        }
        String currentRole = sysRoleMapper.selectRoleCodeByUserId(userId);
        Map<String, Object> before = userAuditSnapshot(u, currentRole);

        if (request.getDisplayName() != null) {
            u.setDisplayName(request.getDisplayName());
        }
        if (request.getEmail() != null) {
            u.setEmail(request.getEmail());
        }
        if (request.getStatus() != null) {
            u.setStatus(request.getStatus());
        }
        sysUserMapper.update(u);
        recordAudit(SecurityContextUtil.currentUserId(),
                "USER_UPDATE",
                "USER",
                u.getId(),
                before,
                userAuditSnapshot(u, currentRole));
    }

    @Override
    @Transactional
    public void updateUserRole(Long userId, AdminUpdateUserRoleRequest request) {
        SysUser u = sysUserMapper.selectById(userId);
        if (u == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, MSG_USER_NOT_FOUND);
        }
        String beforeRole = getRoleOrThrow(userId);
        String roleCode = normalizeAdminRole(request.getRole());
        sysUserRoleMapper.deleteByUserId(userId);
        bindRole(userId, roleCode);
        recordAudit(SecurityContextUtil.currentUserId(),
                "USER_ROLE_UPDATE",
                "USER",
                userId,
                Map.of("role", beforeRole),
                Map.of("role", roleCode));
    }

    @Override
    public Long adminCreateUser(AdminCreateUserRequest request) {
        return createUser(request);
    }

    @Override
    public void adminUpdateUser(Long userId, AdminUpdateUserRequest request) {
        updateUser(userId, request);
    }

    @Override
    public void adminUpdateUserRole(Long userId, AdminUpdateUserRoleRequest request) {
        updateUserRole(userId, request);
    }

    private void bindRole(Long userId, String roleCode) {
        SysRole role = sysRoleMapper.selectByCode(roleCode);
        if (role == null) {
            SysRole r = new SysRole();
            r.setRoleCode(roleCode);
            r.setRoleName(roleCode);
            sysRoleMapper.insert(r);
            role = r;
        }
        sysUserRoleMapper.insert(userId, role.getId());
    }

    private String getRoleOrThrow(Long userId) {
        String roleCode = sysRoleMapper.selectRoleCodeByUserId(userId);
        if (roleCode == null || roleCode.isBlank()) {
            throw BizException.of(ErrorCode.FORBIDDEN, MSG_ROLE_NOT_FOUND);
        }
        return roleCode;
    }

    private void writeLoginLog(Long userId, String username, int successFlag, String failReason, String ip, String userAgent) {
        try {
            SysLoginLog log = new SysLoginLog();
            log.setUserId(userId);
            log.setUsername(username);
            log.setSuccessFlag(successFlag);
            log.setFailReason(failReason);
            log.setIpAddr(ip);
            log.setUserAgent(userAgent);
            sysLoginLogMapper.insert(log);
        } catch (Exception ignore) {
            // Ignore login log errors.
        }
    }

    private String normalizeRegisterRole(String role) {
        if (role == null) {
            throw BizException.of(ErrorCode.PARAM_ERROR, MSG_ROLE_REQUIRED);
        }
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        if (RoleCode.STUDENT.name().equals(normalized) || RoleCode.TEACHER.name().equals(normalized)) {
            return normalized;
        }
        throw BizException.of(ErrorCode.PARAM_ERROR, MSG_REGISTER_ROLE_INVALID);
    }

    private String normalizeLoginRole(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        if (RoleCode.STUDENT.name().equals(normalized)
                || RoleCode.TEACHER.name().equals(normalized)
                || RoleCode.ADMIN.name().equals(normalized)) {
            return normalized;
        }
        throw BizException.of(ErrorCode.PARAM_ERROR, MSG_ROLE_INVALID);
    }

    private String normalizeAdminRole(String role) {
        if (role == null) {
            throw BizException.of(ErrorCode.PARAM_ERROR, MSG_ROLE_REQUIRED);
        }
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        if (RoleCode.STUDENT.name().equals(normalized)
                || RoleCode.TEACHER.name().equals(normalized)
                || RoleCode.ADMIN.name().equals(normalized)) {
            return normalized;
        }
        throw BizException.of(ErrorCode.PARAM_ERROR, MSG_ROLE_INVALID);
    }

    private Map<String, Object> userAuditSnapshot(SysUser user, String roleCode) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", user.getId());
        snapshot.put("username", user.getUsername());
        snapshot.put("displayName", user.getDisplayName());
        snapshot.put("email", user.getEmail());
        snapshot.put("status", user.getStatus());
        snapshot.put("role", roleCode);
        return snapshot;
    }

    private void recordAudit(Long userId,
                             String action,
                             String entityType,
                             Long entityId,
                             Object beforeData,
                             Object afterData) {
        if (auditLogService == null) {
            return;
        }
        auditLogService.record(userId, action, entityType, entityId, beforeData, afterData);
    }
}
