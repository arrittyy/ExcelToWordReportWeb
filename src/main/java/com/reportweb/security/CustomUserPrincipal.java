package com.reportweb.security;

import com.reportweb.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@RequiredArgsConstructor
public class CustomUserPrincipal implements UserDetails {
    
    private final User user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 根据用户角色返回对应权限
        String role = user.getRole();
        if (role != null && UserRoleUtils.ROLE_ADMIN.equals(role)) {
            return Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getUserName();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // 如果锁定功能未启用，账户未锁定
        if (user.getLockoutEnabled() == null || !user.getLockoutEnabled()) {
            return true;
        }
        // 如果锁定功能启用但没有锁定结束时间，账户未锁定
        if (user.getLockoutEnd() == null) {
            return true;
        }
        // 如果锁定结束时间已过，账户未锁定
        return user.getLockoutEnd().isBefore(java.time.LocalDateTime.now());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public User getUser() {
        return user;
    }
}


