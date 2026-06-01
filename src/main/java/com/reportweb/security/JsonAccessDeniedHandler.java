package com.reportweb.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    static final String FORBIDDEN_MESSAGE = "没有权限执行此操作";

    private final SecurityJsonResponseWriter jsonResponseWriter;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            jsonResponseWriter.write(
                    response,
                    HttpStatus.UNAUTHORIZED.value(),
                    JsonAuthenticationEntryPoint.SESSION_EXPIRED_MESSAGE);
            return;
        }
        jsonResponseWriter.write(response, HttpStatus.FORBIDDEN.value(), FORBIDDEN_MESSAGE);
    }
}
