package com.project.reservation.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class EmailVerificationService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String EMAIL_VERIFIED_KEY = "verified:email:";
    private static final String EMAIL_CODE_KEY = "code:email:";
    private static final long CODE_EXPIRE_MINUTES = 5;
    private static final long VERIFIED_EXPIRE_HOURS = 24;

    public EmailVerificationService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // 인증 코드 저장 (실제 발송은 AWS SES에서 처리)
    public void saveVerificationCode(String email, String code) {
        redisTemplate.opsForValue().set(
            EMAIL_CODE_KEY + email,
            code,
            CODE_EXPIRE_MINUTES,
            TimeUnit.MINUTES
        );
    }

    // 인증 코드 검증 (슬라이드 ⑥번 - 이메일 1계정 제한)
    public boolean verifyCode(String email, String inputCode) {
        String savedCode = redisTemplate.opsForValue().get(EMAIL_CODE_KEY + email);

        if (savedCode == null) {
            return false; // 코드 만료
        }

        if (!savedCode.equals(inputCode)) {
            return false; // 코드 불일치
        }

        // 인증 완료 표시 - 24시간 유지
        redisTemplate.opsForValue().set(
            EMAIL_VERIFIED_KEY + email,
            "verified",
            VERIFIED_EXPIRE_HOURS,
            TimeUnit.HOURS
        );

        // 사용한 코드 즉시 삭제
        redisTemplate.delete(EMAIL_CODE_KEY + email);

        return true;
    }

    // 이메일 인증 여부 확인
    public boolean isEmailVerified(String email) {
        return Boolean.TRUE.equals(
            redisTemplate.hasKey(EMAIL_VERIFIED_KEY + email)
        );
    }

    // 중복 계정 체크 (슬라이드 ⑥번 - Redis Set NX 중복차단)
    public boolean registerEmail(String email) {
        Long added = redisTemplate.opsForSet().add("registered:emails", email);
        return added != null && added == 1;
    }
}
