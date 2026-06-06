package com.project.reservation.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        return new InMemoryUserDetailsManager(
            User.withUsername("admin")
                .password(passwordEncoder.encode("3vvnz3Q8VXb6Gk09"))
                .roles("ADMIN")
                .build()
        );
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, HmacTokenFilter hmacTokenFilter) throws Exception {
        http
            // CSRF: REST API는 stateless이므로 비활성화 (HMAC 토큰으로 대체)
            .csrf(csrf -> csrf.disable())

            // 보안 헤더
            .headers(headers -> headers
                .xssProtection(xss -> xss
                    .headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK)
                )
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'")
                )
                .frameOptions(frame -> frame.deny())
            )

            // API 접근 권한
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/waitings/**").permitAll()
                .requestMatchers("/api/reservations/**").permitAll()
                .requestMatchers("/api/email/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/api/admin/**").authenticated()
                .anyRequest().authenticated()
            )

            // Basic Auth 활성화
            .httpBasic(basic -> {});

        http.addFilterBefore(hmacTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
