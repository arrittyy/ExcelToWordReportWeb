package com.reportweb.security;

import com.reportweb.entity.User;
import com.reportweb.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);
        // 先按用户名查，再按规范化用户名查（与登录一致，子账号等不区分大小写）
        User user = userRepository.findByUserName(username)
                .orElseGet(() -> userRepository.findByNormalizedUserName(username != null ? username.toUpperCase() : "")
                        .orElse(null));
        if (user == null) {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }
        log.debug("Found user: {} (id={}, parentUserId={})", user.getUserName(), user.getId(), user.getParentUserId());
        return new CustomUserPrincipal(user);
    }
}


