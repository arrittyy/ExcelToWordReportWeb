package com.reportweb.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    static final String SESSION_EXPIRED_MESSAGE = "登录已过期，请重新登录";

    private final SecurityJsonResponseWriter jsonResponseWriter;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        jsonResponseWriter.write(response, HttpStatus.UNAUTHORIZED.value(), SESSION_EXPIRED_MESSAGE);
    }
}
