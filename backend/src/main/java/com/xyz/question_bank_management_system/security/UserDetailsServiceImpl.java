package com.xyz.question_bank_management_system.security;

import com.xyz.question_bank_management_system.modules.user.entity.SysUser;
import com.xyz.question_bank_management_system.modules.user.mapper.SysRoleMapper;
import com.xyz.question_bank_management_system.modules.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = sysUserMapper.selectByUsername(username);
        if (user == null || user.getIsDeleted() != null && user.getIsDeleted() == 1) {
            throw new UsernameNotFoundException("用户不存在");
        }
        String role = sysRoleMapper.selectRoleCodeByUserId(user.getId());
        boolean enabled = user.getStatus() != null && user.getStatus() == 1;
        return new UserPrincipal(user.getId(), user.getUsername(), user.getPasswordHash(), enabled, role);
    }
}
