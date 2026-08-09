package com.nijat.ptlt.auth.filter;

import com.nijat.ptlt.auth.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

/**
 * JWT 认证过滤器
 * 拦截所有 API 请求，验证 Token 并设置安全上下文
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final UUID userId;

        // 1. 检查 Header 是否包含 Bearer Token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        try {
            // 2. 验证 Token 有效性
            if (jwtUtil.validateToken(jwt)) {
                userId = jwtUtil.getUserIdFromToken(jwt);
                
                // 3. 构建 Spring Security 的认证对象
                if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userId, 
                            null, 
                            new ArrayList<>() // 权限列表，目前留空
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // 4. 将认证信息存入上下文
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Token 解析失败，直接放行，由后续的 Security 配置处理 403
            logger.error("JWT authentication failed: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
