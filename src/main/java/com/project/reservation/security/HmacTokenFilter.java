package com.project.reservation.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.util.Base64;

@Component
public class HmacTokenFilter extends OncePerRequestFilter {

    @Value("${hmac.secret.key}")
    private String secretKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Form 제출(입장권 확인) API에서만 토큰 검증 (슬라이드 ⑤번)
        String path = request.getRequestURI();
        if (path.startsWith("/api/waitings/confirm")) {
            String token = request.getHeader("X-Queue-Token");
            String userId = request.getParameter("userId");

            if (token == null || userId == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing queue token");
                return;
            }

            if (!validateToken(userId, token)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid queue token");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean validateToken(String userId, String token) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
            mac.init(keySpec);
            String expected = Base64.getEncoder().encodeToString(mac.doFinal(userId.getBytes()));
            return expected.equals(token);
        } catch (Exception e) {
            return false;
        }
    }
}
