package org.chengyuan.cyproject.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.chengyuan.cyproject.dto.response.Response;
import org.chengyuan.cyproject.error.ErrorCode;
import org.chengyuan.cyproject.util.JwtUtil;
import org.chengyuan.cyproject.util.UserContext;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * JWT 认证过滤器。
 * 除登录、注册外的接口都需要携带 Authorization: Bearer <token>；
 * 校验通过后将用户 id 写入 UserContext，供 Service 层使用。
 */
public class JwtFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (isPublicPath(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        String token = null;
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            token = header.substring(BEARER_PREFIX.length());
        }

        String userid = token == null ? null : jwtUtil.parseToken(token);
        if (userid == null) {
            writeUnauthorized(response, ErrorCode.UNAUTHORIZED);
            return;
        }

        UserContext.setUserId(userid);
        try {
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }

    private boolean isPublicPath(String uri) {
        // 登录、注册无需认证
        return uri.startsWith("/begain/") || uri.startsWith("/avatarFiles/") || uri.startsWith("/uploads/");
    }

    private void writeUnauthorized(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        response.setStatus(errorCode.getHttpStatus().value());
        response.getWriter().write(objectMapper.writeValueAsString(Response.fail(errorCode)));
    }
}
