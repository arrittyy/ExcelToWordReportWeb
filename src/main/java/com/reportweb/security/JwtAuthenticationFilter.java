package com.reportweb.security;

import com.reportweb.security.CustomUserDetailsService;
import com.reportweb.security.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(@org.springframework.lang.NonNull HttpServletRequest request, @org.springframework.lang.NonNull HttpServletResponse response, @org.springframework.lang.NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);
            String requestURI = request.getRequestURI();
            
            // 跳过认证端点的日志
            if (!requestURI.startsWith("/api/auth/")) {
                if (StringUtils.hasText(jwt)) {
                    log.debug("🔍 [JWT Filter] Request: {} {}, Token present: {}", request.getMethod(), requestURI, jwt.substring(0, Math.min(20, jwt.length())) + "...");
                } else {
                    log.warn("⚠️ [JWT Filter] Request: {} {}, No token in header", request.getMethod(), requestURI);
                }
            }

            if (StringUtils.hasText(jwt)) {
                try {
                    if (tokenProvider.validateToken(jwt)) {
                        String username = tokenProvider.getUsernameFromToken(jwt);
                        log.debug("✅ [JWT Filter] Token valid for user: {}", username);

                        UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.debug("✅ [JWT Filter] Authentication set for user: {}", username);
                    } else {
                        log.warn("❌ [JWT Filter] Token validation failed for request: {} {}", request.getMethod(), requestURI);
                        // Token 无效时，清除可能存在的认证信息
                        SecurityContextHolder.clearContext();
                    }
                } catch (Exception tokenEx) {
                    log.error("❌ [JWT Filter] Error processing token: {}", tokenEx.getMessage(), tokenEx);
                    SecurityContextHolder.clearContext();
                }
            } else {
                // 没有 token，清除可能存在的认证信息
                SecurityContextHolder.clearContext();
            }
        } catch (Exception ex) {
            log.error("❌ [JWT Filter] Could not set user authentication in security context", ex);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
