package com.xyz.question_bank_management_system.config;

import com.xyz.question_bank_management_system.modules.user.entity.SysRole;
import com.xyz.question_bank_management_system.modules.user.entity.SysUser;
import com.xyz.question_bank_management_system.modules.user.mapper.SysRoleMapper;
import com.xyz.question_bank_management_system.modules.user.mapper.SysUserMapper;
import com.xyz.question_bank_management_system.modules.user.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InitDataRunner implements CommandLineRunner {

    private static final String ROLE_STUDENT = "\u5b66\u751f";
    private static final String ROLE_TEACHER = "\u6559\u5e08";
    private static final String ROLE_ADMIN = "\u7ba1\u7406\u5458";
    private static final String DEFAULT_ADMIN_NAME = "\u7cfb\u7edf\u7ba1\u7406\u5458";
    private static final String INIT_ADMIN_LOG = "\u5df2\u521d\u59cb\u5316\u9ed8\u8ba4\u7ba1\u7406\u5458\u8d26\u53f7\uff1a{}\u3002\u4e3a\u4fdd\u8bc1\u5b89\u5168\uff0c\u8bf7\u5c3d\u5feb\u4fee\u6539\u5bc6\u7801\u3002";

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.init.enabled:true}")
    private boolean enabled;

    @Value("${app.init.admin-username:admin}")
    private String adminUsername;

    @Value("${app.init.admin-password:admin123}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (!enabled) {
            return;
        }

        ensureRole("STUDENT", ROLE_STUDENT);
        ensureRole("TEACHER", ROLE_TEACHER);
        ensureRole("ADMIN", ROLE_ADMIN);

        SysUser existing = sysUserMapper.selectByUsername(adminUsername);
        if (existing == null) {
            SysUser u = new SysUser();
            u.setUsername(adminUsername);
            u.setPasswordHash(passwordEncoder.encode(adminPassword));
            u.setDisplayName(DEFAULT_ADMIN_NAME);
            u.setEmail("admin@example.com");
            u.setStatus(1);
            u.setIsDeleted(0);
            sysUserMapper.insert(u);

            SysRole adminRole = sysRoleMapper.selectByCode("ADMIN");
            if (adminRole != null) {
                sysUserRoleMapper.insert(u.getId(), adminRole.getId());
            }
            log.warn(INIT_ADMIN_LOG, adminUsername);
        }
    }

    private void ensureRole(String roleCode, String roleName) {
        SysRole role = sysRoleMapper.selectByCode(roleCode);
        if (role == null) {
            SysRole r = new SysRole();
            r.setRoleCode(roleCode);
            r.setRoleName(roleName);
            sysRoleMapper.insert(r);
        }
    }
}
