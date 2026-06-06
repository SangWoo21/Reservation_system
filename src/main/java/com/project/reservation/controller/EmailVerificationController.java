package com.project.reservation.controller;

import com.project.reservation.service.EmailVerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/email")
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;
    private final MailSender mailSender;

    public EmailVerificationController(EmailVerificationService emailVerificationService,
                                       MailSender mailSender) {
        this.emailVerificationService = emailVerificationService;
        this.mailSender = mailSender;
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

        // AWS SES 이메일 발송
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("twinf1004@hufs.ac.kr");
        message.setTo(email);
        message.setSubject("[예약 시스템] 이메일 인증 코드");
        message.setText("인증 코드: " + code + "\n\n유효 시간: 5분");
        mailSender.send(message);

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
