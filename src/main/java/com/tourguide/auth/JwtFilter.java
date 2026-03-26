package com.tourguide.auth;

import com.tourguide.common.enums.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        String method = request.getMethod();
        String path = request.getRequestURI();
        String clientIp = request.getRemoteAddr();

        try {
            if (!jwtUtil.validateToken(token)) {
                log.debug("JWT ignored: invalid token method={} path={} clientIp={}", method, path, clientIp);
                filterChain.doFilter(request, response);
                return;
            }

            if (isTokenBlacklisted(token)) {
                log.warn("JWT rejected: blacklisted token method={} path={} clientIp={}", method, path, clientIp);
                filterChain.doFilter(request, response);
                return;
            }

            String tokenType = jwtUtil.getTokenType(token);
            if (!"access".equals(tokenType)) {
                log.warn("JWT rejected: non-access token type={} method={} path={} clientIp={}", tokenType, method, path, clientIp);
                filterChain.doFilter(request, response);
                return;
            }

            UUID userId = jwtUtil.getUserId(token);
            Role role = jwtUtil.getRole(token);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("JWT authenticated: userId={} role={} method={} path={}", userId, role, method, path);
        } catch (Exception e) {
            log.warn("JWT authentication failed: method={} path={} clientIp={} reason={}", method, path, clientIp, e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }
}
