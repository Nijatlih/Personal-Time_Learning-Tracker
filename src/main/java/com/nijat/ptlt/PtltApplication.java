package com.nijat.ptlt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 排除 UserDetailsServiceAutoConfiguration：认证走 JwtAuthenticationFilter，
 * 过滤器链里既没有 formLogin 也没有 httpBasic，所以那个自动配出来的内存用户
 * 根本无法用于认证。留着它唯一的效果是每次启动都在 WARN 级别打印一行
 * "Using generated security password: ..."，把一个不存在的凭据当成真的告诉运维。
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableScheduling  // RefreshTokenCleanupJob
public class PtltApplication {
    public static void main(String[] args) {
        SpringApplication.run(PtltApplication.class, args);
    }
}
