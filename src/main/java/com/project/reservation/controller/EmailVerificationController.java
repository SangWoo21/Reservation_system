package com.project.reservation.controller;

import com.project.reservation.service.EmailVerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/email")
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    public EmailVerificationController(EmailVerificationService emailVerificationService) {
        this.emailVerificationService = emailVerificationService;
    }

    // 이메일 인증 코드 발송 요청
    @PostMapping("/send")
    public ResponseEntity<?> sendVerificationCode(@RequestBody Map<String, String> body) {
        String email = body.get("email");

        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing email"));
        }

        // 중복 계정 체크 (슬라이드 ⑥번)
        boolean isNew = emailVerificationService.registerEmail(email);
        if (!isNew) {
            return ResponseEntity.status(409).body(Map.of(
                "error", "Email already registered",
                "code", "DUPLICATE_EMAIL"
            ));
        }

        // 6자리 인증 코드 생성
        String code = String.valueOf((int)(Math.random() * 900000) + 100000);
        emailVerificationService.saveVerificationCode(email, code);

        // TODO: AWS SES로 실제 이메일 발송 연동
        System.out.println("[EMAIL] 인증 코드 발송 → " + email + " : " + code);

        return ResponseEntity.ok(Map.of(
            "message", "Verification code sent",
            "expiresIn", "5 minutes"
        ));
    }

    // 인증 코드 확인
    @PostMapping("/verify")
    public ResponseEntity<?> verifyCode(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String code = body.get("code");

        if (email == null || code == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing email or code"));
        }

        boolean verified = emailVerificationService.verifyCode(email, code);

        if (!verified) {
            return ResponseEntity.status(401).body(Map.of(
                "error", "Invalid or expired code",
                "code", "VERIFICATION_FAILED"
            ));
        }

        return ResponseEntity.ok(Map.of(
            "message", "Email verified successfully",
            "email", email
        ));
    }

    // 인증 여부 확인
    @GetMapping("/status")
    public ResponseEntity<?> checkVerificationStatus(@RequestParam String email) {
        boolean verified = emailVerificationService.isEmailVerified(email);
        return ResponseEntity.ok(Map.of(
            "email", email,
            "verified", verified
        ));
    }
}
