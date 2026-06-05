package com.project.reservation.waiting_redis.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.reservation.common.WaitingsApiResponse;
import com.project.reservation.waiting_redis.service.Queue1Service;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/queue1")
public class AdminController {

    private final Queue1Service queue1Service;

    @GetMapping("/status")
    public ResponseEntity<WaitingsApiResponse<?>> status() {
        boolean active = queue1Service.isActive();
        boolean autoEnabled = queue1Service.isAutoEnabled();
        return ResponseEntity.ok(
            WaitingsApiResponse.success("Queue1 상태 조회 성공", Map.of("active", active, "autoEnabled", autoEnabled))
        );
    }

    @PostMapping("/activate")
    public ResponseEntity<WaitingsApiResponse<?>> activate() {
        queue1Service.activate();
        return ResponseEntity.ok(
            WaitingsApiResponse.success("Queue1 활성화 완료", Map.of("active", true))
        );
    }

    @DeleteMapping("/deactivate")
    public ResponseEntity<WaitingsApiResponse<?>> deactivate() {
        queue1Service.deactivate();
        return ResponseEntity.ok(
            WaitingsApiResponse.success("Queue1 비활성화 완료", Map.of("active", false))
        );
    }

    @PostMapping("/enable-auto")
    public ResponseEntity<WaitingsApiResponse<?>> enableAuto() {
        queue1Service.enableAuto();
        return ResponseEntity.ok(
            WaitingsApiResponse.success("Queue1 자동 활성화 허용 — 대기열 방식 페이즈", Map.of("autoEnabled", true))
        );
    }

    @DeleteMapping("/disable-auto")
    public ResponseEntity<WaitingsApiResponse<?>> disableAuto() {
        queue1Service.disableAuto();
        return ResponseEntity.ok(
            WaitingsApiResponse.success("Queue1 자동 활성화 차단 — Warm Pool 페이즈", Map.of("autoEnabled", false))
        );
    }
}
