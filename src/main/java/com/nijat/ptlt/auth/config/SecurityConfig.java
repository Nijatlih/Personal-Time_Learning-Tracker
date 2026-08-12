package com.nijat.ptlt.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nijat.ptlt.auth.filter.JwtAuthenticationFilter;
import com.nijat.ptlt.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final ObjectMapper objectMapper;

    /**
     * 允许的跨域来源。默认只有本地开发地址；部署时通过 APP_CORS_ALLOWED_ORIGINS 设置真实域名。
     *
     * 这里绑的是原始字符串、自己切分，而不是让 Spring 直接转成 List&lt;String&gt;：
     * 后者走 commaDelimitedListToStringArray，按逗号切但**不 trim**。运维把环境变量写成
     * "a.com, b.com"（逗号后一个空格，最自然的写法）时，第二个来源会变成 " https://b.com"，
     * 而 CorsConfiguration.checkOrigin 是精确匹配 —— 该来源静默失效，无启动报错、无日志。
     */
    @Value("${app.cors.allowed-origins}")
    private String allowedOriginsRaw;


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 后端只提供 API，不 serve 任何前端资源，所以这里不再有静态资源放行项。
                // 此前放行 /、/index.html、/*.js 等，配合 src/main/resources/static/ 里
                // 那份早已废弃的 vanilla JS SPA —— 直接跑后端时 serve 的就是它，
                // 而不是 frontend/ 下的 React 应用（React 应用本来也不进 jar）。
                // 前端现在走 Vite（开发）或独立静态托管（生产）。
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .anyRequest().authenticated()
                )
                // 未认证 → 401，而不是 403。
                //
                // 这条链是 stateless 且没配任何登录入口，所以在没有显式 entry point 时
                // Spring Security 兜底用的是 Http403ForbiddenEntryPoint —— 于是"token 过期了"
                // 和"你没权限做这件事"返回同一个状态码，前端无从区分，刷新流程也没法接。
                // 403 的根因在这里，不在 JwtAuthenticationFilter。
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(this::writeUnauthorized)
                        .accessDeniedHandler((request, response, denied) ->
                                writeError(response, HttpServletResponse.SC_FORBIDDEN,
                                        "FORBIDDEN", "Access denied"))
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void writeUnauthorized(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                "UNAUTHORIZED", "Authentication required or token is invalid");
    }

    /** 认证失败也回 ApiResponse 结构，与 GlobalExceptionHandler 保持一致。 */
    private void writeError(HttpServletResponse response, int status, String code, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.error(code, message));
    }

    /** 按逗号切分并 trim，丢掉空串。见 allowedOriginsRaw 上的说明。 */
    private List<String> parseAllowedOrigins() {
        return Arrays.stream(allowedOriginsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 显式来源列表，从 app.cors.allowed-origins 读取。
        // 此前是 setAllowedOriginPatterns(List.of("*")) 搭配 setAllowCredentials(true)：
        // 等于对任意站点关闭浏览器的同源隔离，任何页面都能带着用户凭据调用本 API。
        configuration.setAllowedOrigins(parseAllowedOrigins());
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "accept", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
