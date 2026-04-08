package com.example.dailymenu.user.adapter.in.web.filter;

import com.example.dailymenu.user.application.port.out.TokenPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * JWT 인증 필터 — Bearer 토큰에서 userId 추출 → @RequestAttribute("userId") 설정.
 * Public 경로(register, login, refresh, health)는 인증 없이 통과.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/auth/", "/health", "/swagger-ui", "/v3/api-docs", "/swagger-ui.html",
            "/index.html", "/favicon.ico"
    );

    private final TokenPort tokenPort;

    /** CORS preflight(OPTIONS) 요청은 인증 없이 통과시킨다. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            try {
                Long userId = tokenPort.parseUserId(header.substring(7));
                request.setAttribute("userId", userId);
            } catch (Exception e) {
                log.warn("JWT 인증 실패 uri={}", request.getRequestURI(), e);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        } else if (!isPublicPath(request.getRequestURI())) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicPath(String uri) {
        return PUBLIC_PATHS.stream().anyMatch(uri::startsWith);
    }
}
